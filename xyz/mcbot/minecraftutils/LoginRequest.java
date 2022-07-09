package xyz.mcbot.minecraftutils;

import io.netty.buffer.ByteBuf;

public class LoginRequest extends DefinedPacket
{
  public String data;
  
  public LoginRequest(String heil)
  {
    data = heil;
  }
  
  public void write(ByteBuf buf)
  {
    DefinedPacket.writeString(data, buf);
  }
  
  public void writeC(ByteBuf buf) {
    DefinedPacket.writeStringC(data, buf);
  }
  
  public byte[] getWrappedPacket() {
    ByteBuf allocated = io.netty.buffer.Unpooled.buffer();
    allocated.writeByte(0);
    write(allocated);
    ByteBuf wrapped = io.netty.buffer.Unpooled.buffer();
    DefinedPacket.writeVarInt(allocated.readableBytes(), wrapped);
    wrapped.writeBytes(allocated);
    byte[] bytes = new byte[wrapped.readableBytes()];
    wrapped.getBytes(0, bytes);
    wrapped.release();
    return bytes;
  }
  
  public byte[] getWrappedPacketC() {
    ByteBuf allocated = io.netty.buffer.Unpooled.buffer();
    allocated.writeByte(0);
    writeC(allocated);
    ByteBuf wrapped = io.netty.buffer.Unpooled.buffer();
    DefinedPacket.writeVarInt(allocated.readableBytes(), wrapped);
    wrapped.writeBytes(allocated);
    byte[] bytes = new byte[wrapped.readableBytes()];
    wrapped.getBytes(0, bytes);
    wrapped.release();
    return bytes;
  }
}
