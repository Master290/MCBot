package io.netty.handler.codec.http2;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.ServerChannel;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.net.ssl.SSLException;






































































public final class Http2MultiplexHandler
  extends Http2ChannelDuplexHandler
{
  static final ChannelFutureListener CHILD_CHANNEL_REGISTRATION_LISTENER = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future) {
      Http2MultiplexHandler.registerDone(future);
    }
  };
  
  private final ChannelHandler inboundStreamHandler;
  private final ChannelHandler upgradeStreamHandler;
  private final Queue<AbstractHttp2StreamChannel> readCompletePendingQueue = new MaxCapacityQueue(new ArrayDeque(8), 100);
  


  private boolean parentReadInProgress;
  


  private int idCount;
  


  private volatile ChannelHandlerContext ctx;
  


  public Http2MultiplexHandler(ChannelHandler inboundStreamHandler)
  {
    this(inboundStreamHandler, null);
  }
  







  public Http2MultiplexHandler(ChannelHandler inboundStreamHandler, ChannelHandler upgradeStreamHandler)
  {
    this.inboundStreamHandler = ((ChannelHandler)ObjectUtil.checkNotNull(inboundStreamHandler, "inboundStreamHandler"));
    this.upgradeStreamHandler = upgradeStreamHandler;
  }
  


  static void registerDone(ChannelFuture future)
  {
    if (!future.isSuccess()) {
      Channel childChannel = future.channel();
      if (childChannel.isRegistered()) {
        childChannel.close();
      } else {
        childChannel.unsafe().closeForcibly();
      }
    }
  }
  
  protected void handlerAdded0(ChannelHandlerContext ctx)
  {
    if (ctx.executor() != ctx.channel().eventLoop()) {
      throw new IllegalStateException("EventExecutor must be EventLoop of Channel");
    }
    this.ctx = ctx;
  }
  
  protected void handlerRemoved0(ChannelHandlerContext ctx)
  {
    readCompletePendingQueue.clear();
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    parentReadInProgress = true;
    if ((msg instanceof Http2StreamFrame)) {
      if ((msg instanceof Http2WindowUpdateFrame))
      {
        return;
      }
      Http2StreamFrame streamFrame = (Http2StreamFrame)msg;
      
      Http2FrameCodec.DefaultHttp2FrameStream s = (Http2FrameCodec.DefaultHttp2FrameStream)streamFrame.stream();
      
      AbstractHttp2StreamChannel channel = (AbstractHttp2StreamChannel)attachment;
      if ((msg instanceof Http2ResetFrame))
      {

        channel.pipeline().fireUserEventTriggered(msg);

      }
      else
      {
        channel.fireChildRead(streamFrame);
      }
      return;
    }
    
    if ((msg instanceof Http2GoAwayFrame))
    {

      onHttp2GoAwayFrame(ctx, (Http2GoAwayFrame)msg);
    }
    

    ctx.fireChannelRead(msg);
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    if (ctx.channel().isWritable())
    {

      forEachActiveStream(AbstractHttp2StreamChannel.WRITABLE_VISITOR);
    }
    
    ctx.fireChannelWritabilityChanged();
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    if ((evt instanceof Http2FrameStreamEvent)) {
      Http2FrameStreamEvent event = (Http2FrameStreamEvent)evt;
      Http2FrameCodec.DefaultHttp2FrameStream stream = (Http2FrameCodec.DefaultHttp2FrameStream)event.stream();
      if (event.type() == Http2FrameStreamEvent.Type.State) {
        switch (4.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()]) {
        case 1: 
          if (stream.id() != 1) {
            break;
          }
        


        case 2: 
        case 3: 
          if (attachment == null)
          {
            AbstractHttp2StreamChannel ch;
            


            if ((stream.id() == 1) && (!isServer(ctx)))
            {
              if (upgradeStreamHandler == null) {
                throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, "Client is misconfigured for upgrade requests", new Object[0]);
              }
              
              AbstractHttp2StreamChannel ch = new Http2MultiplexHandlerStreamChannel(stream, upgradeStreamHandler);
              ch.closeOutbound();
            } else {
              ch = new Http2MultiplexHandlerStreamChannel(stream, inboundStreamHandler);
            }
            ChannelFuture future = ctx.channel().eventLoop().register(ch);
            if (future.isDone()) {
              registerDone(future);
            } else
              future.addListener(CHILD_CHANNEL_REGISTRATION_LISTENER);
          }
          break;
        case 4: 
          AbstractHttp2StreamChannel channel = (AbstractHttp2StreamChannel)attachment;
          if (channel != null) {
            channel.streamClosed();
          }
          
          break;
        }
        
      }
      
      return;
    }
    ctx.fireUserEventTriggered(evt);
  }
  
  Http2StreamChannel newOutboundStream()
  {
    return new Http2MultiplexHandlerStreamChannel((Http2FrameCodec.DefaultHttp2FrameStream)newStream(), null);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) throws Exception
  {
    if ((cause instanceof Http2FrameStreamException)) {
      Http2FrameStreamException exception = (Http2FrameStreamException)cause;
      Http2FrameStream stream = exception.stream();
      AbstractHttp2StreamChannel childChannel = (AbstractHttp2StreamChannel)attachment;
      try
      {
        childChannel.pipeline().fireExceptionCaught(cause.getCause());
      } finally {
        childChannel.unsafe().closeForcibly();
      }
      return;
    }
    if ((cause.getCause() instanceof SSLException)) {
      forEachActiveStream(new Http2FrameStreamVisitor()
      {
        public boolean visit(Http2FrameStream stream) {
          AbstractHttp2StreamChannel childChannel = (AbstractHttp2StreamChannel)attachment;
          
          childChannel.pipeline().fireExceptionCaught(cause);
          return true;
        }
      });
    }
    ctx.fireExceptionCaught(cause);
  }
  
  private static boolean isServer(ChannelHandlerContext ctx) {
    return ctx.channel().parent() instanceof ServerChannel;
  }
  
  private void onHttp2GoAwayFrame(ChannelHandlerContext ctx, final Http2GoAwayFrame goAwayFrame) {
    if (goAwayFrame.lastStreamId() == Integer.MAX_VALUE)
    {
      return;
    }
    try
    {
      final boolean server = isServer(ctx);
      forEachActiveStream(new Http2FrameStreamVisitor()
      {
        public boolean visit(Http2FrameStream stream) {
          int streamId = stream.id();
          if ((streamId > goAwayFrame.lastStreamId()) && (Http2CodecUtil.isStreamIdValid(streamId, server))) {
            AbstractHttp2StreamChannel childChannel = (AbstractHttp2StreamChannel)attachment;
            
            childChannel.pipeline().fireUserEventTriggered(goAwayFrame.retainedDuplicate());
          }
          return true;
        }
      });
    } catch (Http2Exception e) {
      ctx.fireExceptionCaught(e);
      ctx.close();
    }
  }
  


  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    processPendingReadCompleteQueue();
    ctx.fireChannelReadComplete();
  }
  
  private void processPendingReadCompleteQueue() {
    parentReadInProgress = true;
    


    AbstractHttp2StreamChannel childChannel = (AbstractHttp2StreamChannel)readCompletePendingQueue.poll();
    if (childChannel != null) {
      try {
        do {
          childChannel.fireChildReadComplete();
          childChannel = (AbstractHttp2StreamChannel)readCompletePendingQueue.poll();
        } while (childChannel != null);
        
        parentReadInProgress = false;
        readCompletePendingQueue.clear();
        ctx.flush();
      }
      finally
      {
        parentReadInProgress = false;
        readCompletePendingQueue.clear();
        ctx.flush();
      }
    } else {
      parentReadInProgress = false;
    }
  }
  
  private final class Http2MultiplexHandlerStreamChannel extends AbstractHttp2StreamChannel
  {
    Http2MultiplexHandlerStreamChannel(Http2FrameCodec.DefaultHttp2FrameStream stream, ChannelHandler inboundHandler) {
      super(Http2MultiplexHandler.access$004(Http2MultiplexHandler.this), inboundHandler);
    }
    
    protected boolean isParentReadInProgress()
    {
      return parentReadInProgress;
    }
    


    protected void addChannelToReadCompletePendingQueue()
    {
      while (!readCompletePendingQueue.offer(this)) {
        Http2MultiplexHandler.this.processPendingReadCompleteQueue();
      }
    }
    
    protected ChannelHandlerContext parentContext()
    {
      return ctx;
    }
  }
}
