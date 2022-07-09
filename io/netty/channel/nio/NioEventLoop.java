package io.netty.channel.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopException;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.SelectStrategy;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.IntSupplier;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReflectionUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;






















public final class NioEventLoop
  extends SingleThreadEventLoop
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioEventLoop.class);
  

  private static final int CLEANUP_INTERVAL = 256;
  
  private static final boolean DISABLE_KEY_SET_OPTIMIZATION = SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);
  
  private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
  
  private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;
  private final IntSupplier selectNowSupplier = new IntSupplier()
  {

    public int get() throws Exception { return selectNow(); }
  };
  private Selector selector;
  private Selector unwrappedSelector;
  private SelectedSelectionKeySet selectedKeys;
  private final SelectorProvider provider;
  private static final long AWAKE = -1L;
  private static final long NONE = Long.MAX_VALUE;
  
  static {
    String key = "sun.nio.ch.bugLevel";
    String bugLevel = SystemPropertyUtil.get("sun.nio.ch.bugLevel");
    if (bugLevel == null) {
      try {
        AccessController.doPrivileged(new PrivilegedAction()
        {
          public Void run() {
            System.setProperty("sun.nio.ch.bugLevel", "");
            return null;
          }
        });
      } catch (SecurityException e) {
        logger.debug("Unable to get/set System Property: sun.nio.ch.bugLevel", e);
      }
    }
    
    int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
    if (selectorAutoRebuildThreshold < 3) {
      selectorAutoRebuildThreshold = 0;
    }
    
    SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;
    
    if (logger.isDebugEnabled()) {
      logger.debug("-Dio.netty.noKeySetOptimization: {}", Boolean.valueOf(DISABLE_KEY_SET_OPTIMIZATION));
      logger.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", Integer.valueOf(SELECTOR_AUTO_REBUILD_THRESHOLD));
    }
  }
  
















  private final AtomicLong nextWakeupNanos = new AtomicLong(-1L);
  
  private final SelectStrategy selectStrategy;
  
  private volatile int ioRatio = 50;
  
  private int cancelledKeys;
  private boolean needsToSelectAgain;
  
  NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider, SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory, EventLoopTaskQueueFactory tailTaskQueueFactory)
  {
    super(parent, executor, false, newTaskQueue(taskQueueFactory), newTaskQueue(tailTaskQueueFactory), rejectedExecutionHandler);
    
    provider = ((SelectorProvider)ObjectUtil.checkNotNull(selectorProvider, "selectorProvider"));
    selectStrategy = ((SelectStrategy)ObjectUtil.checkNotNull(strategy, "selectStrategy"));
    SelectorTuple selectorTuple = openSelector();
    selector = selector;
    unwrappedSelector = unwrappedSelector;
  }
  
  private static Queue<Runnable> newTaskQueue(EventLoopTaskQueueFactory queueFactory)
  {
    if (queueFactory == null) {
      return newTaskQueue0(DEFAULT_MAX_PENDING_TASKS);
    }
    return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
  }
  
  private static final class SelectorTuple {
    final Selector unwrappedSelector;
    final Selector selector;
    
    SelectorTuple(Selector unwrappedSelector) {
      this.unwrappedSelector = unwrappedSelector;
      selector = unwrappedSelector;
    }
    
    SelectorTuple(Selector unwrappedSelector, Selector selector) {
      this.unwrappedSelector = unwrappedSelector;
      this.selector = selector;
    }
  }
  
  private SelectorTuple openSelector()
  {
    try {
      unwrappedSelector = provider.openSelector();
    } catch (IOException e) { Selector unwrappedSelector;
      throw new ChannelException("failed to open a new selector", e);
    }
    final Selector unwrappedSelector;
    if (DISABLE_KEY_SET_OPTIMIZATION) {
      return new SelectorTuple(unwrappedSelector);
    }
    
    Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction()
    {
      public Object run() {
        try {
          return Class.forName("sun.nio.ch.SelectorImpl", false, 
          

            PlatformDependent.getSystemClassLoader());
        } catch (Throwable cause) {
          return cause;
        }
      }
    });
    
    if ((!(maybeSelectorImplClass instanceof Class)) || 
    
      (!((Class)maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass()))) {
      if ((maybeSelectorImplClass instanceof Throwable)) {
        Throwable t = (Throwable)maybeSelectorImplClass;
        logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, t);
      }
      return new SelectorTuple(unwrappedSelector);
    }
    
    final Class<?> selectorImplClass = (Class)maybeSelectorImplClass;
    final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();
    
    Object maybeException = AccessController.doPrivileged(new PrivilegedAction()
    {
      public Object run() {
        try {
          Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
          Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");
          
          if ((PlatformDependent.javaVersion() >= 9) && (PlatformDependent.hasUnsafe()))
          {

            long selectedKeysFieldOffset = PlatformDependent.objectFieldOffset(selectedKeysField);
            
            long publicSelectedKeysFieldOffset = PlatformDependent.objectFieldOffset(publicSelectedKeysField);
            
            if ((selectedKeysFieldOffset != -1L) && (publicSelectedKeysFieldOffset != -1L)) {
              PlatformDependent.putObject(unwrappedSelector, selectedKeysFieldOffset, selectedKeySet);
              
              PlatformDependent.putObject(unwrappedSelector, publicSelectedKeysFieldOffset, selectedKeySet);
              
              return null;
            }
          }
          

          Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField, true);
          if (cause != null) {
            return cause;
          }
          cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField, true);
          if (cause != null) {
            return cause;
          }
          
          selectedKeysField.set(unwrappedSelector, selectedKeySet);
          publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
          return null;
        } catch (NoSuchFieldException e) {
          return e;
        } catch (IllegalAccessException e) {
          return e;
        }
      }
    });
    
    if ((maybeException instanceof Exception)) {
      selectedKeys = null;
      Exception e = (Exception)maybeException;
      logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, e);
      return new SelectorTuple(unwrappedSelector);
    }
    selectedKeys = selectedKeySet;
    logger.trace("instrumented a special java.util.Set into: {}", unwrappedSelector);
    return new SelectorTuple(unwrappedSelector, new SelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
  }
  



  public SelectorProvider selectorProvider()
  {
    return provider;
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
  




  public void register(final SelectableChannel ch, final int interestOps, final NioTask<?> task)
  {
    ObjectUtil.checkNotNull(ch, "ch");
    if (interestOps == 0) {
      throw new IllegalArgumentException("interestOps must be non-zero.");
    }
    if ((interestOps & (ch.validOps() ^ 0xFFFFFFFF)) != 0)
    {
      throw new IllegalArgumentException("invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
    }
    ObjectUtil.checkNotNull(task, "task");
    
    if (isShutdown()) {
      throw new IllegalStateException("event loop shut down");
    }
    
    if (inEventLoop()) {
      register0(ch, interestOps, task);

    }
    else
    {

      try
      {


        submit(new Runnable()
        {
          public void run()
          {
            NioEventLoop.this.register0(ch, interestOps, task);
          }
        }).sync();
      }
      catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  private void register0(SelectableChannel ch, int interestOps, NioTask<?> task) {
    try {
      ch.register(unwrappedSelector, interestOps, task);
    } catch (Exception e) {
      throw new EventLoopException("failed to register a channel", e);
    }
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
  



  public void rebuildSelector()
  {
    if (!inEventLoop()) {
      execute(new Runnable()
      {
        public void run() {
          NioEventLoop.this.rebuildSelector0();
        }
      });
      return;
    }
    rebuildSelector0();
  }
  
  public int registeredChannels()
  {
    return selector.keys().size() - cancelledKeys;
  }
  
  private void rebuildSelector0() {
    Selector oldSelector = selector;
    

    if (oldSelector == null) {
      return;
    }
    try
    {
      newSelectorTuple = openSelector();
    } catch (Exception e) { SelectorTuple newSelectorTuple;
      logger.warn("Failed to create a new Selector.", e); return;
    }
    
    SelectorTuple newSelectorTuple;
    
    int nChannels = 0;
    for (SelectionKey key : oldSelector.keys()) {
      Object a = key.attachment();
      try {
        if ((!key.isValid()) || (key.channel().keyFor(unwrappedSelector) == null))
        {


          int interestOps = key.interestOps();
          key.cancel();
          SelectionKey newKey = key.channel().register(unwrappedSelector, interestOps, a);
          if ((a instanceof AbstractNioChannel))
          {
            selectionKey = newKey;
          }
          nChannels++;
        }
      } catch (Exception e) { logger.warn("Failed to re-register a Channel to the new Selector.", e);
        if ((a instanceof AbstractNioChannel)) {
          AbstractNioChannel ch = (AbstractNioChannel)a;
          ch.unsafe().close(ch.unsafe().voidPromise());
        }
        else {
          NioTask<SelectableChannel> task = (NioTask)a;
          invokeChannelUnregistered(task, key, e);
        }
      }
    }
    
    selector = selector;
    unwrappedSelector = unwrappedSelector;
    
    try
    {
      oldSelector.close();
    } catch (Throwable t) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to close the old Selector.", t);
      }
    }
    
    if (logger.isInfoEnabled()) {
      logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
    }
  }
  
  protected void run()
  {
    int selectCnt = 0;
    try
    {
      for (;;) {
        try {
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
            long curDeadlineNanos = nextScheduledTaskDeadlineNanos();
            if (curDeadlineNanos == -1L) {
              curDeadlineNanos = Long.MAX_VALUE;
            }
            nextWakeupNanos.set(curDeadlineNanos);
            try {
              if (!hasTasks()) {
                strategy = select(curDeadlineNanos);
              }
            }
            finally
            {
              nextWakeupNanos.lazySet(-1L);
            }
          
          }
          
        }
        catch (IOException e)
        {
          rebuildSelector0();
          selectCnt = 0;
          handleLoopException(e);
        }
        




























































        continue;
        int strategy;
        selectCnt++;
        cancelledKeys = 0;
        needsToSelectAgain = false;
        int ioRatio = this.ioRatio;
        boolean ranTasks;
        if (ioRatio == 100) {
          boolean ranTasks;
          try { if (strategy > 0) {
              processSelectedKeys();
            }
          } finally {
            boolean ranTasks;
            ranTasks = runAllTasks();
          }
        } else if (strategy > 0) {
          long ioStartTime = System.nanoTime();
          boolean ranTasks;
          try { processSelectedKeys();
          } finally { long ioTime;
            boolean ranTasks;
            long ioTime = System.nanoTime() - ioStartTime;
            ranTasks = runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
          }
        } else {
          ranTasks = runAllTasks(0L);
        }
        
        if ((ranTasks) || (strategy > 0)) {
          if ((selectCnt > 3) && (logger.isDebugEnabled())) {
            logger.debug("Selector.select() returned prematurely {} times in a row for Selector {}.", 
              Integer.valueOf(selectCnt - 1), selector);
          }
          selectCnt = 0;
        } else if (unexpectedSelectorWakeup(selectCnt)) {
          selectCnt = 0;
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
    catch (CancelledKeyException e)
    {
      if (logger.isDebugEnabled()) {
        logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?", selector, e);
      }
    }
    catch (Error e) {
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
  

  private boolean unexpectedSelectorWakeup(int selectCnt)
  {
    if (Thread.interrupted())
    {




      if (logger.isDebugEnabled()) {
        logger.debug("Selector.select() returned prematurely because Thread.currentThread().interrupt() was called. Use NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
      }
      

      return true;
    }
    if ((SELECTOR_AUTO_REBUILD_THRESHOLD > 0) && (selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD))
    {


      logger.warn("Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.", 
        Integer.valueOf(selectCnt), selector);
      rebuildSelector();
      return true;
    }
    return false;
  }
  
  private static void handleLoopException(Throwable t) {
    logger.warn("Unexpected exception in the selector loop.", t);
    

    try
    {
      Thread.sleep(1000L);
    }
    catch (InterruptedException localInterruptedException) {}
  }
  
  private void processSelectedKeys()
  {
    if (selectedKeys != null) {
      processSelectedKeysOptimized();
    } else {
      processSelectedKeysPlain(selector.selectedKeys());
    }
  }
  
  protected void cleanup()
  {
    try {
      selector.close();
    } catch (IOException e) {
      logger.warn("Failed to close a selector.", e);
    }
  }
  
  void cancel(SelectionKey key) {
    key.cancel();
    cancelledKeys += 1;
    if (cancelledKeys >= 256) {
      cancelledKeys = 0;
      needsToSelectAgain = true;
    }
  }
  


  private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys)
  {
    if (selectedKeys.isEmpty()) {
      return;
    }
    
    Iterator<SelectionKey> i = selectedKeys.iterator();
    for (;;) {
      SelectionKey k = (SelectionKey)i.next();
      Object a = k.attachment();
      i.remove();
      
      if ((a instanceof AbstractNioChannel)) {
        processSelectedKey(k, (AbstractNioChannel)a);
      }
      else {
        NioTask<SelectableChannel> task = (NioTask)a;
        processSelectedKey(k, task);
      }
      
      if (!i.hasNext()) {
        break;
      }
      
      if (needsToSelectAgain) {
        selectAgain();
        selectedKeys = selector.selectedKeys();
        

        if (selectedKeys.isEmpty()) {
          break;
        }
        i = selectedKeys.iterator();
      }
    }
  }
  
  private void processSelectedKeysOptimized()
  {
    for (int i = 0; i < selectedKeys.size; i++) {
      SelectionKey k = selectedKeys.keys[i];
      

      selectedKeys.keys[i] = null;
      
      Object a = k.attachment();
      
      if ((a instanceof AbstractNioChannel)) {
        processSelectedKey(k, (AbstractNioChannel)a);
      }
      else {
        NioTask<SelectableChannel> task = (NioTask)a;
        processSelectedKey(k, task);
      }
      
      if (needsToSelectAgain)
      {

        selectedKeys.reset(i + 1);
        
        selectAgain();
        i = -1;
      }
    }
  }
  
  private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
    AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
    if (!k.isValid())
    {
      try {
        eventLoop = ch.eventLoop();
      }
      catch (Throwable ignored)
      {
        EventLoop eventLoop;
        
        return;
      }
      
      EventLoop eventLoop;
      
      if (eventLoop == this)
      {
        unsafe.close(unsafe.voidPromise());
      }
      return;
    }
    try
    {
      int readyOps = k.readyOps();
      

      if ((readyOps & 0x8) != 0)
      {

        int ops = k.interestOps();
        ops &= 0xFFFFFFF7;
        k.interestOps(ops);
        
        unsafe.finishConnect();
      }
      

      if ((readyOps & 0x4) != 0)
      {
        ch.unsafe().forceFlush();
      }
      


      if (((readyOps & 0x11) != 0) || (readyOps == 0)) {
        unsafe.read();
      }
    } catch (CancelledKeyException ignored) {
      unsafe.close(unsafe.voidPromise());
    }
  }
  
  private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
    int state = 0;
    try {
      task.channelReady(k.channel(), k);
      state = 1;
    } catch (Exception e) {
      k.cancel();
      invokeChannelUnregistered(task, k, e);
      state = 2;
    } finally {
      switch (state) {
      case 0: 
        k.cancel();
        invokeChannelUnregistered(task, k, null);
        break;
      case 1: 
        if (!k.isValid()) {
          invokeChannelUnregistered(task, k, null);
        }
        break;
      }
      
    }
  }
  
  private void closeAll()
  {
    selectAgain();
    Set<SelectionKey> keys = selector.keys();
    Collection<AbstractNioChannel> channels = new ArrayList(keys.size());
    for (SelectionKey k : keys) {
      Object a = k.attachment();
      if ((a instanceof AbstractNioChannel)) {
        channels.add((AbstractNioChannel)a);
      } else {
        k.cancel();
        
        NioTask<SelectableChannel> task = (NioTask)a;
        invokeChannelUnregistered(task, k, null);
      }
    }
    
    for (AbstractNioChannel ch : channels) {
      ch.unsafe().close(ch.unsafe().voidPromise());
    }
  }
  
  private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k, Throwable cause) {
    try {
      task.channelUnregistered(k.channel(), cause);
    } catch (Exception e) {
      logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
    }
  }
  
  protected void wakeup(boolean inEventLoop)
  {
    if ((!inEventLoop) && (nextWakeupNanos.getAndSet(-1L) != -1L)) {
      selector.wakeup();
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
  
  Selector unwrappedSelector() {
    return unwrappedSelector;
  }
  
  int selectNow() throws IOException {
    return selector.selectNow();
  }
  
  private int select(long deadlineNanos) throws IOException {
    if (deadlineNanos == Long.MAX_VALUE) {
      return selector.select();
    }
    
    long timeoutMillis = deadlineToDelayNanos(deadlineNanos + 995000L) / 1000000L;
    return timeoutMillis <= 0L ? selector.selectNow() : selector.select(timeoutMillis);
  }
  
  private void selectAgain() {
    needsToSelectAgain = false;
    try {
      selector.selectNow();
    } catch (Throwable t) {
      logger.warn("Failed to update SelectionKeys.", t);
    }
  }
}
