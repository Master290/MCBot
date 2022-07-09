package io.netty.util.internal;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;























public final class ResourcesUtil
{
  public static File getFile(Class resourceClass, String fileName)
  {
    try
    {
      return new File(URLDecoder.decode(resourceClass.getResource(fileName).getFile(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {}
    return new File(resourceClass.getResource(fileName).getFile());
  }
  
  private ResourcesUtil() {}
}
