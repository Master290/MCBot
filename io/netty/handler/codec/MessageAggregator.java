package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.List;





































public abstract class MessageAggregator<I, S, C extends ByteBufHolder, O extends ByteBufHolder>
  extends MessageToMessageDecoder<I>
{
  private static final int DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS = 1024;
  private final int maxContentLength;
  private O currentMessage;
  private boolean handlingOversizedMessage;
  private int maxCumulationBufferComponents = 1024;
  

  private ChannelHandlerContext ctx;
  

  private ChannelFutureListener continueResponseWriteListener;
  

  private boolean aggregating;
  


  protected MessageAggregator(int maxContentLength)
  {
    validateMaxContentLength(maxContentLength);
    this.maxContentLength = maxContentLength;
  }
  
  protected MessageAggregator(int maxContentLength, Class<? extends I> inboundMessageType) {
    super(inboundMessageType);
    validateMaxContentLength(maxContentLength);
    this.maxContentLength = maxContentLength;
  }
  
  private static void validateMaxContentLength(int maxContentLength) {
    ObjectUtil.checkPositiveOrZero(maxContentLength, "maxContentLength");
  }
  
  public boolean acceptInboundMessage(Object msg)
    throws Exception
  {
    if (!super.acceptInboundMessage(msg)) {
      return false;
    }
    

    I in = msg;
    
    if (isAggregated(in)) {
      return false;
    }
    


    if (isStartMessage(in)) {
      aggregating = true;
      return true; }
    if ((aggregating) && (isContentMessage(in))) {
      return true;
    }
    
    return false;
  }
  






  protected abstract boolean isStartMessage(I paramI)
    throws Exception;
  






  protected abstract boolean isContentMessage(I paramI)
    throws Exception;
  





  protected abstract boolean isLastContentMessage(C paramC)
    throws Exception;
  





  protected abstract boolean isAggregated(I paramI)
    throws Exception;
  





  public final int maxContentLength()
  {
    return maxContentLength;
  }
  





  public final int maxCumulationBufferComponents()
  {
    return maxCumulationBufferComponents;
  }
  






  public final void setMaxCumulationBufferComponents(int maxCumulationBufferComponents)
  {
    if (maxCumulationBufferComponents < 2) {
      throw new IllegalArgumentException("maxCumulationBufferComponents: " + maxCumulationBufferComponents + " (expected: >= 2)");
    }
    


    if (ctx == null) {
      this.maxCumulationBufferComponents = maxCumulationBufferComponents;
    } else {
      throw new IllegalStateException("decoder properties cannot be changed once the decoder is added to a pipeline.");
    }
  }
  



  @Deprecated
  public final boolean isHandlingOversizedMessage()
  {
    return handlingOversizedMessage;
  }
  
  protected final ChannelHandlerContext ctx() {
    if (ctx == null) {
      throw new IllegalStateException("not added to a pipeline yet");
    }
    return ctx;
  }
  
  protected void decode(final ChannelHandlerContext ctx, I msg, List<Object> out) throws Exception
  {
    assert (aggregating);
    
    if (isStartMessage(msg)) {
      handlingOversizedMessage = false;
      if (currentMessage != null) {
        currentMessage.release();
        currentMessage = null;
        throw new MessageAggregationException();
      }
      

      S m = msg;
      


      Object continueResponse = newContinueResponse(m, maxContentLength, ctx.pipeline());
      if (continueResponse != null)
      {
        ChannelFutureListener listener = continueResponseWriteListener;
        if (listener == null) {
          continueResponseWriteListener = (listener = new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              if (!future.isSuccess()) {
                ctx.fireExceptionCaught(future.cause());
              }
            }
          });
        }
        

        boolean closeAfterWrite = closeAfterContinueResponse(continueResponse);
        handlingOversizedMessage = ignoreContentAfterContinueResponse(continueResponse);
        
        ChannelFuture future = ctx.writeAndFlush(continueResponse).addListener(listener);
        
        if (closeAfterWrite) {
          future.addListener(ChannelFutureListener.CLOSE);
          return;
        }
        if (handlingOversizedMessage) {
          return;
        }
      } else if (isContentLengthInvalid(m, maxContentLength))
      {
        invokeHandleOversizedMessage(ctx, m);
        return;
      }
      
      if (((m instanceof DecoderResultProvider)) && (!((DecoderResultProvider)m).decoderResult().isSuccess())) { O aggregated;
        O aggregated;
        if ((m instanceof ByteBufHolder)) {
          aggregated = beginAggregation(m, ((ByteBufHolder)m).content().retain());
        } else {
          aggregated = beginAggregation(m, Unpooled.EMPTY_BUFFER);
        }
        finishAggregation0(aggregated);
        out.add(aggregated);
        return;
      }
      

      CompositeByteBuf content = ctx.alloc().compositeBuffer(maxCumulationBufferComponents);
      if ((m instanceof ByteBufHolder)) {
        appendPartialContent(content, ((ByteBufHolder)m).content());
      }
      currentMessage = beginAggregation(m, content);
    } else if (isContentMessage(msg)) {
      if (currentMessage == null)
      {

        return;
      }
      

      CompositeByteBuf content = (CompositeByteBuf)currentMessage.content();
      

      C m = (ByteBufHolder)msg;
      
      if (content.readableBytes() > maxContentLength - m.content().readableBytes())
      {

        S s = currentMessage;
        invokeHandleOversizedMessage(ctx, s);
        return;
      }
      

      appendPartialContent(content, m.content());
      

      aggregate(currentMessage, m);
      boolean last;
      boolean last;
      if ((m instanceof DecoderResultProvider)) {
        DecoderResult decoderResult = ((DecoderResultProvider)m).decoderResult();
        boolean last; if (!decoderResult.isSuccess()) {
          if ((currentMessage instanceof DecoderResultProvider)) {
            ((DecoderResultProvider)currentMessage).setDecoderResult(
              DecoderResult.failure(decoderResult.cause()));
          }
          last = true;
        } else {
          last = isLastContentMessage(m);
        }
      } else {
        last = isLastContentMessage(m);
      }
      
      if (last) {
        finishAggregation0(currentMessage);
        

        out.add(currentMessage);
        currentMessage = null;
      }
    } else {
      throw new MessageAggregationException();
    }
  }
  
  private static void appendPartialContent(CompositeByteBuf content, ByteBuf partialContent) {
    if (partialContent.isReadable()) {
      content.addComponent(true, partialContent.retain());
    }
  }
  





  protected abstract boolean isContentLengthInvalid(S paramS, int paramInt)
    throws Exception;
  





  protected abstract Object newContinueResponse(S paramS, int paramInt, ChannelPipeline paramChannelPipeline)
    throws Exception;
  





  protected abstract boolean closeAfterContinueResponse(Object paramObject)
    throws Exception;
  





  protected abstract boolean ignoreContentAfterContinueResponse(Object paramObject)
    throws Exception;
  




  protected abstract O beginAggregation(S paramS, ByteBuf paramByteBuf)
    throws Exception;
  




  protected void aggregate(O aggregated, C content)
    throws Exception
  {}
  




  private void finishAggregation0(O aggregated)
    throws Exception
  {
    aggregating = false;
    finishAggregation(aggregated);
  }
  
  protected void finishAggregation(O aggregated)
    throws Exception
  {}
  
  private void invokeHandleOversizedMessage(ChannelHandlerContext ctx, S oversized) throws Exception
  {
    handlingOversizedMessage = true;
    currentMessage = null;
    try {
      handleOversizedMessage(ctx, oversized);
      

      ReferenceCountUtil.release(oversized); } finally { ReferenceCountUtil.release(oversized);
    }
  }
  





  protected void handleOversizedMessage(ChannelHandlerContext ctx, S oversized)
    throws Exception
  {
    ctx.fireExceptionCaught(new TooLongFrameException("content length exceeded " + 
      maxContentLength() + " bytes."));
  }
  


  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    if ((currentMessage != null) && (!ctx.channel().config().isAutoRead())) {
      ctx.read();
    }
    ctx.fireChannelReadComplete();
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    try
    {
      super.channelInactive(ctx);
      
      releaseCurrentMessage(); } finally { releaseCurrentMessage();
    }
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    try {
      super.handlerRemoved(ctx);
      


      releaseCurrentMessage(); } finally { releaseCurrentMessage();
    }
  }
  
  private void releaseCurrentMessage() {
    if (currentMessage != null) {
      currentMessage.release();
      currentMessage = null;
      handlingOversizedMessage = false;
      aggregating = false;
    }
  }
}
