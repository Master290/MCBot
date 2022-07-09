package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.MessageSizeEstimator.Handle;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator.ExtendedHandle;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.VoidChannelPromise;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
















abstract class AbstractHttp2StreamChannel
  extends DefaultAttributeMap
  implements Http2StreamChannel
{
  static final Http2FrameStreamVisitor WRITABLE_VISITOR = new Http2FrameStreamVisitor()
  {
    public boolean visit(Http2FrameStream stream) {
      AbstractHttp2StreamChannel childChannel = (AbstractHttp2StreamChannel)attachment;
      
      childChannel.trySetWritable();
      return true;
    }
  };
  
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractHttp2StreamChannel.class);
  
  private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  



  private static final int MIN_HTTP2_FRAME_SIZE = 9;
  



  private static final class FlowControlledFrameSizeEstimator
    implements MessageSizeEstimator
  {
    static final FlowControlledFrameSizeEstimator INSTANCE = new FlowControlledFrameSizeEstimator();
    
    private static final MessageSizeEstimator.Handle HANDLE_INSTANCE = new MessageSizeEstimator.Handle()
    {
      public int size(Object msg) {
        return (msg instanceof Http2DataFrame) ? 
        
          (int)Math.min(2147483647L, ((Http2DataFrame)msg).initialFlowControlledBytes() + 9L) : 9;
      }
    };
    
    private FlowControlledFrameSizeEstimator() {}
    
    public MessageSizeEstimator.Handle newHandle() {
      return HANDLE_INSTANCE;
    }
  }
  

  private static final AtomicLongFieldUpdater<AbstractHttp2StreamChannel> TOTAL_PENDING_SIZE_UPDATER = AtomicLongFieldUpdater.newUpdater(AbstractHttp2StreamChannel.class, "totalPendingSize");
  

  private static final AtomicIntegerFieldUpdater<AbstractHttp2StreamChannel> UNWRITABLE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AbstractHttp2StreamChannel.class, "unwritable");
  
  private static void windowUpdateFrameWriteComplete(ChannelFuture future, Channel streamChannel) {
    Throwable cause = future.cause();
    if (cause != null)
    {
      Throwable unwrappedCause;
      if (((cause instanceof Http2FrameStreamException)) && ((unwrappedCause = cause.getCause()) != null)) {
        cause = unwrappedCause;
      }
      

      streamChannel.pipeline().fireExceptionCaught(cause);
      streamChannel.unsafe().close(streamChannel.unsafe().voidPromise());
    }
  }
  
  private final ChannelFutureListener windowUpdateFrameWriteListener = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future) {
      AbstractHttp2StreamChannel.windowUpdateFrameWriteComplete(future, AbstractHttp2StreamChannel.this);
    }
  };
  





  private static enum ReadStatus
  {
    IDLE, 
    



    IN_PROGRESS, 
    



    REQUESTED;
    
    private ReadStatus() {} }
  private final Http2StreamChannelConfig config = new Http2StreamChannelConfig(this);
  private final Http2ChannelUnsafe unsafe = new Http2ChannelUnsafe(null);
  

  private final ChannelId channelId;
  
  private final ChannelPipeline pipeline;
  
  private final Http2FrameCodec.DefaultHttp2FrameStream stream;
  
  private final ChannelPromise closePromise;
  
  private volatile boolean registered;
  
  private volatile long totalPendingSize;
  
  private volatile int unwritable;
  
  private Runnable fireChannelWritabilityChangedTask;
  
  private boolean outboundClosed;
  
  private int flowControlledBytes;
  
  private ReadStatus readStatus = ReadStatus.IDLE;
  
  private Queue<Object> inboundBuffer;
  
  private boolean firstFrameWritten;
  private boolean readCompletePending;
  
  AbstractHttp2StreamChannel(Http2FrameCodec.DefaultHttp2FrameStream stream, int id, ChannelHandler inboundHandler)
  {
    this.stream = stream;
    attachment = this;
    pipeline = new DefaultChannelPipeline(this)
    {
      protected void incrementPendingOutboundBytes(long size) {
        AbstractHttp2StreamChannel.this.incrementPendingOutboundBytes(size, true);
      }
      
      protected void decrementPendingOutboundBytes(long size)
      {
        AbstractHttp2StreamChannel.this.decrementPendingOutboundBytes(size, true);
      }
      
    };
    closePromise = pipeline.newPromise();
    channelId = new Http2StreamChannelId(parent().id(), id);
    
    if (inboundHandler != null)
    {
      pipeline.addLast(new ChannelHandler[] { inboundHandler });
    }
  }
  
  private void incrementPendingOutboundBytes(long size, boolean invokeLater) {
    if (size == 0L) {
      return;
    }
    
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, size);
    if (newWriteBufferSize > config().getWriteBufferHighWaterMark()) {
      setUnwritable(invokeLater);
    }
  }
  
  private void decrementPendingOutboundBytes(long size, boolean invokeLater) {
    if (size == 0L) {
      return;
    }
    
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, -size);
    




    if ((newWriteBufferSize < config().getWriteBufferLowWaterMark()) && (parent().isWritable())) {
      setWritable(invokeLater);
    }
  }
  



  final void trySetWritable()
  {
    if (totalPendingSize < config().getWriteBufferLowWaterMark()) {
      setWritable(false);
    }
  }
  
  private void setWritable(boolean invokeLater) {
    for (;;) {
      int oldValue = unwritable;
      int newValue = oldValue & 0xFFFFFFFE;
      if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
        if ((oldValue == 0) || (newValue != 0)) break;
        fireChannelWritabilityChanged(invokeLater); break;
      }
    }
  }
  
  private void setUnwritable(boolean invokeLater)
  {
    for (;;)
    {
      int oldValue = unwritable;
      int newValue = oldValue | 0x1;
      if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
        if (oldValue != 0) break;
        fireChannelWritabilityChanged(invokeLater); break;
      }
    }
  }
  

  private void fireChannelWritabilityChanged(boolean invokeLater)
  {
    final ChannelPipeline pipeline = pipeline();
    if (invokeLater) {
      Runnable task = fireChannelWritabilityChangedTask;
      if (task == null) {
        fireChannelWritabilityChangedTask = (task = new Runnable()
        {
          public void run() {
            pipeline.fireChannelWritabilityChanged();
          }
        });
      }
      eventLoop().execute(task);
    } else {
      pipeline.fireChannelWritabilityChanged();
    }
  }
  
  public Http2FrameStream stream() {
    return stream;
  }
  
  void closeOutbound() {
    outboundClosed = true;
  }
  
  void streamClosed() {
    unsafe.readEOS();
    

    unsafe.doBeginRead();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public ChannelConfig config()
  {
    return config;
  }
  
  public boolean isOpen()
  {
    return !closePromise.isDone();
  }
  
  public boolean isActive()
  {
    return isOpen();
  }
  
  public boolean isWritable()
  {
    return unwritable == 0;
  }
  
  public ChannelId id()
  {
    return channelId;
  }
  
  public EventLoop eventLoop()
  {
    return parent().eventLoop();
  }
  
  public Channel parent()
  {
    return parentContext().channel();
  }
  
  public boolean isRegistered()
  {
    return registered;
  }
  
  public SocketAddress localAddress()
  {
    return parent().localAddress();
  }
  
  public SocketAddress remoteAddress()
  {
    return parent().remoteAddress();
  }
  
  public ChannelFuture closeFuture()
  {
    return closePromise;
  }
  
  public long bytesBeforeUnwritable()
  {
    long bytes = config().getWriteBufferHighWaterMark() - totalPendingSize;
    


    if (bytes > 0L) {
      return isWritable() ? bytes : 0L;
    }
    return 0L;
  }
  
  public long bytesBeforeWritable()
  {
    long bytes = totalPendingSize - config().getWriteBufferLowWaterMark();
    


    if (bytes > 0L) {
      return isWritable() ? 0L : bytes;
    }
    return 0L;
  }
  
  public Channel.Unsafe unsafe()
  {
    return unsafe;
  }
  
  public ChannelPipeline pipeline()
  {
    return pipeline;
  }
  
  public ByteBufAllocator alloc()
  {
    return config().getAllocator();
  }
  
  public Channel read()
  {
    pipeline().read();
    return this;
  }
  
  public Channel flush()
  {
    pipeline().flush();
    return this;
  }
  
  public ChannelFuture bind(SocketAddress localAddress)
  {
    return pipeline().bind(localAddress);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress)
  {
    return pipeline().connect(remoteAddress);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)
  {
    return pipeline().connect(remoteAddress, localAddress);
  }
  
  public ChannelFuture disconnect()
  {
    return pipeline().disconnect();
  }
  
  public ChannelFuture close()
  {
    return pipeline().close();
  }
  
  public ChannelFuture deregister()
  {
    return pipeline().deregister();
  }
  
  public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)
  {
    return pipeline().bind(localAddress, promise);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)
  {
    return pipeline().connect(remoteAddress, promise);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    return pipeline().connect(remoteAddress, localAddress, promise);
  }
  
  public ChannelFuture disconnect(ChannelPromise promise)
  {
    return pipeline().disconnect(promise);
  }
  
  public ChannelFuture close(ChannelPromise promise)
  {
    return pipeline().close(promise);
  }
  
  public ChannelFuture deregister(ChannelPromise promise)
  {
    return pipeline().deregister(promise);
  }
  
  public ChannelFuture write(Object msg)
  {
    return pipeline().write(msg);
  }
  
  public ChannelFuture write(Object msg, ChannelPromise promise)
  {
    return pipeline().write(msg, promise);
  }
  
  public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)
  {
    return pipeline().writeAndFlush(msg, promise);
  }
  
  public ChannelFuture writeAndFlush(Object msg)
  {
    return pipeline().writeAndFlush(msg);
  }
  
  public ChannelPromise newPromise()
  {
    return pipeline().newPromise();
  }
  
  public ChannelProgressivePromise newProgressivePromise()
  {
    return pipeline().newProgressivePromise();
  }
  
  public ChannelFuture newSucceededFuture()
  {
    return pipeline().newSucceededFuture();
  }
  
  public ChannelFuture newFailedFuture(Throwable cause)
  {
    return pipeline().newFailedFuture(cause);
  }
  
  public ChannelPromise voidPromise()
  {
    return pipeline().voidPromise();
  }
  
  public int hashCode()
  {
    return id().hashCode();
  }
  
  public boolean equals(Object o)
  {
    return this == o;
  }
  
  public int compareTo(Channel o)
  {
    if (this == o) {
      return 0;
    }
    
    return id().compareTo(o.id());
  }
  
  public String toString()
  {
    return parent().toString() + "(H2 - " + stream + ')';
  }
  



  void fireChildRead(Http2Frame frame)
  {
    assert (eventLoop().inEventLoop());
    if (!isActive()) {
      ReferenceCountUtil.release(frame);
    } else if (readStatus != ReadStatus.IDLE)
    {

      assert ((inboundBuffer == null) || (inboundBuffer.isEmpty()));
      RecvByteBufAllocator.Handle allocHandle = unsafe.recvBufAllocHandle();
      unsafe.doRead0(frame, allocHandle);
      



      if (allocHandle.continueReading()) {
        maybeAddChannelToReadCompletePendingQueue();
      } else {
        unsafe.notifyReadComplete(allocHandle, true);
      }
    } else {
      if (inboundBuffer == null) {
        inboundBuffer = new ArrayDeque(4);
      }
      inboundBuffer.add(frame);
    }
  }
  
  void fireChildReadComplete() {
    assert (eventLoop().inEventLoop());
    assert ((readStatus != ReadStatus.IDLE) || (!readCompletePending));
    unsafe.notifyReadComplete(unsafe.recvBufAllocHandle(), false);
  }
  
  private final class Http2ChannelUnsafe implements Channel.Unsafe {
    private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(AbstractHttp2StreamChannel.this, false);
    
    private RecvByteBufAllocator.Handle recvHandle;
    private boolean writeDoneAndNoFlush;
    private boolean closeInitiated;
    private boolean readEOS;
    
    private Http2ChannelUnsafe() {}
    
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      promise.setFailure(new UnsupportedOperationException());
    }
    
    public RecvByteBufAllocator.Handle recvBufAllocHandle()
    {
      if (recvHandle == null) {
        recvHandle = config().getRecvByteBufAllocator().newHandle();
        recvHandle.reset(config());
      }
      return recvHandle;
    }
    
    public SocketAddress localAddress()
    {
      return parent().unsafe().localAddress();
    }
    
    public SocketAddress remoteAddress()
    {
      return parent().unsafe().remoteAddress();
    }
    
    public void register(EventLoop eventLoop, ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      if (registered) {
        promise.setFailure(new UnsupportedOperationException("Re-register is not supported"));
        return;
      }
      
      registered = true;
      
      promise.setSuccess();
      
      pipeline().fireChannelRegistered();
      if (isActive()) {
        pipeline().fireChannelActive();
      }
    }
    
    public void bind(SocketAddress localAddress, ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      promise.setFailure(new UnsupportedOperationException());
    }
    
    public void disconnect(ChannelPromise promise)
    {
      close(promise);
    }
    
    public void close(final ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      if (closeInitiated) {
        if (closePromise.isDone())
        {
          promise.setSuccess();
        } else if (!(promise instanceof VoidChannelPromise))
        {
          closePromise.addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) {
              promise.setSuccess();
            }
          });
        }
        return;
      }
      closeInitiated = true;
      
      readCompletePending = false;
      
      boolean wasActive = isActive();
      





      if ((parent().isActive()) && (!readEOS) && (Http2CodecUtil.isStreamIdValid(stream.id()))) {
        Http2StreamFrame resetFrame = new DefaultHttp2ResetFrame(Http2Error.CANCEL).stream(stream());
        write(resetFrame, unsafe().voidPromise());
        flush();
      }
      
      if (inboundBuffer != null) {
        for (;;) {
          Object msg = inboundBuffer.poll();
          if (msg == null) {
            break;
          }
          ReferenceCountUtil.release(msg);
        }
        inboundBuffer = null;
      }
      

      outboundClosed = true;
      closePromise.setSuccess();
      promise.setSuccess();
      
      fireChannelInactiveAndDeregister(voidPromise(), wasActive);
    }
    
    public void closeForcibly()
    {
      close(unsafe().voidPromise());
    }
    
    public void deregister(ChannelPromise promise)
    {
      fireChannelInactiveAndDeregister(promise, false);
    }
    
    private void fireChannelInactiveAndDeregister(final ChannelPromise promise, final boolean fireChannelInactive)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      
      if (!registered) {
        promise.setSuccess();
        return;
      }
      







      invokeLater(new Runnable()
      {
        public void run() {
          if (fireChannelInactive) {
            pipeline.fireChannelInactive();
          }
          

          if (registered) {
            registered = false;
            pipeline.fireChannelUnregistered();
          }
          AbstractHttp2StreamChannel.Http2ChannelUnsafe.this.safeSetSuccess(promise);
        }
      });
    }
    
    private void safeSetSuccess(ChannelPromise promise) {
      if ((!(promise instanceof VoidChannelPromise)) && (!promise.trySuccess())) {
        AbstractHttp2StreamChannel.logger.warn("Failed to mark a promise as success because it is done already: {}", promise);
      }
    }
    









    private void invokeLater(Runnable task)
    {
      try
      {
        eventLoop().execute(task);
      } catch (RejectedExecutionException e) {
        AbstractHttp2StreamChannel.logger.warn("Can't invoke task later as EventLoop rejected it", e);
      }
    }
    
    public void beginRead()
    {
      if (!isActive()) {
        return;
      }
      updateLocalWindowIfNeeded();
      
      switch (AbstractHttp2StreamChannel.5.$SwitchMap$io$netty$handler$codec$http2$AbstractHttp2StreamChannel$ReadStatus[readStatus.ordinal()]) {
      case 1: 
        readStatus = AbstractHttp2StreamChannel.ReadStatus.IN_PROGRESS;
        doBeginRead();
        break;
      case 2: 
        readStatus = AbstractHttp2StreamChannel.ReadStatus.REQUESTED;
        break;
      }
      
    }
    
    private Object pollQueuedMessage()
    {
      return inboundBuffer == null ? null : inboundBuffer.poll();
    }
    
    void doBeginRead()
    {
      while (readStatus != AbstractHttp2StreamChannel.ReadStatus.IDLE) {
        Object message = pollQueuedMessage();
        if (message == null) {
          if (readEOS) {
            unsafe.closeForcibly();
          }
          

          flush();
          break;
        }
        RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
        allocHandle.reset(config());
        boolean continueReading = false;
        do {
          doRead0((Http2Frame)message, allocHandle);
        } while (((readEOS) || ((continueReading = allocHandle.continueReading()))) && 
          ((message = pollQueuedMessage()) != null));
        
        if ((continueReading) && (isParentReadInProgress()) && (!readEOS))
        {



          AbstractHttp2StreamChannel.this.maybeAddChannelToReadCompletePendingQueue();
        } else {
          notifyReadComplete(allocHandle, true);
        }
      }
    }
    
    void readEOS() {
      readEOS = true;
    }
    
    private void updateLocalWindowIfNeeded() {
      if (flowControlledBytes != 0) {
        int bytes = flowControlledBytes;
        flowControlledBytes = 0;
        ChannelFuture future = write0(parentContext(), new DefaultHttp2WindowUpdateFrame(bytes).stream(stream));
        


        writeDoneAndNoFlush = true;
        



        if (future.isDone()) {
          AbstractHttp2StreamChannel.windowUpdateFrameWriteComplete(future, AbstractHttp2StreamChannel.this);
        } else {
          future.addListener(windowUpdateFrameWriteListener);
        }
      }
    }
    
    void notifyReadComplete(RecvByteBufAllocator.Handle allocHandle, boolean forceReadComplete) {
      if ((!readCompletePending) && (!forceReadComplete)) {
        return;
      }
      
      readCompletePending = false;
      
      if (readStatus == AbstractHttp2StreamChannel.ReadStatus.REQUESTED) {
        readStatus = AbstractHttp2StreamChannel.ReadStatus.IN_PROGRESS;
      } else {
        readStatus = AbstractHttp2StreamChannel.ReadStatus.IDLE;
      }
      
      allocHandle.readComplete();
      pipeline().fireChannelReadComplete();
      


      flush();
      if (readEOS) {
        unsafe.closeForcibly();
      }
    }
    
    void doRead0(Http2Frame frame, RecvByteBufAllocator.Handle allocHandle)
    {
      int bytes;
      if ((frame instanceof Http2DataFrame)) {
        int bytes = ((Http2DataFrame)frame).initialFlowControlledBytes();
        





        flowControlledBytes = (flowControlledBytes + bytes);
      } else {
        bytes = 9;
      }
      
      allocHandle.attemptedBytesRead(bytes);
      allocHandle.lastBytesRead(bytes);
      allocHandle.incMessagesRead(1);
      
      pipeline().fireChannelRead(frame);
    }
    

    public void write(Object msg, ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        ReferenceCountUtil.release(msg);
        return;
      }
      
      if ((!isActive()) || (
      
        (outboundClosed) && (((msg instanceof Http2HeadersFrame)) || ((msg instanceof Http2DataFrame))))) {
        ReferenceCountUtil.release(msg);
        promise.setFailure(new ClosedChannelException());
        return;
      }
      try
      {
        if ((msg instanceof Http2StreamFrame)) {
          Http2StreamFrame frame = validateStreamFrame((Http2StreamFrame)msg).stream(stream());
          writeHttp2StreamFrame(frame, promise);
        } else {
          String msgStr = msg.toString();
          ReferenceCountUtil.release(msg);
          promise.setFailure(new IllegalArgumentException("Message must be an " + 
            StringUtil.simpleClassName(Http2StreamFrame.class) + ": " + msgStr));
        }
      }
      catch (Throwable t) {
        promise.tryFailure(t);
      }
    }
    
    private void writeHttp2StreamFrame(Http2StreamFrame frame, final ChannelPromise promise) {
      if ((!firstFrameWritten) && (!Http2CodecUtil.isStreamIdValid(stream().id())) && (!(frame instanceof Http2HeadersFrame))) {
        ReferenceCountUtil.release(frame);
        promise.setFailure(new IllegalArgumentException("The first frame must be a headers frame. Was: " + frame
        
          .name())); return;
      }
      
      boolean firstWrite;
      final boolean firstWrite;
      if (firstFrameWritten) {
        firstWrite = false;
      } else {
        firstWrite = firstFrameWritten = true;
      }
      
      ChannelFuture f = write0(parentContext(), frame);
      if (f.isDone()) {
        if (firstWrite) {
          firstWriteComplete(f, promise);
        } else {
          writeComplete(f, promise);
        }
      } else {
        final long bytes = AbstractHttp2StreamChannel.FlowControlledFrameSizeEstimator.access$1900().size(frame);
        AbstractHttp2StreamChannel.this.incrementPendingOutboundBytes(bytes, false);
        f.addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) {
            if (firstWrite) {
              AbstractHttp2StreamChannel.Http2ChannelUnsafe.this.firstWriteComplete(future, promise);
            } else {
              AbstractHttp2StreamChannel.Http2ChannelUnsafe.this.writeComplete(future, promise);
            }
            AbstractHttp2StreamChannel.this.decrementPendingOutboundBytes(bytes, false);
          }
        });
        writeDoneAndNoFlush = true;
      }
    }
    
    private void firstWriteComplete(ChannelFuture future, ChannelPromise promise) {
      Throwable cause = future.cause();
      if (cause == null) {
        promise.setSuccess();
      }
      else {
        closeForcibly();
        promise.setFailure(wrapStreamClosedError(cause));
      }
    }
    
    private void writeComplete(ChannelFuture future, ChannelPromise promise) {
      Throwable cause = future.cause();
      if (cause == null) {
        promise.setSuccess();
      } else {
        Throwable error = wrapStreamClosedError(cause);
        
        if ((error instanceof IOException)) {
          if (config.isAutoClose())
          {
            closeForcibly();
          }
          else {
            outboundClosed = true;
          }
        }
        promise.setFailure(error);
      }
    }
    

    private Throwable wrapStreamClosedError(Throwable cause)
    {
      if (((cause instanceof Http2Exception)) && (((Http2Exception)cause).error() == Http2Error.STREAM_CLOSED)) {
        return new ClosedChannelException().initCause(cause);
      }
      return cause;
    }
    
    private Http2StreamFrame validateStreamFrame(Http2StreamFrame frame) {
      if ((frame.stream() != null) && (frame.stream() != stream)) {
        String msgString = frame.toString();
        ReferenceCountUtil.release(frame);
        
        throw new IllegalArgumentException("Stream " + frame.stream() + " must not be set on the frame: " + msgString);
      }
      return frame;
    }
    




    public void flush()
    {
      if ((!writeDoneAndNoFlush) || (isParentReadInProgress()))
      {
        return;
      }
      

      writeDoneAndNoFlush = false;
      flush0(parentContext());
    }
    
    public ChannelPromise voidPromise()
    {
      return unsafeVoidPromise;
    }
    

    public ChannelOutboundBuffer outboundBuffer()
    {
      return null;
    }
  }
  


  private static final class Http2StreamChannelConfig
    extends DefaultChannelConfig
  {
    Http2StreamChannelConfig(Channel channel)
    {
      super();
    }
    
    public MessageSizeEstimator getMessageSizeEstimator()
    {
      return AbstractHttp2StreamChannel.FlowControlledFrameSizeEstimator.INSTANCE;
    }
    
    public ChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
    {
      throw new UnsupportedOperationException();
    }
    
    public ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
    {
      if (!(allocator.newHandle() instanceof RecvByteBufAllocator.ExtendedHandle)) {
        throw new IllegalArgumentException("allocator.newHandle() must return an object of type: " + RecvByteBufAllocator.ExtendedHandle.class);
      }
      
      super.setRecvByteBufAllocator(allocator);
      return this;
    }
  }
  
  private void maybeAddChannelToReadCompletePendingQueue() {
    if (!readCompletePending) {
      readCompletePending = true;
      addChannelToReadCompletePendingQueue();
    }
  }
  
  protected void flush0(ChannelHandlerContext ctx) {
    ctx.flush();
  }
  
  protected ChannelFuture write0(ChannelHandlerContext ctx, Object msg) {
    ChannelPromise promise = ctx.newPromise();
    ctx.write(msg, promise);
    return promise;
  }
  
  protected abstract boolean isParentReadInProgress();
  
  protected abstract void addChannelToReadCompletePendingQueue();
  
  protected abstract ChannelHandlerContext parentContext();
}
