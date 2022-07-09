package io.netty.channel.sctp;

import com.sun.nio.sctp.MessageInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.util.internal.ObjectUtil;























public final class SctpMessage
  extends DefaultByteBufHolder
{
  private final int streamIdentifier;
  private final int protocolIdentifier;
  private final boolean unordered;
  private final MessageInfo msgInfo;
  
  public SctpMessage(int protocolIdentifier, int streamIdentifier, ByteBuf payloadBuffer)
  {
    this(protocolIdentifier, streamIdentifier, false, payloadBuffer);
  }
  






  public SctpMessage(int protocolIdentifier, int streamIdentifier, boolean unordered, ByteBuf payloadBuffer)
  {
    super(payloadBuffer);
    this.protocolIdentifier = protocolIdentifier;
    this.streamIdentifier = streamIdentifier;
    this.unordered = unordered;
    msgInfo = null;
  }
  




  public SctpMessage(MessageInfo msgInfo, ByteBuf payloadBuffer)
  {
    super(payloadBuffer);
    this.msgInfo = ((MessageInfo)ObjectUtil.checkNotNull(msgInfo, "msgInfo"));
    streamIdentifier = msgInfo.streamNumber();
    protocolIdentifier = msgInfo.payloadProtocolID();
    unordered = msgInfo.isUnordered();
  }
  


  public int streamIdentifier()
  {
    return streamIdentifier;
  }
  


  public int protocolIdentifier()
  {
    return protocolIdentifier;
  }
  


  public boolean isUnordered()
  {
    return unordered;
  }
  



  public MessageInfo messageInfo()
  {
    return msgInfo;
  }
  


  public boolean isComplete()
  {
    if (msgInfo != null) {
      return msgInfo.isComplete();
    }
    
    return true;
  }
  

  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    
    SctpMessage sctpFrame = (SctpMessage)o;
    
    if (protocolIdentifier != protocolIdentifier) {
      return false;
    }
    
    if (streamIdentifier != streamIdentifier) {
      return false;
    }
    
    if (unordered != unordered) {
      return false;
    }
    
    return content().equals(sctpFrame.content());
  }
  
  public int hashCode()
  {
    int result = streamIdentifier;
    result = 31 * result + protocolIdentifier;
    
    result = 31 * result + (unordered ? 1231 : 1237);
    result = 31 * result + content().hashCode();
    return result;
  }
  
  public SctpMessage copy()
  {
    return (SctpMessage)super.copy();
  }
  
  public SctpMessage duplicate()
  {
    return (SctpMessage)super.duplicate();
  }
  
  public SctpMessage retainedDuplicate()
  {
    return (SctpMessage)super.retainedDuplicate();
  }
  
  public SctpMessage replace(ByteBuf content)
  {
    if (msgInfo == null) {
      return new SctpMessage(protocolIdentifier, streamIdentifier, unordered, content);
    }
    return new SctpMessage(msgInfo, content);
  }
  

  public SctpMessage retain()
  {
    super.retain();
    return this;
  }
  
  public SctpMessage retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public SctpMessage touch()
  {
    super.touch();
    return this;
  }
  
  public SctpMessage touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public String toString()
  {
    return 
    

      "SctpFrame{streamIdentifier=" + streamIdentifier + ", protocolIdentifier=" + protocolIdentifier + ", unordered=" + unordered + ", data=" + contentToString() + '}';
  }
}
