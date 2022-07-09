package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess;











































































































































































































public class MpscArrayQueue<E>
  extends MpscArrayQueueL3Pad<E>
{
  public MpscArrayQueue(int capacity)
  {
    super(capacity);
  }
  








  public boolean offerIfBelowThreshold(E e, int threshold)
  {
    if (null == e)
    {
      throw new NullPointerException();
    }
    
    long mask = this.mask;
    long capacity = mask + 1L;
    
    long producerLimit = lvProducerLimit();
    long pIndex;
    do
    {
      pIndex = lvProducerIndex();
      long available = producerLimit - pIndex;
      long size = capacity - available;
      if (size >= threshold)
      {
        long cIndex = lvConsumerIndex();
        size = pIndex - cIndex;
        if (size >= threshold)
        {
          return false;
        }
        


        producerLimit = cIndex + capacity;
        

        soProducerLimit(producerLimit);
      }
      
    }
    while (!casProducerIndex(pIndex, pIndex + 1L));
    





    long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(pIndex, mask);
    UnsafeRefArrayAccess.soRefElement(buffer, offset, e);
    return true;
  }
  











  public boolean offer(E e)
  {
    if (null == e)
    {
      throw new NullPointerException();
    }
    

    long mask = this.mask;
    long producerLimit = lvProducerLimit();
    long pIndex;
    do
    {
      pIndex = lvProducerIndex();
      if (pIndex >= producerLimit)
      {
        long cIndex = lvConsumerIndex();
        producerLimit = cIndex + mask + 1L;
        
        if (pIndex >= producerLimit)
        {
          return false;
        }
        



        soProducerLimit(producerLimit);
      }
      
    }
    while (!casProducerIndex(pIndex, pIndex + 1L));
    





    long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(pIndex, mask);
    UnsafeRefArrayAccess.soRefElement(buffer, offset, e);
    return true;
  }
  






  public final int failFastOffer(E e)
  {
    if (null == e)
    {
      throw new NullPointerException();
    }
    long mask = this.mask;
    long capacity = mask + 1L;
    long pIndex = lvProducerIndex();
    long producerLimit = lvProducerLimit();
    if (pIndex >= producerLimit)
    {
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
    

    long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(pIndex, mask);
    UnsafeRefArrayAccess.soRefElement(buffer, offset, e);
    return 0;
  }
  










  public E poll()
  {
    long cIndex = lpConsumerIndex();
    long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(cIndex, mask);
    
    E[] buffer = this.buffer;
    

    E e = UnsafeRefArrayAccess.lvRefElement(buffer, offset);
    if (null == e)
    {





      if (cIndex != lvProducerIndex())
      {
        do
        {
          e = UnsafeRefArrayAccess.lvRefElement(buffer, offset);
        }
        while (e == null);
      }
      else
      {
        return null;
      }
    }
    
    UnsafeRefArrayAccess.spRefElement(buffer, offset, null);
    soConsumerIndex(cIndex + 1L);
    return e;
  }
  











  public E peek()
  {
    E[] buffer = this.buffer;
    
    long cIndex = lpConsumerIndex();
    long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(cIndex, mask);
    E e = UnsafeRefArrayAccess.lvRefElement(buffer, offset);
    if (null == e)
    {





      if (cIndex != lvProducerIndex())
      {
        do
        {
          e = UnsafeRefArrayAccess.lvRefElement(buffer, offset);
        }
        while (e == null);
      }
      else
      {
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
    E[] buffer = this.buffer;
    long cIndex = lpConsumerIndex();
    long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(cIndex, mask);
    

    E e = UnsafeRefArrayAccess.lvRefElement(buffer, offset);
    if (null == e)
    {
      return null;
    }
    
    UnsafeRefArrayAccess.spRefElement(buffer, offset, null);
    soConsumerIndex(cIndex + 1L);
    return e;
  }
  

  public E relaxedPeek()
  {
    E[] buffer = this.buffer;
    long mask = this.mask;
    long cIndex = lpConsumerIndex();
    return UnsafeRefArrayAccess.lvRefElement(buffer, UnsafeRefArrayAccess.calcCircularRefElementOffset(cIndex, mask));
  }
  

  public int drain(MessagePassingQueue.Consumer<E> c, int limit)
  {
    if (null == c)
      throw new IllegalArgumentException("c is null");
    if (limit < 0)
      throw new IllegalArgumentException("limit is negative: " + limit);
    if (limit == 0) {
      return 0;
    }
    E[] buffer = this.buffer;
    long mask = this.mask;
    long cIndex = lpConsumerIndex();
    
    for (int i = 0; i < limit; i++)
    {
      long index = cIndex + i;
      long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(index, mask);
      E e = UnsafeRefArrayAccess.lvRefElement(buffer, offset);
      if (null == e)
      {
        return i;
      }
      UnsafeRefArrayAccess.spRefElement(buffer, offset, null);
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
    if (limit == 0) {
      return 0;
    }
    long mask = this.mask;
    long capacity = mask + 1L;
    long producerLimit = lvProducerLimit();
    long pIndex;
    int actualLimit;
    do
    {
      pIndex = lvProducerIndex();
      long available = producerLimit - pIndex;
      if (available <= 0L)
      {
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
    }
    while (!casProducerIndex(pIndex, pIndex + actualLimit));
    
    E[] buffer = this.buffer;
    for (int i = 0; i < actualLimit; i++)
    {

      long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(pIndex + i, mask);
      UnsafeRefArrayAccess.soRefElement(buffer, offset, s.get());
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
}
