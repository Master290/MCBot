package io.netty.channel.kqueue;

import io.netty.util.internal.ObjectUtil;

















public final class AcceptFilter
{
  static final AcceptFilter PLATFORM_UNSUPPORTED = new AcceptFilter("", "");
  private final String filterName;
  private final String filterArgs;
  
  public AcceptFilter(String filterName, String filterArgs) {
    this.filterName = ((String)ObjectUtil.checkNotNull(filterName, "filterName"));
    this.filterArgs = ((String)ObjectUtil.checkNotNull(filterArgs, "filterArgs"));
  }
  
  public String filterName() {
    return filterName;
  }
  
  public String filterArgs() {
    return filterArgs;
  }
  
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AcceptFilter)) {
      return false;
    }
    AcceptFilter rhs = (AcceptFilter)o;
    return (filterName.equals(filterName)) && (filterArgs.equals(filterArgs));
  }
  
  public int hashCode()
  {
    return 31 * (31 + filterName.hashCode()) + filterArgs.hashCode();
  }
  
  public String toString()
  {
    return filterName + ", " + filterArgs;
  }
}
