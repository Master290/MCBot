package io.netty.util;


























































public abstract interface HashingStrategy<T>
{
  public static final HashingStrategy JAVA_HASHER = new HashingStrategy()
  {
    public int hashCode(Object obj) {
      return obj != null ? obj.hashCode() : 0;
    }
    
    public boolean equals(Object a, Object b)
    {
      return (a == b) || ((a != null) && (a.equals(b)));
    }
  };
  
  public abstract int hashCode(T paramT);
  
  public abstract boolean equals(T paramT1, T paramT2);
}
