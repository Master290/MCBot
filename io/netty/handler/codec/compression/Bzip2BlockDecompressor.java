package io.netty.handler.codec.compression;



















final class Bzip2BlockDecompressor
{
  private final Bzip2BitReader reader;
  

















  private final Crc32 crc = new Crc32();
  




  private final int blockCRC;
  




  private final boolean blockRandomised;
  




  int huffmanEndOfBlockSymbol;
  



  int huffmanInUse16;
  



  final byte[] huffmanSymbolMap = new byte['Ā'];
  





  private final int[] bwtByteCounts = new int['Ā'];
  





  private final byte[] bwtBlock;
  





  private final int bwtStartPointer;
  





  private int[] bwtMergedPointers;
  




  private int bwtCurrentMergedPointer;
  




  private int bwtBlockLength;
  




  private int bwtBytesDecoded;
  




  private int rleLastDecodedByte = -1;
  




  private int rleAccumulator;
  



  private int rleRepeat;
  



  private int randomIndex;
  



  private int randomCount = Bzip2Rand.rNums(0) - 1;
  



  private final Bzip2MoveToFrontTable symbolMTF = new Bzip2MoveToFrontTable();
  
  private int repeatCount;
  
  private int repeatIncrement = 1;
  
  private int mtfValue;
  
  Bzip2BlockDecompressor(int blockSize, int blockCRC, boolean blockRandomised, int bwtStartPointer, Bzip2BitReader reader)
  {
    bwtBlock = new byte[blockSize];
    
    this.blockCRC = blockCRC;
    this.blockRandomised = blockRandomised;
    this.bwtStartPointer = bwtStartPointer;
    
    this.reader = reader;
  }
  



  boolean decodeHuffmanData(Bzip2HuffmanStageDecoder huffmanDecoder)
  {
    Bzip2BitReader reader = this.reader;
    byte[] bwtBlock = this.bwtBlock;
    byte[] huffmanSymbolMap = this.huffmanSymbolMap;
    int streamBlockSize = this.bwtBlock.length;
    int huffmanEndOfBlockSymbol = this.huffmanEndOfBlockSymbol;
    int[] bwtByteCounts = this.bwtByteCounts;
    Bzip2MoveToFrontTable symbolMTF = this.symbolMTF;
    
    int bwtBlockLength = this.bwtBlockLength;
    int repeatCount = this.repeatCount;
    int repeatIncrement = this.repeatIncrement;
    int mtfValue = this.mtfValue;
    for (;;)
    {
      if (!reader.hasReadableBits(23)) {
        this.bwtBlockLength = bwtBlockLength;
        this.repeatCount = repeatCount;
        this.repeatIncrement = repeatIncrement;
        this.mtfValue = mtfValue;
        return false;
      }
      int nextSymbol = huffmanDecoder.nextSymbol();
      
      if (nextSymbol == 0) {
        repeatCount += repeatIncrement;
        repeatIncrement <<= 1;
      } else if (nextSymbol == 1) {
        repeatCount += (repeatIncrement << 1);
        repeatIncrement <<= 1;
      } else {
        if (repeatCount > 0) {
          if (bwtBlockLength + repeatCount > streamBlockSize) {
            throw new DecompressionException("block exceeds declared block size");
          }
          byte nextByte = huffmanSymbolMap[mtfValue];
          bwtByteCounts[(nextByte & 0xFF)] += repeatCount;
          for (;;) { repeatCount--; if (repeatCount < 0) break;
            bwtBlock[(bwtBlockLength++)] = nextByte;
          }
          
          repeatCount = 0;
          repeatIncrement = 1;
        }
        
        if (nextSymbol == huffmanEndOfBlockSymbol) {
          break;
        }
        
        if (bwtBlockLength >= streamBlockSize) {
          throw new DecompressionException("block exceeds declared block size");
        }
        
        mtfValue = symbolMTF.indexToFront(nextSymbol - 1) & 0xFF;
        
        byte nextByte = huffmanSymbolMap[mtfValue];
        bwtByteCounts[(nextByte & 0xFF)] += 1;
        bwtBlock[(bwtBlockLength++)] = nextByte;
      }
    }
    this.bwtBlockLength = bwtBlockLength;
    initialiseInverseBWT();
    return true;
  }
  


  private void initialiseInverseBWT()
  {
    int bwtStartPointer = this.bwtStartPointer;
    byte[] bwtBlock = this.bwtBlock;
    int[] bwtMergedPointers = new int[bwtBlockLength];
    int[] characterBase = new int['Ā'];
    
    if ((bwtStartPointer < 0) || (bwtStartPointer >= bwtBlockLength)) {
      throw new DecompressionException("start pointer invalid");
    }
    

    System.arraycopy(bwtByteCounts, 0, characterBase, 1, 255);
    for (int i = 2; i <= 255; i++) {
      characterBase[i] += characterBase[(i - 1)];
    }
    




    for (int i = 0; i < bwtBlockLength; i++) {
      int value = bwtBlock[i] & 0xFF; int 
        tmp119_117 = value; int[] tmp119_115 = characterBase; int tmp121_120 = tmp119_115[tmp119_117];tmp119_115[tmp119_117] = (tmp121_120 + 1);bwtMergedPointers[tmp121_120] = ((i << 8) + value);
    }
    
    this.bwtMergedPointers = bwtMergedPointers;
    bwtCurrentMergedPointer = bwtMergedPointers[bwtStartPointer];
  }
  




  public int read()
  {
    while (this.rleRepeat < 1) {
      if (bwtBytesDecoded == bwtBlockLength) {
        return -1;
      }
      
      int nextByte = decodeNextBWTByte();
      if (nextByte != rleLastDecodedByte)
      {
        rleLastDecodedByte = nextByte;
        this.rleRepeat = 1;
        rleAccumulator = 1;
        crc.updateCRC(nextByte);
      }
      else if (++rleAccumulator == 4)
      {
        int rleRepeat = decodeNextBWTByte() + 1;
        this.rleRepeat = rleRepeat;
        rleAccumulator = 0;
        crc.updateCRC(nextByte, rleRepeat);
      } else {
        this.rleRepeat = 1;
        crc.updateCRC(nextByte);
      }
    }
    
    this.rleRepeat -= 1;
    
    return rleLastDecodedByte;
  }
  




  private int decodeNextBWTByte()
  {
    int mergedPointer = bwtCurrentMergedPointer;
    int nextDecodedByte = mergedPointer & 0xFF;
    bwtCurrentMergedPointer = bwtMergedPointers[(mergedPointer >>> 8)];
    
    if ((blockRandomised) && 
      (--randomCount == 0)) {
      nextDecodedByte ^= 0x1;
      randomIndex = ((randomIndex + 1) % 512);
      randomCount = Bzip2Rand.rNums(randomIndex);
    }
    
    bwtBytesDecoded += 1;
    
    return nextDecodedByte;
  }
  
  public int blockLength() {
    return bwtBlockLength;
  }
  




  int checkCRC()
  {
    int computedBlockCRC = crc.getCRC();
    if (blockCRC != computedBlockCRC) {
      throw new DecompressionException("block CRC error");
    }
    return computedBlockCRC;
  }
}
