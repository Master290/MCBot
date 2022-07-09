package io.netty.util.internal.shaded.org.jctools.queues.atomic;

import io.netty.util.internal.shaded.org.jctools.queues.IndexedQueueSizeUtil;
import io.netty.util.internal.shaded.org.jctools.queues.IndexedQueueSizeUtil.IndexedQueue;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue;
import io.netty.util.internal.shaded.org.jctools.queues.QueueProgressIndicators;
import io.netty.util.internal.shaded.org.jctools.queues.SupportsIterator;
import io.netty.util.internal.shaded.org.jctools.util.Pow2;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceArray;















abstract class AtomicReferenceArrayQueue<E>
  extends AbstractQueue<E>
  implements IndexedQueueSizeUtil.IndexedQueue, QueueProgressIndicators, MessagePassingQueue<E>, SupportsIterator
{
  protected final AtomicReferenceArray<E> buffer;
  protected final int mask;
  
  public AtomicReferenceArrayQueue(int capacity)
  {
    int actualCapacity = Pow2.roundToPowerOfTwo(capacity);
    mask = (actualCapacity - 1);
    buffer = new AtomicReferenceArray(actualCapacity);
  }
  

  public String toString()
  {
    return getClass().getName();
  }
  

  public void clear()
  {
    while (poll() != null) {}
  }
  




  public final int capacity()
  {
    return mask + 1;
  }
  





  public final int size()
  {
    return IndexedQueueSizeUtil.size(this);
  }
  

  public final boolean isEmpty()
  {
    return IndexedQueueSizeUtil.isEmpty(this);
  }
  

  public final long currentProducerIndex()
  {
    return lvProducerIndex();
  }
  

  public final long currentConsumerIndex()
  {
    return lvConsumerIndex();
  }
  










  public final Iterator<E> iterator()
  {
    long cIndex = lvConsumerIndex();
    long pIndex = lvProducerIndex();
    
    return new WeakIterator(cIndex, pIndex, mask, buffer);
  }
  
  private static class WeakIterator<E> implements Iterator<E>
  {
    private final long pIndex;
    private final int mask;
    private final AtomicReferenceArray<E> buffer;
    private long nextIndex;
    private E nextElement;
    
    WeakIterator(long cIndex, long pIndex, int mask, AtomicReferenceArray<E> buffer) {
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
      int mask = this.mask;
      AtomicReferenceArray<E> buffer = this.buffer;
      while (nextIndex < pIndex) {
        int offset = AtomicQueueUtil.calcCircularRefElementOffset(nextIndex++, mask);
        E e = AtomicQueueUtil.lvRefElement(buffer, offset);
        if (e != null) {
          return e;
        }
      }
      return null;
    }
  }
}
