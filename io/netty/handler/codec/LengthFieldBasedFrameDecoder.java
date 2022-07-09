package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.nio.ByteOrder;
import java.util.List;































































































































































































public class LengthFieldBasedFrameDecoder
  extends ByteToMessageDecoder
{
  private final ByteOrder byteOrder;
  private final int maxFrameLength;
  private final int lengthFieldOffset;
  private final int lengthFieldLength;
  private final int lengthFieldEndOffset;
  private final int lengthAdjustment;
  private final int initialBytesToStrip;
  private final boolean failFast;
  private boolean discardingTooLongFrame;
  private long tooLongFrameLength;
  private long bytesToDiscard;
  
  public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength)
  {
    this(maxFrameLength, lengthFieldOffset, lengthFieldLength, 0, 0);
  }
  


















  public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip)
  {
    this(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, true);
  }
  



























  public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast)
  {
    this(ByteOrder.BIG_ENDIAN, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
  }
  





























  public LengthFieldBasedFrameDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast)
  {
    this.byteOrder = ((ByteOrder)ObjectUtil.checkNotNull(byteOrder, "byteOrder"));
    
    ObjectUtil.checkPositive(maxFrameLength, "maxFrameLength");
    
    ObjectUtil.checkPositiveOrZero(lengthFieldOffset, "lengthFieldOffset");
    
    ObjectUtil.checkPositiveOrZero(initialBytesToStrip, "initialBytesToStrip");
    
    if (lengthFieldOffset > maxFrameLength - lengthFieldLength) {
      throw new IllegalArgumentException("maxFrameLength (" + maxFrameLength + ") must be equal to or greater than lengthFieldOffset (" + lengthFieldOffset + ") + lengthFieldLength (" + lengthFieldLength + ").");
    }
    




    this.maxFrameLength = maxFrameLength;
    this.lengthFieldOffset = lengthFieldOffset;
    this.lengthFieldLength = lengthFieldLength;
    this.lengthAdjustment = lengthAdjustment;
    lengthFieldEndOffset = (lengthFieldOffset + lengthFieldLength);
    this.initialBytesToStrip = initialBytesToStrip;
    this.failFast = failFast;
  }
  
  protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    Object decoded = decode(ctx, in);
    if (decoded != null) {
      out.add(decoded);
    }
  }
  
  private void discardingTooLongFrame(ByteBuf in) {
    long bytesToDiscard = this.bytesToDiscard;
    int localBytesToDiscard = (int)Math.min(bytesToDiscard, in.readableBytes());
    in.skipBytes(localBytesToDiscard);
    bytesToDiscard -= localBytesToDiscard;
    this.bytesToDiscard = bytesToDiscard;
    
    failIfNecessary(false);
  }
  
  private static void failOnNegativeLengthField(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
    in.skipBytes(lengthFieldEndOffset);
    throw new CorruptedFrameException("negative pre-adjustment length field: " + frameLength);
  }
  


  private static void failOnFrameLengthLessThanLengthFieldEndOffset(ByteBuf in, long frameLength, int lengthFieldEndOffset)
  {
    in.skipBytes(lengthFieldEndOffset);
    throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less than lengthFieldEndOffset: " + lengthFieldEndOffset);
  }
  

  private void exceededFrameLength(ByteBuf in, long frameLength)
  {
    long discard = frameLength - in.readableBytes();
    tooLongFrameLength = frameLength;
    
    if (discard < 0L)
    {
      in.skipBytes((int)frameLength);
    }
    else {
      discardingTooLongFrame = true;
      bytesToDiscard = discard;
      in.skipBytes(in.readableBytes());
    }
    failIfNecessary(true);
  }
  

  private static void failOnFrameLengthLessThanInitialBytesToStrip(ByteBuf in, long frameLength, int initialBytesToStrip)
  {
    in.skipBytes((int)frameLength);
    throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less than initialBytesToStrip: " + initialBytesToStrip);
  }
  








  protected Object decode(ChannelHandlerContext ctx, ByteBuf in)
    throws Exception
  {
    if (discardingTooLongFrame) {
      discardingTooLongFrame(in);
    }
    
    if (in.readableBytes() < lengthFieldEndOffset) {
      return null;
    }
    
    int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
    long frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, byteOrder);
    
    if (frameLength < 0L) {
      failOnNegativeLengthField(in, frameLength, lengthFieldEndOffset);
    }
    
    frameLength += lengthAdjustment + lengthFieldEndOffset;
    
    if (frameLength < lengthFieldEndOffset) {
      failOnFrameLengthLessThanLengthFieldEndOffset(in, frameLength, lengthFieldEndOffset);
    }
    
    if (frameLength > maxFrameLength) {
      exceededFrameLength(in, frameLength);
      return null;
    }
    

    int frameLengthInt = (int)frameLength;
    if (in.readableBytes() < frameLengthInt) {
      return null;
    }
    
    if (initialBytesToStrip > frameLengthInt) {
      failOnFrameLengthLessThanInitialBytesToStrip(in, frameLength, initialBytesToStrip);
    }
    in.skipBytes(initialBytesToStrip);
    

    int readerIndex = in.readerIndex();
    int actualFrameLength = frameLengthInt - initialBytesToStrip;
    ByteBuf frame = extractFrame(ctx, in, readerIndex, actualFrameLength);
    in.readerIndex(readerIndex + actualFrameLength);
    return frame;
  }
  







  protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order)
  {
    buf = buf.order(order);
    long frameLength;
    long frameLength; long frameLength; long frameLength; long frameLength; switch (length) {
    case 1: 
      frameLength = buf.getUnsignedByte(offset);
      break;
    case 2: 
      frameLength = buf.getUnsignedShort(offset);
      break;
    case 3: 
      frameLength = buf.getUnsignedMedium(offset);
      break;
    case 4: 
      frameLength = buf.getUnsignedInt(offset);
      break;
    case 8: 
      frameLength = buf.getLong(offset);
      break;
    case 5: case 6: case 7: default: 
      throw new DecoderException("unsupported lengthFieldLength: " + lengthFieldLength + " (expected: 1, 2, 3, 4, or 8)");
    }
    long frameLength;
    return frameLength;
  }
  
  private void failIfNecessary(boolean firstDetectionOfTooLongFrame) {
    if (bytesToDiscard == 0L)
    {

      long tooLongFrameLength = this.tooLongFrameLength;
      this.tooLongFrameLength = 0L;
      discardingTooLongFrame = false;
      if ((!failFast) || (firstDetectionOfTooLongFrame)) {
        fail(tooLongFrameLength);
      }
      
    }
    else if ((failFast) && (firstDetectionOfTooLongFrame)) {
      fail(this.tooLongFrameLength);
    }
  }
  



  protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length)
  {
    return buffer.retainedSlice(index, length);
  }
  
  private void fail(long frameLength) {
    if (frameLength > 0L) {
      throw new TooLongFrameException("Adjusted frame length exceeds " + maxFrameLength + ": " + frameLength + " - discarded");
    }
    

    throw new TooLongFrameException("Adjusted frame length exceeds " + maxFrameLength + " - discarding");
  }
}
