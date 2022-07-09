package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;

















final class Http2EmptyDataFrameListener
  extends Http2FrameListenerDecorator
{
  private final int maxConsecutiveEmptyFrames;
  private boolean violationDetected;
  private int emptyDataFrames;
  
  Http2EmptyDataFrameListener(Http2FrameListener listener, int maxConsecutiveEmptyFrames)
  {
    super(listener);
    this.maxConsecutiveEmptyFrames = ObjectUtil.checkPositive(maxConsecutiveEmptyFrames, "maxConsecutiveEmptyFrames");
  }
  

  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
    throws Http2Exception
  {
    if ((endOfStream) || (data.isReadable())) {
      emptyDataFrames = 0;
    } else if ((emptyDataFrames++ == maxConsecutiveEmptyFrames) && (!violationDetected)) {
      violationDetected = true;
      throw Http2Exception.connectionError(Http2Error.ENHANCE_YOUR_CALM, "Maximum number %d of empty data frames without end_of_stream flag received", new Object[] {
      
        Integer.valueOf(maxConsecutiveEmptyFrames) });
    }
    
    return super.onDataRead(ctx, streamId, data, padding, endOfStream);
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream)
    throws Http2Exception
  {
    emptyDataFrames = 0;
    super.onHeadersRead(ctx, streamId, headers, padding, endStream);
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream)
    throws Http2Exception
  {
    emptyDataFrames = 0;
    super.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
  }
}
