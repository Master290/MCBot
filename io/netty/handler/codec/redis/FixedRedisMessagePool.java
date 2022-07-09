package io.netty.handler.codec.redis;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import java.util.HashMap;
import java.util.Map;
















public final class FixedRedisMessagePool
  implements RedisMessagePool
{
  private static final long MIN_CACHED_INTEGER_NUMBER = -1L;
  private static final long MAX_CACHED_INTEGER_NUMBER = 128L;
  private static final int SIZE_CACHED_INTEGER_NUMBER = 129;
  
  public static enum RedisReplyKey
  {
    OK,  PONG,  QUEUED;
    
    private RedisReplyKey() {} }
  
  public static enum RedisErrorKey { ERR("ERR"), 
    ERR_IDX("ERR index out of range"), 
    ERR_NOKEY("ERR no such key"), 
    ERR_SAMEOBJ("ERR source and destination objects are the same"), 
    ERR_SYNTAX("ERR syntax error"), 
    BUSY("BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE."), 
    BUSYKEY("BUSYKEY Target key name already exists."), 
    EXECABORT("EXECABORT Transaction discarded because of previous errors."), 
    LOADING("LOADING Redis is loading the dataset in memory"), 
    MASTERDOWN("MASTERDOWN Link with MASTER is down and slave-serve-stale-data is set to 'no'."), 
    MISCONF("MISCONF Redis is configured to save RDB snapshots, but is currently not able to persist on disk. Commands that may modify the data set are disabled. Please check Redis logs for details about the error."), 
    

    NOREPLICAS("NOREPLICAS Not enough good slaves to write."), 
    NOSCRIPT("NOSCRIPT No matching script. Please use EVAL."), 
    OOM("OOM command not allowed when used memory > 'maxmemory'."), 
    READONLY("READONLY You can't write against a read only slave."), 
    WRONGTYPE("WRONGTYPE Operation against a key holding the wrong kind of value"), 
    NOT_AUTH("NOAUTH Authentication required.");
    
    private String msg;
    
    private RedisErrorKey(String msg) {
      this.msg = msg;
    }
    
    public String toString()
    {
      return msg;
    }
  }
  









  public static final FixedRedisMessagePool INSTANCE = new FixedRedisMessagePool();
  
  private final Map<ByteBuf, SimpleStringRedisMessage> byteBufToSimpleStrings;
  
  private final Map<String, SimpleStringRedisMessage> stringToSimpleStrings;
  
  private final Map<RedisReplyKey, SimpleStringRedisMessage> keyToSimpleStrings;
  
  private final Map<ByteBuf, ErrorRedisMessage> byteBufToErrors;
  private final Map<String, ErrorRedisMessage> stringToErrors;
  private final Map<RedisErrorKey, ErrorRedisMessage> keyToErrors;
  private final Map<ByteBuf, IntegerRedisMessage> byteBufToIntegers;
  private final LongObjectMap<IntegerRedisMessage> longToIntegers;
  private final LongObjectMap<byte[]> longToByteBufs;
  
  private FixedRedisMessagePool()
  {
    keyToSimpleStrings = new HashMap(RedisReplyKey.values().length, 1.0F);
    stringToSimpleStrings = new HashMap(RedisReplyKey.values().length, 1.0F);
    byteBufToSimpleStrings = new HashMap(RedisReplyKey.values().length, 1.0F);
    for (RedisReplyKey value : RedisReplyKey.values())
    {
      ByteBuf key = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(value.name().getBytes(CharsetUtil.UTF_8))).asReadOnly();
      
      SimpleStringRedisMessage message = new SimpleStringRedisMessage(new String(Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(value.name().getBytes(CharsetUtil.UTF_8))).array()));
      stringToSimpleStrings.put(value.name(), message);
      keyToSimpleStrings.put(value, message);
      byteBufToSimpleStrings.put(key, message);
    }
    
    keyToErrors = new HashMap(RedisErrorKey.values().length, 1.0F);
    stringToErrors = new HashMap(RedisErrorKey.values().length, 1.0F);
    byteBufToErrors = new HashMap(RedisErrorKey.values().length, 1.0F);
    for (RedisErrorKey value : RedisErrorKey.values())
    {
      ByteBuf key = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(value.toString().getBytes(CharsetUtil.UTF_8))).asReadOnly();
      
      ErrorRedisMessage message = new ErrorRedisMessage(new String(Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(value.toString().getBytes(CharsetUtil.UTF_8))).array()));
      stringToErrors.put(value.toString(), message);
      keyToErrors.put(value, message);
      byteBufToErrors.put(key, message);
    }
    
    byteBufToIntegers = new HashMap(129, 1.0F);
    longToIntegers = new LongObjectHashMap(129, 1.0F);
    longToByteBufs = new LongObjectHashMap(129, 1.0F);
    for (long value = -1L; value < 128L; value += 1L) {
      byte[] keyBytes = RedisCodecUtil.longToAsciiBytes(value);
      ByteBuf keyByteBuf = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(keyBytes)).asReadOnly();
      IntegerRedisMessage cached = new IntegerRedisMessage(value);
      byteBufToIntegers.put(keyByteBuf, cached);
      longToIntegers.put(value, cached);
      longToByteBufs.put(value, keyBytes);
    }
  }
  
  public SimpleStringRedisMessage getSimpleString(String content)
  {
    return (SimpleStringRedisMessage)stringToSimpleStrings.get(content);
  }
  



  public SimpleStringRedisMessage getSimpleString(RedisReplyKey key)
  {
    return (SimpleStringRedisMessage)keyToSimpleStrings.get(key);
  }
  
  public SimpleStringRedisMessage getSimpleString(ByteBuf content)
  {
    return (SimpleStringRedisMessage)byteBufToSimpleStrings.get(content);
  }
  
  public ErrorRedisMessage getError(String content)
  {
    return (ErrorRedisMessage)stringToErrors.get(content);
  }
  



  public ErrorRedisMessage getError(RedisErrorKey key)
  {
    return (ErrorRedisMessage)keyToErrors.get(key);
  }
  
  public ErrorRedisMessage getError(ByteBuf content)
  {
    return (ErrorRedisMessage)byteBufToErrors.get(content);
  }
  
  public IntegerRedisMessage getInteger(long value)
  {
    return (IntegerRedisMessage)longToIntegers.get(value);
  }
  
  public IntegerRedisMessage getInteger(ByteBuf content)
  {
    return (IntegerRedisMessage)byteBufToIntegers.get(content);
  }
  
  public byte[] getByteBufOfInteger(long value)
  {
    return (byte[])longToByteBufs.get(value);
  }
}
