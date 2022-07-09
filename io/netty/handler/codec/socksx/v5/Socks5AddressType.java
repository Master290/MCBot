package io.netty.handler.codec.socksx.v5;

import io.netty.util.internal.ObjectUtil;



















public class Socks5AddressType
  implements Comparable<Socks5AddressType>
{
  public static final Socks5AddressType IPv4 = new Socks5AddressType(1, "IPv4");
  public static final Socks5AddressType DOMAIN = new Socks5AddressType(3, "DOMAIN");
  public static final Socks5AddressType IPv6 = new Socks5AddressType(4, "IPv6");
  private final byte byteValue;
  
  public static Socks5AddressType valueOf(byte b) { switch (b) {
    case 1: 
      return IPv4;
    case 3: 
      return DOMAIN;
    case 4: 
      return IPv6;
    }
    
    return new Socks5AddressType(b);
  }
  

  private final String name;
  private String text;
  public Socks5AddressType(int byteValue)
  {
    this(byteValue, "UNKNOWN");
  }
  
  public Socks5AddressType(int byteValue, String name) {
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
    this.byteValue = ((byte)byteValue);
  }
  
  public byte byteValue() {
    return byteValue;
  }
  
  public int hashCode()
  {
    return byteValue;
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Socks5AddressType)) {
      return false;
    }
    
    return byteValue == byteValue;
  }
  
  public int compareTo(Socks5AddressType o)
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
