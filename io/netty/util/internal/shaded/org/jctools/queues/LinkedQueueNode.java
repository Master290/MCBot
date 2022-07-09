package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;














final class LinkedQueueNode<E>
{
  private static final long NEXT_OFFSET = UnsafeAccess.fieldOffset(LinkedQueueNode.class, "next");
  
  private E value;
  private volatile LinkedQueueNode<E> next;
  
  LinkedQueueNode()
  {
    this(null);
  }
  
  LinkedQueueNode(E val)
  {
    spValue(val);
  }
  





  public E getAndNullValue()
  {
    E temp = lpValue();
    spValue(null);
    return temp;
  }
  
  public E lpValue()
  {
    return value;
  }
  
  public void spValue(E newValue)
  {
    value = newValue;
  }
  
  public void soNext(LinkedQueueNode<E> n)
  {
    UnsafeAccess.UNSAFE.putOrderedObject(this, NEXT_OFFSET, n);
  }
  
  public void spNext(LinkedQueueNode<E> n)
  {
    UnsafeAccess.UNSAFE.putObject(this, NEXT_OFFSET, n);
  }
  
  public LinkedQueueNode<E> lvNext()
  {
    return next;
  }
}
