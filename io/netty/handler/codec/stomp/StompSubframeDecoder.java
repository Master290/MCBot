package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.AppendableCharSequence;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.util.List;






























public class StompSubframeDecoder
  extends ReplayingDecoder<State>
{
  private static final int DEFAULT_CHUNK_SIZE = 8132;
  private static final int DEFAULT_MAX_LINE_LENGTH = 1024;
  private final Utf8LineParser commandParser;
  private final HeaderParser headerParser;
  private final int maxChunkSize;
  private int alreadyReadChunkSize;
  private LastStompContentSubframe lastContent;
  
  static enum State
  {
    SKIP_CONTROL_CHARACTERS, 
    READ_HEADERS, 
    READ_CONTENT, 
    FINALIZE_FRAME_READ, 
    BAD_FRAME, 
    INVALID_CHUNK;
    


    private State() {}
  }
  

  private long contentLength = -1L;
  
  public StompSubframeDecoder() {
    this(1024, 8132);
  }
  
  public StompSubframeDecoder(boolean validateHeaders) {
    this(1024, 8132, validateHeaders);
  }
  
  public StompSubframeDecoder(int maxLineLength, int maxChunkSize) {
    this(maxLineLength, maxChunkSize, false);
  }
  
  public StompSubframeDecoder(int maxLineLength, int maxChunkSize, boolean validateHeaders) {
    super(State.SKIP_CONTROL_CHARACTERS);
    ObjectUtil.checkPositive(maxLineLength, "maxLineLength");
    ObjectUtil.checkPositive(maxChunkSize, "maxChunkSize");
    this.maxChunkSize = maxChunkSize;
    commandParser = new Utf8LineParser(new AppendableCharSequence(16), maxLineLength);
    headerParser = new HeaderParser(new AppendableCharSequence(128), maxLineLength, validateHeaders);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$stomp$StompSubframeDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      skipControlCharacters(in);
      checkpoint(State.READ_HEADERS);
    
    case 2: 
      StompCommand command = StompCommand.UNKNOWN;
      StompHeadersSubframe frame = null;
      try {
        command = readCommand(in);
        frame = new DefaultStompHeadersSubframe(command);
        checkpoint(readHeaders(in, frame.headers()));
        out.add(frame);
      } catch (Exception e) {
        if (frame == null) {
          frame = new DefaultStompHeadersSubframe(command);
        }
        frame.setDecoderResult(DecoderResult.failure(e));
        out.add(frame);
        checkpoint(State.BAD_FRAME);
        return;
      }
    
    case 3: 
      in.skipBytes(actualReadableBytes());
      return;
    }
    try {
      switch (1.$SwitchMap$io$netty$handler$codec$stomp$StompSubframeDecoder$State[((State)state()).ordinal()]) {
      case 4: 
        int toRead = in.readableBytes();
        if (toRead == 0) {
          return;
        }
        if (toRead > maxChunkSize) {
          toRead = maxChunkSize;
        }
        if (contentLength >= 0L) {
          int remainingLength = (int)(contentLength - alreadyReadChunkSize);
          if (toRead > remainingLength) {
            toRead = remainingLength;
          }
          ByteBuf chunkBuffer = ByteBufUtil.readBytes(ctx.alloc(), in, toRead);
          if (this.alreadyReadChunkSize += toRead >= contentLength) {
            lastContent = new DefaultLastStompContentSubframe(chunkBuffer);
            checkpoint(State.FINALIZE_FRAME_READ);
          } else {
            out.add(new DefaultStompContentSubframe(chunkBuffer));
            return;
          }
        } else {
          int nulIndex = ByteBufUtil.indexOf(in, in.readerIndex(), in.writerIndex(), (byte)0);
          if (nulIndex == in.readerIndex()) {
            checkpoint(State.FINALIZE_FRAME_READ);
          } else {
            if (nulIndex > 0) {
              toRead = nulIndex - in.readerIndex();
            } else {
              toRead = in.writerIndex() - in.readerIndex();
            }
            ByteBuf chunkBuffer = ByteBufUtil.readBytes(ctx.alloc(), in, toRead);
            alreadyReadChunkSize += toRead;
            if (nulIndex > 0) {
              lastContent = new DefaultLastStompContentSubframe(chunkBuffer);
              checkpoint(State.FINALIZE_FRAME_READ);
            } else {
              out.add(new DefaultStompContentSubframe(chunkBuffer));
              return;
            }
          }
        }
      
      case 5: 
        skipNullCharacter(in);
        if (lastContent == null) {
          lastContent = LastStompContentSubframe.EMPTY_LAST_CONTENT;
        }
        out.add(lastContent);
        resetDecoder();
      }
    } catch (Exception e) {
      StompContentSubframe errorContent = new DefaultLastStompContentSubframe(Unpooled.EMPTY_BUFFER);
      errorContent.setDecoderResult(DecoderResult.failure(e));
      out.add(errorContent);
      checkpoint(State.BAD_FRAME);
    }
  }
  
  private StompCommand readCommand(ByteBuf in) {
    CharSequence commandSequence = commandParser.parse(in);
    if (commandSequence == null) {
      throw new DecoderException("Failed to read command from channel");
    }
    String commandStr = commandSequence.toString();
    try {
      return StompCommand.valueOf(commandStr);
    } catch (IllegalArgumentException iae) {
      throw new DecoderException("Cannot to parse command " + commandStr);
    }
  }
  
  private State readHeaders(ByteBuf buffer, StompHeaders headers) {
    for (;;) {
      boolean headerRead = headerParser.parseHeader(headers, buffer);
      if (!headerRead) {
        if (headers.contains(StompHeaders.CONTENT_LENGTH)) {
          contentLength = getContentLength(headers);
          if (contentLength == 0L) {
            return State.FINALIZE_FRAME_READ;
          }
        }
        return State.READ_CONTENT;
      }
    }
  }
  
  private static long getContentLength(StompHeaders headers) {
    long contentLength = headers.getLong(StompHeaders.CONTENT_LENGTH, 0L);
    if (contentLength < 0L) {
      throw new DecoderException(StompHeaders.CONTENT_LENGTH + " must be non-negative");
    }
    return contentLength;
  }
  
  private static void skipNullCharacter(ByteBuf buffer) {
    byte b = buffer.readByte();
    if (b != 0) {
      throw new IllegalStateException("unexpected byte in buffer " + b + " while expecting NULL byte");
    }
  }
  
  private static void skipControlCharacters(ByteBuf buffer) {
    byte b;
    do {
      b = buffer.readByte();
    } while ((b == 13) || (b == 10));
    buffer.readerIndex(buffer.readerIndex() - 1);
  }
  


  private void resetDecoder()
  {
    checkpoint(State.SKIP_CONTROL_CHARACTERS);
    contentLength = -1L;
    alreadyReadChunkSize = 0;
    lastContent = null;
  }
  
  private static class Utf8LineParser implements ByteProcessor
  {
    private final AppendableCharSequence charSeq;
    private final int maxLineLength;
    private int lineLength;
    private char interim;
    private boolean nextRead;
    
    Utf8LineParser(AppendableCharSequence charSeq, int maxLineLength)
    {
      this.charSeq = ((AppendableCharSequence)ObjectUtil.checkNotNull(charSeq, "charSeq"));
      this.maxLineLength = maxLineLength;
    }
    
    AppendableCharSequence parse(ByteBuf byteBuf) {
      reset();
      int offset = byteBuf.forEachByte(this);
      if (offset == -1) {
        return null;
      }
      
      byteBuf.readerIndex(offset + 1);
      return charSeq;
    }
    
    AppendableCharSequence charSequence() {
      return charSeq;
    }
    
    public boolean process(byte nextByte) throws Exception
    {
      if (nextByte == 13) {
        lineLength += 1;
        return true;
      }
      
      if (nextByte == 10) {
        return false;
      }
      
      if (++lineLength > maxLineLength) {
        throw new TooLongFrameException("An STOMP line is larger than " + maxLineLength + " bytes.");
      }
      



      if (nextRead) {
        interim = ((char)(interim | (nextByte & 0x3F) << 6));
        nextRead = false;
      } else if (interim != 0) {
        charSeq.append((char)(interim | nextByte & 0x3F));
        interim = '\000';
      } else if (nextByte >= 0)
      {
        charSeq.append((char)nextByte);
      } else if ((nextByte & 0xE0) == 192)
      {

        interim = ((char)((nextByte & 0x1F) << 6));
      }
      else {
        interim = ((char)((nextByte & 0xF) << 12));
        nextRead = true;
      }
      
      return true;
    }
    
    protected void reset() {
      charSeq.reset();
      lineLength = 0;
      interim = '\000';
      nextRead = false;
    }
  }
  
  private static final class HeaderParser extends StompSubframeDecoder.Utf8LineParser
  {
    private final boolean validateHeaders;
    private String name;
    private boolean valid;
    
    HeaderParser(AppendableCharSequence charSeq, int maxLineLength, boolean validateHeaders)
    {
      super(maxLineLength);
      this.validateHeaders = validateHeaders;
    }
    
    boolean parseHeader(StompHeaders headers, ByteBuf buf) {
      AppendableCharSequence value = super.parse(buf);
      if ((value == null) || ((name == null) && (value.length() == 0))) {
        return false;
      }
      
      if (valid) {
        headers.add(name, value.toString());
      } else if (validateHeaders) {
        if (StringUtil.isNullOrEmpty(name)) {
          throw new IllegalArgumentException("received an invalid header line '" + value + '\'');
        }
        String line = name + ':' + value;
        throw new IllegalArgumentException("a header value or name contains a prohibited character ':', " + line);
      }
      
      return true;
    }
    
    public boolean process(byte nextByte) throws Exception
    {
      if (nextByte == 58) {
        if (name == null) {
          AppendableCharSequence charSeq = charSequence();
          if (charSeq.length() != 0) {
            name = charSeq.substring(0, charSeq.length());
            charSeq.reset();
            valid = true;
            return true;
          }
          name = "";
        }
        else {
          valid = false;
        }
      }
      
      return super.process(nextByte);
    }
    
    protected void reset()
    {
      name = null;
      valid = false;
      super.reset();
    }
  }
}
