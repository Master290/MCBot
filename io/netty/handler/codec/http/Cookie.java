package io.netty.handler.codec.http;

import java.util.Set;

@Deprecated
public abstract interface Cookie
  extends io.netty.handler.codec.http.cookie.Cookie
{
  @Deprecated
  public abstract String getName();
  
  @Deprecated
  public abstract String getValue();
  
  @Deprecated
  public abstract String getDomain();
  
  @Deprecated
  public abstract String getPath();
  
  @Deprecated
  public abstract String getComment();
  
  @Deprecated
  public abstract String comment();
  
  @Deprecated
  public abstract void setComment(String paramString);
  
  @Deprecated
  public abstract long getMaxAge();
  
  @Deprecated
  public abstract long maxAge();
  
  @Deprecated
  public abstract void setMaxAge(long paramLong);
  
  @Deprecated
  public abstract int getVersion();
  
  @Deprecated
  public abstract int version();
  
  @Deprecated
  public abstract void setVersion(int paramInt);
  
  @Deprecated
  public abstract String getCommentUrl();
  
  @Deprecated
  public abstract String commentUrl();
  
  @Deprecated
  public abstract void setCommentUrl(String paramString);
  
  @Deprecated
  public abstract boolean isDiscard();
  
  @Deprecated
  public abstract void setDiscard(boolean paramBoolean);
  
  @Deprecated
  public abstract Set<Integer> getPorts();
  
  @Deprecated
  public abstract Set<Integer> ports();
  
  @Deprecated
  public abstract void setPorts(int... paramVarArgs);
  
  @Deprecated
  public abstract void setPorts(Iterable<Integer> paramIterable);
}
