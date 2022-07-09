package io.netty.handler.codec.xml;










public class XmlDTD
{
  private final String text;
  








  public XmlDTD(String text)
  {
    this.text = text;
  }
  
  public String text() {
    return text;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    
    XmlDTD xmlDTD = (XmlDTD)o;
    
    if (text != null ? !text.equals(text) : text != null) {
      return false;
    }
    
    return true;
  }
  
  public int hashCode()
  {
    return text != null ? text.hashCode() : 0;
  }
  
  public String toString()
  {
    return "XmlDTD{text='" + text + '\'' + '}';
  }
}
