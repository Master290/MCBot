package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.BrotliEncoder;
import io.netty.handler.codec.compression.BrotliOptions;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.compression.DeflateOptions;
import io.netty.handler.codec.compression.GzipOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.compression.ZstdEncoder;
import io.netty.handler.codec.compression.ZstdOptions;
import io.netty.util.internal.ObjectUtil;
import java.util.HashMap;
import java.util.Map;

























public class HttpContentCompressor
  extends HttpContentEncoder
{
  private final boolean supportsCompressionOptions;
  private final BrotliOptions brotliOptions;
  private final GzipOptions gzipOptions;
  private final DeflateOptions deflateOptions;
  private final ZstdOptions zstdOptions;
  private final int compressionLevel;
  private final int windowBits;
  private final int memLevel;
  private final int contentSizeThreshold;
  private ChannelHandlerContext ctx;
  private final Map<String, CompressionEncoderFactory> factories;
  
  public HttpContentCompressor()
  {
    this(6);
  }
  








  @Deprecated
  public HttpContentCompressor(int compressionLevel)
  {
    this(compressionLevel, 15, 8, 0);
  }
  


















  @Deprecated
  public HttpContentCompressor(int compressionLevel, int windowBits, int memLevel)
  {
    this(compressionLevel, windowBits, memLevel, 0);
  }
  






















  @Deprecated
  public HttpContentCompressor(int compressionLevel, int windowBits, int memLevel, int contentSizeThreshold)
  {
    this.compressionLevel = ObjectUtil.checkInRange(compressionLevel, 0, 9, "compressionLevel");
    this.windowBits = ObjectUtil.checkInRange(windowBits, 9, 15, "windowBits");
    this.memLevel = ObjectUtil.checkInRange(memLevel, 1, 9, "memLevel");
    this.contentSizeThreshold = ObjectUtil.checkPositiveOrZero(contentSizeThreshold, "contentSizeThreshold");
    brotliOptions = null;
    gzipOptions = null;
    deflateOptions = null;
    zstdOptions = null;
    factories = null;
    supportsCompressionOptions = false;
  }
  






  public HttpContentCompressor(CompressionOptions... compressionOptions)
  {
    this(0, compressionOptions);
  }
  










  public HttpContentCompressor(int contentSizeThreshold, CompressionOptions... compressionOptions)
  {
    this.contentSizeThreshold = ObjectUtil.checkPositiveOrZero(contentSizeThreshold, "contentSizeThreshold");
    BrotliOptions brotliOptions = null;
    GzipOptions gzipOptions = null;
    DeflateOptions deflateOptions = null;
    ZstdOptions zstdOptions = null;
    if ((compressionOptions == null) || (compressionOptions.length == 0)) {
      brotliOptions = StandardCompressionOptions.brotli();
      gzipOptions = StandardCompressionOptions.gzip();
      deflateOptions = StandardCompressionOptions.deflate();
      zstdOptions = StandardCompressionOptions.zstd();
    } else {
      ObjectUtil.deepCheckNotNull("compressionOptions", compressionOptions);
      for (CompressionOptions compressionOption : compressionOptions) {
        if ((compressionOption instanceof BrotliOptions)) {
          brotliOptions = (BrotliOptions)compressionOption;
        } else if ((compressionOption instanceof GzipOptions)) {
          gzipOptions = (GzipOptions)compressionOption;
        } else if ((compressionOption instanceof DeflateOptions)) {
          deflateOptions = (DeflateOptions)compressionOption;
        } else if ((compressionOption instanceof ZstdOptions)) {
          zstdOptions = (ZstdOptions)compressionOption;
        } else {
          throw new IllegalArgumentException("Unsupported " + CompressionOptions.class.getSimpleName() + ": " + compressionOption);
        }
      }
      
      if (brotliOptions == null) {
        brotliOptions = StandardCompressionOptions.brotli();
      }
      if (gzipOptions == null) {
        gzipOptions = StandardCompressionOptions.gzip();
      }
      if (deflateOptions == null) {
        deflateOptions = StandardCompressionOptions.deflate();
      }
      if (zstdOptions == null) {
        zstdOptions = StandardCompressionOptions.zstd();
      }
    }
    this.brotliOptions = brotliOptions;
    this.gzipOptions = gzipOptions;
    this.deflateOptions = deflateOptions;
    this.zstdOptions = zstdOptions;
    factories = new HashMap() {};
    compressionLevel = -1;
    windowBits = -1;
    memLevel = -1;
    supportsCompressionOptions = true;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
  }
  
  protected HttpContentEncoder.Result beginEncode(HttpResponse httpResponse, String acceptEncoding) throws Exception
  {
    if ((contentSizeThreshold > 0) && 
      ((httpResponse instanceof HttpContent)) && 
      (((HttpContent)httpResponse).content().readableBytes() < contentSizeThreshold)) {
      return null;
    }
    

    String contentEncoding = httpResponse.headers().get(HttpHeaderNames.CONTENT_ENCODING);
    if (contentEncoding != null)
    {

      return null;
    }
    
    if (supportsCompressionOptions) {
      String targetContentEncoding = determineEncoding(acceptEncoding);
      if (targetContentEncoding == null) {
        return null;
      }
      
      CompressionEncoderFactory encoderFactory = (CompressionEncoderFactory)factories.get(targetContentEncoding);
      
      if (encoderFactory == null) {
        throw new Error();
      }
      
      return new HttpContentEncoder.Result(targetContentEncoding, new EmbeddedChannel(ctx
        .channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
        .channel().config(), new ChannelHandler[] { encoderFactory.createEncoder() }));
    }
    ZlibWrapper wrapper = determineWrapper(acceptEncoding);
    if (wrapper == null) {
      return null;
    }
    String targetContentEncoding;
    String targetContentEncoding;
    switch (2.$SwitchMap$io$netty$handler$codec$compression$ZlibWrapper[wrapper.ordinal()]) {
    case 1: 
      targetContentEncoding = "gzip";
      break;
    case 2: 
      targetContentEncoding = "deflate";
      break;
    default: 
      throw new Error();
    }
    String targetContentEncoding;
    return new HttpContentEncoder.Result(targetContentEncoding, new EmbeddedChannel(ctx
    
      .channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
      .channel().config(), new ChannelHandler[] { ZlibCodecFactory.newZlibEncoder(wrapper, compressionLevel, windowBits, memLevel) }));
  }
  


  protected String determineEncoding(String acceptEncoding)
  {
    float starQ = -1.0F;
    float brQ = -1.0F;
    float zstdQ = -1.0F;
    float gzipQ = -1.0F;
    float deflateQ = -1.0F;
    for (String encoding : acceptEncoding.split(",")) {
      float q = 1.0F;
      int equalsPos = encoding.indexOf('=');
      if (equalsPos != -1) {
        try {
          q = Float.parseFloat(encoding.substring(equalsPos + 1));
        }
        catch (NumberFormatException e) {
          q = 0.0F;
        }
      }
      if (encoding.contains("*")) {
        starQ = q;
      } else if ((encoding.contains("br")) && (q > brQ)) {
        brQ = q;
      } else if ((encoding.contains("zstd")) && (q > zstdQ)) {
        zstdQ = q;
      } else if ((encoding.contains("gzip")) && (q > gzipQ)) {
        gzipQ = q;
      } else if ((encoding.contains("deflate")) && (q > deflateQ)) {
        deflateQ = q;
      }
    }
    if ((brQ > 0.0F) || (zstdQ > 0.0F) || (gzipQ > 0.0F) || (deflateQ > 0.0F)) {
      if ((brQ != -1.0F) && (brQ >= zstdQ))
        return Brotli.isAvailable() ? "br" : null;
      if ((zstdQ != -1.0F) && (zstdQ >= gzipQ))
        return "zstd";
      if ((gzipQ != -1.0F) && (gzipQ >= deflateQ))
        return "gzip";
      if (deflateQ != -1.0F) {
        return "deflate";
      }
    }
    if (starQ > 0.0F) {
      if (brQ == -1.0F) {
        return Brotli.isAvailable() ? "br" : null;
      }
      if (zstdQ == -1.0F) {
        return "zstd";
      }
      if (gzipQ == -1.0F) {
        return "gzip";
      }
      if (deflateQ == -1.0F) {
        return "deflate";
      }
    }
    return null;
  }
  
  @Deprecated
  protected ZlibWrapper determineWrapper(String acceptEncoding)
  {
    float starQ = -1.0F;
    float gzipQ = -1.0F;
    float deflateQ = -1.0F;
    for (String encoding : acceptEncoding.split(",")) {
      float q = 1.0F;
      int equalsPos = encoding.indexOf('=');
      if (equalsPos != -1) {
        try {
          q = Float.parseFloat(encoding.substring(equalsPos + 1));
        }
        catch (NumberFormatException e) {
          q = 0.0F;
        }
      }
      if (encoding.contains("*")) {
        starQ = q;
      } else if ((encoding.contains("gzip")) && (q > gzipQ)) {
        gzipQ = q;
      } else if ((encoding.contains("deflate")) && (q > deflateQ)) {
        deflateQ = q;
      }
    }
    if ((gzipQ > 0.0F) || (deflateQ > 0.0F)) {
      if (gzipQ >= deflateQ) {
        return ZlibWrapper.GZIP;
      }
      return ZlibWrapper.ZLIB;
    }
    
    if (starQ > 0.0F) {
      if (gzipQ == -1.0F) {
        return ZlibWrapper.GZIP;
      }
      if (deflateQ == -1.0F) {
        return ZlibWrapper.ZLIB;
      }
    }
    return null;
  }
  

  private final class GzipEncoderFactory
    implements CompressionEncoderFactory
  {
    private GzipEncoderFactory() {}
    
    public MessageToByteEncoder<ByteBuf> createEncoder()
    {
      return ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP, 
        gzipOptions.compressionLevel(), 
        gzipOptions.windowBits(), gzipOptions.memLevel());
    }
  }
  

  private final class DeflateEncoderFactory
    implements CompressionEncoderFactory
  {
    private DeflateEncoderFactory() {}
    
    public MessageToByteEncoder<ByteBuf> createEncoder()
    {
      return ZlibCodecFactory.newZlibEncoder(ZlibWrapper.ZLIB, 
        deflateOptions.compressionLevel(), 
        deflateOptions.windowBits(), deflateOptions.memLevel());
    }
  }
  

  private final class BrEncoderFactory
    implements CompressionEncoderFactory
  {
    private BrEncoderFactory() {}
    
    public MessageToByteEncoder<ByteBuf> createEncoder()
    {
      return new BrotliEncoder(brotliOptions.parameters());
    }
  }
  

  private final class ZstdEncoderFactory
    implements CompressionEncoderFactory
  {
    private ZstdEncoderFactory() {}
    
    public MessageToByteEncoder<ByteBuf> createEncoder()
    {
      return new ZstdEncoder(zstdOptions.compressionLevel(), 
        zstdOptions.blockSize(), zstdOptions.maxEncodeSize());
    }
  }
}
