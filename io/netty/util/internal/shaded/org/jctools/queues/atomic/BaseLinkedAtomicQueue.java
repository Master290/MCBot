package io.netty.util.internal.shaded.org.jctools.queues.atomic;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.ExitCondition;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.WaitStrategy;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueueUtil;
import java.util.Iterator;






















































































































































































































































abstract class BaseLinkedAtomicQueue<E>
  extends BaseLinkedAtomicQueuePad2<E>
{
  BaseLinkedAtomicQueue() {}
  
  public final Iterator<E> iterator()
  {
    throw new UnsupportedOperationException();
  }
  
  public String toString()
  {
    return getClass().getName();
  }
  
  protected final LinkedQueueAtomicNode<E> newNode() {
    return new LinkedQueueAtomicNode();
  }
  
  protected final LinkedQueueAtomicNode<E> newNode(E e) {
    return new LinkedQueueAtomicNode(e);
  }
  












  public final int size()
  {
    LinkedQueueAtomicNode<E> chaserNode = lvConsumerNode();
    LinkedQueueAtomicNode<E> producerNode = lvProducerNode();
    int size = 0;
    
    while ((chaserNode != producerNode) && (chaserNode != null) && (size < Integer.MAX_VALUE))
    {



      LinkedQueueAtomicNode<E> next = chaserNode.lvNext();
      
      if (next == chaserNode) {
        return size;
      }
      chaserNode = next;
      size++;
    }
    return size;
  }
  










  public boolean isEmpty()
  {
    LinkedQueueAtomicNode<E> consumerNode = lvConsumerNode();
    LinkedQueueAtomicNode<E> producerNode = lvProducerNode();
    return consumerNode == producerNode;
  }
  
  protected E getSingleConsumerNodeValue(LinkedQueueAtomicNode<E> currConsumerNode, LinkedQueueAtomicNode<E> nextNode)
  {
    E nextValue = nextNode.getAndNullValue();
    


    currConsumerNode.soNext(currConsumerNode);
    spConsumerNode(nextNode);
    
    return nextValue;
  }
  





















  public E poll()
  {
    LinkedQueueAtomicNode<E> currConsumerNode = lpConsumerNode();
    LinkedQueueAtomicNode<E> nextNode = currConsumerNode.lvNext();
    if (nextNode != null)
      return getSingleConsumerNodeValue(currConsumerNode, nextNode);
    if (currConsumerNode != lvProducerNode()) {
      nextNode = spinWaitForNextNode(currConsumerNode);
      
      return getSingleConsumerNodeValue(currConsumerNode, nextNode);
    }
    return null;
  }
  


















  public E peek()
  {
    LinkedQueueAtomicNode<E> currConsumerNode = lpConsumerNode();
    LinkedQueueAtomicNode<E> nextNode = currConsumerNode.lvNext();
    if (nextNode != null)
      return nextNode.lpValue();
    if (currConsumerNode != lvProducerNode()) {
      nextNode = spinWaitForNextNode(currConsumerNode);
      
      return nextNode.lpValue();
    }
    return null;
  }
  
  LinkedQueueAtomicNode<E> spinWaitForNextNode(LinkedQueueAtomicNode<E> currNode) {
    LinkedQueueAtomicNode<E> nextNode;
    while ((nextNode = currNode.lvNext()) == null) {}
    

    return nextNode;
  }
  
  public E relaxedPoll()
  {
    LinkedQueueAtomicNode<E> currConsumerNode = lpConsumerNode();
    LinkedQueueAtomicNode<E> nextNode = currConsumerNode.lvNext();
    if (nextNode != null) {
      return getSingleConsumerNodeValue(currConsumerNode, nextNode);
    }
    return null;
  }
  
  public E relaxedPeek()
  {
    LinkedQueueAtomicNode<E> nextNode = lpConsumerNode().lvNext();
    if (nextNode != null) {
      return nextNode.lpValue();
    }
    return null;
  }
  
  public boolean relaxedOffer(E e)
  {
    return offer(e);
  }
  
  public int drain(MessagePassingQueue.Consumer<E> c, int limit)
  {
    if (null == c)
      throw new IllegalArgumentException("c is null");
    if (limit < 0)
      throw new IllegalArgumentException("limit is negative: " + limit);
    if (limit == 0)
      return 0;
    LinkedQueueAtomicNode<E> chaserNode = lpConsumerNode();
    for (int i = 0; i < limit; i++) {
      LinkedQueueAtomicNode<E> nextNode = chaserNode.lvNext();
      if (nextNode == null) {
        return i;
      }
      
      E nextValue = getSingleConsumerNodeValue(chaserNode, nextNode);
      chaserNode = nextNode;
      c.accept(nextValue);
    }
    return limit;
  }
  
  public int drain(MessagePassingQueue.Consumer<E> c)
  {
    return MessagePassingQueueUtil.drain(this, c);
  }
  
  public void drain(MessagePassingQueue.Consumer<E> c, MessagePassingQueue.WaitStrategy wait, MessagePassingQueue.ExitCondition exit)
  {
    MessagePassingQueueUtil.drain(this, c, wait, exit);
  }
  
  public int capacity()
  {
    return -1;
  }
}
