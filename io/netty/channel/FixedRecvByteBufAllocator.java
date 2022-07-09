package io.netty.channel;

import io.netty.util.internal.ObjectUtil;















public class FixedRecvByteBufAllocator
  extends DefaultMaxMessagesRecvByteBufAllocator
{
  private final int bufferSize;
  
  private final class HandleImpl
    extends DefaultMaxMessagesRecvByteBufAllocator.MaxMessageHandle
  {
    private final int bufferSize;
    
    HandleImpl(int bufferSize)
    {
      super();
      this.bufferSize = bufferSize;
    }
    
    public int guess()
    {
      return bufferSize;
    }
  }
  



  public FixedRecvByteBufAllocator(int bufferSize)
  {
    ObjectUtil.checkPositive(bufferSize, "bufferSize");
    this.bufferSize = bufferSize;
  }
  

  public RecvByteBufAllocator.Handle newHandle()
  {
    return new HandleImpl(bufferSize);
  }
  
  public FixedRecvByteBufAllocator respectMaybeMoreData(boolean respectMaybeMoreData)
  {
    super.respectMaybeMoreData(respectMaybeMoreData);
    return this;
  }
}
