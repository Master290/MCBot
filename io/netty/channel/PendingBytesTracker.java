package io.netty.channel;

import io.netty.util.internal.ObjectUtil;













abstract class PendingBytesTracker
  implements MessageSizeEstimator.Handle
{
  private final MessageSizeEstimator.Handle estimatorHandle;
  
  private PendingBytesTracker(MessageSizeEstimator.Handle estimatorHandle)
  {
    this.estimatorHandle = ((MessageSizeEstimator.Handle)ObjectUtil.checkNotNull(estimatorHandle, "estimatorHandle"));
  }
  


  public final int size(Object msg) { return estimatorHandle.size(msg); }
  
  public abstract void incrementPendingOutboundBytes(long paramLong);
  
  public abstract void decrementPendingOutboundBytes(long paramLong);
  
  static PendingBytesTracker newTracker(Channel channel) {
    if ((channel.pipeline() instanceof DefaultChannelPipeline)) {
      return new DefaultChannelPipelinePendingBytesTracker((DefaultChannelPipeline)channel.pipeline());
    }
    ChannelOutboundBuffer buffer = channel.unsafe().outboundBuffer();
    MessageSizeEstimator.Handle handle = channel.config().getMessageSizeEstimator().newHandle();
    


    return buffer == null ? new NoopPendingBytesTracker(handle) : new ChannelOutboundBufferPendingBytesTracker(buffer, handle);
  }
  
  private static final class DefaultChannelPipelinePendingBytesTracker extends PendingBytesTracker
  {
    private final DefaultChannelPipeline pipeline;
    
    DefaultChannelPipelinePendingBytesTracker(DefaultChannelPipeline pipeline)
    {
      super(null);
      this.pipeline = pipeline;
    }
    
    public void incrementPendingOutboundBytes(long bytes)
    {
      pipeline.incrementPendingOutboundBytes(bytes);
    }
    
    public void decrementPendingOutboundBytes(long bytes)
    {
      pipeline.decrementPendingOutboundBytes(bytes);
    }
  }
  
  private static final class ChannelOutboundBufferPendingBytesTracker extends PendingBytesTracker
  {
    private final ChannelOutboundBuffer buffer;
    
    ChannelOutboundBufferPendingBytesTracker(ChannelOutboundBuffer buffer, MessageSizeEstimator.Handle estimatorHandle) {
      super(null);
      this.buffer = buffer;
    }
    
    public void incrementPendingOutboundBytes(long bytes)
    {
      buffer.incrementPendingOutboundBytes(bytes);
    }
    
    public void decrementPendingOutboundBytes(long bytes)
    {
      buffer.decrementPendingOutboundBytes(bytes);
    }
  }
  
  private static final class NoopPendingBytesTracker extends PendingBytesTracker
  {
    NoopPendingBytesTracker(MessageSizeEstimator.Handle estimatorHandle) {
      super(null);
    }
    
    public void incrementPendingOutboundBytes(long bytes) {}
    
    public void decrementPendingOutboundBytes(long bytes) {}
  }
}
