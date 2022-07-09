package io.netty.util;

public abstract interface Constant<T extends Constant<T>>
  extends Comparable<T>
{
  public abstract int id();
  
  public abstract String name();
}
