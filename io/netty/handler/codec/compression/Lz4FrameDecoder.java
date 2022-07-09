package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.ObjectUtil;
import java.util.List;
import java.util.zip.Checksum;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;


































public class Lz4FrameDecoder
  extends ByteToMessageDecoder
{
  private static enum State
  {
    INIT_BLOCK, 
    DECOMPRESS_DATA, 
    FINISHED, 
    CORRUPTED;
    
    private State() {} }
  private State currentState = State.INIT_BLOCK;
  




  private LZ4FastDecompressor decompressor;
  




  private ByteBufChecksum checksum;
  




  private int blockType;
  




  private int compressedLength;
  



  private int decompressedLength;
  



  private int currentChecksum;
  




  public Lz4FrameDecoder()
  {
    this(false);
  }
  






  public Lz4FrameDecoder(boolean validateChecksums)
  {
    this(LZ4Factory.fastestInstance(), validateChecksums);
  }
  











  public Lz4FrameDecoder(LZ4Factory factory, boolean validateChecksums)
  {
    this(factory, validateChecksums ? new Lz4XXHash32(-1756908916) : null);
  }
  








  public Lz4FrameDecoder(LZ4Factory factory, Checksum checksum)
  {
    decompressor = ((LZ4Factory)ObjectUtil.checkNotNull(factory, "factory")).fastDecompressor();
    this.checksum = (checksum == null ? null : ByteBufChecksum.wrapChecksum(checksum));
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    try {
      switch (1.$SwitchMap$io$netty$handler$codec$compression$Lz4FrameDecoder$State[currentState.ordinal()]) {
      case 1: 
        if (in.readableBytes() >= 21)
        {

          long magic = in.readLong();
          if (magic != 5501767354678207339L) {
            throw new DecompressionException("unexpected block identifier");
          }
          
          int token = in.readByte();
          int compressionLevel = (token & 0xF) + 10;
          int blockType = token & 0xF0;
          
          int compressedLength = Integer.reverseBytes(in.readInt());
          if ((compressedLength < 0) || (compressedLength > 33554432)) {
            throw new DecompressionException(String.format("invalid compressedLength: %d (expected: 0-%d)", new Object[] {
            
              Integer.valueOf(compressedLength), Integer.valueOf(33554432) }));
          }
          
          int decompressedLength = Integer.reverseBytes(in.readInt());
          int maxDecompressedLength = 1 << compressionLevel;
          if ((decompressedLength < 0) || (decompressedLength > maxDecompressedLength)) {
            throw new DecompressionException(String.format("invalid decompressedLength: %d (expected: 0-%d)", new Object[] {
            
              Integer.valueOf(decompressedLength), Integer.valueOf(maxDecompressedLength) }));
          }
          if (((decompressedLength == 0) && (compressedLength != 0)) || ((decompressedLength != 0) && (compressedLength == 0)) || ((blockType == 16) && (decompressedLength != compressedLength)))
          {

            throw new DecompressionException(String.format("stream corrupted: compressedLength(%d) and decompressedLength(%d) mismatch", new Object[] {
            
              Integer.valueOf(compressedLength), Integer.valueOf(decompressedLength) }));
          }
          
          int currentChecksum = Integer.reverseBytes(in.readInt());
          if ((decompressedLength == 0) && (compressedLength == 0)) {
            if (currentChecksum != 0) {
              throw new DecompressionException("stream corrupted: checksum error");
            }
            currentState = State.FINISHED;
            decompressor = null;
            this.checksum = null;
          }
          else
          {
            this.blockType = blockType;
            this.compressedLength = compressedLength;
            this.decompressedLength = decompressedLength;
            this.currentChecksum = currentChecksum;
            
            currentState = State.DECOMPRESS_DATA;
          }
        }
        break; case 2:  int blockType = this.blockType;
        int compressedLength = this.compressedLength;
        int decompressedLength = this.decompressedLength;
        int currentChecksum = this.currentChecksum;
        
        if (in.readableBytes() >= compressedLength)
        {


          ByteBufChecksum checksum = this.checksum;
          ByteBuf uncompressed = null;
          try
          {
            switch (blockType)
            {

            case 16: 
              uncompressed = in.retainedSlice(in.readerIndex(), decompressedLength);
              break;
            case 32: 
              uncompressed = ctx.alloc().buffer(decompressedLength, decompressedLength);
              
              decompressor.decompress(CompressionUtil.safeNioBuffer(in), uncompressed
                .internalNioBuffer(uncompressed.writerIndex(), decompressedLength));
              
              uncompressed.writerIndex(uncompressed.writerIndex() + decompressedLength);
              break;
            default: 
              throw new DecompressionException(String.format("unexpected blockType: %d (expected: %d or %d)", new Object[] {
              
                Integer.valueOf(blockType), Integer.valueOf(16), Integer.valueOf(32) }));
            }
            
            in.skipBytes(compressedLength);
            
            if (checksum != null) {
              CompressionUtil.checkChecksum(checksum, uncompressed, currentChecksum);
            }
            out.add(uncompressed);
            uncompressed = null;
            currentState = State.INIT_BLOCK;
          } catch (LZ4Exception e) {
            throw new DecompressionException(e);
          } finally {
            if (uncompressed != null)
              uncompressed.release();
          }
        }
        break;
      case 3: 
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
  



  public boolean isClosed()
  {
    return currentState == State.FINISHED;
  }
}
