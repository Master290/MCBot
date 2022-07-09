package io.netty.channel.rxtx;

import io.netty.channel.WriteBufferWaterMark;

@Deprecated
public abstract interface RxtxChannelConfig extends io.netty.channel.ChannelConfig { public abstract RxtxChannelConfig setBaudrate(int paramInt);
  
  public abstract RxtxChannelConfig setStopbits(Stopbits paramStopbits);
  
  public abstract RxtxChannelConfig setDatabits(Databits paramDatabits);
  
  public abstract RxtxChannelConfig setParitybit(Paritybit paramParitybit);
  
  public abstract int getBaudrate();
  
  public abstract Stopbits getStopbits();
  
  public abstract Databits getDatabits();
  
  public abstract Paritybit getParitybit();
  
  public abstract boolean isDtr();
  
  public abstract RxtxChannelConfig setDtr(boolean paramBoolean);
  
  public abstract boolean isRts();
  
  public abstract RxtxChannelConfig setRts(boolean paramBoolean);
  
  public abstract int getWaitTimeMillis();
  
  public abstract RxtxChannelConfig setWaitTimeMillis(int paramInt);
  
  public abstract RxtxChannelConfig setReadTimeout(int paramInt);
  
  public abstract int getReadTimeout();
  
  public abstract RxtxChannelConfig setConnectTimeoutMillis(int paramInt);
  
  @Deprecated
  public abstract RxtxChannelConfig setMaxMessagesPerRead(int paramInt);
  
  public abstract RxtxChannelConfig setWriteSpinCount(int paramInt);
  
  public abstract RxtxChannelConfig setAllocator(io.netty.buffer.ByteBufAllocator paramByteBufAllocator);
  
  public abstract RxtxChannelConfig setRecvByteBufAllocator(io.netty.channel.RecvByteBufAllocator paramRecvByteBufAllocator);
  
  public abstract RxtxChannelConfig setAutoRead(boolean paramBoolean);
  
  public abstract RxtxChannelConfig setAutoClose(boolean paramBoolean);
  
  public abstract RxtxChannelConfig setWriteBufferHighWaterMark(int paramInt);
  
  public abstract RxtxChannelConfig setWriteBufferLowWaterMark(int paramInt);
  
  public abstract RxtxChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark paramWriteBufferWaterMark);
  
  public abstract RxtxChannelConfig setMessageSizeEstimator(io.netty.channel.MessageSizeEstimator paramMessageSizeEstimator);
  
  public static enum Stopbits { STOPBITS_1(1), 
    


    STOPBITS_2(2), 
    


    STOPBITS_1_5(3);
    
    private final int value;
    
    private Stopbits(int value) {
      this.value = value;
    }
    
    public int value() {
      return value;
    }
    
    public static Stopbits valueOf(int value) {
      for (Stopbits stopbit : ) {
        if (value == value) {
          return stopbit;
        }
      }
      throw new IllegalArgumentException("unknown " + Stopbits.class.getSimpleName() + " value: " + value);
    }
  }
  


  public static enum Databits
  {
    DATABITS_5(5), 
    


    DATABITS_6(6), 
    


    DATABITS_7(7), 
    


    DATABITS_8(8);
    
    private final int value;
    
    private Databits(int value) {
      this.value = value;
    }
    
    public int value() {
      return value;
    }
    
    public static Databits valueOf(int value) {
      for (Databits databit : ) {
        if (value == value) {
          return databit;
        }
      }
      throw new IllegalArgumentException("unknown " + Databits.class.getSimpleName() + " value: " + value);
    }
  }
  


  public static enum Paritybit
  {
    NONE(0), 
    



    ODD(1), 
    



    EVEN(2), 
    


    MARK(3), 
    


    SPACE(4);
    
    private final int value;
    
    private Paritybit(int value) {
      this.value = value;
    }
    
    public int value() {
      return value;
    }
    
    public static Paritybit valueOf(int value) {
      for (Paritybit paritybit : ) {
        if (value == value) {
          return paritybit;
        }
      }
      throw new IllegalArgumentException("unknown " + Paritybit.class.getSimpleName() + " value: " + value);
    }
  }
}
