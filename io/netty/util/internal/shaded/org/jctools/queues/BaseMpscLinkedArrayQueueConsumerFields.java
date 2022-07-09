package io.netty.util.internal.shaded.org.jctools.queues;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;



























































































abstract class BaseMpscLinkedArrayQueueConsumerFields<E>
  extends BaseMpscLinkedArrayQueuePad2<E>
{
  private static final long C_INDEX_OFFSET = UnsafeAccess.fieldOffset(BaseMpscLinkedArrayQueueConsumerFields.class, "consumerIndex");
  private volatile long consumerIndex;
  protected long consumerMask;
  protected E[] consumerBuffer;
  
  BaseMpscLinkedArrayQueueConsumerFields() {}
  
  public final long lvConsumerIndex()
  {
    return consumerIndex;
  }
  
  final long lpConsumerIndex()
  {
    return UnsafeAccess.UNSAFE.getLong(this, C_INDEX_OFFSET);
  }
  
  final void soConsumerIndex(long newValue)
  {
    UnsafeAccess.UNSAFE.putOrderedLong(this, C_INDEX_OFFSET, newValue);
  }
}
