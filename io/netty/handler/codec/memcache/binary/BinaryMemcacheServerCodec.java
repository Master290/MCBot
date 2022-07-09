package io.netty.handler.codec.memcache.binary;

import io.netty.channel.CombinedChannelDuplexHandler;
























public class BinaryMemcacheServerCodec
  extends CombinedChannelDuplexHandler<BinaryMemcacheRequestDecoder, BinaryMemcacheResponseEncoder>
{
  public BinaryMemcacheServerCodec()
  {
    this(8192);
  }
  
  public BinaryMemcacheServerCodec(int decodeChunkSize) {
    super(new BinaryMemcacheRequestDecoder(decodeChunkSize), new BinaryMemcacheResponseEncoder());
  }
}
