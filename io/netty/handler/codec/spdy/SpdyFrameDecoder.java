package io.netty.handler.codec.spdy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;














































public class SpdyFrameDecoder
{
  private final int spdyVersion;
  private final int maxChunkSize;
  private final SpdyFrameDecoderDelegate delegate;
  private State state;
  private byte flags;
  private int length;
  private int streamId;
  private int numSettings;
  
  private static enum State
  {
    READ_COMMON_HEADER, 
    READ_DATA_FRAME, 
    READ_SYN_STREAM_FRAME, 
    READ_SYN_REPLY_FRAME, 
    READ_RST_STREAM_FRAME, 
    READ_SETTINGS_FRAME, 
    READ_SETTING, 
    READ_PING_FRAME, 
    READ_GOAWAY_FRAME, 
    READ_HEADERS_FRAME, 
    READ_WINDOW_UPDATE_FRAME, 
    READ_HEADER_BLOCK, 
    DISCARD_FRAME, 
    FRAME_ERROR;
    

    private State() {}
  }
  
  public SpdyFrameDecoder(SpdyVersion spdyVersion, SpdyFrameDecoderDelegate delegate)
  {
    this(spdyVersion, delegate, 8192);
  }
  


  public SpdyFrameDecoder(SpdyVersion spdyVersion, SpdyFrameDecoderDelegate delegate, int maxChunkSize)
  {
    this.spdyVersion = ((SpdyVersion)ObjectUtil.checkNotNull(spdyVersion, "spdyVersion")).getVersion();
    this.delegate = ((SpdyFrameDecoderDelegate)ObjectUtil.checkNotNull(delegate, "delegate"));
    this.maxChunkSize = ObjectUtil.checkPositive(maxChunkSize, "maxChunkSize");
    state = State.READ_COMMON_HEADER;
  }
  

  public void decode(ByteBuf buffer)
  {
    for (;;)
    {
      switch (1.$SwitchMap$io$netty$handler$codec$spdy$SpdyFrameDecoder$State[state.ordinal()]) {
      case 1: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        
        int frameOffset = buffer.readerIndex();
        int flagsOffset = frameOffset + 4;
        int lengthOffset = frameOffset + 5;
        buffer.skipBytes(8);
        
        boolean control = (buffer.getByte(frameOffset) & 0x80) != 0;
        
        int version;
        int type;
        if (control)
        {
          int version = SpdyCodecUtil.getUnsignedShort(buffer, frameOffset) & 0x7FFF;
          int type = SpdyCodecUtil.getUnsignedShort(buffer, frameOffset + 2);
          streamId = 0;
        }
        else {
          version = spdyVersion;
          type = 0;
          streamId = SpdyCodecUtil.getUnsignedInt(buffer, frameOffset);
        }
        
        flags = buffer.getByte(flagsOffset);
        length = SpdyCodecUtil.getUnsignedMedium(buffer, lengthOffset);
        

        if (version != spdyVersion) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid SPDY Version");
        } else if (!isValidFrameHeader(streamId, type, flags, length)) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid Frame Error");
        } else {
          state = getNextState(type, length);
        }
        break;
      
      case 2: 
        if (length == 0) {
          state = State.READ_COMMON_HEADER;
          delegate.readDataFrame(streamId, hasFlag(flags, (byte)1), Unpooled.buffer(0));

        }
        else
        {
          int dataLength = Math.min(maxChunkSize, length);
          

          if (buffer.readableBytes() < dataLength) {
            return;
          }
          
          ByteBuf data = buffer.alloc().buffer(dataLength);
          data.writeBytes(buffer, dataLength);
          length -= dataLength;
          
          if (length == 0) {
            state = State.READ_COMMON_HEADER;
          }
          
          boolean last = (length == 0) && (hasFlag(flags, (byte)1));
          
          delegate.readDataFrame(streamId, last, data); }
        break;
      
      case 3: 
        if (buffer.readableBytes() < 10) {
          return;
        }
        
        int offset = buffer.readerIndex();
        streamId = SpdyCodecUtil.getUnsignedInt(buffer, offset);
        int associatedToStreamId = SpdyCodecUtil.getUnsignedInt(buffer, offset + 4);
        byte priority = (byte)(buffer.getByte(offset + 8) >> 5 & 0x7);
        boolean last = hasFlag(flags, (byte)1);
        boolean unidirectional = hasFlag(flags, (byte)2);
        buffer.skipBytes(10);
        length -= 10;
        
        if (streamId == 0) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid SYN_STREAM Frame");
        } else {
          state = State.READ_HEADER_BLOCK;
          delegate.readSynStreamFrame(streamId, associatedToStreamId, priority, last, unidirectional);
        }
        break;
      
      case 4: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        
        streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        boolean last = hasFlag(flags, (byte)1);
        
        buffer.skipBytes(4);
        length -= 4;
        
        if (streamId == 0) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid SYN_REPLY Frame");
        } else {
          state = State.READ_HEADER_BLOCK;
          delegate.readSynReplyFrame(streamId, last);
        }
        break;
      
      case 5: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        
        streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        int statusCode = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex() + 4);
        buffer.skipBytes(8);
        
        if ((streamId == 0) || (statusCode == 0)) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid RST_STREAM Frame");
        } else {
          state = State.READ_COMMON_HEADER;
          delegate.readRstStreamFrame(streamId, statusCode);
        }
        break;
      
      case 6: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        
        boolean clear = hasFlag(flags, (byte)1);
        
        numSettings = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        buffer.skipBytes(4);
        length -= 4;
        

        if (((length & 0x7) != 0) || (length >> 3 != numSettings)) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid SETTINGS Frame");
        } else {
          state = State.READ_SETTING;
          delegate.readSettingsFrame(clear);
        }
        break;
      
      case 7: 
        if (numSettings == 0) {
          state = State.READ_COMMON_HEADER;
          delegate.readSettingsEnd();
        }
        else
        {
          if (buffer.readableBytes() < 8) {
            return;
          }
          
          byte settingsFlags = buffer.getByte(buffer.readerIndex());
          int id = SpdyCodecUtil.getUnsignedMedium(buffer, buffer.readerIndex() + 1);
          int value = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex() + 4);
          boolean persistValue = hasFlag(settingsFlags, (byte)1);
          boolean persisted = hasFlag(settingsFlags, (byte)2);
          buffer.skipBytes(8);
          
          numSettings -= 1;
          
          delegate.readSetting(id, value, persistValue, persisted); }
        break;
      
      case 8: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        
        int pingId = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex());
        buffer.skipBytes(4);
        
        state = State.READ_COMMON_HEADER;
        delegate.readPingFrame(pingId);
        break;
      
      case 9: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        
        int lastGoodStreamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        int statusCode = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex() + 4);
        buffer.skipBytes(8);
        
        state = State.READ_COMMON_HEADER;
        delegate.readGoAwayFrame(lastGoodStreamId, statusCode);
        break;
      
      case 10: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        
        streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        boolean last = hasFlag(flags, (byte)1);
        
        buffer.skipBytes(4);
        length -= 4;
        
        if (streamId == 0) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid HEADERS Frame");
        } else {
          state = State.READ_HEADER_BLOCK;
          delegate.readHeadersFrame(streamId, last);
        }
        break;
      
      case 11: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        
        streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        int deltaWindowSize = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex() + 4);
        buffer.skipBytes(8);
        
        if (deltaWindowSize == 0) {
          state = State.FRAME_ERROR;
          delegate.readFrameError("Invalid WINDOW_UPDATE Frame");
        } else {
          state = State.READ_COMMON_HEADER;
          delegate.readWindowUpdateFrame(streamId, deltaWindowSize);
        }
        break;
      
      case 12: 
        if (length == 0) {
          state = State.READ_COMMON_HEADER;
          delegate.readHeaderBlockEnd();
        }
        else
        {
          if (!buffer.isReadable()) {
            return;
          }
          
          int compressedBytes = Math.min(buffer.readableBytes(), length);
          ByteBuf headerBlock = buffer.alloc().buffer(compressedBytes);
          headerBlock.writeBytes(buffer, compressedBytes);
          length -= compressedBytes;
          
          delegate.readHeaderBlock(headerBlock); }
        break;
      
      case 13: 
        int numBytes = Math.min(buffer.readableBytes(), length);
        buffer.skipBytes(numBytes);
        length -= numBytes;
        if (length != 0) break label1466;
        state = State.READ_COMMON_HEADER; }
    }
    label1466:
    return;
    

    buffer.skipBytes(buffer.readableBytes());
    return;
    

    throw new Error("Shouldn't reach here.");
  }
  

  private static boolean hasFlag(byte flags, byte flag)
  {
    return (flags & flag) != 0;
  }
  
  private static State getNextState(int type, int length) {
    switch (type) {
    case 0: 
      return State.READ_DATA_FRAME;
    
    case 1: 
      return State.READ_SYN_STREAM_FRAME;
    
    case 2: 
      return State.READ_SYN_REPLY_FRAME;
    
    case 3: 
      return State.READ_RST_STREAM_FRAME;
    
    case 4: 
      return State.READ_SETTINGS_FRAME;
    
    case 6: 
      return State.READ_PING_FRAME;
    
    case 7: 
      return State.READ_GOAWAY_FRAME;
    
    case 8: 
      return State.READ_HEADERS_FRAME;
    
    case 9: 
      return State.READ_WINDOW_UPDATE_FRAME;
    }
    
    if (length != 0) {
      return State.DISCARD_FRAME;
    }
    return State.READ_COMMON_HEADER;
  }
  

  private static boolean isValidFrameHeader(int streamId, int type, byte flags, int length)
  {
    switch (type) {
    case 0: 
      return streamId != 0;
    
    case 1: 
      return length >= 10;
    
    case 2: 
      return length >= 4;
    
    case 3: 
      return (flags == 0) && (length == 8);
    
    case 4: 
      return length >= 4;
    
    case 6: 
      return length == 4;
    
    case 7: 
      return length == 8;
    
    case 8: 
      return length >= 4;
    
    case 9: 
      return length == 8;
    }
    
    return true;
  }
}
