package io.netty.util;

import java.util.concurrent.atomic.AtomicLong;


















public abstract class AbstractConstant<T extends AbstractConstant<T>>
  implements Constant<T>
{
  private static final AtomicLong uniqueIdGenerator = new AtomicLong();
  
  private final int id;
  
  private final String name;
  private final long uniquifier;
  
  protected AbstractConstant(int id, String name)
  {
    this.id = id;
    this.name = name;
    uniquifier = uniqueIdGenerator.getAndIncrement();
  }
  
  public final String name()
  {
    return name;
  }
  
  public final int id()
  {
    return id;
  }
  
  public final String toString()
  {
    return name();
  }
  
  public final int hashCode()
  {
    return super.hashCode();
  }
  
  public final boolean equals(Object obj)
  {
    return super.equals(obj);
  }
  
  public final int compareTo(T o)
  {
    if (this == o) {
      return 0;
    }
    

    AbstractConstant<T> other = o;
    

    int returnCode = hashCode() - other.hashCode();
    if (returnCode != 0) {
      return returnCode;
    }
    
    if (uniquifier < uniquifier) {
      return -1;
    }
    if (uniquifier > uniquifier) {
      return 1;
    }
    
    throw new Error("failed to compare two different constants");
  }
}
