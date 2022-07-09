package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.UnsupportedValueConverter;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;









































public final class HttpConversionUtil
{
  private static final CharSequenceMap<AsciiString> HTTP_TO_HTTP2_HEADER_BLACKLIST = new CharSequenceMap();
  
  static {
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(HttpHeaderNames.CONNECTION, AsciiString.EMPTY_STRING);
    
    AsciiString keepAlive = HttpHeaderNames.KEEP_ALIVE;
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(keepAlive, AsciiString.EMPTY_STRING);
    
    AsciiString proxyConnection = HttpHeaderNames.PROXY_CONNECTION;
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(proxyConnection, AsciiString.EMPTY_STRING);
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(HttpHeaderNames.TRANSFER_ENCODING, AsciiString.EMPTY_STRING);
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(HttpHeaderNames.HOST, AsciiString.EMPTY_STRING);
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(HttpHeaderNames.UPGRADE, AsciiString.EMPTY_STRING);
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(ExtensionHeaderNames.STREAM_ID.text(), AsciiString.EMPTY_STRING);
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(ExtensionHeaderNames.SCHEME.text(), AsciiString.EMPTY_STRING);
    HTTP_TO_HTTP2_HEADER_BLACKLIST.add(ExtensionHeaderNames.PATH.text(), AsciiString.EMPTY_STRING);
  }
  




  public static final HttpMethod OUT_OF_MESSAGE_SEQUENCE_METHOD = HttpMethod.OPTIONS;
  




  public static final String OUT_OF_MESSAGE_SEQUENCE_PATH = "";
  




  public static final HttpResponseStatus OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE = HttpResponseStatus.OK;
  




  private static final AsciiString EMPTY_REQUEST_PATH = AsciiString.cached("/");
  











  public static enum ExtensionHeaderNames
  {
    STREAM_ID("x-http2-stream-id"), 
    





    SCHEME("x-http2-scheme"), 
    





    PATH("x-http2-path"), 
    





    STREAM_PROMISE_ID("x-http2-stream-promise-id"), 
    





    STREAM_DEPENDENCY_ID("x-http2-stream-dependency-id"), 
    





    STREAM_WEIGHT("x-http2-stream-weight");
    
    private final AsciiString text;
    
    private ExtensionHeaderNames(String text) {
      this.text = AsciiString.cached(text);
    }
    
    public AsciiString text() {
      return text;
    }
  }
  





  public static HttpResponseStatus parseStatus(CharSequence status)
    throws Http2Exception
  {
    try
    {
      HttpResponseStatus result = HttpResponseStatus.parseLine(status);
      if (result == HttpResponseStatus.SWITCHING_PROTOCOLS) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Invalid HTTP/2 status code '%d'", new Object[] { Integer.valueOf(result.code()) });
      }
    } catch (Http2Exception e) {
      throw e;
    } catch (Throwable t) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, t, "Unrecognized HTTP status code '%s' encountered in translation to HTTP/1.x", new Object[] { status });
    }
    HttpResponseStatus result;
    return result;
  }
  












  public static FullHttpResponse toFullHttpResponse(int streamId, Http2Headers http2Headers, ByteBufAllocator alloc, boolean validateHttpHeaders)
    throws Http2Exception
  {
    return toFullHttpResponse(streamId, http2Headers, alloc.buffer(), validateHttpHeaders);
  }
  













  public static FullHttpResponse toFullHttpResponse(int streamId, Http2Headers http2Headers, ByteBuf content, boolean validateHttpHeaders)
    throws Http2Exception
  {
    HttpResponseStatus status = parseStatus(http2Headers.status());
    

    FullHttpResponse msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, validateHttpHeaders);
    try
    {
      addHttp2ToHttpHeaders(streamId, http2Headers, msg, false);
    } catch (Http2Exception e) {
      msg.release();
      throw e;
    } catch (Throwable t) {
      msg.release();
      throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, t, "HTTP/2 to HTTP/1.x headers conversion error", new Object[0]);
    }
    return msg;
  }
  












  public static FullHttpRequest toFullHttpRequest(int streamId, Http2Headers http2Headers, ByteBufAllocator alloc, boolean validateHttpHeaders)
    throws Http2Exception
  {
    return toFullHttpRequest(streamId, http2Headers, alloc.buffer(), validateHttpHeaders);
  }
  
  private static String extractPath(CharSequence method, Http2Headers headers) {
    if (HttpMethod.CONNECT.asciiName().contentEqualsIgnoreCase(method))
    {
      return 
        ((CharSequence)ObjectUtil.checkNotNull(headers.authority(), "authority header cannot be null in the conversion to HTTP/1.x")).toString();
    }
    return 
      ((CharSequence)ObjectUtil.checkNotNull(headers.path(), "path header cannot be null in conversion to HTTP/1.x")).toString();
  }
  














  public static FullHttpRequest toFullHttpRequest(int streamId, Http2Headers http2Headers, ByteBuf content, boolean validateHttpHeaders)
    throws Http2Exception
  {
    CharSequence method = (CharSequence)ObjectUtil.checkNotNull(http2Headers.method(), "method header cannot be null in conversion to HTTP/1.x");
    
    CharSequence path = extractPath(method, http2Headers);
    
    FullHttpRequest msg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(method.toString()), path.toString(), content, validateHttpHeaders);
    try {
      addHttp2ToHttpHeaders(streamId, http2Headers, msg, false);
    } catch (Http2Exception e) {
      msg.release();
      throw e;
    } catch (Throwable t) {
      msg.release();
      throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, t, "HTTP/2 to HTTP/1.x headers conversion error", new Object[0]);
    }
    return msg;
  }
  












  public static HttpRequest toHttpRequest(int streamId, Http2Headers http2Headers, boolean validateHttpHeaders)
    throws Http2Exception
  {
    CharSequence method = (CharSequence)ObjectUtil.checkNotNull(http2Headers.method(), "method header cannot be null in conversion to HTTP/1.x");
    
    CharSequence path = extractPath(method, http2Headers);
    
    HttpRequest msg = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(method.toString()), path.toString(), validateHttpHeaders);
    try {
      addHttp2ToHttpHeaders(streamId, http2Headers, msg.headers(), msg.protocolVersion(), false, true);
    } catch (Http2Exception e) {
      throw e;
    } catch (Throwable t) {
      throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, t, "HTTP/2 to HTTP/1.x headers conversion error", new Object[0]);
    }
    return msg;
  }
  













  public static HttpResponse toHttpResponse(int streamId, Http2Headers http2Headers, boolean validateHttpHeaders)
    throws Http2Exception
  {
    HttpResponseStatus status = parseStatus(http2Headers.status());
    

    HttpResponse msg = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, validateHttpHeaders);
    try {
      addHttp2ToHttpHeaders(streamId, http2Headers, msg.headers(), msg.protocolVersion(), false, false);
    } catch (Http2Exception e) {
      throw e;
    } catch (Throwable t) {
      throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, t, "HTTP/2 to HTTP/1.x headers conversion error", new Object[0]);
    }
    return msg;
  }
  









  public static void addHttp2ToHttpHeaders(int streamId, Http2Headers inputHeaders, FullHttpMessage destinationMessage, boolean addToTrailer)
    throws Http2Exception
  {
    addHttp2ToHttpHeaders(streamId, inputHeaders, addToTrailer ? destinationMessage
      .trailingHeaders() : destinationMessage.headers(), destinationMessage
      .protocolVersion(), addToTrailer, destinationMessage instanceof HttpRequest);
  }
  












  public static void addHttp2ToHttpHeaders(int streamId, Http2Headers inputHeaders, HttpHeaders outputHeaders, HttpVersion httpVersion, boolean isTrailer, boolean isRequest)
    throws Http2Exception
  {
    Http2ToHttpHeaderTranslator translator = new Http2ToHttpHeaderTranslator(streamId, outputHeaders, isRequest);
    try {
      translator.translateHeaders(inputHeaders);
    } catch (Http2Exception ex) {
      throw ex;
    } catch (Throwable t) {
      throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, t, "HTTP/2 to HTTP/1.x headers conversion error", new Object[0]);
    }
    
    outputHeaders.remove(HttpHeaderNames.TRANSFER_ENCODING);
    outputHeaders.remove(HttpHeaderNames.TRAILER);
    if (!isTrailer) {
      outputHeaders.setInt(ExtensionHeaderNames.STREAM_ID.text(), streamId);
      HttpUtil.setKeepAlive(outputHeaders, httpVersion, true);
    }
  }
  








  public static Http2Headers toHttp2Headers(HttpMessage in, boolean validateHeaders)
  {
    HttpHeaders inHeaders = in.headers();
    Http2Headers out = new DefaultHttp2Headers(validateHeaders, inHeaders.size());
    if ((in instanceof HttpRequest)) {
      HttpRequest request = (HttpRequest)in;
      URI requestTargetUri = URI.create(request.uri());
      out.path(toHttp2Path(requestTargetUri));
      out.method(request.method().asciiName());
      setHttp2Scheme(inHeaders, requestTargetUri, out);
      
      if ((!HttpUtil.isOriginForm(requestTargetUri)) && (!HttpUtil.isAsteriskForm(requestTargetUri)))
      {
        String host = inHeaders.getAsString(HttpHeaderNames.HOST);
        setHttp2Authority((host == null) || (host.isEmpty()) ? requestTargetUri.getAuthority() : host, out);
      }
    } else if ((in instanceof HttpResponse)) {
      HttpResponse response = (HttpResponse)in;
      out.status(response.status().codeAsText());
    }
    

    toHttp2Headers(inHeaders, out);
    return out;
  }
  
  public static Http2Headers toHttp2Headers(HttpHeaders inHeaders, boolean validateHeaders) {
    if (inHeaders.isEmpty()) {
      return EmptyHttp2Headers.INSTANCE;
    }
    
    Http2Headers out = new DefaultHttp2Headers(validateHeaders, inHeaders.size());
    toHttp2Headers(inHeaders, out);
    return out;
  }
  
  private static CharSequenceMap<AsciiString> toLowercaseMap(Iterator<? extends CharSequence> valuesIter, int arraySizeHint)
  {
    UnsupportedValueConverter<AsciiString> valueConverter = UnsupportedValueConverter.instance();
    CharSequenceMap<AsciiString> result = new CharSequenceMap(true, valueConverter, arraySizeHint);
    
    while (valuesIter.hasNext()) {
      AsciiString lowerCased = AsciiString.of((CharSequence)valuesIter.next()).toLowerCase();
      try {
        int index = lowerCased.forEachByte(ByteProcessor.FIND_COMMA);
        if (index != -1) {
          int start = 0;
          do {
            result.add(lowerCased.subSequence(start, index, false).trim(), AsciiString.EMPTY_STRING);
            start = index + 1;
          } while ((start < lowerCased.length()) && 
            ((index = lowerCased.forEachByte(start, lowerCased.length() - start, ByteProcessor.FIND_COMMA)) != -1));
          result.add(lowerCased.subSequence(start, lowerCased.length(), false).trim(), AsciiString.EMPTY_STRING);
        } else {
          result.add(lowerCased.trim(), AsciiString.EMPTY_STRING);
        }
      }
      catch (Exception e)
      {
        throw new IllegalStateException(e);
      }
    }
    return result;
  }
  






  private static void toHttp2HeadersFilterTE(Map.Entry<CharSequence, CharSequence> entry, Http2Headers out)
  {
    if (AsciiString.indexOf((CharSequence)entry.getValue(), ',', 0) == -1) {
      if (AsciiString.contentEqualsIgnoreCase(AsciiString.trim((CharSequence)entry.getValue()), HttpHeaderValues.TRAILERS)) {
        out.add(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
      }
    } else {
      List<CharSequence> teValues = StringUtil.unescapeCsvFields((CharSequence)entry.getValue());
      for (CharSequence teValue : teValues) {
        if (AsciiString.contentEqualsIgnoreCase(AsciiString.trim(teValue), HttpHeaderValues.TRAILERS)) {
          out.add(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
          break;
        }
      }
    }
  }
  
  public static void toHttp2Headers(HttpHeaders inHeaders, Http2Headers out) {
    Iterator<Map.Entry<CharSequence, CharSequence>> iter = inHeaders.iteratorCharSequence();
    


    CharSequenceMap<AsciiString> connectionBlacklist = toLowercaseMap(inHeaders.valueCharSequenceIterator(HttpHeaderNames.CONNECTION), 8);
    while (iter.hasNext()) {
      Map.Entry<CharSequence, CharSequence> entry = (Map.Entry)iter.next();
      AsciiString aName = AsciiString.of((CharSequence)entry.getKey()).toLowerCase();
      if ((!HTTP_TO_HTTP2_HEADER_BLACKLIST.contains(aName)) && (!connectionBlacklist.contains(aName)))
      {
        if (aName.contentEqualsIgnoreCase(HttpHeaderNames.TE)) {
          toHttp2HeadersFilterTE(entry, out);
        } else if (aName.contentEqualsIgnoreCase(HttpHeaderNames.COOKIE)) {
          AsciiString value = AsciiString.of((CharSequence)entry.getValue());
          
          try
          {
            int index = value.forEachByte(ByteProcessor.FIND_SEMI_COLON);
            if (index != -1) {
              int start = 0;
              do {
                out.add(HttpHeaderNames.COOKIE, value.subSequence(start, index, false));
                
                start = index + 2;
              } while ((start < value.length()) && 
                ((index = value.forEachByte(start, value.length() - start, ByteProcessor.FIND_SEMI_COLON)) != -1));
              if (start >= value.length()) {
                throw new IllegalArgumentException("cookie value is of unexpected format: " + value);
              }
              out.add(HttpHeaderNames.COOKIE, value.subSequence(start, value.length(), false));
            } else {
              out.add(HttpHeaderNames.COOKIE, value);
            }
          }
          catch (Exception e)
          {
            throw new IllegalStateException(e);
          }
        } else {
          out.add(aName, entry.getValue());
        }
      }
    }
  }
  




  private static AsciiString toHttp2Path(URI uri)
  {
    StringBuilder pathBuilder = new StringBuilder(StringUtil.length(uri.getRawPath()) + StringUtil.length(uri.getRawQuery()) + StringUtil.length(uri.getRawFragment()) + 2);
    if (!StringUtil.isNullOrEmpty(uri.getRawPath())) {
      pathBuilder.append(uri.getRawPath());
    }
    if (!StringUtil.isNullOrEmpty(uri.getRawQuery())) {
      pathBuilder.append('?');
      pathBuilder.append(uri.getRawQuery());
    }
    if (!StringUtil.isNullOrEmpty(uri.getRawFragment())) {
      pathBuilder.append('#');
      pathBuilder.append(uri.getRawFragment());
    }
    String path = pathBuilder.toString();
    return path.isEmpty() ? EMPTY_REQUEST_PATH : new AsciiString(path);
  }
  

  static void setHttp2Authority(String authority, Http2Headers out)
  {
    if (authority != null) {
      if (authority.isEmpty()) {
        out.authority(AsciiString.EMPTY_STRING);
      } else {
        int start = authority.indexOf('@') + 1;
        int length = authority.length() - start;
        if (length == 0) {
          throw new IllegalArgumentException("authority: " + authority);
        }
        out.authority(new AsciiString(authority, start, length));
      }
    }
  }
  
  private static void setHttp2Scheme(HttpHeaders in, URI uri, Http2Headers out) {
    String value = uri.getScheme();
    if (value != null) {
      out.scheme(new AsciiString(value));
      return;
    }
    

    CharSequence cValue = in.get(ExtensionHeaderNames.SCHEME.text());
    if (cValue != null) {
      out.scheme(AsciiString.of(cValue));
      return;
    }
    
    if (uri.getPort() == HttpScheme.HTTPS.port()) {
      out.scheme(HttpScheme.HTTPS.name());
    } else if (uri.getPort() == HttpScheme.HTTP.port()) {
      out.scheme(HttpScheme.HTTP.name());
    } else {
      throw new IllegalArgumentException(":scheme must be specified. see https://tools.ietf.org/html/rfc7540#section-8.1.2.3");
    }
  }
  



  private HttpConversionUtil() {}
  


  private static final class Http2ToHttpHeaderTranslator
  {
    private static final CharSequenceMap<AsciiString> REQUEST_HEADER_TRANSLATIONS = new CharSequenceMap();
    
    private static final CharSequenceMap<AsciiString> RESPONSE_HEADER_TRANSLATIONS = new CharSequenceMap();
    
    static { RESPONSE_HEADER_TRANSLATIONS.add(Http2Headers.PseudoHeaderName.AUTHORITY.value(), HttpHeaderNames.HOST);
      
      RESPONSE_HEADER_TRANSLATIONS.add(Http2Headers.PseudoHeaderName.SCHEME.value(), HttpConversionUtil.ExtensionHeaderNames.SCHEME
        .text());
      REQUEST_HEADER_TRANSLATIONS.add(RESPONSE_HEADER_TRANSLATIONS);
      RESPONSE_HEADER_TRANSLATIONS.add(Http2Headers.PseudoHeaderName.PATH.value(), HttpConversionUtil.ExtensionHeaderNames.PATH
        .text());
    }
    


    private final int streamId;
    

    private final HttpHeaders output;
    

    private final CharSequenceMap<AsciiString> translations;
    
    Http2ToHttpHeaderTranslator(int streamId, HttpHeaders output, boolean request)
    {
      this.streamId = streamId;
      this.output = output;
      translations = (request ? REQUEST_HEADER_TRANSLATIONS : RESPONSE_HEADER_TRANSLATIONS);
    }
    
    void translateHeaders(Iterable<Map.Entry<CharSequence, CharSequence>> inputHeaders) throws Http2Exception
    {
      StringBuilder cookies = null;
      
      for (Map.Entry<CharSequence, CharSequence> entry : inputHeaders) {
        CharSequence name = (CharSequence)entry.getKey();
        CharSequence value = (CharSequence)entry.getValue();
        AsciiString translatedName = (AsciiString)translations.get(name);
        if (translatedName != null) {
          output.add(translatedName, AsciiString.of(value));
        } else if (!Http2Headers.PseudoHeaderName.isPseudoHeader(name))
        {

          if ((name.length() == 0) || (name.charAt(0) == ':')) {
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Invalid HTTP/2 header '%s' encountered in translation to HTTP/1.x", new Object[] { name });
          }
          
          if (HttpHeaderNames.COOKIE.equals(name))
          {

            if (cookies == null) {
              cookies = InternalThreadLocalMap.get().stringBuilder();
            } else if (cookies.length() > 0) {
              cookies.append("; ");
            }
            cookies.append(value);
          } else {
            output.add(name, value);
          }
        }
      }
      if (cookies != null) {
        output.add(HttpHeaderNames.COOKIE, cookies.toString());
      }
    }
  }
}
