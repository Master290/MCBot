package io.netty.handler.codec.http2;

public abstract interface Http2PushPromiseFrame
  extends Http2StreamFrame
{
  public abstract Http2StreamFrame pushStream(Http2FrameStream paramHttp2FrameStream);
  
  public abstract Http2FrameStream pushStream();
  
  public abstract Http2Headers http2Headers();
  
  public abstract int padding();
  
  public abstract int promisedStreamId();
  
  public abstract Http2PushPromiseFrame stream(Http2FrameStream paramHttp2FrameStream);
}
