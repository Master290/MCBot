package io.netty.handler.codec.http.cookie;

import io.netty.util.internal.ObjectUtil;























public class DefaultCookie
  implements Cookie
{
  private final String name;
  private String value;
  private boolean wrap;
  private String domain;
  private String path;
  private long maxAge = Long.MIN_VALUE;
  
  private boolean secure;
  
  private boolean httpOnly;
  private CookieHeaderNames.SameSite sameSite;
  
  public DefaultCookie(String name, String value)
  {
    this.name = ObjectUtil.checkNonEmptyAfterTrim(name, "name");
    setValue(value);
  }
  
  public String name()
  {
    return name;
  }
  
  public String value()
  {
    return value;
  }
  
  public void setValue(String value)
  {
    this.value = ((String)ObjectUtil.checkNotNull(value, "value"));
  }
  
  public boolean wrap()
  {
    return wrap;
  }
  
  public void setWrap(boolean wrap)
  {
    this.wrap = wrap;
  }
  
  public String domain()
  {
    return domain;
  }
  
  public void setDomain(String domain)
  {
    this.domain = CookieUtil.validateAttributeValue("domain", domain);
  }
  
  public String path()
  {
    return path;
  }
  
  public void setPath(String path)
  {
    this.path = CookieUtil.validateAttributeValue("path", path);
  }
  
  public long maxAge()
  {
    return maxAge;
  }
  
  public void setMaxAge(long maxAge)
  {
    this.maxAge = maxAge;
  }
  
  public boolean isSecure()
  {
    return secure;
  }
  
  public void setSecure(boolean secure)
  {
    this.secure = secure;
  }
  
  public boolean isHttpOnly()
  {
    return httpOnly;
  }
  
  public void setHttpOnly(boolean httpOnly)
  {
    this.httpOnly = httpOnly;
  }
  





  public CookieHeaderNames.SameSite sameSite()
  {
    return sameSite;
  }
  





  public void setSameSite(CookieHeaderNames.SameSite sameSite)
  {
    this.sameSite = sameSite;
  }
  
  public int hashCode()
  {
    return name().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    
    if (!(o instanceof Cookie)) {
      return false;
    }
    
    Cookie that = (Cookie)o;
    if (!name().equals(that.name())) {
      return false;
    }
    
    if (path() == null) {
      if (that.path() != null)
        return false;
    } else {
      if (that.path() == null)
        return false;
      if (!path().equals(that.path())) {
        return false;
      }
    }
    if (domain() == null) {
      if (that.domain() != null) {
        return false;
      }
    } else {
      return domain().equalsIgnoreCase(that.domain());
    }
    
    return true;
  }
  
  public int compareTo(Cookie c)
  {
    int v = name().compareTo(c.name());
    if (v != 0) {
      return v;
    }
    
    if (path() == null) {
      if (c.path() != null)
        return -1;
    } else {
      if (c.path() == null) {
        return 1;
      }
      v = path().compareTo(c.path());
      if (v != 0) {
        return v;
      }
    }
    
    if (domain() == null) {
      if (c.domain() != null)
        return -1;
    } else {
      if (c.domain() == null) {
        return 1;
      }
      v = domain().compareToIgnoreCase(c.domain());
      return v;
    }
    
    return 0;
  }
  







  @Deprecated
  protected String validateValue(String name, String value)
  {
    return CookieUtil.validateAttributeValue(name, value);
  }
  



  public String toString()
  {
    StringBuilder buf = CookieUtil.stringBuilder().append(name()).append('=').append(value());
    if (domain() != null)
    {
      buf.append(", domain=").append(domain());
    }
    if (path() != null)
    {
      buf.append(", path=").append(path());
    }
    if (maxAge() >= 0L)
    {

      buf.append(", maxAge=").append(maxAge()).append('s');
    }
    if (isSecure()) {
      buf.append(", secure");
    }
    if (isHttpOnly()) {
      buf.append(", HTTPOnly");
    }
    if (sameSite() != null) {
      buf.append(", SameSite=").append(sameSite());
    }
    return buf.toString();
  }
}
