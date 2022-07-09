package io.netty.channel.group;

import io.netty.util.internal.ObjectUtil;
import java.util.Iterator;
import java.util.NoSuchElementException;

















final class CombinedIterator<E>
  implements Iterator<E>
{
  private final Iterator<E> i1;
  private final Iterator<E> i2;
  private Iterator<E> currentIterator;
  
  CombinedIterator(Iterator<E> i1, Iterator<E> i2)
  {
    this.i1 = ((Iterator)ObjectUtil.checkNotNull(i1, "i1"));
    this.i2 = ((Iterator)ObjectUtil.checkNotNull(i2, "i2"));
    currentIterator = i1;
  }
  
  public boolean hasNext()
  {
    for (;;) {
      if (currentIterator.hasNext()) {
        return true;
      }
      
      if (currentIterator != i1) break;
      currentIterator = i2;
    }
    return false;
  }
  
  public E next()
  {
    for (;;)
    {
      try
      {
        return currentIterator.next();
      } catch (NoSuchElementException e) {
        if (currentIterator == i1) {
          currentIterator = i2;
        } else {
          throw e;
        }
      }
    }
  }
  
  public void remove()
  {
    currentIterator.remove();
  }
}
