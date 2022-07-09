package io.netty.handler.codec.http;











































public class HttpRequestDecoder
  extends HttpObjectDecoder
{
  public HttpRequestDecoder() {}
  










































  public HttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true);
  }
  
  public HttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders);
  }
  

  public HttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders, initialBufferSize);
  }
  


  public HttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders, initialBufferSize, allowDuplicateContentLengths);
  }
  


  public HttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths, boolean allowPartialChunks)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders, initialBufferSize, allowDuplicateContentLengths, allowPartialChunks);
  }
  
  protected HttpMessage createMessage(String[] initialLine)
    throws Exception
  {
    return new DefaultHttpRequest(
      HttpVersion.valueOf(initialLine[2]), 
      HttpMethod.valueOf(initialLine[0]), initialLine[1], validateHeaders);
  }
  
  protected HttpMessage createInvalidMessage()
  {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/bad-request", validateHeaders);
  }
  
  protected boolean isDecodingRequest()
  {
    return true;
  }
}
