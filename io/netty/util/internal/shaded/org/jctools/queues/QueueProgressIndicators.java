package io.netty.util.internal.shaded.org.jctools.queues;

public abstract interface QueueProgressIndicators
{
  public abstract long currentProducerIndex();
  
  public abstract long currentConsumerIndex();
}
