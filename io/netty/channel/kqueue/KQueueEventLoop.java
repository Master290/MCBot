package io.netty.channel.kqueue;

import io.netty.channel.Channel.Unsafe;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.SelectStrategy;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.IovArray;
import io.netty.util.IntSupplier;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;









final class KQueueEventLoop
  extends SingleThreadEventLoop
{
  private static final InternalLogger logger;
  private static final AtomicIntegerFieldUpdater<KQueueEventLoop> WAKEN_UP_UPDATER;
  private static final int KQUEUE_WAKE_UP_IDENT = 0;
  private final boolean allowGrowing;
  private final FileDescriptor kqueueFd;
  private final KQueueEventArray changeList;
  private final KQueueEventArray eventList;
  private final SelectStrategy selectStrategy;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(KQueueEventLoop.class);
    
    WAKEN_UP_UPDATER = AtomicIntegerFieldUpdater.newUpdater(KQueueEventLoop.class, "wakenUp");
    




    KQueue.ensureAvailability();
  }
  





  private final IovArray iovArray = new IovArray();
  private final IntSupplier selectNowSupplier = new IntSupplier()
  {
    public int get() throws Exception {
      return KQueueEventLoop.this.kqueueWaitNow();
    }
  };
  private final IntObjectMap<AbstractKQueueChannel> channels = new IntObjectHashMap(4096);
  
  private volatile int wakenUp;
  private volatile int ioRatio = 50;
  

  KQueueEventLoop(EventLoopGroup parent, Executor executor, int maxEvents, SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory, EventLoopTaskQueueFactory tailTaskQueueFactory)
  {
    super(parent, executor, false, newTaskQueue(taskQueueFactory), newTaskQueue(tailTaskQueueFactory), rejectedExecutionHandler);
    
    selectStrategy = ((SelectStrategy)ObjectUtil.checkNotNull(strategy, "strategy"));
    kqueueFd = Native.newKQueue();
    if (maxEvents == 0) {
      allowGrowing = true;
      maxEvents = 4096;
    } else {
      allowGrowing = false;
    }
    changeList = new KQueueEventArray(maxEvents);
    eventList = new KQueueEventArray(maxEvents);
    int result = Native.keventAddUserEvent(kqueueFd.intValue(), 0);
    if (result < 0) {
      cleanup();
      throw new IllegalStateException("kevent failed to add user event with errno: " + -result);
    }
  }
  
  private static Queue<Runnable> newTaskQueue(EventLoopTaskQueueFactory queueFactory)
  {
    if (queueFactory == null) {
      return newTaskQueue0(DEFAULT_MAX_PENDING_TASKS);
    }
    return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
  }
  
  void add(AbstractKQueueChannel ch) {
    assert (inEventLoop());
    AbstractKQueueChannel old = (AbstractKQueueChannel)channels.put(ch.fd().intValue(), ch);
    

    assert ((old == null) || (!old.isOpen()));
  }
  
  void evSet(AbstractKQueueChannel ch, short filter, short flags, int fflags) {
    assert (inEventLoop());
    changeList.evSet(ch, filter, flags, fflags);
  }
  
  void remove(AbstractKQueueChannel ch) throws Exception {
    assert (inEventLoop());
    int fd = ch.fd().intValue();
    
    AbstractKQueueChannel old = (AbstractKQueueChannel)channels.remove(fd);
    if ((old != null) && (old != ch))
    {
      channels.put(fd, old);
      

      if ((!$assertionsDisabled) && (ch.isOpen())) throw new AssertionError();
    } else if (ch.isOpen())
    {



      ch.unregisterFilters();
    }
  }
  


  IovArray cleanArray()
  {
    iovArray.clear();
    return iovArray;
  }
  
  protected void wakeup(boolean inEventLoop)
  {
    if ((!inEventLoop) && (WAKEN_UP_UPDATER.compareAndSet(this, 0, 1))) {
      wakeup();
    }
  }
  
  private void wakeup() {
    Native.keventTriggerUserEvent(kqueueFd.intValue(), 0);
  }
  




  private int kqueueWait(boolean oldWakeup)
    throws IOException
  {
    if ((oldWakeup) && (hasTasks())) {
      return kqueueWaitNow();
    }
    
    long totalDelay = delayNanos(System.nanoTime());
    int delaySeconds = (int)Math.min(totalDelay / 1000000000L, 2147483647L);
    return kqueueWait(delaySeconds, (int)Math.min(totalDelay - delaySeconds * 1000000000L, 2147483647L));
  }
  
  private int kqueueWaitNow() throws IOException {
    return kqueueWait(0, 0);
  }
  
  private int kqueueWait(int timeoutSec, int timeoutNs) throws IOException {
    int numEvents = Native.keventWait(kqueueFd.intValue(), changeList, eventList, timeoutSec, timeoutNs);
    changeList.clear();
    return numEvents;
  }
  
  private void processReady(int ready) {
    for (int i = 0; i < ready; i++) {
      short filter = eventList.filter(i);
      short flags = eventList.flags(i);
      int fd = eventList.fd(i);
      if ((filter == Native.EVFILT_USER) || ((flags & Native.EV_ERROR) != 0))
      {

        if ((!$assertionsDisabled) && (filter == Native.EVFILT_USER) && ((filter != Native.EVFILT_USER) || (fd != 0))) { throw new AssertionError();
        }
      }
      else
      {
        AbstractKQueueChannel channel = (AbstractKQueueChannel)channels.get(fd);
        if (channel == null)
        {


          logger.warn("events[{}]=[{}, {}] had no channel!", new Object[] { Integer.valueOf(i), Integer.valueOf(eventList.fd(i)), Short.valueOf(filter) });
        }
        else
        {
          AbstractKQueueChannel.AbstractKQueueUnsafe unsafe = (AbstractKQueueChannel.AbstractKQueueUnsafe)channel.unsafe();
          

          if (filter == Native.EVFILT_WRITE) {
            unsafe.writeReady();
          } else if (filter == Native.EVFILT_READ)
          {
            unsafe.readReady(eventList.data(i));
          } else if ((filter == Native.EVFILT_SOCK) && ((eventList.fflags(i) & Native.NOTE_RDHUP) != 0)) {
            unsafe.readEOF();
          }
          



          if ((flags & Native.EV_EOF) != 0)
            unsafe.readEOF();
        }
      }
    }
  }
  
  protected void run() {
    try {
      for (;;) {
        int strategy = selectStrategy.calculateStrategy(selectNowSupplier, hasTasks());
        switch (strategy)
        {




































        case -2: 
          try
          {




































            if (isShuttingDown()) {
              closeAll();
              if (confirmShutdown()) {
                return;
              }
            }
          } catch (Error e) {
            throw e;




























































































































































          }
          catch (Throwable t)
          {




























































































































































            handleLoopException(t); }
          break;
        case -3: 
        case -1: 
          strategy = kqueueWait(WAKEN_UP_UPDATER.getAndSet(this, 0) == 1);
          




























          if (wakenUp == 1) {
            wakeup();
          }
        


        default: 
          int ioRatio = this.ioRatio;
          if (ioRatio == 100) {
            try {
              if (strategy > 0) {
                processReady(strategy);
              }
              
              runAllTasks(); } finally { runAllTasks();
            }
          }
          long ioStartTime = System.nanoTime();
          try
          {
            if (strategy > 0)
              processReady(strategy);
          } finally {
            long ioTime;
            long ioTime = System.nanoTime() - ioStartTime;
            runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
          }
          
          if ((allowGrowing) && (strategy == eventList.capacity()))
          {
            eventList.realloc(false);
          }
          




          try
          {
            if (isShuttingDown()) {
              closeAll();
              if (confirmShutdown()) {
                return;
              }
            }
          } catch (Error e) {
            throw e;




























































































































































          }
          catch (Throwable t)
          {




























































































































































            handleLoopException(t);
          }
        }
        
      }
    }
    catch (Error e)
    {
      throw e;
    } catch (Throwable t) {
      handleLoopException(t);
    }
    finally {
      try {
        if (isShuttingDown()) {
          closeAll();
          if (confirmShutdown()) {
            return;
          }
        }
      } catch (Error e) {
        throw e;
      } catch (Throwable t) {
        handleLoopException(t);
      }
    }
  }
  

  protected Queue<Runnable> newTaskQueue(int maxPendingTasks)
  {
    return newTaskQueue0(maxPendingTasks);
  }
  
  private static Queue<Runnable> newTaskQueue0(int maxPendingTasks)
  {
    return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.newMpscQueue() : 
      PlatformDependent.newMpscQueue(maxPendingTasks);
  }
  


  public int getIoRatio()
  {
    return ioRatio;
  }
  



  public void setIoRatio(int ioRatio)
  {
    if ((ioRatio <= 0) || (ioRatio > 100)) {
      throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
    }
    this.ioRatio = ioRatio;
  }
  
  public int registeredChannels()
  {
    return channels.size();
  }
  
  protected void cleanup()
  {
    try {
      try {
        kqueueFd.close();
      } catch (IOException e) {
        logger.warn("Failed to close the kqueue fd.", e);
      }
      

      changeList.free();
      eventList.free();
    }
    finally
    {
      changeList.free();
      eventList.free();
    }
  }
  
  private void closeAll() {
    try {
      kqueueWaitNow();
    }
    catch (IOException localIOException) {}
    



    AbstractKQueueChannel[] localChannels = (AbstractKQueueChannel[])channels.values().toArray(new AbstractKQueueChannel[0]);
    
    for (AbstractKQueueChannel ch : localChannels) {
      ch.unsafe().close(ch.unsafe().voidPromise());
    }
  }
  
  private static void handleLoopException(Throwable t) {
    logger.warn("Unexpected exception in the selector loop.", t);
    

    try
    {
      Thread.sleep(1000L);
    }
    catch (InterruptedException localInterruptedException) {}
  }
}
