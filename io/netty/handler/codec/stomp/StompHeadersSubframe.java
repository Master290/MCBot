package io.netty.handler.codec.stomp;

public abstract interface StompHeadersSubframe
  extends StompSubframe
{
  public abstract StompCommand command();
  
  public abstract StompHeaders headers();
}
