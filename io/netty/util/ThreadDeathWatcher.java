package io.netty.util;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;




























@Deprecated
public final class ThreadDeathWatcher
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadDeathWatcher.class);
  

  static final ThreadFactory threadFactory;
  

  private static final Queue<Entry> pendingEntries = new ConcurrentLinkedQueue();
  private static final Watcher watcher = new Watcher(null);
  private static final AtomicBoolean started = new AtomicBoolean();
  private static volatile Thread watcherThread;
  
  static {
    String poolName = "threadDeathWatcher";
    String serviceThreadPrefix = SystemPropertyUtil.get("io.netty.serviceThreadPrefix");
    if (!StringUtil.isNullOrEmpty(serviceThreadPrefix)) {
      poolName = serviceThreadPrefix + poolName;
    }
    


    threadFactory = new DefaultThreadFactory(poolName, true, 1, null);
  }
  







  public static void watch(Thread thread, Runnable task)
  {
    ObjectUtil.checkNotNull(thread, "thread");
    ObjectUtil.checkNotNull(task, "task");
    
    if (!thread.isAlive()) {
      throw new IllegalArgumentException("thread must be alive.");
    }
    
    schedule(thread, task, true);
  }
  


  public static void unwatch(Thread thread, Runnable task)
  {
    schedule((Thread)ObjectUtil.checkNotNull(thread, "thread"), 
      (Runnable)ObjectUtil.checkNotNull(task, "task"), false);
  }
  
  private static void schedule(Thread thread, Runnable task, boolean isWatch)
  {
    pendingEntries.add(new Entry(thread, task, isWatch));
    
    if (started.compareAndSet(false, true)) {
      Thread watcherThread = threadFactory.newThread(watcher);
      




      AccessController.doPrivileged(new PrivilegedAction()
      {
        public Void run() {
          val$watcherThread.setContextClassLoader(null);
          return null;
        }
        
      });
      watcherThread.start();
      watcherThread = watcherThread;
    }
  }
  







  public static boolean awaitInactivity(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    ObjectUtil.checkNotNull(unit, "unit");
    
    Thread watcherThread = watcherThread;
    if (watcherThread != null) {
      watcherThread.join(unit.toMillis(timeout));
      return !watcherThread.isAlive();
    }
    return true;
  }
  
  private ThreadDeathWatcher() {}
  
  private static final class Watcher
    implements Runnable
  {
    private final List<ThreadDeathWatcher.Entry> watchees = new ArrayList();
    
    private Watcher() {}
    
    public void run() {
      for (;;) { fetchWatchees();
        notifyWatchees();
        

        fetchWatchees();
        notifyWatchees();
        try
        {
          Thread.sleep(1000L);
        }
        catch (InterruptedException localInterruptedException) {}
        

        if ((watchees.isEmpty()) && (ThreadDeathWatcher.pendingEntries.isEmpty()))
        {



          boolean stopped = ThreadDeathWatcher.started.compareAndSet(true, false);
          assert (stopped);
          

          if (ThreadDeathWatcher.pendingEntries.isEmpty()) {
            break;
          }
          





          if (!ThreadDeathWatcher.started.compareAndSet(false, true)) {
            break;
          }
        }
      }
    }
    




    private void fetchWatchees()
    {
      for (;;)
      {
        ThreadDeathWatcher.Entry e = (ThreadDeathWatcher.Entry)ThreadDeathWatcher.pendingEntries.poll();
        if (e == null) {
          break;
        }
        
        if (isWatch) {
          watchees.add(e);
        } else {
          watchees.remove(e);
        }
      }
    }
    
    private void notifyWatchees() {
      List<ThreadDeathWatcher.Entry> watchees = this.watchees;
      for (int i = 0; i < watchees.size();) {
        ThreadDeathWatcher.Entry e = (ThreadDeathWatcher.Entry)watchees.get(i);
        if (!thread.isAlive()) {
          watchees.remove(i);
          try {
            task.run();
          } catch (Throwable t) {
            ThreadDeathWatcher.logger.warn("Thread death watcher task raised an exception:", t);
          }
        } else {
          i++;
        }
      }
    }
  }
  
  private static final class Entry {
    final Thread thread;
    final Runnable task;
    final boolean isWatch;
    
    Entry(Thread thread, Runnable task, boolean isWatch) {
      this.thread = thread;
      this.task = task;
      this.isWatch = isWatch;
    }
    
    public int hashCode()
    {
      return thread.hashCode() ^ task.hashCode();
    }
    
    public boolean equals(Object obj)
    {
      if (obj == this) {
        return true;
      }
      
      if (!(obj instanceof Entry)) {
        return false;
      }
      
      Entry that = (Entry)obj;
      return (thread == thread) && (task == task);
    }
  }
}
