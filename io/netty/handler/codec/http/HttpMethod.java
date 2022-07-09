package io.netty.handler.codec.http;

import io.netty.util.AsciiString;
import io.netty.util.internal.MathUtil;
import io.netty.util.internal.ObjectUtil;



























public class HttpMethod
  implements Comparable<HttpMethod>
{
  public static final HttpMethod OPTIONS = new HttpMethod("OPTIONS");
  






  public static final HttpMethod GET = new HttpMethod("GET");
  




  public static final HttpMethod HEAD = new HttpMethod("HEAD");
  





  public static final HttpMethod POST = new HttpMethod("POST");
  



  public static final HttpMethod PUT = new HttpMethod("PUT");
  




  public static final HttpMethod PATCH = new HttpMethod("PATCH");
  




  public static final HttpMethod DELETE = new HttpMethod("DELETE");
  




  public static final HttpMethod TRACE = new HttpMethod("TRACE");
  




  public static final HttpMethod CONNECT = new HttpMethod("CONNECT");
  



  private static final EnumNameMap<HttpMethod> methodMap = new EnumNameMap(new HttpMethod.EnumNameMap.Node[] { new HttpMethod.EnumNameMap.Node(OPTIONS
    .toString(), OPTIONS), new HttpMethod.EnumNameMap.Node(GET
    .toString(), GET), new HttpMethod.EnumNameMap.Node(HEAD
    .toString(), HEAD), new HttpMethod.EnumNameMap.Node(POST
    .toString(), POST), new HttpMethod.EnumNameMap.Node(PUT
    .toString(), PUT), new HttpMethod.EnumNameMap.Node(PATCH
    .toString(), PATCH), new HttpMethod.EnumNameMap.Node(DELETE
    .toString(), DELETE), new HttpMethod.EnumNameMap.Node(TRACE
    .toString(), TRACE), new HttpMethod.EnumNameMap.Node(CONNECT
    .toString(), CONNECT) });
  

  private final AsciiString name;
  


  public static HttpMethod valueOf(String name)
  {
    HttpMethod result = (HttpMethod)methodMap.get(name);
    return result != null ? result : new HttpMethod(name);
  }
  








  public HttpMethod(String name)
  {
    name = ObjectUtil.checkNonEmptyAfterTrim(name, "name");
    
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if ((Character.isISOControl(c)) || (Character.isWhitespace(c))) {
        throw new IllegalArgumentException("invalid character in name");
      }
    }
    
    this.name = AsciiString.cached(name);
  }
  


  public String name()
  {
    return name.toString();
  }
  


  public AsciiString asciiName()
  {
    return name;
  }
  
  public int hashCode()
  {
    return name().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HttpMethod)) {
      return false;
    }
    
    HttpMethod that = (HttpMethod)o;
    return name().equals(that.name());
  }
  
  public String toString()
  {
    return name.toString();
  }
  
  public int compareTo(HttpMethod o)
  {
    if (o == this) {
      return 0;
    }
    return name().compareTo(o.name());
  }
  
  private static final class EnumNameMap<T> {
    private final Node<T>[] values;
    private final int valuesMask;
    
    EnumNameMap(Node<T>... nodes) {
      values = ((Node[])new Node[MathUtil.findNextPositivePowerOfTwo(nodes.length)]);
      valuesMask = (values.length - 1);
      for (Node<T> node : nodes) {
        int i = hashCode(key) & valuesMask;
        if (values[i] != null) {
          throw new IllegalArgumentException("index " + i + " collision between values: [" + values[i].key + ", " + key + ']');
        }
        
        values[i] = node;
      }
    }
    
    T get(String name) {
      Node<T> node = values[(hashCode(name) & valuesMask)];
      return (node == null) || (!key.equals(name)) ? null : value;
    }
    




    private static int hashCode(String name)
    {
      return name.hashCode() >>> 6;
    }
    
    private static final class Node<T> {
      final String key;
      final T value;
      
      Node(String key, T value) {
        this.key = key;
        this.value = value;
      }
    }
  }
}
