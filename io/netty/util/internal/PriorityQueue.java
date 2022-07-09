package io.netty.util.internal;

import java.util.Queue;

public abstract interface PriorityQueue<T>
  extends Queue<T>
{
  public abstract boolean removeTyped(T paramT);
  
  public abstract boolean containsTyped(T paramT);
  
  public abstract void priorityChanged(T paramT);
  
  public abstract void clearIgnoringIndexes();
}
