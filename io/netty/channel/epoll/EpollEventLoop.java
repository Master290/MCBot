package io.netty.channel.epoll;

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
import java.util.concurrent.atomic.AtomicLong;













class EpollEventLoop
  extends SingleThreadEventLoop
{
  private static final InternalLogger logger;
  private final FileDescriptor epollFd;
  private final FileDescriptor eventFd;
  private final FileDescriptor timerFd;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(EpollEventLoop.class);
    



    Epoll.ensureAvailability();
  }
  



  private final IntObjectMap<AbstractEpollChannel> channels = new IntObjectHashMap(4096);
  
  private final boolean allowGrowing;
  
  private final EpollEventArray events;
  
  private IovArray iovArray;
  private NativeDatagramPacketArray datagramPacketArray;
  private final SelectStrategy selectStrategy;
  private final IntSupplier selectNowSupplier = new IntSupplier()
  {
    public int get() throws Exception {
      return EpollEventLoop.this.epollWaitNow();
    }
  };
  

  private static final long AWAKE = -1L;
  

  private static final long NONE = Long.MAX_VALUE;
  

  private final AtomicLong nextWakeupNanos = new AtomicLong(-1L);
  private boolean pendingWakeup;
  private volatile int ioRatio = 50;
  

  private static final long MAX_SCHEDULED_TIMERFD_NS = 999999999L;
  

  EpollEventLoop(EventLoopGroup parent, Executor executor, int maxEvents, SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory, EventLoopTaskQueueFactory tailTaskQueueFactory)
  {
    super(parent, executor, false, newTaskQueue(taskQueueFactory), newTaskQueue(tailTaskQueueFactory), rejectedExecutionHandler);
    
    selectStrategy = ((SelectStrategy)ObjectUtil.checkNotNull(strategy, "strategy"));
    if (maxEvents == 0) {
      allowGrowing = true;
      events = new EpollEventArray(4096);
    } else {
      allowGrowing = false;
      events = new EpollEventArray(maxEvents);
    }
    boolean success = false;
    FileDescriptor epollFd = null;
    FileDescriptor eventFd = null;
    FileDescriptor timerFd = null;
    try {
      this.epollFd = (epollFd = Native.newEpollCreate());
      this.eventFd = (eventFd = Native.newEventFd());
      
      try
      {
        Native.epollCtlAdd(epollFd.intValue(), eventFd.intValue(), Native.EPOLLIN | Native.EPOLLET);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to add eventFd filedescriptor to epoll", e);
      }
      this.timerFd = (timerFd = Native.newTimerFd());
      
      try
      {
        Native.epollCtlAdd(epollFd.intValue(), timerFd.intValue(), Native.EPOLLIN | Native.EPOLLET);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to add timerFd filedescriptor to epoll", e);
      }
      success = true; return;
    } finally {
      if (!success) {
        if (epollFd != null) {
          try {
            epollFd.close();
          }
          catch (Exception localException3) {}
        }
        
        if (eventFd != null) {
          try {
            eventFd.close();
          }
          catch (Exception localException4) {}
        }
        
        if (timerFd != null) {
          try {
            timerFd.close();
          }
          catch (Exception localException5) {}
        }
      }
    }
  }
  

  private static Queue<Runnable> newTaskQueue(EventLoopTaskQueueFactory queueFactory)
  {
    if (queueFactory == null) {
      return newTaskQueue0(DEFAULT_MAX_PENDING_TASKS);
    }
    return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
  }
  


  IovArray cleanIovArray()
  {
    if (iovArray == null) {
      iovArray = new IovArray();
    } else {
      iovArray.clear();
    }
    return iovArray;
  }
  


  NativeDatagramPacketArray cleanDatagramPacketArray()
  {
    if (datagramPacketArray == null) {
      datagramPacketArray = new NativeDatagramPacketArray();
    } else {
      datagramPacketArray.clear();
    }
    return datagramPacketArray;
  }
  
  protected void wakeup(boolean inEventLoop)
  {
    if ((!inEventLoop) && (nextWakeupNanos.getAndSet(-1L) != -1L))
    {
      Native.eventFdWrite(eventFd.intValue(), 1L);
    }
  }
  

  protected boolean beforeScheduledTaskSubmitted(long deadlineNanos)
  {
    return deadlineNanos < nextWakeupNanos.get();
  }
  

  protected boolean afterScheduledTaskSubmitted(long deadlineNanos)
  {
    return deadlineNanos < nextWakeupNanos.get();
  }
  

  void add(AbstractEpollChannel ch)
    throws IOException
  {
    assert (inEventLoop());
    int fd = socket.intValue();
    Native.epollCtlAdd(epollFd.intValue(), fd, flags);
    AbstractEpollChannel old = (AbstractEpollChannel)channels.put(fd, ch);
    


    assert ((old == null) || (!old.isOpen()));
  }
  

  void modify(AbstractEpollChannel ch)
    throws IOException
  {
    assert (inEventLoop());
    Native.epollCtlMod(epollFd.intValue(), socket.intValue(), flags);
  }
  

  void remove(AbstractEpollChannel ch)
    throws IOException
  {
    assert (inEventLoop());
    int fd = socket.intValue();
    
    AbstractEpollChannel old = (AbstractEpollChannel)channels.remove(fd);
    if ((old != null) && (old != ch))
    {
      channels.put(fd, old);
      

      if ((!$assertionsDisabled) && (ch.isOpen())) throw new AssertionError();
    } else if (ch.isOpen())
    {

      Native.epollCtlDel(epollFd.intValue(), fd);
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
  
  private int epollWait(long deadlineNanos) throws IOException {
    if (deadlineNanos == Long.MAX_VALUE) {
      return Native.epollWait(epollFd, events, timerFd, Integer.MAX_VALUE, 0);
    }
    long totalDelay = deadlineToDelayNanos(deadlineNanos);
    int delaySeconds = (int)Math.min(totalDelay / 1000000000L, 2147483647L);
    int delayNanos = (int)Math.min(totalDelay - delaySeconds * 1000000000L, 999999999L);
    return Native.epollWait(epollFd, events, timerFd, delaySeconds, delayNanos);
  }
  
  private int epollWaitNoTimerChange() throws IOException {
    return Native.epollWait(epollFd, events, false);
  }
  
  private int epollWaitNow() throws IOException {
    return Native.epollWait(epollFd, events, true);
  }
  
  private int epollBusyWait() throws IOException {
    return Native.epollBusyWait(epollFd, events);
  }
  
  private int epollWaitTimeboxed() throws IOException
  {
    return Native.epollWait(epollFd, events, 1000);
  }
  
  protected void run()
  {
    long prevDeadlineNanos = Long.MAX_VALUE;
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
          strategy = epollBusyWait();
          break;
        
        case -1: 
          if (pendingWakeup)
          {

            strategy = epollWaitTimeboxed();
            if (strategy == 0)
            {



              logger.warn("Missed eventfd write (not seen after > 1 second)");
              pendingWakeup = false;
              if (hasTasks()) {}
            }
            
          }
          else
          {
            long curDeadlineNanos = nextScheduledTaskDeadlineNanos();
            if (curDeadlineNanos == -1L) {
              curDeadlineNanos = Long.MAX_VALUE;
            }
            nextWakeupNanos.set(curDeadlineNanos);
            try {
              if (!hasTasks()) {
                if (curDeadlineNanos == prevDeadlineNanos)
                {
                  strategy = epollWaitNoTimerChange();
                }
                else {
                  prevDeadlineNanos = curDeadlineNanos;
                  strategy = epollWait(curDeadlineNanos);
                }
              }
            }
            finally
            {
              if ((nextWakeupNanos.get() == -1L) || (nextWakeupNanos.getAndSet(-1L) == -1L)) {
                pendingWakeup = true;
              }
            }
          }
        

        default: 
          int ioRatio = this.ioRatio;
          if (ioRatio == 100) {
            try {
              if ((strategy > 0) && (processReady(events, strategy))) {
                prevDeadlineNanos = Long.MAX_VALUE;
              }
            }
            finally {
              runAllTasks();
            }
          } else if (strategy > 0) {
            long ioStartTime = System.nanoTime();
            try {
              if (processReady(events, strategy)) {
                prevDeadlineNanos = Long.MAX_VALUE;
              }
            } finally {
              long ioTime;
              long ioTime = System.nanoTime() - ioStartTime;
              runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
            }
          } else {
            runAllTasks(0L);
          }
          if ((allowGrowing) && (strategy == events.length()))
          {
            events.increase();
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
  



  void handleLoopException(Throwable t)
  {
    logger.warn("Unexpected exception in the selector loop.", t);
    

    try
    {
      Thread.sleep(1000L);
    }
    catch (InterruptedException localInterruptedException) {}
  }
  


  private void closeAll()
  {
    AbstractEpollChannel[] localChannels = (AbstractEpollChannel[])channels.values().toArray(new AbstractEpollChannel[0]);
    
    for (AbstractEpollChannel ch : localChannels) {
      ch.unsafe().close(ch.unsafe().voidPromise());
    }
  }
  
  private boolean processReady(EpollEventArray events, int ready)
  {
    boolean timerFired = false;
    for (int i = 0; i < ready; i++) {
      int fd = events.fd(i);
      if (fd == eventFd.intValue()) {
        pendingWakeup = false;
      } else if (fd == timerFd.intValue()) {
        timerFired = true;
      } else {
        long ev = events.events(i);
        
        AbstractEpollChannel ch = (AbstractEpollChannel)channels.get(fd);
        if (ch != null)
        {



          AbstractEpollChannel.AbstractEpollUnsafe unsafe = (AbstractEpollChannel.AbstractEpollUnsafe)ch.unsafe();
          








          if ((ev & (Native.EPOLLERR | Native.EPOLLOUT)) != 0L)
          {
            unsafe.epollOutReady();
          }
          





          if ((ev & (Native.EPOLLERR | Native.EPOLLIN)) != 0L)
          {
            unsafe.epollInReady();
          }
          



          if ((ev & Native.EPOLLRDHUP) != 0L) {
            unsafe.epollRdHupReady();
          }
        }
        else {
          try {
            Native.epollCtlDel(epollFd.intValue(), fd);
          }
          catch (IOException localIOException) {}
        }
      }
    }
    



    return timerFired;
  }
  
  protected void cleanup()
  {
    try
    {
      while (pendingWakeup) {
        try {
          int count = epollWaitTimeboxed();
          if (count == 0) {
            break;
          }
          
          for (int i = 0; i < count; i++) {
            if (events.fd(i) == eventFd.intValue()) {
              pendingWakeup = false;
              break;
            }
          }
        }
        catch (IOException localIOException1) {}
      }
      try
      {
        eventFd.close();
      } catch (IOException e) {
        logger.warn("Failed to close the event fd.", e);
      }
      try {
        timerFd.close();
      } catch (IOException e) {
        logger.warn("Failed to close the timer fd.", e);
      }
      try
      {
        epollFd.close();
      } catch (IOException e) {
        logger.warn("Failed to close the epoll fd.", e);
      }
      

      if (iovArray != null) {
        iovArray.release();
        iovArray = null;
      }
      if (datagramPacketArray != null) {
        datagramPacketArray.release();
        datagramPacketArray = null;
      }
      events.free();
    }
    finally
    {
      if (iovArray != null) {
        iovArray.release();
        iovArray = null;
      }
      if (datagramPacketArray != null) {
        datagramPacketArray.release();
        datagramPacketArray = null;
      }
      events.free();
    }
  }
}
