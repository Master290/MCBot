package io.netty.handler.codec.http2;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;













final class MaxCapacityQueue<E>
  implements Queue<E>
{
  private final Queue<E> queue;
  private final int maxCapacity;
  
  MaxCapacityQueue(Queue<E> queue, int maxCapacity)
  {
    this.queue = queue;
    this.maxCapacity = maxCapacity;
  }
  
  public boolean add(E element)
  {
    if (offer(element)) {
      return true;
    }
    throw new IllegalStateException();
  }
  
  public boolean offer(E element)
  {
    if (maxCapacity <= queue.size()) {
      return false;
    }
    return queue.offer(element);
  }
  
  public E remove()
  {
    return queue.remove();
  }
  
  public E poll()
  {
    return queue.poll();
  }
  
  public E element()
  {
    return queue.element();
  }
  
  public E peek()
  {
    return queue.peek();
  }
  
  public int size()
  {
    return queue.size();
  }
  
  public boolean isEmpty()
  {
    return queue.isEmpty();
  }
  
  public boolean contains(Object o)
  {
    return queue.contains(o);
  }
  
  public Iterator<E> iterator()
  {
    return queue.iterator();
  }
  
  public Object[] toArray()
  {
    return queue.toArray();
  }
  
  public <T> T[] toArray(T[] a)
  {
    return queue.toArray(a);
  }
  
  public boolean remove(Object o)
  {
    return queue.remove(o);
  }
  
  public boolean containsAll(Collection<?> c)
  {
    return queue.containsAll(c);
  }
  
  public boolean addAll(Collection<? extends E> c)
  {
    if (maxCapacity >= size() + c.size()) {
      return queue.addAll(c);
    }
    throw new IllegalStateException();
  }
  
  public boolean removeAll(Collection<?> c)
  {
    return queue.removeAll(c);
  }
  
  public boolean retainAll(Collection<?> c)
  {
    return queue.retainAll(c);
  }
  
  public void clear()
  {
    queue.clear();
  }
}
