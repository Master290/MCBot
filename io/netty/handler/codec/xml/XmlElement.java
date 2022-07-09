package io.netty.handler.codec.xml;

import java.util.LinkedList;
import java.util.List;





















public abstract class XmlElement
{
  private final String name;
  private final String namespace;
  private final String prefix;
  private final List<XmlNamespace> namespaces = new LinkedList();
  
  protected XmlElement(String name, String namespace, String prefix) {
    this.name = name;
    this.namespace = namespace;
    this.prefix = prefix;
  }
  
  public String name() {
    return name;
  }
  
  public String namespace() {
    return namespace;
  }
  
  public String prefix() {
    return prefix;
  }
  
  public List<XmlNamespace> namespaces() {
    return namespaces;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    
    XmlElement that = (XmlElement)o;
    
    if (!name.equals(name)) {
      return false;
    }
    if (namespace != null ? !namespace.equals(namespace) : namespace != null) {
      return false;
    }
    if (!namespaces.equals(namespaces)) {
      return false;
    }
    if (prefix != null ? !prefix.equals(prefix) : prefix != null) {
      return false;
    }
    
    return true;
  }
  
  public int hashCode()
  {
    int result = name.hashCode();
    result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
    result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
    result = 31 * result + namespaces.hashCode();
    return result;
  }
  
  public String toString()
  {
    return ", name='" + name + '\'' + ", namespace='" + namespace + '\'' + ", prefix='" + prefix + '\'' + ", namespaces=" + namespaces;
  }
}
