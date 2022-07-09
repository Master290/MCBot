package io.netty.handler.codec.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.List;


































public final class MqttDecoder
  extends ReplayingDecoder<DecoderState>
{
  private MqttFixedHeader mqttFixedHeader;
  private Object variableHeader;
  private int bytesRemainingInVariablePart;
  private final int maxBytesInMessage;
  private final int maxClientIdLength;
  
  static enum DecoderState
  {
    READ_FIXED_HEADER, 
    READ_VARIABLE_HEADER, 
    READ_PAYLOAD, 
    BAD_MESSAGE;
    


    private DecoderState() {}
  }
  


  public MqttDecoder()
  {
    this(8092, 23);
  }
  
  public MqttDecoder(int maxBytesInMessage) {
    this(maxBytesInMessage, 23);
  }
  
  public MqttDecoder(int maxBytesInMessage, int maxClientIdLength) {
    super(DecoderState.READ_FIXED_HEADER);
    this.maxBytesInMessage = ObjectUtil.checkPositive(maxBytesInMessage, "maxBytesInMessage");
    this.maxClientIdLength = ObjectUtil.checkPositive(maxClientIdLength, "maxClientIdLength");
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttDecoder$DecoderState[((DecoderState)state()).ordinal()]) {
    case 1:  try {
        mqttFixedHeader = decodeFixedHeader(ctx, buffer);
        bytesRemainingInVariablePart = mqttFixedHeader.remainingLength();
        checkpoint(DecoderState.READ_VARIABLE_HEADER);
      }
      catch (Exception cause) {
        out.add(invalidMessage(cause));
        return;
      }
    case 2: 
      try {
        Result<?> decodedVariableHeader = decodeVariableHeader(ctx, buffer, mqttFixedHeader);
        variableHeader = value;
        if (bytesRemainingInVariablePart > maxBytesInMessage) {
          buffer.skipBytes(actualReadableBytes());
          throw new TooLongFrameException("too large message: " + bytesRemainingInVariablePart + " bytes");
        }
        bytesRemainingInVariablePart -= numberOfBytesConsumed;
        checkpoint(DecoderState.READ_PAYLOAD);
      }
      catch (Exception cause) {
        out.add(invalidMessage(cause));
        return;
      }
    case 3: 
      try
      {
        Result<?> decodedPayload = decodePayload(ctx, buffer, mqttFixedHeader
        

          .messageType(), bytesRemainingInVariablePart, maxClientIdLength, variableHeader);
        


        bytesRemainingInVariablePart -= numberOfBytesConsumed;
        if (bytesRemainingInVariablePart != 0)
        {

          throw new DecoderException("non-zero remaining payload bytes: " + bytesRemainingInVariablePart + " (" + mqttFixedHeader.messageType() + ')');
        }
        checkpoint(DecoderState.READ_FIXED_HEADER);
        MqttMessage message = MqttMessageFactory.newMessage(mqttFixedHeader, variableHeader, 
          value);
        mqttFixedHeader = null;
        variableHeader = null;
        out.add(message);
      }
      catch (Exception cause) {
        out.add(invalidMessage(cause));
        return;
      }
    

    case 4: 
      buffer.skipBytes(actualReadableBytes());
      break;
    }
    
    
    throw new Error();
  }
  
  private MqttMessage invalidMessage(Throwable cause)
  {
    checkpoint(DecoderState.BAD_MESSAGE);
    return MqttMessageFactory.newInvalidMessage(mqttFixedHeader, variableHeader, cause);
  }
  









  private static MqttFixedHeader decodeFixedHeader(ChannelHandlerContext ctx, ByteBuf buffer)
  {
    short b1 = buffer.readUnsignedByte();
    
    MqttMessageType messageType = MqttMessageType.valueOf(b1 >> 4);
    boolean dupFlag = (b1 & 0x8) == 8;
    int qosLevel = (b1 & 0x6) >> 1;
    boolean retain = (b1 & 0x1) != 0;
    
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttMessageType[messageType.ordinal()]) {
    case 1: 
      if (qosLevel == 3) {
        throw new DecoderException("Illegal QOS Level in fixed header of PUBLISH message (" + qosLevel + ')');
      }
      

      break;
    case 2: 
    case 3: 
    case 4: 
      if (dupFlag) {
        throw new DecoderException("Illegal BIT 3 in fixed header of " + messageType + " message, must be 0, found 1");
      }
      
      if (qosLevel != 1) {
        throw new DecoderException("Illegal QOS Level in fixed header of " + messageType + " message, must be 1, found " + qosLevel);
      }
      
      if (retain) {
        throw new DecoderException("Illegal BIT 0 in fixed header of " + messageType + " message, must be 0, found 1");
      }
      

      break;
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 9: 
    case 10: 
    case 11: 
    case 12: 
    case 13: 
    case 14: 
    case 15: 
      if (dupFlag) {
        throw new DecoderException("Illegal BIT 3 in fixed header of " + messageType + " message, must be 0, found 1");
      }
      
      if (qosLevel != 0) {
        throw new DecoderException("Illegal BIT 2 or 1 in fixed header of " + messageType + " message, must be 0, found " + qosLevel);
      }
      
      if (retain) {
        throw new DecoderException("Illegal BIT 0 in fixed header of " + messageType + " message, must be 0, found 1");
      }
      
      break;
    default: 
      throw new DecoderException("Unknown message type, do not know how to validate fixed header");
    }
    
    int remainingLength = 0;
    int multiplier = 1;
    
    int loops = 0;
    short digit;
    do { digit = buffer.readUnsignedByte();
      remainingLength += (digit & 0x7F) * multiplier;
      multiplier *= 128;
      loops++;
    } while (((digit & 0x80) != 0) && (loops < 4));
    

    if ((loops == 4) && ((digit & 0x80) != 0)) {
      throw new DecoderException("remaining length exceeds 4 digits (" + messageType + ')');
    }
    
    MqttFixedHeader decodedFixedHeader = new MqttFixedHeader(messageType, dupFlag, MqttQoS.valueOf(qosLevel), retain, remainingLength);
    return MqttCodecUtil.validateFixedHeader(ctx, MqttCodecUtil.resetUnusedFields(decodedFixedHeader));
  }
  





  private Result<?> decodeVariableHeader(ChannelHandlerContext ctx, ByteBuf buffer, MqttFixedHeader mqttFixedHeader)
  {
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttMessageType[mqttFixedHeader.messageType().ordinal()]) {
    case 7: 
      return decodeConnectionVariableHeader(ctx, buffer);
    
    case 6: 
      return decodeConnAckVariableHeader(ctx, buffer);
    
    case 3: 
    case 4: 
    case 14: 
    case 15: 
      return decodeMessageIdAndPropertiesVariableHeader(ctx, buffer);
    
    case 2: 
    case 11: 
    case 12: 
    case 13: 
      return decodePubReplyMessage(buffer);
    
    case 1: 
      return decodePublishVariableHeader(ctx, buffer, mqttFixedHeader);
    
    case 5: 
    case 8: 
      return decodeReasonCodeAndPropertiesVariableHeader(buffer);
    

    case 9: 
    case 10: 
      return new Result(null, 0);
    }
    
    throw new DecoderException("Unknown message type: " + mqttFixedHeader.messageType());
  }
  


  private static Result<MqttConnectVariableHeader> decodeConnectionVariableHeader(ChannelHandlerContext ctx, ByteBuf buffer)
  {
    Result<String> protoString = decodeString(buffer);
    int numberOfBytesConsumed = numberOfBytesConsumed;
    
    byte protocolLevel = buffer.readByte();
    numberOfBytesConsumed++;
    
    MqttVersion version = MqttVersion.fromProtocolNameAndLevel((String)value, protocolLevel);
    MqttCodecUtil.setMqttVersion(ctx, version);
    
    int b1 = buffer.readUnsignedByte();
    numberOfBytesConsumed++;
    
    int keepAlive = decodeMsbLsb(buffer);
    numberOfBytesConsumed += 2;
    
    boolean hasUserName = (b1 & 0x80) == 128;
    boolean hasPassword = (b1 & 0x40) == 64;
    boolean willRetain = (b1 & 0x20) == 32;
    int willQos = (b1 & 0x18) >> 3;
    boolean willFlag = (b1 & 0x4) == 4;
    boolean cleanSession = (b1 & 0x2) == 2;
    if ((version == MqttVersion.MQTT_3_1_1) || (version == MqttVersion.MQTT_5)) {
      boolean zeroReservedFlag = (b1 & 0x1) == 0;
      if (!zeroReservedFlag)
      {


        throw new DecoderException("non-zero reserved flag");
      }
    }
    
    MqttProperties properties;
    if (version == MqttVersion.MQTT_5) {
      Result<MqttProperties> propertiesResult = decodeProperties(buffer);
      MqttProperties properties = (MqttProperties)value;
      numberOfBytesConsumed += numberOfBytesConsumed;
    } else {
      properties = MqttProperties.NO_PROPERTIES;
    }
    


    MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(version.protocolName(), version.protocolLevel(), hasUserName, hasPassword, willRetain, willQos, willFlag, cleanSession, keepAlive, properties);
    







    return new Result(mqttConnectVariableHeader, numberOfBytesConsumed);
  }
  

  private static Result<MqttConnAckVariableHeader> decodeConnAckVariableHeader(ChannelHandlerContext ctx, ByteBuf buffer)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    boolean sessionPresent = (buffer.readUnsignedByte() & 0x1) == 1;
    byte returnCode = buffer.readByte();
    int numberOfBytesConsumed = 2;
    
    MqttProperties properties;
    if (mqttVersion == MqttVersion.MQTT_5) {
      Result<MqttProperties> propertiesResult = decodeProperties(buffer);
      MqttProperties properties = (MqttProperties)value;
      numberOfBytesConsumed += numberOfBytesConsumed;
    } else {
      properties = MqttProperties.NO_PROPERTIES;
    }
    

    MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent, properties);
    return new Result(mqttConnAckVariableHeader, numberOfBytesConsumed);
  }
  

  private static Result<MqttMessageIdAndPropertiesVariableHeader> decodeMessageIdAndPropertiesVariableHeader(ChannelHandlerContext ctx, ByteBuf buffer)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    int packetId = decodeMessageId(buffer);
    
    int mqtt5Consumed;
    MqttMessageIdAndPropertiesVariableHeader mqttVariableHeader;
    int mqtt5Consumed;
    if (mqttVersion == MqttVersion.MQTT_5) {
      Result<MqttProperties> properties = decodeProperties(buffer);
      MqttMessageIdAndPropertiesVariableHeader mqttVariableHeader = new MqttMessageIdAndPropertiesVariableHeader(packetId, (MqttProperties)value);
      mqtt5Consumed = numberOfBytesConsumed;
    } else {
      mqttVariableHeader = new MqttMessageIdAndPropertiesVariableHeader(packetId, MqttProperties.NO_PROPERTIES);
      
      mqtt5Consumed = 0;
    }
    
    return new Result(mqttVariableHeader, 2 + mqtt5Consumed);
  }
  
  private Result<MqttPubReplyMessageVariableHeader> decodePubReplyMessage(ByteBuf buffer)
  {
    int packetId = decodeMessageId(buffer);
    


    int packetIdNumberOfBytesConsumed = 2;
    int consumed; MqttPubReplyMessageVariableHeader mqttPubAckVariableHeader; int consumed; if (bytesRemainingInVariablePart > 3) {
      byte reasonCode = buffer.readByte();
      Result<MqttProperties> properties = decodeProperties(buffer);
      

      MqttPubReplyMessageVariableHeader mqttPubAckVariableHeader = new MqttPubReplyMessageVariableHeader(packetId, reasonCode, (MqttProperties)value);
      consumed = 3 + numberOfBytesConsumed; } else { int consumed;
      if (bytesRemainingInVariablePart > 2) {
        byte reasonCode = buffer.readByte();
        MqttPubReplyMessageVariableHeader mqttPubAckVariableHeader = new MqttPubReplyMessageVariableHeader(packetId, reasonCode, MqttProperties.NO_PROPERTIES);
        

        consumed = 3;
      } else {
        mqttPubAckVariableHeader = new MqttPubReplyMessageVariableHeader(packetId, (byte)0, MqttProperties.NO_PROPERTIES);
        

        consumed = 2;
      }
    }
    return new Result(mqttPubAckVariableHeader, consumed);
  }
  
  private Result<MqttReasonCodeAndPropertiesVariableHeader> decodeReasonCodeAndPropertiesVariableHeader(ByteBuf buffer) {
    int consumed;
    byte reasonCode;
    MqttProperties properties;
    int consumed;
    if (bytesRemainingInVariablePart > 1) {
      byte reasonCode = buffer.readByte();
      Result<MqttProperties> propertiesResult = decodeProperties(buffer);
      MqttProperties properties = (MqttProperties)value;
      consumed = 1 + numberOfBytesConsumed; } else { int consumed;
      if (bytesRemainingInVariablePart > 0) {
        byte reasonCode = buffer.readByte();
        MqttProperties properties = MqttProperties.NO_PROPERTIES;
        consumed = 1;
      } else {
        reasonCode = 0;
        properties = MqttProperties.NO_PROPERTIES;
        consumed = 0;
      } }
    MqttReasonCodeAndPropertiesVariableHeader mqttReasonAndPropsVariableHeader = new MqttReasonCodeAndPropertiesVariableHeader(reasonCode, properties);
    

    return new Result(mqttReasonAndPropsVariableHeader, consumed);
  }
  




  private Result<MqttPublishVariableHeader> decodePublishVariableHeader(ChannelHandlerContext ctx, ByteBuf buffer, MqttFixedHeader mqttFixedHeader)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    Result<String> decodedTopic = decodeString(buffer);
    if (!MqttCodecUtil.isValidPublishTopicName((String)value)) {
      throw new DecoderException("invalid publish topic name: " + (String)value + " (contains wildcards)");
    }
    int numberOfBytesConsumed = numberOfBytesConsumed;
    
    int messageId = -1;
    if (mqttFixedHeader.qosLevel().value() > 0) {
      messageId = decodeMessageId(buffer);
      numberOfBytesConsumed += 2;
    }
    
    MqttProperties properties;
    if (mqttVersion == MqttVersion.MQTT_5) {
      Result<MqttProperties> propertiesResult = decodeProperties(buffer);
      MqttProperties properties = (MqttProperties)value;
      numberOfBytesConsumed += numberOfBytesConsumed;
    } else {
      properties = MqttProperties.NO_PROPERTIES;
    }
    

    MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader((String)value, messageId, properties);
    return new Result(mqttPublishVariableHeader, numberOfBytesConsumed);
  }
  


  private static int decodeMessageId(ByteBuf buffer)
  {
    int messageId = decodeMsbLsb(buffer);
    if (!MqttCodecUtil.isValidMessageId(messageId)) {
      throw new DecoderException("invalid messageId: " + messageId);
    }
    return messageId;
  }
  














  private static Result<?> decodePayload(ChannelHandlerContext ctx, ByteBuf buffer, MqttMessageType messageType, int bytesRemainingInVariablePart, int maxClientIdLength, Object variableHeader)
  {
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttMessageType[messageType.ordinal()]) {
    case 7: 
      return decodeConnectionPayload(buffer, maxClientIdLength, (MqttConnectVariableHeader)variableHeader);
    
    case 3: 
      return decodeSubscribePayload(buffer, bytesRemainingInVariablePart);
    
    case 14: 
      return decodeSubackPayload(buffer, bytesRemainingInVariablePart);
    
    case 4: 
      return decodeUnsubscribePayload(buffer, bytesRemainingInVariablePart);
    
    case 15: 
      return decodeUnsubAckPayload(ctx, buffer, bytesRemainingInVariablePart);
    
    case 1: 
      return decodePublishPayload(buffer, bytesRemainingInVariablePart);
    }
    
    
    return new Result(null, 0);
  }
  



  private static Result<MqttConnectPayload> decodeConnectionPayload(ByteBuf buffer, int maxClientIdLength, MqttConnectVariableHeader mqttConnectVariableHeader)
  {
    Result<String> decodedClientId = decodeString(buffer);
    String decodedClientIdValue = (String)value;
    MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(mqttConnectVariableHeader.name(), 
      (byte)mqttConnectVariableHeader.version());
    if (!MqttCodecUtil.isValidClientId(mqttVersion, maxClientIdLength, decodedClientIdValue)) {
      throw new MqttIdentifierRejectedException("invalid clientIdentifier: " + decodedClientIdValue);
    }
    int numberOfBytesConsumed = numberOfBytesConsumed;
    
    Result<String> decodedWillTopic = null;
    byte[] decodedWillMessage = null;
    
    MqttProperties willProperties;
    if (mqttConnectVariableHeader.isWillFlag()) { MqttProperties willProperties;
      if (mqttVersion == MqttVersion.MQTT_5) {
        Result<MqttProperties> propertiesResult = decodeProperties(buffer);
        MqttProperties willProperties = (MqttProperties)value;
        numberOfBytesConsumed += numberOfBytesConsumed;
      } else {
        willProperties = MqttProperties.NO_PROPERTIES;
      }
      decodedWillTopic = decodeString(buffer, 0, 32767);
      numberOfBytesConsumed += numberOfBytesConsumed;
      decodedWillMessage = decodeByteArray(buffer);
      numberOfBytesConsumed += decodedWillMessage.length + 2;
    } else {
      willProperties = MqttProperties.NO_PROPERTIES;
    }
    Result<String> decodedUserName = null;
    byte[] decodedPassword = null;
    if (mqttConnectVariableHeader.hasUserName()) {
      decodedUserName = decodeString(buffer);
      numberOfBytesConsumed += numberOfBytesConsumed;
    }
    if (mqttConnectVariableHeader.hasPassword()) {
      decodedPassword = decodeByteArray(buffer);
      numberOfBytesConsumed += decodedPassword.length + 2;
    }
    






    MqttConnectPayload mqttConnectPayload = new MqttConnectPayload((String)value, willProperties, decodedWillTopic != null ? (String)value : null, decodedWillMessage, decodedUserName != null ? (String)value : null, decodedPassword);
    
    return new Result(mqttConnectPayload, numberOfBytesConsumed);
  }
  

  private static Result<MqttSubscribePayload> decodeSubscribePayload(ByteBuf buffer, int bytesRemainingInVariablePart)
  {
    List<MqttTopicSubscription> subscribeTopics = new ArrayList();
    int numberOfBytesConsumed = 0;
    while (numberOfBytesConsumed < bytesRemainingInVariablePart) {
      Result<String> decodedTopicName = decodeString(buffer);
      numberOfBytesConsumed += numberOfBytesConsumed;
      
      short optionByte = buffer.readUnsignedByte();
      
      MqttQoS qos = MqttQoS.valueOf(optionByte & 0x3);
      boolean noLocal = (optionByte & 0x4) >> 2 == 1;
      boolean retainAsPublished = (optionByte & 0x8) >> 3 == 1;
      MqttSubscriptionOption.RetainedHandlingPolicy retainHandling = MqttSubscriptionOption.RetainedHandlingPolicy.valueOf((optionByte & 0x30) >> 4);
      
      MqttSubscriptionOption subscriptionOption = new MqttSubscriptionOption(qos, noLocal, retainAsPublished, retainHandling);
      



      numberOfBytesConsumed++;
      subscribeTopics.add(new MqttTopicSubscription((String)value, subscriptionOption));
    }
    return new Result(new MqttSubscribePayload(subscribeTopics), numberOfBytesConsumed);
  }
  

  private static Result<MqttSubAckPayload> decodeSubackPayload(ByteBuf buffer, int bytesRemainingInVariablePart)
  {
    List<Integer> grantedQos = new ArrayList(bytesRemainingInVariablePart);
    int numberOfBytesConsumed = 0;
    while (numberOfBytesConsumed < bytesRemainingInVariablePart) {
      int reasonCode = buffer.readUnsignedByte();
      numberOfBytesConsumed++;
      grantedQos.add(Integer.valueOf(reasonCode));
    }
    return new Result(new MqttSubAckPayload(grantedQos), numberOfBytesConsumed);
  }
  


  private static Result<MqttUnsubAckPayload> decodeUnsubAckPayload(ChannelHandlerContext ctx, ByteBuf buffer, int bytesRemainingInVariablePart)
  {
    List<Short> reasonCodes = new ArrayList(bytesRemainingInVariablePart);
    int numberOfBytesConsumed = 0;
    while (numberOfBytesConsumed < bytesRemainingInVariablePart) {
      short reasonCode = buffer.readUnsignedByte();
      numberOfBytesConsumed++;
      reasonCodes.add(Short.valueOf(reasonCode));
    }
    return new Result(new MqttUnsubAckPayload(reasonCodes), numberOfBytesConsumed);
  }
  

  private static Result<MqttUnsubscribePayload> decodeUnsubscribePayload(ByteBuf buffer, int bytesRemainingInVariablePart)
  {
    List<String> unsubscribeTopics = new ArrayList();
    int numberOfBytesConsumed = 0;
    while (numberOfBytesConsumed < bytesRemainingInVariablePart) {
      Result<String> decodedTopicName = decodeString(buffer);
      numberOfBytesConsumed += numberOfBytesConsumed;
      unsubscribeTopics.add(value);
    }
    return new Result(new MqttUnsubscribePayload(unsubscribeTopics), numberOfBytesConsumed);
  }
  

  private static Result<ByteBuf> decodePublishPayload(ByteBuf buffer, int bytesRemainingInVariablePart)
  {
    ByteBuf b = buffer.readRetainedSlice(bytesRemainingInVariablePart);
    return new Result(b, bytesRemainingInVariablePart);
  }
  
  private static Result<String> decodeString(ByteBuf buffer) {
    return decodeString(buffer, 0, Integer.MAX_VALUE);
  }
  
  private static Result<String> decodeString(ByteBuf buffer, int minBytes, int maxBytes) {
    int size = decodeMsbLsb(buffer);
    int numberOfBytesConsumed = 2;
    if ((size < minBytes) || (size > maxBytes)) {
      buffer.skipBytes(size);
      numberOfBytesConsumed += size;
      return new Result(null, numberOfBytesConsumed);
    }
    String s = buffer.toString(buffer.readerIndex(), size, CharsetUtil.UTF_8);
    buffer.skipBytes(size);
    numberOfBytesConsumed += size;
    return new Result(s, numberOfBytesConsumed);
  }
  



  private static byte[] decodeByteArray(ByteBuf buffer)
  {
    int size = decodeMsbLsb(buffer);
    byte[] bytes = new byte[size];
    buffer.readBytes(bytes);
    return bytes;
  }
  
  private static long packInts(int a, int b)
  {
    return a << 32 | b & 0xFFFFFFFF;
  }
  
  private static int unpackA(long ints) {
    return (int)(ints >> 32);
  }
  
  private static int unpackB(long ints) {
    return (int)ints;
  }
  


  private static int decodeMsbLsb(ByteBuf buffer)
  {
    int min = 0;
    int max = 65535;
    short msbSize = buffer.readUnsignedByte();
    short lsbSize = buffer.readUnsignedByte();
    int result = msbSize << 8 | lsbSize;
    if ((result < min) || (result > max)) {
      result = -1;
    }
    return result;
  }
  






  private static long decodeVariableByteInteger(ByteBuf buffer)
  {
    int remainingLength = 0;
    int multiplier = 1;
    
    int loops = 0;
    short digit;
    do { digit = buffer.readUnsignedByte();
      remainingLength += (digit & 0x7F) * multiplier;
      multiplier *= 128;
      loops++;
    } while (((digit & 0x80) != 0) && (loops < 4));
    
    if ((loops == 4) && ((digit & 0x80) != 0)) {
      throw new DecoderException("MQTT protocol limits Remaining Length to 4 bytes");
    }
    return packInts(remainingLength, loops);
  }
  
  private static final class Result<T>
  {
    private final T value;
    private final int numberOfBytesConsumed;
    
    Result(T value, int numberOfBytesConsumed) {
      this.value = value;
      this.numberOfBytesConsumed = numberOfBytesConsumed;
    }
  }
  
  private static Result<MqttProperties> decodeProperties(ByteBuf buffer) {
    long propertiesLength = decodeVariableByteInteger(buffer);
    int totalPropertiesLength = unpackA(propertiesLength);
    int numberOfBytesConsumed = unpackB(propertiesLength);
    
    MqttProperties decodedProperties = new MqttProperties();
    while (numberOfBytesConsumed < totalPropertiesLength) {
      long propertyId = decodeVariableByteInteger(buffer);
      int propertyIdValue = unpackA(propertyId);
      numberOfBytesConsumed += unpackB(propertyId);
      MqttProperties.MqttPropertyType propertyType = MqttProperties.MqttPropertyType.valueOf(propertyIdValue);
      switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttProperties$MqttPropertyType[propertyType.ordinal()]) {
      case 1: 
      case 2: 
      case 3: 
      case 4: 
      case 5: 
      case 6: 
      case 7: 
      case 8: 
        int b1 = buffer.readUnsignedByte();
        numberOfBytesConsumed++;
        decodedProperties.add(new MqttProperties.IntegerProperty(propertyIdValue, Integer.valueOf(b1)));
        break;
      case 9: 
      case 10: 
      case 11: 
      case 12: 
        int int2BytesResult = decodeMsbLsb(buffer);
        numberOfBytesConsumed += 2;
        decodedProperties.add(new MqttProperties.IntegerProperty(propertyIdValue, Integer.valueOf(int2BytesResult)));
        break;
      case 13: 
      case 14: 
      case 15: 
      case 16: 
        int maxPacketSize = buffer.readInt();
        numberOfBytesConsumed += 4;
        decodedProperties.add(new MqttProperties.IntegerProperty(propertyIdValue, Integer.valueOf(maxPacketSize)));
        break;
      case 17: 
        long vbIntegerResult = decodeVariableByteInteger(buffer);
        numberOfBytesConsumed += unpackB(vbIntegerResult);
        decodedProperties.add(new MqttProperties.IntegerProperty(propertyIdValue, Integer.valueOf(unpackA(vbIntegerResult))));
        break;
      case 18: 
      case 19: 
      case 20: 
      case 21: 
      case 22: 
      case 23: 
      case 24: 
        Result<String> stringResult = decodeString(buffer);
        numberOfBytesConsumed += numberOfBytesConsumed;
        decodedProperties.add(new MqttProperties.StringProperty(propertyIdValue, (String)value));
        break;
      case 25: 
        Result<String> keyResult = decodeString(buffer);
        Result<String> valueResult = decodeString(buffer);
        numberOfBytesConsumed += numberOfBytesConsumed;
        numberOfBytesConsumed += numberOfBytesConsumed;
        decodedProperties.add(new MqttProperties.UserProperty((String)value, (String)value));
        break;
      case 26: 
      case 27: 
        byte[] binaryDataResult = decodeByteArray(buffer);
        numberOfBytesConsumed += binaryDataResult.length + 2;
        decodedProperties.add(new MqttProperties.BinaryProperty(propertyIdValue, binaryDataResult));
        break;
      
      default: 
        throw new DecoderException("Unknown property type: " + propertyType);
      }
      
    }
    return new Result(decodedProperties, numberOfBytesConsumed);
  }
}
