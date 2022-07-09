package io.netty.handler.codec.http.cookie;

import io.netty.handler.codec.DateFormatter;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;













































public final class ServerCookieEncoder
  extends CookieEncoder
{
  public static final ServerCookieEncoder STRICT = new ServerCookieEncoder(true);
  




  public static final ServerCookieEncoder LAX = new ServerCookieEncoder(false);
  
  private ServerCookieEncoder(boolean strict) {
    super(strict);
  }
  






  public String encode(String name, String value)
  {
    return encode(new DefaultCookie(name, value));
  }
  





  public String encode(Cookie cookie)
  {
    String name = ((Cookie)ObjectUtil.checkNotNull(cookie, "cookie")).name();
    String value = cookie.value() != null ? cookie.value() : "";
    
    validateCookie(name, value);
    
    StringBuilder buf = CookieUtil.stringBuilder();
    
    if (cookie.wrap()) {
      CookieUtil.addQuoted(buf, name, value);
    } else {
      CookieUtil.add(buf, name, value);
    }
    
    if (cookie.maxAge() != Long.MIN_VALUE) {
      CookieUtil.add(buf, "Max-Age", cookie.maxAge());
      Date expires = new Date(cookie.maxAge() * 1000L + System.currentTimeMillis());
      buf.append("Expires");
      buf.append('=');
      DateFormatter.append(expires, buf);
      buf.append(';');
      buf.append(' ');
    }
    
    if (cookie.path() != null) {
      CookieUtil.add(buf, "Path", cookie.path());
    }
    
    if (cookie.domain() != null) {
      CookieUtil.add(buf, "Domain", cookie.domain());
    }
    if (cookie.isSecure()) {
      CookieUtil.add(buf, "Secure");
    }
    if (cookie.isHttpOnly()) {
      CookieUtil.add(buf, "HTTPOnly");
    }
    if ((cookie instanceof DefaultCookie)) {
      DefaultCookie c = (DefaultCookie)cookie;
      if (c.sameSite() != null) {
        CookieUtil.add(buf, "SameSite", c.sameSite().name());
      }
    }
    
    return CookieUtil.stripTrailingSeparator(buf);
  }
  





  private static List<String> dedup(List<String> encoded, Map<String, Integer> nameToLastIndex)
  {
    boolean[] isLastInstance = new boolean[encoded.size()];
    for (Iterator localIterator = nameToLastIndex.values().iterator(); localIterator.hasNext();) { int idx = ((Integer)localIterator.next()).intValue();
      isLastInstance[idx] = true;
    }
    Object dedupd = new ArrayList(nameToLastIndex.size());
    int i = 0; for (int n = encoded.size(); i < n; i++) {
      if (isLastInstance[i] != 0) {
        ((List)dedupd).add(encoded.get(i));
      }
    }
    return dedupd;
  }
  





  public List<String> encode(Cookie... cookies)
  {
    if (((Cookie[])ObjectUtil.checkNotNull(cookies, "cookies")).length == 0) {
      return Collections.emptyList();
    }
    
    List<String> encoded = new ArrayList(cookies.length);
    Map<String, Integer> nameToIndex = (strict) && (cookies.length > 1) ? new HashMap() : null;
    boolean hasDupdName = false;
    for (int i = 0; i < cookies.length; i++) {
      Cookie c = cookies[i];
      encoded.add(encode(c));
      if (nameToIndex != null) {
        hasDupdName |= nameToIndex.put(c.name(), Integer.valueOf(i)) != null;
      }
    }
    return hasDupdName ? dedup(encoded, nameToIndex) : encoded;
  }
  





  public List<String> encode(Collection<? extends Cookie> cookies)
  {
    if (((Collection)ObjectUtil.checkNotNull(cookies, "cookies")).isEmpty()) {
      return Collections.emptyList();
    }
    
    List<String> encoded = new ArrayList(cookies.size());
    Map<String, Integer> nameToIndex = (strict) && (cookies.size() > 1) ? new HashMap() : null;
    int i = 0;
    boolean hasDupdName = false;
    for (Cookie c : cookies) {
      encoded.add(encode(c));
      if (nameToIndex != null) {
        hasDupdName |= nameToIndex.put(c.name(), Integer.valueOf(i++)) != null;
      }
    }
    return hasDupdName ? dedup(encoded, nameToIndex) : encoded;
  }
  





  public List<String> encode(Iterable<? extends Cookie> cookies)
  {
    Iterator<? extends Cookie> cookiesIt = ((Iterable)ObjectUtil.checkNotNull(cookies, "cookies")).iterator();
    if (!cookiesIt.hasNext()) {
      return Collections.emptyList();
    }
    
    List<String> encoded = new ArrayList();
    Cookie firstCookie = (Cookie)cookiesIt.next();
    Map<String, Integer> nameToIndex = (strict) && (cookiesIt.hasNext()) ? new HashMap() : null;
    int i = 0;
    encoded.add(encode(firstCookie));
    boolean hasDupdName = (nameToIndex != null) && (nameToIndex.put(firstCookie.name(), Integer.valueOf(i++)) != null);
    while (cookiesIt.hasNext()) {
      Cookie c = (Cookie)cookiesIt.next();
      encoded.add(encode(c));
      if (nameToIndex != null) {
        hasDupdName |= nameToIndex.put(c.name(), Integer.valueOf(i++)) != null;
      }
    }
    return hasDupdName ? dedup(encoded, nameToIndex) : encoded;
  }
}
