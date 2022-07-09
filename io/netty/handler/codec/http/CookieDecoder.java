package io.netty.handler.codec.http;

import io.netty.handler.codec.DateFormatter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;






































@Deprecated
public final class CookieDecoder
{
  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());
  
  private static final CookieDecoder STRICT = new CookieDecoder(true);
  
  private static final CookieDecoder LAX = new CookieDecoder(false);
  
  private static final String COMMENT = "Comment";
  
  private static final String COMMENTURL = "CommentURL";
  
  private static final String DISCARD = "Discard";
  
  private static final String PORT = "Port";
  
  private static final String VERSION = "Version";
  private final boolean strict;
  
  public static Set<Cookie> decode(String header)
  {
    return decode(header, true);
  }
  
  public static Set<Cookie> decode(String header, boolean strict) {
    return (strict ? STRICT : LAX).doDecode(header);
  }
  




  private Set<Cookie> doDecode(String header)
  {
    List<String> names = new ArrayList(8);
    List<String> values = new ArrayList(8);
    extractKeyValuePairs(header, names, values);
    
    if (names.isEmpty()) {
      return Collections.emptySet();
    }
    

    int version = 0;
    
    int i;
    int i;
    if (((String)names.get(0)).equalsIgnoreCase("Version")) {
      try {
        version = Integer.parseInt((String)values.get(0));
      }
      catch (NumberFormatException localNumberFormatException) {}
      
      i = 1;
    } else {
      i = 0;
    }
    
    if (names.size() <= i)
    {
      return Collections.emptySet();
    }
    
    Set<Cookie> cookies = new TreeSet();
    for (; i < names.size(); i++) {
      String name = (String)names.get(i);
      String value = (String)values.get(i);
      if (value == null) {
        value = "";
      }
      
      Cookie c = initCookie(name, value);
      
      if (c == null) {
        break;
      }
      
      boolean discard = false;
      boolean secure = false;
      boolean httpOnly = false;
      String comment = null;
      String commentURL = null;
      String domain = null;
      String path = null;
      long maxAge = Long.MIN_VALUE;
      List<Integer> ports = new ArrayList(2);
      
      for (int j = i + 1; j < names.size(); i++) {
        name = (String)names.get(j);
        value = (String)values.get(j);
        
        if ("Discard".equalsIgnoreCase(name)) {
          discard = true;
        } else if ("Secure".equalsIgnoreCase(name)) {
          secure = true;
        } else if ("HTTPOnly".equalsIgnoreCase(name)) {
          httpOnly = true;
        } else if ("Comment".equalsIgnoreCase(name)) {
          comment = value;
        } else if ("CommentURL".equalsIgnoreCase(name)) {
          commentURL = value;
        } else if ("Domain".equalsIgnoreCase(name)) {
          domain = value;
        } else if ("Path".equalsIgnoreCase(name)) {
          path = value; } else { long maxAgeMillis;
          if ("Expires".equalsIgnoreCase(name)) {
            Date date = DateFormatter.parseHttpDate(value);
            if (date != null) {
              maxAgeMillis = date.getTime() - System.currentTimeMillis();
              maxAge = maxAgeMillis / 1000L + (maxAgeMillis % 1000L != 0L ? 1 : 0);
            }
          } else if ("Max-Age".equalsIgnoreCase(name)) {
            maxAge = Integer.parseInt(value);
          } else if ("Version".equalsIgnoreCase(name)) {
            version = Integer.parseInt(value);
          } else { if (!"Port".equalsIgnoreCase(name)) break;
            String[] portList = value.split(",");
            for (String s1 : portList) {
              try {
                ports.add(Integer.valueOf(s1));
              }
              catch (NumberFormatException localNumberFormatException1) {}
            }
          }
        }
        j++;
      }
      







































      c.setVersion(version);
      c.setMaxAge(maxAge);
      c.setPath(path);
      c.setDomain(domain);
      c.setSecure(secure);
      c.setHttpOnly(httpOnly);
      if (version > 0) {
        c.setComment(comment);
      }
      if (version > 1) {
        c.setCommentUrl(commentURL);
        c.setPorts(ports);
        c.setDiscard(discard);
      }
      
      cookies.add(c);
    }
    
    return cookies;
  }
  
  private static void extractKeyValuePairs(String header, List<String> names, List<String> values)
  {
    int headerLen = header.length();
    int i = 0;
    


    while (i != headerLen)
    {

      switch (header.charAt(i)) {
      case '\t': case '\n': case '\013': case '\f': 
      case '\r': case ' ': case ',': case ';': 
        i++;
        break;
      


      default: 
        for (;;)
        {
          if (i == headerLen) {
            return;
          }
          if (header.charAt(i) != '$') break;
          i++;
        }
        
        String value;
        
        String name;
        
        String value;
        
        if (i == headerLen) {
          String name = null;
          value = null;
        } else {
          int newNameStart = i;
          do { String value;
            switch (header.charAt(i))
            {
            case ';': 
              String name = header.substring(newNameStart, i);
              value = null;
              break;
            
            case '=': 
              String name = header.substring(newNameStart, i);
              i++;
              String value; if (i == headerLen)
              {
                value = "";
              }
              else
              {
                int newValueStart = i;
                char c = header.charAt(i);
                if ((c == '"') || (c == '\''))
                {
                  StringBuilder newValueBuf = new StringBuilder(header.length() - i);
                  char q = c;
                  boolean hadBackslash = false;
                  i++;
                  for (;;) {
                    if (i == headerLen) {
                      String value = newValueBuf.toString();
                      break;
                    }
                    if (hadBackslash) {
                      hadBackslash = false;
                      c = header.charAt(i++);
                      switch (c) {
                      case '"': case '\'': 
                      case '\\': 
                        newValueBuf.setCharAt(newValueBuf.length() - 1, c);
                        break;
                      
                      default: 
                        newValueBuf.append(c);break;
                      }
                    } else {
                      c = header.charAt(i++);
                      if (c == q) {
                        String value = newValueBuf.toString();
                        break;
                      }
                      newValueBuf.append(c);
                      if (c == '\\') {
                        hadBackslash = true;
                      }
                    }
                  }
                }
                
                int semiPos = header.indexOf(';', i);
                if (semiPos > 0) {
                  String value = header.substring(newValueStart, semiPos);
                  i = semiPos;
                } else {
                  String value = header.substring(newValueStart);
                  i = headerLen;
                }
              }
              break;
            default: 
              i++;
            }
            
          } while (i != headerLen);
          
          name = header.substring(newNameStart);
          value = null;
        }
        



        names.add(name);
        values.add(value);
      } }
  }
  
  private CookieDecoder(boolean strict) {
    this.strict = strict;
  }
  
  private DefaultCookie initCookie(String name, String value) {
    if ((name == null) || (name.length() == 0)) {
      logger.debug("Skipping cookie with null name");
      return null;
    }
    
    if (value == null) {
      logger.debug("Skipping cookie with null value");
      return null;
    }
    
    CharSequence unwrappedValue = CookieUtil.unwrapValue(value);
    if (unwrappedValue == null) {
      logger.debug("Skipping cookie because starting quotes are not properly balanced in '{}'", unwrappedValue);
      
      return null;
    }
    
    int invalidOctetPos;
    if ((strict) && ((invalidOctetPos = CookieUtil.firstInvalidCookieNameOctet(name)) >= 0)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Skipping cookie because name '{}' contains invalid char '{}'", name, 
          Character.valueOf(name.charAt(invalidOctetPos)));
      }
      return null;
    }
    
    boolean wrap = unwrappedValue.length() != value.length();
    int invalidOctetPos;
    if ((strict) && ((invalidOctetPos = CookieUtil.firstInvalidCookieValueOctet(unwrappedValue)) >= 0)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Skipping cookie because value '{}' contains invalid char '{}'", unwrappedValue, 
          Character.valueOf(unwrappedValue.charAt(invalidOctetPos)));
      }
      return null;
    }
    
    DefaultCookie cookie = new DefaultCookie(name, unwrappedValue.toString());
    cookie.setWrap(wrap);
    return cookie;
  }
}
