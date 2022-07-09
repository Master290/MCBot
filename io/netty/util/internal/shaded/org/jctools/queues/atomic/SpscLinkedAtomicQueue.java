package io.netty.util.internal.shaded.org.jctools.queues.atomic;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.ExitCondition;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.WaitStrategy;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueueUtil;



































public class SpscLinkedAtomicQueue<E>
  extends BaseLinkedAtomicQueue<E>
{
  public SpscLinkedAtomicQueue()
  {
    LinkedQueueAtomicNode<E> node = newNode();
    spProducerNode(node);
    spConsumerNode(node);
    
    node.soNext(null);
  }
  















  public boolean offer(E e)
  {
    if (null == e) {
      throw new NullPointerException();
    }
    LinkedQueueAtomicNode<E> nextNode = newNode(e);
    LinkedQueueAtomicNode<E> oldNode = lpProducerNode();
    soProducerNode(nextNode);
    


    oldNode.soNext(nextNode);
    return true;
  }
  
  public int fill(MessagePassingQueue.Supplier<E> s)
  {
    return MessagePassingQueueUtil.fillUnbounded(this, s);
  }
  
  public int fill(MessagePassingQueue.Supplier<E> s, int limit)
  {
    if (null == s)
      throw new IllegalArgumentException("supplier is null");
    if (limit < 0)
      throw new IllegalArgumentException("limit is negative:" + limit);
    if (limit == 0)
      return 0;
    LinkedQueueAtomicNode<E> tail = newNode(s.get());
    LinkedQueueAtomicNode<E> head = tail;
    for (int i = 1; i < limit; i++) {
      LinkedQueueAtomicNode<E> temp = newNode(s.get());
      
      tail.spNext(temp);
      tail = temp;
    }
    LinkedQueueAtomicNode<E> oldPNode = lpProducerNode();
    soProducerNode(tail);
    
    oldPNode.soNext(head);
    return limit;
  }
  
  public void fill(MessagePassingQueue.Supplier<E> s, MessagePassingQueue.WaitStrategy wait, MessagePassingQueue.ExitCondition exit)
  {
    MessagePassingQueueUtil.fill(this, s, wait, exit);
  }
}
