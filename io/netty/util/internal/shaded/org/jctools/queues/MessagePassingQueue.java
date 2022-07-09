package io.netty.util.internal.shaded.org.jctools.queues;

public abstract interface MessagePassingQueue<T>
{
  public static final int UNBOUNDED_CAPACITY = -1;
  
  public abstract boolean offer(T paramT);
  
  public abstract T poll();
  
  public abstract T peek();
  
  public abstract int size();
  
  public abstract void clear();
  
  public abstract boolean isEmpty();
  
  public abstract int capacity();
  
  public abstract boolean relaxedOffer(T paramT);
  
  public abstract T relaxedPoll();
  
  public abstract T relaxedPeek();
  
  public abstract int drain(Consumer<T> paramConsumer, int paramInt);
  
  public abstract int fill(Supplier<T> paramSupplier, int paramInt);
  
  public abstract int drain(Consumer<T> paramConsumer);
  
  public abstract int fill(Supplier<T> paramSupplier);
  
  public abstract void drain(Consumer<T> paramConsumer, WaitStrategy paramWaitStrategy, ExitCondition paramExitCondition);
  
  public abstract void fill(Supplier<T> paramSupplier, WaitStrategy paramWaitStrategy, ExitCondition paramExitCondition);
  
  public static abstract interface ExitCondition
  {
    public abstract boolean keepRunning();
  }
  
  public static abstract interface WaitStrategy
  {
    public abstract int idle(int paramInt);
  }
  
  public static abstract interface Consumer<T>
  {
    public abstract void accept(T paramT);
  }
  
  public static abstract interface Supplier<T>
  {
    public abstract T get();
  }
}
