package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.UncheckedBooleanSupplier;
import io.netty.util.internal.ObjectUtil;













































































































public abstract interface RecvByteBufAllocator
{
  public abstract Handle newHandle();
  
  public static class DelegatingHandle
    implements RecvByteBufAllocator.Handle
  {
    private final RecvByteBufAllocator.Handle delegate;
    
    public DelegatingHandle(RecvByteBufAllocator.Handle delegate)
    {
      this.delegate = ((RecvByteBufAllocator.Handle)ObjectUtil.checkNotNull(delegate, "delegate"));
    }
    



    protected final RecvByteBufAllocator.Handle delegate()
    {
      return delegate;
    }
    
    public ByteBuf allocate(ByteBufAllocator alloc)
    {
      return delegate.allocate(alloc);
    }
    
    public int guess()
    {
      return delegate.guess();
    }
    
    public void reset(ChannelConfig config)
    {
      delegate.reset(config);
    }
    
    public void incMessagesRead(int numMessages)
    {
      delegate.incMessagesRead(numMessages);
    }
    
    public void lastBytesRead(int bytes)
    {
      delegate.lastBytesRead(bytes);
    }
    
    public int lastBytesRead()
    {
      return delegate.lastBytesRead();
    }
    
    public boolean continueReading()
    {
      return delegate.continueReading();
    }
    
    public int attemptedBytesRead()
    {
      return delegate.attemptedBytesRead();
    }
    
    public void attemptedBytesRead(int bytes)
    {
      delegate.attemptedBytesRead(bytes);
    }
    
    public void readComplete()
    {
      delegate.readComplete();
    }
  }
  
  public static abstract interface ExtendedHandle
    extends RecvByteBufAllocator.Handle
  {
    public abstract boolean continueReading(UncheckedBooleanSupplier paramUncheckedBooleanSupplier);
  }
  
  @Deprecated
  public static abstract interface Handle
  {
    public abstract ByteBuf allocate(ByteBufAllocator paramByteBufAllocator);
    
    public abstract int guess();
    
    public abstract void reset(ChannelConfig paramChannelConfig);
    
    public abstract void incMessagesRead(int paramInt);
    
    public abstract void lastBytesRead(int paramInt);
    
    public abstract int lastBytesRead();
    
    public abstract void attemptedBytesRead(int paramInt);
    
    public abstract int attemptedBytesRead();
    
    public abstract boolean continueReading();
    
    public abstract void readComplete();
  }
}
