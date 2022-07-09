package io.netty.util.internal.shaded.org.jctools.queues.atomic;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.ExitCondition;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.WaitStrategy;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueueUtil;
import io.netty.util.internal.shaded.org.jctools.queues.QueueProgressIndicators;
import io.netty.util.internal.shaded.org.jctools.util.PortableJvmInfo;
import io.netty.util.internal.shaded.org.jctools.util.Pow2;
import io.netty.util.internal.shaded.org.jctools.util.RangeUtil;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceArray;












































































































































































































































































abstract class BaseMpscLinkedAtomicArrayQueue<E>
  extends BaseMpscLinkedAtomicArrayQueueColdProducerFields<E>
  implements MessagePassingQueue<E>, QueueProgressIndicators
{
  private static final Object JUMP = new Object();
  
  private static final Object BUFFER_CONSUMED = new Object();
  

  private static final int CONTINUE_TO_P_INDEX_CAS = 0;
  

  private static final int RETRY = 1;
  
  private static final int QUEUE_FULL = 2;
  
  private static final int QUEUE_RESIZE = 3;
  

  public BaseMpscLinkedAtomicArrayQueue(int initialCapacity)
  {
    RangeUtil.checkGreaterThanOrEqual(initialCapacity, 2, "initialCapacity");
    int p2capacity = Pow2.roundToPowerOfTwo(initialCapacity);
    
    long mask = p2capacity - 1 << 1;
    
    AtomicReferenceArray<E> buffer = AtomicQueueUtil.allocateRefArray(p2capacity + 1);
    producerBuffer = buffer;
    producerMask = mask;
    consumerBuffer = buffer;
    consumerMask = mask;
    
    soProducerLimit(mask);
  }
  







  public int size()
  {
    long after = lvConsumerIndex();
    for (;;)
    {
      long before = after;
      long currentProducerIndex = lvProducerIndex();
      after = lvConsumerIndex();
      if (before == after) {
        long size = currentProducerIndex - after >> 1;
        break;
      }
    }
    long size;
    if (size > 2147483647L) {
      return Integer.MAX_VALUE;
    }
    return (int)size;
  }
  


  public boolean isEmpty()
  {
    return lvConsumerIndex() == lvProducerIndex();
  }
  
  public String toString()
  {
    return getClass().getName();
  }
  
  public boolean offer(E e)
  {
    if (null == e) {
      throw new NullPointerException();
    }
    long pIndex;
    long mask;
    AtomicReferenceArray<E> buffer;
    for (;;) {
      long producerLimit = lvProducerLimit();
      pIndex = lvProducerIndex();
      
      if ((pIndex & 1L) != 1L)
      {



        mask = producerMask;
        buffer = producerBuffer;
        
        if (producerLimit <= pIndex) {
          int result = offerSlowPath(mask, pIndex, producerLimit);
          switch (result) {
          case 0: 
            break;
          case 1: 
            break;
          case 2: 
            return false;
          case 3: 
            resize(mask, buffer, pIndex, e, null);
            return true;
          }
        } else {
          if (casProducerIndex(pIndex, pIndex + 2L))
            break;
        }
      }
    }
    int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(pIndex, mask);
    
    AtomicQueueUtil.soRefElement(buffer, offset, e);
    return true;
  }
  






  public E poll()
  {
    AtomicReferenceArray<E> buffer = consumerBuffer;
    long index = lpConsumerIndex();
    long mask = consumerMask;
    int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(index, mask);
    Object e = AtomicQueueUtil.lvRefElement(buffer, offset);
    if (e == null) {
      if (index != lvProducerIndex()) {
        do
        {
          e = AtomicQueueUtil.lvRefElement(buffer, offset);
        } while (e == null);
      } else {
        return null;
      }
    }
    if (e == JUMP) {
      AtomicReferenceArray<E> nextBuffer = nextBuffer(buffer, mask);
      return newBufferPoll(nextBuffer, index);
    }
    
    AtomicQueueUtil.soRefElement(buffer, offset, null);
    
    soConsumerIndex(index + 2L);
    return e;
  }
  






  public E peek()
  {
    AtomicReferenceArray<E> buffer = consumerBuffer;
    long index = lpConsumerIndex();
    long mask = consumerMask;
    int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(index, mask);
    Object e = AtomicQueueUtil.lvRefElement(buffer, offset);
    if ((e == null) && (index != lvProducerIndex())) {
      do
      {
        e = AtomicQueueUtil.lvRefElement(buffer, offset);
      } while (e == null);
    }
    if (e == JUMP) {
      return newBufferPeek(nextBuffer(buffer, mask), index);
    }
    return e;
  }
  


  private int offerSlowPath(long mask, long pIndex, long producerLimit)
  {
    long cIndex = lvConsumerIndex();
    long bufferCapacity = getCurrentBufferCapacity(mask);
    if (cIndex + bufferCapacity > pIndex) {
      if (!casProducerLimit(producerLimit, cIndex + bufferCapacity))
      {
        return 1;
      }
      
      return 0;
    }
    
    if (availableInQueue(pIndex, cIndex) <= 0L)
    {
      return 2;
    }
    if (casProducerIndex(pIndex, pIndex + 1L))
    {
      return 3;
    }
    
    return 1;
  }
  


  protected abstract long availableInQueue(long paramLong1, long paramLong2);
  


  private AtomicReferenceArray<E> nextBuffer(AtomicReferenceArray<E> buffer, long mask)
  {
    int offset = nextArrayOffset(mask);
    AtomicReferenceArray<E> nextBuffer = (AtomicReferenceArray)AtomicQueueUtil.lvRefElement(buffer, offset);
    consumerBuffer = nextBuffer;
    consumerMask = (AtomicQueueUtil.length(nextBuffer) - 2 << 1);
    AtomicQueueUtil.soRefElement(buffer, offset, BUFFER_CONSUMED);
    return nextBuffer;
  }
  
  private static int nextArrayOffset(long mask) {
    return AtomicQueueUtil.modifiedCalcCircularRefElementOffset(mask + 2L, Long.MAX_VALUE);
  }
  
  private E newBufferPoll(AtomicReferenceArray<E> nextBuffer, long index) {
    int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(index, consumerMask);
    E n = AtomicQueueUtil.lvRefElement(nextBuffer, offset);
    if (n == null) {
      throw new IllegalStateException("new buffer must have at least one element");
    }
    AtomicQueueUtil.soRefElement(nextBuffer, offset, null);
    soConsumerIndex(index + 2L);
    return n;
  }
  
  private E newBufferPeek(AtomicReferenceArray<E> nextBuffer, long index) {
    int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(index, consumerMask);
    E n = AtomicQueueUtil.lvRefElement(nextBuffer, offset);
    if (null == n) {
      throw new IllegalStateException("new buffer must have at least one element");
    }
    return n;
  }
  
  public long currentProducerIndex()
  {
    return lvProducerIndex() / 2L;
  }
  
  public long currentConsumerIndex()
  {
    return lvConsumerIndex() / 2L;
  }
  

  public abstract int capacity();
  
  public boolean relaxedOffer(E e)
  {
    return offer(e);
  }
  

  public E relaxedPoll()
  {
    AtomicReferenceArray<E> buffer = consumerBuffer;
    long index = lpConsumerIndex();
    long mask = consumerMask;
    int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(index, mask);
    Object e = AtomicQueueUtil.lvRefElement(buffer, offset);
    if (e == null) {
      return null;
    }
    if (e == JUMP) {
      AtomicReferenceArray<E> nextBuffer = nextBuffer(buffer, mask);
      return newBufferPoll(nextBuffer, index);
    }
    AtomicQueueUtil.soRefElement(buffer, offset, null);
    soConsumerIndex(index + 2L);
    return e;
  }
  

  public E relaxedPeek()
  {
    AtomicReferenceArray<E> buffer = consumerBuffer;
    long index = lpConsumerIndex();
    long mask = consumerMask;
    int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(index, mask);
    Object e = AtomicQueueUtil.lvRefElement(buffer, offset);
    if (e == JUMP) {
      return newBufferPeek(nextBuffer(buffer, mask), index);
    }
    return e;
  }
  

  public int fill(MessagePassingQueue.Supplier<E> s)
  {
    long result = 0L;
    int capacity = capacity();
    do {
      int filled = fill(s, PortableJvmInfo.RECOMENDED_OFFER_BATCH);
      if (filled == 0) {
        return (int)result;
      }
      result += filled;
    } while (result <= capacity);
    return (int)result;
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
    long pIndex;
    long mask;
    AtomicReferenceArray<E> buffer;
    for (;;) {
      long producerLimit = lvProducerLimit();
      pIndex = lvProducerIndex();
      
      if ((pIndex & 1L) != 1L)
      {





        mask = producerMask;
        buffer = producerBuffer;
        


        long batchIndex = Math.min(producerLimit, pIndex + 2L * limit);
        if (pIndex >= producerLimit) {
          int result = offerSlowPath(mask, pIndex, producerLimit);
          switch (result)
          {
          case 0: 
          case 1: 
            break;
          case 2: 
            return 0;
          case 3: 
            resize(mask, buffer, pIndex, null, s);
            return 1;
          }
          
        }
        else if (casProducerIndex(pIndex, batchIndex)) {
          int claimedSlots = (int)((batchIndex - pIndex) / 2L);
          break;
        } } }
    int claimedSlots;
    for (int i = 0; i < claimedSlots; i++) {
      int offset = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(pIndex + 2L * i, mask);
      AtomicQueueUtil.soRefElement(buffer, offset, s.get());
    }
    return claimedSlots;
  }
  
  public void fill(MessagePassingQueue.Supplier<E> s, MessagePassingQueue.WaitStrategy wait, MessagePassingQueue.ExitCondition exit)
  {
    MessagePassingQueueUtil.fill(this, s, wait, exit);
  }
  
  public int drain(MessagePassingQueue.Consumer<E> c)
  {
    return drain(c, capacity());
  }
  
  public int drain(MessagePassingQueue.Consumer<E> c, int limit)
  {
    return MessagePassingQueueUtil.drain(this, c, limit);
  }
  
  public void drain(MessagePassingQueue.Consumer<E> c, MessagePassingQueue.WaitStrategy wait, MessagePassingQueue.ExitCondition exit)
  {
    MessagePassingQueueUtil.drain(this, c, wait, exit);
  }
  










  public Iterator<E> iterator()
  {
    return new WeakIterator(consumerBuffer, lvConsumerIndex(), lvProducerIndex());
  }
  

  private static class WeakIterator<E>
    implements Iterator<E>
  {
    private final long pIndex;
    
    private long nextIndex;
    
    private E nextElement;
    
    private AtomicReferenceArray<E> currentBuffer;
    
    private int mask;
    

    WeakIterator(AtomicReferenceArray<E> currentBuffer, long cIndex, long pIndex)
    {
      this.pIndex = (pIndex >> 1);
      nextIndex = (cIndex >> 1);
      setBuffer(currentBuffer);
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
      if (e == null) {
        throw new NoSuchElementException();
      }
      nextElement = getNext();
      return e;
    }
    
    private void setBuffer(AtomicReferenceArray<E> buffer) {
      currentBuffer = buffer;
      mask = (AtomicQueueUtil.length(buffer) - 2);
    }
    
    private E getNext() {
      while (nextIndex < pIndex) {
        long index = nextIndex++;
        E e = AtomicQueueUtil.lvRefElement(currentBuffer, AtomicQueueUtil.calcCircularRefElementOffset(index, mask));
        
        if (e != null)
        {


          if (e != BaseMpscLinkedAtomicArrayQueue.JUMP) {
            return e;
          }
          
          int nextBufferIndex = mask + 1;
          Object nextBuffer = AtomicQueueUtil.lvRefElement(currentBuffer, AtomicQueueUtil.calcRefElementOffset(nextBufferIndex));
          if ((nextBuffer == BaseMpscLinkedAtomicArrayQueue.BUFFER_CONSUMED) || (nextBuffer == null))
          {
            return null;
          }
          setBuffer((AtomicReferenceArray)nextBuffer);
          
          e = AtomicQueueUtil.lvRefElement(currentBuffer, AtomicQueueUtil.calcCircularRefElementOffset(index, mask));
          
          if (e != null)
          {

            return e; }
        }
      }
      return null;
    }
  }
  
  private void resize(long oldMask, AtomicReferenceArray<E> oldBuffer, long pIndex, E e, MessagePassingQueue.Supplier<E> s) {
    assert (((e != null) && (s == null)) || (e == null) || (s != null));
    int newBufferLength = getNextBufferSize(oldBuffer);
    try
    {
      newBuffer = AtomicQueueUtil.allocateRefArray(newBufferLength);
    } catch (OutOfMemoryError oom) { AtomicReferenceArray<E> newBuffer;
      assert (lvProducerIndex() == pIndex + 1L);
      soProducerIndex(pIndex);
      throw oom; }
    AtomicReferenceArray<E> newBuffer;
    producerBuffer = newBuffer;
    int newMask = newBufferLength - 2 << 1;
    producerMask = newMask;
    int offsetInOld = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(pIndex, oldMask);
    int offsetInNew = AtomicQueueUtil.modifiedCalcCircularRefElementOffset(pIndex, newMask);
    
    AtomicQueueUtil.soRefElement(newBuffer, offsetInNew, e == null ? s.get() : e);
    
    AtomicQueueUtil.soRefElement(oldBuffer, nextArrayOffset(oldMask), newBuffer);
    
    long cIndex = lvConsumerIndex();
    long availableInQueue = availableInQueue(pIndex, cIndex);
    RangeUtil.checkPositive(availableInQueue, "availableInQueue");
    

    soProducerLimit(pIndex + Math.min(newMask, availableInQueue));
    
    soProducerIndex(pIndex + 2L);
    

    AtomicQueueUtil.soRefElement(oldBuffer, offsetInOld, JUMP);
  }
  
  protected abstract int getNextBufferSize(AtomicReferenceArray<E> paramAtomicReferenceArray);
  
  protected abstract long getCurrentBufferCapacity(long paramLong);
}
