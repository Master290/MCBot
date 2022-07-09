package io.netty.handler.codec.redis;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageAggregator;







































public final class RedisBulkStringAggregator
  extends MessageAggregator<RedisMessage, BulkStringHeaderRedisMessage, BulkStringRedisContent, FullBulkStringRedisMessage>
{
  public RedisBulkStringAggregator()
  {
    super(536870912);
  }
  
  protected boolean isStartMessage(RedisMessage msg) throws Exception
  {
    return ((msg instanceof BulkStringHeaderRedisMessage)) && (!isAggregated(msg));
  }
  
  protected boolean isContentMessage(RedisMessage msg) throws Exception
  {
    return msg instanceof BulkStringRedisContent;
  }
  
  protected boolean isLastContentMessage(BulkStringRedisContent msg) throws Exception
  {
    return msg instanceof LastBulkStringRedisContent;
  }
  
  protected boolean isAggregated(RedisMessage msg) throws Exception
  {
    return msg instanceof FullBulkStringRedisMessage;
  }
  
  protected boolean isContentLengthInvalid(BulkStringHeaderRedisMessage start, int maxContentLength)
    throws Exception
  {
    return start.bulkStringLength() > maxContentLength;
  }
  
  protected Object newContinueResponse(BulkStringHeaderRedisMessage start, int maxContentLength, ChannelPipeline pipeline)
    throws Exception
  {
    return null;
  }
  
  protected boolean closeAfterContinueResponse(Object msg) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected boolean ignoreContentAfterContinueResponse(Object msg) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected FullBulkStringRedisMessage beginAggregation(BulkStringHeaderRedisMessage start, ByteBuf content)
    throws Exception
  {
    return new FullBulkStringRedisMessage(content);
  }
}
