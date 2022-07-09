package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.PromiseNotifier;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Deflater;






















public class JdkZlibEncoder
  extends ZlibEncoder
{
  private final ZlibWrapper wrapper;
  private final Deflater deflater;
  private volatile boolean finished;
  private volatile ChannelHandlerContext ctx;
  private final CRC32 crc = new CRC32();
  private static final byte[] gzipHeader = { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0 };
  private boolean writeHeader = true;
  





  public JdkZlibEncoder()
  {
    this(6);
  }
  










  public JdkZlibEncoder(int compressionLevel)
  {
    this(ZlibWrapper.ZLIB, compressionLevel);
  }
  





  public JdkZlibEncoder(ZlibWrapper wrapper)
  {
    this(wrapper, 6);
  }
  










  public JdkZlibEncoder(ZlibWrapper wrapper, int compressionLevel)
  {
    if ((compressionLevel < 0) || (compressionLevel > 9)) {
      throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
    }
    
    ObjectUtil.checkNotNull(wrapper, "wrapper");
    if (wrapper == ZlibWrapper.ZLIB_OR_NONE) {
      throw new IllegalArgumentException("wrapper '" + ZlibWrapper.ZLIB_OR_NONE + "' is not allowed for compression.");
    }
    


    this.wrapper = wrapper;
    deflater = new Deflater(compressionLevel, wrapper != ZlibWrapper.ZLIB);
  }
  









  public JdkZlibEncoder(byte[] dictionary)
  {
    this(6, dictionary);
  }
  













  public JdkZlibEncoder(int compressionLevel, byte[] dictionary)
  {
    if ((compressionLevel < 0) || (compressionLevel > 9)) {
      throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
    }
    
    ObjectUtil.checkNotNull(dictionary, "dictionary");
    
    wrapper = ZlibWrapper.ZLIB;
    deflater = new Deflater(compressionLevel);
    deflater.setDictionary(dictionary);
  }
  
  public ChannelFuture close()
  {
    return close(ctx().newPromise());
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
        ChannelFuture f = JdkZlibEncoder.this.finishEncode(JdkZlibEncoder.access$000(JdkZlibEncoder.this), p);
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
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf uncompressed, ByteBuf out) throws Exception
  {
    if (finished) {
      out.writeBytes(uncompressed);
      return;
    }
    
    int len = uncompressed.readableBytes();
    if (len == 0) {
      return;
    }
    
    byte[] inAry;
    int offset;
    if (uncompressed.hasArray())
    {
      byte[] inAry = uncompressed.array();
      int offset = uncompressed.arrayOffset() + uncompressed.readerIndex();
      
      uncompressed.skipBytes(len);
    } else {
      inAry = new byte[len];
      uncompressed.readBytes(inAry);
      offset = 0;
    }
    
    if (writeHeader) {
      writeHeader = false;
      if (wrapper == ZlibWrapper.GZIP) {
        out.writeBytes(gzipHeader);
      }
    }
    
    if (wrapper == ZlibWrapper.GZIP) {
      crc.update(inAry, offset, len);
    }
    
    deflater.setInput(inAry, offset, len);
    for (;;) {
      deflate(out);
      if (deflater.needsInput()) {
        break;
      }
      
      if (!out.isWritable())
      {

        out.ensureWritable(out.writerIndex());
      }
    }
  }
  

  protected final ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
    throws Exception
  {
    int sizeEstimate = (int)Math.ceil(msg.readableBytes() * 1.001D) + 12;
    if (writeHeader) {
      switch (4.$SwitchMap$io$netty$handler$codec$compression$ZlibWrapper[wrapper.ordinal()]) {
      case 1: 
        sizeEstimate += gzipHeader.length;
        break;
      case 2: 
        sizeEstimate += 2;
        break;
      }
      
    }
    
    return ctx.alloc().heapBuffer(sizeEstimate);
  }
  
  public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception
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
    ByteBuf footer = ctx.alloc().heapBuffer();
    if ((writeHeader) && (wrapper == ZlibWrapper.GZIP))
    {
      writeHeader = false;
      footer.writeBytes(gzipHeader);
    }
    
    deflater.finish();
    
    while (!deflater.finished()) {
      deflate(footer);
      if (!footer.isWritable())
      {
        ctx.write(footer);
        footer = ctx.alloc().heapBuffer();
      }
    }
    if (wrapper == ZlibWrapper.GZIP) {
      int crcValue = (int)crc.getValue();
      int uncBytes = deflater.getTotalIn();
      footer.writeByte(crcValue);
      footer.writeByte(crcValue >>> 8);
      footer.writeByte(crcValue >>> 16);
      footer.writeByte(crcValue >>> 24);
      footer.writeByte(uncBytes);
      footer.writeByte(uncBytes >>> 8);
      footer.writeByte(uncBytes >>> 16);
      footer.writeByte(uncBytes >>> 24);
    }
    deflater.end();
    return ctx.writeAndFlush(footer, promise);
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  private void deflate(ByteBuf out) {
    if (PlatformDependent.javaVersion() < 7) {
      deflateJdk6(out);
    }
    int numBytes;
    do {
      int writerIndex = out.writerIndex();
      numBytes = deflater.deflate(out
        .array(), out.arrayOffset() + writerIndex, out.writableBytes(), 2);
      out.writerIndex(writerIndex + numBytes);
    } while (numBytes > 0);
  }
  
  private void deflateJdk6(ByteBuf out) {
    int numBytes;
    do {
      int writerIndex = out.writerIndex();
      numBytes = deflater.deflate(out
        .array(), out.arrayOffset() + writerIndex, out.writableBytes());
      out.writerIndex(writerIndex + numBytes);
    } while (numBytes > 0);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
  }
}
