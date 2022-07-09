package io.netty.handler.codec.xml;







public class XmlProcessingInstruction
{
  private final String data;
  





  private final String target;
  





  public XmlProcessingInstruction(String data, String target)
  {
    this.data = data;
    this.target = target;
  }
  
  public String data() {
    return data;
  }
  
  public String target() {
    return target;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    
    XmlProcessingInstruction that = (XmlProcessingInstruction)o;
    
    if (data != null ? !data.equals(data) : data != null) {
      return false;
    }
    if (target != null ? !target.equals(target) : target != null) {
      return false;
    }
    
    return true;
  }
  
  public int hashCode()
  {
    int result = data != null ? data.hashCode() : 0;
    result = 31 * result + (target != null ? target.hashCode() : 0);
    return result;
  }
  
  public String toString()
  {
    return "XmlProcessingInstruction{data='" + data + '\'' + ", target='" + target + '\'' + '}';
  }
}
