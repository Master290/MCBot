package io.netty.handler.codec.http2;







public final class Http2FrameStreamEvent
{
  private final Http2FrameStream stream;
  





  private final Type type;
  





  static enum Type
  {
    State, 
    Writability;
    
    private Type() {} }
  
  private Http2FrameStreamEvent(Http2FrameStream stream, Type type) { this.stream = stream;
    this.type = type;
  }
  
  public Http2FrameStream stream() {
    return stream;
  }
  
  public Type type() {
    return type;
  }
  
  static Http2FrameStreamEvent stateChanged(Http2FrameStream stream) {
    return new Http2FrameStreamEvent(stream, Type.State);
  }
  
  static Http2FrameStreamEvent writabilityChanged(Http2FrameStream stream) {
    return new Http2FrameStreamEvent(stream, Type.Writability);
  }
}
