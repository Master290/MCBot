package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import java.util.Map.Entry;

















public class StompSubframeEncoder
  extends MessageToMessageEncoder<StompSubframe>
{
  public StompSubframeEncoder() {}
  
  protected void encode(ChannelHandlerContext ctx, StompSubframe msg, List<Object> out)
    throws Exception
  {
    if ((msg instanceof StompFrame)) {
      StompFrame stompFrame = (StompFrame)msg;
      ByteBuf buf = encodeFullFrame(stompFrame, ctx);
      
      out.add(convertFullFrame(stompFrame, buf));
    } else if ((msg instanceof StompHeadersSubframe)) {
      StompHeadersSubframe stompHeadersSubframe = (StompHeadersSubframe)msg;
      ByteBuf buf = ctx.alloc().buffer(headersSubFrameSize(stompHeadersSubframe));
      encodeHeaders(stompHeadersSubframe, buf);
      
      out.add(convertHeadersSubFrame(stompHeadersSubframe, buf));
    } else if ((msg instanceof StompContentSubframe)) {
      StompContentSubframe stompContentSubframe = (StompContentSubframe)msg;
      ByteBuf buf = encodeContent(stompContentSubframe, ctx);
      
      out.add(convertContentSubFrame(stompContentSubframe, buf));
    }
  }
  





  protected Object convertFullFrame(StompFrame original, ByteBuf encoded)
  {
    return encoded;
  }
  





  protected Object convertHeadersSubFrame(StompHeadersSubframe original, ByteBuf encoded)
  {
    return encoded;
  }
  





  protected Object convertContentSubFrame(StompContentSubframe original, ByteBuf encoded)
  {
    return encoded;
  }
  



  protected int headersSubFrameSize(StompHeadersSubframe headersSubframe)
  {
    int estimatedSize = headersSubframe.headers().size() * 34 + 48;
    if (estimatedSize < 128)
      return 128;
    if (estimatedSize < 256) {
      return 256;
    }
    
    return estimatedSize;
  }
  
  private ByteBuf encodeFullFrame(StompFrame frame, ChannelHandlerContext ctx) {
    int contentReadableBytes = frame.content().readableBytes();
    ByteBuf buf = ctx.alloc().buffer(headersSubFrameSize(frame) + contentReadableBytes);
    encodeHeaders(frame, buf);
    
    if (contentReadableBytes > 0) {
      buf.writeBytes(frame.content());
    }
    
    return buf.writeByte(0);
  }
  
  private static void encodeHeaders(StompHeadersSubframe frame, ByteBuf buf) {
    ByteBufUtil.writeUtf8(buf, frame.command().toString());
    buf.writeByte(10);
    
    for (Map.Entry<CharSequence, CharSequence> entry : frame.headers()) {
      ByteBufUtil.writeUtf8(buf, (CharSequence)entry.getKey());
      buf.writeByte(58);
      ByteBufUtil.writeUtf8(buf, (CharSequence)entry.getValue());
      buf.writeByte(10);
    }
    
    buf.writeByte(10);
  }
  
  private static ByteBuf encodeContent(StompContentSubframe content, ChannelHandlerContext ctx) {
    if ((content instanceof LastStompContentSubframe)) {
      ByteBuf buf = ctx.alloc().buffer(content.content().readableBytes() + 1);
      buf.writeBytes(content.content());
      buf.writeByte(0);
      return buf;
    }
    
    return content.content().retain();
  }
}
