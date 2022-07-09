package io.netty.util;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.MathUtil;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;


























public abstract class Recycler<T>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(Recycler.class);
  

  private static final Handle NOOP_HANDLE = new Handle()
  {
    public void recycle(Object object) {}
  };
  

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger(Integer.MIN_VALUE);
  private static final int OWN_THREAD_ID = ID_GENERATOR.getAndIncrement();
  
  private static final int DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD = 4096;
  
  private static final int DEFAULT_MAX_CAPACITY_PER_THREAD;
  
  private static final int INITIAL_CAPACITY;
  
  private static final int MAX_SHARED_CAPACITY_FACTOR;
  private static final int MAX_DELAYED_QUEUES_PER_THREAD;
  private static final int LINK_CAPACITY;
  
  static
  {
    int maxCapacityPerThread = SystemPropertyUtil.getInt("io.netty.recycler.maxCapacityPerThread", 
      SystemPropertyUtil.getInt("io.netty.recycler.maxCapacity", 4096));
    if (maxCapacityPerThread < 0) {
      maxCapacityPerThread = 4096;
    }
    
    DEFAULT_MAX_CAPACITY_PER_THREAD = maxCapacityPerThread;
    
    MAX_SHARED_CAPACITY_FACTOR = Math.max(2, 
      SystemPropertyUtil.getInt("io.netty.recycler.maxSharedCapacityFactor", 2));
    

    MAX_DELAYED_QUEUES_PER_THREAD = Math.max(0, 
      SystemPropertyUtil.getInt("io.netty.recycler.maxDelayedQueuesPerThread", 
      
      NettyRuntime.availableProcessors() * 2));
    
    LINK_CAPACITY = MathUtil.safeFindNextPositivePowerOfTwo(
      Math.max(SystemPropertyUtil.getInt("io.netty.recycler.linkCapacity", 16), 16));
    



    RATIO = Math.max(0, SystemPropertyUtil.getInt("io.netty.recycler.ratio", 8));
    DELAYED_QUEUE_RATIO = Math.max(0, SystemPropertyUtil.getInt("io.netty.recycler.delayedQueue.ratio", RATIO));
    
    INITIAL_CAPACITY = Math.min(DEFAULT_MAX_CAPACITY_PER_THREAD, 256);
    
    if (logger.isDebugEnabled())
      if (DEFAULT_MAX_CAPACITY_PER_THREAD == 0) {
        logger.debug("-Dio.netty.recycler.maxCapacityPerThread: disabled");
        logger.debug("-Dio.netty.recycler.maxSharedCapacityFactor: disabled");
        logger.debug("-Dio.netty.recycler.linkCapacity: disabled");
        logger.debug("-Dio.netty.recycler.ratio: disabled");
        logger.debug("-Dio.netty.recycler.delayedQueue.ratio: disabled");
      } else {
        logger.debug("-Dio.netty.recycler.maxCapacityPerThread: {}", Integer.valueOf(DEFAULT_MAX_CAPACITY_PER_THREAD));
        logger.debug("-Dio.netty.recycler.maxSharedCapacityFactor: {}", Integer.valueOf(MAX_SHARED_CAPACITY_FACTOR));
        logger.debug("-Dio.netty.recycler.linkCapacity: {}", Integer.valueOf(LINK_CAPACITY));
        logger.debug("-Dio.netty.recycler.ratio: {}", Integer.valueOf(RATIO));
        logger.debug("-Dio.netty.recycler.delayedQueue.ratio: {}", Integer.valueOf(DELAYED_QUEUE_RATIO));
      }
  }
  
  private static final int RATIO;
  private static final int DELAYED_QUEUE_RATIO;
  private final int maxCapacityPerThread;
  private final int maxSharedCapacityFactor;
  private final int interval;
  private final int maxDelayedQueuesPerThread;
  private final int delayedQueueInterval;
  private final FastThreadLocal<Stack<T>> threadLocal = new FastThreadLocal()
  {
    protected Recycler.Stack<T> initialValue() {
      return new Recycler.Stack(Recycler.this, Thread.currentThread(), maxCapacityPerThread, maxSharedCapacityFactor, 
        interval, maxDelayedQueuesPerThread, delayedQueueInterval);
    }
    

    protected void onRemoval(Recycler.Stack<T> value)
    {
      if ((threadRef.get() == Thread.currentThread()) && 
        (Recycler.DELAYED_RECYCLED.isSet())) {
        ((Map)Recycler.DELAYED_RECYCLED.get()).remove(value);
      }
    }
  };
  
  protected Recycler()
  {
    this(DEFAULT_MAX_CAPACITY_PER_THREAD);
  }
  
  protected Recycler(int maxCapacityPerThread) {
    this(maxCapacityPerThread, MAX_SHARED_CAPACITY_FACTOR);
  }
  
  protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor) {
    this(maxCapacityPerThread, maxSharedCapacityFactor, RATIO, MAX_DELAYED_QUEUES_PER_THREAD);
  }
  
  protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor, int ratio, int maxDelayedQueuesPerThread)
  {
    this(maxCapacityPerThread, maxSharedCapacityFactor, ratio, maxDelayedQueuesPerThread, DELAYED_QUEUE_RATIO);
  }
  

  protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor, int ratio, int maxDelayedQueuesPerThread, int delayedQueueRatio)
  {
    interval = Math.max(0, ratio);
    delayedQueueInterval = Math.max(0, delayedQueueRatio);
    if (maxCapacityPerThread <= 0) {
      this.maxCapacityPerThread = 0;
      this.maxSharedCapacityFactor = 1;
      this.maxDelayedQueuesPerThread = 0;
    } else {
      this.maxCapacityPerThread = maxCapacityPerThread;
      this.maxSharedCapacityFactor = Math.max(1, maxSharedCapacityFactor);
      this.maxDelayedQueuesPerThread = Math.max(0, maxDelayedQueuesPerThread);
    }
  }
  
  public final T get()
  {
    if (maxCapacityPerThread == 0) {
      return newObject(NOOP_HANDLE);
    }
    Stack<T> stack = (Stack)threadLocal.get();
    DefaultHandle<T> handle = stack.pop();
    if (handle == null) {
      handle = stack.newHandle();
      value = newObject(handle);
    }
    return value;
  }
  


  @Deprecated
  public final boolean recycle(T o, Handle<T> handle)
  {
    if (handle == NOOP_HANDLE) {
      return false;
    }
    
    DefaultHandle<T> h = (DefaultHandle)handle;
    if (stack.parent != this) {
      return false;
    }
    
    h.recycle(o);
    return true;
  }
  
  final int threadLocalCapacity() {
    return threadLocal.get()).elements.length;
  }
  

  final int threadLocalSize() { return threadLocal.get()).size; }
  
  protected abstract T newObject(Handle<T> paramHandle);
  
  public static abstract interface Handle<T> extends ObjectPool.Handle<T>
  {}
  
  private static final class DefaultHandle<T> implements Recycler.Handle<T> { private static final AtomicIntegerFieldUpdater<DefaultHandle<?>> LAST_RECYCLED_ID_UPDATER;
    volatile int lastRecycledId;
    int recycleId;
    
    static { AtomicIntegerFieldUpdater<?> updater = AtomicIntegerFieldUpdater.newUpdater(DefaultHandle.class, "lastRecycledId");
      
      LAST_RECYCLED_ID_UPDATER = updater;
    }
    

    boolean hasBeenRecycled;
    
    Recycler.Stack<?> stack;
    
    Object value;
    
    DefaultHandle(Recycler.Stack<?> stack)
    {
      this.stack = stack;
    }
    
    public void recycle(Object object)
    {
      if (object != value) {
        throw new IllegalArgumentException("object does not belong to handle");
      }
      
      Recycler.Stack<?> stack = this.stack;
      if ((lastRecycledId != recycleId) || (stack == null)) {
        throw new IllegalStateException("recycled already");
      }
      
      stack.push(this);
    }
    

    public boolean compareAndSetLastRecycledId(int expectLastRecycledId, int updateLastRecycledId)
    {
      return LAST_RECYCLED_ID_UPDATER.weakCompareAndSet(this, expectLastRecycledId, updateLastRecycledId);
    }
  }
  
  private static final FastThreadLocal<Map<Stack<?>, WeakOrderQueue>> DELAYED_RECYCLED = new FastThreadLocal()
  {
    protected Map<Recycler.Stack<?>, Recycler.WeakOrderQueue> initialValue()
    {
      return new WeakHashMap();
    }
  };
  

  private static final class WeakOrderQueue
    extends WeakReference<Thread>
  {
    static final WeakOrderQueue DUMMY = new WeakOrderQueue();
    private final Head head;
    private Link tail;
    private WeakOrderQueue next;
    
    static final class Link extends AtomicInteger { final Recycler.DefaultHandle<?>[] elements = new Recycler.DefaultHandle[Recycler.LINK_CAPACITY];
      int readIndex;
      Link next;
      
      Link() {}
    }
    
    private static final class Head
    {
      private final AtomicInteger availableSharedCapacity;
      Recycler.WeakOrderQueue.Link link;
      
      Head(AtomicInteger availableSharedCapacity) {
        this.availableSharedCapacity = availableSharedCapacity;
      }
      


      void reclaimAllSpaceAndUnlink()
      {
        Recycler.WeakOrderQueue.Link head = link;
        link = null;
        int reclaimSpace = 0;
        while (head != null) {
          reclaimSpace += Recycler.LINK_CAPACITY;
          Recycler.WeakOrderQueue.Link next = next;
          
          next = null;
          head = next;
        }
        if (reclaimSpace > 0) {
          reclaimSpace(reclaimSpace);
        }
      }
      
      private void reclaimSpace(int space) {
        availableSharedCapacity.addAndGet(space);
      }
      
      void relink(Recycler.WeakOrderQueue.Link link) {
        reclaimSpace(Recycler.LINK_CAPACITY);
        this.link = link;
      }
      



      Recycler.WeakOrderQueue.Link newLink()
      {
        return reserveSpaceForLink(availableSharedCapacity) ? new Recycler.WeakOrderQueue.Link() : null;
      }
      
      static boolean reserveSpaceForLink(AtomicInteger availableSharedCapacity) {
        for (;;) {
          int available = availableSharedCapacity.get();
          if (available < Recycler.LINK_CAPACITY) {
            return false;
          }
          if (availableSharedCapacity.compareAndSet(available, available - Recycler.LINK_CAPACITY)) {
            return true;
          }
        }
      }
    }
    





    private final int id = Recycler.ID_GENERATOR.getAndIncrement();
    private final int interval;
    private int handleRecycleCount;
    
    private WeakOrderQueue() {
      super();
      head = new Head(null);
      interval = 0;
    }
    
    private WeakOrderQueue(Recycler.Stack<?> stack, Thread thread) {
      super();
      tail = new Link();
      



      head = new Head(availableSharedCapacity);
      head.link = tail;
      interval = delayedQueueInterval;
      handleRecycleCount = interval;
    }
    
    static WeakOrderQueue newQueue(Recycler.Stack<?> stack, Thread thread)
    {
      if (!Head.reserveSpaceForLink(availableSharedCapacity)) {
        return null;
      }
      WeakOrderQueue queue = new WeakOrderQueue(stack, thread);
      

      stack.setHead(queue);
      
      return queue;
    }
    
    WeakOrderQueue getNext() {
      return next;
    }
    
    void setNext(WeakOrderQueue next) {
      assert (next != this);
      this.next = next;
    }
    
    void reclaimAllSpaceAndUnlink() {
      head.reclaimAllSpaceAndUnlink();
      next = null;
    }
    
    void add(Recycler.DefaultHandle<?> handle) {
      if (!handle.compareAndSetLastRecycledId(0, id))
      {

        return;
      }
      



      if (!hasBeenRecycled) {
        if (handleRecycleCount < interval) {
          handleRecycleCount += 1;
          
          return;
        }
        handleRecycleCount = 0;
      }
      
      Link tail = this.tail;
      int writeIndex;
      if ((writeIndex = tail.get()) == Recycler.LINK_CAPACITY) {
        Link link = head.newLink();
        if (link == null)
        {
          return;
        }
        
        this.tail = (tail = tail.next = link);
        
        writeIndex = tail.get();
      }
      elements[writeIndex] = handle;
      stack = null;
      

      tail.lazySet(writeIndex + 1);
    }
    
    boolean hasFinalData() {
      return tail.readIndex != tail.get();
    }
    

    boolean transfer(Recycler.Stack<?> dst)
    {
      Link head = headlink;
      if (head == null) {
        return false;
      }
      
      if (readIndex == Recycler.LINK_CAPACITY) {
        if (next == null) {
          return false;
        }
        head = next;
        this.head.relink(head);
      }
      
      int srcStart = readIndex;
      int srcEnd = head.get();
      int srcSize = srcEnd - srcStart;
      if (srcSize == 0) {
        return false;
      }
      
      int dstSize = size;
      int expectedCapacity = dstSize + srcSize;
      
      if (expectedCapacity > elements.length) {
        int actualCapacity = dst.increaseCapacity(expectedCapacity);
        srcEnd = Math.min(srcStart + actualCapacity - dstSize, srcEnd);
      }
      
      if (srcStart != srcEnd) {
        Recycler.DefaultHandle[] srcElems = elements;
        Recycler.DefaultHandle[] dstElems = elements;
        int newDstSize = dstSize;
        for (int i = srcStart; i < srcEnd; i++) {
          Recycler.DefaultHandle<?> element = srcElems[i];
          if (recycleId == 0) {
            recycleId = lastRecycledId;
          } else if (recycleId != lastRecycledId) {
            throw new IllegalStateException("recycled already");
          }
          srcElems[i] = null;
          
          if (!dst.dropHandle(element))
          {


            stack = dst;
            dstElems[(newDstSize++)] = element;
          }
        }
        if ((srcEnd == Recycler.LINK_CAPACITY) && (next != null))
        {
          this.head.relink(next);
        }
        
        readIndex = srcEnd;
        if (size == newDstSize) {
          return false;
        }
        size = newDstSize;
        return true;
      }
      
      return false;
    }
  }
  

  private static final class Stack<T>
  {
    final Recycler<T> parent;
    
    final WeakReference<Thread> threadRef;
    
    final AtomicInteger availableSharedCapacity;
    
    private final int maxDelayedQueues;
    
    private final int maxCapacity;
    
    private final int interval;
    
    private final int delayedQueueInterval;
    
    Recycler.DefaultHandle<?>[] elements;
    
    int size;
    
    private int handleRecycleCount;
    
    private Recycler.WeakOrderQueue cursor;
    
    private Recycler.WeakOrderQueue prev;
    private volatile Recycler.WeakOrderQueue head;
    
    Stack(Recycler<T> parent, Thread thread, int maxCapacity, int maxSharedCapacityFactor, int interval, int maxDelayedQueues, int delayedQueueInterval)
    {
      this.parent = parent;
      threadRef = new WeakReference(thread);
      this.maxCapacity = maxCapacity;
      availableSharedCapacity = new AtomicInteger(Math.max(maxCapacity / maxSharedCapacityFactor, Recycler.LINK_CAPACITY));
      elements = new Recycler.DefaultHandle[Math.min(Recycler.INITIAL_CAPACITY, maxCapacity)];
      this.interval = interval;
      this.delayedQueueInterval = delayedQueueInterval;
      handleRecycleCount = interval;
      this.maxDelayedQueues = maxDelayedQueues;
    }
    
    synchronized void setHead(Recycler.WeakOrderQueue queue)
    {
      queue.setNext(head);
      head = queue;
    }
    
    int increaseCapacity(int expectedCapacity) {
      int newCapacity = elements.length;
      int maxCapacity = this.maxCapacity;
      do {
        newCapacity <<= 1;
      } while ((newCapacity < expectedCapacity) && (newCapacity < maxCapacity));
      
      newCapacity = Math.min(newCapacity, maxCapacity);
      if (newCapacity != elements.length) {
        elements = ((Recycler.DefaultHandle[])Arrays.copyOf(elements, newCapacity));
      }
      
      return newCapacity;
    }
    
    Recycler.DefaultHandle<T> pop()
    {
      int size = this.size;
      if (size == 0) {
        if (!scavenge()) {
          return null;
        }
        size = this.size;
        if (size <= 0)
        {
          return null;
        }
      }
      size--;
      Recycler.DefaultHandle ret = elements[size];
      elements[size] = null;
      


      this.size = size;
      
      if (lastRecycledId != recycleId) {
        throw new IllegalStateException("recycled multiple times");
      }
      recycleId = 0;
      lastRecycledId = 0;
      return ret;
    }
    
    private boolean scavenge()
    {
      if (scavengeSome()) {
        return true;
      }
      

      prev = null;
      cursor = head;
      return false;
    }
    
    private boolean scavengeSome()
    {
      Recycler.WeakOrderQueue cursor = this.cursor;
      Recycler.WeakOrderQueue prev; if (cursor == null) {
        Recycler.WeakOrderQueue prev = null;
        cursor = head;
        if (cursor == null) {
          return false;
        }
      } else {
        prev = this.prev;
      }
      
      boolean success = false;
      do {
        if (cursor.transfer(this)) {
          success = true;
          break;
        }
        Recycler.WeakOrderQueue next = cursor.getNext();
        if (cursor.get() == null)
        {


          if (cursor.hasFinalData())
          {
            while (cursor.transfer(this)) {
              success = true;
            }
          }
          



          if (prev != null)
          {
            cursor.reclaimAllSpaceAndUnlink();
            prev.setNext(next);
          }
        } else {
          prev = cursor;
        }
        
        cursor = next;
      }
      while ((cursor != null) && (!success));
      
      this.prev = prev;
      this.cursor = cursor;
      return success;
    }
    
    void push(Recycler.DefaultHandle<?> item) {
      Thread currentThread = Thread.currentThread();
      if (threadRef.get() == currentThread)
      {
        pushNow(item);

      }
      else
      {
        pushLater(item, currentThread);
      }
    }
    
    private void pushNow(Recycler.DefaultHandle<?> item) {
      if ((recycleId != 0) || (!item.compareAndSetLastRecycledId(0, Recycler.OWN_THREAD_ID))) {
        throw new IllegalStateException("recycled already");
      }
      recycleId = Recycler.OWN_THREAD_ID;
      
      int size = this.size;
      if ((size >= maxCapacity) || (dropHandle(item)))
      {
        return;
      }
      if (size == elements.length) {
        elements = ((Recycler.DefaultHandle[])Arrays.copyOf(elements, Math.min(size << 1, maxCapacity)));
      }
      
      elements[size] = item;
      this.size = (size + 1);
    }
    
    private void pushLater(Recycler.DefaultHandle<?> item, Thread thread) {
      if (maxDelayedQueues == 0)
      {
        return;
      }
      



      Map<Stack<?>, Recycler.WeakOrderQueue> delayedRecycled = (Map)Recycler.DELAYED_RECYCLED.get();
      Recycler.WeakOrderQueue queue = (Recycler.WeakOrderQueue)delayedRecycled.get(this);
      if (queue == null) {
        if (delayedRecycled.size() >= maxDelayedQueues)
        {
          delayedRecycled.put(this, Recycler.WeakOrderQueue.DUMMY);
          return;
        }
        
        if ((queue = newWeakOrderQueue(thread)) == null)
        {
          return;
        }
        delayedRecycled.put(this, queue);
      } else if (queue == Recycler.WeakOrderQueue.DUMMY)
      {
        return;
      }
      
      queue.add(item);
    }
    


    private Recycler.WeakOrderQueue newWeakOrderQueue(Thread thread)
    {
      return Recycler.WeakOrderQueue.newQueue(this, thread);
    }
    
    boolean dropHandle(Recycler.DefaultHandle<?> handle) {
      if (!hasBeenRecycled) {
        if (handleRecycleCount < interval) {
          handleRecycleCount += 1;
          
          return true;
        }
        handleRecycleCount = 0;
        hasBeenRecycled = true;
      }
      return false;
    }
    
    Recycler.DefaultHandle<T> newHandle() {
      return new Recycler.DefaultHandle(this);
    }
  }
}
