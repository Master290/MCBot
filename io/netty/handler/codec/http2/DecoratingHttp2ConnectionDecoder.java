package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.util.List;



















public class DecoratingHttp2ConnectionDecoder
  implements Http2ConnectionDecoder
{
  private final Http2ConnectionDecoder delegate;
  
  public DecoratingHttp2ConnectionDecoder(Http2ConnectionDecoder delegate)
  {
    this.delegate = ((Http2ConnectionDecoder)ObjectUtil.checkNotNull(delegate, "delegate"));
  }
  
  public void lifecycleManager(Http2LifecycleManager lifecycleManager)
  {
    delegate.lifecycleManager(lifecycleManager);
  }
  
  public Http2Connection connection()
  {
    return delegate.connection();
  }
  
  public Http2LocalFlowController flowController()
  {
    return delegate.flowController();
  }
  
  public void frameListener(Http2FrameListener listener)
  {
    delegate.frameListener(listener);
  }
  
  public Http2FrameListener frameListener()
  {
    return delegate.frameListener();
  }
  
  public void decodeFrame(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Http2Exception
  {
    delegate.decodeFrame(ctx, in, out);
  }
  
  public Http2Settings localSettings()
  {
    return delegate.localSettings();
  }
  
  public boolean prefaceReceived()
  {
    return delegate.prefaceReceived();
  }
  
  public void close()
  {
    delegate.close();
  }
}
