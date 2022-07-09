package io.netty.handler.codec.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


















public final class MqttMessageBuilders
{
  public static final class PublishBuilder
  {
    private String topic;
    private boolean retained;
    private MqttQoS qos;
    private ByteBuf payload;
    private int messageId;
    private MqttProperties mqttProperties;
    
    PublishBuilder() {}
    
    public PublishBuilder topicName(String topic)
    {
      this.topic = topic;
      return this;
    }
    
    public PublishBuilder retained(boolean retained) {
      this.retained = retained;
      return this;
    }
    
    public PublishBuilder qos(MqttQoS qos) {
      this.qos = qos;
      return this;
    }
    
    public PublishBuilder payload(ByteBuf payload) {
      this.payload = payload;
      return this;
    }
    
    public PublishBuilder messageId(int messageId) {
      this.messageId = messageId;
      return this;
    }
    
    public PublishBuilder properties(MqttProperties properties) {
      mqttProperties = properties;
      return this;
    }
    
    public MqttPublishMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained, 0);
      MqttPublishVariableHeader mqttVariableHeader = new MqttPublishVariableHeader(topic, messageId, mqttProperties);
      
      return new MqttPublishMessage(mqttFixedHeader, mqttVariableHeader, Unpooled.buffer().writeBytes(payload));
    }
  }
  
  public static final class ConnectBuilder
  {
    private MqttVersion version = MqttVersion.MQTT_3_1_1;
    private String clientId;
    private boolean cleanSession;
    private boolean hasUser;
    private boolean hasPassword;
    private int keepAliveSecs;
    private MqttProperties willProperties = MqttProperties.NO_PROPERTIES;
    private boolean willFlag;
    private boolean willRetain;
    private MqttQoS willQos = MqttQoS.AT_MOST_ONCE;
    private String willTopic;
    private byte[] willMessage;
    private String username;
    private byte[] password;
    private MqttProperties properties = MqttProperties.NO_PROPERTIES;
    
    ConnectBuilder() {}
    
    public ConnectBuilder protocolVersion(MqttVersion version)
    {
      this.version = version;
      return this;
    }
    
    public ConnectBuilder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }
    
    public ConnectBuilder cleanSession(boolean cleanSession) {
      this.cleanSession = cleanSession;
      return this;
    }
    
    public ConnectBuilder keepAlive(int keepAliveSecs) {
      this.keepAliveSecs = keepAliveSecs;
      return this;
    }
    
    public ConnectBuilder willFlag(boolean willFlag) {
      this.willFlag = willFlag;
      return this;
    }
    
    public ConnectBuilder willQoS(MqttQoS willQos) {
      this.willQos = willQos;
      return this;
    }
    
    public ConnectBuilder willTopic(String willTopic) {
      this.willTopic = willTopic;
      return this;
    }
    


    @Deprecated
    public ConnectBuilder willMessage(String willMessage)
    {
      willMessage(willMessage == null ? null : willMessage.getBytes(CharsetUtil.UTF_8));
      return this;
    }
    
    public ConnectBuilder willMessage(byte[] willMessage) {
      this.willMessage = willMessage;
      return this;
    }
    
    public ConnectBuilder willRetain(boolean willRetain) {
      this.willRetain = willRetain;
      return this;
    }
    
    public ConnectBuilder willProperties(MqttProperties willProperties) {
      this.willProperties = willProperties;
      return this;
    }
    
    public ConnectBuilder hasUser(boolean value) {
      hasUser = value;
      return this;
    }
    
    public ConnectBuilder hasPassword(boolean value) {
      hasPassword = value;
      return this;
    }
    
    public ConnectBuilder username(String username) {
      hasUser = (username != null);
      this.username = username;
      return this;
    }
    


    @Deprecated
    public ConnectBuilder password(String password)
    {
      password(password == null ? null : password.getBytes(CharsetUtil.UTF_8));
      return this;
    }
    
    public ConnectBuilder password(byte[] password) {
      hasPassword = (password != null);
      this.password = password;
      return this;
    }
    
    public ConnectBuilder properties(MqttProperties properties) {
      this.properties = properties;
      return this;
    }
    
    public MqttConnectMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
      







      MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(version.protocolName(), version.protocolLevel(), hasUser, hasPassword, willRetain, willQos.value(), willFlag, cleanSession, keepAliveSecs, properties);
      



      MqttConnectPayload mqttConnectPayload = new MqttConnectPayload(clientId, willProperties, willTopic, willMessage, username, password);
      
      return new MqttConnectMessage(mqttFixedHeader, mqttConnectVariableHeader, mqttConnectPayload);
    }
  }
  
  public static final class SubscribeBuilder
  {
    private List<MqttTopicSubscription> subscriptions;
    private int messageId;
    private MqttProperties properties;
    
    SubscribeBuilder() {}
    
    public SubscribeBuilder addSubscription(MqttQoS qos, String topic)
    {
      ensureSubscriptionsExist();
      subscriptions.add(new MqttTopicSubscription(topic, qos));
      return this;
    }
    
    public SubscribeBuilder addSubscription(String topic, MqttSubscriptionOption option) {
      ensureSubscriptionsExist();
      subscriptions.add(new MqttTopicSubscription(topic, option));
      return this;
    }
    
    public SubscribeBuilder messageId(int messageId) {
      this.messageId = messageId;
      return this;
    }
    
    public SubscribeBuilder properties(MqttProperties properties) {
      this.properties = properties;
      return this;
    }
    
    public MqttSubscribeMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
      
      MqttMessageIdAndPropertiesVariableHeader mqttVariableHeader = new MqttMessageIdAndPropertiesVariableHeader(messageId, properties);
      
      MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(subscriptions);
      return new MqttSubscribeMessage(mqttFixedHeader, mqttVariableHeader, mqttSubscribePayload);
    }
    
    private void ensureSubscriptionsExist() {
      if (subscriptions == null) {
        subscriptions = new ArrayList(5);
      }
    }
  }
  
  public static final class UnsubscribeBuilder
  {
    private List<String> topicFilters;
    private int messageId;
    private MqttProperties properties;
    
    UnsubscribeBuilder() {}
    
    public UnsubscribeBuilder addTopicFilter(String topic)
    {
      if (topicFilters == null) {
        topicFilters = new ArrayList(5);
      }
      topicFilters.add(topic);
      return this;
    }
    
    public UnsubscribeBuilder messageId(int messageId) {
      this.messageId = messageId;
      return this;
    }
    
    public UnsubscribeBuilder properties(MqttProperties properties) {
      this.properties = properties;
      return this;
    }
    
    public MqttUnsubscribeMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
      
      MqttMessageIdAndPropertiesVariableHeader mqttVariableHeader = new MqttMessageIdAndPropertiesVariableHeader(messageId, properties);
      
      MqttUnsubscribePayload mqttSubscribePayload = new MqttUnsubscribePayload(topicFilters);
      return new MqttUnsubscribeMessage(mqttFixedHeader, mqttVariableHeader, mqttSubscribePayload);
    }
  }
  
  public static abstract interface PropertiesInitializer<T>
  {
    public abstract void apply(T paramT);
  }
  
  public static final class ConnAckBuilder {
    private MqttConnectReturnCode returnCode;
    private boolean sessionPresent;
    private MqttProperties properties = MqttProperties.NO_PROPERTIES;
    private MqttMessageBuilders.ConnAckPropertiesBuilder propsBuilder;
    
    private ConnAckBuilder() {}
    
    public ConnAckBuilder returnCode(MqttConnectReturnCode returnCode)
    {
      this.returnCode = returnCode;
      return this;
    }
    
    public ConnAckBuilder sessionPresent(boolean sessionPresent) {
      this.sessionPresent = sessionPresent;
      return this;
    }
    
    public ConnAckBuilder properties(MqttProperties properties) {
      this.properties = properties;
      return this;
    }
    
    public ConnAckBuilder properties(MqttMessageBuilders.PropertiesInitializer<MqttMessageBuilders.ConnAckPropertiesBuilder> consumer) {
      if (propsBuilder == null) {
        propsBuilder = new MqttMessageBuilders.ConnAckPropertiesBuilder();
      }
      consumer.apply(propsBuilder);
      return this;
    }
    
    public MqttConnAckMessage build() {
      if (propsBuilder != null) {
        properties = propsBuilder.build();
      }
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      
      MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent, properties);
      
      return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }
  }
  
  public static final class ConnAckPropertiesBuilder {
    private String clientId;
    private Long sessionExpiryInterval;
    private int receiveMaximum;
    private Byte maximumQos;
    private boolean retain;
    private Long maximumPacketSize;
    private int topicAliasMaximum;
    private String reasonString;
    private MqttProperties.UserProperties userProperties = new MqttProperties.UserProperties();
    
    private Boolean wildcardSubscriptionAvailable;
    private Boolean subscriptionIdentifiersAvailable;
    private Boolean sharedSubscriptionAvailable;
    private Integer serverKeepAlive;
    
    public ConnAckPropertiesBuilder() {}
    
    public MqttProperties build()
    {
      MqttProperties props = new MqttProperties();
      if (clientId != null) {
        props.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.ASSIGNED_CLIENT_IDENTIFIER.value(), clientId));
      }
      
      if (sessionExpiryInterval != null) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL
          .value(), Integer.valueOf(sessionExpiryInterval.intValue())));
      }
      if (receiveMaximum > 0) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.RECEIVE_MAXIMUM.value(), Integer.valueOf(receiveMaximum)));
      }
      if (maximumQos != null) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.MAXIMUM_QOS.value(), Integer.valueOf(receiveMaximum)));
      }
      props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.RETAIN_AVAILABLE.value(), Integer.valueOf(retain ? 1 : 0)));
      if (maximumPacketSize != null) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE.value(), 
          Integer.valueOf(maximumPacketSize.intValue())));
      }
      props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM.value(), 
        Integer.valueOf(topicAliasMaximum)));
      if (reasonString != null) {
        props.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.REASON_STRING.value(), reasonString));
      }
      props.add(userProperties);
      if (wildcardSubscriptionAvailable != null) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.WILDCARD_SUBSCRIPTION_AVAILABLE.value(), 
          Integer.valueOf(wildcardSubscriptionAvailable.booleanValue() ? 1 : 0)));
      }
      if (subscriptionIdentifiersAvailable != null) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER_AVAILABLE.value(), 
          Integer.valueOf(subscriptionIdentifiersAvailable.booleanValue() ? 1 : 0)));
      }
      if (sharedSubscriptionAvailable != null) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.SHARED_SUBSCRIPTION_AVAILABLE.value(), 
          Integer.valueOf(sharedSubscriptionAvailable.booleanValue() ? 1 : 0)));
      }
      if (serverKeepAlive != null) {
        props.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.SERVER_KEEP_ALIVE.value(), serverKeepAlive));
      }
      
      if (responseInformation != null) {
        props.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.RESPONSE_INFORMATION.value(), responseInformation));
      }
      
      if (serverReference != null) {
        props.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.SERVER_REFERENCE.value(), serverReference));
      }
      
      if (authenticationMethod != null) {
        props.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.AUTHENTICATION_METHOD.value(), authenticationMethod));
      }
      
      if (authenticationData != null) {
        props.add(new MqttProperties.BinaryProperty(MqttProperties.MqttPropertyType.AUTHENTICATION_DATA.value(), authenticationData));
      }
      

      return props; }
    
    private String responseInformation;
    
    public ConnAckPropertiesBuilder sessionExpiryInterval(long seconds) { sessionExpiryInterval = Long.valueOf(seconds);
      return this; }
    
    private String serverReference;
    
    public ConnAckPropertiesBuilder receiveMaximum(int value) { receiveMaximum = ObjectUtil.checkPositive(value, "value");
      return this; }
    
    private String authenticationMethod;
    private byte[] authenticationData;
    public ConnAckPropertiesBuilder maximumQos(byte value) { if ((value != 0) && (value != 1)) {
        throw new IllegalArgumentException("maximum QoS property could be 0 or 1");
      }
      maximumQos = Byte.valueOf(value);
      return this;
    }
    
    public ConnAckPropertiesBuilder retainAvailable(boolean retain) {
      this.retain = retain;
      return this;
    }
    
    public ConnAckPropertiesBuilder maximumPacketSize(long size) {
      maximumPacketSize = Long.valueOf(ObjectUtil.checkPositive(size, "size"));
      return this;
    }
    
    public ConnAckPropertiesBuilder assignedClientId(String clientId) {
      this.clientId = clientId;
      return this;
    }
    
    public ConnAckPropertiesBuilder topicAliasMaximum(int value) {
      topicAliasMaximum = value;
      return this;
    }
    
    public ConnAckPropertiesBuilder reasonString(String reason) {
      reasonString = reason;
      return this;
    }
    
    public ConnAckPropertiesBuilder userProperty(String name, String value) {
      userProperties.add(name, value);
      return this;
    }
    
    public ConnAckPropertiesBuilder wildcardSubscriptionAvailable(boolean value) {
      wildcardSubscriptionAvailable = Boolean.valueOf(value);
      return this;
    }
    
    public ConnAckPropertiesBuilder subscriptionIdentifiersAvailable(boolean value) {
      subscriptionIdentifiersAvailable = Boolean.valueOf(value);
      return this;
    }
    
    public ConnAckPropertiesBuilder sharedSubscriptionAvailable(boolean value) {
      sharedSubscriptionAvailable = Boolean.valueOf(value);
      return this;
    }
    
    public ConnAckPropertiesBuilder serverKeepAlive(int seconds) {
      serverKeepAlive = Integer.valueOf(seconds);
      return this;
    }
    
    public ConnAckPropertiesBuilder responseInformation(String value) {
      responseInformation = value;
      return this;
    }
    
    public ConnAckPropertiesBuilder serverReference(String host) {
      serverReference = host;
      return this;
    }
    
    public ConnAckPropertiesBuilder authenticationMethod(String methodName) {
      authenticationMethod = methodName;
      return this;
    }
    
    public ConnAckPropertiesBuilder authenticationData(byte[] rawData) {
      authenticationData = ((byte[])rawData.clone());
      return this;
    }
  }
  
  public static final class PubAckBuilder
  {
    private int packetId;
    private byte reasonCode;
    private MqttProperties properties;
    
    PubAckBuilder() {}
    
    public PubAckBuilder reasonCode(byte reasonCode)
    {
      this.reasonCode = reasonCode;
      return this;
    }
    
    public PubAckBuilder packetId(int packetId) {
      this.packetId = packetId;
      return this;
    }
    


    @Deprecated
    public PubAckBuilder packetId(short packetId)
    {
      return packetId(packetId & 0xFFFF);
    }
    
    public PubAckBuilder properties(MqttProperties properties) {
      this.properties = properties;
      return this;
    }
    
    public MqttMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      
      MqttPubReplyMessageVariableHeader mqttPubAckVariableHeader = new MqttPubReplyMessageVariableHeader(packetId, reasonCode, properties);
      
      return new MqttMessage(mqttFixedHeader, mqttPubAckVariableHeader);
    }
  }
  
  public static final class SubAckBuilder
  {
    private int packetId;
    private MqttProperties properties;
    private final List<MqttQoS> grantedQoses = new ArrayList();
    
    SubAckBuilder() {}
    
    public SubAckBuilder packetId(int packetId)
    {
      this.packetId = packetId;
      return this;
    }
    


    @Deprecated
    public SubAckBuilder packetId(short packetId)
    {
      return packetId(packetId & 0xFFFF);
    }
    
    public SubAckBuilder properties(MqttProperties properties) {
      this.properties = properties;
      return this;
    }
    
    public SubAckBuilder addGrantedQos(MqttQoS qos) {
      grantedQoses.add(qos);
      return this;
    }
    
    public SubAckBuilder addGrantedQoses(MqttQoS... qoses) {
      grantedQoses.addAll(Arrays.asList(qoses));
      return this;
    }
    
    public MqttSubAckMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      
      MqttMessageIdAndPropertiesVariableHeader mqttSubAckVariableHeader = new MqttMessageIdAndPropertiesVariableHeader(packetId, properties);
      


      int[] grantedQoses = new int[this.grantedQoses.size()];
      int i = 0;
      for (MqttQoS grantedQos : this.grantedQoses) {
        grantedQoses[(i++)] = grantedQos.value();
      }
      
      MqttSubAckPayload subAckPayload = new MqttSubAckPayload(grantedQoses);
      return new MqttSubAckMessage(mqttFixedHeader, mqttSubAckVariableHeader, subAckPayload);
    }
  }
  
  public static final class UnsubAckBuilder
  {
    private int packetId;
    private MqttProperties properties;
    private final List<Short> reasonCodes = new ArrayList();
    
    UnsubAckBuilder() {}
    
    public UnsubAckBuilder packetId(int packetId)
    {
      this.packetId = packetId;
      return this;
    }
    


    @Deprecated
    public UnsubAckBuilder packetId(short packetId)
    {
      return packetId(packetId & 0xFFFF);
    }
    
    public UnsubAckBuilder properties(MqttProperties properties) {
      this.properties = properties;
      return this;
    }
    
    public UnsubAckBuilder addReasonCode(short reasonCode) {
      reasonCodes.add(Short.valueOf(reasonCode));
      return this;
    }
    
    public UnsubAckBuilder addReasonCodes(Short... reasonCodes) {
      this.reasonCodes.addAll(Arrays.asList(reasonCodes));
      return this;
    }
    
    public MqttUnsubAckMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      
      MqttMessageIdAndPropertiesVariableHeader mqttSubAckVariableHeader = new MqttMessageIdAndPropertiesVariableHeader(packetId, properties);
      

      MqttUnsubAckPayload subAckPayload = new MqttUnsubAckPayload(reasonCodes);
      return new MqttUnsubAckMessage(mqttFixedHeader, mqttSubAckVariableHeader, subAckPayload);
    }
  }
  
  public static final class DisconnectBuilder
  {
    private MqttProperties properties;
    private byte reasonCode;
    
    DisconnectBuilder() {}
    
    public DisconnectBuilder properties(MqttProperties properties)
    {
      this.properties = properties;
      return this;
    }
    
    public DisconnectBuilder reasonCode(byte reasonCode) {
      this.reasonCode = reasonCode;
      return this;
    }
    
    public MqttMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
      
      MqttReasonCodeAndPropertiesVariableHeader mqttDisconnectVariableHeader = new MqttReasonCodeAndPropertiesVariableHeader(reasonCode, properties);
      

      return new MqttMessage(mqttFixedHeader, mqttDisconnectVariableHeader);
    }
  }
  
  public static final class AuthBuilder
  {
    private MqttProperties properties;
    private byte reasonCode;
    
    AuthBuilder() {}
    
    public AuthBuilder properties(MqttProperties properties)
    {
      this.properties = properties;
      return this;
    }
    
    public AuthBuilder reasonCode(byte reasonCode) {
      this.reasonCode = reasonCode;
      return this;
    }
    
    public MqttMessage build() {
      MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.AUTH, false, MqttQoS.AT_MOST_ONCE, false, 0);
      
      MqttReasonCodeAndPropertiesVariableHeader mqttAuthVariableHeader = new MqttReasonCodeAndPropertiesVariableHeader(reasonCode, properties);
      

      return new MqttMessage(mqttFixedHeader, mqttAuthVariableHeader);
    }
  }
  
  public static ConnectBuilder connect() {
    return new ConnectBuilder();
  }
  
  public static ConnAckBuilder connAck() {
    return new ConnAckBuilder(null);
  }
  
  public static PublishBuilder publish() {
    return new PublishBuilder();
  }
  
  public static SubscribeBuilder subscribe() {
    return new SubscribeBuilder();
  }
  
  public static UnsubscribeBuilder unsubscribe() {
    return new UnsubscribeBuilder();
  }
  
  public static PubAckBuilder pubAck() {
    return new PubAckBuilder();
  }
  
  public static SubAckBuilder subAck() {
    return new SubAckBuilder();
  }
  
  public static UnsubAckBuilder unsubAck() {
    return new UnsubAckBuilder();
  }
  
  public static DisconnectBuilder disconnect() {
    return new DisconnectBuilder();
  }
  
  public static AuthBuilder auth() {
    return new AuthBuilder();
  }
  
  private MqttMessageBuilders() {}
}
