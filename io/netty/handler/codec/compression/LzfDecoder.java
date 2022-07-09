package io.netty.handler.codec.compression;

import com.ning.compress.BufferRecycler;
import com.ning.compress.lzf.ChunkDecoder;
import com.ning.compress.lzf.util.ChunkDecoderFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;





























public class LzfDecoder
  extends ByteToMessageDecoder
{
  private static enum State
  {
    INIT_BLOCK, 
    INIT_ORIGINAL_LENGTH, 
    DECOMPRESS_DATA, 
    CORRUPTED;
    
    private State() {} }
  private State currentState = State.INIT_BLOCK;
  




  private static final short MAGIC_NUMBER = 23126;
  




  private ChunkDecoder decoder;
  



  private BufferRecycler recycler;
  



  private int chunkLength;
  



  private int originalLength;
  



  private boolean isCompressed;
  




  public LzfDecoder()
  {
    this(false);
  }
  










  public LzfDecoder(boolean safeInstance)
  {
    decoder = (safeInstance ? ChunkDecoderFactory.safeInstance() : ChunkDecoderFactory.optimalInstance());
    
    recycler = BufferRecycler.instance();
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    try {
      switch (1.$SwitchMap$io$netty$handler$codec$compression$LzfDecoder$State[currentState.ordinal()]) {
      case 1: 
        if (in.readableBytes() >= 5)
        {

          int magic = in.readUnsignedShort();
          if (magic != 23126) {
            throw new DecompressionException("unexpected block identifier");
          }
          
          int type = in.readByte();
          switch (type) {
          case 0: 
            isCompressed = false;
            currentState = State.DECOMPRESS_DATA;
            break;
          case 1: 
            isCompressed = true;
            currentState = State.INIT_ORIGINAL_LENGTH;
            break;
          default: 
            throw new DecompressionException(String.format("unknown type of chunk: %d (expected: %d or %d)", new Object[] {
            
              Integer.valueOf(type), Integer.valueOf(0), Integer.valueOf(1) }));
          }
          this.chunkLength = in.readUnsignedShort();
          



          if (this.chunkLength > 65535) {
            throw new DecompressionException(String.format("chunk length exceeds maximum: %d (expected: =< %d)", new Object[] {
            
              Integer.valueOf(this.chunkLength), Integer.valueOf(65535) }));
          }
          
          if (type != 1)
            break;
        }
        break;
      case 2: 
        if (in.readableBytes() >= 2)
        {

          this.originalLength = in.readUnsignedShort();
          



          if (this.originalLength > 65535) {
            throw new DecompressionException(String.format("original length exceeds maximum: %d (expected: =< %d)", new Object[] {
            
              Integer.valueOf(this.chunkLength), Integer.valueOf(65535) }));
          }
          
          currentState = State.DECOMPRESS_DATA;
        }
        break;
      case 3:  int chunkLength = this.chunkLength;
        if (in.readableBytes() >= chunkLength)
        {

          int originalLength = this.originalLength;
          
          if (isCompressed) {
            int idx = in.readerIndex();
            int inPos;
            byte[] inputArray;
            int inPos;
            if (in.hasArray()) {
              byte[] inputArray = in.array();
              inPos = in.arrayOffset() + idx;
            } else {
              inputArray = recycler.allocInputBuffer(chunkLength);
              in.getBytes(idx, inputArray, 0, chunkLength);
              inPos = 0;
            }
            
            ByteBuf uncompressed = ctx.alloc().heapBuffer(originalLength, originalLength);
            int outPos;
            byte[] outputArray;
            int outPos; if (uncompressed.hasArray()) {
              byte[] outputArray = uncompressed.array();
              outPos = uncompressed.arrayOffset() + uncompressed.writerIndex();
            } else {
              outputArray = new byte[originalLength];
              outPos = 0;
            }
            
            boolean success = false;
            try {
              decoder.decodeChunk(inputArray, inPos, outputArray, outPos, outPos + originalLength);
              if (uncompressed.hasArray()) {
                uncompressed.writerIndex(uncompressed.writerIndex() + originalLength);
              } else {
                uncompressed.writeBytes(outputArray);
              }
              out.add(uncompressed);
              in.skipBytes(chunkLength);
              success = true;
            } finally {
              if (!success) {
                uncompressed.release();
              }
            }
            
            if (!in.hasArray()) {
              recycler.releaseInputBuffer(inputArray);
            }
          } else if (chunkLength > 0) {
            out.add(in.readRetainedSlice(chunkLength));
          }
          
          currentState = State.INIT_BLOCK; }
        break;
      case 4: 
        in.skipBytes(in.readableBytes());
        break;
      default: 
        throw new IllegalStateException();
      }
    } catch (Exception e) {
      currentState = State.CORRUPTED;
      decoder = null;
      recycler = null;
      throw e;
    }
  }
}
