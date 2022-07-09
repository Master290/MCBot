package io.netty.buffer.search;

import io.netty.util.internal.PlatformDependent;






















public class KmpSearchProcessorFactory
  extends AbstractSearchProcessorFactory
{
  private final int[] jumpTable;
  private final byte[] needle;
  
  public static class Processor
    implements SearchProcessor
  {
    private final byte[] needle;
    private final int[] jumpTable;
    private long currentPosition;
    
    Processor(byte[] needle, int[] jumpTable)
    {
      this.needle = needle;
      this.jumpTable = jumpTable;
    }
    
    public boolean process(byte value)
    {
      while ((currentPosition > 0L) && (PlatformDependent.getByte(needle, currentPosition) != value)) {
        currentPosition = PlatformDependent.getInt(jumpTable, currentPosition);
      }
      if (PlatformDependent.getByte(needle, currentPosition) == value) {
        currentPosition += 1L;
      }
      if (currentPosition == needle.length) {
        currentPosition = PlatformDependent.getInt(jumpTable, currentPosition);
        return false;
      }
      
      return true;
    }
    
    public void reset()
    {
      currentPosition = 0L;
    }
  }
  
  KmpSearchProcessorFactory(byte[] needle) {
    this.needle = ((byte[])needle.clone());
    jumpTable = new int[needle.length + 1];
    
    int j = 0;
    for (int i = 1; i < needle.length; i++) {
      while ((j > 0) && (needle[j] != needle[i])) {
        j = jumpTable[j];
      }
      if (needle[j] == needle[i]) {
        j++;
      }
      jumpTable[(i + 1)] = j;
    }
  }
  



  public Processor newSearchProcessor()
  {
    return new Processor(needle, jumpTable);
  }
}
