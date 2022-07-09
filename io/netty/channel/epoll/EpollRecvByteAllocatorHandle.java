package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator.DelegatingHandle;
import io.netty.channel.RecvByteBufAllocator.ExtendedHandle;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.util.UncheckedBooleanSupplier;












class EpollRecvByteAllocatorHandle
  extends RecvByteBufAllocator.DelegatingHandle
  implements RecvByteBufAllocator.ExtendedHandle
{
  private final PreferredDirectByteBufAllocator preferredDirectByteBufAllocator = new PreferredDirectByteBufAllocator();
  
  private final UncheckedBooleanSupplier defaultMaybeMoreDataSupplier = new UncheckedBooleanSupplier()
  {
    public boolean get() {
      return maybeMoreDataToRead();
    }
  };
  private boolean isEdgeTriggered;
  private boolean receivedRdHup;
  
  EpollRecvByteAllocatorHandle(RecvByteBufAllocator.ExtendedHandle handle) {
    super(handle);
  }
  
  final void receivedRdHup() {
    receivedRdHup = true;
  }
  
  final boolean isReceivedRdHup() {
    return receivedRdHup;
  }
  








  boolean maybeMoreDataToRead()
  {
    return ((isEdgeTriggered) && (lastBytesRead() > 0)) || ((!isEdgeTriggered) && 
      (lastBytesRead() == attemptedBytesRead()));
  }
  
  final void edgeTriggered(boolean edgeTriggered) {
    isEdgeTriggered = edgeTriggered;
  }
  
  final boolean isEdgeTriggered() {
    return isEdgeTriggered;
  }
  

  public final ByteBuf allocate(ByteBufAllocator alloc)
  {
    preferredDirectByteBufAllocator.updateAllocator(alloc);
    return delegate().allocate(preferredDirectByteBufAllocator);
  }
  
  public final boolean continueReading(UncheckedBooleanSupplier maybeMoreDataSupplier)
  {
    return ((RecvByteBufAllocator.ExtendedHandle)delegate()).continueReading(maybeMoreDataSupplier);
  }
  

  public final boolean continueReading()
  {
    return continueReading(defaultMaybeMoreDataSupplier);
  }
}
