package io.netty.handler.codec.http;

import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.internal.ObjectUtil;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
























public final class HttpUtil
{
  private static final AsciiString CHARSET_EQUALS = AsciiString.of(HttpHeaderValues.CHARSET + "=");
  private static final AsciiString SEMICOLON = AsciiString.cached(";");
  private static final String COMMA_STRING = String.valueOf(',');
  


  private HttpUtil() {}
  

  public static boolean isOriginForm(URI uri)
  {
    return (uri.getScheme() == null) && (uri.getSchemeSpecificPart() == null) && 
      (uri.getHost() == null) && (uri.getAuthority() == null);
  }
  



  public static boolean isAsteriskForm(URI uri)
  {
    return ("*".equals(uri.getPath())) && 
      (uri.getScheme() == null) && (uri.getSchemeSpecificPart() == null) && 
      (uri.getHost() == null) && (uri.getAuthority() == null) && (uri.getQuery() == null) && 
      (uri.getFragment() == null);
  }
  






  public static boolean isKeepAlive(HttpMessage message)
  {
    return (!message.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true)) && (
      (message.protocolVersion().isKeepAliveDefault()) || 
      (message.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE, true)));
  }
  



















  public static void setKeepAlive(HttpMessage message, boolean keepAlive)
  {
    setKeepAlive(message.headers(), message.protocolVersion(), keepAlive);
  }
  


















  public static void setKeepAlive(HttpHeaders h, HttpVersion httpVersion, boolean keepAlive)
  {
    if (httpVersion.isKeepAliveDefault()) {
      if (keepAlive) {
        h.remove(HttpHeaderNames.CONNECTION);
      } else {
        h.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      }
    }
    else if (keepAlive) {
      h.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    } else {
      h.remove(HttpHeaderNames.CONNECTION);
    }
  }
  












  public static long getContentLength(HttpMessage message)
  {
    String value = message.headers().get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value);
    }
    


    long webSocketContentLength = getWebSocketContentLength(message);
    if (webSocketContentLength >= 0L) {
      return webSocketContentLength;
    }
    

    throw new NumberFormatException("header not found: " + HttpHeaderNames.CONTENT_LENGTH);
  }
  









  public static long getContentLength(HttpMessage message, long defaultValue)
  {
    String value = message.headers().get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value);
    }
    


    long webSocketContentLength = getWebSocketContentLength(message);
    if (webSocketContentLength >= 0L) {
      return webSocketContentLength;
    }
    

    return defaultValue;
  }
  






  public static int getContentLength(HttpMessage message, int defaultValue)
  {
    return (int)Math.min(2147483647L, getContentLength(message, defaultValue));
  }
  




  private static int getWebSocketContentLength(HttpMessage message)
  {
    HttpHeaders h = message.headers();
    if ((message instanceof HttpRequest)) {
      HttpRequest req = (HttpRequest)message;
      if ((HttpMethod.GET.equals(req.method())) && 
        (h.contains(HttpHeaderNames.SEC_WEBSOCKET_KEY1)) && 
        (h.contains(HttpHeaderNames.SEC_WEBSOCKET_KEY2))) {
        return 8;
      }
    } else if ((message instanceof HttpResponse)) {
      HttpResponse res = (HttpResponse)message;
      if ((res.status().code() == 101) && 
        (h.contains(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN)) && 
        (h.contains(HttpHeaderNames.SEC_WEBSOCKET_LOCATION))) {
        return 16;
      }
    }
    

    return -1;
  }
  


  public static void setContentLength(HttpMessage message, long length)
  {
    message.headers().set(HttpHeaderNames.CONTENT_LENGTH, Long.valueOf(length));
  }
  
  public static boolean isContentLengthSet(HttpMessage m) {
    return m.headers().contains(HttpHeaderNames.CONTENT_LENGTH);
  }
  








  public static boolean is100ContinueExpected(HttpMessage message)
  {
    if (isExpectHeaderValid(message)) {} return 
    
      message.headers().contains(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE, true);
  }
  







  static boolean isUnsupportedExpectation(HttpMessage message)
  {
    if (!isExpectHeaderValid(message)) {
      return false;
    }
    
    String expectValue = message.headers().get(HttpHeaderNames.EXPECT);
    return (expectValue != null) && (!HttpHeaderValues.CONTINUE.toString().equalsIgnoreCase(expectValue));
  }
  




  private static boolean isExpectHeaderValid(HttpMessage message)
  {
    return ((message instanceof HttpRequest)) && 
      (message.protocolVersion().compareTo(HttpVersion.HTTP_1_1) >= 0);
  }
  






  public static void set100ContinueExpected(HttpMessage message, boolean expected)
  {
    if (expected) {
      message.headers().set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE);
    } else {
      message.headers().remove(HttpHeaderNames.EXPECT);
    }
  }
  





  public static boolean isTransferEncodingChunked(HttpMessage message)
  {
    return message.headers().containsValue(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED, true);
  }
  







  public static void setTransferEncodingChunked(HttpMessage m, boolean chunked)
  {
    if (chunked) {
      m.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
      m.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
    } else {
      List<String> encodings = m.headers().getAll(HttpHeaderNames.TRANSFER_ENCODING);
      if (encodings.isEmpty()) {
        return;
      }
      List<CharSequence> values = new ArrayList(encodings);
      Iterator<CharSequence> valuesIt = values.iterator();
      while (valuesIt.hasNext()) {
        CharSequence value = (CharSequence)valuesIt.next();
        if (HttpHeaderValues.CHUNKED.contentEqualsIgnoreCase(value)) {
          valuesIt.remove();
        }
      }
      if (values.isEmpty()) {
        m.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
      } else {
        m.headers().set(HttpHeaderNames.TRANSFER_ENCODING, values);
      }
    }
  }
  






  public static Charset getCharset(HttpMessage message)
  {
    return getCharset(message, CharsetUtil.ISO_8859_1);
  }
  






  public static Charset getCharset(CharSequence contentTypeValue)
  {
    if (contentTypeValue != null) {
      return getCharset(contentTypeValue, CharsetUtil.ISO_8859_1);
    }
    return CharsetUtil.ISO_8859_1;
  }
  








  public static Charset getCharset(HttpMessage message, Charset defaultCharset)
  {
    CharSequence contentTypeValue = message.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if (contentTypeValue != null) {
      return getCharset(contentTypeValue, defaultCharset);
    }
    return defaultCharset;
  }
  








  public static Charset getCharset(CharSequence contentTypeValue, Charset defaultCharset)
  {
    if (contentTypeValue != null) {
      CharSequence charsetRaw = getCharsetAsSequence(contentTypeValue);
      if (charsetRaw != null) {
        if ((charsetRaw.length() > 2) && 
          (charsetRaw.charAt(0) == '"') && (charsetRaw.charAt(charsetRaw.length() - 1) == '"')) {
          charsetRaw = charsetRaw.subSequence(1, charsetRaw.length() - 1);
        }
        try
        {
          return Charset.forName(charsetRaw.toString());
        }
        catch (IllegalCharsetNameException localIllegalCharsetNameException) {}catch (UnsupportedCharsetException localUnsupportedCharsetException) {}
      }
    }
    


    return defaultCharset;
  }
  










  @Deprecated
  public static CharSequence getCharsetAsString(HttpMessage message)
  {
    return getCharsetAsSequence(message);
  }
  








  public static CharSequence getCharsetAsSequence(HttpMessage message)
  {
    CharSequence contentTypeValue = message.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if (contentTypeValue != null) {
      return getCharsetAsSequence(contentTypeValue);
    }
    return null;
  }
  











  public static CharSequence getCharsetAsSequence(CharSequence contentTypeValue)
  {
    ObjectUtil.checkNotNull(contentTypeValue, "contentTypeValue");
    
    int indexOfCharset = AsciiString.indexOfIgnoreCaseAscii(contentTypeValue, CHARSET_EQUALS, 0);
    if (indexOfCharset == -1) {
      return null;
    }
    
    int indexOfEncoding = indexOfCharset + CHARSET_EQUALS.length();
    if (indexOfEncoding < contentTypeValue.length()) {
      CharSequence charsetCandidate = contentTypeValue.subSequence(indexOfEncoding, contentTypeValue.length());
      int indexOfSemicolon = AsciiString.indexOfIgnoreCaseAscii(charsetCandidate, SEMICOLON, 0);
      if (indexOfSemicolon == -1) {
        return charsetCandidate;
      }
      
      return charsetCandidate.subSequence(0, indexOfSemicolon);
    }
    
    return null;
  }
  










  public static CharSequence getMimeType(HttpMessage message)
  {
    CharSequence contentTypeValue = message.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if (contentTypeValue != null) {
      return getMimeType(contentTypeValue);
    }
    return null;
  }
  












  public static CharSequence getMimeType(CharSequence contentTypeValue)
  {
    ObjectUtil.checkNotNull(contentTypeValue, "contentTypeValue");
    
    int indexOfSemicolon = AsciiString.indexOfIgnoreCaseAscii(contentTypeValue, SEMICOLON, 0);
    if (indexOfSemicolon != -1) {
      return contentTypeValue.subSequence(0, indexOfSemicolon);
    }
    return contentTypeValue.length() > 0 ? contentTypeValue : null;
  }
  







  public static String formatHostnameForHttp(InetSocketAddress addr)
  {
    String hostString = NetUtil.getHostname(addr);
    if (NetUtil.isValidIpV6Address(hostString)) {
      if (!addr.isUnresolved()) {
        hostString = NetUtil.toAddressString(addr.getAddress());
      }
      return '[' + hostString + ']';
    }
    return hostString;
  }
  












  public static long normalizeAndGetContentLength(List<? extends CharSequence> contentLengthFields, boolean isHttp10OrEarlier, boolean allowDuplicateContentLengths)
  {
    if (contentLengthFields.isEmpty()) {
      return -1L;
    }
    













    String firstField = ((CharSequence)contentLengthFields.get(0)).toString();
    
    boolean multipleContentLengths = (contentLengthFields.size() > 1) || (firstField.indexOf(',') >= 0);
    
    if ((multipleContentLengths) && (!isHttp10OrEarlier)) {
      if (allowDuplicateContentLengths)
      {
        String firstValue = null;
        for (CharSequence field : contentLengthFields) {
          String[] tokens = field.toString().split(COMMA_STRING, -1);
          for (String token : tokens) {
            String trimmed = token.trim();
            if (firstValue == null) {
              firstValue = trimmed;
            } else if (!trimmed.equals(firstValue)) {
              throw new IllegalArgumentException("Multiple Content-Length values found: " + contentLengthFields);
            }
          }
        }
        

        firstField = firstValue;
      }
      else {
        throw new IllegalArgumentException("Multiple Content-Length values found: " + contentLengthFields);
      }
    }
    


    if ((firstField.isEmpty()) || (!Character.isDigit(firstField.charAt(0))))
    {
      throw new IllegalArgumentException("Content-Length value is not a number: " + firstField);
    }
    try
    {
      long value = Long.parseLong(firstField);
      return ObjectUtil.checkPositiveOrZero(value, "Content-Length value");
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("Content-Length value is not a number: " + firstField, e);
    }
  }
}
