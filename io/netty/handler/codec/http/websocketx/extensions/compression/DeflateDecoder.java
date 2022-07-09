package io.netty.handler.codec.http.websocketx.extensions.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionDecoder;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionFilter;
import io.netty.util.internal.ObjectUtil;
import java.util.List;



















abstract class DeflateDecoder
  extends WebSocketExtensionDecoder
{
  static final ByteBuf FRAME_TAIL = Unpooled.unreleasableBuffer(
    Unpooled.wrappedBuffer(new byte[] { 0, 0, -1, -1 }))
    .asReadOnly();
  
  static final ByteBuf EMPTY_DEFLATE_BLOCK = Unpooled.unreleasableBuffer(
    Unpooled.wrappedBuffer(new byte[] { 0 }))
    .asReadOnly();
  

  private final boolean noContext;
  

  private final WebSocketExtensionFilter extensionDecoderFilter;
  

  private EmbeddedChannel decoder;
  

  DeflateDecoder(boolean noContext, WebSocketExtensionFilter extensionDecoderFilter)
  {
    this.noContext = noContext;
    this.extensionDecoderFilter = ((WebSocketExtensionFilter)ObjectUtil.checkNotNull(extensionDecoderFilter, "extensionDecoderFilter"));
  }
  


  protected WebSocketExtensionFilter extensionDecoderFilter()
  {
    return extensionDecoderFilter;
  }
  
  protected abstract boolean appendFrameTail(WebSocketFrame paramWebSocketFrame);
  
  protected abstract int newRsv(WebSocketFrame paramWebSocketFrame);
  
  protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception
  {
    ByteBuf decompressedContent = decompressContent(ctx, msg);
    
    WebSocketFrame outMsg;
    if ((msg instanceof TextWebSocketFrame)) {
      outMsg = new TextWebSocketFrame(msg.isFinalFragment(), newRsv(msg), decompressedContent); } else { WebSocketFrame outMsg;
      if ((msg instanceof BinaryWebSocketFrame)) {
        outMsg = new BinaryWebSocketFrame(msg.isFinalFragment(), newRsv(msg), decompressedContent); } else { WebSocketFrame outMsg;
        if ((msg instanceof ContinuationWebSocketFrame)) {
          outMsg = new ContinuationWebSocketFrame(msg.isFinalFragment(), newRsv(msg), decompressedContent);
        } else
          throw new CodecException("unexpected frame type: " + msg.getClass().getName());
      } }
    WebSocketFrame outMsg;
    out.add(outMsg);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    cleanup();
    super.handlerRemoved(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    cleanup();
    super.channelInactive(ctx);
  }
  
  private ByteBuf decompressContent(ChannelHandlerContext ctx, WebSocketFrame msg) {
    if (decoder == null) {
      if ((!(msg instanceof TextWebSocketFrame)) && (!(msg instanceof BinaryWebSocketFrame))) {
        throw new CodecException("unexpected initial frame type: " + msg.getClass().getName());
      }
      decoder = new EmbeddedChannel(new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(ZlibWrapper.NONE) });
    }
    
    boolean readable = msg.content().isReadable();
    boolean emptyDeflateBlock = EMPTY_DEFLATE_BLOCK.equals(msg.content());
    
    decoder.writeInbound(new Object[] { msg.content().retain() });
    if (appendFrameTail(msg)) {
      decoder.writeInbound(new Object[] { FRAME_TAIL.duplicate() });
    }
    
    CompositeByteBuf compositeDecompressedContent = ctx.alloc().compositeBuffer();
    for (;;) {
      ByteBuf partUncompressedContent = (ByteBuf)decoder.readInbound();
      if (partUncompressedContent == null) {
        break;
      }
      if (!partUncompressedContent.isReadable()) {
        partUncompressedContent.release();
      }
      else {
        compositeDecompressedContent.addComponent(true, partUncompressedContent);
      }
    }
    
    if ((!emptyDeflateBlock) && (readable) && (compositeDecompressedContent.numComponents() <= 0))
    {

      if (!(msg instanceof ContinuationWebSocketFrame)) {
        compositeDecompressedContent.release();
        throw new CodecException("cannot read uncompressed buffer");
      }
    }
    
    if ((msg.isFinalFragment()) && (noContext)) {
      cleanup();
    }
    
    return compositeDecompressedContent;
  }
  
  private void cleanup() {
    if (decoder != null)
    {
      decoder.finishAndReleaseAll();
      decoder = null;
    }
  }
}
