package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
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
import io.netty.channel.RecvByteBufAllocator.ExtendedHandle;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.IovArray;
import io.netty.channel.unix.SocketWritableByteChannel;
import io.netty.channel.unix.UnixChannelUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;
import java.util.concurrent.Executor;


















public abstract class AbstractEpollStreamChannel
  extends AbstractEpollChannel
  implements DuplexChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  private static final String EXPECTED_TYPES = " (expected: " + 
    StringUtil.simpleClassName(ByteBuf.class) + ", " + 
    StringUtil.simpleClassName(DefaultFileRegion.class) + ')';
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEpollStreamChannel.class);
  
  private final Runnable flushTask = new Runnable()
  {

    public void run()
    {
      ((AbstractEpollChannel.AbstractEpollUnsafe)unsafe()).flush0();
    }
  };
  
  private volatile Queue<SpliceInTask> spliceQueue;
  
  private FileDescriptor pipeIn;
  private FileDescriptor pipeOut;
  private WritableByteChannel byteChannel;
  
  protected AbstractEpollStreamChannel(Channel parent, int fd)
  {
    this(parent, new LinuxSocket(fd));
  }
  
  protected AbstractEpollStreamChannel(int fd) {
    this(new LinuxSocket(fd));
  }
  
  AbstractEpollStreamChannel(LinuxSocket fd) {
    this(fd, isSoErrorZero(fd));
  }
  
  AbstractEpollStreamChannel(Channel parent, LinuxSocket fd) {
    super(parent, fd, true);
    
    flags |= Native.EPOLLRDHUP;
  }
  
  AbstractEpollStreamChannel(Channel parent, LinuxSocket fd, SocketAddress remote) {
    super(parent, fd, remote);
    
    flags |= Native.EPOLLRDHUP;
  }
  
  protected AbstractEpollStreamChannel(LinuxSocket fd, boolean active) {
    super(null, fd, active);
    
    flags |= Native.EPOLLRDHUP;
  }
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollStreamUnsafe();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  













  public final ChannelFuture spliceTo(AbstractEpollStreamChannel ch, int len)
  {
    return spliceTo(ch, len, newPromise());
  }
  














  public final ChannelFuture spliceTo(AbstractEpollStreamChannel ch, int len, ChannelPromise promise)
  {
    if (ch.eventLoop() != eventLoop()) {
      throw new IllegalArgumentException("EventLoops are not the same.");
    }
    ObjectUtil.checkPositiveOrZero(len, "len");
    if ((ch.config().getEpollMode() != EpollMode.LEVEL_TRIGGERED) || 
      (config().getEpollMode() != EpollMode.LEVEL_TRIGGERED)) {
      throw new IllegalStateException("spliceTo() supported only when using " + EpollMode.LEVEL_TRIGGERED);
    }
    ObjectUtil.checkNotNull(promise, "promise");
    if (!isOpen()) {
      promise.tryFailure(new ClosedChannelException());
    } else {
      addToSpliceQueue(new SpliceInChannelTask(ch, len, promise));
      failSpliceIfClosed(promise);
    }
    return promise;
  }
  













  public final ChannelFuture spliceTo(FileDescriptor ch, int offset, int len)
  {
    return spliceTo(ch, offset, len, newPromise());
  }
  














  public final ChannelFuture spliceTo(FileDescriptor ch, int offset, int len, ChannelPromise promise)
  {
    ObjectUtil.checkPositiveOrZero(len, "len");
    ObjectUtil.checkPositiveOrZero(offset, "offset");
    if (config().getEpollMode() != EpollMode.LEVEL_TRIGGERED) {
      throw new IllegalStateException("spliceTo() supported only when using " + EpollMode.LEVEL_TRIGGERED);
    }
    ObjectUtil.checkNotNull(promise, "promise");
    if (!isOpen()) {
      promise.tryFailure(new ClosedChannelException());
    } else {
      addToSpliceQueue(new SpliceFdTask(ch, offset, len, promise));
      failSpliceIfClosed(promise);
    }
    return promise;
  }
  
  private void failSpliceIfClosed(ChannelPromise promise) {
    if (!isOpen())
    {

      if (promise.tryFailure(new ClosedChannelException())) {
        eventLoop().execute(new Runnable()
        {
          public void run()
          {
            AbstractEpollStreamChannel.this.clearSpliceQueue();
          }
        });
      }
    }
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
    long offset = region.transferred();
    long regionCount = region.count();
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
      byteChannel = new EpollSocketWritableByteChannel();
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
          clearFlag(Native.EPOLLOUT);
          
          return;
        }
        writeSpinCount -= doWriteSingle(in);

      }
      

    }
    while (writeSpinCount > 0);
    
    if (writeSpinCount == 0)
    {



      clearFlag(Native.EPOLLOUT);
      

      eventLoop().execute(flushTask);
    }
    else
    {
      setFlag(Native.EPOLLOUT);
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
    if ((msg instanceof FileRegion))
      return writeFileRegion(in, (FileRegion)msg);
    if ((msg instanceof SpliceOutTask)) {
      if (!((SpliceOutTask)msg).spliceOut()) {
        return Integer.MAX_VALUE;
      }
      in.remove();
      return 1;
    }
    
    throw new Error();
  }
  













  private int doWriteMultiple(ChannelOutboundBuffer in)
    throws Exception
  {
    long maxBytesPerGatheringWrite = config().getMaxBytesPerGatheringWrite();
    IovArray array = ((EpollEventLoop)eventLoop()).cleanIovArray();
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
    
    if (((msg instanceof FileRegion)) || ((msg instanceof SpliceOutTask))) {
      return msg;
    }
    

    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  protected final void doShutdownOutput()
    throws Exception
  {
    socket.shutdown(false, true);
  }
  
  private void shutdownInput0(ChannelPromise promise) {
    try {
      socket.shutdown(true, false);
      promise.setSuccess();
    } catch (Throwable cause) {
      promise.setFailure(cause);
    }
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
    Executor closeExecutor = ((EpollStreamUnsafe)unsafe()).prepareToClose();
    if (closeExecutor != null) {
      closeExecutor.execute(new Runnable()
      {
        public void run() {
          AbstractEpollStreamChannel.this.shutdownInput0(promise);
        }
      });
    } else {
      EventLoop loop = eventLoop();
      if (loop.inEventLoop()) {
        shutdownInput0(promise);
      } else {
        loop.execute(new Runnable()
        {
          public void run() {
            AbstractEpollStreamChannel.this.shutdownInput0(promise);
          }
        });
      }
    }
    return promise;
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
          AbstractEpollStreamChannel.this.shutdownOutputDone(shutdownOutputFuture, promise);
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
          AbstractEpollStreamChannel.shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
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
  
  protected void doClose() throws Exception
  {
    try
    {
      super.doClose();
      
      safeClosePipe(pipeIn);
      safeClosePipe(pipeOut);
      clearSpliceQueue();
    }
    finally
    {
      safeClosePipe(pipeIn);
      safeClosePipe(pipeOut);
      clearSpliceQueue();
    }
  }
  
  private void clearSpliceQueue() {
    Queue<SpliceInTask> sQueue = spliceQueue;
    if (sQueue == null) {
      return;
    }
    ClosedChannelException exception = null;
    for (;;)
    {
      SpliceInTask task = (SpliceInTask)sQueue.poll();
      if (task == null) {
        break;
      }
      if (exception == null) {
        exception = new ClosedChannelException();
      }
      promise.tryFailure(exception);
    }
  }
  
  private static void safeClosePipe(FileDescriptor fd) {
    if (fd != null)
      try {
        fd.close();
      } catch (IOException e) {
        logger.warn("Error while closing a pipe", e);
      }
  }
  
  class EpollStreamUnsafe extends AbstractEpollChannel.AbstractEpollUnsafe {
    EpollStreamUnsafe() { super(); }
    
    protected Executor prepareToClose()
    {
      return super.prepareToClose();
    }
    
    private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close, EpollRecvByteAllocatorHandle allocHandle)
    {
      if (byteBuf != null) {
        if (byteBuf.isReadable()) {
          readPending = false;
          pipeline.fireChannelRead(byteBuf);
        } else {
          byteBuf.release();
        }
      }
      allocHandle.readComplete();
      pipeline.fireChannelReadComplete();
      pipeline.fireExceptionCaught(cause);
      


      if ((close) || ((cause instanceof OutOfMemoryError)) || ((cause instanceof IOException))) {
        shutdownInput(false);
      }
    }
    
    EpollRecvByteAllocatorHandle newEpollHandle(RecvByteBufAllocator.ExtendedHandle handle)
    {
      return new EpollRecvByteAllocatorStreamingHandle(handle);
    }
    
    void epollInReady()
    {
      ChannelConfig config = config();
      if (shouldBreakEpollInReady(config)) {
        clearEpollIn0();
        return;
      }
      EpollRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
      allocHandle.edgeTriggered(isFlagSet(Native.EPOLLET));
      
      ChannelPipeline pipeline = pipeline();
      ByteBufAllocator allocator = config.getAllocator();
      allocHandle.reset(config);
      epollInBefore();
      
      ByteBuf byteBuf = null;
      boolean close = false;
      try {
        Queue<AbstractEpollStreamChannel.SpliceInTask> sQueue = null;
        do {
          if ((sQueue != null) || ((sQueue = spliceQueue) != null)) {
            AbstractEpollStreamChannel.SpliceInTask spliceTask = (AbstractEpollStreamChannel.SpliceInTask)sQueue.peek();
            if (spliceTask != null) {
              if (!spliceTask.spliceIn(allocHandle)) {
                break;
              }
              if (!isActive()) continue;
              sQueue.remove(); continue;
            }
          }
          







          byteBuf = allocHandle.allocate(allocator);
          allocHandle.lastBytesRead(doReadBytes(byteBuf));
          if (allocHandle.lastBytesRead() <= 0)
          {
            byteBuf.release();
            byteBuf = null;
            close = allocHandle.lastBytesRead() < 0;
            if (close)
            {
              readPending = false;
            }
          }
          else {
            allocHandle.incMessagesRead(1);
            readPending = false;
            pipeline.fireChannelRead(byteBuf);
            byteBuf = null;
            
            if (shouldBreakEpollInReady(config))
            {

              break;


            }
            


          }
          

        }
        while (allocHandle.continueReading());
        
        allocHandle.readComplete();
        pipeline.fireChannelReadComplete();
        
        if (close) {
          shutdownInput(false);
        }
      } catch (Throwable t) {
        handleReadException(pipeline, byteBuf, t, close, allocHandle);
      } finally {
        epollInFinally(config);
      }
    }
  }
  
  private void addToSpliceQueue(SpliceInTask task) {
    Queue<SpliceInTask> sQueue = spliceQueue;
    if (sQueue == null) {
      synchronized (this) {
        sQueue = spliceQueue;
        if (sQueue == null) {
          spliceQueue = (sQueue = PlatformDependent.newMpscQueue());
        }
      }
    }
    sQueue.add(task);
  }
  
  protected abstract class SpliceInTask {
    final ChannelPromise promise;
    int len;
    
    protected SpliceInTask(int len, ChannelPromise promise) {
      this.promise = promise;
      this.len = len;
    }
    
    abstract boolean spliceIn(RecvByteBufAllocator.Handle paramHandle);
    
    protected final int spliceIn(FileDescriptor pipeOut, RecvByteBufAllocator.Handle handle) throws IOException
    {
      int length = Math.min(handle.guess(), len);
      int splicedIn = 0;
      for (;;)
      {
        int localSplicedIn = Native.splice(socket.intValue(), -1L, pipeOut.intValue(), -1L, length);
        if (localSplicedIn == 0) {
          break;
        }
        splicedIn += localSplicedIn;
        length -= localSplicedIn;
      }
      
      return splicedIn;
    }
  }
  
  private final class SpliceInChannelTask extends AbstractEpollStreamChannel.SpliceInTask implements ChannelFutureListener
  {
    private final AbstractEpollStreamChannel ch;
    
    SpliceInChannelTask(AbstractEpollStreamChannel ch, int len, ChannelPromise promise) {
      super(len, promise);
      this.ch = ch;
    }
    
    public void operationComplete(ChannelFuture future) throws Exception
    {
      if (!future.isSuccess()) {
        promise.setFailure(future.cause());
      }
    }
    
    public boolean spliceIn(RecvByteBufAllocator.Handle handle)
    {
      assert (ch.eventLoop().inEventLoop());
      if (len == 0) {
        promise.setSuccess();
        return true;
      }
      

      try
      {
        FileDescriptor pipeOut = ch.pipeOut;
        if (pipeOut == null)
        {
          FileDescriptor[] pipe = FileDescriptor.pipe();
          ch.pipeIn = pipe[0];
          pipeOut = ch.pipeOut = pipe[1];
        }
        
        int splicedIn = spliceIn(pipeOut, handle);
        if (splicedIn > 0)
        {
          if (len != Integer.MAX_VALUE) {
            len -= splicedIn;
          }
          
          ChannelPromise splicePromise;
          
          ChannelPromise splicePromise;
          if (len == 0) {
            splicePromise = promise;
          } else {
            splicePromise = ch.newPromise().addListener(this);
          }
          
          boolean autoRead = config().isAutoRead();
          


          ch.unsafe().write(new AbstractEpollStreamChannel.SpliceOutTask(AbstractEpollStreamChannel.this, ch, splicedIn, autoRead), splicePromise);
          ch.unsafe().flush();
          if ((autoRead) && (!splicePromise.isDone()))
          {



            config().setAutoRead(false);
          }
        }
        
        return len == 0;
      } catch (Throwable cause) {
        promise.setFailure(cause); }
      return true;
    }
  }
  
  private final class SpliceOutTask
  {
    private final AbstractEpollStreamChannel ch;
    private final boolean autoRead;
    private int len;
    
    SpliceOutTask(AbstractEpollStreamChannel ch, int len, boolean autoRead) {
      this.ch = ch;
      this.len = len;
      this.autoRead = autoRead;
    }
    
    public boolean spliceOut() throws Exception {
      assert (ch.eventLoop().inEventLoop());
      try {
        int splicedOut = Native.splice(ch.pipeIn.intValue(), -1L, ch.socket.intValue(), -1L, len);
        len -= splicedOut;
        if (len == 0) {
          if (autoRead)
          {
            config().setAutoRead(true);
          }
          return true;
        }
        return false;
      } catch (IOException e) {
        if (autoRead)
        {
          config().setAutoRead(true);
        }
        throw e;
      }
    }
  }
  
  private final class SpliceFdTask extends AbstractEpollStreamChannel.SpliceInTask {
    private final FileDescriptor fd;
    private final ChannelPromise promise;
    private int offset;
    
    SpliceFdTask(FileDescriptor fd, int offset, int len, ChannelPromise promise) {
      super(len, promise);
      this.fd = fd;
      this.promise = promise;
      this.offset = offset;
    }
    
    public boolean spliceIn(RecvByteBufAllocator.Handle handle)
    {
      assert (eventLoop().inEventLoop());
      if (len == 0) {
        promise.setSuccess();
        return true;
      }
      try
      {
        FileDescriptor[] pipe = FileDescriptor.pipe();
        FileDescriptor pipeIn = pipe[0];
        FileDescriptor pipeOut = pipe[1];
        try {
          int splicedIn = spliceIn(pipeOut, handle);
          int splicedOut; if (splicedIn > 0)
          {
            if (len != Integer.MAX_VALUE) {
              len -= splicedIn;
            }
            do {
              splicedOut = Native.splice(pipeIn.intValue(), -1L, fd.intValue(), offset, splicedIn);
              offset += splicedOut;
              splicedIn -= splicedOut;
            } while (splicedIn > 0);
            if (len == 0) {
              promise.setSuccess();
              return 1;
            }
          }
          return 0;
        } finally {
          AbstractEpollStreamChannel.safeClosePipe(pipeIn);
          AbstractEpollStreamChannel.safeClosePipe(pipeOut);
        }
        

        return true;
      }
      catch (Throwable cause)
      {
        promise.setFailure(cause);
      }
    }
  }
  
  private final class EpollSocketWritableByteChannel extends SocketWritableByteChannel
  {
    EpollSocketWritableByteChannel() {
      super();
    }
    
    protected ByteBufAllocator alloc()
    {
      return AbstractEpollStreamChannel.this.alloc();
    }
  }
}
