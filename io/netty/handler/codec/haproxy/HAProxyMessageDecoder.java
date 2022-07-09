package io.netty.handler.codec.haproxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.util.CharsetUtil;
import java.util.List;








































public class HAProxyMessageDecoder
  extends ByteToMessageDecoder
{
  private static final int V1_MAX_LENGTH = 108;
  private static final int V2_MAX_LENGTH = 65551;
  private static final int V2_MIN_LENGTH = 232;
  private static final int V2_MAX_TLV = 65319;
  private static final int BINARY_PREFIX_LENGTH = HAProxyConstants.BINARY_PREFIX.length;
  




  private static final ProtocolDetectionResult<HAProxyProtocolVersion> DETECTION_RESULT_V1 = ProtocolDetectionResult.detected(HAProxyProtocolVersion.V1);
  




  private static final ProtocolDetectionResult<HAProxyProtocolVersion> DETECTION_RESULT_V2 = ProtocolDetectionResult.detected(HAProxyProtocolVersion.V2);
  



  private HeaderExtractor headerExtractor;
  



  private boolean discarding;
  



  private int discardedBytes;
  



  private final boolean failFast;
  



  private boolean finished;
  



  private int version = -1;
  



  private final int v2MaxHeaderSize;
  




  public HAProxyMessageDecoder()
  {
    this(true);
  }
  





  public HAProxyMessageDecoder(boolean failFast)
  {
    v2MaxHeaderSize = 65551;
    this.failFast = failFast;
  }
  










  public HAProxyMessageDecoder(int maxTlvSize)
  {
    this(maxTlvSize, true);
  }
  






  public HAProxyMessageDecoder(int maxTlvSize, boolean failFast)
  {
    if (maxTlvSize < 1) {
      v2MaxHeaderSize = 232;
    } else if (maxTlvSize > 65319) {
      v2MaxHeaderSize = 65551;
    } else {
      int calcMax = maxTlvSize + 232;
      if (calcMax > 65551) {
        v2MaxHeaderSize = 65551;
      } else {
        v2MaxHeaderSize = calcMax;
      }
    }
    this.failFast = failFast;
  }
  



  private static int findVersion(ByteBuf buffer)
  {
    int n = buffer.readableBytes();
    
    if (n < 13) {
      return -1;
    }
    
    int idx = buffer.readerIndex();
    return match(HAProxyConstants.BINARY_PREFIX, buffer, idx) ? buffer.getByte(idx + BINARY_PREFIX_LENGTH) : 1;
  }
  



  private static int findEndOfHeader(ByteBuf buffer)
  {
    int n = buffer.readableBytes();
    

    if (n < 16) {
      return -1;
    }
    
    int offset = buffer.readerIndex() + 14;
    

    int totalHeaderBytes = 16 + buffer.getUnsignedShort(offset);
    

    if (n >= totalHeaderBytes) {
      return totalHeaderBytes;
    }
    return -1;
  }
  




  private static int findEndOfLine(ByteBuf buffer)
  {
    int n = buffer.writerIndex();
    for (int i = buffer.readerIndex(); i < n; i++) {
      byte b = buffer.getByte(i);
      if ((b == 13) && (i < n - 1) && (buffer.getByte(i + 1) == 10)) {
        return i;
      }
    }
    return -1;
  }
  


  public boolean isSingleDecode()
  {
    return true;
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    super.channelRead(ctx, msg);
    if (finished) {
      ctx.pipeline().remove(this);
    }
  }
  
  protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    if ((version == -1) && 
      ((this.version = findVersion(in)) == -1)) {
      return;
    }
    
    ByteBuf decoded;
    
    ByteBuf decoded;
    if (version == 1) {
      decoded = decodeLine(ctx, in);
    } else {
      decoded = decodeStruct(ctx, in);
    }
    
    if (decoded != null) {
      finished = true;
      try {
        if (version == 1) {
          out.add(HAProxyMessage.decodeHeader(decoded.toString(CharsetUtil.US_ASCII)));
        } else {
          out.add(HAProxyMessage.decodeHeader(decoded));
        }
      } catch (HAProxyProtocolException e) {
        fail(ctx, null, e);
      }
    }
  }
  






  private ByteBuf decodeStruct(ChannelHandlerContext ctx, ByteBuf buffer)
    throws Exception
  {
    if (headerExtractor == null) {
      headerExtractor = new StructHeaderExtractor(v2MaxHeaderSize);
    }
    return headerExtractor.extract(ctx, buffer);
  }
  






  private ByteBuf decodeLine(ChannelHandlerContext ctx, ByteBuf buffer)
    throws Exception
  {
    if (headerExtractor == null) {
      headerExtractor = new LineHeaderExtractor(108);
    }
    return headerExtractor.extract(ctx, buffer);
  }
  
  private void failOverLimit(ChannelHandlerContext ctx, int length) {
    failOverLimit(ctx, String.valueOf(length));
  }
  
  private void failOverLimit(ChannelHandlerContext ctx, String length) {
    int maxLength = version == 1 ? 108 : v2MaxHeaderSize;
    fail(ctx, "header length (" + length + ") exceeds the allowed maximum (" + maxLength + ')', null);
  }
  
  private void fail(ChannelHandlerContext ctx, String errMsg, Exception e) {
    finished = true;
    ctx.close();
    HAProxyProtocolException ppex;
    HAProxyProtocolException ppex; if ((errMsg != null) && (e != null)) {
      ppex = new HAProxyProtocolException(errMsg, e); } else { HAProxyProtocolException ppex;
      if (errMsg != null) {
        ppex = new HAProxyProtocolException(errMsg); } else { HAProxyProtocolException ppex;
        if (e != null) {
          ppex = new HAProxyProtocolException(e);
        } else
          ppex = new HAProxyProtocolException();
      } }
    throw ppex;
  }
  


  public static ProtocolDetectionResult<HAProxyProtocolVersion> detectProtocol(ByteBuf buffer)
  {
    if (buffer.readableBytes() < 12) {
      return ProtocolDetectionResult.needsMoreData();
    }
    
    int idx = buffer.readerIndex();
    
    if (match(HAProxyConstants.BINARY_PREFIX, buffer, idx)) {
      return DETECTION_RESULT_V2;
    }
    if (match(HAProxyConstants.TEXT_PREFIX, buffer, idx)) {
      return DETECTION_RESULT_V1;
    }
    return ProtocolDetectionResult.invalid();
  }
  
  private static boolean match(byte[] prefix, ByteBuf buffer, int idx) {
    for (int i = 0; i < prefix.length; i++) {
      byte b = buffer.getByte(idx + i);
      if (b != prefix[i]) {
        return false;
      }
    }
    return true;
  }
  

  private abstract class HeaderExtractor
  {
    private final int maxHeaderSize;
    

    protected HeaderExtractor(int maxHeaderSize)
    {
      this.maxHeaderSize = maxHeaderSize;
    }
    







    public ByteBuf extract(ChannelHandlerContext ctx, ByteBuf buffer)
      throws Exception
    {
      int eoh = findEndOfHeader(buffer);
      if (!discarding) {
        if (eoh >= 0) {
          int length = eoh - buffer.readerIndex();
          if (length > maxHeaderSize) {
            buffer.readerIndex(eoh + delimiterLength(buffer, eoh));
            HAProxyMessageDecoder.this.failOverLimit(ctx, length);
            return null;
          }
          ByteBuf frame = buffer.readSlice(length);
          buffer.skipBytes(delimiterLength(buffer, eoh));
          return frame;
        }
        int length = buffer.readableBytes();
        if (length > maxHeaderSize) {
          discardedBytes = length;
          buffer.skipBytes(length);
          discarding = true;
          if (failFast) {
            HAProxyMessageDecoder.this.failOverLimit(ctx, "over " + discardedBytes);
          }
        }
        return null;
      }
      
      if (eoh >= 0) {
        int length = discardedBytes + eoh - buffer.readerIndex();
        buffer.readerIndex(eoh + delimiterLength(buffer, eoh));
        discardedBytes = 0;
        discarding = false;
        if (!failFast) {
          HAProxyMessageDecoder.this.failOverLimit(ctx, "over " + length);
        }
      } else {
        discardedBytes = (discardedBytes + buffer.readableBytes());
        buffer.skipBytes(buffer.readableBytes());
      }
      return null;
    }
    





    protected abstract int findEndOfHeader(ByteBuf paramByteBuf);
    




    protected abstract int delimiterLength(ByteBuf paramByteBuf, int paramInt);
  }
  




  private final class LineHeaderExtractor
    extends HAProxyMessageDecoder.HeaderExtractor
  {
    LineHeaderExtractor(int maxHeaderSize)
    {
      super(maxHeaderSize);
    }
    
    protected int findEndOfHeader(ByteBuf buffer)
    {
      return HAProxyMessageDecoder.findEndOfLine(buffer);
    }
    
    protected int delimiterLength(ByteBuf buffer, int eoh)
    {
      return buffer.getByte(eoh) == 13 ? 2 : 1;
    }
  }
  
  private final class StructHeaderExtractor extends HAProxyMessageDecoder.HeaderExtractor
  {
    StructHeaderExtractor(int maxHeaderSize) {
      super(maxHeaderSize);
    }
    
    protected int findEndOfHeader(ByteBuf buffer)
    {
      return HAProxyMessageDecoder.findEndOfHeader(buffer);
    }
    
    protected int delimiterLength(ByteBuf buffer, int eoh)
    {
      return 0;
    }
  }
}
