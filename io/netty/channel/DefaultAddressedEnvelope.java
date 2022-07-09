package io.netty.channel;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.net.SocketAddress;


























public class DefaultAddressedEnvelope<M, A extends SocketAddress>
  implements AddressedEnvelope<M, A>
{
  private final M message;
  private final A sender;
  private final A recipient;
  
  public DefaultAddressedEnvelope(M message, A recipient, A sender)
  {
    ObjectUtil.checkNotNull(message, "message");
    if ((recipient == null) && (sender == null)) {
      throw new NullPointerException("recipient and sender");
    }
    
    this.message = message;
    this.sender = sender;
    this.recipient = recipient;
  }
  



  public DefaultAddressedEnvelope(M message, A recipient)
  {
    this(message, recipient, null);
  }
  
  public M content()
  {
    return message;
  }
  
  public A sender()
  {
    return sender;
  }
  
  public A recipient()
  {
    return recipient;
  }
  
  public int refCnt()
  {
    if ((message instanceof ReferenceCounted)) {
      return ((ReferenceCounted)message).refCnt();
    }
    return 1;
  }
  

  public AddressedEnvelope<M, A> retain()
  {
    ReferenceCountUtil.retain(message);
    return this;
  }
  
  public AddressedEnvelope<M, A> retain(int increment)
  {
    ReferenceCountUtil.retain(message, increment);
    return this;
  }
  
  public boolean release()
  {
    return ReferenceCountUtil.release(message);
  }
  
  public boolean release(int decrement)
  {
    return ReferenceCountUtil.release(message, decrement);
  }
  
  public AddressedEnvelope<M, A> touch()
  {
    ReferenceCountUtil.touch(message);
    return this;
  }
  
  public AddressedEnvelope<M, A> touch(Object hint)
  {
    ReferenceCountUtil.touch(message, hint);
    return this;
  }
  
  public String toString()
  {
    if (sender != null) {
      return StringUtil.simpleClassName(this) + '(' + sender + " => " + recipient + ", " + message + ')';
    }
    
    return StringUtil.simpleClassName(this) + "(=> " + recipient + ", " + message + ')';
  }
}
