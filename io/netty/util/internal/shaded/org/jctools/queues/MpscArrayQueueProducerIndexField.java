package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;








































abstract class MpscArrayQueueProducerIndexField<E>
  extends MpscArrayQueueL1Pad<E>
{
  private static final long P_INDEX_OFFSET = UnsafeAccess.fieldOffset(MpscArrayQueueProducerIndexField.class, "producerIndex");
  
  private volatile long producerIndex;
  
  MpscArrayQueueProducerIndexField(int capacity)
  {
    super(capacity);
  }
  

  public final long lvProducerIndex()
  {
    return producerIndex;
  }
  
  final boolean casProducerIndex(long expect, long newValue)
  {
    return UnsafeAccess.UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, expect, newValue);
  }
}
