package io.netty.handler.codec.redis;













public class BulkStringHeaderRedisMessage
  implements RedisMessage
{
  private final int bulkStringLength;
  












  public BulkStringHeaderRedisMessage(int bulkStringLength)
  {
    if (bulkStringLength <= 0) {
      throw new RedisCodecException("bulkStringLength: " + bulkStringLength + " (expected: > 0)");
    }
    this.bulkStringLength = bulkStringLength;
  }
  


  public final int bulkStringLength()
  {
    return bulkStringLength;
  }
  




  public boolean isNull()
  {
    return bulkStringLength == -1;
  }
}
