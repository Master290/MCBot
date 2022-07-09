package io.netty.handler.codec.http2;




public final class Http2Flags
{
  public static final short END_STREAM = 1;
  


  public static final short END_HEADERS = 4;
  


  public static final short ACK = 1;
  


  public static final short PADDED = 8;
  


  public static final short PRIORITY = 32;
  

  private short value;
  


  public Http2Flags() {}
  


  public Http2Flags(short value)
  {
    this.value = value;
  }
  


  public short value()
  {
    return value;
  }
  



  public boolean endOfStream()
  {
    return isFlagSet((short)1);
  }
  



  public boolean endOfHeaders()
  {
    return isFlagSet((short)4);
  }
  



  public boolean priorityPresent()
  {
    return isFlagSet((short)32);
  }
  



  public boolean ack()
  {
    return isFlagSet((short)1);
  }
  



  public boolean paddingPresent()
  {
    return isFlagSet((short)8);
  }
  



  public int getNumPriorityBytes()
  {
    return priorityPresent() ? 5 : 0;
  }
  



  public int getPaddingPresenceFieldLength()
  {
    return paddingPresent() ? 1 : 0;
  }
  


  public Http2Flags endOfStream(boolean endOfStream)
  {
    return setFlag(endOfStream, (short)1);
  }
  


  public Http2Flags endOfHeaders(boolean endOfHeaders)
  {
    return setFlag(endOfHeaders, (short)4);
  }
  


  public Http2Flags priorityPresent(boolean priorityPresent)
  {
    return setFlag(priorityPresent, (short)32);
  }
  


  public Http2Flags paddingPresent(boolean paddingPresent)
  {
    return setFlag(paddingPresent, (short)8);
  }
  


  public Http2Flags ack(boolean ack)
  {
    return setFlag(ack, (short)1);
  }
  





  public Http2Flags setFlag(boolean on, short mask)
  {
    if (on) {
      value = ((short)(value | mask));
    } else {
      value = ((short)(value & (mask ^ 0xFFFFFFFF)));
    }
    return this;
  }
  




  public boolean isFlagSet(short mask)
  {
    return (value & mask) != 0;
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + value;
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    
    return value == value;
  }
  
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append("value = ").append(value).append(" (");
    if (ack()) {
      builder.append("ACK,");
    }
    if (endOfHeaders()) {
      builder.append("END_OF_HEADERS,");
    }
    if (endOfStream()) {
      builder.append("END_OF_STREAM,");
    }
    if (priorityPresent()) {
      builder.append("PRIORITY_PRESENT,");
    }
    if (paddingPresent()) {
      builder.append("PADDING_PRESENT,");
    }
    builder.append(')');
    return builder.toString();
  }
}
