package io.netty.util.internal.shaded.org.jctools.queues;



















public final class IndexedQueueSizeUtil
{
  public IndexedQueueSizeUtil() {}
  


















  public static int size(IndexedQueue iq)
  {
    long after = iq.lvConsumerIndex();
    
    for (;;)
    {
      long before = after;
      long currentProducerIndex = iq.lvProducerIndex();
      after = iq.lvConsumerIndex();
      if (before == after)
      {
        long size = currentProducerIndex - after;
        break;
      }
    }
    
    long size;
    if (size > 2147483647L)
    {
      return Integer.MAX_VALUE;
    }
    

    if (size < 0L)
    {
      return 0;
    }
    if ((iq.capacity() != -1) && (size > iq.capacity()))
    {
      return iq.capacity();
    }
    

    return (int)size;
  }
  





  public static boolean isEmpty(IndexedQueue iq)
  {
    return iq.lvConsumerIndex() >= iq.lvProducerIndex();
  }
  
  public static abstract interface IndexedQueue
  {
    public abstract long lvConsumerIndex();
    
    public abstract long lvProducerIndex();
    
    public abstract int capacity();
  }
}
