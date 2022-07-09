package io.netty.channel.local;

import io.netty.channel.Channel;
import io.netty.util.internal.ObjectUtil;
import java.net.SocketAddress;





















public final class LocalAddress
  extends SocketAddress
  implements Comparable<LocalAddress>
{
  private static final long serialVersionUID = 4644331421130916435L;
  public static final LocalAddress ANY = new LocalAddress("ANY");
  

  private final String id;
  

  private final String strVal;
  

  LocalAddress(Channel channel)
  {
    StringBuilder buf = new StringBuilder(16);
    buf.append("local:E");
    buf.append(Long.toHexString(channel.hashCode() & 0xFFFFFFFF | 0x100000000));
    buf.setCharAt(7, ':');
    id = buf.substring(6);
    strVal = buf.toString();
  }
  


  public LocalAddress(String id)
  {
    this.id = ObjectUtil.checkNonEmptyAfterTrim(id, "id").toLowerCase();
    strVal = ("local:" + this.id);
  }
  


  public String id()
  {
    return id;
  }
  
  public int hashCode()
  {
    return id.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof LocalAddress)) {
      return false;
    }
    
    return id.equals(id);
  }
  
  public int compareTo(LocalAddress o)
  {
    return id.compareTo(id);
  }
  
  public String toString()
  {
    return strVal;
  }
}
