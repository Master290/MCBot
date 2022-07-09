package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Random;

























































public class WebSocket08FrameEncoder
  extends MessageToMessageEncoder<WebSocketFrame>
  implements WebSocketFrameEncoder
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocket08FrameEncoder.class);
  

  private static final byte OPCODE_CONT = 0;
  

  private static final byte OPCODE_TEXT = 1;
  

  private static final byte OPCODE_BINARY = 2;
  

  private static final byte OPCODE_CLOSE = 8;
  

  private static final byte OPCODE_PING = 9;
  

  private static final byte OPCODE_PONG = 10;
  
  private static final int GATHERING_WRITE_THRESHOLD = 1024;
  
  private final boolean maskPayload;
  

  public WebSocket08FrameEncoder(boolean maskPayload)
  {
    this.maskPayload = maskPayload;
  }
  
  protected void encode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception
  {
    ByteBuf data = msg.content();
    
    byte opcode;
    
    if ((msg instanceof TextWebSocketFrame)) {
      opcode = 1; } else { byte opcode;
      if ((msg instanceof PingWebSocketFrame)) {
        opcode = 9; } else { byte opcode;
        if ((msg instanceof PongWebSocketFrame)) {
          opcode = 10; } else { byte opcode;
          if ((msg instanceof CloseWebSocketFrame)) {
            opcode = 8; } else { byte opcode;
            if ((msg instanceof BinaryWebSocketFrame)) {
              opcode = 2; } else { byte opcode;
              if ((msg instanceof ContinuationWebSocketFrame)) {
                opcode = 0;
              } else
                throw new UnsupportedOperationException("Cannot encode frame of type: " + msg.getClass().getName());
            } } } } }
    byte opcode;
    int length = data.readableBytes();
    
    if (logger.isTraceEnabled()) {
      logger.trace("Encoding WebSocket Frame opCode={} length={}", Byte.valueOf(opcode), Integer.valueOf(length));
    }
    
    int b0 = 0;
    if (msg.isFinalFragment()) {
      b0 |= 0x80;
    }
    b0 |= msg.rsv() % 8 << 4;
    b0 |= opcode % 128;
    
    if ((opcode == 9) && (length > 125)) {
      throw new TooLongFrameException("invalid payload for PING (payload length must be <= 125, was " + length);
    }
    

    boolean release = true;
    ByteBuf buf = null;
    try {
      int maskLength = maskPayload ? 4 : 0;
      if (length <= 125) {
        int size = 2 + maskLength;
        if ((maskPayload) || (length <= 1024)) {
          size += length;
        }
        buf = ctx.alloc().buffer(size);
        buf.writeByte(b0);
        byte b = (byte)(maskPayload ? 0x80 | (byte)length : (byte)length);
        buf.writeByte(b);
      } else if (length <= 65535) {
        int size = 4 + maskLength;
        if ((maskPayload) || (length <= 1024)) {
          size += length;
        }
        buf = ctx.alloc().buffer(size);
        buf.writeByte(b0);
        buf.writeByte(maskPayload ? 254 : 126);
        buf.writeByte(length >>> 8 & 0xFF);
        buf.writeByte(length & 0xFF);
      } else {
        int size = 10 + maskLength;
        if ((maskPayload) || (length <= 1024)) {
          size += length;
        }
        buf = ctx.alloc().buffer(size);
        buf.writeByte(b0);
        buf.writeByte(maskPayload ? 255 : 127);
        buf.writeLong(length);
      }
      

      if (maskPayload) {
        int random = PlatformDependent.threadLocalRandom().nextInt(Integer.MAX_VALUE);
        byte[] mask = ByteBuffer.allocate(4).putInt(random).array();
        buf.writeBytes(mask);
        
        ByteOrder srcOrder = data.order();
        ByteOrder dstOrder = buf.order();
        
        int counter = 0;
        int i = data.readerIndex();
        int end = data.writerIndex();
        
        if (srcOrder == dstOrder)
        {


          int intMask = (mask[0] & 0xFF) << 24 | (mask[1] & 0xFF) << 16 | (mask[2] & 0xFF) << 8 | mask[3] & 0xFF;
          





          if (srcOrder == ByteOrder.LITTLE_ENDIAN) {
            intMask = Integer.reverseBytes(intMask);
          }
          for (; 
              i + 3 < end; i += 4) {
            int intData = data.getInt(i);
            buf.writeInt(intData ^ intMask);
          }
        }
        for (; i < end; i++) {
          byte byteData = data.getByte(i);
          buf.writeByte(byteData ^ mask[(counter++ % 4)]);
        }
        out.add(buf);
      }
      else if (buf.writableBytes() >= data.readableBytes())
      {
        buf.writeBytes(data);
        out.add(buf);
      } else {
        out.add(buf);
        out.add(data.retain());
      }
      
      release = false;
    } finally {
      if ((release) && (buf != null)) {
        buf.release();
      }
    }
  }
}
