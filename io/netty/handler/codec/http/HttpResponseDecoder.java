package io.netty.handler.codec.http;









































































































public class HttpResponseDecoder
  extends HttpObjectDecoder
{
  private static final HttpResponseStatus UNKNOWN_STATUS = new HttpResponseStatus(999, "Unknown");
  





  public HttpResponseDecoder() {}
  




  public HttpResponseDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true);
  }
  
  public HttpResponseDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders);
  }
  

  public HttpResponseDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders, initialBufferSize);
  }
  


  public HttpResponseDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders, initialBufferSize, allowDuplicateContentLengths);
  }
  


  public HttpResponseDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths, boolean allowPartialChunks)
  {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders, initialBufferSize, allowDuplicateContentLengths, allowPartialChunks);
  }
  

  protected HttpMessage createMessage(String[] initialLine)
  {
    return new DefaultHttpResponse(
      HttpVersion.valueOf(initialLine[0]), 
      HttpResponseStatus.valueOf(Integer.parseInt(initialLine[1]), initialLine[2]), validateHeaders);
  }
  
  protected HttpMessage createInvalidMessage()
  {
    return new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, UNKNOWN_STATUS, validateHeaders);
  }
  
  protected boolean isDecodingRequest()
  {
    return false;
  }
}
