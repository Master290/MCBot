package io.netty.handler.codec.socksx;






















public enum SocksVersion
{
  SOCKS4a((byte)4), 
  


  SOCKS5((byte)5), 
  


  UNKNOWN((byte)-1);
  


  private final byte b;
  

  public static SocksVersion valueOf(byte b)
  {
    if (b == SOCKS4a.byteValue()) {
      return SOCKS4a;
    }
    if (b == SOCKS5.byteValue()) {
      return SOCKS5;
    }
    return UNKNOWN;
  }
  

  private SocksVersion(byte b)
  {
    this.b = b;
  }
  


  public byte byteValue()
  {
    return b;
  }
}
