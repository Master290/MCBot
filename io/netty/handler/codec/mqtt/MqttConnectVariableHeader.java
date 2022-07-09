package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;




























public final class MqttConnectVariableHeader
{
  private final String name;
  private final int version;
  private final boolean hasUserName;
  private final boolean hasPassword;
  private final boolean isWillRetain;
  private final int willQos;
  private final boolean isWillFlag;
  private final boolean isCleanSession;
  private final int keepAliveTimeSeconds;
  private final MqttProperties properties;
  
  public MqttConnectVariableHeader(String name, int version, boolean hasUserName, boolean hasPassword, boolean isWillRetain, int willQos, boolean isWillFlag, boolean isCleanSession, int keepAliveTimeSeconds)
  {
    this(name, version, hasUserName, hasPassword, isWillRetain, willQos, isWillFlag, isCleanSession, keepAliveTimeSeconds, MqttProperties.NO_PROPERTIES);
  }
  


















  public MqttConnectVariableHeader(String name, int version, boolean hasUserName, boolean hasPassword, boolean isWillRetain, int willQos, boolean isWillFlag, boolean isCleanSession, int keepAliveTimeSeconds, MqttProperties properties)
  {
    this.name = name;
    this.version = version;
    this.hasUserName = hasUserName;
    this.hasPassword = hasPassword;
    this.isWillRetain = isWillRetain;
    this.willQos = willQos;
    this.isWillFlag = isWillFlag;
    this.isCleanSession = isCleanSession;
    this.keepAliveTimeSeconds = keepAliveTimeSeconds;
    this.properties = MqttProperties.withEmptyDefaults(properties);
  }
  
  public String name() {
    return name;
  }
  
  public int version() {
    return version;
  }
  
  public boolean hasUserName() {
    return hasUserName;
  }
  
  public boolean hasPassword() {
    return hasPassword;
  }
  
  public boolean isWillRetain() {
    return isWillRetain;
  }
  
  public int willQos() {
    return willQos;
  }
  
  public boolean isWillFlag() {
    return isWillFlag;
  }
  
  public boolean isCleanSession() {
    return isCleanSession;
  }
  
  public int keepAliveTimeSeconds() {
    return keepAliveTimeSeconds;
  }
  
  public MqttProperties properties() {
    return properties;
  }
  
  public String toString()
  {
    return 
    








      StringUtil.simpleClassName(this) + '[' + "name=" + name + ", version=" + version + ", hasUserName=" + hasUserName + ", hasPassword=" + hasPassword + ", isWillRetain=" + isWillRetain + ", isWillFlag=" + isWillFlag + ", isCleanSession=" + isCleanSession + ", keepAliveTimeSeconds=" + keepAliveTimeSeconds + ']';
  }
}
