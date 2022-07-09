package io.netty.handler.codec.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;




















public class MqttPublishMessage
  extends MqttMessage
  implements ByteBufHolder
{
  public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader variableHeader, ByteBuf payload)
  {
    super(mqttFixedHeader, variableHeader, payload);
  }
  
  public MqttPublishVariableHeader variableHeader()
  {
    return (MqttPublishVariableHeader)super.variableHeader();
  }
  
  public ByteBuf payload()
  {
    return content();
  }
  
  public ByteBuf content()
  {
    return ByteBufUtil.ensureAccessible((ByteBuf)super.payload());
  }
  
  public MqttPublishMessage copy()
  {
    return replace(content().copy());
  }
  
  public MqttPublishMessage duplicate()
  {
    return replace(content().duplicate());
  }
  
  public MqttPublishMessage retainedDuplicate()
  {
    return replace(content().retainedDuplicate());
  }
  
  public MqttPublishMessage replace(ByteBuf content)
  {
    return new MqttPublishMessage(fixedHeader(), variableHeader(), content);
  }
  
  public int refCnt()
  {
    return content().refCnt();
  }
  
  public MqttPublishMessage retain()
  {
    content().retain();
    return this;
  }
  
  public MqttPublishMessage retain(int increment)
  {
    content().retain(increment);
    return this;
  }
  
  public MqttPublishMessage touch()
  {
    content().touch();
    return this;
  }
  
  public MqttPublishMessage touch(Object hint)
  {
    content().touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return content().release();
  }
  
  public boolean release(int decrement)
  {
    return content().release(decrement);
  }
}
