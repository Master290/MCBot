package io.netty.handler.codec.compression;









final class Bzip2MTFAndRLE2StageEncoder
{
  private final int[] bwtBlock;
  







  private final int bwtLength;
  







  private final boolean[] bwtValuesPresent;
  






  private final char[] mtfBlock;
  






  private int mtfLength;
  






  private final int[] mtfSymbolFrequencies = new int['Ă'];
  




  private int alphabetSize;
  




  Bzip2MTFAndRLE2StageEncoder(int[] bwtBlock, int bwtLength, boolean[] bwtValuesPresent)
  {
    this.bwtBlock = bwtBlock;
    this.bwtLength = bwtLength;
    this.bwtValuesPresent = bwtValuesPresent;
    mtfBlock = new char[bwtLength + 1];
  }
  


  void encode()
  {
    int bwtLength = this.bwtLength;
    boolean[] bwtValuesPresent = this.bwtValuesPresent;
    int[] bwtBlock = this.bwtBlock;
    char[] mtfBlock = this.mtfBlock;
    int[] mtfSymbolFrequencies = this.mtfSymbolFrequencies;
    byte[] huffmanSymbolMap = new byte['Ā'];
    Bzip2MoveToFrontTable symbolMTF = new Bzip2MoveToFrontTable();
    
    int totalUniqueValues = 0;
    for (int i = 0; i < huffmanSymbolMap.length; i++) {
      if (bwtValuesPresent[i] != 0) {
        huffmanSymbolMap[i] = ((byte)totalUniqueValues++);
      }
    }
    int endOfBlockSymbol = totalUniqueValues + 1;
    
    int mtfIndex = 0;
    int repeatCount = 0;
    int totalRunAs = 0;
    int totalRunBs = 0;
    for (int i = 0; i < bwtLength; i++)
    {
      int mtfPosition = symbolMTF.valueToFront(huffmanSymbolMap[(bwtBlock[i] & 0xFF)]);
      
      if (mtfPosition == 0) {
        repeatCount++;
      } else {
        if (repeatCount > 0) {
          repeatCount--;
          for (;;) {
            if ((repeatCount & 0x1) == 0) {
              mtfBlock[(mtfIndex++)] = '\000';
              totalRunAs++;
            } else {
              mtfBlock[(mtfIndex++)] = '\001';
              totalRunBs++;
            }
            
            if (repeatCount <= 1) {
              break;
            }
            repeatCount = repeatCount - 2 >>> 1;
          }
          repeatCount = 0;
        }
        mtfBlock[(mtfIndex++)] = ((char)(mtfPosition + 1));
        mtfSymbolFrequencies[(mtfPosition + 1)] += 1;
      }
    }
    
    if (repeatCount > 0) {
      repeatCount--;
      for (;;) {
        if ((repeatCount & 0x1) == 0) {
          mtfBlock[(mtfIndex++)] = '\000';
          totalRunAs++;
        } else {
          mtfBlock[(mtfIndex++)] = '\001';
          totalRunBs++;
        }
        
        if (repeatCount <= 1) {
          break;
        }
        repeatCount = repeatCount - 2 >>> 1;
      }
    }
    
    mtfBlock[mtfIndex] = ((char)endOfBlockSymbol);
    mtfSymbolFrequencies[endOfBlockSymbol] += 1;
    mtfSymbolFrequencies[0] += totalRunAs;
    mtfSymbolFrequencies[1] += totalRunBs;
    
    mtfLength = (mtfIndex + 1);
    alphabetSize = (endOfBlockSymbol + 1);
  }
  


  char[] mtfBlock()
  {
    return mtfBlock;
  }
  


  int mtfLength()
  {
    return mtfLength;
  }
  


  int mtfAlphabetSize()
  {
    return alphabetSize;
  }
  


  int[] mtfSymbolFrequencies()
  {
    return mtfSymbolFrequencies;
  }
}
