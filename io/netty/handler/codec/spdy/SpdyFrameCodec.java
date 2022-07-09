package io.netty.handler.codec.spdy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import java.net.SocketAddress;
import java.util.List;


















public class SpdyFrameCodec
  extends ByteToMessageDecoder
  implements SpdyFrameDecoderDelegate, ChannelOutboundHandler
{
  private static final SpdyProtocolException INVALID_FRAME = new SpdyProtocolException("Received invalid frame");
  

  private final SpdyFrameDecoder spdyFrameDecoder;
  
  private final SpdyFrameEncoder spdyFrameEncoder;
  
  private final SpdyHeaderBlockDecoder spdyHeaderBlockDecoder;
  
  private final SpdyHeaderBlockEncoder spdyHeaderBlockEncoder;
  
  private SpdyHeadersFrame spdyHeadersFrame;
  
  private SpdySettingsFrame spdySettingsFrame;
  
  private ChannelHandlerContext ctx;
  
  private boolean read;
  
  private final boolean validateHeaders;
  

  public SpdyFrameCodec(SpdyVersion version)
  {
    this(version, true);
  }
  







  public SpdyFrameCodec(SpdyVersion version, boolean validateHeaders)
  {
    this(version, 8192, 16384, 6, 15, 8, validateHeaders);
  }
  





  public SpdyFrameCodec(SpdyVersion version, int maxChunkSize, int maxHeaderSize, int compressionLevel, int windowBits, int memLevel)
  {
    this(version, maxChunkSize, maxHeaderSize, compressionLevel, windowBits, memLevel, true);
  }
  





  public SpdyFrameCodec(SpdyVersion version, int maxChunkSize, int maxHeaderSize, int compressionLevel, int windowBits, int memLevel, boolean validateHeaders)
  {
    this(version, maxChunkSize, 
      SpdyHeaderBlockDecoder.newInstance(version, maxHeaderSize), 
      SpdyHeaderBlockEncoder.newInstance(version, compressionLevel, windowBits, memLevel), validateHeaders);
  }
  

  protected SpdyFrameCodec(SpdyVersion version, int maxChunkSize, SpdyHeaderBlockDecoder spdyHeaderBlockDecoder, SpdyHeaderBlockEncoder spdyHeaderBlockEncoder, boolean validateHeaders)
  {
    spdyFrameDecoder = new SpdyFrameDecoder(version, this, maxChunkSize);
    spdyFrameEncoder = new SpdyFrameEncoder(version);
    this.spdyHeaderBlockDecoder = spdyHeaderBlockDecoder;
    this.spdyHeaderBlockEncoder = spdyHeaderBlockEncoder;
    this.validateHeaders = validateHeaders;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    super.handlerAdded(ctx);
    this.ctx = ctx;
    ctx.channel().closeFuture().addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) throws Exception {
        spdyHeaderBlockDecoder.end();
        spdyHeaderBlockEncoder.end();
      }
    });
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    spdyFrameDecoder.decode(in);
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    if ((!read) && 
      (!ctx.channel().config().isAutoRead())) {
      ctx.read();
    }
    
    read = false;
    super.channelReadComplete(ctx);
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.disconnect(promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.close(promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.deregister(promise);
  }
  
  public void read(ChannelHandlerContext ctx) throws Exception
  {
    ctx.read();
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    ctx.flush();
  }
  

  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    if ((msg instanceof SpdyDataFrame))
    {
      SpdyDataFrame spdyDataFrame = (SpdyDataFrame)msg;
      ByteBuf frame = spdyFrameEncoder.encodeDataFrame(ctx
        .alloc(), spdyDataFrame
        .streamId(), spdyDataFrame
        .isLast(), spdyDataFrame
        .content());
      
      spdyDataFrame.release();
      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdySynStreamFrame))
    {
      SpdySynStreamFrame spdySynStreamFrame = (SpdySynStreamFrame)msg;
      ByteBuf headerBlock = spdyHeaderBlockEncoder.encode(ctx.alloc(), spdySynStreamFrame);
      try {
        frame = spdyFrameEncoder.encodeSynStreamFrame(ctx
          .alloc(), spdySynStreamFrame
          .streamId(), spdySynStreamFrame
          .associatedStreamId(), spdySynStreamFrame
          .priority(), spdySynStreamFrame
          .isLast(), spdySynStreamFrame
          .isUnidirectional(), headerBlock);
      }
      finally {
        ByteBuf frame;
        headerBlock.release(); }
      ByteBuf frame;
      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdySynReplyFrame))
    {
      SpdySynReplyFrame spdySynReplyFrame = (SpdySynReplyFrame)msg;
      ByteBuf headerBlock = spdyHeaderBlockEncoder.encode(ctx.alloc(), spdySynReplyFrame);
      try {
        frame = spdyFrameEncoder.encodeSynReplyFrame(ctx
          .alloc(), spdySynReplyFrame
          .streamId(), spdySynReplyFrame
          .isLast(), headerBlock);
      }
      finally {
        ByteBuf frame;
        headerBlock.release(); }
      ByteBuf frame;
      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdyRstStreamFrame))
    {
      SpdyRstStreamFrame spdyRstStreamFrame = (SpdyRstStreamFrame)msg;
      ByteBuf frame = spdyFrameEncoder.encodeRstStreamFrame(ctx
        .alloc(), spdyRstStreamFrame
        .streamId(), spdyRstStreamFrame
        .status().code());
      
      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdySettingsFrame))
    {
      SpdySettingsFrame spdySettingsFrame = (SpdySettingsFrame)msg;
      ByteBuf frame = spdyFrameEncoder.encodeSettingsFrame(ctx
        .alloc(), spdySettingsFrame);
      

      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdyPingFrame))
    {
      SpdyPingFrame spdyPingFrame = (SpdyPingFrame)msg;
      ByteBuf frame = spdyFrameEncoder.encodePingFrame(ctx
        .alloc(), spdyPingFrame
        .id());
      
      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdyGoAwayFrame))
    {
      SpdyGoAwayFrame spdyGoAwayFrame = (SpdyGoAwayFrame)msg;
      ByteBuf frame = spdyFrameEncoder.encodeGoAwayFrame(ctx
        .alloc(), spdyGoAwayFrame
        .lastGoodStreamId(), spdyGoAwayFrame
        .status().code());
      
      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdyHeadersFrame))
    {
      SpdyHeadersFrame spdyHeadersFrame = (SpdyHeadersFrame)msg;
      ByteBuf headerBlock = spdyHeaderBlockEncoder.encode(ctx.alloc(), spdyHeadersFrame);
      try {
        frame = spdyFrameEncoder.encodeHeadersFrame(ctx
          .alloc(), spdyHeadersFrame
          .streamId(), spdyHeadersFrame
          .isLast(), headerBlock);
      }
      finally {
        ByteBuf frame;
        headerBlock.release(); }
      ByteBuf frame;
      ctx.write(frame, promise);
    }
    else if ((msg instanceof SpdyWindowUpdateFrame))
    {
      SpdyWindowUpdateFrame spdyWindowUpdateFrame = (SpdyWindowUpdateFrame)msg;
      ByteBuf frame = spdyFrameEncoder.encodeWindowUpdateFrame(ctx
        .alloc(), spdyWindowUpdateFrame
        .streamId(), spdyWindowUpdateFrame
        .deltaWindowSize());
      
      ctx.write(frame, promise);
    } else {
      throw new UnsupportedMessageTypeException(msg, new Class[0]);
    }
    ByteBuf frame;
  }
  
  public void readDataFrame(int streamId, boolean last, ByteBuf data) {
    read = true;
    
    SpdyDataFrame spdyDataFrame = new DefaultSpdyDataFrame(streamId, data);
    spdyDataFrame.setLast(last);
    ctx.fireChannelRead(spdyDataFrame);
  }
  

  public void readSynStreamFrame(int streamId, int associatedToStreamId, byte priority, boolean last, boolean unidirectional)
  {
    SpdySynStreamFrame spdySynStreamFrame = new DefaultSpdySynStreamFrame(streamId, associatedToStreamId, priority, validateHeaders);
    
    spdySynStreamFrame.setLast(last);
    spdySynStreamFrame.setUnidirectional(unidirectional);
    spdyHeadersFrame = spdySynStreamFrame;
  }
  
  public void readSynReplyFrame(int streamId, boolean last)
  {
    SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId, validateHeaders);
    spdySynReplyFrame.setLast(last);
    spdyHeadersFrame = spdySynReplyFrame;
  }
  
  public void readRstStreamFrame(int streamId, int statusCode)
  {
    read = true;
    
    SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, statusCode);
    ctx.fireChannelRead(spdyRstStreamFrame);
  }
  
  public void readSettingsFrame(boolean clearPersisted)
  {
    read = true;
    
    spdySettingsFrame = new DefaultSpdySettingsFrame();
    spdySettingsFrame.setClearPreviouslyPersistedSettings(clearPersisted);
  }
  
  public void readSetting(int id, int value, boolean persistValue, boolean persisted)
  {
    spdySettingsFrame.setValue(id, value, persistValue, persisted);
  }
  
  public void readSettingsEnd()
  {
    read = true;
    
    Object frame = spdySettingsFrame;
    spdySettingsFrame = null;
    ctx.fireChannelRead(frame);
  }
  
  public void readPingFrame(int id)
  {
    read = true;
    
    SpdyPingFrame spdyPingFrame = new DefaultSpdyPingFrame(id);
    ctx.fireChannelRead(spdyPingFrame);
  }
  
  public void readGoAwayFrame(int lastGoodStreamId, int statusCode)
  {
    read = true;
    
    SpdyGoAwayFrame spdyGoAwayFrame = new DefaultSpdyGoAwayFrame(lastGoodStreamId, statusCode);
    ctx.fireChannelRead(spdyGoAwayFrame);
  }
  
  public void readHeadersFrame(int streamId, boolean last)
  {
    spdyHeadersFrame = new DefaultSpdyHeadersFrame(streamId, validateHeaders);
    spdyHeadersFrame.setLast(last);
  }
  
  public void readWindowUpdateFrame(int streamId, int deltaWindowSize)
  {
    read = true;
    
    SpdyWindowUpdateFrame spdyWindowUpdateFrame = new DefaultSpdyWindowUpdateFrame(streamId, deltaWindowSize);
    ctx.fireChannelRead(spdyWindowUpdateFrame);
  }
  
  public void readHeaderBlock(ByteBuf headerBlock)
  {
    try {
      spdyHeaderBlockDecoder.decode(ctx.alloc(), headerBlock, spdyHeadersFrame);
    } catch (Exception e) {
      ctx.fireExceptionCaught(e);
    } finally {
      headerBlock.release();
    }
  }
  
  public void readHeaderBlockEnd()
  {
    Object frame = null;
    try {
      spdyHeaderBlockDecoder.endHeaderBlock(spdyHeadersFrame);
      frame = spdyHeadersFrame;
      spdyHeadersFrame = null;
    } catch (Exception e) {
      ctx.fireExceptionCaught(e);
    }
    if (frame != null) {
      read = true;
      
      ctx.fireChannelRead(frame);
    }
  }
  
  public void readFrameError(String message)
  {
    ctx.fireExceptionCaught(INVALID_FRAME);
  }
}
