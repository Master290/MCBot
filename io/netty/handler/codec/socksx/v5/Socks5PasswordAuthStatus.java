package io.netty.handler.codec.socksx.v5;

import io.netty.util.internal.ObjectUtil;



















public class Socks5PasswordAuthStatus
  implements Comparable<Socks5PasswordAuthStatus>
{
  public static final Socks5PasswordAuthStatus SUCCESS = new Socks5PasswordAuthStatus(0, "SUCCESS");
  public static final Socks5PasswordAuthStatus FAILURE = new Socks5PasswordAuthStatus(255, "FAILURE");
  private final byte byteValue;
  
  public static Socks5PasswordAuthStatus valueOf(byte b) { switch (b) {
    case 0: 
      return SUCCESS;
    case -1: 
      return FAILURE;
    }
    
    return new Socks5PasswordAuthStatus(b);
  }
  

  private final String name;
  private String text;
  public Socks5PasswordAuthStatus(int byteValue)
  {
    this(byteValue, "UNKNOWN");
  }
  
  public Socks5PasswordAuthStatus(int byteValue, String name) {
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
    this.byteValue = ((byte)byteValue);
  }
  
  public byte byteValue() {
    return byteValue;
  }
  
  public boolean isSuccess() {
    return byteValue == 0;
  }
  
  public int hashCode()
  {
    return byteValue;
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Socks5PasswordAuthStatus)) {
      return false;
    }
    
    return byteValue == byteValue;
  }
  
  public int compareTo(Socks5PasswordAuthStatus o)
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
