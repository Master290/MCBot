package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.memcache.AbstractMemcacheObjectDecoder;
import io.netty.handler.codec.memcache.DefaultLastMemcacheContent;
import io.netty.handler.codec.memcache.DefaultMemcacheContent;
import io.netty.handler.codec.memcache.LastMemcacheContent;
import io.netty.handler.codec.memcache.MemcacheContent;
import io.netty.util.internal.ObjectUtil;
import java.util.List;




























public abstract class AbstractBinaryMemcacheDecoder<M extends BinaryMemcacheMessage>
  extends AbstractMemcacheObjectDecoder
{
  public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;
  private final int chunkSize;
  private M currentMessage;
  private int alreadyReadChunkSize;
  private State state = State.READ_HEADER;
  


  protected AbstractBinaryMemcacheDecoder()
  {
    this(8192);
  }
  




  protected AbstractBinaryMemcacheDecoder(int chunkSize)
  {
    ObjectUtil.checkPositiveOrZero(chunkSize, "chunkSize");
    
    this.chunkSize = chunkSize;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$memcache$binary$AbstractBinaryMemcacheDecoder$State[state.ordinal()]) {
    case 1:  try {
        if (in.readableBytes() < 24) {
          return;
        }
        resetDecoder();
        
        currentMessage = decodeHeader(in);
        state = State.READ_EXTRAS;
      } catch (Exception e) {
        resetDecoder();
        out.add(invalidMessage(e));
        return;
      }
    case 2:  try {
        byte extrasLength = currentMessage.extrasLength();
        if (extrasLength > 0) {
          if (in.readableBytes() < extrasLength) {
            return;
          }
          
          currentMessage.setExtras(in.readRetainedSlice(extrasLength));
        }
        
        state = State.READ_KEY;
      } catch (Exception e) {
        resetDecoder();
        out.add(invalidMessage(e));
        return;
      }
    case 3:  try {
        short keyLength = currentMessage.keyLength();
        if (keyLength > 0) {
          if (in.readableBytes() < keyLength) {
            return;
          }
          
          currentMessage.setKey(in.readRetainedSlice(keyLength));
        }
        out.add(currentMessage.retain());
        state = State.READ_CONTENT;
      } catch (Exception e) {
        resetDecoder();
        out.add(invalidMessage(e));
        return;
      }
    case 4: 
      try
      {
        int valueLength = currentMessage.totalBodyLength() - currentMessage.keyLength() - currentMessage.extrasLength();
        int toRead = in.readableBytes();
        if (valueLength > 0) {
          if (toRead == 0) {
            return;
          }
          
          if (toRead > chunkSize) {
            toRead = chunkSize;
          }
          
          int remainingLength = valueLength - alreadyReadChunkSize;
          if (toRead > remainingLength) {
            toRead = remainingLength;
          }
          
          ByteBuf chunkBuffer = in.readRetainedSlice(toRead);
          MemcacheContent chunk;
          MemcacheContent chunk;
          if (this.alreadyReadChunkSize += toRead >= valueLength) {
            chunk = new DefaultLastMemcacheContent(chunkBuffer);
          } else {
            chunk = new DefaultMemcacheContent(chunkBuffer);
          }
          
          out.add(chunk);
          if (alreadyReadChunkSize < valueLength) {
            return;
          }
        } else {
          out.add(LastMemcacheContent.EMPTY_LAST_CONTENT);
        }
        
        resetDecoder();
        state = State.READ_HEADER;
        return;
      } catch (Exception e) {
        resetDecoder();
        out.add(invalidChunk(e));
        return;
      }
    case 5: 
      in.skipBytes(actualReadableBytes());
      return;
    }
    throw new Error("Unknown state reached: " + state);
  }
  






  private M invalidMessage(Exception cause)
  {
    state = State.BAD_MESSAGE;
    M message = buildInvalidMessage();
    message.setDecoderResult(DecoderResult.failure(cause));
    return message;
  }
  





  private MemcacheContent invalidChunk(Exception cause)
  {
    state = State.BAD_MESSAGE;
    MemcacheContent chunk = new DefaultLastMemcacheContent(Unpooled.EMPTY_BUFFER);
    chunk.setDecoderResult(DecoderResult.failure(cause));
    return chunk;
  }
  





  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    super.channelInactive(ctx);
    
    resetDecoder();
  }
  


  protected void resetDecoder()
  {
    if (currentMessage != null) {
      currentMessage.release();
      currentMessage = null;
    }
    alreadyReadChunkSize = 0;
  }
  







  protected abstract M decodeHeader(ByteBuf paramByteBuf);
  







  protected abstract M buildInvalidMessage();
  






  static enum State
  {
    READ_HEADER, 
    



    READ_EXTRAS, 
    



    READ_KEY, 
    



    READ_CONTENT, 
    



    BAD_MESSAGE;
    
    private State() {}
  }
}
