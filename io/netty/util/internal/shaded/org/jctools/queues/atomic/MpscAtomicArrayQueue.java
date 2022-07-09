package io.netty.util.internal.shaded.org.jctools.queues.atomic;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.ExitCondition;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.WaitStrategy;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueueUtil;
import java.util.concurrent.atomic.AtomicReferenceArray;










































































































































































































































































































































public class MpscAtomicArrayQueue<E>
  extends MpscAtomicArrayQueueL3Pad<E>
{
  public MpscAtomicArrayQueue(int capacity)
  {
    super(capacity);
  }
  







  public boolean offerIfBelowThreshold(E e, int threshold)
  {
    if (null == e) {
      throw new NullPointerException();
    }
    int mask = this.mask;
    long capacity = mask + 1;
    long producerLimit = lvProducerLimit();
    long pIndex;
    do {
      pIndex = lvProducerIndex();
      long available = producerLimit - pIndex;
      long size = capacity - available;
      if (size >= threshold) {
        long cIndex = lvConsumerIndex();
        size = pIndex - cIndex;
        if (size >= threshold)
        {
          return false;
        }
        
        producerLimit = cIndex + capacity;
        
        soProducerLimit(producerLimit);
      }
      
    } while (!casProducerIndex(pIndex, pIndex + 1L));
    




    int offset = AtomicQueueUtil.calcCircularRefElementOffset(pIndex, mask);
    AtomicQueueUtil.soRefElement(buffer, offset, e);
    
    return true;
  }
  










  public boolean offer(E e)
  {
    if (null == e) {
      throw new NullPointerException();
    }
    
    int mask = this.mask;
    long producerLimit = lvProducerLimit();
    long pIndex;
    do {
      pIndex = lvProducerIndex();
      if (pIndex >= producerLimit) {
        long cIndex = lvConsumerIndex();
        producerLimit = cIndex + mask + 1L;
        if (pIndex >= producerLimit)
        {
          return false;
        }
        

        soProducerLimit(producerLimit);
      }
      
    } while (!casProducerIndex(pIndex, pIndex + 1L));
    




    int offset = AtomicQueueUtil.calcCircularRefElementOffset(pIndex, mask);
    AtomicQueueUtil.soRefElement(buffer, offset, e);
    
    return true;
  }
  





  public final int failFastOffer(E e)
  {
    if (null == e) {
      throw new NullPointerException();
    }
    int mask = this.mask;
    long capacity = mask + 1;
    long pIndex = lvProducerIndex();
    long producerLimit = lvProducerLimit();
    if (pIndex >= producerLimit) {
      long cIndex = lvConsumerIndex();
      producerLimit = cIndex + capacity;
      if (pIndex >= producerLimit)
      {
        return 1;
      }
      
      soProducerLimit(producerLimit);
    }
    

    if (!casProducerIndex(pIndex, pIndex + 1L))
    {
      return -1;
    }
    
    int offset = AtomicQueueUtil.calcCircularRefElementOffset(pIndex, mask);
    AtomicQueueUtil.soRefElement(buffer, offset, e);
    
    return 0;
  }
  









  public E poll()
  {
    long cIndex = lpConsumerIndex();
    int offset = AtomicQueueUtil.calcCircularRefElementOffset(cIndex, mask);
    
    AtomicReferenceArray<E> buffer = this.buffer;
    
    E e = AtomicQueueUtil.lvRefElement(buffer, offset);
    if (null == e)
    {




      if (cIndex != lvProducerIndex()) {
        do {
          e = AtomicQueueUtil.lvRefElement(buffer, offset);
        } while (e == null);
      } else {
        return null;
      }
    }
    AtomicQueueUtil.spRefElement(buffer, offset, null);
    soConsumerIndex(cIndex + 1L);
    return e;
  }
  










  public E peek()
  {
    AtomicReferenceArray<E> buffer = this.buffer;
    long cIndex = lpConsumerIndex();
    int offset = AtomicQueueUtil.calcCircularRefElementOffset(cIndex, mask);
    E e = AtomicQueueUtil.lvRefElement(buffer, offset);
    if (null == e)
    {




      if (cIndex != lvProducerIndex()) {
        do {
          e = AtomicQueueUtil.lvRefElement(buffer, offset);
        } while (e == null);
      } else {
        return null;
      }
    }
    return e;
  }
  
  public boolean relaxedOffer(E e)
  {
    return offer(e);
  }
  
  public E relaxedPoll()
  {
    AtomicReferenceArray<E> buffer = this.buffer;
    long cIndex = lpConsumerIndex();
    int offset = AtomicQueueUtil.calcCircularRefElementOffset(cIndex, mask);
    
    E e = AtomicQueueUtil.lvRefElement(buffer, offset);
    if (null == e) {
      return null;
    }
    AtomicQueueUtil.spRefElement(buffer, offset, null);
    soConsumerIndex(cIndex + 1L);
    return e;
  }
  
  public E relaxedPeek()
  {
    AtomicReferenceArray<E> buffer = this.buffer;
    int mask = this.mask;
    long cIndex = lpConsumerIndex();
    return AtomicQueueUtil.lvRefElement(buffer, AtomicQueueUtil.calcCircularRefElementOffset(cIndex, mask));
  }
  
  public int drain(MessagePassingQueue.Consumer<E> c, int limit)
  {
    if (null == c)
      throw new IllegalArgumentException("c is null");
    if (limit < 0)
      throw new IllegalArgumentException("limit is negative: " + limit);
    if (limit == 0)
      return 0;
    AtomicReferenceArray<E> buffer = this.buffer;
    int mask = this.mask;
    long cIndex = lpConsumerIndex();
    for (int i = 0; i < limit; i++) {
      long index = cIndex + i;
      int offset = AtomicQueueUtil.calcCircularRefElementOffset(index, mask);
      E e = AtomicQueueUtil.lvRefElement(buffer, offset);
      if (null == e) {
        return i;
      }
      AtomicQueueUtil.spRefElement(buffer, offset, null);
      
      soConsumerIndex(index + 1L);
      c.accept(e);
    }
    return limit;
  }
  
  public int fill(MessagePassingQueue.Supplier<E> s, int limit)
  {
    if (null == s)
      throw new IllegalArgumentException("supplier is null");
    if (limit < 0)
      throw new IllegalArgumentException("limit is negative:" + limit);
    if (limit == 0)
      return 0;
    int mask = this.mask;
    long capacity = mask + 1;
    long producerLimit = lvProducerLimit();
    
    int actualLimit = 0;
    long pIndex;
    do { pIndex = lvProducerIndex();
      long available = producerLimit - pIndex;
      if (available <= 0L) {
        long cIndex = lvConsumerIndex();
        producerLimit = cIndex + capacity;
        available = producerLimit - pIndex;
        if (available <= 0L)
        {
          return 0;
        }
        
        soProducerLimit(producerLimit);
      }
      
      actualLimit = Math.min((int)available, limit);
    } while (!casProducerIndex(pIndex, pIndex + actualLimit));
    
    AtomicReferenceArray<E> buffer = this.buffer;
    for (int i = 0; i < actualLimit; i++)
    {
      int offset = AtomicQueueUtil.calcCircularRefElementOffset(pIndex + i, mask);
      AtomicQueueUtil.soRefElement(buffer, offset, s.get());
    }
    return actualLimit;
  }
  
  public int drain(MessagePassingQueue.Consumer<E> c)
  {
    return drain(c, capacity());
  }
  
  public int fill(MessagePassingQueue.Supplier<E> s)
  {
    return MessagePassingQueueUtil.fillBounded(this, s);
  }
  
  public void drain(MessagePassingQueue.Consumer<E> c, MessagePassingQueue.WaitStrategy w, MessagePassingQueue.ExitCondition exit)
  {
    MessagePassingQueueUtil.drain(this, c, w, exit);
  }
  
  public void fill(MessagePassingQueue.Supplier<E> s, MessagePassingQueue.WaitStrategy wait, MessagePassingQueue.ExitCondition exit)
  {
    MessagePassingQueueUtil.fill(this, s, wait, exit);
  }
  


  @Deprecated
  public int weakOffer(E e)
  {
    return failFastOffer(e);
  }
}
