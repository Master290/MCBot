package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;
import io.netty.util.AsciiString;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


































public class HttpPostRequestEncoder
  implements ChunkedInput<HttpContent>
{
  public static enum EncoderMode
  {
    RFC1738, 
    



    RFC3986, 
    









    HTML5;
    

    private EncoderMode() {}
  }
  

  private static final Map.Entry[] percentEncodings = { new AbstractMap.SimpleImmutableEntry(
    Pattern.compile("\\*"), "%2A"), new AbstractMap.SimpleImmutableEntry(
    Pattern.compile("\\+"), "%20"), new AbstractMap.SimpleImmutableEntry(
    Pattern.compile("~"), "%7E") };
  

  private final HttpDataFactory factory;
  

  private final HttpRequest request;
  

  private final Charset charset;
  

  private boolean isChunked;
  

  private final List<InterfaceHttpData> bodyListDatas;
  

  final List<InterfaceHttpData> multipartHttpDatas;
  

  private final boolean isMultipart;
  

  String multipartDataBoundary;
  

  String multipartMixedBoundary;
  

  private boolean headerFinalized;
  

  private final EncoderMode encoderMode;
  

  private boolean isLastChunk;
  

  private boolean isLastChunkSent;
  

  private FileUpload currentFileUpload;
  

  private boolean duringMixedMode;
  

  private long globalBodySize;
  

  private long globalProgress;
  

  private ListIterator<InterfaceHttpData> iterator;
  

  private ByteBuf currentBuffer;
  

  private InterfaceHttpData currentData;
  

  public HttpPostRequestEncoder(HttpRequest request, boolean multipart)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    this(new DefaultHttpDataFactory(16384L), request, multipart, HttpConstants.DEFAULT_CHARSET, EncoderMode.RFC1738);
  }
  













  public HttpPostRequestEncoder(HttpDataFactory factory, HttpRequest request, boolean multipart)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    this(factory, request, multipart, HttpConstants.DEFAULT_CHARSET, EncoderMode.RFC1738);
  }
  


















  public HttpPostRequestEncoder(HttpDataFactory factory, HttpRequest request, boolean multipart, Charset charset, EncoderMode encoderMode)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    this.request = ((HttpRequest)ObjectUtil.checkNotNull(request, "request"));
    this.charset = ((Charset)ObjectUtil.checkNotNull(charset, "charset"));
    this.factory = ((HttpDataFactory)ObjectUtil.checkNotNull(factory, "factory"));
    if (HttpMethod.TRACE.equals(request.method())) {
      throw new ErrorDataEncoderException("Cannot create a Encoder if request is a TRACE");
    }
    
    bodyListDatas = new ArrayList();
    
    isLastChunk = false;
    isLastChunkSent = false;
    isMultipart = multipart;
    multipartHttpDatas = new ArrayList();
    this.encoderMode = encoderMode;
    if (isMultipart) {
      initDataMultipart();
    }
  }
  


  public void cleanFiles()
  {
    factory.cleanRequestHttpData(request);
  }
  





























  public boolean isMultipart()
  {
    return isMultipart;
  }
  


  private void initDataMultipart()
  {
    multipartDataBoundary = getNewMultipartDelimiter();
  }
  


  private void initMixedMultipart()
  {
    multipartMixedBoundary = getNewMultipartDelimiter();
  }
  




  private static String getNewMultipartDelimiter()
  {
    return Long.toHexString(PlatformDependent.threadLocalRandom().nextLong());
  }
  




  public List<InterfaceHttpData> getBodyListAttributes()
  {
    return bodyListDatas;
  }
  






  public void setBodyHttpDatas(List<InterfaceHttpData> datas)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    ObjectUtil.checkNotNull(datas, "datas");
    globalBodySize = 0L;
    bodyListDatas.clear();
    currentFileUpload = null;
    duringMixedMode = false;
    multipartHttpDatas.clear();
    for (InterfaceHttpData data : datas) {
      addBodyHttpData(data);
    }
  }
  










  public void addBodyAttribute(String name, String value)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    String svalue = value != null ? value : "";
    Attribute data = factory.createAttribute(request, (String)ObjectUtil.checkNotNull(name, "name"), svalue);
    addBodyHttpData(data);
  }
  















  public void addBodyFileUpload(String name, File file, String contentType, boolean isText)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    addBodyFileUpload(name, file.getName(), file, contentType, isText);
  }
  


















  public void addBodyFileUpload(String name, String filename, File file, String contentType, boolean isText)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    ObjectUtil.checkNotNull(name, "name");
    ObjectUtil.checkNotNull(file, "file");
    if (filename == null) {
      filename = "";
    }
    String scontentType = contentType;
    String contentTransferEncoding = null;
    if (contentType == null) {
      if (isText) {
        scontentType = "text/plain";
      } else {
        scontentType = "application/octet-stream";
      }
    }
    if (!isText) {
      contentTransferEncoding = HttpPostBodyUtil.TransferEncodingMechanism.BINARY.value();
    }
    FileUpload fileUpload = factory.createFileUpload(request, name, filename, scontentType, contentTransferEncoding, null, file
      .length());
    try {
      fileUpload.setContent(file);
    } catch (IOException e) {
      throw new ErrorDataEncoderException(e);
    }
    addBodyHttpData(fileUpload);
  }
  















  public void addBodyFileUploads(String name, File[] file, String[] contentType, boolean[] isText)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    if ((file.length != contentType.length) && (file.length != isText.length)) {
      throw new IllegalArgumentException("Different array length");
    }
    for (int i = 0; i < file.length; i++) {
      addBodyFileUpload(name, file[i], contentType[i], isText[i]);
    }
  }
  






  public void addBodyHttpData(InterfaceHttpData data)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    if (headerFinalized) {
      throw new ErrorDataEncoderException("Cannot add value once finalized");
    }
    bodyListDatas.add(ObjectUtil.checkNotNull(data, "data"));
    if (!isMultipart) {
      if ((data instanceof Attribute)) {
        Attribute attribute = (Attribute)data;
        try
        {
          String key = encodeAttribute(attribute.getName(), charset);
          String value = encodeAttribute(attribute.getValue(), charset);
          Attribute newattribute = factory.createAttribute(request, key, value);
          multipartHttpDatas.add(newattribute);
          globalBodySize += newattribute.getName().length() + 1 + newattribute.length() + 1L;
        } catch (IOException e) {
          throw new ErrorDataEncoderException(e);
        }
      } else if ((data instanceof FileUpload))
      {
        FileUpload fileUpload = (FileUpload)data;
        
        String key = encodeAttribute(fileUpload.getName(), charset);
        String value = encodeAttribute(fileUpload.getFilename(), charset);
        Attribute newattribute = factory.createAttribute(request, key, value);
        multipartHttpDatas.add(newattribute);
        globalBodySize += newattribute.getName().length() + 1 + newattribute.length() + 1L;
      }
      return;
    }
    































    if ((data instanceof Attribute)) {
      if (duringMixedMode) {
        InternalAttribute internal = new InternalAttribute(charset);
        internal.addValue("\r\n--" + multipartMixedBoundary + "--");
        multipartHttpDatas.add(internal);
        multipartMixedBoundary = null;
        currentFileUpload = null;
        duringMixedMode = false;
      }
      InternalAttribute internal = new InternalAttribute(charset);
      if (!multipartHttpDatas.isEmpty())
      {
        internal.addValue("\r\n");
      }
      internal.addValue("--" + multipartDataBoundary + "\r\n");
      
      Attribute attribute = (Attribute)data;
      internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + attribute
        .getName() + "\"\r\n");
      
      internal.addValue(HttpHeaderNames.CONTENT_LENGTH + ": " + attribute
        .length() + "\r\n");
      Charset localcharset = attribute.getCharset();
      if (localcharset != null)
      {
        internal.addValue(HttpHeaderNames.CONTENT_TYPE + ": " + "text/plain" + "; " + HttpHeaderValues.CHARSET + '=' + localcharset
        

          .name() + "\r\n");
      }
      
      internal.addValue("\r\n");
      multipartHttpDatas.add(internal);
      multipartHttpDatas.add(data);
      globalBodySize += attribute.length() + internal.size();
    } else if ((data instanceof FileUpload)) {
      FileUpload fileUpload = (FileUpload)data;
      InternalAttribute internal = new InternalAttribute(charset);
      if (!multipartHttpDatas.isEmpty())
      {
        internal.addValue("\r\n");
      }
      boolean localMixed;
      if (duringMixedMode) { boolean localMixed;
        if ((currentFileUpload != null) && (currentFileUpload.getName().equals(fileUpload.getName())))
        {

          localMixed = true;


        }
        else
        {

          internal.addValue("--" + multipartMixedBoundary + "--");
          multipartHttpDatas.add(internal);
          multipartMixedBoundary = null;
          

          internal = new InternalAttribute(charset);
          internal.addValue("\r\n");
          boolean localMixed = false;
          
          currentFileUpload = fileUpload;
          duringMixedMode = false;
        }
      }
      else if ((encoderMode != EncoderMode.HTML5) && (currentFileUpload != null) && 
        (currentFileUpload.getName().equals(fileUpload.getName())))
      {



















        initMixedMultipart();
        InternalAttribute pastAttribute = (InternalAttribute)multipartHttpDatas.get(multipartHttpDatas
          .size() - 2);
        
        globalBodySize -= pastAttribute.size();
        































        StringBuilder replacement = new StringBuilder(139 + multipartDataBoundary.length() + multipartMixedBoundary.length() * 2 + fileUpload.getFilename().length() + fileUpload.getName().length()).append("--").append(multipartDataBoundary).append("\r\n").append(HttpHeaderNames.CONTENT_DISPOSITION).append(": ").append(HttpHeaderValues.FORM_DATA).append("; ").append(HttpHeaderValues.NAME).append("=\"").append(fileUpload.getName()).append("\"\r\n").append(HttpHeaderNames.CONTENT_TYPE).append(": ").append(HttpHeaderValues.MULTIPART_MIXED).append("; ").append(HttpHeaderValues.BOUNDARY).append('=').append(multipartMixedBoundary).append("\r\n\r\n").append("--").append(multipartMixedBoundary).append("\r\n").append(HttpHeaderNames.CONTENT_DISPOSITION).append(": ").append(HttpHeaderValues.ATTACHMENT);
        
        if (!fileUpload.getFilename().isEmpty())
        {



          replacement.append("; ").append(HttpHeaderValues.FILENAME).append("=\"").append(currentFileUpload.getFilename()).append('"');
        }
        
        replacement.append("\r\n");
        
        pastAttribute.setValue(replacement.toString(), 1);
        pastAttribute.setValue("", 2);
        

        globalBodySize += pastAttribute.size();
        




        boolean localMixed = true;
        duringMixedMode = true;

      }
      else
      {
        localMixed = false;
        currentFileUpload = fileUpload;
        duringMixedMode = false;
      }
      

      if (localMixed)
      {

        internal.addValue("--" + multipartMixedBoundary + "\r\n");
        
        if (fileUpload.getFilename().isEmpty())
        {
          internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.ATTACHMENT + "\r\n");
        }
        else
        {
          internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.ATTACHMENT + "; " + HttpHeaderValues.FILENAME + "=\"" + fileUpload
          
            .getFilename() + "\"\r\n");
        }
      } else {
        internal.addValue("--" + multipartDataBoundary + "\r\n");
        
        if (fileUpload.getFilename().isEmpty())
        {
          internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + fileUpload
            .getName() + "\"\r\n");
        }
        else
        {
          internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + fileUpload
            .getName() + "\"; " + HttpHeaderValues.FILENAME + "=\"" + fileUpload
            .getFilename() + "\"\r\n");
        }
      }
      
      internal.addValue(HttpHeaderNames.CONTENT_LENGTH + ": " + fileUpload
        .length() + "\r\n");
      


      internal.addValue(HttpHeaderNames.CONTENT_TYPE + ": " + fileUpload.getContentType());
      String contentTransferEncoding = fileUpload.getContentTransferEncoding();
      if ((contentTransferEncoding != null) && 
        (contentTransferEncoding.equals(HttpPostBodyUtil.TransferEncodingMechanism.BINARY.value()))) {
        internal.addValue("\r\n" + HttpHeaderNames.CONTENT_TRANSFER_ENCODING + ": " + HttpPostBodyUtil.TransferEncodingMechanism.BINARY
          .value() + "\r\n\r\n");
      } else if (fileUpload.getCharset() != null) {
        internal.addValue("; " + HttpHeaderValues.CHARSET + '=' + fileUpload.getCharset().name() + "\r\n\r\n");
      } else {
        internal.addValue("\r\n\r\n");
      }
      multipartHttpDatas.add(internal);
      multipartHttpDatas.add(data);
      globalBodySize += fileUpload.length() + internal.size();
    }
  }
  














  public HttpRequest finalizeRequest()
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    if (!headerFinalized) {
      if (isMultipart) {
        InternalAttribute internal = new InternalAttribute(charset);
        if (duringMixedMode) {
          internal.addValue("\r\n--" + multipartMixedBoundary + "--");
        }
        internal.addValue("\r\n--" + multipartDataBoundary + "--\r\n");
        multipartHttpDatas.add(internal);
        multipartMixedBoundary = null;
        currentFileUpload = null;
        duringMixedMode = false;
        globalBodySize += internal.size();
      }
      headerFinalized = true;
    } else {
      throw new ErrorDataEncoderException("Header already encoded");
    }
    
    HttpHeaders headers = request.headers();
    List<String> contentTypes = headers.getAll(HttpHeaderNames.CONTENT_TYPE);
    List<String> transferEncoding = headers.getAll(HttpHeaderNames.TRANSFER_ENCODING);
    if (contentTypes != null) {
      headers.remove(HttpHeaderNames.CONTENT_TYPE);
      for (String contentType : contentTypes)
      {
        lowercased = contentType.toLowerCase();
        if ((!lowercased.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) && 
          (!lowercased.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())))
        {

          headers.add(HttpHeaderNames.CONTENT_TYPE, contentType); }
      }
    }
    String lowercased;
    if (isMultipart) {
      String value = HttpHeaderValues.MULTIPART_FORM_DATA + "; " + HttpHeaderValues.BOUNDARY + '=' + multipartDataBoundary;
      
      headers.add(HttpHeaderNames.CONTENT_TYPE, value);
    }
    else {
      headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
    }
    
    long realSize = globalBodySize;
    if (!isMultipart) {
      realSize -= 1L;
    }
    iterator = multipartHttpDatas.listIterator();
    
    headers.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(realSize));
    if ((realSize > 8096L) || (isMultipart)) {
      isChunked = true;
      if (transferEncoding != null) {
        headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
        for (CharSequence v : transferEncoding) {
          if (!HttpHeaderValues.CHUNKED.contentEqualsIgnoreCase(v))
          {

            headers.add(HttpHeaderNames.TRANSFER_ENCODING, v);
          }
        }
      }
      HttpUtil.setTransferEncodingChunked(request, true);
      

      return new WrappedHttpRequest(request);
    }
    
    HttpContent chunk = nextChunk();
    if ((request instanceof FullHttpRequest)) {
      FullHttpRequest fullRequest = (FullHttpRequest)request;
      ByteBuf chunkContent = chunk.content();
      if (fullRequest.content() != chunkContent) {
        fullRequest.content().clear().writeBytes(chunkContent);
        chunkContent.release();
      }
      return fullRequest;
    }
    return new WrappedFullHttpRequest(request, chunk, null);
  }
  




  public boolean isChunked()
  {
    return isChunked;
  }
  






  private String encodeAttribute(String s, Charset charset)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    if (s == null) {
      return "";
    }
    try {
      String encoded = URLEncoder.encode(s, charset.name());
      if (encoderMode == EncoderMode.RFC3986) {
        for (Map.Entry<Pattern, String> entry : percentEncodings) {
          String replacement = (String)entry.getValue();
          encoded = ((Pattern)entry.getKey()).matcher(encoded).replaceAll(replacement);
        }
      }
      return encoded;
    } catch (UnsupportedEncodingException e) {
      throw new ErrorDataEncoderException(charset.name(), e);
    }
  }
  











  private boolean isKey = true;
  



  private ByteBuf fillByteBuf()
  {
    int length = currentBuffer.readableBytes();
    if (length > 8096) {
      return currentBuffer.readRetainedSlice(8096);
    }
    
    ByteBuf slice = currentBuffer;
    currentBuffer = null;
    return slice;
  }
  









  private HttpContent encodeNextChunkMultipart(int sizeleft)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    if (currentData == null) {
      return null;
    }
    
    if ((currentData instanceof InternalAttribute)) {
      ByteBuf buffer = ((InternalAttribute)currentData).toByteBuf();
      currentData = null;
    } else {
      try {
        buffer = ((HttpData)currentData).getChunk(sizeleft);
      } catch (IOException e) { ByteBuf buffer;
        throw new ErrorDataEncoderException(e);
      }
      if (buffer.capacity() == 0)
      {
        currentData = null;
        return null;
      }
    }
    if (currentBuffer == null) {
      currentBuffer = buffer;
    } else {
      currentBuffer = Unpooled.wrappedBuffer(new ByteBuf[] { currentBuffer, buffer });
    }
    if (currentBuffer.readableBytes() < 8096) {
      currentData = null;
      return null;
    }
    ByteBuf buffer = fillByteBuf();
    return new DefaultHttpContent(buffer);
  }
  








  private HttpContent encodeNextChunkUrlEncoded(int sizeleft)
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    if (currentData == null) {
      return null;
    }
    int size = sizeleft;
    


    if (isKey) {
      String key = currentData.getName();
      ByteBuf buffer = Unpooled.wrappedBuffer(key.getBytes(charset));
      isKey = false;
      if (currentBuffer == null) {
        currentBuffer = Unpooled.wrappedBuffer(new ByteBuf[] { buffer, Unpooled.wrappedBuffer("=".getBytes(charset)) });
      } else {
        currentBuffer = Unpooled.wrappedBuffer(new ByteBuf[] { currentBuffer, buffer, Unpooled.wrappedBuffer("=".getBytes(charset)) });
      }
      
      size -= buffer.readableBytes() + 1;
      if (currentBuffer.readableBytes() >= 8096) {
        buffer = fillByteBuf();
        return new DefaultHttpContent(buffer);
      }
    }
    
    try
    {
      buffer = ((HttpData)currentData).getChunk(size);
    } catch (IOException e) { ByteBuf buffer;
      throw new ErrorDataEncoderException(e);
    }
    

    ByteBuf delimiter = null;
    if (buffer.readableBytes() < size) {
      isKey = true;
      delimiter = iterator.hasNext() ? Unpooled.wrappedBuffer("&".getBytes(charset)) : null;
    }
    

    if (buffer.capacity() == 0) {
      currentData = null;
      if (currentBuffer == null) {
        if (delimiter == null) {
          return null;
        }
        currentBuffer = delimiter;

      }
      else if (delimiter != null) {
        currentBuffer = Unpooled.wrappedBuffer(new ByteBuf[] { currentBuffer, delimiter });
      }
      
      if (currentBuffer.readableBytes() >= 8096) {
        buffer = fillByteBuf();
        return new DefaultHttpContent(buffer);
      }
      return null;
    }
    

    if (currentBuffer == null) {
      if (delimiter != null) {
        currentBuffer = Unpooled.wrappedBuffer(new ByteBuf[] { buffer, delimiter });
      } else {
        currentBuffer = buffer;
      }
    }
    else if (delimiter != null) {
      currentBuffer = Unpooled.wrappedBuffer(new ByteBuf[] { currentBuffer, buffer, delimiter });
    } else {
      currentBuffer = Unpooled.wrappedBuffer(new ByteBuf[] { currentBuffer, buffer });
    }
    


    if (currentBuffer.readableBytes() < 8096) {
      currentData = null;
      isKey = true;
      return null;
    }
    
    ByteBuf buffer = fillByteBuf();
    return new DefaultHttpContent(buffer);
  }
  

  public void close()
    throws Exception
  {}
  
  @Deprecated
  public HttpContent readChunk(ChannelHandlerContext ctx)
    throws Exception
  {
    return readChunk(ctx.alloc());
  }
  







  public HttpContent readChunk(ByteBufAllocator allocator)
    throws Exception
  {
    if (isLastChunkSent) {
      return null;
    }
    HttpContent nextChunk = nextChunk();
    globalProgress += nextChunk.content().readableBytes();
    return nextChunk;
  }
  







  private HttpContent nextChunk()
    throws HttpPostRequestEncoder.ErrorDataEncoderException
  {
    if (isLastChunk) {
      isLastChunkSent = true;
      return LastHttpContent.EMPTY_LAST_CONTENT;
    }
    
    int size = calculateRemainingSize();
    if (size <= 0)
    {
      ByteBuf buffer = fillByteBuf();
      return new DefaultHttpContent(buffer);
    }
    
    if (currentData != null) {
      HttpContent chunk;
      HttpContent chunk;
      if (isMultipart) {
        chunk = encodeNextChunkMultipart(size);
      } else {
        chunk = encodeNextChunkUrlEncoded(size);
      }
      if (chunk != null)
      {
        return chunk;
      }
      size = calculateRemainingSize();
    }
    if (!iterator.hasNext()) {
      return lastChunk();
    }
    while ((size > 0) && (iterator.hasNext())) {
      currentData = ((InterfaceHttpData)iterator.next());
      HttpContent chunk;
      HttpContent chunk; if (isMultipart) {
        chunk = encodeNextChunkMultipart(size);
      } else {
        chunk = encodeNextChunkUrlEncoded(size);
      }
      if (chunk == null)
      {
        size = calculateRemainingSize();
      }
      else
      {
        return chunk;
      }
    }
    return lastChunk();
  }
  
  private int calculateRemainingSize() {
    int size = 8096;
    if (currentBuffer != null) {
      size -= currentBuffer.readableBytes();
    }
    return size;
  }
  
  private HttpContent lastChunk() {
    isLastChunk = true;
    if (currentBuffer == null) {
      isLastChunkSent = true;
      
      return LastHttpContent.EMPTY_LAST_CONTENT;
    }
    
    ByteBuf buffer = currentBuffer;
    currentBuffer = null;
    return new DefaultHttpContent(buffer);
  }
  
  public boolean isEndOfInput() throws Exception
  {
    return isLastChunkSent;
  }
  
  public long length()
  {
    return isMultipart ? globalBodySize : globalBodySize - 1L;
  }
  
  public long progress()
  {
    return globalProgress;
  }
  

  public static class ErrorDataEncoderException
    extends Exception
  {
    private static final long serialVersionUID = 5020247425493164465L;
    
    public ErrorDataEncoderException() {}
    
    public ErrorDataEncoderException(String msg)
    {
      super();
    }
    
    public ErrorDataEncoderException(Throwable cause) {
      super();
    }
    
    public ErrorDataEncoderException(String msg, Throwable cause) {
      super(cause);
    }
  }
  
  private static class WrappedHttpRequest implements HttpRequest {
    private final HttpRequest request;
    
    WrappedHttpRequest(HttpRequest request) { this.request = request; }
    

    public HttpRequest setProtocolVersion(HttpVersion version)
    {
      request.setProtocolVersion(version);
      return this;
    }
    
    public HttpRequest setMethod(HttpMethod method)
    {
      request.setMethod(method);
      return this;
    }
    
    public HttpRequest setUri(String uri)
    {
      request.setUri(uri);
      return this;
    }
    
    public HttpMethod getMethod()
    {
      return request.method();
    }
    
    public HttpMethod method()
    {
      return request.method();
    }
    
    public String getUri()
    {
      return request.uri();
    }
    
    public String uri()
    {
      return request.uri();
    }
    
    public HttpVersion getProtocolVersion()
    {
      return request.protocolVersion();
    }
    
    public HttpVersion protocolVersion()
    {
      return request.protocolVersion();
    }
    
    public HttpHeaders headers()
    {
      return request.headers();
    }
    
    public DecoderResult decoderResult()
    {
      return request.decoderResult();
    }
    
    @Deprecated
    public DecoderResult getDecoderResult()
    {
      return request.getDecoderResult();
    }
    
    public void setDecoderResult(DecoderResult result)
    {
      request.setDecoderResult(result);
    }
  }
  
  private static final class WrappedFullHttpRequest extends HttpPostRequestEncoder.WrappedHttpRequest implements FullHttpRequest {
    private final HttpContent content;
    
    private WrappedFullHttpRequest(HttpRequest request, HttpContent content) {
      super();
      this.content = content;
    }
    
    public FullHttpRequest setProtocolVersion(HttpVersion version)
    {
      super.setProtocolVersion(version);
      return this;
    }
    
    public FullHttpRequest setMethod(HttpMethod method)
    {
      super.setMethod(method);
      return this;
    }
    
    public FullHttpRequest setUri(String uri)
    {
      super.setUri(uri);
      return this;
    }
    
    public FullHttpRequest copy()
    {
      return replace(content().copy());
    }
    
    public FullHttpRequest duplicate()
    {
      return replace(content().duplicate());
    }
    
    public FullHttpRequest retainedDuplicate()
    {
      return replace(content().retainedDuplicate());
    }
    
    public FullHttpRequest replace(ByteBuf content)
    {
      DefaultFullHttpRequest duplicate = new DefaultFullHttpRequest(protocolVersion(), method(), uri(), content);
      duplicate.headers().set(headers());
      duplicate.trailingHeaders().set(trailingHeaders());
      return duplicate;
    }
    
    public FullHttpRequest retain(int increment)
    {
      content.retain(increment);
      return this;
    }
    
    public FullHttpRequest retain()
    {
      content.retain();
      return this;
    }
    
    public FullHttpRequest touch()
    {
      content.touch();
      return this;
    }
    
    public FullHttpRequest touch(Object hint)
    {
      content.touch(hint);
      return this;
    }
    
    public ByteBuf content()
    {
      return content.content();
    }
    
    public HttpHeaders trailingHeaders()
    {
      if ((content instanceof LastHttpContent)) {
        return ((LastHttpContent)content).trailingHeaders();
      }
      return EmptyHttpHeaders.INSTANCE;
    }
    

    public int refCnt()
    {
      return content.refCnt();
    }
    
    public boolean release()
    {
      return content.release();
    }
    
    public boolean release(int decrement)
    {
      return content.release(decrement);
    }
  }
}
