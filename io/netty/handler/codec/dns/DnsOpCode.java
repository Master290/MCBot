package io.netty.handler.codec.dns;

import io.netty.util.internal.ObjectUtil;
























public class DnsOpCode
  implements Comparable<DnsOpCode>
{
  public static final DnsOpCode QUERY = new DnsOpCode(0, "QUERY");
  



  public static final DnsOpCode IQUERY = new DnsOpCode(1, "IQUERY");
  



  public static final DnsOpCode STATUS = new DnsOpCode(2, "STATUS");
  



  public static final DnsOpCode NOTIFY = new DnsOpCode(4, "NOTIFY");
  



  public static final DnsOpCode UPDATE = new DnsOpCode(5, "UPDATE");
  private final byte byteValue;
  private final String name;
  private String text;
  
  public static DnsOpCode valueOf(int b) {
    switch (b) {
    case 0: 
      return QUERY;
    case 1: 
      return IQUERY;
    case 2: 
      return STATUS;
    case 4: 
      return NOTIFY;
    case 5: 
      return UPDATE;
    }
    
    

    return new DnsOpCode(b);
  }
  



  private DnsOpCode(int byteValue)
  {
    this(byteValue, "UNKNOWN");
  }
  
  public DnsOpCode(int byteValue, String name) {
    this.byteValue = ((byte)byteValue);
    this.name = ((String)ObjectUtil.checkNotNull(name, "name"));
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
    if (this == obj) {
      return true;
    }
    
    if (!(obj instanceof DnsOpCode)) {
      return false;
    }
    
    return byteValue == byteValue;
  }
  
  public int compareTo(DnsOpCode o)
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
