package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;



















public class BinaryMemcacheResponseEncoder
  extends AbstractBinaryMemcacheEncoder<BinaryMemcacheResponse>
{
  public BinaryMemcacheResponseEncoder() {}
  
  protected void encodeHeader(ByteBuf buf, BinaryMemcacheResponse msg)
  {
    buf.writeByte(msg.magic());
    buf.writeByte(msg.opcode());
    buf.writeShort(msg.keyLength());
    buf.writeByte(msg.extrasLength());
    buf.writeByte(msg.dataType());
    buf.writeShort(msg.status());
    buf.writeInt(msg.totalBodyLength());
    buf.writeInt(msg.opaque());
    buf.writeLong(msg.cas());
  }
}
