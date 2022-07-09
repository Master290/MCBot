package io.netty.util.concurrent;

public abstract interface ThreadProperties
{
  public abstract Thread.State state();
  
  public abstract int priority();
  
  public abstract boolean isInterrupted();
  
  public abstract boolean isDaemon();
  
  public abstract String name();
  
  public abstract long id();
  
  public abstract StackTraceElement[] stackTrace();
  
  public abstract boolean isAlive();
}
