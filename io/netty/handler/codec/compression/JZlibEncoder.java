package io.netty.handler.codec.compression;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.JZlib;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.PromiseNotifier;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.TimeUnit;


















public class JZlibEncoder
  extends ZlibEncoder
{
  private final int wrapperOverhead;
  private final Deflater z = new Deflater();
  

  private volatile boolean finished;
  

  private volatile ChannelHandlerContext ctx;
  


  public JZlibEncoder()
  {
    this(6);
  }
  











  public JZlibEncoder(int compressionLevel)
  {
    this(ZlibWrapper.ZLIB, compressionLevel);
  }
  






  public JZlibEncoder(ZlibWrapper wrapper)
  {
    this(wrapper, 6);
  }
  











  public JZlibEncoder(ZlibWrapper wrapper, int compressionLevel)
  {
    this(wrapper, compressionLevel, 15, 8);
  }
  






















  public JZlibEncoder(ZlibWrapper wrapper, int compressionLevel, int windowBits, int memLevel)
  {
    if ((compressionLevel < 0) || (compressionLevel > 9)) {
      throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
    }
    

    if ((windowBits < 9) || (windowBits > 15)) {
      throw new IllegalArgumentException("windowBits: " + windowBits + " (expected: 9-15)");
    }
    
    if ((memLevel < 1) || (memLevel > 9)) {
      throw new IllegalArgumentException("memLevel: " + memLevel + " (expected: 1-9)");
    }
    
    ObjectUtil.checkNotNull(wrapper, "wrapper");
    if (wrapper == ZlibWrapper.ZLIB_OR_NONE) {
      throw new IllegalArgumentException("wrapper '" + ZlibWrapper.ZLIB_OR_NONE + "' is not allowed for compression.");
    }
    


    int resultCode = z.init(compressionLevel, windowBits, memLevel, 
    
      ZlibUtil.convertWrapperType(wrapper));
    if (resultCode != 0) {
      ZlibUtil.fail(z, "initialization failure", resultCode);
    }
    
    wrapperOverhead = ZlibUtil.wrapperOverhead(wrapper);
  }
  










  public JZlibEncoder(byte[] dictionary)
  {
    this(6, dictionary);
  }
  














  public JZlibEncoder(int compressionLevel, byte[] dictionary)
  {
    this(compressionLevel, 15, 8, dictionary);
  }
  
























  public JZlibEncoder(int compressionLevel, int windowBits, int memLevel, byte[] dictionary)
  {
    if ((compressionLevel < 0) || (compressionLevel > 9)) {
      throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
    }
    if ((windowBits < 9) || (windowBits > 15)) {
      throw new IllegalArgumentException("windowBits: " + windowBits + " (expected: 9-15)");
    }
    
    if ((memLevel < 1) || (memLevel > 9)) {
      throw new IllegalArgumentException("memLevel: " + memLevel + " (expected: 1-9)");
    }
    
    ObjectUtil.checkNotNull(dictionary, "dictionary");
    

    int resultCode = z.deflateInit(compressionLevel, windowBits, memLevel, JZlib.W_ZLIB);
    

    if (resultCode != 0) {
      ZlibUtil.fail(z, "initialization failure", resultCode);
    } else {
      resultCode = z.deflateSetDictionary(dictionary, dictionary.length);
      if (resultCode != 0) {
        ZlibUtil.fail(z, "failed to set the dictionary", resultCode);
      }
    }
    
    wrapperOverhead = ZlibUtil.wrapperOverhead(ZlibWrapper.ZLIB);
  }
  
  public ChannelFuture close()
  {
    return close(ctx().channel().newPromise());
  }
  
  public ChannelFuture close(final ChannelPromise promise)
  {
    ChannelHandlerContext ctx = ctx();
    EventExecutor executor = ctx.executor();
    if (executor.inEventLoop()) {
      return finishEncode(ctx, promise);
    }
    final ChannelPromise p = ctx.newPromise();
    executor.execute(new Runnable()
    {
      public void run() {
        ChannelFuture f = JZlibEncoder.this.finishEncode(JZlibEncoder.access$000(JZlibEncoder.this), p);
        PromiseNotifier.cascade(f, promise);
      }
    });
    return p;
  }
  
  private ChannelHandlerContext ctx()
  {
    ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      throw new IllegalStateException("not added to a pipeline");
    }
    return ctx;
  }
  
  public boolean isClosed()
  {
    return finished;
  }
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception
  {
    if (finished) {
      out.writeBytes(in);
      return;
    }
    
    int inputLength = in.readableBytes();
    if (inputLength == 0) {
      return;
    }
    
    try
    {
      boolean inHasArray = in.hasArray();
      z.avail_in = inputLength;
      if (inHasArray) {
        z.next_in = in.array();
        z.next_in_index = (in.arrayOffset() + in.readerIndex());
      } else {
        byte[] array = new byte[inputLength];
        in.getBytes(in.readerIndex(), array);
        z.next_in = array;
        z.next_in_index = 0;
      }
      int oldNextInIndex = z.next_in_index;
      

      int maxOutputLength = (int)Math.ceil(inputLength * 1.001D) + 12 + wrapperOverhead;
      out.ensureWritable(maxOutputLength);
      z.avail_out = maxOutputLength;
      z.next_out = out.array();
      z.next_out_index = (out.arrayOffset() + out.writerIndex());
      int oldNextOutIndex = z.next_out_index;
      

      try
      {
        resultCode = z.deflate(2);
      } finally { int resultCode;
        in.skipBytes(z.next_in_index - oldNextInIndex);
      }
      int resultCode;
      if (resultCode != 0) {
        ZlibUtil.fail(z, "compression failure", resultCode);
      }
      
      int outputLength = z.next_out_index - oldNextOutIndex;
      if (outputLength > 0) {
        out.writerIndex(out.writerIndex() + outputLength);
      }
      

    }
    finally
    {
      z.next_in = null;
      z.next_out = null;
    }
  }
  


  public void close(final ChannelHandlerContext ctx, final ChannelPromise promise)
  {
    ChannelFuture f = finishEncode(ctx, ctx.newPromise());
    f.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture f) throws Exception {
        ctx.close(promise);
      }
    });
    
    if (!f.isDone())
    {
      ctx.executor().schedule(new Runnable()
      {

        public void run() { ctx.close(promise); } }, 10L, TimeUnit.SECONDS);
    }
  }
  

  private ChannelFuture finishEncode(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    if (finished) {
      promise.setSuccess();
      return promise;
    }
    finished = true;
    

    try
    {
      z.next_in = EmptyArrays.EMPTY_BYTES;
      z.next_in_index = 0;
      z.avail_in = 0;
      

      byte[] out = new byte[32];
      z.next_out = out;
      z.next_out_index = 0;
      z.avail_out = out.length;
      

      int resultCode = z.deflate(4);
      if ((resultCode != 0) && (resultCode != 1)) {
        promise.setFailure(ZlibUtil.deflaterException(z, "compression failure", resultCode));
        return promise; }
      ByteBuf footer; if (z.next_out_index != 0)
      {

        footer = Unpooled.wrappedBuffer(out, 0, z.next_out_index);
      } else
        footer = Unpooled.EMPTY_BUFFER;
    } finally {
      ByteBuf footer;
      z.deflateEnd();
      




      z.next_in = null;
      z.next_out = null; }
    ByteBuf footer;
    return ctx.writeAndFlush(footer, promise);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
  }
}
