package io.netty.handler.codec.http.multipart;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AsciiString;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.nio.charset.Charset;
import java.util.List;






























public class HttpPostRequestDecoder
  implements InterfaceHttpPostRequestDecoder
{
  static final int DEFAULT_DISCARD_THRESHOLD = 10485760;
  private final InterfaceHttpPostRequestDecoder decoder;
  
  public HttpPostRequestDecoder(HttpRequest request)
  {
    this(new DefaultHttpDataFactory(16384L), request, HttpConstants.DEFAULT_CHARSET);
  }
  











  public HttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request)
  {
    this(factory, request, HttpConstants.DEFAULT_CHARSET);
  }
  













  public HttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset)
  {
    ObjectUtil.checkNotNull(factory, "factory");
    ObjectUtil.checkNotNull(request, "request");
    ObjectUtil.checkNotNull(charset, "charset");
    

    if (isMultipart(request)) {
      decoder = new HttpPostMultipartRequestDecoder(factory, request, charset);
    } else {
      decoder = new HttpPostStandardRequestDecoder(factory, request, charset);
    }
  }
  





























  protected static enum MultiPartStatus
  {
    NOTSTARTED,  PREAMBLE,  HEADERDELIMITER,  DISPOSITION,  FIELD,  FILEUPLOAD,  MIXEDPREAMBLE,  MIXEDDELIMITER, 
    MIXEDDISPOSITION,  MIXEDFILEUPLOAD,  MIXEDCLOSEDELIMITER,  CLOSEDELIMITER,  PREEPILOGUE,  EPILOGUE;
    

    private MultiPartStatus() {}
  }
  
  public static boolean isMultipart(HttpRequest request)
  {
    String mimeType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if ((mimeType != null) && (mimeType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString()))) {
      return getMultipartDataBoundary(mimeType) != null;
    }
    return false;
  }
  





  protected static String[] getMultipartDataBoundary(String contentType)
  {
    String[] headerContentType = splitHeaderContentType(contentType);
    String multiPartHeader = HttpHeaderValues.MULTIPART_FORM_DATA.toString();
    if (headerContentType[0].regionMatches(true, 0, multiPartHeader, 0, multiPartHeader.length()))
    {

      String boundaryHeader = HttpHeaderValues.BOUNDARY.toString();
      int crank; if (headerContentType[1].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
        int mrank = 1;
        crank = 2; } else { int crank;
        if (headerContentType[2].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
          int mrank = 2;
          crank = 1;
        } else {
          return null; } }
      int crank;
      int mrank; String boundary = StringUtil.substringAfter(headerContentType[mrank], '=');
      if (boundary == null) {
        throw new ErrorDataDecoderException("Needs a boundary value");
      }
      if (boundary.charAt(0) == '"') {
        String bound = boundary.trim();
        int index = bound.length() - 1;
        if (bound.charAt(index) == '"') {
          boundary = bound.substring(1, index);
        }
      }
      String charsetHeader = HttpHeaderValues.CHARSET.toString();
      if (headerContentType[crank].regionMatches(true, 0, charsetHeader, 0, charsetHeader.length())) {
        String charset = StringUtil.substringAfter(headerContentType[crank], '=');
        if (charset != null) {
          return new String[] { "--" + boundary, charset };
        }
      }
      return new String[] { "--" + boundary };
    }
    return null;
  }
  
  public boolean isMultipart()
  {
    return decoder.isMultipart();
  }
  
  public void setDiscardThreshold(int discardThreshold)
  {
    decoder.setDiscardThreshold(discardThreshold);
  }
  
  public int getDiscardThreshold()
  {
    return decoder.getDiscardThreshold();
  }
  
  public List<InterfaceHttpData> getBodyHttpDatas()
  {
    return decoder.getBodyHttpDatas();
  }
  
  public List<InterfaceHttpData> getBodyHttpDatas(String name)
  {
    return decoder.getBodyHttpDatas(name);
  }
  
  public InterfaceHttpData getBodyHttpData(String name)
  {
    return decoder.getBodyHttpData(name);
  }
  
  public InterfaceHttpPostRequestDecoder offer(HttpContent content)
  {
    return decoder.offer(content);
  }
  
  public boolean hasNext()
  {
    return decoder.hasNext();
  }
  
  public InterfaceHttpData next()
  {
    return decoder.next();
  }
  
  public InterfaceHttpData currentPartialHttpData()
  {
    return decoder.currentPartialHttpData();
  }
  
  public void destroy()
  {
    decoder.destroy();
  }
  
  public void cleanFiles()
  {
    decoder.cleanFiles();
  }
  
  public void removeHttpDataFromClean(InterfaceHttpData data)
  {
    decoder.removeHttpDataFromClean(data);
  }
  










  private static String[] splitHeaderContentType(String sb)
  {
    int aStart = HttpPostBodyUtil.findNonWhitespace(sb, 0);
    int aEnd = sb.indexOf(';');
    if (aEnd == -1) {
      return new String[] { sb, "", "" };
    }
    int bStart = HttpPostBodyUtil.findNonWhitespace(sb, aEnd + 1);
    if (sb.charAt(aEnd - 1) == ' ') {
      aEnd--;
    }
    int bEnd = sb.indexOf(';', bStart);
    if (bEnd == -1) {
      bEnd = HttpPostBodyUtil.findEndOfString(sb);
      return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), "" };
    }
    int cStart = HttpPostBodyUtil.findNonWhitespace(sb, bEnd + 1);
    if (sb.charAt(bEnd - 1) == ' ') {
      bEnd--;
    }
    int cEnd = HttpPostBodyUtil.findEndOfString(sb);
    return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), sb.substring(cStart, cEnd) };
  }
  

  public static class NotEnoughDataDecoderException
    extends DecoderException
  {
    private static final long serialVersionUID = -7846841864603865638L;
    

    public NotEnoughDataDecoderException() {}
    
    public NotEnoughDataDecoderException(String msg)
    {
      super();
    }
    
    public NotEnoughDataDecoderException(Throwable cause) {
      super();
    }
    
    public NotEnoughDataDecoderException(String msg, Throwable cause) {
      super(cause);
    }
  }
  
  public static class EndOfDataDecoderException
    extends DecoderException
  {
    private static final long serialVersionUID = 1336267941020800769L;
    
    public EndOfDataDecoderException() {}
  }
  
  public static class ErrorDataDecoderException
    extends DecoderException
  {
    private static final long serialVersionUID = 5020247425493164465L;
    
    public ErrorDataDecoderException() {}
    
    public ErrorDataDecoderException(String msg)
    {
      super();
    }
    
    public ErrorDataDecoderException(Throwable cause) {
      super();
    }
    
    public ErrorDataDecoderException(String msg, Throwable cause) {
      super(cause);
    }
  }
}
