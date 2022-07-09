package io.netty.handler.codec.stomp;

import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.ObjectUtil;


















public class DefaultStompHeadersSubframe
  implements StompHeadersSubframe
{
  protected final StompCommand command;
  protected DecoderResult decoderResult = DecoderResult.SUCCESS;
  protected final DefaultStompHeaders headers;
  
  public DefaultStompHeadersSubframe(StompCommand command) {
    this(command, null);
  }
  
  DefaultStompHeadersSubframe(StompCommand command, DefaultStompHeaders headers) {
    this.command = ((StompCommand)ObjectUtil.checkNotNull(command, "command"));
    this.headers = (headers == null ? new DefaultStompHeaders() : headers);
  }
  
  public StompCommand command()
  {
    return command;
  }
  
  public StompHeaders headers()
  {
    return headers;
  }
  
  public DecoderResult decoderResult()
  {
    return decoderResult;
  }
  
  public void setDecoderResult(DecoderResult decoderResult)
  {
    this.decoderResult = decoderResult;
  }
  
  public String toString()
  {
    return "StompFrame{command=" + command + ", headers=" + headers + '}';
  }
}
