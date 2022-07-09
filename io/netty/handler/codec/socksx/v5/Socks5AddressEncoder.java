package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;























public abstract interface Socks5AddressEncoder
{
  public static final Socks5AddressEncoder DEFAULT = new Socks5AddressEncoder()
  {
    public void encodeAddress(Socks5AddressType addrType, String addrValue, ByteBuf out) throws Exception {
      byte typeVal = addrType.byteValue();
      if (typeVal == Socks5AddressType.IPv4.byteValue()) {
        if (addrValue != null) {
          out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
        } else {
          out.writeInt(0);
        }
      } else if (typeVal == Socks5AddressType.DOMAIN.byteValue()) {
        if (addrValue != null) {
          out.writeByte(addrValue.length());
          out.writeCharSequence(addrValue, CharsetUtil.US_ASCII);
        } else {
          out.writeByte(0);
        }
      } else if (typeVal == Socks5AddressType.IPv6.byteValue()) {
        if (addrValue != null) {
          out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
        } else {
          out.writeLong(0L);
          out.writeLong(0L);
        }
      } else {
        throw new EncoderException("unsupported addrType: " + (addrType.byteValue() & 0xFF));
      }
    }
  };
  
  public abstract void encodeAddress(Socks5AddressType paramSocks5AddressType, String paramString, ByteBuf paramByteBuf)
    throws Exception;
}
