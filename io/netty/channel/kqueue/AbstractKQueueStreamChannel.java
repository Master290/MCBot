package io.netty.channel.kqueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.unix.IovArray;
import io.netty.channel.unix.SocketWritableByteChannel;
import io.netty.channel.unix.UnixChannelUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;



















public abstract class AbstractKQueueStreamChannel
  extends AbstractKQueueChannel
  implements DuplexChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractKQueueStreamChannel.class);
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  private static final String EXPECTED_TYPES = " (expected: " + 
    StringUtil.simpleClassName(ByteBuf.class) + ", " + 
    StringUtil.simpleClassName(DefaultFileRegion.class) + ')';
  private WritableByteChannel byteChannel;
  private final Runnable flushTask = new Runnable()
  {

    public void run()
    {
      ((AbstractKQueueChannel.AbstractKQueueUnsafe)unsafe()).flush0();
    }
  };
  
  AbstractKQueueStreamChannel(Channel parent, BsdSocket fd, boolean active) {
    super(parent, fd, active);
  }
  
  AbstractKQueueStreamChannel(Channel parent, BsdSocket fd, SocketAddress remote) {
    super(parent, fd, remote);
  }
  
  AbstractKQueueStreamChannel(BsdSocket fd) {
    this(null, fd, isSoErrorZero(fd));
  }
  
  protected AbstractKQueueChannel.AbstractKQueueUnsafe newUnsafe()
  {
    return new KQueueStreamUnsafe();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  












  private int writeBytes(ChannelOutboundBuffer in, ByteBuf buf)
    throws Exception
  {
    int readableBytes = buf.readableBytes();
    if (readableBytes == 0) {
      in.remove();
      return 0;
    }
    
    if ((buf.hasMemoryAddress()) || (buf.nioBufferCount() == 1)) {
      return doWriteBytes(in, buf);
    }
    ByteBuffer[] nioBuffers = buf.nioBuffers();
    return writeBytesMultiple(in, nioBuffers, nioBuffers.length, readableBytes, 
      config().getMaxBytesPerGatheringWrite());
  }
  



  private void adjustMaxBytesPerGatheringWrite(long attempted, long written, long oldMaxBytesPerGatheringWrite)
  {
    if (attempted == written) {
      if (attempted << 1 > oldMaxBytesPerGatheringWrite) {
        config().setMaxBytesPerGatheringWrite(attempted << 1);
      }
    } else if ((attempted > 4096L) && (written < attempted >>> 1)) {
      config().setMaxBytesPerGatheringWrite(attempted >>> 1);
    }
  }
  













  private int writeBytesMultiple(ChannelOutboundBuffer in, IovArray array)
    throws IOException
  {
    long expectedWrittenBytes = array.size();
    assert (expectedWrittenBytes != 0L);
    int cnt = array.count();
    assert (cnt != 0);
    
    long localWrittenBytes = socket.writevAddresses(array.memoryAddress(0), cnt);
    if (localWrittenBytes > 0L) {
      adjustMaxBytesPerGatheringWrite(expectedWrittenBytes, localWrittenBytes, array.maxBytes());
      in.removeBytes(localWrittenBytes);
      return 1;
    }
    return Integer.MAX_VALUE;
  }
  


















  private int writeBytesMultiple(ChannelOutboundBuffer in, ByteBuffer[] nioBuffers, int nioBufferCnt, long expectedWrittenBytes, long maxBytesPerGatheringWrite)
    throws IOException
  {
    assert (expectedWrittenBytes != 0L);
    if (expectedWrittenBytes > maxBytesPerGatheringWrite) {
      expectedWrittenBytes = maxBytesPerGatheringWrite;
    }
    
    long localWrittenBytes = socket.writev(nioBuffers, 0, nioBufferCnt, expectedWrittenBytes);
    if (localWrittenBytes > 0L) {
      adjustMaxBytesPerGatheringWrite(expectedWrittenBytes, localWrittenBytes, maxBytesPerGatheringWrite);
      in.removeBytes(localWrittenBytes);
      return 1;
    }
    return Integer.MAX_VALUE;
  }
  












  private int writeDefaultFileRegion(ChannelOutboundBuffer in, DefaultFileRegion region)
    throws Exception
  {
    long regionCount = region.count();
    long offset = region.transferred();
    
    if (offset >= regionCount) {
      in.remove();
      return 0;
    }
    
    long flushedAmount = socket.sendFile(region, region.position(), offset, regionCount - offset);
    if (flushedAmount > 0L) {
      in.progress(flushedAmount);
      if (region.transferred() >= regionCount) {
        in.remove();
      }
      return 1; }
    if (flushedAmount == 0L) {
      validateFileRegion(region, offset);
    }
    return Integer.MAX_VALUE;
  }
  












  private int writeFileRegion(ChannelOutboundBuffer in, FileRegion region)
    throws Exception
  {
    if (region.transferred() >= region.count()) {
      in.remove();
      return 0;
    }
    
    if (byteChannel == null) {
      byteChannel = new KQueueSocketWritableByteChannel();
    }
    long flushedAmount = region.transferTo(byteChannel, region.transferred());
    if (flushedAmount > 0L) {
      in.progress(flushedAmount);
      if (region.transferred() >= region.count()) {
        in.remove();
      }
      return 1;
    }
    return Integer.MAX_VALUE;
  }
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    int writeSpinCount = config().getWriteSpinCount();
    do {
      int msgCount = in.size();
      
      if ((msgCount > 1) && ((in.current() instanceof ByteBuf))) {
        writeSpinCount -= doWriteMultiple(in);
      } else { if (msgCount == 0)
        {
          writeFilter(false);
          
          return;
        }
        writeSpinCount -= doWriteSingle(in);

      }
      

    }
    while (writeSpinCount > 0);
    
    if (writeSpinCount == 0)
    {



      writeFilter(false);
      

      eventLoop().execute(flushTask);
    }
    else
    {
      writeFilter(true);
    }
  }
  













  protected int doWriteSingle(ChannelOutboundBuffer in)
    throws Exception
  {
    Object msg = in.current();
    if ((msg instanceof ByteBuf))
      return writeBytes(in, (ByteBuf)msg);
    if ((msg instanceof DefaultFileRegion))
      return writeDefaultFileRegion(in, (DefaultFileRegion)msg);
    if ((msg instanceof FileRegion)) {
      return writeFileRegion(in, (FileRegion)msg);
    }
    
    throw new Error();
  }
  













  private int doWriteMultiple(ChannelOutboundBuffer in)
    throws Exception
  {
    long maxBytesPerGatheringWrite = config().getMaxBytesPerGatheringWrite();
    IovArray array = ((KQueueEventLoop)eventLoop()).cleanArray();
    array.maxBytes(maxBytesPerGatheringWrite);
    in.forEachFlushedMessage(array);
    
    if (array.count() >= 1)
    {
      return writeBytesMultiple(in, array);
    }
    
    in.removeBytes(0L);
    return 0;
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof ByteBuf)) {
      ByteBuf buf = (ByteBuf)msg;
      return UnixChannelUtil.isBufferCopyNeededForWrite(buf) ? newDirectBuffer(buf) : buf;
    }
    
    if ((msg instanceof FileRegion)) {
      return msg;
    }
    

    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  protected final void doShutdownOutput()
    throws Exception
  {
    socket.shutdown(false, true);
  }
  
  public boolean isOutputShutdown()
  {
    return socket.isOutputShutdown();
  }
  
  public boolean isInputShutdown()
  {
    return socket.isInputShutdown();
  }
  
  public boolean isShutdown()
  {
    return socket.isShutdown();
  }
  
  public ChannelFuture shutdownOutput()
  {
    return shutdownOutput(newPromise());
  }
  
  public ChannelFuture shutdownOutput(final ChannelPromise promise)
  {
    EventLoop loop = eventLoop();
    if (loop.inEventLoop()) {
      ((AbstractChannel.AbstractUnsafe)unsafe()).shutdownOutput(promise);
    } else {
      loop.execute(new Runnable()
      {
        public void run() {
          ((AbstractChannel.AbstractUnsafe)unsafe()).shutdownOutput(promise);
        }
      });
    }
    return promise;
  }
  
  public ChannelFuture shutdownInput()
  {
    return shutdownInput(newPromise());
  }
  
  public ChannelFuture shutdownInput(final ChannelPromise promise)
  {
    EventLoop loop = eventLoop();
    if (loop.inEventLoop()) {
      shutdownInput0(promise);
    } else {
      loop.execute(new Runnable()
      {
        public void run() {
          AbstractKQueueStreamChannel.this.shutdownInput0(promise);
        }
      });
    }
    return promise;
  }
  
  private void shutdownInput0(ChannelPromise promise) {
    try {
      socket.shutdown(true, false);
    } catch (Throwable cause) {
      promise.setFailure(cause);
      return;
    }
    promise.setSuccess();
  }
  
  public ChannelFuture shutdown()
  {
    return shutdown(newPromise());
  }
  
  public ChannelFuture shutdown(final ChannelPromise promise)
  {
    ChannelFuture shutdownOutputFuture = shutdownOutput();
    if (shutdownOutputFuture.isDone()) {
      shutdownOutputDone(shutdownOutputFuture, promise);
    } else {
      shutdownOutputFuture.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture shutdownOutputFuture) throws Exception {
          AbstractKQueueStreamChannel.this.shutdownOutputDone(shutdownOutputFuture, promise);
        }
      });
    }
    return promise;
  }
  
  private void shutdownOutputDone(final ChannelFuture shutdownOutputFuture, final ChannelPromise promise) {
    ChannelFuture shutdownInputFuture = shutdownInput();
    if (shutdownInputFuture.isDone()) {
      shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
    } else {
      shutdownInputFuture.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture shutdownInputFuture) throws Exception {
          AbstractKQueueStreamChannel.shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
        }
      });
    }
  }
  

  private static void shutdownDone(ChannelFuture shutdownOutputFuture, ChannelFuture shutdownInputFuture, ChannelPromise promise)
  {
    Throwable shutdownOutputCause = shutdownOutputFuture.cause();
    Throwable shutdownInputCause = shutdownInputFuture.cause();
    if (shutdownOutputCause != null) {
      if (shutdownInputCause != null) {
        logger.debug("Exception suppressed because a previous exception occurred.", shutdownInputCause);
      }
      
      promise.setFailure(shutdownOutputCause);
    } else if (shutdownInputCause != null) {
      promise.setFailure(shutdownInputCause);
    } else {
      promise.setSuccess();
    }
  }
  
  class KQueueStreamUnsafe extends AbstractKQueueChannel.AbstractKQueueUnsafe { KQueueStreamUnsafe() { super(); }
    
    protected Executor prepareToClose()
    {
      return super.prepareToClose();
    }
    
    void readReady(KQueueRecvByteAllocatorHandle allocHandle)
    {
      ChannelConfig config = config();
      if (shouldBreakReadReady(config)) {
        clearReadFilter0();
        return;
      }
      ChannelPipeline pipeline = pipeline();
      ByteBufAllocator allocator = config.getAllocator();
      allocHandle.reset(config);
      readReadyBefore();
      
      ByteBuf byteBuf = null;
      boolean close = false;
      try
      {
        do
        {
          byteBuf = allocHandle.allocate(allocator);
          allocHandle.lastBytesRead(doReadBytes(byteBuf));
          if (allocHandle.lastBytesRead() <= 0)
          {
            byteBuf.release();
            byteBuf = null;
            close = allocHandle.lastBytesRead() < 0;
            if (!close)
              break;
            readPending = false; break;
          }
          

          allocHandle.incMessagesRead(1);
          readPending = false;
          pipeline.fireChannelRead(byteBuf);
          byteBuf = null;
        }
        while ((!shouldBreakReadReady(config)) && 
        












          (allocHandle.continueReading()));
        
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        
        if (close) {
          shutdownInput(false);
        }
      } catch (Throwable t) {
        handleReadException(pipeline, byteBuf, t, close, allocHandle);
      } finally {
        readReadyFinally(config);
      }
    }
    
    private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close, KQueueRecvByteAllocatorHandle allocHandle)
    {
      if (byteBuf != null) {
        if (byteBuf.isReadable()) {
          readPending = false;
          pipeline.fireChannelRead(byteBuf);
        } else {
          byteBuf.release();
        }
      }
      if (!failConnectPromise(cause)) {
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        pipeline.fireExceptionCaught(cause);
        


        if ((close) || ((cause instanceof OutOfMemoryError)) || ((cause instanceof IOException))) {
          shutdownInput(false);
        }
      }
    }
  }
  
  private final class KQueueSocketWritableByteChannel extends SocketWritableByteChannel {
    KQueueSocketWritableByteChannel() {
      super();
    }
    
    protected ByteBufAllocator alloc()
    {
      return AbstractKQueueStreamChannel.this.alloc();
    }
  }
}
