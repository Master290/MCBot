package io.netty.handler.codec.socks;
















public enum SocksAuthStatus
{
  SUCCESS((byte)0), 
  FAILURE((byte)-1);
  
  private final byte b;
  
  private SocksAuthStatus(byte b) {
    this.b = b;
  }
  


  @Deprecated
  public static SocksAuthStatus fromByte(byte b)
  {
    return valueOf(b);
  }
  
  public static SocksAuthStatus valueOf(byte b) {
    for (SocksAuthStatus code : ) {
      if (b == b) {
        return code;
      }
    }
    return FAILURE;
  }
  
  public byte byteValue() {
    return b;
  }
}
