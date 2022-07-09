package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;





























final class Bzip2BlockCompressor
{
  private final ByteProcessor writeProcessor = new ByteProcessor()
  {
    public boolean process(byte value) throws Exception {
      return write(value);
    }
  };
  



  private final Bzip2BitWriter writer;
  



  private final Crc32 crc = new Crc32();
  




  private final byte[] block;
  



  private int blockLength;
  



  private final int blockLengthLimit;
  



  private final boolean[] blockValuesPresent = new boolean['Ā'];
  



  private final int[] bwtBlock;
  



  private int rleCurrentValue = -1;
  



  private int rleLength;
  




  Bzip2BlockCompressor(Bzip2BitWriter writer, int blockSize)
  {
    this.writer = writer;
    

    block = new byte[blockSize + 1];
    bwtBlock = new int[blockSize + 1];
    blockLengthLimit = (blockSize - 6);
  }
  


  private void writeSymbolMap(ByteBuf out)
  {
    Bzip2BitWriter writer = this.writer;
    
    boolean[] blockValuesPresent = this.blockValuesPresent;
    boolean[] condensedInUse = new boolean[16];
    int j;
    int k; for (int i = 0; i < condensedInUse.length; i++) {
      j = 0; for (k = i << 4; j < 16; k++) {
        if (blockValuesPresent[k] != 0) {
          condensedInUse[i] = true;
        }
        j++;
      }
    }
    



    for (boolean isCondensedInUse : condensedInUse) {
      writer.writeBoolean(out, isCondensedInUse);
    }
    
    for (int i = 0; i < condensedInUse.length; i++) {
      if (condensedInUse[i] != 0) {
        int j = 0; for (int k = i << 4; j < 16; k++) {
          writer.writeBoolean(out, blockValuesPresent[k]);j++;
        }
      }
    }
  }
  




  private void writeRun(int value, int runLength)
  {
    int blockLength = this.blockLength;
    byte[] block = this.block;
    
    blockValuesPresent[value] = true;
    crc.updateCRC(value, runLength);
    
    byte byteValue = (byte)value;
    switch (runLength) {
    case 1: 
      block[blockLength] = byteValue;
      this.blockLength = (blockLength + 1);
      break;
    case 2: 
      block[blockLength] = byteValue;
      block[(blockLength + 1)] = byteValue;
      this.blockLength = (blockLength + 2);
      break;
    case 3: 
      block[blockLength] = byteValue;
      block[(blockLength + 1)] = byteValue;
      block[(blockLength + 2)] = byteValue;
      this.blockLength = (blockLength + 3);
      break;
    default: 
      runLength -= 4;
      blockValuesPresent[runLength] = true;
      block[blockLength] = byteValue;
      block[(blockLength + 1)] = byteValue;
      block[(blockLength + 2)] = byteValue;
      block[(blockLength + 3)] = byteValue;
      block[(blockLength + 4)] = ((byte)runLength);
      this.blockLength = (blockLength + 5);
    }
    
  }
  




  boolean write(int value)
  {
    if (blockLength > blockLengthLimit) {
      return false;
    }
    int rleCurrentValue = this.rleCurrentValue;
    int rleLength = this.rleLength;
    
    if (rleLength == 0) {
      this.rleCurrentValue = value;
      this.rleLength = 1;
    } else if (rleCurrentValue != value)
    {
      writeRun(rleCurrentValue & 0xFF, rleLength);
      this.rleCurrentValue = value;
      this.rleLength = 1;
    }
    else if (rleLength == 254) {
      writeRun(rleCurrentValue & 0xFF, 255);
      this.rleLength = 0;
    } else {
      this.rleLength = (rleLength + 1);
    }
    
    return true;
  }
  







  int write(ByteBuf buffer, int offset, int length)
  {
    int index = buffer.forEachByte(offset, length, writeProcessor);
    return index == -1 ? length : index - offset;
  }
  



  void close(ByteBuf out)
  {
    if (rleLength > 0) {
      writeRun(rleCurrentValue & 0xFF, rleLength);
    }
    

    block[blockLength] = block[0];
    

    Bzip2DivSufSort divSufSort = new Bzip2DivSufSort(block, bwtBlock, blockLength);
    int bwtStartPointer = divSufSort.bwt();
    
    Bzip2BitWriter writer = this.writer;
    

    writer.writeBits(out, 24, 3227993L);
    writer.writeBits(out, 24, 2511705L);
    writer.writeInt(out, crc.getCRC());
    writer.writeBoolean(out, false);
    writer.writeBits(out, 24, bwtStartPointer);
    

    writeSymbolMap(out);
    

    Bzip2MTFAndRLE2StageEncoder mtfEncoder = new Bzip2MTFAndRLE2StageEncoder(bwtBlock, blockLength, blockValuesPresent);
    
    mtfEncoder.encode();
    





    Bzip2HuffmanStageEncoder huffmanEncoder = new Bzip2HuffmanStageEncoder(writer, mtfEncoder.mtfBlock(), mtfEncoder.mtfLength(), mtfEncoder.mtfAlphabetSize(), mtfEncoder.mtfSymbolFrequencies());
    huffmanEncoder.encode(out);
  }
  



  int availableSize()
  {
    if (blockLength == 0) {
      return blockLengthLimit + 2;
    }
    return blockLengthLimit - blockLength + 1;
  }
  



  boolean isFull()
  {
    return blockLength > blockLengthLimit;
  }
  



  boolean isEmpty()
  {
    return (blockLength == 0) && (rleLength == 0);
  }
  



  int crc()
  {
    return crc.getCRC();
  }
}
