package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.internal.ObjectUtil;
import java.net.IDN;




















public final class SocksCmdRequest
  extends SocksRequest
{
  private final SocksCmdType cmdType;
  private final SocksAddressType addressType;
  private final String host;
  private final int port;
  
  public SocksCmdRequest(SocksCmdType cmdType, SocksAddressType addressType, String host, int port)
  {
    super(SocksRequestType.CMD);
    ObjectUtil.checkNotNull(cmdType, "cmdType");
    ObjectUtil.checkNotNull(addressType, "addressType");
    ObjectUtil.checkNotNull(host, "host");
    
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksAddressType[addressType.ordinal()]) {
    case 1: 
      if (!NetUtil.isValidIpV4Address(host)) {
        throw new IllegalArgumentException(host + " is not a valid IPv4 address");
      }
      break;
    case 2: 
      String asciiHost = IDN.toASCII(host);
      if (asciiHost.length() > 255) {
        throw new IllegalArgumentException(host + " IDN: " + asciiHost + " exceeds 255 char limit");
      }
      host = asciiHost;
      break;
    case 3: 
      if (!NetUtil.isValidIpV6Address(host)) {
        throw new IllegalArgumentException(host + " is not a valid IPv6 address");
      }
      
      break;
    }
    
    if ((port <= 0) || (port >= 65536)) {
      throw new IllegalArgumentException(port + " is not in bounds 0 < x < 65536");
    }
    this.cmdType = cmdType;
    this.addressType = addressType;
    this.host = host;
    this.port = port;
  }
  




  public SocksCmdType cmdType()
  {
    return cmdType;
  }
  




  public SocksAddressType addressType()
  {
    return addressType;
  }
  




  public String host()
  {
    return addressType == SocksAddressType.DOMAIN ? IDN.toUnicode(host) : host;
  }
  




  public int port()
  {
    return port;
  }
  
  public void encodeAsByteBuf(ByteBuf byteBuf)
  {
    byteBuf.writeByte(protocolVersion().byteValue());
    byteBuf.writeByte(cmdType.byteValue());
    byteBuf.writeByte(0);
    byteBuf.writeByte(addressType.byteValue());
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksAddressType[addressType.ordinal()]) {
    case 1: 
      byteBuf.writeBytes(NetUtil.createByteArrayFromIpAddressString(host));
      byteBuf.writeShort(port);
      break;
    

    case 2: 
      byteBuf.writeByte(host.length());
      byteBuf.writeCharSequence(host, CharsetUtil.US_ASCII);
      byteBuf.writeShort(port);
      break;
    

    case 3: 
      byteBuf.writeBytes(NetUtil.createByteArrayFromIpAddressString(host));
      byteBuf.writeShort(port);
    }
  }
}
