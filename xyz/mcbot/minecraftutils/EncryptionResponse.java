package xyz.mcbot.minecraftutils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class EncryptionResponse extends DefinedPacket
{
  private byte[] sharedSecret;
  private byte[] verifyToken;
  
  public void write(ByteBuf buf)
  {
    DefinedPacket.writeArray(sharedSecret, buf);
    DefinedPacket.writeArray(verifyToken, buf);
  }
  
  public EncryptionResponse(byte[] sharedSecret, byte[] verifyToken) {
    this.sharedSecret = sharedSecret;
    this.verifyToken = verifyToken;
  }
  
  public byte[] getWrappedPacket() {
    ByteBuf allocated = Unpooled.buffer();
    allocated.writeByte(1);
    write(allocated);
    ByteBuf wrapped = Unpooled.buffer();
    DefinedPacket.writeVarInt(allocated.readableBytes(), wrapped);
    wrapped.writeBytes(allocated);
    byte[] bytes = new byte[wrapped.readableBytes()];
    wrapped.getBytes(0, bytes);
    wrapped.release();
    return bytes;
  }
}
