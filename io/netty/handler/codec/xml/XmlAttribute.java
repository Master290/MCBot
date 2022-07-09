package io.netty.handler.codec.xml;




public class XmlAttribute
{
  private final String type;
  


  private final String name;
  


  private final String prefix;
  


  private final String namespace;
  


  private final String value;
  



  public XmlAttribute(String type, String name, String prefix, String namespace, String value)
  {
    this.type = type;
    this.name = name;
    this.prefix = prefix;
    this.namespace = namespace;
    this.value = value;
  }
  
  public String type() {
    return type;
  }
  
  public String name() {
    return name;
  }
  
  public String prefix() {
    return prefix;
  }
  
  public String namespace() {
    return namespace;
  }
  
  public String value() {
    return value;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    
    XmlAttribute that = (XmlAttribute)o;
    
    if (!name.equals(name)) {
      return false;
    }
    if (namespace != null ? !namespace.equals(namespace) : namespace != null) {
      return false;
    }
    if (prefix != null ? !prefix.equals(prefix) : prefix != null) {
      return false;
    }
    if (type != null ? !type.equals(type) : type != null) {
      return false;
    }
    if (value != null ? !value.equals(value) : value != null) {
      return false;
    }
    
    return true;
  }
  
  public int hashCode()
  {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + name.hashCode();
    result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
    result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
  
  public String toString()
  {
    return "XmlAttribute{type='" + type + '\'' + ", name='" + name + '\'' + ", prefix='" + prefix + '\'' + ", namespace='" + namespace + '\'' + ", value='" + value + '\'' + '}';
  }
}
