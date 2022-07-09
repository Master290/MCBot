package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.PromiseNotifier;
import io.netty.util.internal.ObjectUtil;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.zip.Checksum;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;















































































public class Lz4FrameEncoder
  extends MessageToByteEncoder<ByteBuf>
{
  static final int DEFAULT_MAX_ENCODE_SIZE = Integer.MAX_VALUE;
  private final int blockSize;
  private final LZ4Compressor compressor;
  private final ByteBufChecksum checksum;
  private final int compressionLevel;
  private ByteBuf buffer;
  private final int maxEncodeSize;
  private volatile boolean finished;
  private volatile ChannelHandlerContext ctx;
  
  public Lz4FrameEncoder()
  {
    this(false);
  }
  







  public Lz4FrameEncoder(boolean highCompressor)
  {
    this(LZ4Factory.fastestInstance(), highCompressor, 65536, new Lz4XXHash32(-1756908916));
  }
  











  public Lz4FrameEncoder(LZ4Factory factory, boolean highCompressor, int blockSize, Checksum checksum)
  {
    this(factory, highCompressor, blockSize, checksum, Integer.MAX_VALUE);
  }
  













  public Lz4FrameEncoder(LZ4Factory factory, boolean highCompressor, int blockSize, Checksum checksum, int maxEncodeSize)
  {
    ObjectUtil.checkNotNull(factory, "factory");
    ObjectUtil.checkNotNull(checksum, "checksum");
    
    compressor = (highCompressor ? factory.highCompressor() : factory.fastCompressor());
    this.checksum = ByteBufChecksum.wrapChecksum(checksum);
    
    compressionLevel = compressionLevel(blockSize);
    this.blockSize = blockSize;
    this.maxEncodeSize = ObjectUtil.checkPositive(maxEncodeSize, "maxEncodeSize");
    finished = false;
  }
  


  private static int compressionLevel(int blockSize)
  {
    if ((blockSize < 64) || (blockSize > 33554432)) {
      throw new IllegalArgumentException(String.format("blockSize: %d (expected: %d-%d)", new Object[] {
        Integer.valueOf(blockSize), Integer.valueOf(64), Integer.valueOf(33554432) }));
    }
    int compressionLevel = 32 - Integer.numberOfLeadingZeros(blockSize - 1);
    compressionLevel = Math.max(0, compressionLevel - 10);
    return compressionLevel;
  }
  
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
  {
    return allocateBuffer(ctx, msg, preferDirect, true);
  }
  
  private ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect, boolean allowEmptyReturn)
  {
    int targetBufSize = 0;
    int remaining = msg.readableBytes() + buffer.readableBytes();
    

    if (remaining < 0) {
      throw new EncoderException("too much data to allocate a buffer for compression");
    }
    
    while (remaining > 0) {
      int curSize = Math.min(blockSize, remaining);
      remaining -= curSize;
      
      targetBufSize += compressor.maxCompressedLength(curSize) + 21;
    }
    



    if ((targetBufSize > maxEncodeSize) || (0 > targetBufSize)) {
      throw new EncoderException(String.format("requested encode buffer size (%d bytes) exceeds the maximum allowable size (%d bytes)", new Object[] {
        Integer.valueOf(targetBufSize), Integer.valueOf(maxEncodeSize) }));
    }
    
    if ((allowEmptyReturn) && (targetBufSize < blockSize)) {
      return Unpooled.EMPTY_BUFFER;
    }
    
    if (preferDirect) {
      return ctx.alloc().ioBuffer(targetBufSize, targetBufSize);
    }
    return ctx.alloc().heapBuffer(targetBufSize, targetBufSize);
  }
  







  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out)
    throws Exception
  {
    if (finished) {
      if (!out.isWritable(in.readableBytes()))
      {
        throw new IllegalStateException("encode finished and not enough space to write remaining data");
      }
      out.writeBytes(in);
      return;
    }
    
    ByteBuf buffer = this.buffer;
    int length;
    while ((length = in.readableBytes()) > 0) {
      int nextChunkSize = Math.min(length, buffer.writableBytes());
      in.readBytes(buffer, nextChunkSize);
      
      if (!buffer.isWritable()) {
        flushBufferedData(out);
      }
    }
  }
  
  private void flushBufferedData(ByteBuf out) {
    int flushableBytes = buffer.readableBytes();
    if (flushableBytes == 0) {
      return;
    }
    checksum.reset();
    checksum.update(buffer, buffer.readerIndex(), flushableBytes);
    int check = (int)checksum.getValue();
    
    int bufSize = compressor.maxCompressedLength(flushableBytes) + 21;
    out.ensureWritable(bufSize);
    int idx = out.writerIndex();
    try
    {
      ByteBuffer outNioBuffer = out.internalNioBuffer(idx + 21, out.writableBytes() - 21);
      int pos = outNioBuffer.position();
      
      compressor.compress(buffer.internalNioBuffer(buffer.readerIndex(), flushableBytes), outNioBuffer);
      compressedLength = outNioBuffer.position() - pos;
    } catch (LZ4Exception e) { int compressedLength;
      throw new CompressionException(e); }
    int compressedLength;
    int blockType;
    if (compressedLength >= flushableBytes) {
      int blockType = 16;
      compressedLength = flushableBytes;
      out.setBytes(idx + 21, buffer, 0, flushableBytes);
    } else {
      blockType = 32;
    }
    
    out.setLong(idx, 5501767354678207339L);
    out.setByte(idx + 8, (byte)(blockType | compressionLevel));
    out.setIntLE(idx + 9, compressedLength);
    out.setIntLE(idx + 13, flushableBytes);
    out.setIntLE(idx + 17, check);
    out.writerIndex(idx + 21 + compressedLength);
    buffer.clear();
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    if ((buffer != null) && (buffer.isReadable())) {
      ByteBuf buf = allocateBuffer(ctx, Unpooled.EMPTY_BUFFER, isPreferDirect(), false);
      flushBufferedData(buf);
      ctx.write(buf);
    }
    ctx.flush();
  }
  
  private ChannelFuture finishEncode(ChannelHandlerContext ctx, ChannelPromise promise) {
    if (finished) {
      promise.setSuccess();
      return promise;
    }
    finished = true;
    
    ByteBuf footer = ctx.alloc().heapBuffer(compressor
      .maxCompressedLength(buffer.readableBytes()) + 21);
    flushBufferedData(footer);
    
    footer.ensureWritable(21);
    int idx = footer.writerIndex();
    footer.setLong(idx, 5501767354678207339L);
    footer.setByte(idx + 8, (byte)(0x10 | compressionLevel));
    footer.setInt(idx + 9, 0);
    footer.setInt(idx + 13, 0);
    footer.setInt(idx + 17, 0);
    
    footer.writerIndex(idx + 21);
    
    return ctx.writeAndFlush(footer, promise);
  }
  


  public boolean isClosed()
  {
    return finished;
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
    executor.execute(new Runnable()
    {
      public void run() {
        ChannelFuture f = Lz4FrameEncoder.this.finishEncode(Lz4FrameEncoder.access$000(Lz4FrameEncoder.this), promise);
        PromiseNotifier.cascade(f, promise);
      }
    });
    return promise;
  }
  
  public void close(final ChannelHandlerContext ctx, final ChannelPromise promise)
    throws Exception
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
  

  private ChannelHandlerContext ctx()
  {
    ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      throw new IllegalStateException("not added to a pipeline");
    }
    return ctx;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
  {
    this.ctx = ctx;
    
    buffer = Unpooled.wrappedBuffer(new byte[blockSize]);
    buffer.clear();
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    super.handlerRemoved(ctx);
    if (buffer != null) {
      buffer.release();
      buffer = null;
    }
  }
  
  final ByteBuf getBackingBuffer() {
    return buffer;
  }
}
