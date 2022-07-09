package io.netty.handler.codec.compression;

import com.aayushatharva.brotli4j.encoder.Encoder.Parameters;

























public final class StandardCompressionOptions
{
  private StandardCompressionOptions() {}
  
  public static BrotliOptions brotli()
  {
    return BrotliOptions.DEFAULT;
  }
  





  public static BrotliOptions brotli(Encoder.Parameters parameters)
  {
    return new BrotliOptions(parameters);
  }
  




  public static ZstdOptions zstd()
  {
    return ZstdOptions.DEFAULT;
  }
  









  public static ZstdOptions zstd(int compressionLevel, int blockSize, int maxEncodeSize)
  {
    return new ZstdOptions(compressionLevel, blockSize, maxEncodeSize);
  }
  



  public static GzipOptions gzip()
  {
    return GzipOptions.DEFAULT;
  }
  
















  public static GzipOptions gzip(int compressionLevel, int windowBits, int memLevel)
  {
    return new GzipOptions(compressionLevel, windowBits, memLevel);
  }
  



  public static DeflateOptions deflate()
  {
    return DeflateOptions.DEFAULT;
  }
  
















  public static DeflateOptions deflate(int compressionLevel, int windowBits, int memLevel)
  {
    return new DeflateOptions(compressionLevel, windowBits, memLevel);
  }
}
