package io.netty.channel.kqueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.RecvByteBufAllocator.DelegatingHandle;
import io.netty.channel.RecvByteBufAllocator.ExtendedHandle;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.util.UncheckedBooleanSupplier;















final class KQueueRecvByteAllocatorHandle
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
  private boolean overrideGuess;
  private boolean readEOF;
  private long numberBytesPending;
  
  KQueueRecvByteAllocatorHandle(RecvByteBufAllocator.ExtendedHandle handle) {
    super(handle);
  }
  
  public int guess()
  {
    return overrideGuess ? guess0() : delegate().guess();
  }
  
  public void reset(ChannelConfig config)
  {
    overrideGuess = ((KQueueChannelConfig)config).getRcvAllocTransportProvidesGuess();
    delegate().reset(config);
  }
  

  public ByteBuf allocate(ByteBufAllocator alloc)
  {
    preferredDirectByteBufAllocator.updateAllocator(alloc);
    return overrideGuess ? preferredDirectByteBufAllocator.ioBuffer(guess0()) : 
      delegate().allocate(preferredDirectByteBufAllocator);
  }
  
  public void lastBytesRead(int bytes)
  {
    numberBytesPending = (bytes < 0 ? 0L : Math.max(0L, numberBytesPending - bytes));
    delegate().lastBytesRead(bytes);
  }
  
  public boolean continueReading(UncheckedBooleanSupplier maybeMoreDataSupplier)
  {
    return ((RecvByteBufAllocator.ExtendedHandle)delegate()).continueReading(maybeMoreDataSupplier);
  }
  

  public boolean continueReading()
  {
    return continueReading(defaultMaybeMoreDataSupplier);
  }
  
  void readEOF() {
    readEOF = true;
  }
  
  boolean isReadEOF() {
    return readEOF;
  }
  
  void numberBytesPending(long numberBytesPending) {
    this.numberBytesPending = numberBytesPending;
  }
  









  boolean maybeMoreDataToRead()
  {
    return numberBytesPending != 0L;
  }
  
  private int guess0() {
    return (int)Math.min(numberBytesPending, 2147483647L);
  }
}
