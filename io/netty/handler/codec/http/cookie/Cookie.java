package io.netty.handler.codec.http.cookie;

public abstract interface Cookie
  extends Comparable<Cookie>
{
  public static final long UNDEFINED_MAX_AGE = Long.MIN_VALUE;
  
  public abstract String name();
  
  public abstract String value();
  
  public abstract void setValue(String paramString);
  
  public abstract boolean wrap();
  
  public abstract void setWrap(boolean paramBoolean);
  
  public abstract String domain();
  
  public abstract void setDomain(String paramString);
  
  public abstract String path();
  
  public abstract void setPath(String paramString);
  
  public abstract long maxAge();
  
  public abstract void setMaxAge(long paramLong);
  
  public abstract boolean isSecure();
  
  public abstract void setSecure(boolean paramBoolean);
  
  public abstract boolean isHttpOnly();
  
  public abstract void setHttpOnly(boolean paramBoolean);
}
