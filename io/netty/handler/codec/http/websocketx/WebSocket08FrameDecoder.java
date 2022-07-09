package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteOrder;
import java.util.List;
























































public class WebSocket08FrameDecoder
  extends ByteToMessageDecoder
  implements WebSocketFrameDecoder
{
  static enum State
  {
    READING_FIRST, 
    READING_SECOND, 
    READING_SIZE, 
    MASKING_KEY, 
    PAYLOAD, 
    CORRUPT;
    
    private State() {} }
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocket08FrameDecoder.class);
  
  private static final byte OPCODE_CONT = 0;
  
  private static final byte OPCODE_TEXT = 1;
  
  private static final byte OPCODE_BINARY = 2;
  private static final byte OPCODE_CLOSE = 8;
  private static final byte OPCODE_PING = 9;
  private static final byte OPCODE_PONG = 10;
  private final WebSocketDecoderConfig config;
  private int fragmentedFramesCount;
  private boolean frameFinalFlag;
  private boolean frameMasked;
  private int frameRsv;
  private int frameOpcode;
  private long framePayloadLength;
  private byte[] maskingKey;
  private int framePayloadLen1;
  private boolean receivedClosingHandshake;
  private State state = State.READING_FIRST;
  











  public WebSocket08FrameDecoder(boolean expectMaskedFrames, boolean allowExtensions, int maxFramePayloadLength)
  {
    this(expectMaskedFrames, allowExtensions, maxFramePayloadLength, false);
  }
  















  public WebSocket08FrameDecoder(boolean expectMaskedFrames, boolean allowExtensions, int maxFramePayloadLength, boolean allowMaskMismatch)
  {
    this(WebSocketDecoderConfig.newBuilder()
      .expectMaskedFrames(expectMaskedFrames)
      .allowExtensions(allowExtensions)
      .maxFramePayloadLength(maxFramePayloadLength)
      .allowMaskMismatch(allowMaskMismatch)
      .build());
  }
  





  public WebSocket08FrameDecoder(WebSocketDecoderConfig decoderConfig)
  {
    config = ((WebSocketDecoderConfig)ObjectUtil.checkNotNull(decoderConfig, "decoderConfig"));
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    if (receivedClosingHandshake) {
      in.skipBytes(actualReadableBytes());
      return;
    }
    
    switch (1.$SwitchMap$io$netty$handler$codec$http$websocketx$WebSocket08FrameDecoder$State[state.ordinal()]) {
    case 1: 
      if (!in.isReadable()) {
        return;
      }
      
      framePayloadLength = 0L;
      

      byte b = in.readByte();
      frameFinalFlag = ((b & 0x80) != 0);
      frameRsv = ((b & 0x70) >> 4);
      frameOpcode = (b & 0xF);
      
      if (logger.isTraceEnabled()) {
        logger.trace("Decoding WebSocket Frame opCode={}", Integer.valueOf(frameOpcode));
      }
      
      state = State.READING_SECOND;
    case 2: 
      if (!in.isReadable()) {
        return;
      }
      
      byte b = in.readByte();
      frameMasked = ((b & 0x80) != 0);
      framePayloadLen1 = (b & 0x7F);
      
      if ((frameRsv != 0) && (!config.allowExtensions())) {
        protocolViolation(ctx, in, "RSV != 0 and no extension negotiated, RSV:" + frameRsv);
        return;
      }
      
      if ((!config.allowMaskMismatch()) && (config.expectMaskedFrames() != frameMasked)) {
        protocolViolation(ctx, in, "received a frame that is not masked as expected");
        return;
      }
      
      if (frameOpcode > 7)
      {

        if (!frameFinalFlag) {
          protocolViolation(ctx, in, "fragmented control frame");
          return;
        }
        

        if (framePayloadLen1 > 125) {
          protocolViolation(ctx, in, "control frame with payload length > 125 octets");
          return;
        }
        

        if ((frameOpcode != 8) && (frameOpcode != 9) && (frameOpcode != 10))
        {
          protocolViolation(ctx, in, "control frame using reserved opcode " + frameOpcode);
          return;
        }
        



        if ((frameOpcode == 8) && (framePayloadLen1 == 1)) {
          protocolViolation(ctx, in, "received close control frame with payload len 1");
        }
      }
      else
      {
        if ((frameOpcode != 0) && (frameOpcode != 1) && (frameOpcode != 2))
        {
          protocolViolation(ctx, in, "data frame using reserved opcode " + frameOpcode);
          return;
        }
        

        if ((fragmentedFramesCount == 0) && (frameOpcode == 0)) {
          protocolViolation(ctx, in, "received continuation data frame outside fragmented message");
          return;
        }
        

        if ((fragmentedFramesCount != 0) && (frameOpcode != 0)) {
          protocolViolation(ctx, in, "received non-continuation data frame while inside fragmented message");
          
          return;
        }
      }
      
      state = State.READING_SIZE;
    

    case 3: 
      if (framePayloadLen1 == 126) {
        if (in.readableBytes() < 2) {
          return;
        }
        framePayloadLength = in.readUnsignedShort();
        if (framePayloadLength < 126L) {
          protocolViolation(ctx, in, "invalid data frame length (not using minimal length encoding)");
        }
      }
      else if (framePayloadLen1 == 127) {
        if (in.readableBytes() < 8) {
          return;
        }
        framePayloadLength = in.readLong();
        


        if (framePayloadLength < 65536L) {
          protocolViolation(ctx, in, "invalid data frame length (not using minimal length encoding)");
        }
      }
      else {
        framePayloadLength = framePayloadLen1;
      }
      
      if (framePayloadLength > config.maxFramePayloadLength()) {
        protocolViolation(ctx, in, WebSocketCloseStatus.MESSAGE_TOO_BIG, "Max frame length of " + config
          .maxFramePayloadLength() + " has been exceeded.");
        return;
      }
      
      if (logger.isTraceEnabled()) {
        logger.trace("Decoding WebSocket Frame length={}", Long.valueOf(framePayloadLength));
      }
      
      state = State.MASKING_KEY;
    case 4: 
      if (frameMasked) {
        if (in.readableBytes() < 4) {
          return;
        }
        if (maskingKey == null) {
          maskingKey = new byte[4];
        }
        in.readBytes(maskingKey);
      }
      state = State.PAYLOAD;
    case 5: 
      if (in.readableBytes() < framePayloadLength) {
        return;
      }
      
      ByteBuf payloadBuffer = null;
      try {
        payloadBuffer = ByteBufUtil.readBytes(ctx.alloc(), in, toFrameLength(framePayloadLength));
        


        state = State.READING_FIRST;
        

        if (frameMasked) {
          unmask(payloadBuffer);
        }
        


        if (frameOpcode == 9) {
          out.add(new PingWebSocketFrame(frameFinalFlag, frameRsv, payloadBuffer));
          payloadBuffer = null;
          return;
        }
        if (frameOpcode == 10) {
          out.add(new PongWebSocketFrame(frameFinalFlag, frameRsv, payloadBuffer));
          payloadBuffer = null;
          return;
        }
        if (frameOpcode == 8) {
          receivedClosingHandshake = true;
          checkCloseFrameBody(ctx, payloadBuffer);
          out.add(new CloseWebSocketFrame(frameFinalFlag, frameRsv, payloadBuffer));
          payloadBuffer = null;
          return;
        }
        


        if (frameFinalFlag)
        {

          fragmentedFramesCount = 0;
        }
        else {
          fragmentedFramesCount += 1;
        }
        

        if (frameOpcode == 1) {
          out.add(new TextWebSocketFrame(frameFinalFlag, frameRsv, payloadBuffer));
          payloadBuffer = null;
          return; }
        if (frameOpcode == 2) {
          out.add(new BinaryWebSocketFrame(frameFinalFlag, frameRsv, payloadBuffer));
          payloadBuffer = null;
          return; }
        if (frameOpcode == 0) {
          out.add(new ContinuationWebSocketFrame(frameFinalFlag, frameRsv, payloadBuffer));
          
          payloadBuffer = null;
          return;
        }
        throw new UnsupportedOperationException("Cannot decode web socket frame with opcode: " + frameOpcode);
      }
      finally
      {
        if (payloadBuffer != null) {
          payloadBuffer.release();
        }
      }
    case 6: 
      if (in.isReadable())
      {

        in.readByte();
      }
      return;
    }
    throw new Error("Shouldn't reach here.");
  }
  
  private void unmask(ByteBuf frame)
  {
    int i = frame.readerIndex();
    int end = frame.writerIndex();
    
    ByteOrder order = frame.order();
    


    int intMask = (maskingKey[0] & 0xFF) << 24 | (maskingKey[1] & 0xFF) << 16 | (maskingKey[2] & 0xFF) << 8 | maskingKey[3] & 0xFF;
    





    if (order == ByteOrder.LITTLE_ENDIAN) {
      intMask = Integer.reverseBytes(intMask);
    }
    for (; 
        i + 3 < end; i += 4) {
      int unmasked = frame.getInt(i) ^ intMask;
      frame.setInt(i, unmasked);
    }
    for (; i < end; i++) {
      frame.setByte(i, frame.getByte(i) ^ maskingKey[(i % 4)]);
    }
  }
  
  private void protocolViolation(ChannelHandlerContext ctx, ByteBuf in, String reason) {
    protocolViolation(ctx, in, WebSocketCloseStatus.PROTOCOL_ERROR, reason);
  }
  
  private void protocolViolation(ChannelHandlerContext ctx, ByteBuf in, WebSocketCloseStatus status, String reason) {
    protocolViolation(ctx, in, new CorruptedWebSocketFrameException(status, reason));
  }
  
  private void protocolViolation(ChannelHandlerContext ctx, ByteBuf in, CorruptedWebSocketFrameException ex) {
    state = State.CORRUPT;
    int readableBytes = in.readableBytes();
    if (readableBytes > 0)
    {

      in.skipBytes(readableBytes);
    }
    if ((ctx.channel().isActive()) && (config.closeOnProtocolViolation())) { Object closeMessage;
      Object closeMessage;
      if (receivedClosingHandshake) {
        closeMessage = Unpooled.EMPTY_BUFFER;
      } else {
        WebSocketCloseStatus closeStatus = ex.closeStatus();
        String reasonText = ex.getMessage();
        if (reasonText == null) {
          reasonText = closeStatus.reasonText();
        }
        closeMessage = new CloseWebSocketFrame(closeStatus, reasonText);
      }
      ctx.writeAndFlush(closeMessage).addListener(ChannelFutureListener.CLOSE);
    }
    throw ex;
  }
  
  private static int toFrameLength(long l) {
    if (l > 2147483647L) {
      throw new TooLongFrameException("Length:" + l);
    }
    return (int)l;
  }
  


  protected void checkCloseFrameBody(ChannelHandlerContext ctx, ByteBuf buffer)
  {
    if ((buffer == null) || (!buffer.isReadable())) {
      return;
    }
    if (buffer.readableBytes() == 1) {
      protocolViolation(ctx, buffer, WebSocketCloseStatus.INVALID_PAYLOAD_DATA, "Invalid close frame body");
    }
    

    int idx = buffer.readerIndex();
    buffer.readerIndex(0);
    

    int statusCode = buffer.readShort();
    if (!WebSocketCloseStatus.isValidStatusCode(statusCode)) {
      protocolViolation(ctx, buffer, "Invalid close frame getStatus code: " + statusCode);
    }
    

    if (buffer.isReadable()) {
      try {
        new Utf8Validator().check(buffer);
      } catch (CorruptedWebSocketFrameException ex) {
        protocolViolation(ctx, buffer, ex);
      }
    }
    

    buffer.readerIndex(idx);
  }
}
