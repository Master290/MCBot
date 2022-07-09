package io.netty.handler.codec.http2;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import java.util.ArrayDeque;
import java.util.Queue;







































































@Deprecated
public class Http2MultiplexCodec
  extends Http2FrameCodec
{
  private final ChannelHandler inboundStreamHandler;
  private final ChannelHandler upgradeStreamHandler;
  private final Queue<AbstractHttp2StreamChannel> readCompletePendingQueue = new MaxCapacityQueue(new ArrayDeque(8), 100);
  


  private boolean parentReadInProgress;
  

  private int idCount;
  

  volatile ChannelHandlerContext ctx;
  


  Http2MultiplexCodec(Http2ConnectionEncoder encoder, Http2ConnectionDecoder decoder, Http2Settings initialSettings, ChannelHandler inboundStreamHandler, ChannelHandler upgradeStreamHandler, boolean decoupleCloseAndGoAway)
  {
    super(encoder, decoder, initialSettings, decoupleCloseAndGoAway);
    this.inboundStreamHandler = inboundStreamHandler;
    this.upgradeStreamHandler = upgradeStreamHandler;
  }
  
  public void onHttpClientUpgrade()
    throws Http2Exception
  {
    if (upgradeStreamHandler == null) {
      throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, "Client is misconfigured for upgrade requests", new Object[0]);
    }
    
    super.onHttpClientUpgrade();
  }
  
  public final void handlerAdded0(ChannelHandlerContext ctx) throws Exception
  {
    if (ctx.executor() != ctx.channel().eventLoop()) {
      throw new IllegalStateException("EventExecutor must be EventLoop of Channel");
    }
    this.ctx = ctx;
  }
  
  public final void handlerRemoved0(ChannelHandlerContext ctx) throws Exception
  {
    super.handlerRemoved0(ctx);
    
    readCompletePendingQueue.clear();
  }
  
  final void onHttp2Frame(ChannelHandlerContext ctx, Http2Frame frame)
  {
    if ((frame instanceof Http2StreamFrame)) {
      Http2StreamFrame streamFrame = (Http2StreamFrame)frame;
      
      AbstractHttp2StreamChannel channel = (AbstractHttp2StreamChannel)streamattachment;
      channel.fireChildRead(streamFrame);
      return;
    }
    if ((frame instanceof Http2GoAwayFrame)) {
      onHttp2GoAwayFrame(ctx, (Http2GoAwayFrame)frame);
    }
    
    ctx.fireChannelRead(frame);
  }
  
  final void onHttp2StreamStateChanged(ChannelHandlerContext ctx, Http2FrameCodec.DefaultHttp2FrameStream stream)
  {
    switch (2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()]) {
    case 1: 
      if (stream.id() != 1) {
        break;
      }
    


    case 2: 
    case 3: 
      if (attachment == null)
      {
        Http2MultiplexCodecStreamChannel streamChannel;
        


        if ((stream.id() == 1) && (!connection().isServer()))
        {

          assert (upgradeStreamHandler != null);
          Http2MultiplexCodecStreamChannel streamChannel = new Http2MultiplexCodecStreamChannel(stream, upgradeStreamHandler);
          streamChannel.closeOutbound();
        } else {
          streamChannel = new Http2MultiplexCodecStreamChannel(stream, inboundStreamHandler);
        }
        ChannelFuture future = ctx.channel().eventLoop().register(streamChannel);
        if (future.isDone()) {
          Http2MultiplexHandler.registerDone(future);
        } else
          future.addListener(Http2MultiplexHandler.CHILD_CHANNEL_REGISTRATION_LISTENER);
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
  

  final Http2StreamChannel newOutboundStream()
  {
    return new Http2MultiplexCodecStreamChannel(newStream(), null);
  }
  
  final void onHttp2FrameStreamException(ChannelHandlerContext ctx, Http2FrameStreamException cause)
  {
    Http2FrameStream stream = cause.stream();
    AbstractHttp2StreamChannel channel = (AbstractHttp2StreamChannel)attachment;
    try
    {
      channel.pipeline().fireExceptionCaught(cause.getCause());
    } finally {
      channel.unsafe().closeForcibly();
    }
  }
  
  private void onHttp2GoAwayFrame(ChannelHandlerContext ctx, final Http2GoAwayFrame goAwayFrame) {
    if (goAwayFrame.lastStreamId() == Integer.MAX_VALUE)
    {
      return;
    }
    try
    {
      forEachActiveStream(new Http2FrameStreamVisitor()
      {
        public boolean visit(Http2FrameStream stream) {
          int streamId = stream.id();
          AbstractHttp2StreamChannel channel = (AbstractHttp2StreamChannel)attachment;
          
          if ((streamId > goAwayFrame.lastStreamId()) && (connection().local().isValidStreamId(streamId))) {
            channel.pipeline().fireUserEventTriggered(goAwayFrame.retainedDuplicate());
          }
          return true;
        }
      });
    } catch (Http2Exception e) {
      ctx.fireExceptionCaught(e);
      ctx.close();
    }
  }
  


  public final void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    processPendingReadCompleteQueue();
    channelReadComplete0(ctx);
  }
  
  private void processPendingReadCompleteQueue() {
    parentReadInProgress = true;
    
    try
    {
      for (;;)
      {
        AbstractHttp2StreamChannel childChannel = (AbstractHttp2StreamChannel)readCompletePendingQueue.poll();
        if (childChannel == null) {
          break;
        }
        childChannel.fireChildReadComplete();
      }
      
      parentReadInProgress = false;
      readCompletePendingQueue.clear();
      
      flush0(ctx);
    }
    finally
    {
      parentReadInProgress = false;
      readCompletePendingQueue.clear();
      
      flush0(ctx);
    }
  }
  
  public final void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    parentReadInProgress = true;
    super.channelRead(ctx, msg);
  }
  
  public final void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    if (ctx.channel().isWritable())
    {

      forEachActiveStream(AbstractHttp2StreamChannel.WRITABLE_VISITOR);
    }
    
    super.channelWritabilityChanged(ctx);
  }
  
  final void flush0(ChannelHandlerContext ctx) {
    flush(ctx);
  }
  
  private final class Http2MultiplexCodecStreamChannel extends AbstractHttp2StreamChannel
  {
    Http2MultiplexCodecStreamChannel(Http2FrameCodec.DefaultHttp2FrameStream stream, ChannelHandler inboundHandler) {
      super(Http2MultiplexCodec.access$004(Http2MultiplexCodec.this), inboundHandler);
    }
    
    protected boolean isParentReadInProgress()
    {
      return parentReadInProgress;
    }
    


    protected void addChannelToReadCompletePendingQueue()
    {
      while (!readCompletePendingQueue.offer(this)) {
        Http2MultiplexCodec.this.processPendingReadCompleteQueue();
      }
    }
    
    protected ChannelHandlerContext parentContext()
    {
      return ctx;
    }
    
    protected ChannelFuture write0(ChannelHandlerContext ctx, Object msg)
    {
      ChannelPromise promise = ctx.newPromise();
      write(ctx, msg, promise);
      return promise;
    }
    
    protected void flush0(ChannelHandlerContext ctx)
    {
      Http2MultiplexCodec.this.flush0(ctx);
    }
  }
}
