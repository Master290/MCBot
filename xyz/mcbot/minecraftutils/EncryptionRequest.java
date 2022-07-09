package xyz.mcbot.minecraftutils;

import io.netty.buffer.ByteBuf;

public class EncryptionRequest extends DefinedPacket {
  public String serverId;
  public byte[] publicKey;
  public byte[] verifyToken;
  
  public EncryptionRequest() {}
  
  public void read(ByteBuf buf) {
    serverId = DefinedPacket.readString(buf);
    publicKey = DefinedPacket.readArray(buf);
    verifyToken = DefinedPacket.readArray(buf);
  }
}
