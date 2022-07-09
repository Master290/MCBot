package io.netty.handler.codec.mqtt;

import io.netty.util.collection.IntObjectHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;




















public final class MqttProperties
{
  public static enum MqttPropertyType
  {
    PAYLOAD_FORMAT_INDICATOR(1), 
    REQUEST_PROBLEM_INFORMATION(23), 
    REQUEST_RESPONSE_INFORMATION(25), 
    MAXIMUM_QOS(36), 
    RETAIN_AVAILABLE(37), 
    WILDCARD_SUBSCRIPTION_AVAILABLE(40), 
    SUBSCRIPTION_IDENTIFIER_AVAILABLE(41), 
    SHARED_SUBSCRIPTION_AVAILABLE(42), 
    

    SERVER_KEEP_ALIVE(19), 
    RECEIVE_MAXIMUM(33), 
    TOPIC_ALIAS_MAXIMUM(34), 
    TOPIC_ALIAS(35), 
    

    PUBLICATION_EXPIRY_INTERVAL(2), 
    SESSION_EXPIRY_INTERVAL(17), 
    WILL_DELAY_INTERVAL(24), 
    MAXIMUM_PACKET_SIZE(39), 
    

    SUBSCRIPTION_IDENTIFIER(11), 
    

    CONTENT_TYPE(3), 
    RESPONSE_TOPIC(8), 
    ASSIGNED_CLIENT_IDENTIFIER(18), 
    AUTHENTICATION_METHOD(21), 
    RESPONSE_INFORMATION(26), 
    SERVER_REFERENCE(28), 
    REASON_STRING(31), 
    USER_PROPERTY(38), 
    

    CORRELATION_DATA(9), 
    AUTHENTICATION_DATA(22);
    
    private static final MqttPropertyType[] VALUES;
    private final int value;
    
    static { VALUES = new MqttPropertyType[43];
      for (MqttPropertyType v : values()) {
        VALUES[value] = v;
      }
    }
    

    private MqttPropertyType(int value)
    {
      this.value = value;
    }
    
    public int value() {
      return value;
    }
    
    public static MqttPropertyType valueOf(int type) {
      MqttPropertyType t = null;
      try {
        t = VALUES[type];
      }
      catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {}
      
      if (t == null) {
        throw new IllegalArgumentException("unknown property type: " + type);
      }
      return t;
    }
  }
  
  public static final MqttProperties NO_PROPERTIES = new MqttProperties(false);
  private IntObjectHashMap<MqttProperty> props;
  
  static MqttProperties withEmptyDefaults(MqttProperties properties) { if (properties == null) {
      return NO_PROPERTIES;
    }
    return properties;
  }
  

  public static abstract class MqttProperty<T>
  {
    final T value;
    
    final int propertyId;
    

    protected MqttProperty(int propertyId, T value)
    {
      this.propertyId = propertyId;
      this.value = value;
    }
    




    public T value()
    {
      return value;
    }
    



    public int propertyId()
    {
      return propertyId;
    }
    
    public int hashCode()
    {
      return propertyId + 31 * value.hashCode();
    }
    
    public boolean equals(Object obj)
    {
      if (this == obj) {
        return true;
      }
      if ((obj == null) || (getClass() != obj.getClass())) {
        return false;
      }
      MqttProperty that = (MqttProperty)obj;
      return (propertyId == propertyId) && (value.equals(value));
    }
  }
  
  public static final class IntegerProperty extends MqttProperties.MqttProperty<Integer>
  {
    public IntegerProperty(int propertyId, Integer value) {
      super(value);
    }
    
    public String toString()
    {
      return "IntegerProperty(" + propertyId + ", " + value + ")";
    }
  }
  
  public static final class StringProperty extends MqttProperties.MqttProperty<String>
  {
    public StringProperty(int propertyId, String value) {
      super(value);
    }
    
    public String toString()
    {
      return "StringProperty(" + propertyId + ", " + (String)value + ")";
    }
  }
  
  public static final class StringPair {
    public final String key;
    public final String value;
    
    public StringPair(String key, String value) {
      this.key = key;
      this.value = value;
    }
    
    public int hashCode()
    {
      return key.hashCode() + 31 * value.hashCode();
    }
    
    public boolean equals(Object obj)
    {
      if (this == obj) {
        return true;
      }
      if ((obj == null) || (getClass() != obj.getClass())) {
        return false;
      }
      StringPair that = (StringPair)obj;
      
      return (key.equals(key)) && (value.equals(value));
    }
  }
  
  public static final class UserProperties extends MqttProperties.MqttProperty<List<MqttProperties.StringPair>>
  {
    public UserProperties()
    {
      super(new ArrayList());
    }
    




    public UserProperties(Collection<MqttProperties.StringPair> values)
    {
      this();
      ((List)value).addAll(values);
    }
    
    private static UserProperties fromUserPropertyCollection(Collection<MqttProperties.UserProperty> properties) {
      UserProperties userProperties = new UserProperties();
      for (MqttProperties.UserProperty property : properties) {
        userProperties.add(new MqttProperties.StringPair(value).key, value).value));
      }
      return userProperties;
    }
    
    public void add(MqttProperties.StringPair pair) {
      ((List)value).add(pair);
    }
    
    public void add(String key, String value) {
      ((List)this.value).add(new MqttProperties.StringPair(key, value));
    }
    
    public String toString()
    {
      StringBuilder builder = new StringBuilder("UserProperties(");
      boolean first = true;
      for (MqttProperties.StringPair pair : (List)value) {
        if (!first) {
          builder.append(", ");
        }
        builder.append(key + "->" + value);
        first = false;
      }
      builder.append(")");
      return builder.toString();
    }
  }
  
  public static final class UserProperty extends MqttProperties.MqttProperty<MqttProperties.StringPair> {
    public UserProperty(String key, String value) {
      super(new MqttProperties.StringPair(key, value));
    }
    
    public String toString()
    {
      return "UserProperty(" + value).key + ", " + value).value + ")";
    }
  }
  
  public static final class BinaryProperty extends MqttProperties.MqttProperty<byte[]>
  {
    public BinaryProperty(int propertyId, byte[] value) {
      super(value);
    }
    
    public String toString()
    {
      return "BinaryProperty(" + propertyId + ", " + ((byte[])value).length + " bytes)";
    }
  }
  
  public MqttProperties() {
    this(true);
  }
  
  private MqttProperties(boolean canModify) {
    this.canModify = canModify;
  }
  




  public void add(MqttProperty property)
  {
    if (!canModify) {
      throw new UnsupportedOperationException("adding property isn't allowed");
    }
    IntObjectHashMap<MqttProperty> props = this.props;
    if (propertyId == USER_PROPERTYvalue) {
      List<UserProperty> userProperties = this.userProperties;
      if (userProperties == null) {
        userProperties = new ArrayList(1);
        this.userProperties = userProperties;
      }
      if ((property instanceof UserProperty)) {
        userProperties.add((UserProperty)property);
      } else if ((property instanceof UserProperties)) {
        for (StringPair pair : (List)value) {
          userProperties.add(new UserProperty(key, value));
        }
      } else {
        throw new IllegalArgumentException("User property must be of UserProperty or UserProperties type");
      }
    } else if (propertyId == SUBSCRIPTION_IDENTIFIERvalue) {
      List<IntegerProperty> subscriptionIds = this.subscriptionIds;
      if (subscriptionIds == null) {
        subscriptionIds = new ArrayList(1);
        this.subscriptionIds = subscriptionIds;
      }
      if ((property instanceof IntegerProperty)) {
        subscriptionIds.add((IntegerProperty)property);
      } else {
        throw new IllegalArgumentException("Subscription ID must be an integer property");
      }
    } else {
      if (props == null) {
        props = new IntObjectHashMap();
        this.props = props;
      }
      props.put(propertyId, property);
    }
  }
  
  public Collection<? extends MqttProperty> listAll() {
    IntObjectHashMap<MqttProperty> props = this.props;
    if ((props == null) && (subscriptionIds == null) && (userProperties == null)) {
      return Collections.emptyList();
    }
    if ((subscriptionIds == null) && (userProperties == null)) {
      return props.values();
    }
    if ((props == null) && (userProperties == null)) {
      return subscriptionIds;
    }
    List<MqttProperty> propValues = new ArrayList(props != null ? props.size() : 1);
    if (props != null) {
      propValues.addAll(props.values());
    }
    if (subscriptionIds != null) {
      propValues.addAll(subscriptionIds);
    }
    if (userProperties != null) {
      propValues.add(UserProperties.fromUserPropertyCollection(userProperties));
    }
    return propValues;
  }
  
  public boolean isEmpty() {
    IntObjectHashMap<MqttProperty> props = this.props;
    return (props == null) || (props.isEmpty());
  }
  



  private List<UserProperty> userProperties;
  

  public MqttProperty getProperty(int propertyId)
  {
    if (propertyId == USER_PROPERTYvalue)
    {
      List<UserProperty> userProperties = this.userProperties;
      if (userProperties == null) {
        return null;
      }
      return UserProperties.fromUserPropertyCollection(userProperties);
    }
    if (propertyId == SUBSCRIPTION_IDENTIFIERvalue) {
      List<IntegerProperty> subscriptionIds = this.subscriptionIds;
      if ((subscriptionIds == null) || (subscriptionIds.isEmpty())) {
        return null;
      }
      return (MqttProperty)subscriptionIds.get(0);
    }
    IntObjectHashMap<MqttProperty> props = this.props;
    return props == null ? null : (MqttProperty)props.get(propertyId);
  }
  


  private List<IntegerProperty> subscriptionIds;
  

  private final boolean canModify;
  
  public List<? extends MqttProperty> getProperties(int propertyId)
  {
    if (propertyId == USER_PROPERTYvalue) {
      return userProperties == null ? Collections.emptyList() : userProperties;
    }
    if (propertyId == SUBSCRIPTION_IDENTIFIERvalue) {
      return subscriptionIds == null ? Collections.emptyList() : subscriptionIds;
    }
    IntObjectHashMap<MqttProperty> props = this.props;
    return (props == null) || (!props.containsKey(propertyId)) ? 
      Collections.emptyList() : 
      Collections.singletonList(props.get(propertyId));
  }
}
