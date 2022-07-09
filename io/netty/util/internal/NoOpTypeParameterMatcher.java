package io.netty.util.internal;







public final class NoOpTypeParameterMatcher
  extends TypeParameterMatcher
{
  public NoOpTypeParameterMatcher() {}
  






  public boolean match(Object msg)
  {
    return true;
  }
}
