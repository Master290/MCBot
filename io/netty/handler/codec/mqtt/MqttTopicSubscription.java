package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;




















public final class MqttTopicSubscription
{
  private final String topicFilter;
  private final MqttSubscriptionOption option;
  
  public MqttTopicSubscription(String topicFilter, MqttQoS qualityOfService)
  {
    this.topicFilter = topicFilter;
    option = MqttSubscriptionOption.onlyFromQos(qualityOfService);
  }
  
  public MqttTopicSubscription(String topicFilter, MqttSubscriptionOption option) {
    this.topicFilter = topicFilter;
    this.option = option;
  }
  
  public String topicName() {
    return topicFilter;
  }
  
  public MqttQoS qualityOfService() {
    return option.qos();
  }
  
  public MqttSubscriptionOption option() {
    return option;
  }
  
  public String toString()
  {
    return 
    


      StringUtil.simpleClassName(this) + '[' + "topicFilter=" + topicFilter + ", option=" + option + ']';
  }
}
