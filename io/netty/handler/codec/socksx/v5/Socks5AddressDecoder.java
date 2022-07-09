package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;























public abstract interface Socks5AddressDecoder
{
  public static final Socks5AddressDecoder DEFAULT = new Socks5AddressDecoder()
  {
    private static final int IPv6_LEN = 16;
    
    public String decodeAddress(Socks5AddressType addrType, ByteBuf in) throws Exception
    {
      if (addrType == Socks5AddressType.IPv4) {
        return NetUtil.intToIpAddress(in.readInt());
      }
      if (addrType == Socks5AddressType.DOMAIN) {
        int length = in.readUnsignedByte();
        String domain = in.toString(in.readerIndex(), length, CharsetUtil.US_ASCII);
        in.skipBytes(length);
        return domain;
      }
      if (addrType == Socks5AddressType.IPv6) {
        if (in.hasArray()) {
          int readerIdx = in.readerIndex();
          in.readerIndex(readerIdx + 16);
          return NetUtil.bytesToIpAddress(in.array(), in.arrayOffset() + readerIdx, 16);
        }
        byte[] tmp = new byte[16];
        in.readBytes(tmp);
        return NetUtil.bytesToIpAddress(tmp);
      }
      
      throw new DecoderException("unsupported address type: " + (addrType.byteValue() & 0xFF));
    }
  };
  
  public abstract String decodeAddress(Socks5AddressType paramSocks5AddressType, ByteBuf paramByteBuf)
    throws Exception;
}
