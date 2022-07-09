package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;

























































































abstract class MpscArrayQueueProducerLimitField<E>
  extends MpscArrayQueueMidPad<E>
{
  private static final long P_LIMIT_OFFSET = UnsafeAccess.fieldOffset(MpscArrayQueueProducerLimitField.class, "producerLimit");
  
  private volatile long producerLimit;
  

  MpscArrayQueueProducerLimitField(int capacity)
  {
    super(capacity);
    producerLimit = capacity;
  }
  
  final long lvProducerLimit()
  {
    return producerLimit;
  }
  
  final void soProducerLimit(long newValue)
  {
    UnsafeAccess.UNSAFE.putOrderedLong(this, P_LIMIT_OFFSET, newValue);
  }
}
