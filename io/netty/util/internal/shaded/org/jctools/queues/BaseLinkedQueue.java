package io.netty.util.internal.shaded.org.jctools.queues;

import java.util.Iterator;












































































































































abstract class BaseLinkedQueue<E>
  extends BaseLinkedQueuePad2<E>
{
  BaseLinkedQueue() {}
  
  public final Iterator<E> iterator()
  {
    throw new UnsupportedOperationException();
  }
  

  public String toString()
  {
    return getClass().getName();
  }
  
  protected final LinkedQueueNode<E> newNode()
  {
    return new LinkedQueueNode();
  }
  
  protected final LinkedQueueNode<E> newNode(E e)
  {
    return new LinkedQueueNode(e);
  }
  













  public final int size()
  {
    LinkedQueueNode<E> chaserNode = lvConsumerNode();
    LinkedQueueNode<E> producerNode = lvProducerNode();
    int size = 0;
    
    while ((chaserNode != producerNode) && (chaserNode != null) && (size < Integer.MAX_VALUE))
    {



      LinkedQueueNode<E> next = chaserNode.lvNext();
      
      if (next == chaserNode)
      {
        return size;
      }
      chaserNode = next;
      size++;
    }
    return size;
  }
  











  public boolean isEmpty()
  {
    LinkedQueueNode<E> consumerNode = lvConsumerNode();
    LinkedQueueNode<E> producerNode = lvProducerNode();
    return consumerNode == producerNode;
  }
  

  protected E getSingleConsumerNodeValue(LinkedQueueNode<E> currConsumerNode, LinkedQueueNode<E> nextNode)
  {
    E nextValue = nextNode.getAndNullValue();
    



    currConsumerNode.soNext(currConsumerNode);
    spConsumerNode(nextNode);
    
    return nextValue;
  }
  






















  public E poll()
  {
    LinkedQueueNode<E> currConsumerNode = lpConsumerNode();
    LinkedQueueNode<E> nextNode = currConsumerNode.lvNext();
    if (nextNode != null)
    {
      return getSingleConsumerNodeValue(currConsumerNode, nextNode);
    }
    if (currConsumerNode != lvProducerNode())
    {
      nextNode = spinWaitForNextNode(currConsumerNode);
      
      return getSingleConsumerNodeValue(currConsumerNode, nextNode);
    }
    return null;
  }
  



















  public E peek()
  {
    LinkedQueueNode<E> currConsumerNode = lpConsumerNode();
    LinkedQueueNode<E> nextNode = currConsumerNode.lvNext();
    if (nextNode != null)
    {
      return nextNode.lpValue();
    }
    if (currConsumerNode != lvProducerNode())
    {
      nextNode = spinWaitForNextNode(currConsumerNode);
      
      return nextNode.lpValue();
    }
    return null;
  }
  
  LinkedQueueNode<E> spinWaitForNextNode(LinkedQueueNode<E> currNode)
  {
    LinkedQueueNode<E> nextNode;
    while ((nextNode = currNode.lvNext()) == null) {}
    


    return nextNode;
  }
  

  public E relaxedPoll()
  {
    LinkedQueueNode<E> currConsumerNode = lpConsumerNode();
    LinkedQueueNode<E> nextNode = currConsumerNode.lvNext();
    if (nextNode != null)
    {
      return getSingleConsumerNodeValue(currConsumerNode, nextNode);
    }
    return null;
  }
  

  public E relaxedPeek()
  {
    LinkedQueueNode<E> nextNode = lpConsumerNode().lvNext();
    if (nextNode != null)
    {
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
    if (limit == 0) {
      return 0;
    }
    LinkedQueueNode<E> chaserNode = lpConsumerNode();
    for (int i = 0; i < limit; i++)
    {
      LinkedQueueNode<E> nextNode = chaserNode.lvNext();
      
      if (nextNode == null)
      {
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
