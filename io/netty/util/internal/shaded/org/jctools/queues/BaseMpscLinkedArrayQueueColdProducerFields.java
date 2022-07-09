package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;









































































































































abstract class BaseMpscLinkedArrayQueueColdProducerFields<E>
  extends BaseMpscLinkedArrayQueuePad3<E>
{
  private static final long P_LIMIT_OFFSET = UnsafeAccess.fieldOffset(BaseMpscLinkedArrayQueueColdProducerFields.class, "producerLimit");
  private volatile long producerLimit;
  protected long producerMask;
  protected E[] producerBuffer;
  
  BaseMpscLinkedArrayQueueColdProducerFields() {}
  
  final long lvProducerLimit() {
    return producerLimit;
  }
  
  final boolean casProducerLimit(long expect, long newValue)
  {
    return UnsafeAccess.UNSAFE.compareAndSwapLong(this, P_LIMIT_OFFSET, expect, newValue);
  }
  
  final void soProducerLimit(long newValue)
  {
    UnsafeAccess.UNSAFE.putOrderedLong(this, P_LIMIT_OFFSET, newValue);
  }
}
