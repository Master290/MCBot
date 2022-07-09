package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.UncheckedBooleanSupplier;
import io.netty.util.internal.ObjectUtil;



















public abstract class DefaultMaxMessagesRecvByteBufAllocator
  implements MaxMessagesRecvByteBufAllocator
{
  private volatile int maxMessagesPerRead;
  private volatile boolean respectMaybeMoreData = true;
  
  public DefaultMaxMessagesRecvByteBufAllocator() {
    this(1);
  }
  
  public DefaultMaxMessagesRecvByteBufAllocator(int maxMessagesPerRead) {
    maxMessagesPerRead(maxMessagesPerRead);
  }
  
  public int maxMessagesPerRead()
  {
    return maxMessagesPerRead;
  }
  
  public MaxMessagesRecvByteBufAllocator maxMessagesPerRead(int maxMessagesPerRead)
  {
    ObjectUtil.checkPositive(maxMessagesPerRead, "maxMessagesPerRead");
    this.maxMessagesPerRead = maxMessagesPerRead;
    return this;
  }
  











  public DefaultMaxMessagesRecvByteBufAllocator respectMaybeMoreData(boolean respectMaybeMoreData)
  {
    this.respectMaybeMoreData = respectMaybeMoreData;
    return this;
  }
  










  public final boolean respectMaybeMoreData()
  {
    return respectMaybeMoreData;
  }
  

  public abstract class MaxMessageHandle
    implements RecvByteBufAllocator.ExtendedHandle
  {
    private ChannelConfig config;
    private int maxMessagePerRead;
    private int totalMessages;
    private int totalBytesRead;
    private int attemptedBytesRead;
    private int lastBytesRead;
    private final boolean respectMaybeMoreData = respectMaybeMoreData;
    private final UncheckedBooleanSupplier defaultMaybeMoreSupplier = new UncheckedBooleanSupplier()
    {
      public boolean get() {
        return attemptedBytesRead == lastBytesRead;
      }
    };
    

    public MaxMessageHandle() {}
    
    public void reset(ChannelConfig config)
    {
      this.config = config;
      maxMessagePerRead = maxMessagesPerRead();
      totalMessages = (this.totalBytesRead = 0);
    }
    
    public ByteBuf allocate(ByteBufAllocator alloc)
    {
      return alloc.ioBuffer(guess());
    }
    
    public final void incMessagesRead(int amt)
    {
      totalMessages += amt;
    }
    
    public void lastBytesRead(int bytes)
    {
      lastBytesRead = bytes;
      if (bytes > 0) {
        totalBytesRead += bytes;
      }
    }
    
    public final int lastBytesRead()
    {
      return lastBytesRead;
    }
    
    public boolean continueReading()
    {
      return continueReading(defaultMaybeMoreSupplier);
    }
    
    public boolean continueReading(UncheckedBooleanSupplier maybeMoreDataSupplier)
    {
      return (config.isAutoRead()) && ((!respectMaybeMoreData) || 
        (maybeMoreDataSupplier.get())) && (totalMessages < maxMessagePerRead) && (totalBytesRead > 0);
    }
    


    public void readComplete() {}
    


    public int attemptedBytesRead()
    {
      return attemptedBytesRead;
    }
    
    public void attemptedBytesRead(int bytes)
    {
      attemptedBytesRead = bytes;
    }
    
    protected final int totalBytesRead() {
      return totalBytesRead < 0 ? Integer.MAX_VALUE : totalBytesRead;
    }
  }
}
