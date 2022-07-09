package io.netty.handler.ssl;

import io.netty.util.internal.EmptyArrays;
import java.util.Arrays;





















final class OpenSslSessionId
{
  private final byte[] id;
  private final int hashCode;
  static final OpenSslSessionId NULL_ID = new OpenSslSessionId(EmptyArrays.EMPTY_BYTES);
  
  OpenSslSessionId(byte[] id)
  {
    this.id = id;
    
    hashCode = Arrays.hashCode(id);
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OpenSslSessionId)) {
      return false;
    }
    
    return Arrays.equals(id, id);
  }
  
  public String toString()
  {
    return 
      "OpenSslSessionId{id=" + Arrays.toString(id) + '}';
  }
  

  public int hashCode()
  {
    return hashCode;
  }
  
  byte[] cloneBytes() {
    return (byte[])id.clone();
  }
}
