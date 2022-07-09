package io.netty.util.internal.shaded.org.jctools.queues.atomic;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
















































































abstract class BaseLinkedAtomicQueueProducerNodeRef<E>
  extends BaseLinkedAtomicQueuePad0<E>
{
  private static final AtomicReferenceFieldUpdater<BaseLinkedAtomicQueueProducerNodeRef, LinkedQueueAtomicNode> P_NODE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(BaseLinkedAtomicQueueProducerNodeRef.class, LinkedQueueAtomicNode.class, "producerNode");
  private volatile LinkedQueueAtomicNode<E> producerNode;
  
  BaseLinkedAtomicQueueProducerNodeRef() {}
  
  final void spProducerNode(LinkedQueueAtomicNode<E> newValue) { P_NODE_UPDATER.lazySet(this, newValue); }
  
  final void soProducerNode(LinkedQueueAtomicNode<E> newValue)
  {
    P_NODE_UPDATER.lazySet(this, newValue);
  }
  
  final LinkedQueueAtomicNode<E> lvProducerNode() {
    return producerNode;
  }
  
  final boolean casProducerNode(LinkedQueueAtomicNode<E> expect, LinkedQueueAtomicNode<E> newValue) {
    return P_NODE_UPDATER.compareAndSet(this, expect, newValue);
  }
  
  final LinkedQueueAtomicNode<E> lpProducerNode() {
    return producerNode;
  }
  
  protected final LinkedQueueAtomicNode<E> xchgProducerNode(LinkedQueueAtomicNode<E> newValue) {
    return (LinkedQueueAtomicNode)P_NODE_UPDATER.getAndSet(this, newValue);
  }
}
