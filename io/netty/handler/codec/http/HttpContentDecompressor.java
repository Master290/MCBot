package io.netty.handler.codec.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.BrotliDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.util.AsciiString;























public class HttpContentDecompressor
  extends HttpContentDecoder
{
  private final boolean strict;
  
  public HttpContentDecompressor()
  {
    this(false);
  }
  





  public HttpContentDecompressor(boolean strict)
  {
    this.strict = strict;
  }
  
  protected EmbeddedChannel newContentDecoder(String contentEncoding) throws Exception
  {
    if ((HttpHeaderValues.GZIP.contentEqualsIgnoreCase(contentEncoding)) || 
      (HttpHeaderValues.X_GZIP.contentEqualsIgnoreCase(contentEncoding))) {
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
        .channel().config(), new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP) });
    }
    if ((HttpHeaderValues.DEFLATE.contentEqualsIgnoreCase(contentEncoding)) || 
      (HttpHeaderValues.X_DEFLATE.contentEqualsIgnoreCase(contentEncoding))) {
      ZlibWrapper wrapper = strict ? ZlibWrapper.ZLIB : ZlibWrapper.ZLIB_OR_NONE;
      
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
        .channel().config(), new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(wrapper) });
    }
    if ((Brotli.isAvailable()) && (HttpHeaderValues.BR.contentEqualsIgnoreCase(contentEncoding))) {
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
        .channel().config(), new ChannelHandler[] { new BrotliDecoder() });
    }
    

    return null;
  }
}
