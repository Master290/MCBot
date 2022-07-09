package io.netty.util.internal;

import java.util.Iterator;














public final class ReadOnlyIterator<T>
  implements Iterator<T>
{
  private final Iterator<? extends T> iterator;
  
  public ReadOnlyIterator(Iterator<? extends T> iterator)
  {
    this.iterator = ((Iterator)ObjectUtil.checkNotNull(iterator, "iterator"));
  }
  
  public boolean hasNext()
  {
    return iterator.hasNext();
  }
  
  public T next()
  {
    return iterator.next();
  }
  
  public void remove()
  {
    throw new UnsupportedOperationException("read-only");
  }
}
