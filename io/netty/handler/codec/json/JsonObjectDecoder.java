package io.netty.handler.codec.json;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.ObjectUtil;
import java.util.List;




































public class JsonObjectDecoder
  extends ByteToMessageDecoder
{
  private static final int ST_CORRUPTED = -1;
  private static final int ST_INIT = 0;
  private static final int ST_DECODING_NORMAL = 1;
  private static final int ST_DECODING_ARRAY_STREAM = 2;
  private int openBraces;
  private int idx;
  private int lastReaderIndex;
  private int state;
  private boolean insideString;
  private final int maxObjectLength;
  private final boolean streamArrayElements;
  
  public JsonObjectDecoder()
  {
    this(1048576);
  }
  
  public JsonObjectDecoder(int maxObjectLength) {
    this(maxObjectLength, false);
  }
  
  public JsonObjectDecoder(boolean streamArrayElements) {
    this(1048576, streamArrayElements);
  }
  








  public JsonObjectDecoder(int maxObjectLength, boolean streamArrayElements)
  {
    this.maxObjectLength = ObjectUtil.checkPositive(maxObjectLength, "maxObjectLength");
    this.streamArrayElements = streamArrayElements;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    if (state == -1) {
      in.skipBytes(in.readableBytes());
      return;
    }
    
    if ((this.idx > in.readerIndex()) && (lastReaderIndex != in.readerIndex())) {
      this.idx = (in.readerIndex() + (this.idx - lastReaderIndex));
    }
    

    int idx = this.idx;
    int wrtIdx = in.writerIndex();
    
    if (wrtIdx > maxObjectLength)
    {
      in.skipBytes(in.readableBytes());
      reset();
      throw new TooLongFrameException("object length exceeds " + maxObjectLength + ": " + wrtIdx + " bytes discarded");
    }
    for (; 
        
        idx < wrtIdx; idx++) {
      byte c = in.getByte(idx);
      if (state == 1) {
        decodeByte(c, in, idx);
        


        if (openBraces == 0) {
          ByteBuf json = extractObject(ctx, in, in.readerIndex(), idx + 1 - in.readerIndex());
          if (json != null) {
            out.add(json);
          }
          


          in.readerIndex(idx + 1);
          

          reset();
        }
      } else if (state == 2) {
        decodeByte(c, in, idx);
        
        if ((!insideString) && (((openBraces == 1) && (c == 44)) || ((openBraces == 0) && (c == 93))))
        {

          for (int i = in.readerIndex(); Character.isWhitespace(in.getByte(i)); i++) {
            in.skipBytes(1);
          }
          

          int idxNoSpaces = idx - 1;
          while ((idxNoSpaces >= in.readerIndex()) && (Character.isWhitespace(in.getByte(idxNoSpaces)))) {
            idxNoSpaces--;
          }
          
          ByteBuf json = extractObject(ctx, in, in.readerIndex(), idxNoSpaces + 1 - in.readerIndex());
          if (json != null) {
            out.add(json);
          }
          
          in.readerIndex(idx + 1);
          
          if (c == 93) {
            reset();
          }
        }
      }
      else if ((c == 123) || (c == 91)) {
        initDecoding(c);
        
        if (state == 2)
        {
          in.skipBytes(1);
        }
      }
      else if (Character.isWhitespace(c)) {
        in.skipBytes(1);
      } else {
        state = -1;
        
        throw new CorruptedFrameException("invalid JSON received at byte position " + idx + ": " + ByteBufUtil.hexDump(in));
      }
    }
    
    if (in.readableBytes() == 0) {
      this.idx = 0;
    } else {
      this.idx = idx;
    }
    lastReaderIndex = in.readerIndex();
  }
  



  protected ByteBuf extractObject(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length)
  {
    return buffer.retainedSlice(index, length);
  }
  
  private void decodeByte(byte c, ByteBuf in, int idx) {
    if (((c == 123) || (c == 91)) && (!insideString)) {
      openBraces += 1;
    } else if (((c == 125) || (c == 93)) && (!insideString)) {
      openBraces -= 1;
    } else if (c == 34)
    {

      if (!insideString) {
        insideString = true;
      } else {
        int backslashCount = 0;
        idx--;
        while ((idx >= 0) && 
          (in.getByte(idx) == 92)) {
          backslashCount++;
          idx--;
        }
        



        if (backslashCount % 2 == 0)
        {
          insideString = false;
        }
      }
    }
  }
  
  private void initDecoding(byte openingBrace) {
    openBraces = 1;
    if ((openingBrace == 91) && (streamArrayElements)) {
      state = 2;
    } else {
      state = 1;
    }
  }
  
  private void reset() {
    insideString = false;
    state = 0;
    openBraces = 0;
  }
}
