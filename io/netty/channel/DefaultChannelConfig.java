package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.ObjectUtil;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;



































public class DefaultChannelConfig
  implements ChannelConfig
{
  private static final MessageSizeEstimator DEFAULT_MSG_SIZE_ESTIMATOR = DefaultMessageSizeEstimator.DEFAULT;
  

  private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
  
  private static final AtomicIntegerFieldUpdater<DefaultChannelConfig> AUTOREAD_UPDATER = AtomicIntegerFieldUpdater.newUpdater(DefaultChannelConfig.class, "autoRead");
  
  private static final AtomicReferenceFieldUpdater<DefaultChannelConfig, WriteBufferWaterMark> WATERMARK_UPDATER = AtomicReferenceFieldUpdater.newUpdater(DefaultChannelConfig.class, WriteBufferWaterMark.class, "writeBufferWaterMark");
  

  protected final Channel channel;
  
  private volatile ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
  private volatile RecvByteBufAllocator rcvBufAllocator;
  private volatile MessageSizeEstimator msgSizeEstimator = DEFAULT_MSG_SIZE_ESTIMATOR;
  
  private volatile int connectTimeoutMillis = 30000;
  private volatile int writeSpinCount = 16;
  private volatile int maxMessagesPerWrite = Integer.MAX_VALUE;
  
  private volatile int autoRead = 1;
  
  private volatile boolean autoClose = true;
  private volatile WriteBufferWaterMark writeBufferWaterMark = WriteBufferWaterMark.DEFAULT;
  private volatile boolean pinEventExecutor = true;
  
  public DefaultChannelConfig(Channel channel) {
    this(channel, new AdaptiveRecvByteBufAllocator());
  }
  
  protected DefaultChannelConfig(Channel channel, RecvByteBufAllocator allocator) {
    setRecvByteBufAllocator(allocator, channel.metadata());
    this.channel = channel;
  }
  

  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(null, new ChannelOption[] { ChannelOption.CONNECT_TIMEOUT_MILLIS, ChannelOption.MAX_MESSAGES_PER_READ, ChannelOption.WRITE_SPIN_COUNT, ChannelOption.ALLOCATOR, ChannelOption.AUTO_READ, ChannelOption.AUTO_CLOSE, ChannelOption.RCVBUF_ALLOCATOR, ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, ChannelOption.WRITE_BUFFER_WATER_MARK, ChannelOption.MESSAGE_SIZE_ESTIMATOR, ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP, ChannelOption.MAX_MESSAGES_PER_WRITE });
  }
  





  protected Map<ChannelOption<?>, Object> getOptions(Map<ChannelOption<?>, Object> result, ChannelOption<?>... options)
  {
    if (result == null) {
      result = new IdentityHashMap();
    }
    for (ChannelOption<?> o : options) {
      result.put(o, getOption(o));
    }
    return result;
  }
  

  public boolean setOptions(Map<ChannelOption<?>, ?> options)
  {
    ObjectUtil.checkNotNull(options, "options");
    
    boolean setAllOptions = true;
    for (Map.Entry<ChannelOption<?>, ?> e : options.entrySet()) {
      if (!setOption((ChannelOption)e.getKey(), e.getValue())) {
        setAllOptions = false;
      }
    }
    
    return setAllOptions;
  }
  

  public <T> T getOption(ChannelOption<T> option)
  {
    ObjectUtil.checkNotNull(option, "option");
    
    if (option == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
      return Integer.valueOf(getConnectTimeoutMillis());
    }
    if (option == ChannelOption.MAX_MESSAGES_PER_READ) {
      return Integer.valueOf(getMaxMessagesPerRead());
    }
    if (option == ChannelOption.WRITE_SPIN_COUNT) {
      return Integer.valueOf(getWriteSpinCount());
    }
    if (option == ChannelOption.ALLOCATOR) {
      return getAllocator();
    }
    if (option == ChannelOption.RCVBUF_ALLOCATOR) {
      return getRecvByteBufAllocator();
    }
    if (option == ChannelOption.AUTO_READ) {
      return Boolean.valueOf(isAutoRead());
    }
    if (option == ChannelOption.AUTO_CLOSE) {
      return Boolean.valueOf(isAutoClose());
    }
    if (option == ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK) {
      return Integer.valueOf(getWriteBufferHighWaterMark());
    }
    if (option == ChannelOption.WRITE_BUFFER_LOW_WATER_MARK) {
      return Integer.valueOf(getWriteBufferLowWaterMark());
    }
    if (option == ChannelOption.WRITE_BUFFER_WATER_MARK) {
      return getWriteBufferWaterMark();
    }
    if (option == ChannelOption.MESSAGE_SIZE_ESTIMATOR) {
      return getMessageSizeEstimator();
    }
    if (option == ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP) {
      return Boolean.valueOf(getPinEventExecutorPerGroup());
    }
    if (option == ChannelOption.MAX_MESSAGES_PER_WRITE) {
      return Integer.valueOf(getMaxMessagesPerWrite());
    }
    return null;
  }
  

  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
      setConnectTimeoutMillis(((Integer)value).intValue());
    } else if (option == ChannelOption.MAX_MESSAGES_PER_READ) {
      setMaxMessagesPerRead(((Integer)value).intValue());
    } else if (option == ChannelOption.WRITE_SPIN_COUNT) {
      setWriteSpinCount(((Integer)value).intValue());
    } else if (option == ChannelOption.ALLOCATOR) {
      setAllocator((ByteBufAllocator)value);
    } else if (option == ChannelOption.RCVBUF_ALLOCATOR) {
      setRecvByteBufAllocator((RecvByteBufAllocator)value);
    } else if (option == ChannelOption.AUTO_READ) {
      setAutoRead(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.AUTO_CLOSE) {
      setAutoClose(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK) {
      setWriteBufferHighWaterMark(((Integer)value).intValue());
    } else if (option == ChannelOption.WRITE_BUFFER_LOW_WATER_MARK) {
      setWriteBufferLowWaterMark(((Integer)value).intValue());
    } else if (option == ChannelOption.WRITE_BUFFER_WATER_MARK) {
      setWriteBufferWaterMark((WriteBufferWaterMark)value);
    } else if (option == ChannelOption.MESSAGE_SIZE_ESTIMATOR) {
      setMessageSizeEstimator((MessageSizeEstimator)value);
    } else if (option == ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP) {
      setPinEventExecutorPerGroup(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.MAX_MESSAGES_PER_WRITE) {
      setMaxMessagesPerWrite(((Integer)value).intValue());
    } else {
      return false;
    }
    
    return true;
  }
  
  protected <T> void validate(ChannelOption<T> option, T value) {
    ((ChannelOption)ObjectUtil.checkNotNull(option, "option")).validate(value);
  }
  
  public int getConnectTimeoutMillis()
  {
    return connectTimeoutMillis;
  }
  
  public ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    ObjectUtil.checkPositiveOrZero(connectTimeoutMillis, "connectTimeoutMillis");
    this.connectTimeoutMillis = connectTimeoutMillis;
    return this;
  }
  





  @Deprecated
  public int getMaxMessagesPerRead()
  {
    try
    {
      MaxMessagesRecvByteBufAllocator allocator = (MaxMessagesRecvByteBufAllocator)getRecvByteBufAllocator();
      return allocator.maxMessagesPerRead();
    } catch (ClassCastException e) {
      throw new IllegalStateException("getRecvByteBufAllocator() must return an object of type MaxMessagesRecvByteBufAllocator", e);
    }
  }
  






  @Deprecated
  public ChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    try
    {
      MaxMessagesRecvByteBufAllocator allocator = (MaxMessagesRecvByteBufAllocator)getRecvByteBufAllocator();
      allocator.maxMessagesPerRead(maxMessagesPerRead);
      return this;
    } catch (ClassCastException e) {
      throw new IllegalStateException("getRecvByteBufAllocator() must return an object of type MaxMessagesRecvByteBufAllocator", e);
    }
  }
  




  public int getMaxMessagesPerWrite()
  {
    return maxMessagesPerWrite;
  }
  



  public ChannelConfig setMaxMessagesPerWrite(int maxMessagesPerWrite)
  {
    this.maxMessagesPerWrite = ObjectUtil.checkPositive(maxMessagesPerWrite, "maxMessagesPerWrite");
    return this;
  }
  
  public int getWriteSpinCount()
  {
    return writeSpinCount;
  }
  
  public ChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    ObjectUtil.checkPositive(writeSpinCount, "writeSpinCount");
    



    if (writeSpinCount == Integer.MAX_VALUE) {
      writeSpinCount--;
    }
    this.writeSpinCount = writeSpinCount;
    return this;
  }
  
  public ByteBufAllocator getAllocator()
  {
    return allocator;
  }
  
  public ChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    this.allocator = ((ByteBufAllocator)ObjectUtil.checkNotNull(allocator, "allocator"));
    return this;
  }
  

  public <T extends RecvByteBufAllocator> T getRecvByteBufAllocator()
  {
    return rcvBufAllocator;
  }
  
  public ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    rcvBufAllocator = ((RecvByteBufAllocator)ObjectUtil.checkNotNull(allocator, "allocator"));
    return this;
  }
  





  private void setRecvByteBufAllocator(RecvByteBufAllocator allocator, ChannelMetadata metadata)
  {
    ObjectUtil.checkNotNull(allocator, "allocator");
    ObjectUtil.checkNotNull(metadata, "metadata");
    if ((allocator instanceof MaxMessagesRecvByteBufAllocator)) {
      ((MaxMessagesRecvByteBufAllocator)allocator).maxMessagesPerRead(metadata.defaultMaxMessagesPerRead());
    }
    setRecvByteBufAllocator(allocator);
  }
  
  public boolean isAutoRead()
  {
    return autoRead == 1;
  }
  
  public ChannelConfig setAutoRead(boolean autoRead)
  {
    boolean oldAutoRead = AUTOREAD_UPDATER.getAndSet(this, autoRead ? 1 : 0) == 1;
    if ((autoRead) && (!oldAutoRead)) {
      channel.read();
    } else if ((!autoRead) && (oldAutoRead)) {
      autoReadCleared();
    }
    return this;
  }
  


  protected void autoReadCleared() {}
  


  public boolean isAutoClose()
  {
    return autoClose;
  }
  
  public ChannelConfig setAutoClose(boolean autoClose)
  {
    this.autoClose = autoClose;
    return this;
  }
  
  public int getWriteBufferHighWaterMark()
  {
    return writeBufferWaterMark.high();
  }
  
  public ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    ObjectUtil.checkPositiveOrZero(writeBufferHighWaterMark, "writeBufferHighWaterMark");
    for (;;) {
      WriteBufferWaterMark waterMark = writeBufferWaterMark;
      if (writeBufferHighWaterMark < waterMark.low())
      {

        throw new IllegalArgumentException("writeBufferHighWaterMark cannot be less than writeBufferLowWaterMark (" + waterMark.low() + "): " + writeBufferHighWaterMark);
      }
      
      if (WATERMARK_UPDATER.compareAndSet(this, waterMark, new WriteBufferWaterMark(waterMark
        .low(), writeBufferHighWaterMark, false))) {
        return this;
      }
    }
  }
  
  public int getWriteBufferLowWaterMark()
  {
    return writeBufferWaterMark.low();
  }
  
  public ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    ObjectUtil.checkPositiveOrZero(writeBufferLowWaterMark, "writeBufferLowWaterMark");
    for (;;) {
      WriteBufferWaterMark waterMark = writeBufferWaterMark;
      if (writeBufferLowWaterMark > waterMark.high())
      {

        throw new IllegalArgumentException("writeBufferLowWaterMark cannot be greater than writeBufferHighWaterMark (" + waterMark.high() + "): " + writeBufferLowWaterMark);
      }
      
      if (WATERMARK_UPDATER.compareAndSet(this, waterMark, new WriteBufferWaterMark(writeBufferLowWaterMark, waterMark
        .high(), false))) {
        return this;
      }
    }
  }
  
  public ChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    this.writeBufferWaterMark = ((WriteBufferWaterMark)ObjectUtil.checkNotNull(writeBufferWaterMark, "writeBufferWaterMark"));
    return this;
  }
  
  public WriteBufferWaterMark getWriteBufferWaterMark()
  {
    return writeBufferWaterMark;
  }
  
  public MessageSizeEstimator getMessageSizeEstimator()
  {
    return msgSizeEstimator;
  }
  
  public ChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    msgSizeEstimator = ((MessageSizeEstimator)ObjectUtil.checkNotNull(estimator, "estimator"));
    return this;
  }
  
  private ChannelConfig setPinEventExecutorPerGroup(boolean pinEventExecutor) {
    this.pinEventExecutor = pinEventExecutor;
    return this;
  }
  
  private boolean getPinEventExecutorPerGroup() {
    return pinEventExecutor;
  }
}
