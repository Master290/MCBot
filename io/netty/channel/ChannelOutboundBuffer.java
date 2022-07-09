package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PromiseNotificationUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;







































public final class ChannelOutboundBuffer
{
  static final int CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD = SystemPropertyUtil.getInt("io.netty.transport.outboundBufferEntrySizeOverhead", 96);
  
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelOutboundBuffer.class);
  
  private static final FastThreadLocal<ByteBuffer[]> NIO_BUFFERS = new FastThreadLocal()
  {
    protected ByteBuffer[] initialValue() throws Exception {
      return new ByteBuffer['Ѐ'];
    }
  };
  

  private final Channel channel;
  

  private Entry flushedEntry;
  

  private Entry unflushedEntry;
  
  private Entry tailEntry;
  
  private int flushed;
  
  private int nioBufferCount;
  
  private long nioBufferSize;
  
  private boolean inFail;
  
  private static final AtomicLongFieldUpdater<ChannelOutboundBuffer> TOTAL_PENDING_SIZE_UPDATER = AtomicLongFieldUpdater.newUpdater(ChannelOutboundBuffer.class, "totalPendingSize");
  

  private volatile long totalPendingSize;
  

  private static final AtomicIntegerFieldUpdater<ChannelOutboundBuffer> UNWRITABLE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(ChannelOutboundBuffer.class, "unwritable");
  
  private volatile int unwritable;
  
  private volatile Runnable fireChannelWritabilityChangedTask;
  
  ChannelOutboundBuffer(AbstractChannel channel)
  {
    this.channel = channel;
  }
  



  public void addMessage(Object msg, int size, ChannelPromise promise)
  {
    Entry entry = Entry.newInstance(msg, size, total(msg), promise);
    if (tailEntry == null) {
      flushedEntry = null;
    } else {
      Entry tail = tailEntry;
      next = entry;
    }
    tailEntry = entry;
    if (unflushedEntry == null) {
      unflushedEntry = entry;
    }
    


    incrementPendingOutboundBytes(pendingSize, false);
  }
  







  public void addFlush()
  {
    Entry entry = unflushedEntry;
    if (entry != null) {
      if (flushedEntry == null)
      {
        flushedEntry = entry;
      }
      do {
        flushed += 1;
        if (!promise.setUncancellable())
        {
          int pending = entry.cancel();
          decrementPendingOutboundBytes(pending, false, true);
        }
        entry = next;
      } while (entry != null);
      

      unflushedEntry = null;
    }
  }
  



  void incrementPendingOutboundBytes(long size)
  {
    incrementPendingOutboundBytes(size, true);
  }
  
  private void incrementPendingOutboundBytes(long size, boolean invokeLater) {
    if (size == 0L) {
      return;
    }
    
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, size);
    if (newWriteBufferSize > channel.config().getWriteBufferHighWaterMark()) {
      setUnwritable(invokeLater);
    }
  }
  



  void decrementPendingOutboundBytes(long size)
  {
    decrementPendingOutboundBytes(size, true, true);
  }
  
  private void decrementPendingOutboundBytes(long size, boolean invokeLater, boolean notifyWritability) {
    if (size == 0L) {
      return;
    }
    
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, -size);
    if ((notifyWritability) && (newWriteBufferSize < channel.config().getWriteBufferLowWaterMark())) {
      setWritable(invokeLater);
    }
  }
  
  private static long total(Object msg) {
    if ((msg instanceof ByteBuf)) {
      return ((ByteBuf)msg).readableBytes();
    }
    if ((msg instanceof FileRegion)) {
      return ((FileRegion)msg).count();
    }
    if ((msg instanceof ByteBufHolder)) {
      return ((ByteBufHolder)msg).content().readableBytes();
    }
    return -1L;
  }
  


  public Object current()
  {
    Entry entry = flushedEntry;
    if (entry == null) {
      return null;
    }
    
    return msg;
  }
  



  public long currentProgress()
  {
    Entry entry = flushedEntry;
    if (entry == null) {
      return 0L;
    }
    return progress;
  }
  


  public void progress(long amount)
  {
    Entry e = flushedEntry;
    assert (e != null);
    ChannelPromise p = promise;
    long progress = progress + amount;
    progress = progress;
    if ((p instanceof ChannelProgressivePromise)) {
      ((ChannelProgressivePromise)p).tryProgress(progress, total);
    }
  }
  




  public boolean remove()
  {
    Entry e = flushedEntry;
    if (e == null) {
      clearNioBuffers();
      return false;
    }
    Object msg = msg;
    
    ChannelPromise promise = promise;
    int size = pendingSize;
    
    removeEntry(e);
    
    if (!cancelled)
    {
      ReferenceCountUtil.safeRelease(msg);
      safeSuccess(promise);
      decrementPendingOutboundBytes(size, false, true);
    }
    

    e.recycle();
    
    return true;
  }
  




  public boolean remove(Throwable cause)
  {
    return remove0(cause, true);
  }
  
  private boolean remove0(Throwable cause, boolean notifyWritability) {
    Entry e = flushedEntry;
    if (e == null) {
      clearNioBuffers();
      return false;
    }
    Object msg = msg;
    
    ChannelPromise promise = promise;
    int size = pendingSize;
    
    removeEntry(e);
    
    if (!cancelled)
    {
      ReferenceCountUtil.safeRelease(msg);
      
      safeFail(promise, cause);
      decrementPendingOutboundBytes(size, false, notifyWritability);
    }
    

    e.recycle();
    
    return true;
  }
  
  private void removeEntry(Entry e) {
    if (--flushed == 0)
    {
      flushedEntry = null;
      if (e == tailEntry) {
        tailEntry = null;
        unflushedEntry = null;
      }
    } else {
      flushedEntry = next;
    }
  }
  


  public void removeBytes(long writtenBytes)
  {
    for (;;)
    {
      Object msg = current();
      if (!(msg instanceof ByteBuf)) {
        if (($assertionsDisabled) || (writtenBytes == 0L)) break; throw new AssertionError();
      }
      

      ByteBuf buf = (ByteBuf)msg;
      int readerIndex = buf.readerIndex();
      int readableBytes = buf.writerIndex() - readerIndex;
      
      if (readableBytes <= writtenBytes) {
        if (writtenBytes != 0L) {
          progress(readableBytes);
          writtenBytes -= readableBytes;
        }
        remove();
      } else {
        if (writtenBytes == 0L) break;
        buf.readerIndex(readerIndex + (int)writtenBytes);
        progress(writtenBytes); break;
      }
    }
    

    clearNioBuffers();
  }
  

  private void clearNioBuffers()
  {
    int count = nioBufferCount;
    if (count > 0) {
      nioBufferCount = 0;
      Arrays.fill((Object[])NIO_BUFFERS.get(), 0, count, null);
    }
  }
  









  public ByteBuffer[] nioBuffers()
  {
    return nioBuffers(Integer.MAX_VALUE, 2147483647L);
  }
  













  public ByteBuffer[] nioBuffers(int maxCount, long maxBytes)
  {
    assert (maxCount > 0);
    assert (maxBytes > 0L);
    long nioBufferSize = 0L;
    int nioBufferCount = 0;
    InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
    ByteBuffer[] nioBuffers = (ByteBuffer[])NIO_BUFFERS.get(threadLocalMap);
    Entry entry = flushedEntry;
    while ((isFlushedEntry(entry)) && ((msg instanceof ByteBuf))) {
      if (!cancelled) {
        ByteBuf buf = (ByteBuf)msg;
        int readerIndex = buf.readerIndex();
        int readableBytes = buf.writerIndex() - readerIndex;
        
        if (readableBytes > 0) {
          if ((maxBytes - readableBytes < nioBufferSize) && (nioBufferCount != 0)) {
            break;
          }
          










          nioBufferSize += readableBytes;
          int count = count;
          if (count == -1)
          {
            count = (count = buf.nioBufferCount());
          }
          int neededSpace = Math.min(maxCount, nioBufferCount + count);
          if (neededSpace > nioBuffers.length) {
            nioBuffers = expandNioBufferArray(nioBuffers, neededSpace, nioBufferCount);
            NIO_BUFFERS.set(threadLocalMap, nioBuffers);
          }
          if (count == 1) {
            ByteBuffer nioBuf = buf;
            if (nioBuf == null)
            {

              buf = (nioBuf = buf.internalNioBuffer(readerIndex, readableBytes));
            }
            nioBuffers[(nioBufferCount++)] = nioBuf;
          }
          else
          {
            nioBufferCount = nioBuffers(entry, buf, nioBuffers, nioBufferCount, maxCount);
          }
          if (nioBufferCount >= maxCount) {
            break;
          }
        }
      }
      entry = next;
    }
    this.nioBufferCount = nioBufferCount;
    this.nioBufferSize = nioBufferSize;
    
    return nioBuffers;
  }
  
  private static int nioBuffers(Entry entry, ByteBuf buf, ByteBuffer[] nioBuffers, int nioBufferCount, int maxCount) {
    ByteBuffer[] nioBufs = bufs;
    if (nioBufs == null)
    {

      bufs = (nioBufs = buf.nioBuffers());
    }
    for (int i = 0; (i < nioBufs.length) && (nioBufferCount < maxCount); i++) {
      ByteBuffer nioBuf = nioBufs[i];
      if (nioBuf == null)
        break;
      if (nioBuf.hasRemaining())
      {

        nioBuffers[(nioBufferCount++)] = nioBuf; }
    }
    return nioBufferCount;
  }
  
  private static ByteBuffer[] expandNioBufferArray(ByteBuffer[] array, int neededSpace, int size) {
    int newCapacity = array.length;
    
    do
    {
      newCapacity <<= 1;
      
      if (newCapacity < 0) {
        throw new IllegalStateException();
      }
      
    } while (neededSpace > newCapacity);
    
    ByteBuffer[] newArray = new ByteBuffer[newCapacity];
    System.arraycopy(array, 0, newArray, 0, size);
    
    return newArray;
  }
  




  public int nioBufferCount()
  {
    return nioBufferCount;
  }
  




  public long nioBufferSize()
  {
    return nioBufferSize;
  }
  





  public boolean isWritable()
  {
    return unwritable == 0;
  }
  



  public boolean getUserDefinedWritability(int index)
  {
    return (unwritable & writabilityMask(index)) == 0;
  }
  


  public void setUserDefinedWritability(int index, boolean writable)
  {
    if (writable) {
      setUserDefinedWritability(index);
    } else {
      clearUserDefinedWritability(index);
    }
  }
  
  private void setUserDefinedWritability(int index) {
    int mask = writabilityMask(index) ^ 0xFFFFFFFF;
    for (;;) {
      int oldValue = unwritable;
      int newValue = oldValue & mask;
      if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
        if ((oldValue == 0) || (newValue != 0)) break;
        fireChannelWritabilityChanged(true); break;
      }
    }
  }
  

  private void clearUserDefinedWritability(int index)
  {
    int mask = writabilityMask(index);
    for (;;) {
      int oldValue = unwritable;
      int newValue = oldValue | mask;
      if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
        if ((oldValue != 0) || (newValue == 0)) break;
        fireChannelWritabilityChanged(true); break;
      }
    }
  }
  

  private static int writabilityMask(int index)
  {
    if ((index < 1) || (index > 31)) {
      throw new IllegalArgumentException("index: " + index + " (expected: 1~31)");
    }
    return 1 << index;
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
    final ChannelPipeline pipeline = channel.pipeline();
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
      channel.eventLoop().execute(task);
    } else {
      pipeline.fireChannelWritabilityChanged();
    }
  }
  


  public int size()
  {
    return flushed;
  }
  



  public boolean isEmpty()
  {
    return flushed == 0;
  }
  




  void failFlushed(Throwable cause, boolean notify)
  {
    if (inFail) {
      return;
    }
    try
    {
      inFail = true;
      for (;;) {
        if (!remove0(cause, notify)) {
          break;
        }
      }
      
      inFail = false; } finally { inFail = false;
    }
  }
  
  void close(final Throwable cause, final boolean allowChannelOpen) {
    if (inFail) {
      channel.eventLoop().execute(new Runnable()
      {
        public void run() {
          close(cause, allowChannelOpen);
        }
      });
      return;
    }
    
    inFail = true;
    
    if ((!allowChannelOpen) && (channel.isOpen())) {
      throw new IllegalStateException("close() must be invoked after the channel is closed.");
    }
    
    if (!isEmpty()) {
      throw new IllegalStateException("close() must be invoked after all flushed writes are handled.");
    }
    
    try
    {
      Entry e = unflushedEntry;
      while (e != null)
      {
        int size = pendingSize;
        TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, -size);
        
        if (!cancelled) {
          ReferenceCountUtil.safeRelease(msg);
          safeFail(promise, cause);
        }
        e = e.recycleAndGetNext();
      }
    } finally {
      inFail = false;
    }
    clearNioBuffers();
  }
  
  void close(ClosedChannelException cause) {
    close(cause, false);
  }
  

  private static void safeSuccess(ChannelPromise promise)
  {
    PromiseNotificationUtil.trySuccess(promise, null, (promise instanceof VoidChannelPromise) ? null : logger);
  }
  

  private static void safeFail(ChannelPromise promise, Throwable cause)
  {
    PromiseNotificationUtil.tryFailure(promise, cause, (promise instanceof VoidChannelPromise) ? null : logger);
  }
  

  @Deprecated
  public void recycle() {}
  
  public long totalPendingWriteBytes()
  {
    return totalPendingSize;
  }
  



  public long bytesBeforeUnwritable()
  {
    long bytes = channel.config().getWriteBufferHighWaterMark() - totalPendingSize;
    


    if (bytes > 0L) {
      return isWritable() ? bytes : 0L;
    }
    return 0L;
  }
  



  public long bytesBeforeWritable()
  {
    long bytes = totalPendingSize - channel.config().getWriteBufferLowWaterMark();
    


    if (bytes > 0L) {
      return isWritable() ? 0L : bytes;
    }
    return 0L;
  }
  



  public void forEachFlushedMessage(MessageProcessor processor)
    throws Exception
  {
    ObjectUtil.checkNotNull(processor, "processor");
    
    Entry entry = flushedEntry;
    if (entry == null) {
      return;
    }
    do
    {
      if ((!cancelled) && 
        (!processor.processMessage(msg))) {
        return;
      }
      
      entry = next;
    } while (isFlushedEntry(entry));
  }
  
  private boolean isFlushedEntry(Entry e) {
    return (e != null) && (e != unflushedEntry);
  }
  







  static final class Entry
  {
    private static final ObjectPool<Entry> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
    {
      public ChannelOutboundBuffer.Entry newObject(ObjectPool.Handle<ChannelOutboundBuffer.Entry> handle) {
        return new ChannelOutboundBuffer.Entry(handle, null);
      }
    });
    
    private final ObjectPool.Handle<Entry> handle;
    
    Entry next;
    
    Object msg;
    
    ByteBuffer[] bufs;
    
    ByteBuffer buf;
    
    ChannelPromise promise;
    long progress;
    long total;
    int pendingSize;
    int count = -1;
    boolean cancelled;
    
    private Entry(ObjectPool.Handle<Entry> handle) {
      this.handle = handle;
    }
    
    static Entry newInstance(Object msg, int size, long total, ChannelPromise promise) {
      Entry entry = (Entry)RECYCLER.get();
      msg = msg;
      pendingSize = (size + ChannelOutboundBuffer.CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD);
      total = total;
      promise = promise;
      return entry;
    }
    
    int cancel() {
      if (!cancelled) {
        cancelled = true;
        int pSize = pendingSize;
        

        ReferenceCountUtil.safeRelease(msg);
        msg = Unpooled.EMPTY_BUFFER;
        
        pendingSize = 0;
        total = 0L;
        progress = 0L;
        bufs = null;
        buf = null;
        return pSize;
      }
      return 0;
    }
    
    void recycle() {
      next = null;
      bufs = null;
      buf = null;
      msg = null;
      promise = null;
      progress = 0L;
      total = 0L;
      pendingSize = 0;
      count = -1;
      cancelled = false;
      handle.recycle(this);
    }
    
    Entry recycleAndGetNext() {
      Entry next = this.next;
      recycle();
      return next;
    }
  }
  
  public static abstract interface MessageProcessor
  {
    public abstract boolean processMessage(Object paramObject)
      throws Exception;
  }
}
