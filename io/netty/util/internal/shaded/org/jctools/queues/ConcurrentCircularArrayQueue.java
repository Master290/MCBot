package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.Pow2;
import io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess;
import java.util.Iterator;
import java.util.NoSuchElementException;









































abstract class ConcurrentCircularArrayQueue<E>
  extends ConcurrentCircularArrayQueueL0Pad<E>
  implements MessagePassingQueue<E>, IndexedQueueSizeUtil.IndexedQueue, QueueProgressIndicators, SupportsIterator
{
  protected final long mask;
  protected final E[] buffer;
  
  ConcurrentCircularArrayQueue(int capacity)
  {
    int actualCapacity = Pow2.roundToPowerOfTwo(capacity);
    mask = (actualCapacity - 1);
    buffer = UnsafeRefArrayAccess.allocateRefArray(actualCapacity);
  }
  

  public int size()
  {
    return IndexedQueueSizeUtil.size(this);
  }
  

  public boolean isEmpty()
  {
    return IndexedQueueSizeUtil.isEmpty(this);
  }
  

  public String toString()
  {
    return getClass().getName();
  }
  

  public void clear()
  {
    while (poll() != null) {}
  }
  




  public int capacity()
  {
    return (int)(mask + 1L);
  }
  

  public long currentProducerIndex()
  {
    return lvProducerIndex();
  }
  

  public long currentConsumerIndex()
  {
    return lvConsumerIndex();
  }
  










  public Iterator<E> iterator()
  {
    long cIndex = lvConsumerIndex();
    long pIndex = lvProducerIndex();
    
    return new WeakIterator(cIndex, pIndex, mask, buffer);
  }
  
  private static class WeakIterator<E> implements Iterator<E> {
    private final long pIndex;
    private final long mask;
    private final E[] buffer;
    private long nextIndex;
    private E nextElement;
    
    WeakIterator(long cIndex, long pIndex, long mask, E[] buffer) {
      nextIndex = cIndex;
      this.pIndex = pIndex;
      this.mask = mask;
      this.buffer = buffer;
      nextElement = getNext();
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("remove");
    }
    
    public boolean hasNext()
    {
      return nextElement != null;
    }
    
    public E next()
    {
      E e = nextElement;
      if (e == null)
        throw new NoSuchElementException();
      nextElement = getNext();
      return e;
    }
    
    private E getNext() {
      while (nextIndex < pIndex) {
        long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(nextIndex++, mask);
        E e = UnsafeRefArrayAccess.lvRefElement(buffer, offset);
        if (e != null) {
          return e;
        }
      }
      return null;
    }
  }
}
