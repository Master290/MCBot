package io.netty.handler.codec.socks;
















public enum SocksSubnegotiationVersion
{
  AUTH_PASSWORD((byte)1), 
  UNKNOWN((byte)-1);
  
  private final byte b;
  
  private SocksSubnegotiationVersion(byte b) {
    this.b = b;
  }
  


  @Deprecated
  public static SocksSubnegotiationVersion fromByte(byte b)
  {
    return valueOf(b);
  }
  
  public static SocksSubnegotiationVersion valueOf(byte b) {
    for (SocksSubnegotiationVersion code : ) {
      if (b == b) {
        return code;
      }
    }
    return UNKNOWN;
  }
  
  public byte byteValue() {
    return b;
  }
}
