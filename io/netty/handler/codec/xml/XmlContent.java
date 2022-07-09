package io.netty.handler.codec.xml;










public abstract class XmlContent
{
  private final String data;
  








  protected XmlContent(String data)
  {
    this.data = data;
  }
  
  public String data() {
    return data;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    
    XmlContent that = (XmlContent)o;
    
    if (data != null ? !data.equals(data) : data != null) {
      return false;
    }
    
    return true;
  }
  
  public int hashCode()
  {
    return data != null ? data.hashCode() : 0;
  }
  
  public String toString()
  {
    return "XmlContent{data='" + data + '\'' + '}';
  }
}
