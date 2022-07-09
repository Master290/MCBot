package io.netty.handler.codec.redis;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;






















public final class RedisArrayAggregator
  extends MessageToMessageDecoder<RedisMessage>
{
  private final Deque<AggregateState> depths = new ArrayDeque(4);
  
  public RedisArrayAggregator() {}
  
  protected void decode(ChannelHandlerContext ctx, RedisMessage msg, List<Object> out) throws Exception { if ((msg instanceof ArrayHeaderRedisMessage)) {
      msg = decodeRedisArrayHeader((ArrayHeaderRedisMessage)msg);
      if (msg != null) {}
    }
    else
    {
      ReferenceCountUtil.retain(msg);
    }
    
    while (!depths.isEmpty()) {
      AggregateState current = (AggregateState)depths.peek();
      children.add(msg);
      

      if (children.size() == length) {
        msg = new ArrayRedisMessage(children);
        depths.pop();
      }
      else {
        return;
      }
    }
    
    out.add(msg);
  }
  
  private RedisMessage decodeRedisArrayHeader(ArrayHeaderRedisMessage header) {
    if (header.isNull())
      return ArrayRedisMessage.NULL_INSTANCE;
    if (header.length() == 0L)
      return ArrayRedisMessage.EMPTY_INSTANCE;
    if (header.length() > 0L)
    {
      if (header.length() > 2147483647L) {
        throw new CodecException("this codec doesn't support longer length than 2147483647");
      }
      

      depths.push(new AggregateState((int)header.length()));
      return null;
    }
    throw new CodecException("bad length: " + header.length());
  }
  
  private static final class AggregateState {
    private final int length;
    private final List<RedisMessage> children;
    
    AggregateState(int length) {
      this.length = length;
      children = new ArrayList(length);
    }
  }
}
