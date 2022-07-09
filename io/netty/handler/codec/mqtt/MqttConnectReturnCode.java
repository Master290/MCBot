package io.netty.handler.codec.mqtt;



















public enum MqttConnectReturnCode
{
  CONNECTION_ACCEPTED((byte)0), 
  
  CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte)1), 
  CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte)2), 
  CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte)3), 
  CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte)4), 
  CONNECTION_REFUSED_NOT_AUTHORIZED((byte)5), 
  
  CONNECTION_REFUSED_UNSPECIFIED_ERROR((byte)Byte.MIN_VALUE), 
  CONNECTION_REFUSED_MALFORMED_PACKET((byte)-127), 
  CONNECTION_REFUSED_PROTOCOL_ERROR((byte)-126), 
  CONNECTION_REFUSED_IMPLEMENTATION_SPECIFIC((byte)-125), 
  CONNECTION_REFUSED_UNSUPPORTED_PROTOCOL_VERSION((byte)-124), 
  CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID((byte)-123), 
  CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD((byte)-122), 
  CONNECTION_REFUSED_NOT_AUTHORIZED_5((byte)-121), 
  CONNECTION_REFUSED_SERVER_UNAVAILABLE_5((byte)-120), 
  CONNECTION_REFUSED_SERVER_BUSY((byte)-119), 
  CONNECTION_REFUSED_BANNED((byte)-118), 
  CONNECTION_REFUSED_BAD_AUTHENTICATION_METHOD((byte)-116), 
  CONNECTION_REFUSED_TOPIC_NAME_INVALID((byte)-112), 
  CONNECTION_REFUSED_PACKET_TOO_LARGE((byte)-107), 
  CONNECTION_REFUSED_QUOTA_EXCEEDED((byte)-105), 
  CONNECTION_REFUSED_PAYLOAD_FORMAT_INVALID((byte)-103), 
  CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED((byte)-102), 
  CONNECTION_REFUSED_QOS_NOT_SUPPORTED((byte)-101), 
  CONNECTION_REFUSED_USE_ANOTHER_SERVER((byte)-100), 
  CONNECTION_REFUSED_SERVER_MOVED((byte)-99), 
  CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED((byte)-97);
  
  private static final MqttConnectReturnCode[] VALUES;
  private final byte byteValue;
  
  static { MqttConnectReturnCode[] values = values();
    VALUES = new MqttConnectReturnCode[' '];
    for (MqttConnectReturnCode code : values) {
      int unsignedByte = byteValue & 0xFF;
      
      VALUES[unsignedByte] = code;
    }
  }
  

  private MqttConnectReturnCode(byte byteValue)
  {
    this.byteValue = byteValue;
  }
  
  public byte byteValue() {
    return byteValue;
  }
  
  public static MqttConnectReturnCode valueOf(byte b) {
    int unsignedByte = b & 0xFF;
    MqttConnectReturnCode mqttConnectReturnCode = null;
    try {
      mqttConnectReturnCode = VALUES[unsignedByte];
    }
    catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {}
    
    if (mqttConnectReturnCode == null) {
      throw new IllegalArgumentException("unknown connect return code: " + unsignedByte);
    }
    return mqttConnectReturnCode;
  }
}
