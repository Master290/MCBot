package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;






















public class JdkZlibDecoder
  extends ZlibDecoder
{
  private static final int FHCRC = 2;
  private static final int FEXTRA = 4;
  private static final int FNAME = 8;
  private static final int FCOMMENT = 16;
  private static final int FRESERVED = 224;
  private Inflater inflater;
  private final byte[] dictionary;
  private final ByteBufChecksum crc;
  private final boolean decompressConcatenated;
  
  private static enum GzipState
  {
    HEADER_START, 
    HEADER_END, 
    FLG_READ, 
    XLEN_READ, 
    SKIP_FNAME, 
    SKIP_COMMENT, 
    PROCESS_FHCRC, 
    FOOTER_START;
    
    private GzipState() {} }
  private GzipState gzipState = GzipState.HEADER_START;
  private int flags = -1;
  private int xlen = -1;
  

  private volatile boolean finished;
  
  private boolean decideZlibOrNone;
  

  public JdkZlibDecoder()
  {
    this(ZlibWrapper.ZLIB, null, false, 0);
  }
  







  public JdkZlibDecoder(int maxAllocation)
  {
    this(ZlibWrapper.ZLIB, null, false, maxAllocation);
  }
  




  public JdkZlibDecoder(byte[] dictionary)
  {
    this(ZlibWrapper.ZLIB, dictionary, false, 0);
  }
  








  public JdkZlibDecoder(byte[] dictionary, int maxAllocation)
  {
    this(ZlibWrapper.ZLIB, dictionary, false, maxAllocation);
  }
  




  public JdkZlibDecoder(ZlibWrapper wrapper)
  {
    this(wrapper, null, false, 0);
  }
  








  public JdkZlibDecoder(ZlibWrapper wrapper, int maxAllocation)
  {
    this(wrapper, null, false, maxAllocation);
  }
  
  public JdkZlibDecoder(ZlibWrapper wrapper, boolean decompressConcatenated) {
    this(wrapper, null, decompressConcatenated, 0);
  }
  
  public JdkZlibDecoder(ZlibWrapper wrapper, boolean decompressConcatenated, int maxAllocation) {
    this(wrapper, null, decompressConcatenated, maxAllocation);
  }
  
  public JdkZlibDecoder(boolean decompressConcatenated) {
    this(ZlibWrapper.GZIP, null, decompressConcatenated, 0);
  }
  
  public JdkZlibDecoder(boolean decompressConcatenated, int maxAllocation) {
    this(ZlibWrapper.GZIP, null, decompressConcatenated, maxAllocation);
  }
  
  private JdkZlibDecoder(ZlibWrapper wrapper, byte[] dictionary, boolean decompressConcatenated, int maxAllocation) {
    super(maxAllocation);
    
    ObjectUtil.checkNotNull(wrapper, "wrapper");
    
    this.decompressConcatenated = decompressConcatenated;
    switch (1.$SwitchMap$io$netty$handler$codec$compression$ZlibWrapper[wrapper.ordinal()]) {
    case 1: 
      inflater = new Inflater(true);
      crc = ByteBufChecksum.wrapChecksum(new CRC32());
      break;
    case 2: 
      inflater = new Inflater(true);
      crc = null;
      break;
    case 3: 
      inflater = new Inflater();
      crc = null;
      break;
    
    case 4: 
      decideZlibOrNone = true;
      crc = null;
      break;
    default: 
      throw new IllegalArgumentException("Only GZIP or ZLIB is supported, but you used " + wrapper);
    }
    this.dictionary = dictionary;
  }
  
  public boolean isClosed()
  {
    return finished;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    if (finished)
    {
      in.skipBytes(in.readableBytes());
      return;
    }
    
    int readableBytes = in.readableBytes();
    if (readableBytes == 0) {
      return;
    }
    
    if (decideZlibOrNone)
    {
      if (readableBytes < 2) {
        return;
      }
      
      boolean nowrap = !looksLikeZlib(in.getShort(in.readerIndex()));
      inflater = new Inflater(nowrap);
      decideZlibOrNone = false;
    }
    
    if ((crc != null) && 
      (gzipState != GzipState.HEADER_END)) {
      if (gzipState == GzipState.FOOTER_START) {
        if (!handleGzipFooter(in))
        {
          return;
        }
        
        assert (gzipState == GzipState.HEADER_START);
      }
      if (!readGZIPHeader(in))
      {
        return;
      }
      
      readableBytes = in.readableBytes();
      if (readableBytes == 0) {
        return;
      }
    }
    

    if (inflater.needsInput()) {
      if (in.hasArray()) {
        inflater.setInput(in.array(), in.arrayOffset() + in.readerIndex(), readableBytes);
      } else {
        byte[] array = new byte[readableBytes];
        in.getBytes(in.readerIndex(), array);
        inflater.setInput(array);
      }
    }
    
    ByteBuf decompressed = prepareDecompressBuffer(ctx, null, inflater.getRemaining() << 1);
    try {
      boolean readFooter = false;
      while (!inflater.needsInput()) {
        byte[] outArray = decompressed.array();
        int writerIndex = decompressed.writerIndex();
        int outIndex = decompressed.arrayOffset() + writerIndex;
        int writable = decompressed.writableBytes();
        int outputLength = inflater.inflate(outArray, outIndex, writable);
        if (outputLength > 0) {
          decompressed.writerIndex(writerIndex + outputLength);
          if (crc != null) {
            crc.update(outArray, outIndex, outputLength);
          }
        } else if (inflater.needsDictionary()) {
          if (dictionary == null) {
            throw new DecompressionException("decompression failure, unable to set dictionary as non was specified");
          }
          
          inflater.setDictionary(dictionary);
        }
        
        if (inflater.finished()) {
          if (crc == null) {
            finished = true; break;
          }
          readFooter = true;
          
          break;
        }
        decompressed = prepareDecompressBuffer(ctx, decompressed, inflater.getRemaining() << 1);
      }
      

      in.skipBytes(readableBytes - inflater.getRemaining());
      
      if (readFooter) {
        gzipState = GzipState.FOOTER_START;
        handleGzipFooter(in);
      }
    } catch (DataFormatException e) {
      throw new DecompressionException("decompression failure", e);
    } finally {
      if (decompressed.isReadable()) {
        out.add(decompressed);
      } else {
        decompressed.release();
      }
    }
  }
  
  private boolean handleGzipFooter(ByteBuf in) {
    if (readGZIPFooter(in)) {
      finished = (!decompressConcatenated);
      
      if (!finished) {
        inflater.reset();
        crc.reset();
        gzipState = GzipState.HEADER_START;
        return true;
      }
    }
    return false;
  }
  
  protected void decompressionBufferExhausted(ByteBuf buffer)
  {
    finished = true;
  }
  
  protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception
  {
    super.handlerRemoved0(ctx);
    if (inflater != null) {
      inflater.end();
    }
  }
  
  private boolean readGZIPHeader(ByteBuf in) {
    switch (1.$SwitchMap$io$netty$handler$codec$compression$JdkZlibDecoder$GzipState[gzipState.ordinal()]) {
    case 1: 
      if (in.readableBytes() < 10) {
        return false;
      }
      
      int magic0 = in.readByte();
      int magic1 = in.readByte();
      
      if (magic0 != 31) {
        throw new DecompressionException("Input is not in the GZIP format");
      }
      crc.update(magic0);
      crc.update(magic1);
      
      int method = in.readUnsignedByte();
      if (method != 8) {
        throw new DecompressionException("Unsupported compression method " + method + " in the GZIP header");
      }
      
      crc.update(method);
      
      flags = in.readUnsignedByte();
      crc.update(flags);
      
      if ((flags & 0xE0) != 0) {
        throw new DecompressionException("Reserved flags are set in the GZIP header");
      }
      


      crc.update(in, in.readerIndex(), 4);
      in.skipBytes(4);
      
      crc.update(in.readUnsignedByte());
      crc.update(in.readUnsignedByte());
      
      gzipState = GzipState.FLG_READ;
    
    case 2: 
      if ((flags & 0x4) != 0) {
        if (in.readableBytes() < 2) {
          return false;
        }
        int xlen1 = in.readUnsignedByte();
        int xlen2 = in.readUnsignedByte();
        crc.update(xlen1);
        crc.update(xlen2);
        
        xlen |= xlen1 << 8 | xlen2;
      }
      gzipState = GzipState.XLEN_READ;
    
    case 3: 
      if (xlen != -1) {
        if (in.readableBytes() < xlen) {
          return false;
        }
        crc.update(in, in.readerIndex(), xlen);
        in.skipBytes(xlen);
      }
      gzipState = GzipState.SKIP_FNAME;
    
    case 4: 
      if (!skipIfNeeded(in, 8)) {
        return false;
      }
      gzipState = GzipState.SKIP_COMMENT;
    
    case 5: 
      if (!skipIfNeeded(in, 16)) {
        return false;
      }
      gzipState = GzipState.PROCESS_FHCRC;
    
    case 6: 
      if (((flags & 0x2) != 0) && 
        (!verifyCrc(in))) {
        return false;
      }
      
      crc.reset();
      gzipState = GzipState.HEADER_END;
    
    case 7: 
      return true;
    }
    throw new IllegalStateException();
  }
  







  private boolean skipIfNeeded(ByteBuf in, int flagMask)
  {
    if ((flags & flagMask) != 0) {
      for (;;) {
        if (!in.isReadable())
        {
          return false;
        }
        int b = in.readUnsignedByte();
        crc.update(b);
        if (b == 0) {
          break;
        }
      }
    }
    
    return true;
  }
  






  private boolean readGZIPFooter(ByteBuf in)
  {
    if (in.readableBytes() < 8) {
      return false;
    }
    
    boolean enoughData = verifyCrc(in);
    assert (enoughData);
    

    int dataLength = 0;
    for (int i = 0; i < 4; i++) {
      dataLength |= in.readUnsignedByte() << i * 8;
    }
    int readLength = inflater.getTotalOut();
    if (dataLength != readLength) {
      throw new DecompressionException("Number of bytes mismatch. Expected: " + dataLength + ", Got: " + readLength);
    }
    
    return true;
  }
  






  private boolean verifyCrc(ByteBuf in)
  {
    if (in.readableBytes() < 4) {
      return false;
    }
    long crcValue = 0L;
    for (int i = 0; i < 4; i++) {
      crcValue |= in.readUnsignedByte() << i * 8;
    }
    long readCrc = crc.getValue();
    if (crcValue != readCrc) {
      throw new DecompressionException("CRC value mismatch. Expected: " + crcValue + ", Got: " + readCrc);
    }
    
    return true;
  }
  






  private static boolean looksLikeZlib(short cmf_flg)
  {
    return ((cmf_flg & 0x7800) == 30720) && (cmf_flg % 31 == 0);
  }
}
