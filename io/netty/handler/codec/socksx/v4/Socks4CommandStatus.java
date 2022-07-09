package io.netty.handler.codec.socksx.v4;

import io.netty.util.internal.ObjectUtil;


















public class Socks4CommandStatus
  implements Comparable<Socks4CommandStatus>
{
  public static final Socks4CommandStatus SUCCESS = new Socks4CommandStatus(90, "SUCCESS");
  public static final Socks4CommandStatus REJECTED_OR_FAILED = new Socks4CommandStatus(91, "REJECTED_OR_FAILED");
  public static final Socks4CommandStatus IDENTD_UNREACHABLE = new Socks4CommandStatus(92, "IDENTD_UNREACHABLE");
  public static final Socks4CommandStatus IDENTD_AUTH_FAILURE = new Socks4CommandStatus(93, "IDENTD_AUTH_FAILURE");
  private final byte byteValue;
  
  public static Socks4CommandStatus valueOf(byte b) { switch (b) {
    case 90: 
      return SUCCESS;
    case 91: 
      return REJECTED_OR_FAILED;
    case 92: 
      return IDENTD_UNREACHABLE;
    case 93: 
      return IDENTD_AUTH_FAILURE;
    }
    
    return new Socks4CommandStatus(b);
  }
  

  private final String name;
  private String text;
  public Socks4CommandStatus(int byteValue)
  {
    this(byteValue, "UNKNOWN");
  }
  
  public Socks4CommandStatus(int byteValue, String name) {
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
    this.byteValue = ((byte)byteValue);
  }
  
  public byte byteValue() {
    return byteValue;
  }
  
  public boolean isSuccess() {
    return byteValue == 90;
  }
  
  public int hashCode()
  {
    return byteValue;
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Socks4CommandStatus)) {
      return false;
    }
    
    return byteValue == byteValue;
  }
  
  public int compareTo(Socks4CommandStatus o)
  {
    return byteValue - byteValue;
  }
  
  public String toString()
  {
    String text = this.text;
    if (text == null) {
      this.text = (text = name + '(' + (byteValue & 0xFF) + ')');
    }
    return text;
  }
}
