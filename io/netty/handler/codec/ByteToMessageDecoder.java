package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.util.List;






























































public abstract class ByteToMessageDecoder
  extends ChannelInboundHandlerAdapter
{
  public static final Cumulator MERGE_CUMULATOR = new Cumulator()
  {
    public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
      if ((!cumulation.isReadable()) && (in.isContiguous()))
      {
        cumulation.release();
        return in;
      }
      try {
        int required = in.readableBytes();
        ByteBuf localByteBuf; if ((required > cumulation.maxWritableBytes()) || 
          ((required > cumulation.maxFastWritableBytes()) && (cumulation.refCnt() > 1)) || 
          (cumulation.isReadOnly()))
        {



          return ByteToMessageDecoder.expandCumulation(alloc, cumulation, in);
        }
        cumulation.writeBytes(in, in.readerIndex(), required);
        in.readerIndex(in.writerIndex());
        return cumulation;
      }
      finally
      {
        in.release();
      }
    }
  };
  





  public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator()
  {
    public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
      if (!cumulation.isReadable()) {
        cumulation.release();
        return in;
      }
      CompositeByteBuf composite = null;
      try {
        if (((cumulation instanceof CompositeByteBuf)) && (cumulation.refCnt() == 1)) {
          composite = (CompositeByteBuf)cumulation;
          

          if (composite.writerIndex() != composite.capacity()) {
            composite.capacity(composite.writerIndex());
          }
        } else {
          composite = alloc.compositeBuffer(Integer.MAX_VALUE).addFlattenedComponents(true, cumulation);
        }
        composite.addFlattenedComponents(true, in);
        in = null;
        return composite;
      } finally {
        if (in != null)
        {
          in.release();
          
          if ((composite != null) && (composite != cumulation)) {
            composite.release();
          }
        }
      }
    }
  };
  
  private static final byte STATE_INIT = 0;
  
  private static final byte STATE_CALLING_CHILD_DECODE = 1;
  private static final byte STATE_HANDLER_REMOVED_PENDING = 2;
  ByteBuf cumulation;
  private Cumulator cumulator = MERGE_CUMULATOR;
  



  private boolean singleDecode;
  



  private boolean first;
  


  private boolean firedChannelRead;
  


  private byte decodeState = 0;
  private int discardAfterReads = 16;
  private int numReads;
  
  protected ByteToMessageDecoder() {
    ensureNotSharable();
  }
  





  public void setSingleDecode(boolean singleDecode)
  {
    this.singleDecode = singleDecode;
  }
  





  public boolean isSingleDecode()
  {
    return singleDecode;
  }
  


  public void setCumulator(Cumulator cumulator)
  {
    this.cumulator = ((Cumulator)ObjectUtil.checkNotNull(cumulator, "cumulator"));
  }
  



  public void setDiscardAfterReads(int discardAfterReads)
  {
    ObjectUtil.checkPositive(discardAfterReads, "discardAfterReads");
    this.discardAfterReads = discardAfterReads;
  }
  





  protected int actualReadableBytes()
  {
    return internalBuffer().readableBytes();
  }
  




  protected ByteBuf internalBuffer()
  {
    if (cumulation != null) {
      return cumulation;
    }
    return Unpooled.EMPTY_BUFFER;
  }
  
  public final void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    if (decodeState == 1) {
      decodeState = 2;
      return;
    }
    ByteBuf buf = cumulation;
    if (buf != null)
    {
      cumulation = null;
      numReads = 0;
      int readable = buf.readableBytes();
      if (readable > 0) {
        ctx.fireChannelRead(buf);
        ctx.fireChannelReadComplete();
      } else {
        buf.release();
      }
    }
    handlerRemoved0(ctx);
  }
  

  protected void handlerRemoved0(ChannelHandlerContext ctx)
    throws Exception
  {}
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    if ((msg instanceof ByteBuf)) {
      CodecOutputList out = CodecOutputList.newInstance();
      try {
        first = (cumulation == null);
        cumulation = cumulator.cumulate(ctx.alloc(), first ? Unpooled.EMPTY_BUFFER : cumulation, (ByteBuf)msg);
        
        callDecode(ctx, cumulation, out);
      } catch (DecoderException e) { int size;
        throw e;
      } catch (Exception e) {
        throw new DecoderException(e);
      } finally {
        try {
          if ((cumulation != null) && (!cumulation.isReadable())) {
            numReads = 0;
            cumulation.release();
            cumulation = null;
          } else if (++numReads >= discardAfterReads)
          {

            numReads = 0;
            discardSomeReadBytes();
          }
          
          int size = out.size();
          firedChannelRead |= out.insertSinceRecycled();
          fireChannelRead(ctx, out, size);
        } finally {
          out.recycle();
        }
      }
    } else {
      ctx.fireChannelRead(msg);
    }
  }
  


  static void fireChannelRead(ChannelHandlerContext ctx, List<Object> msgs, int numElements)
  {
    if ((msgs instanceof CodecOutputList)) {
      fireChannelRead(ctx, (CodecOutputList)msgs, numElements);
    } else {
      for (int i = 0; i < numElements; i++) {
        ctx.fireChannelRead(msgs.get(i));
      }
    }
  }
  


  static void fireChannelRead(ChannelHandlerContext ctx, CodecOutputList msgs, int numElements)
  {
    for (int i = 0; i < numElements; i++) {
      ctx.fireChannelRead(msgs.getUnsafe(i));
    }
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    numReads = 0;
    discardSomeReadBytes();
    if ((!firedChannelRead) && (!ctx.channel().config().isAutoRead())) {
      ctx.read();
    }
    firedChannelRead = false;
    ctx.fireChannelReadComplete();
  }
  
  protected final void discardSomeReadBytes() {
    if ((cumulation != null) && (!first) && (cumulation.refCnt() == 1))
    {






      cumulation.discardSomeReadBytes();
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    channelInputClosed(ctx, true);
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    if ((evt instanceof ChannelInputShutdownEvent))
    {


      channelInputClosed(ctx, false);
    }
    super.userEventTriggered(ctx, evt);
  }
  
  private void channelInputClosed(ChannelHandlerContext ctx, boolean callChannelInactive) {
    CodecOutputList out = CodecOutputList.newInstance();
    try {
      channelInputClosed(ctx, out);
    } catch (DecoderException e) { int size;
      throw e;
    } catch (Exception e) {
      throw new DecoderException(e);
    } finally {
      try {
        if (cumulation != null) {
          cumulation.release();
          cumulation = null;
        }
        int size = out.size();
        fireChannelRead(ctx, out, size);
        if (size > 0)
        {
          ctx.fireChannelReadComplete();
        }
        if (callChannelInactive) {
          ctx.fireChannelInactive();
        }
      }
      finally {
        out.recycle();
      }
    }
  }
  


  void channelInputClosed(ChannelHandlerContext ctx, List<Object> out)
    throws Exception
  {
    if (cumulation != null) {
      callDecode(ctx, cumulation, out);
      

      if (!ctx.isRemoved())
      {

        ByteBuf buffer = cumulation == null ? Unpooled.EMPTY_BUFFER : cumulation;
        decodeLast(ctx, buffer, out);
      }
    } else {
      decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
    }
  }
  






  protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
  {
    try
    {
      while (in.isReadable()) {
        int outSize = out.size();
        
        if (outSize > 0) {
          fireChannelRead(ctx, out, outSize);
          out.clear();
          





          if (ctx.isRemoved()) {
            break;
          }
        }
        else {
          int oldInputLength = in.readableBytes();
          decodeRemovalReentryProtection(ctx, in, out);
          




          if (!ctx.isRemoved())
          {


            if (out.isEmpty()) {
              if (oldInputLength != in.readableBytes()) {
                continue;
              }
              
            }
            else
            {
              if (oldInputLength == in.readableBytes())
              {
                throw new DecoderException(StringUtil.simpleClassName(getClass()) + ".decode() did not read anything but decoded a message.");
              }
              

              if (isSingleDecode())
                break;
            } }
        }
      }
    } catch (DecoderException e) { throw e;
    } catch (Exception cause) {
      throw new DecoderException(cause);
    }
  }
  









  protected abstract void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList)
    throws Exception;
  









  final void decodeRemovalReentryProtection(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    decodeState = 1;
    try {
      decode(ctx, in, out);
    } finally { boolean removePending;
      boolean removePending = decodeState == 2;
      decodeState = 0;
      if (removePending) {
        fireChannelRead(ctx, out, out.size());
        out.clear();
        handlerRemoved(ctx);
      }
    }
  }
  





  protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    if (in.isReadable())
    {

      decodeRemovalReentryProtection(ctx, in, out);
    }
  }
  
  static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf oldCumulation, ByteBuf in) {
    int oldBytes = oldCumulation.readableBytes();
    int newBytes = in.readableBytes();
    int totalBytes = oldBytes + newBytes;
    ByteBuf newCumulation = alloc.buffer(alloc.calculateNewCapacity(totalBytes, Integer.MAX_VALUE));
    ByteBuf toRelease = newCumulation;
    

    try
    {
      newCumulation.setBytes(0, oldCumulation, oldCumulation.readerIndex(), oldBytes).setBytes(oldBytes, in, in.readerIndex(), newBytes).writerIndex(totalBytes);
      in.readerIndex(in.writerIndex());
      toRelease = oldCumulation;
      return newCumulation;
    } finally {
      toRelease.release();
    }
  }
  
  public static abstract interface Cumulator
  {
    public abstract ByteBuf cumulate(ByteBufAllocator paramByteBufAllocator, ByteBuf paramByteBuf1, ByteBuf paramByteBuf2);
  }
}
