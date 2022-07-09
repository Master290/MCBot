package io.netty.handler.codec.mqtt;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;





















public final class MqttUnsubAckPayload
{
  private final List<Short> unsubscribeReasonCodes;
  private static final MqttUnsubAckPayload EMPTY = new MqttUnsubAckPayload(new short[0]);
  
  public static MqttUnsubAckPayload withEmptyDefaults(MqttUnsubAckPayload payload) {
    if (payload == null) {
      return EMPTY;
    }
    return payload;
  }
  
  public MqttUnsubAckPayload(short... unsubscribeReasonCodes)
  {
    ObjectUtil.checkNotNull(unsubscribeReasonCodes, "unsubscribeReasonCodes");
    
    List<Short> list = new ArrayList(unsubscribeReasonCodes.length);
    short[] arrayOfShort = unsubscribeReasonCodes;int i = arrayOfShort.length; for (int j = 0; j < i; j++) { Short v = Short.valueOf(arrayOfShort[j]);
      list.add(v);
    }
    this.unsubscribeReasonCodes = Collections.unmodifiableList(list);
  }
  
  public MqttUnsubAckPayload(Iterable<Short> unsubscribeReasonCodes) {
    ObjectUtil.checkNotNull(unsubscribeReasonCodes, "unsubscribeReasonCodes");
    
    List<Short> list = new ArrayList();
    for (Short v : unsubscribeReasonCodes) {
      ObjectUtil.checkNotNull(v, "unsubscribeReasonCode");
      list.add(v);
    }
    this.unsubscribeReasonCodes = Collections.unmodifiableList(list);
  }
  
  public List<Short> unsubscribeReasonCodes() {
    return unsubscribeReasonCodes;
  }
  
  public String toString()
  {
    return 
    

      StringUtil.simpleClassName(this) + '[' + "unsubscribeReasonCodes=" + unsubscribeReasonCodes + ']';
  }
}
