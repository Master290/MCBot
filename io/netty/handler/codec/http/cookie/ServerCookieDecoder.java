package io.netty.handler.codec.http.cookie;

import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


































public final class ServerCookieDecoder
  extends CookieDecoder
{
  private static final String RFC2965_VERSION = "$Version";
  private static final String RFC2965_PATH = "$Path";
  private static final String RFC2965_DOMAIN = "$Domain";
  private static final String RFC2965_PORT = "$Port";
  public static final ServerCookieDecoder STRICT = new ServerCookieDecoder(true);
  



  public static final ServerCookieDecoder LAX = new ServerCookieDecoder(false);
  
  private ServerCookieDecoder(boolean strict) {
    super(strict);
  }
  





  public List<Cookie> decodeAll(String header)
  {
    List<Cookie> cookies = new ArrayList();
    decode(cookies, header);
    return Collections.unmodifiableList(cookies);
  }
  




  public Set<Cookie> decode(String header)
  {
    Set<Cookie> cookies = new TreeSet();
    decode(cookies, header);
    return cookies;
  }
  


  private void decode(Collection<? super Cookie> cookies, String header)
  {
    int headerLen = ((String)ObjectUtil.checkNotNull(header, "header")).length();
    
    if (headerLen == 0) {
      return;
    }
    
    int i = 0;
    
    boolean rfc2965Style = false;
    if (header.regionMatches(true, 0, "$Version", 0, "$Version".length()))
    {
      i = header.indexOf(';') + 1;
      rfc2965Style = true;
    }
    




    while (i != headerLen)
    {

      char c = header.charAt(i);
      if ((c == '\t') || (c == '\n') || (c == '\013') || (c == '\f') || (c == '\r') || (c == ' ') || (c == ',') || (c == ';'))
      {
        i++;

      }
      else
      {

        int nameBegin = i;
        



        for (;;)
        {
          char curChar = header.charAt(i);
          if (curChar == ';')
          {
            int nameEnd = i;
            int valueEnd; int valueBegin = valueEnd = -1;
            break;
          }
          if (curChar == '=')
          {
            int nameEnd = i;
            i++;
            if (i == headerLen) {
              int valueEnd;
              int valueBegin = valueEnd = 0;
              break;
            }
            
            int valueBegin = i;
            
            int semiPos = header.indexOf(';', i);
            int valueEnd = i = semiPos > 0 ? semiPos : headerLen;
            break;
          }
          i++;
          

          if (i == headerLen)
          {
            int nameEnd = headerLen;
            int valueEnd; int valueBegin = valueEnd = -1;
            break; } }
        int valueEnd;
        int valueBegin;
        int nameEnd;
        if ((!rfc2965Style) || ((!header.regionMatches(nameBegin, "$Path", 0, "$Path".length())) && 
          (!header.regionMatches(nameBegin, "$Domain", 0, "$Domain".length())) && 
          (!header.regionMatches(nameBegin, "$Port", 0, "$Port".length()))))
        {




          DefaultCookie cookie = initCookie(header, nameBegin, nameEnd, valueBegin, valueEnd);
          if (cookie != null) {
            cookies.add(cookie);
          }
        }
      }
    }
  }
}
