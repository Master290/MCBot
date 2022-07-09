package io.netty.handler.codec.mqtt;

import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import java.util.Arrays;





























public final class MqttConnectPayload
{
  private final String clientIdentifier;
  private final MqttProperties willProperties;
  private final String willTopic;
  private final byte[] willMessage;
  private final String userName;
  private final byte[] password;
  
  @Deprecated
  public MqttConnectPayload(String clientIdentifier, String willTopic, String willMessage, String userName, String password)
  {
    this(clientIdentifier, MqttProperties.NO_PROPERTIES, willTopic, willMessage
    


      .getBytes(CharsetUtil.UTF_8), userName, password
      
      .getBytes(CharsetUtil.UTF_8));
  }
  




  public MqttConnectPayload(String clientIdentifier, String willTopic, byte[] willMessage, String userName, byte[] password)
  {
    this(clientIdentifier, MqttProperties.NO_PROPERTIES, willTopic, willMessage, userName, password);
  }
  










  public MqttConnectPayload(String clientIdentifier, MqttProperties willProperties, String willTopic, byte[] willMessage, String userName, byte[] password)
  {
    this.clientIdentifier = clientIdentifier;
    this.willProperties = MqttProperties.withEmptyDefaults(willProperties);
    this.willTopic = willTopic;
    this.willMessage = willMessage;
    this.userName = userName;
    this.password = password;
  }
  
  public String clientIdentifier() {
    return clientIdentifier;
  }
  
  public MqttProperties willProperties() {
    return willProperties;
  }
  
  public String willTopic() {
    return willTopic;
  }
  


  @Deprecated
  public String willMessage()
  {
    return willMessage == null ? null : new String(willMessage, CharsetUtil.UTF_8);
  }
  
  public byte[] willMessageInBytes() {
    return willMessage;
  }
  
  public String userName() {
    return userName;
  }
  


  @Deprecated
  public String password()
  {
    return password == null ? null : new String(password, CharsetUtil.UTF_8);
  }
  
  public byte[] passwordInBytes() {
    return password;
  }
  
  public String toString()
  {
    return 
    





      StringUtil.simpleClassName(this) + '[' + "clientIdentifier=" + clientIdentifier + ", willTopic=" + willTopic + ", willMessage=" + Arrays.toString(willMessage) + ", userName=" + userName + ", password=" + Arrays.toString(password) + ']';
  }
}
