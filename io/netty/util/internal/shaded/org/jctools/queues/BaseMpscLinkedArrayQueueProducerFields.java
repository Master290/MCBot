package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;















































abstract class BaseMpscLinkedArrayQueueProducerFields<E>
  extends BaseMpscLinkedArrayQueuePad1<E>
{
  private static final long P_INDEX_OFFSET = UnsafeAccess.fieldOffset(BaseMpscLinkedArrayQueueProducerFields.class, "producerIndex");
  private volatile long producerIndex;
  
  BaseMpscLinkedArrayQueueProducerFields() {}
  
  public final long lvProducerIndex()
  {
    return producerIndex;
  }
  
  final void soProducerIndex(long newValue)
  {
    UnsafeAccess.UNSAFE.putOrderedLong(this, P_INDEX_OFFSET, newValue);
  }
  
  final boolean casProducerIndex(long expect, long newValue)
  {
    return UnsafeAccess.UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, expect, newValue);
  }
}
