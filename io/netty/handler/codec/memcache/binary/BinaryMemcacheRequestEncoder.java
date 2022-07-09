package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;



















public class BinaryMemcacheRequestEncoder
  extends AbstractBinaryMemcacheEncoder<BinaryMemcacheRequest>
{
  public BinaryMemcacheRequestEncoder() {}
  
  protected void encodeHeader(ByteBuf buf, BinaryMemcacheRequest msg)
  {
    buf.writeByte(msg.magic());
    buf.writeByte(msg.opcode());
    buf.writeShort(msg.keyLength());
    buf.writeByte(msg.extrasLength());
    buf.writeByte(msg.dataType());
    buf.writeShort(msg.reserved());
    buf.writeInt(msg.totalBodyLength());
    buf.writeInt(msg.opaque());
    buf.writeLong(msg.cas());
  }
}
