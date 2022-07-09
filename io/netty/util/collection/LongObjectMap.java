package io.netty.util.collection;

import java.util.Map;

public abstract interface LongObjectMap<V>
  extends Map<Long, V>
{
  public abstract V get(long paramLong);
  
  public abstract V put(long paramLong, V paramV);
  
  public abstract V remove(long paramLong);
  
  public abstract Iterable<PrimitiveEntry<V>> entries();
  
  public abstract boolean containsKey(long paramLong);
  
  public static abstract interface PrimitiveEntry<V>
  {
    public abstract long key();
    
    public abstract V value();
    
    public abstract void setValue(V paramV);
  }
}
