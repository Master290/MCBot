package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Checksum;























public class FastLzFrameDecoder
  extends ByteToMessageDecoder
{
  private static enum State
  {
    INIT_BLOCK, 
    INIT_BLOCK_PARAMS, 
    DECOMPRESS_DATA, 
    CORRUPTED;
    
    private State() {} }
  private State currentState = State.INIT_BLOCK;
  



  private final ByteBufChecksum checksum;
  



  private int chunkLength;
  



  private int originalLength;
  



  private boolean isCompressed;
  



  private boolean hasChecksum;
  



  private int currentChecksum;
  



  public FastLzFrameDecoder()
  {
    this(false);
  }
  









  public FastLzFrameDecoder(boolean validateChecksums)
  {
    this(validateChecksums ? new Adler32() : null);
  }
  






  public FastLzFrameDecoder(Checksum checksum)
  {
    this.checksum = (checksum == null ? null : ByteBufChecksum.wrapChecksum(checksum));
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    try {
      switch (1.$SwitchMap$io$netty$handler$codec$compression$FastLzFrameDecoder$State[currentState.ordinal()]) {
      case 1: 
        if (in.readableBytes() >= 4)
        {


          int magic = in.readUnsignedMedium();
          if (magic != 4607066) {
            throw new DecompressionException("unexpected block identifier");
          }
          
          byte options = in.readByte();
          isCompressed = ((options & 0x1) == 1);
          hasChecksum = ((options & 0x10) == 16);
          
          currentState = State.INIT_BLOCK_PARAMS;
        }
        break;
      case 2:  if (in.readableBytes() >= 2 + (isCompressed ? 2 : 0) + (hasChecksum ? 4 : 0))
        {

          currentChecksum = (hasChecksum ? in.readInt() : 0);
          this.chunkLength = in.readUnsignedShort();
          this.originalLength = (isCompressed ? in.readUnsignedShort() : this.chunkLength);
          
          currentState = State.DECOMPRESS_DATA;
        }
        break;
      case 3:  int chunkLength = this.chunkLength;
        if (in.readableBytes() >= chunkLength)
        {


          int idx = in.readerIndex();
          int originalLength = this.originalLength;
          
          ByteBuf output = null;
          try
          {
            if (isCompressed)
            {
              output = ctx.alloc().buffer(originalLength);
              int outputOffset = output.writerIndex();
              int decompressedBytes = FastLz.decompress(in, idx, chunkLength, output, outputOffset, originalLength);
              
              if (originalLength != decompressedBytes) {
                throw new DecompressionException(String.format("stream corrupted: originalLength(%d) and actual length(%d) mismatch", new Object[] {
                
                  Integer.valueOf(originalLength), Integer.valueOf(decompressedBytes) }));
              }
              output.writerIndex(output.writerIndex() + decompressedBytes);
            } else {
              output = in.retainedSlice(idx, chunkLength);
            }
            
            ByteBufChecksum checksum = this.checksum;
            if ((hasChecksum) && (checksum != null)) {
              checksum.reset();
              checksum.update(output, output.readerIndex(), output.readableBytes());
              int checksumResult = (int)checksum.getValue();
              if (checksumResult != currentChecksum) {
                throw new DecompressionException(String.format("stream corrupted: mismatching checksum: %d (expected: %d)", new Object[] {
                
                  Integer.valueOf(checksumResult), Integer.valueOf(currentChecksum) }));
              }
            }
            
            if (output.readableBytes() > 0) {
              out.add(output);
            } else {
              output.release();
            }
            output = null;
            in.skipBytes(chunkLength);
            
            currentState = State.INIT_BLOCK;
          } finally {
            if (output != null)
              output.release();
          }
        }
        break;
      case 4: 
        in.skipBytes(in.readableBytes());
        break;
      default: 
        throw new IllegalStateException();
      }
    } catch (Exception e) {
      currentState = State.CORRUPTED;
      throw e;
    }
  }
}
