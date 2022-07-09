package io.netty.channel.rxtx;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.PreferHeapByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.util.internal.ObjectUtil;
import java.util.Map;





























@Deprecated
final class DefaultRxtxChannelConfig
  extends DefaultChannelConfig
  implements RxtxChannelConfig
{
  private volatile int baudrate = 115200;
  private volatile boolean dtr;
  private volatile boolean rts;
  private volatile RxtxChannelConfig.Stopbits stopbits = RxtxChannelConfig.Stopbits.STOPBITS_1;
  private volatile RxtxChannelConfig.Databits databits = RxtxChannelConfig.Databits.DATABITS_8;
  private volatile RxtxChannelConfig.Paritybit paritybit = RxtxChannelConfig.Paritybit.NONE;
  private volatile int waitTime;
  private volatile int readTimeout = 1000;
  
  DefaultRxtxChannelConfig(RxtxChannel channel) {
    super(channel);
    setAllocator(new PreferHeapByteBufAllocator(getAllocator()));
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(super.getOptions(), new ChannelOption[] { RxtxChannelOption.BAUD_RATE, RxtxChannelOption.DTR, RxtxChannelOption.RTS, RxtxChannelOption.STOP_BITS, RxtxChannelOption.DATA_BITS, RxtxChannelOption.PARITY_BIT, RxtxChannelOption.WAIT_TIME });
  }
  

  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == RxtxChannelOption.BAUD_RATE) {
      return Integer.valueOf(getBaudrate());
    }
    if (option == RxtxChannelOption.DTR) {
      return Boolean.valueOf(isDtr());
    }
    if (option == RxtxChannelOption.RTS) {
      return Boolean.valueOf(isRts());
    }
    if (option == RxtxChannelOption.STOP_BITS) {
      return getStopbits();
    }
    if (option == RxtxChannelOption.DATA_BITS) {
      return getDatabits();
    }
    if (option == RxtxChannelOption.PARITY_BIT) {
      return getParitybit();
    }
    if (option == RxtxChannelOption.WAIT_TIME) {
      return Integer.valueOf(getWaitTimeMillis());
    }
    if (option == RxtxChannelOption.READ_TIMEOUT) {
      return Integer.valueOf(getReadTimeout());
    }
    return super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == RxtxChannelOption.BAUD_RATE) {
      setBaudrate(((Integer)value).intValue());
    } else if (option == RxtxChannelOption.DTR) {
      setDtr(((Boolean)value).booleanValue());
    } else if (option == RxtxChannelOption.RTS) {
      setRts(((Boolean)value).booleanValue());
    } else if (option == RxtxChannelOption.STOP_BITS) {
      setStopbits((RxtxChannelConfig.Stopbits)value);
    } else if (option == RxtxChannelOption.DATA_BITS) {
      setDatabits((RxtxChannelConfig.Databits)value);
    } else if (option == RxtxChannelOption.PARITY_BIT) {
      setParitybit((RxtxChannelConfig.Paritybit)value);
    } else if (option == RxtxChannelOption.WAIT_TIME) {
      setWaitTimeMillis(((Integer)value).intValue());
    } else if (option == RxtxChannelOption.READ_TIMEOUT) {
      setReadTimeout(((Integer)value).intValue());
    } else {
      return super.setOption(option, value);
    }
    return true;
  }
  
  public RxtxChannelConfig setBaudrate(int baudrate)
  {
    this.baudrate = baudrate;
    return this;
  }
  
  public RxtxChannelConfig setStopbits(RxtxChannelConfig.Stopbits stopbits)
  {
    this.stopbits = stopbits;
    return this;
  }
  
  public RxtxChannelConfig setDatabits(RxtxChannelConfig.Databits databits)
  {
    this.databits = databits;
    return this;
  }
  
  public RxtxChannelConfig setParitybit(RxtxChannelConfig.Paritybit paritybit)
  {
    this.paritybit = paritybit;
    return this;
  }
  
  public int getBaudrate()
  {
    return baudrate;
  }
  
  public RxtxChannelConfig.Stopbits getStopbits()
  {
    return stopbits;
  }
  
  public RxtxChannelConfig.Databits getDatabits()
  {
    return databits;
  }
  
  public RxtxChannelConfig.Paritybit getParitybit()
  {
    return paritybit;
  }
  
  public boolean isDtr()
  {
    return dtr;
  }
  
  public RxtxChannelConfig setDtr(boolean dtr)
  {
    this.dtr = dtr;
    return this;
  }
  
  public boolean isRts()
  {
    return rts;
  }
  
  public RxtxChannelConfig setRts(boolean rts)
  {
    this.rts = rts;
    return this;
  }
  
  public int getWaitTimeMillis()
  {
    return waitTime;
  }
  
  public RxtxChannelConfig setWaitTimeMillis(int waitTimeMillis)
  {
    waitTime = ObjectUtil.checkPositiveOrZero(waitTimeMillis, "waitTimeMillis");
    return this;
  }
  
  public RxtxChannelConfig setReadTimeout(int readTimeout)
  {
    this.readTimeout = ObjectUtil.checkPositiveOrZero(readTimeout, "readTimeout");
    return this;
  }
  
  public int getReadTimeout()
  {
    return readTimeout;
  }
  
  public RxtxChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  @Deprecated
  public RxtxChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public RxtxChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public RxtxChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public RxtxChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public RxtxChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  public RxtxChannelConfig setAutoClose(boolean autoClose)
  {
    super.setAutoClose(autoClose);
    return this;
  }
  
  public RxtxChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public RxtxChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public RxtxChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark)
  {
    super.setWriteBufferWaterMark(writeBufferWaterMark);
    return this;
  }
  
  public RxtxChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
}
