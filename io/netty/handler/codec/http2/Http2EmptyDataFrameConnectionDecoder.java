package io.netty.handler.codec.http2;

import io.netty.util.internal.ObjectUtil;

















final class Http2EmptyDataFrameConnectionDecoder
  extends DecoratingHttp2ConnectionDecoder
{
  private final int maxConsecutiveEmptyFrames;
  
  Http2EmptyDataFrameConnectionDecoder(Http2ConnectionDecoder delegate, int maxConsecutiveEmptyFrames)
  {
    super(delegate);
    this.maxConsecutiveEmptyFrames = ObjectUtil.checkPositive(maxConsecutiveEmptyFrames, "maxConsecutiveEmptyFrames");
  }
  

  public void frameListener(Http2FrameListener listener)
  {
    if (listener != null) {
      super.frameListener(new Http2EmptyDataFrameListener(listener, maxConsecutiveEmptyFrames));
    } else {
      super.frameListener(null);
    }
  }
  
  public Http2FrameListener frameListener()
  {
    Http2FrameListener frameListener = frameListener0();
    
    if ((frameListener instanceof Http2EmptyDataFrameListener)) {
      return listener;
    }
    return frameListener;
  }
  
  Http2FrameListener frameListener0()
  {
    return super.frameListener();
  }
}
