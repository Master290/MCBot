package io.netty.handler.codec.http;

import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;



















@Deprecated
public class DefaultCookie
  extends io.netty.handler.codec.http.cookie.DefaultCookie
  implements Cookie
{
  private String comment;
  private String commentUrl;
  private boolean discard;
  private Set<Integer> ports = Collections.emptySet();
  private Set<Integer> unmodifiablePorts = ports;
  
  private int version;
  

  public DefaultCookie(String name, String value)
  {
    super(name, value);
  }
  
  @Deprecated
  public String getName()
  {
    return name();
  }
  
  @Deprecated
  public String getValue()
  {
    return value();
  }
  
  @Deprecated
  public String getDomain()
  {
    return domain();
  }
  
  @Deprecated
  public String getPath()
  {
    return path();
  }
  
  @Deprecated
  public String getComment()
  {
    return comment();
  }
  
  @Deprecated
  public String comment()
  {
    return comment;
  }
  
  @Deprecated
  public void setComment(String comment)
  {
    this.comment = validateValue("comment", comment);
  }
  
  @Deprecated
  public String getCommentUrl()
  {
    return commentUrl();
  }
  
  @Deprecated
  public String commentUrl()
  {
    return commentUrl;
  }
  
  @Deprecated
  public void setCommentUrl(String commentUrl)
  {
    this.commentUrl = validateValue("commentUrl", commentUrl);
  }
  
  @Deprecated
  public boolean isDiscard()
  {
    return discard;
  }
  
  @Deprecated
  public void setDiscard(boolean discard)
  {
    this.discard = discard;
  }
  
  @Deprecated
  public Set<Integer> getPorts()
  {
    return ports();
  }
  
  @Deprecated
  public Set<Integer> ports()
  {
    if (unmodifiablePorts == null) {
      unmodifiablePorts = Collections.unmodifiableSet(ports);
    }
    return unmodifiablePorts;
  }
  
  @Deprecated
  public void setPorts(int... ports)
  {
    ObjectUtil.checkNotNull(ports, "ports");
    
    int[] portsCopy = (int[])ports.clone();
    if (portsCopy.length == 0) {
      unmodifiablePorts = (this.ports = Collections.emptySet());
    } else {
      Set<Integer> newPorts = new TreeSet();
      for (int p : portsCopy) {
        if ((p <= 0) || (p > 65535)) {
          throw new IllegalArgumentException("port out of range: " + p);
        }
        newPorts.add(Integer.valueOf(p));
      }
      this.ports = newPorts;
      unmodifiablePorts = null;
    }
  }
  
  @Deprecated
  public void setPorts(Iterable<Integer> ports)
  {
    Set<Integer> newPorts = new TreeSet();
    for (Iterator localIterator = ports.iterator(); localIterator.hasNext();) { int p = ((Integer)localIterator.next()).intValue();
      if ((p <= 0) || (p > 65535)) {
        throw new IllegalArgumentException("port out of range: " + p);
      }
      newPorts.add(Integer.valueOf(p));
    }
    if (newPorts.isEmpty()) {
      unmodifiablePorts = (this.ports = Collections.emptySet());
    } else {
      this.ports = newPorts;
      unmodifiablePorts = null;
    }
  }
  
  @Deprecated
  public long getMaxAge()
  {
    return maxAge();
  }
  
  @Deprecated
  public int getVersion()
  {
    return version();
  }
  
  @Deprecated
  public int version()
  {
    return version;
  }
  
  @Deprecated
  public void setVersion(int version)
  {
    this.version = version;
  }
}
