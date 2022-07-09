package io.netty.handler.codec.xml;

import java.util.LinkedList;
import java.util.List;


















public class XmlElementStart
  extends XmlElement
{
  private final List<XmlAttribute> attributes = new LinkedList();
  
  public XmlElementStart(String name, String namespace, String prefix) {
    super(name, namespace, prefix);
  }
  
  public List<XmlAttribute> attributes() {
    return attributes;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    
    XmlElementStart that = (XmlElementStart)o;
    
    return attributes.equals(attributes);
  }
  
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + attributes.hashCode();
    return result;
  }
  
  public String toString()
  {
    return 
    
      "XmlElementStart{attributes=" + attributes + super.toString() + "} ";
  }
}
