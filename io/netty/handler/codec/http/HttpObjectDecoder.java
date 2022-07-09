package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.AppendableCharSequence;
import io.netty.util.internal.ObjectUtil;
import java.util.List;





















































































































public abstract class HttpObjectDecoder
  extends ByteToMessageDecoder
{
  public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;
  public static final int DEFAULT_MAX_HEADER_SIZE = 8192;
  public static final boolean DEFAULT_CHUNKED_SUPPORTED = true;
  public static final boolean DEFAULT_ALLOW_PARTIAL_CHUNKS = true;
  public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;
  public static final boolean DEFAULT_VALIDATE_HEADERS = true;
  public static final int DEFAULT_INITIAL_BUFFER_SIZE = 128;
  public static final boolean DEFAULT_ALLOW_DUPLICATE_CONTENT_LENGTHS = false;
  private static final String EMPTY_VALUE = "";
  private final int maxChunkSize;
  private final boolean chunkedSupported;
  private final boolean allowPartialChunks;
  protected final boolean validateHeaders;
  private final boolean allowDuplicateContentLengths;
  private final HeaderParser headerParser;
  private final LineParser lineParser;
  private HttpMessage message;
  private long chunkSize;
  private long contentLength = Long.MIN_VALUE;
  

  private volatile boolean resetRequested;
  
  private CharSequence name;
  
  private CharSequence value;
  
  private LastHttpContent trailer;
  

  private static enum State
  {
    SKIP_CONTROL_CHARS, 
    READ_INITIAL, 
    READ_HEADER, 
    READ_VARIABLE_LENGTH_CONTENT, 
    READ_FIXED_LENGTH_CONTENT, 
    READ_CHUNK_SIZE, 
    READ_CHUNKED_CONTENT, 
    READ_CHUNK_DELIMITER, 
    READ_CHUNK_FOOTER, 
    BAD_MESSAGE, 
    UPGRADED;
    
    private State() {} }
  private State currentState = State.SKIP_CONTROL_CHARS;
  




  protected HttpObjectDecoder()
  {
    this(4096, 8192, 8192, true);
  }
  




  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported)
  {
    this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, true);
  }
  




  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported, boolean validateHeaders)
  {
    this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, validateHeaders, 128);
  }
  





  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported, boolean validateHeaders, int initialBufferSize)
  {
    this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, validateHeaders, initialBufferSize, false);
  }
  






  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths)
  {
    this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, validateHeaders, initialBufferSize, allowDuplicateContentLengths, true);
  }
  






  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths, boolean allowPartialChunks)
  {
    ObjectUtil.checkPositive(maxInitialLineLength, "maxInitialLineLength");
    ObjectUtil.checkPositive(maxHeaderSize, "maxHeaderSize");
    ObjectUtil.checkPositive(maxChunkSize, "maxChunkSize");
    
    AppendableCharSequence seq = new AppendableCharSequence(initialBufferSize);
    lineParser = new LineParser(seq, maxInitialLineLength);
    headerParser = new HeaderParser(seq, maxHeaderSize);
    this.maxChunkSize = maxChunkSize;
    this.chunkedSupported = chunkedSupported;
    this.validateHeaders = validateHeaders;
    this.allowDuplicateContentLengths = allowDuplicateContentLengths;
    this.allowPartialChunks = allowPartialChunks;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception
  {
    if (resetRequested) {
      resetNow();
    }
    
    switch (1.$SwitchMap$io$netty$handler$codec$http$HttpObjectDecoder$State[currentState.ordinal()]) {
    case 1: 
    case 3: 
      try {
        AppendableCharSequence line = lineParser.parse(buffer);
        if (line == null) {
          return;
        }
        String[] initialLine = splitInitialLine(line);
        if (initialLine.length < 3)
        {
          currentState = State.SKIP_CONTROL_CHARS;
          return;
        }
        
        message = createMessage(initialLine);
        currentState = State.READ_HEADER;
      }
      catch (Exception e) {
        out.add(invalidMessage(buffer, e));
        return;
      }
    case 4:  try {
        State nextState = readHeaders(buffer);
        if (nextState == null) {
          return;
        }
        currentState = nextState;
        switch (1.$SwitchMap$io$netty$handler$codec$http$HttpObjectDecoder$State[nextState.ordinal()])
        {

        case 1: 
          out.add(message);
          out.add(LastHttpContent.EMPTY_LAST_CONTENT);
          resetNow();
          return;
        case 2: 
          if (!chunkedSupported) {
            throw new IllegalArgumentException("Chunked messages not supported");
          }
          
          out.add(message);
          return;
        }
        
        




        long contentLength = contentLength();
        if ((contentLength == 0L) || ((contentLength == -1L) && (isDecodingRequest()))) {
          out.add(message);
          out.add(LastHttpContent.EMPTY_LAST_CONTENT);
          resetNow();
          return;
        }
        
        assert ((nextState == State.READ_FIXED_LENGTH_CONTENT) || (nextState == State.READ_VARIABLE_LENGTH_CONTENT));
        

        out.add(message);
        
        if (nextState == State.READ_FIXED_LENGTH_CONTENT)
        {
          this.chunkSize = contentLength;
        }
        

        return;
      }
      catch (Exception e) {
        out.add(invalidMessage(buffer, e));
        return;
      }
    
    case 5: 
      int toRead = Math.min(buffer.readableBytes(), maxChunkSize);
      if (toRead > 0) {
        ByteBuf content = buffer.readRetainedSlice(toRead);
        out.add(new DefaultHttpContent(content));
      }
      return;
    
    case 6: 
      int readLimit = buffer.readableBytes();
      






      if (readLimit == 0) {
        return;
      }
      
      int toRead = Math.min(readLimit, maxChunkSize);
      if (toRead > this.chunkSize) {
        toRead = (int)this.chunkSize;
      }
      ByteBuf content = buffer.readRetainedSlice(toRead);
      this.chunkSize -= toRead;
      
      if (this.chunkSize == 0L)
      {
        out.add(new DefaultLastHttpContent(content, validateHeaders));
        resetNow();
      } else {
        out.add(new DefaultHttpContent(content));
      }
      return;
    


    case 2: 
      try
      {
        AppendableCharSequence line = lineParser.parse(buffer);
        if (line == null) {
          return;
        }
        int chunkSize = getChunkSize(line.toString());
        this.chunkSize = chunkSize;
        if (chunkSize == 0) {
          currentState = State.READ_CHUNK_FOOTER;
          return;
        }
        currentState = State.READ_CHUNKED_CONTENT;
      }
      catch (Exception e) {
        out.add(invalidChunk(buffer, e));
        return;
      }
    case 7: 
      assert (this.chunkSize <= 2147483647L);
      int toRead = Math.min((int)this.chunkSize, maxChunkSize);
      if ((!allowPartialChunks) && (buffer.readableBytes() < toRead)) {
        return;
      }
      toRead = Math.min(toRead, buffer.readableBytes());
      if (toRead == 0) {
        return;
      }
      HttpContent chunk = new DefaultHttpContent(buffer.readRetainedSlice(toRead));
      this.chunkSize -= toRead;
      
      out.add(chunk);
      
      if (this.chunkSize != 0L) {
        return;
      }
      currentState = State.READ_CHUNK_DELIMITER;
    

    case 8: 
      int wIdx = buffer.writerIndex();
      int rIdx = buffer.readerIndex();
      while (wIdx > rIdx) {
        byte next = buffer.getByte(rIdx++);
        if (next == 10) {
          currentState = State.READ_CHUNK_SIZE;
          break;
        }
      }
      buffer.readerIndex(rIdx);
      return;
    case 9: 
      try {
        LastHttpContent trailer = readTrailingHeaders(buffer);
        if (trailer == null) {
          return;
        }
        out.add(trailer);
        resetNow();
        return;
      } catch (Exception e) {
        out.add(invalidChunk(buffer, e));
        return;
      }
    
    case 10: 
      buffer.skipBytes(buffer.readableBytes());
      break;
    
    case 11: 
      int readableBytes = buffer.readableBytes();
      if (readableBytes > 0)
      {



        out.add(buffer.readBytes(readableBytes));
      }
      
      break;
    }
    
  }
  
  protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    super.decodeLast(ctx, in, out);
    
    if (resetRequested)
    {

      resetNow();
    }
    
    if (message != null) {
      boolean chunked = HttpUtil.isTransferEncodingChunked(message);
      if ((currentState == State.READ_VARIABLE_LENGTH_CONTENT) && (!in.isReadable()) && (!chunked))
      {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
        resetNow();
        return;
      }
      
      if (currentState == State.READ_HEADER)
      {

        out.add(invalidMessage(Unpooled.EMPTY_BUFFER, new PrematureChannelClosureException("Connection closed before received headers")));
        
        resetNow(); return;
      }
      
      boolean prematureClosure;
      
      boolean prematureClosure;
      if ((isDecodingRequest()) || (chunked))
      {
        prematureClosure = true;

      }
      else
      {
        prematureClosure = contentLength() > 0L;
      }
      
      if (!prematureClosure) {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      }
      resetNow();
    }
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    if ((evt instanceof HttpExpectationFailedEvent)) {
      switch (1.$SwitchMap$io$netty$handler$codec$http$HttpObjectDecoder$State[currentState.ordinal()]) {
      case 2: 
      case 5: 
      case 6: 
        reset();
        break;
      }
      
    }
    
    super.userEventTriggered(ctx, evt);
  }
  
  protected boolean isContentAlwaysEmpty(HttpMessage msg) {
    if ((msg instanceof HttpResponse)) {
      HttpResponse res = (HttpResponse)msg;
      int code = res.status().code();
      





      if ((code >= 100) && (code < 200))
      {
        return (code != 101) || (res.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT)) || 
          (!res.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true));
      }
      
      switch (code) {
      case 204: case 304: 
        return true;
      }
      return false;
    }
    
    return false;
  }
  



  protected boolean isSwitchingToNonHttp1Protocol(HttpResponse msg)
  {
    if (msg.status().code() != HttpResponseStatus.SWITCHING_PROTOCOLS.code()) {
      return false;
    }
    String newProtocol = msg.headers().get(HttpHeaderNames.UPGRADE);
    return (newProtocol == null) || (
      (!newProtocol.contains(HttpVersion.HTTP_1_0.text())) && 
      (!newProtocol.contains(HttpVersion.HTTP_1_1.text())));
  }
  



  public void reset()
  {
    resetRequested = true;
  }
  
  private void resetNow() {
    HttpMessage message = this.message;
    this.message = null;
    name = null;
    value = null;
    contentLength = Long.MIN_VALUE;
    lineParser.reset();
    headerParser.reset();
    trailer = null;
    if (!isDecodingRequest()) {
      HttpResponse res = (HttpResponse)message;
      if ((res != null) && (isSwitchingToNonHttp1Protocol(res))) {
        currentState = State.UPGRADED;
        return;
      }
    }
    
    resetRequested = false;
    currentState = State.SKIP_CONTROL_CHARS;
  }
  
  private HttpMessage invalidMessage(ByteBuf in, Exception cause) {
    currentState = State.BAD_MESSAGE;
    


    in.skipBytes(in.readableBytes());
    
    if (message == null) {
      message = createInvalidMessage();
    }
    message.setDecoderResult(DecoderResult.failure(cause));
    
    HttpMessage ret = message;
    message = null;
    return ret;
  }
  
  private HttpContent invalidChunk(ByteBuf in, Exception cause) {
    currentState = State.BAD_MESSAGE;
    


    in.skipBytes(in.readableBytes());
    
    HttpContent chunk = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
    chunk.setDecoderResult(DecoderResult.failure(cause));
    message = null;
    trailer = null;
    return chunk;
  }
  
  private State readHeaders(ByteBuf buffer) {
    HttpMessage message = this.message;
    HttpHeaders headers = message.headers();
    
    AppendableCharSequence line = headerParser.parse(buffer);
    if (line == null) {
      return null;
    }
    if (line.length() > 0) {
      do {
        char firstChar = line.charAtUnsafe(0);
        if ((name != null) && ((firstChar == ' ') || (firstChar == '\t')))
        {

          String trimmedLine = line.toString().trim();
          String valueStr = String.valueOf(value);
          value = (valueStr + ' ' + trimmedLine);
        } else {
          if (name != null) {
            headers.add(name, value);
          }
          splitHeader(line);
        }
        
        line = headerParser.parse(buffer);
        if (line == null) {
          return null;
        }
      } while (line.length() > 0);
    }
    

    if (name != null) {
      headers.add(name, value);
    }
    

    name = null;
    value = null;
    

    HttpMessageDecoderResult decoderResult = new HttpMessageDecoderResult(lineParser.size, headerParser.size);
    message.setDecoderResult(decoderResult);
    
    List<String> contentLengthFields = headers.getAll(HttpHeaderNames.CONTENT_LENGTH);
    if (!contentLengthFields.isEmpty()) {
      HttpVersion version = message.protocolVersion();
      
      boolean isHttp10OrEarlier = (version.majorVersion() < 1) || ((version.majorVersion() == 1) && (version.minorVersion() == 0));
      

      contentLength = HttpUtil.normalizeAndGetContentLength(contentLengthFields, isHttp10OrEarlier, allowDuplicateContentLengths);
      
      if (contentLength != -1L) {
        headers.set(HttpHeaderNames.CONTENT_LENGTH, Long.valueOf(contentLength));
      }
    }
    
    if (isContentAlwaysEmpty(message)) {
      HttpUtil.setTransferEncodingChunked(message, false);
      return State.SKIP_CONTROL_CHARS; }
    if (HttpUtil.isTransferEncodingChunked(message)) {
      if ((!contentLengthFields.isEmpty()) && (message.protocolVersion() == HttpVersion.HTTP_1_1)) {
        handleTransferEncodingChunkedWithContentLength(message);
      }
      return State.READ_CHUNK_SIZE; }
    if (contentLength() >= 0L) {
      return State.READ_FIXED_LENGTH_CONTENT;
    }
    return State.READ_VARIABLE_LENGTH_CONTENT;
  }
  





















  protected void handleTransferEncodingChunkedWithContentLength(HttpMessage message)
  {
    message.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
    contentLength = Long.MIN_VALUE;
  }
  
  private long contentLength() {
    if (contentLength == Long.MIN_VALUE) {
      contentLength = HttpUtil.getContentLength(message, -1L);
    }
    return contentLength;
  }
  
  private LastHttpContent readTrailingHeaders(ByteBuf buffer) {
    AppendableCharSequence line = headerParser.parse(buffer);
    if (line == null) {
      return null;
    }
    LastHttpContent trailer = this.trailer;
    if ((line.length() == 0) && (trailer == null))
    {

      return LastHttpContent.EMPTY_LAST_CONTENT;
    }
    
    CharSequence lastHeader = null;
    if (trailer == null) {
      trailer = this.trailer = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER, validateHeaders);
    }
    while (line.length() > 0) {
      char firstChar = line.charAtUnsafe(0);
      if ((lastHeader != null) && ((firstChar == ' ') || (firstChar == '\t'))) {
        List<String> current = trailer.trailingHeaders().getAll(lastHeader);
        if (!current.isEmpty()) {
          int lastPos = current.size() - 1;
          

          String lineTrimmed = line.toString().trim();
          String currentLastPos = (String)current.get(lastPos);
          current.set(lastPos, currentLastPos + lineTrimmed);
        }
      } else {
        splitHeader(line);
        CharSequence headerName = name;
        if ((!HttpHeaderNames.CONTENT_LENGTH.contentEqualsIgnoreCase(headerName)) && 
          (!HttpHeaderNames.TRANSFER_ENCODING.contentEqualsIgnoreCase(headerName)) && 
          (!HttpHeaderNames.TRAILER.contentEqualsIgnoreCase(headerName))) {
          trailer.trailingHeaders().add(headerName, value);
        }
        lastHeader = name;
        
        name = null;
        value = null;
      }
      line = headerParser.parse(buffer);
      if (line == null) {
        return null;
      }
    }
    
    this.trailer = null;
    return trailer; }
  
  protected abstract boolean isDecodingRequest();
  
  protected abstract HttpMessage createMessage(String[] paramArrayOfString) throws Exception;
  
  protected abstract HttpMessage createInvalidMessage();
  
  private static int getChunkSize(String hex) { hex = hex.trim();
    for (int i = 0; i < hex.length(); i++) {
      char c = hex.charAt(i);
      if ((c == ';') || (Character.isWhitespace(c)) || (Character.isISOControl(c))) {
        hex = hex.substring(0, i);
        break;
      }
    }
    
    return Integer.parseInt(hex, 16);
  }
  






  private static String[] splitInitialLine(AppendableCharSequence sb)
  {
    int aStart = findNonSPLenient(sb, 0);
    int aEnd = findSPLenient(sb, aStart);
    
    int bStart = findNonSPLenient(sb, aEnd);
    int bEnd = findSPLenient(sb, bStart);
    
    int cStart = findNonSPLenient(sb, bEnd);
    int cEnd = findEndOfString(sb);
    
    return new String[] {sb
      .subStringUnsafe(aStart, aEnd), sb
      .subStringUnsafe(bStart, bEnd), cStart < cEnd ? sb
      .subStringUnsafe(cStart, cEnd) : "" };
  }
  
  private void splitHeader(AppendableCharSequence sb) {
    int length = sb.length();
    





    int nameStart = findNonWhitespace(sb, 0, false);
    for (int nameEnd = nameStart; nameEnd < length; nameEnd++) {
      char ch = sb.charAtUnsafe(nameEnd);
      








      if (ch == ':') {
        break;
      }
      

      if ((!isDecodingRequest()) && (isOWS(ch))) {
        break;
      }
    }
    
    if (nameEnd == length)
    {
      throw new IllegalArgumentException("No colon found");
    }
    
    for (int colonEnd = nameEnd; colonEnd < length; colonEnd++) {
      if (sb.charAtUnsafe(colonEnd) == ':') {
        colonEnd++;
        break;
      }
    }
    
    name = sb.subStringUnsafe(nameStart, nameEnd);
    int valueStart = findNonWhitespace(sb, colonEnd, true);
    if (valueStart == length) {
      value = "";
    } else {
      int valueEnd = findEndOfString(sb);
      value = sb.subStringUnsafe(valueStart, valueEnd);
    }
  }
  
  private static int findNonSPLenient(AppendableCharSequence sb, int offset) {
    for (int result = offset; result < sb.length(); result++) {
      char c = sb.charAtUnsafe(result);
      
      if (!isSPLenient(c))
      {

        if (Character.isWhitespace(c))
        {
          throw new IllegalArgumentException("Invalid separator");
        }
        return result;
      } }
    return sb.length();
  }
  
  private static int findSPLenient(AppendableCharSequence sb, int offset) {
    for (int result = offset; result < sb.length(); result++) {
      if (isSPLenient(sb.charAtUnsafe(result))) {
        return result;
      }
    }
    return sb.length();
  }
  
  private static boolean isSPLenient(char c)
  {
    return (c == ' ') || (c == '\t') || (c == '\013') || (c == '\f') || (c == '\r');
  }
  
  private static int findNonWhitespace(AppendableCharSequence sb, int offset, boolean validateOWS) {
    for (int result = offset; result < sb.length(); result++) {
      char c = sb.charAtUnsafe(result);
      if (!Character.isWhitespace(c))
        return result;
      if ((validateOWS) && (!isOWS(c)))
      {

        throw new IllegalArgumentException("Invalid separator, only a single space or horizontal tab allowed, but received a '" + c + "' (0x" + Integer.toHexString(c) + ")");
      }
    }
    return sb.length();
  }
  
  private static int findEndOfString(AppendableCharSequence sb) {
    for (int result = sb.length() - 1; result > 0; result--) {
      if (!Character.isWhitespace(sb.charAtUnsafe(result))) {
        return result + 1;
      }
    }
    return 0;
  }
  
  private static boolean isOWS(char ch) {
    return (ch == ' ') || (ch == '\t');
  }
  
  private static class HeaderParser implements ByteProcessor {
    private final AppendableCharSequence seq;
    private final int maxLength;
    int size;
    
    HeaderParser(AppendableCharSequence seq, int maxLength) {
      this.seq = seq;
      this.maxLength = maxLength;
    }
    
    public AppendableCharSequence parse(ByteBuf buffer) {
      int oldSize = size;
      seq.reset();
      int i = buffer.forEachByte(this);
      if (i == -1) {
        size = oldSize;
        return null;
      }
      buffer.readerIndex(i + 1);
      return seq;
    }
    
    public void reset() {
      size = 0;
    }
    
    public boolean process(byte value) throws Exception
    {
      char nextByte = (char)(value & 0xFF);
      if (nextByte == '\n') {
        int len = seq.length();
        
        if ((len >= 1) && (seq.charAtUnsafe(len - 1) == '\r')) {
          size -= 1;
          seq.setLength(len - 1);
        }
        return false;
      }
      
      increaseCount();
      
      seq.append(nextByte);
      return true;
    }
    
    protected final void increaseCount() {
      if (++size > maxLength)
      {



        throw newException(maxLength);
      }
    }
    
    protected TooLongFrameException newException(int maxLength) {
      return new TooLongFrameException("HTTP header is larger than " + maxLength + " bytes.");
    }
  }
  
  private final class LineParser extends HttpObjectDecoder.HeaderParser
  {
    LineParser(AppendableCharSequence seq, int maxLength) {
      super(maxLength);
    }
    

    public AppendableCharSequence parse(ByteBuf buffer)
    {
      reset();
      return super.parse(buffer);
    }
    
    public boolean process(byte value) throws Exception
    {
      if (currentState == HttpObjectDecoder.State.SKIP_CONTROL_CHARS) {
        char c = (char)(value & 0xFF);
        if ((Character.isISOControl(c)) || (Character.isWhitespace(c))) {
          increaseCount();
          return true;
        }
        currentState = HttpObjectDecoder.State.READ_INITIAL;
      }
      return super.process(value);
    }
    
    protected TooLongFrameException newException(int maxLength)
    {
      return new TooLongFrameException("An HTTP line is larger than " + maxLength + " bytes.");
    }
  }
}
