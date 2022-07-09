package io.netty.handler.codec.xml;





public class XmlDocumentStart
{
  private final String encoding;
  



  private final String version;
  


  private final boolean standalone;
  


  private final String encodingScheme;
  



  public XmlDocumentStart(String encoding, String version, boolean standalone, String encodingScheme)
  {
    this.encoding = encoding;
    this.version = version;
    this.standalone = standalone;
    this.encodingScheme = encodingScheme;
  }
  
  public String encoding()
  {
    return encoding;
  }
  
  public String version()
  {
    return version;
  }
  
  public boolean standalone()
  {
    return standalone;
  }
  
  public String encodingScheme()
  {
    return encodingScheme;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    
    XmlDocumentStart that = (XmlDocumentStart)o;
    
    if (standalone != standalone) {
      return false;
    }
    if (encoding != null ? !encoding.equals(encoding) : encoding != null) {
      return false;
    }
    if (encodingScheme != null ? !encodingScheme.equals(encodingScheme) : encodingScheme != null) {
      return false;
    }
    if (version != null ? !version.equals(version) : version != null) {
      return false;
    }
    
    return true;
  }
  
  public int hashCode()
  {
    int result = encoding != null ? encoding.hashCode() : 0;
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (standalone ? 1 : 0);
    result = 31 * result + (encodingScheme != null ? encodingScheme.hashCode() : 0);
    return result;
  }
  
  public String toString()
  {
    return "XmlDocumentStart{encoding='" + encoding + '\'' + ", version='" + version + '\'' + ", standalone=" + standalone + ", encodingScheme='" + encodingScheme + '\'' + '}';
  }
}
