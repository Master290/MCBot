package io.netty.util.internal.shaded.org.jctools.queues;





























public class SpscLinkedQueue<E>
  extends BaseLinkedQueue<E>
{
  public SpscLinkedQueue()
  {
    LinkedQueueNode<E> node = newNode();
    spProducerNode(node);
    spConsumerNode(node);
    node.soNext(null);
  }
  
















  public boolean offer(E e)
  {
    if (null == e)
    {
      throw new NullPointerException();
    }
    LinkedQueueNode<E> nextNode = newNode(e);
    LinkedQueueNode<E> oldNode = lpProducerNode();
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
    if (limit == 0) {
      return 0;
    }
    LinkedQueueNode<E> tail = newNode(s.get());
    LinkedQueueNode<E> head = tail;
    for (int i = 1; i < limit; i++)
    {
      LinkedQueueNode<E> temp = newNode(s.get());
      
      tail.spNext(temp);
      tail = temp;
    }
    LinkedQueueNode<E> oldPNode = lpProducerNode();
    soProducerNode(tail);
    
    oldPNode.soNext(head);
    return limit;
  }
  

  public void fill(MessagePassingQueue.Supplier<E> s, MessagePassingQueue.WaitStrategy wait, MessagePassingQueue.ExitCondition exit)
  {
    MessagePassingQueueUtil.fill(this, s, wait, exit);
  }
}
