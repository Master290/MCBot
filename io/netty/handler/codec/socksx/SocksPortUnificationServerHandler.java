package io.netty.handler.codec.socksx;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.List;























public class SocksPortUnificationServerHandler
  extends ByteToMessageDecoder
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(SocksPortUnificationServerHandler.class);
  

  private final Socks5ServerEncoder socks5encoder;
  

  public SocksPortUnificationServerHandler()
  {
    this(Socks5ServerEncoder.DEFAULT);
  }
  



  public SocksPortUnificationServerHandler(Socks5ServerEncoder socks5encoder)
  {
    this.socks5encoder = ((Socks5ServerEncoder)ObjectUtil.checkNotNull(socks5encoder, "socks5encoder"));
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    int readerIndex = in.readerIndex();
    if (in.writerIndex() == readerIndex) {
      return;
    }
    
    ChannelPipeline p = ctx.pipeline();
    byte versionVal = in.getByte(readerIndex);
    SocksVersion version = SocksVersion.valueOf(versionVal);
    
    switch (1.$SwitchMap$io$netty$handler$codec$socksx$SocksVersion[version.ordinal()]) {
    case 1: 
      logKnownVersion(ctx, version);
      p.addAfter(ctx.name(), null, Socks4ServerEncoder.INSTANCE);
      p.addAfter(ctx.name(), null, new Socks4ServerDecoder());
      break;
    case 2: 
      logKnownVersion(ctx, version);
      p.addAfter(ctx.name(), null, socks5encoder);
      p.addAfter(ctx.name(), null, new Socks5InitialRequestDecoder());
      break;
    default: 
      logUnknownVersion(ctx, versionVal);
      in.skipBytes(in.readableBytes());
      ctx.close();
      return;
    }
    
    p.remove(this);
  }
  
  private static void logKnownVersion(ChannelHandlerContext ctx, SocksVersion version) {
    logger.debug("{} Protocol version: {}({})", ctx.channel(), version);
  }
  
  private static void logUnknownVersion(ChannelHandlerContext ctx, byte versionVal) {
    if (logger.isDebugEnabled()) {
      logger.debug("{} Unknown protocol version: {}", ctx.channel(), Integer.valueOf(versionVal & 0xFF));
    }
  }
}
