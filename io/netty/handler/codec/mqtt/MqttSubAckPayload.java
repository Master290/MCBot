package io.netty.handler.codec.mqtt;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;



















public class MqttSubAckPayload
{
  private final List<Integer> reasonCodes;
  
  public MqttSubAckPayload(int... reasonCodes)
  {
    ObjectUtil.checkNotNull(reasonCodes, "reasonCodes");
    
    List<Integer> list = new ArrayList(reasonCodes.length);
    for (int v : reasonCodes) {
      list.add(Integer.valueOf(v));
    }
    this.reasonCodes = Collections.unmodifiableList(list);
  }
  
  public MqttSubAckPayload(Iterable<Integer> reasonCodes) {
    ObjectUtil.checkNotNull(reasonCodes, "reasonCodes");
    List<Integer> list = new ArrayList();
    for (Integer v : reasonCodes) {
      if (v == null) {
        break;
      }
      list.add(v);
    }
    this.reasonCodes = Collections.unmodifiableList(list);
  }
  
  public List<Integer> grantedQoSLevels() {
    List<Integer> qosLevels = new ArrayList(reasonCodes.size());
    for (Iterator localIterator = reasonCodes.iterator(); localIterator.hasNext();) { int code = ((Integer)localIterator.next()).intValue();
      if (code > MqttQoS.EXACTLY_ONCE.value()) {
        qosLevels.add(Integer.valueOf(MqttQoS.FAILURE.value()));
      } else {
        qosLevels.add(Integer.valueOf(code));
      }
    }
    return qosLevels;
  }
  
  public List<Integer> reasonCodes() {
    return reasonCodes;
  }
  
  public String toString()
  {
    return 
    

      StringUtil.simpleClassName(this) + '[' + "reasonCodes=" + reasonCodes + ']';
  }
}
