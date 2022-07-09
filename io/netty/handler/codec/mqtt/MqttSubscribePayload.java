package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;
import java.util.Collections;
import java.util.List;




















public final class MqttSubscribePayload
{
  private final List<MqttTopicSubscription> topicSubscriptions;
  
  public MqttSubscribePayload(List<MqttTopicSubscription> topicSubscriptions)
  {
    this.topicSubscriptions = Collections.unmodifiableList(topicSubscriptions);
  }
  
  public List<MqttTopicSubscription> topicSubscriptions() {
    return topicSubscriptions;
  }
  
  public String toString()
  {
    StringBuilder builder = new StringBuilder(StringUtil.simpleClassName(this)).append('[');
    for (int i = 0; i < topicSubscriptions.size(); i++) {
      builder.append(topicSubscriptions.get(i)).append(", ");
    }
    if (!topicSubscriptions.isEmpty()) {
      builder.setLength(builder.length() - 2);
    }
    return ']';
  }
}
