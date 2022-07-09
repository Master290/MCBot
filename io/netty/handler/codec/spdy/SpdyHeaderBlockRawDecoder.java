package io.netty.handler.codec.spdy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.ObjectUtil;




















public class SpdyHeaderBlockRawDecoder
  extends SpdyHeaderBlockDecoder
{
  private static final int LENGTH_FIELD_SIZE = 4;
  private final int maxHeaderSize;
  private State state;
  private ByteBuf cumulation;
  private int headerSize;
  private int numHeaders;
  private int length;
  private String name;
  
  private static enum State
  {
    READ_NUM_HEADERS, 
    READ_NAME_LENGTH, 
    READ_NAME, 
    SKIP_NAME, 
    READ_VALUE_LENGTH, 
    READ_VALUE, 
    SKIP_VALUE, 
    END_HEADER_BLOCK, 
    ERROR;
    
    private State() {} }
  
  public SpdyHeaderBlockRawDecoder(SpdyVersion spdyVersion, int maxHeaderSize) { ObjectUtil.checkNotNull(spdyVersion, "spdyVersion");
    this.maxHeaderSize = maxHeaderSize;
    state = State.READ_NUM_HEADERS;
  }
  
  private static int readLengthField(ByteBuf buffer) {
    int length = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex());
    buffer.skipBytes(4);
    return length;
  }
  
  void decode(ByteBufAllocator alloc, ByteBuf headerBlock, SpdyHeadersFrame frame) throws Exception
  {
    ObjectUtil.checkNotNull(headerBlock, "headerBlock");
    ObjectUtil.checkNotNull(frame, "frame");
    
    if (cumulation == null) {
      decodeHeaderBlock(headerBlock, frame);
      if (headerBlock.isReadable()) {
        cumulation = alloc.buffer(headerBlock.readableBytes());
        cumulation.writeBytes(headerBlock);
      }
    } else {
      cumulation.writeBytes(headerBlock);
      decodeHeaderBlock(cumulation, frame);
      if (cumulation.isReadable()) {
        cumulation.discardReadBytes();
      } else {
        releaseBuffer();
      }
    }
  }
  
  protected void decodeHeaderBlock(ByteBuf headerBlock, SpdyHeadersFrame frame) throws Exception
  {
    while (headerBlock.isReadable()) {
      switch (1.$SwitchMap$io$netty$handler$codec$spdy$SpdyHeaderBlockRawDecoder$State[state.ordinal()]) {
      case 1: 
        if (headerBlock.readableBytes() < 4) {
          return;
        }
        
        numHeaders = readLengthField(headerBlock);
        
        if (numHeaders < 0) {
          state = State.ERROR;
          frame.setInvalid();
        } else if (numHeaders == 0) {
          state = State.END_HEADER_BLOCK;
        } else {
          state = State.READ_NAME_LENGTH;
        }
        break;
      
      case 2: 
        if (headerBlock.readableBytes() < 4) {
          return;
        }
        
        length = readLengthField(headerBlock);
        

        if (length <= 0) {
          state = State.ERROR;
          frame.setInvalid();
        } else if ((length > maxHeaderSize) || (headerSize > maxHeaderSize - length)) {
          headerSize = (maxHeaderSize + 1);
          state = State.SKIP_NAME;
          frame.setTruncated();
        } else {
          headerSize += length;
          state = State.READ_NAME;
        }
        break;
      
      case 3: 
        if (headerBlock.readableBytes() < length) {
          return;
        }
        
        byte[] nameBytes = new byte[length];
        headerBlock.readBytes(nameBytes);
        name = new String(nameBytes, "UTF-8");
        

        if (frame.headers().contains(name)) {
          state = State.ERROR;
          frame.setInvalid();
        } else {
          state = State.READ_VALUE_LENGTH;
        }
        break;
      
      case 4: 
        int skipLength = Math.min(headerBlock.readableBytes(), length);
        headerBlock.skipBytes(skipLength);
        length -= skipLength;
        
        if (length == 0) {
          state = State.READ_VALUE_LENGTH;
        }
        
        break;
      case 5: 
        if (headerBlock.readableBytes() < 4) {
          return;
        }
        
        length = readLengthField(headerBlock);
        

        if (length < 0) {
          state = State.ERROR;
          frame.setInvalid();
        } else if (length == 0) {
          if (!frame.isTruncated())
          {
            frame.headers().add(name, "");
          }
          
          name = null;
          if (--numHeaders == 0) {
            state = State.END_HEADER_BLOCK;
          } else {
            state = State.READ_NAME_LENGTH;
          }
        }
        else if ((length > maxHeaderSize) || (headerSize > maxHeaderSize - length)) {
          headerSize = (maxHeaderSize + 1);
          name = null;
          state = State.SKIP_VALUE;
          frame.setTruncated();
        } else {
          headerSize += length;
          state = State.READ_VALUE;
        }
        break;
      
      case 6: 
        if (headerBlock.readableBytes() < length) {
          return;
        }
        
        byte[] valueBytes = new byte[length];
        headerBlock.readBytes(valueBytes);
        

        int index = 0;
        int offset = 0;
        

        if (valueBytes[0] == 0) {
          state = State.ERROR;
          frame.setInvalid();
        }
        else
        {
          while (index < length) {
            while ((index < valueBytes.length) && (valueBytes[index] != 0)) {
              index++;
            }
            if (index < valueBytes.length)
            {
              if ((index + 1 == valueBytes.length) || (valueBytes[(index + 1)] == 0))
              {


                state = State.ERROR;
                frame.setInvalid();
                break;
              }
            }
            String value = new String(valueBytes, offset, index - offset, "UTF-8");
            try
            {
              frame.headers().add(name, value);
            }
            catch (IllegalArgumentException e) {
              state = State.ERROR;
              frame.setInvalid();
              break;
            }
            index++;
            offset = index;
          }
          
          name = null;
          

          if (state != State.ERROR)
          {


            if (--numHeaders == 0) {
              state = State.END_HEADER_BLOCK;
            } else
              state = State.READ_NAME_LENGTH; }
        }
        break;
      
      case 7: 
        int skipLength = Math.min(headerBlock.readableBytes(), length);
        headerBlock.skipBytes(skipLength);
        length -= skipLength;
        
        if (length == 0) {
          if (--numHeaders == 0) {
            state = State.END_HEADER_BLOCK;
          } else {
            state = State.READ_NAME_LENGTH;
          }
        }
        
        break;
      case 8: 
        state = State.ERROR;
        frame.setInvalid();
        break;
      
      case 9: 
        headerBlock.skipBytes(headerBlock.readableBytes());
        return;
      
      default: 
        throw new Error("Shouldn't reach here.");
      }
    }
  }
  
  void endHeaderBlock(SpdyHeadersFrame frame) throws Exception
  {
    if (state != State.END_HEADER_BLOCK) {
      frame.setInvalid();
    }
    
    releaseBuffer();
    

    headerSize = 0;
    name = null;
    state = State.READ_NUM_HEADERS;
  }
  
  void end()
  {
    releaseBuffer();
  }
  
  private void releaseBuffer() {
    if (cumulation != null) {
      cumulation.release();
      cumulation = null;
    }
  }
}
