package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
















public class DefaultPromise<V>
  extends AbstractFuture<V>
  implements Promise<V>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultPromise.class);
  
  private static final InternalLogger rejectedExecutionLogger = InternalLoggerFactory.getInstance(DefaultPromise.class.getName() + ".rejectedExecution");
  private static final int MAX_LISTENER_STACK_DEPTH = Math.min(8, 
    SystemPropertyUtil.getInt("io.netty.defaultPromise.maxListenerStackDepth", 8));
  

  private static final AtomicReferenceFieldUpdater<DefaultPromise, Object> RESULT_UPDATER = AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Object.class, "result");
  private static final Object SUCCESS = new Object();
  private static final Object UNCANCELLABLE = new Object();
  private static final CauseHolder CANCELLATION_CAUSE_HOLDER = new CauseHolder(
    StacklessCancellationException.newInstance(DefaultPromise.class, "cancel(...)"));
  private static final StackTraceElement[] CANCELLATION_STACK = CANCELLATION_CAUSE_HOLDERcause.getStackTrace();
  




  private volatile Object result;
  




  private final EventExecutor executor;
  



  private Object listeners;
  



  private short waiters;
  



  private boolean notifyingListeners;
  




  public DefaultPromise(EventExecutor executor)
  {
    this.executor = ((EventExecutor)ObjectUtil.checkNotNull(executor, "executor"));
  }
  



  protected DefaultPromise()
  {
    executor = null;
  }
  
  public Promise<V> setSuccess(V result)
  {
    if (setSuccess0(result)) {
      return this;
    }
    throw new IllegalStateException("complete already: " + this);
  }
  
  public boolean trySuccess(V result)
  {
    return setSuccess0(result);
  }
  
  public Promise<V> setFailure(Throwable cause)
  {
    if (setFailure0(cause)) {
      return this;
    }
    throw new IllegalStateException("complete already: " + this, cause);
  }
  
  public boolean tryFailure(Throwable cause)
  {
    return setFailure0(cause);
  }
  
  public boolean setUncancellable()
  {
    if (RESULT_UPDATER.compareAndSet(this, null, UNCANCELLABLE)) {
      return true;
    }
    Object result = this.result;
    return (!isDone0(result)) || (!isCancelled0(result));
  }
  
  public boolean isSuccess()
  {
    Object result = this.result;
    return (result != null) && (result != UNCANCELLABLE) && (!(result instanceof CauseHolder));
  }
  
  public boolean isCancellable()
  {
    return result == null;
  }
  
  private static final class LeanCancellationException extends CancellationException {
    private static final long serialVersionUID = 2794674970981187807L;
    
    private LeanCancellationException() {}
    
    public Throwable fillInStackTrace() {
      setStackTrace(DefaultPromise.CANCELLATION_STACK);
      return this;
    }
    
    public String toString()
    {
      return CancellationException.class.getName();
    }
  }
  
  public Throwable cause()
  {
    return cause0(result);
  }
  
  private Throwable cause0(Object result) {
    if (!(result instanceof CauseHolder)) {
      return null;
    }
    if (result == CANCELLATION_CAUSE_HOLDER) {
      CancellationException ce = new LeanCancellationException(null);
      if (RESULT_UPDATER.compareAndSet(this, CANCELLATION_CAUSE_HOLDER, new CauseHolder(ce))) {
        return ce;
      }
      result = this.result;
    }
    return cause;
  }
  
  public Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener)
  {
    ObjectUtil.checkNotNull(listener, "listener");
    
    synchronized (this) {
      addListener0(listener);
    }
    
    if (isDone()) {
      notifyListeners();
    }
    
    return this;
  }
  
  public Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners)
  {
    ObjectUtil.checkNotNull(listeners, "listeners");
    
    synchronized (this) {
      for (GenericFutureListener<? extends Future<? super V>> listener : listeners) {
        if (listener == null) {
          break;
        }
        addListener0(listener);
      }
    }
    
    if (isDone()) {
      notifyListeners();
    }
    
    return this;
  }
  
  public Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener)
  {
    ObjectUtil.checkNotNull(listener, "listener");
    
    synchronized (this) {
      removeListener0(listener);
    }
    
    return this;
  }
  
  public Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners)
  {
    ObjectUtil.checkNotNull(listeners, "listeners");
    
    synchronized (this) {
      for (GenericFutureListener<? extends Future<? super V>> listener : listeners) {
        if (listener == null) {
          break;
        }
        removeListener0(listener);
      }
    }
    
    return this;
  }
  
  public Promise<V> await() throws InterruptedException
  {
    if (isDone()) {
      return this;
    }
    
    if (Thread.interrupted()) {
      throw new InterruptedException(toString());
    }
    
    checkDeadLock();
    
    synchronized (this) {
      if (!isDone()) {
        incWaiters();
        try {
          wait();
          
          decWaiters(); } finally { decWaiters();
        }
      }
    }
    return this;
  }
  
  public Promise<V> awaitUninterruptibly()
  {
    if (isDone()) {
      return this;
    }
    
    checkDeadLock();
    
    boolean interrupted = false;
    synchronized (this) {
      while (!isDone()) {
        incWaiters();
        try {
          wait();
        }
        catch (InterruptedException e) {
          interrupted = true;
        } finally {
          decWaiters();
        }
      }
    }
    
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    
    return this;
  }
  
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException
  {
    return await0(unit.toNanos(timeout), true);
  }
  
  public boolean await(long timeoutMillis) throws InterruptedException
  {
    return await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), true);
  }
  
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit)
  {
    try {
      return await0(unit.toNanos(timeout), false);
    }
    catch (InterruptedException e) {
      throw new InternalError();
    }
  }
  
  public boolean awaitUninterruptibly(long timeoutMillis)
  {
    try {
      return await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), false);
    }
    catch (InterruptedException e) {
      throw new InternalError();
    }
  }
  

  public V getNow()
  {
    Object result = this.result;
    if (((result instanceof CauseHolder)) || (result == SUCCESS) || (result == UNCANCELLABLE)) {
      return null;
    }
    return result;
  }
  
  public V get()
    throws InterruptedException, ExecutionException
  {
    Object result = this.result;
    if (!isDone0(result)) {
      await();
      result = this.result;
    }
    if ((result == SUCCESS) || (result == UNCANCELLABLE)) {
      return null;
    }
    Throwable cause = cause0(result);
    if (cause == null) {
      return result;
    }
    if ((cause instanceof CancellationException)) {
      throw ((CancellationException)cause);
    }
    throw new ExecutionException(cause);
  }
  
  public V get(long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    Object result = this.result;
    if (!isDone0(result)) {
      if (!await(timeout, unit)) {
        throw new TimeoutException();
      }
      result = this.result;
    }
    if ((result == SUCCESS) || (result == UNCANCELLABLE)) {
      return null;
    }
    Throwable cause = cause0(result);
    if (cause == null) {
      return result;
    }
    if ((cause instanceof CancellationException)) {
      throw ((CancellationException)cause);
    }
    throw new ExecutionException(cause);
  }
  





  public boolean cancel(boolean mayInterruptIfRunning)
  {
    if (RESULT_UPDATER.compareAndSet(this, null, CANCELLATION_CAUSE_HOLDER)) {
      if (checkNotifyWaiters()) {
        notifyListeners();
      }
      return true;
    }
    return false;
  }
  
  public boolean isCancelled()
  {
    return isCancelled0(result);
  }
  
  public boolean isDone()
  {
    return isDone0(result);
  }
  
  public Promise<V> sync() throws InterruptedException
  {
    await();
    rethrowIfFailed();
    return this;
  }
  
  public Promise<V> syncUninterruptibly()
  {
    awaitUninterruptibly();
    rethrowIfFailed();
    return this;
  }
  
  public String toString()
  {
    return toStringBuilder().toString();
  }
  


  protected StringBuilder toStringBuilder()
  {
    StringBuilder buf = new StringBuilder(64).append(StringUtil.simpleClassName(this)).append('@').append(Integer.toHexString(hashCode()));
    
    Object result = this.result;
    if (result == SUCCESS) {
      buf.append("(success)");
    } else if (result == UNCANCELLABLE) {
      buf.append("(uncancellable)");
    } else if ((result instanceof CauseHolder))
    {

      buf.append("(failure: ").append(cause).append(')');
    } else if (result != null)
    {

      buf.append("(success: ").append(result).append(')');
    } else {
      buf.append("(incomplete)");
    }
    
    return buf;
  }
  







  protected EventExecutor executor()
  {
    return executor;
  }
  
  protected void checkDeadLock() {
    EventExecutor e = executor();
    if ((e != null) && (e.inEventLoop())) {
      throw new BlockingOperationException(toString());
    }
  }
  









  protected static void notifyListener(EventExecutor eventExecutor, Future<?> future, GenericFutureListener<?> listener)
  {
    notifyListenerWithStackOverFlowProtection(
      (EventExecutor)ObjectUtil.checkNotNull(eventExecutor, "eventExecutor"), 
      (Future)ObjectUtil.checkNotNull(future, "future"), 
      (GenericFutureListener)ObjectUtil.checkNotNull(listener, "listener"));
  }
  
  private void notifyListeners() {
    EventExecutor executor = executor();
    if (executor.inEventLoop()) {
      InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
      int stackDepth = threadLocals.futureListenerStackDepth();
      if (stackDepth < MAX_LISTENER_STACK_DEPTH) {
        threadLocals.setFutureListenerStackDepth(stackDepth + 1);
        try {
          notifyListenersNow();
        } finally {
          threadLocals.setFutureListenerStackDepth(stackDepth);
        }
        return;
      }
    }
    
    safeExecute(executor, new Runnable()
    {
      public void run() {
        DefaultPromise.this.notifyListenersNow();
      }
    });
  }
  






  private static void notifyListenerWithStackOverFlowProtection(EventExecutor executor, Future<?> future, final GenericFutureListener<?> listener)
  {
    if (executor.inEventLoop()) {
      InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
      int stackDepth = threadLocals.futureListenerStackDepth();
      if (stackDepth < MAX_LISTENER_STACK_DEPTH) {
        threadLocals.setFutureListenerStackDepth(stackDepth + 1);
        try {
          notifyListener0(future, listener);
        } finally {
          threadLocals.setFutureListenerStackDepth(stackDepth);
        }
        return;
      }
    }
    
    safeExecute(executor, new Runnable()
    {
      public void run() {
        DefaultPromise.notifyListener0(val$future, listener);
      }
    });
  }
  
  private void notifyListenersNow()
  {
    synchronized (this)
    {
      if ((notifyingListeners) || (this.listeners == null)) {
        return;
      }
      notifyingListeners = true;
      Object listeners = this.listeners;
      this.listeners = null;
    }
    for (;;) { Object listeners;
      if ((listeners instanceof DefaultFutureListeners)) {
        notifyListeners0((DefaultFutureListeners)listeners);
      } else {
        notifyListener0(this, (GenericFutureListener)listeners);
      }
      synchronized (this) {
        if (this.listeners == null)
        {

          notifyingListeners = false;
          return;
        }
        listeners = this.listeners;
        this.listeners = null;
      }
    }
  }
  
  private void notifyListeners0(DefaultFutureListeners listeners) {
    GenericFutureListener<?>[] a = listeners.listeners();
    int size = listeners.size();
    for (int i = 0; i < size; i++) {
      notifyListener0(this, a[i]);
    }
  }
  
  private static void notifyListener0(Future future, GenericFutureListener l)
  {
    try {
      l.operationComplete(future);
    } catch (Throwable t) {
      if (logger.isWarnEnabled()) {
        logger.warn("An exception was thrown by " + l.getClass().getName() + ".operationComplete()", t);
      }
    }
  }
  
  private void addListener0(GenericFutureListener<? extends Future<? super V>> listener) {
    if (listeners == null) {
      listeners = listener;
    } else if ((listeners instanceof DefaultFutureListeners)) {
      ((DefaultFutureListeners)listeners).add(listener);
    } else {
      listeners = new DefaultFutureListeners((GenericFutureListener)listeners, listener);
    }
  }
  
  private void removeListener0(GenericFutureListener<? extends Future<? super V>> listener) {
    if ((listeners instanceof DefaultFutureListeners)) {
      ((DefaultFutureListeners)listeners).remove(listener);
    } else if (listeners == listener) {
      listeners = null;
    }
  }
  
  private boolean setSuccess0(V result) {
    return setValue0(result == null ? SUCCESS : result);
  }
  
  private boolean setFailure0(Throwable cause) {
    return setValue0(new CauseHolder((Throwable)ObjectUtil.checkNotNull(cause, "cause")));
  }
  
  private boolean setValue0(Object objResult) {
    if ((RESULT_UPDATER.compareAndSet(this, null, objResult)) || 
      (RESULT_UPDATER.compareAndSet(this, UNCANCELLABLE, objResult))) {
      if (checkNotifyWaiters()) {
        notifyListeners();
      }
      return true;
    }
    return false;
  }
  



  private synchronized boolean checkNotifyWaiters()
  {
    if (waiters > 0) {
      notifyAll();
    }
    return listeners != null;
  }
  
  private void incWaiters() {
    if (waiters == Short.MAX_VALUE) {
      throw new IllegalStateException("too many waiters: " + this);
    }
    waiters = ((short)(waiters + 1));
  }
  
  private void decWaiters() {
    waiters = ((short)(waiters - 1));
  }
  
  private void rethrowIfFailed() {
    Throwable cause = cause();
    if (cause == null) {
      return;
    }
    
    PlatformDependent.throwException(cause);
  }
  
  private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
    if (isDone()) {
      return true;
    }
    
    if (timeoutNanos <= 0L) {
      return isDone();
    }
    
    if ((interruptable) && (Thread.interrupted())) {
      throw new InterruptedException(toString());
    }
    
    checkDeadLock();
    


    long startTime = System.nanoTime();
    synchronized (this) {
      boolean interrupted = false;
      try {
        long waitTime = timeoutNanos;
        while ((!isDone()) && (waitTime > 0L)) {
          incWaiters();
          try {
            wait(waitTime / 1000000L, (int)(waitTime % 1000000L));
          } catch (InterruptedException e) {
            if (interruptable) {
              throw e;
            }
            interrupted = true;
          }
          finally {
            decWaiters();
          }
          
          if (isDone()) {
            e = 1;
            






            if (interrupted) {
              Thread.currentThread().interrupt();
            }
            return e;
          }
          

          waitTime = timeoutNanos - (System.nanoTime() - startTime);
        }
        e = isDone();
        
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
        return e;
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
  










  void notifyProgressiveListeners(final long progress, long total)
  {
    Object listeners = progressiveListeners();
    if (listeners == null) {
      return;
    }
    
    final ProgressiveFuture<V> self = (ProgressiveFuture)this;
    
    EventExecutor executor = executor();
    if (executor.inEventLoop()) {
      if ((listeners instanceof GenericProgressiveFutureListener[])) {
        notifyProgressiveListeners0(self, (GenericProgressiveFutureListener[])listeners, progress, total);
      }
      else {
        notifyProgressiveListener0(self, (GenericProgressiveFutureListener)listeners, progress, total);
      }
      
    }
    else if ((listeners instanceof GenericProgressiveFutureListener[])) {
      final GenericProgressiveFutureListener<?>[] array = (GenericProgressiveFutureListener[])listeners;
      
      safeExecute(executor, new Runnable()
      {
        public void run() {
          DefaultPromise.notifyProgressiveListeners0(self, array, progress, val$total);
        }
      });
    } else {
      final GenericProgressiveFutureListener<ProgressiveFuture<V>> l = (GenericProgressiveFutureListener)listeners;
      
      safeExecute(executor, new Runnable()
      {
        public void run() {
          DefaultPromise.notifyProgressiveListener0(self, l, progress, val$total);
        }
      });
    }
  }
  




  private synchronized Object progressiveListeners()
  {
    Object listeners = this.listeners;
    if (listeners == null)
    {
      return null;
    }
    
    if ((listeners instanceof DefaultFutureListeners))
    {
      DefaultFutureListeners dfl = (DefaultFutureListeners)listeners;
      int progressiveSize = dfl.progressiveSize();
      switch (progressiveSize) {
      case 0: 
        return null;
      case 1: 
        for (GenericFutureListener<?> l : dfl.listeners()) {
          if ((l instanceof GenericProgressiveFutureListener)) {
            return l;
          }
        }
        return null;
      }
      
      Object array = dfl.listeners();
      Object copy = new GenericProgressiveFutureListener[progressiveSize];
      int i = 0; for (int j = 0; j < progressiveSize; i++) {
        GenericFutureListener<?> l = array[i];
        if ((l instanceof GenericProgressiveFutureListener)) {
          copy[(j++)] = ((GenericProgressiveFutureListener)l);
        }
      }
      
      return copy; }
    if ((listeners instanceof GenericProgressiveFutureListener)) {
      return listeners;
    }
    
    return null;
  }
  

  private static void notifyProgressiveListeners0(ProgressiveFuture<?> future, GenericProgressiveFutureListener<?>[] listeners, long progress, long total)
  {
    for (GenericProgressiveFutureListener<?> l : listeners) {
      if (l == null) {
        break;
      }
      notifyProgressiveListener0(future, l, progress, total);
    }
  }
  
  private static void notifyProgressiveListener0(ProgressiveFuture future, GenericProgressiveFutureListener l, long progress, long total)
  {
    try
    {
      l.operationProgressed(future, progress, total);
    } catch (Throwable t) {
      if (logger.isWarnEnabled()) {
        logger.warn("An exception was thrown by " + l.getClass().getName() + ".operationProgressed()", t);
      }
    }
  }
  
  private static boolean isCancelled0(Object result) {
    return ((result instanceof CauseHolder)) && ((cause instanceof CancellationException));
  }
  
  private static boolean isDone0(Object result) {
    return (result != null) && (result != UNCANCELLABLE);
  }
  
  private static final class CauseHolder {
    final Throwable cause;
    
    CauseHolder(Throwable cause) { this.cause = cause; }
  }
  
  private static void safeExecute(EventExecutor executor, Runnable task)
  {
    try {
      executor.execute(task);
    } catch (Throwable t) {
      rejectedExecutionLogger.error("Failed to submit a listener notification task. Event loop shut down?", t);
    }
  }
  

  private static final class StacklessCancellationException
    extends CancellationException
  {
    private static final long serialVersionUID = -2974906711413716191L;
    
    private StacklessCancellationException() {}
    
    public Throwable fillInStackTrace()
    {
      return this;
    }
    
    static StacklessCancellationException newInstance(Class<?> clazz, String method) {
      return (StacklessCancellationException)ThrowableUtil.unknownStackTrace(new StacklessCancellationException(), clazz, method);
    }
  }
}
