package io.netty.util.internal.shaded.org.jctools.queues.atomic;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;























































































abstract class BaseMpscLinkedAtomicArrayQueueProducerFields<E>
  extends BaseMpscLinkedAtomicArrayQueuePad1<E>
{
  private static final AtomicLongFieldUpdater<BaseMpscLinkedAtomicArrayQueueProducerFields> P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater(BaseMpscLinkedAtomicArrayQueueProducerFields.class, "producerIndex");
  private volatile long producerIndex;
  
  BaseMpscLinkedAtomicArrayQueueProducerFields() {}
  
  public final long lvProducerIndex() {
    return producerIndex;
  }
  
  final void soProducerIndex(long newValue) {
    P_INDEX_UPDATER.lazySet(this, newValue);
  }
  
  final boolean casProducerIndex(long expect, long newValue) {
    return P_INDEX_UPDATER.compareAndSet(this, expect, newValue);
  }
}
