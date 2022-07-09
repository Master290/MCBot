package io.netty.buffer.search;

import io.netty.util.internal.PlatformDependent;






















public class BitapSearchProcessorFactory
  extends AbstractSearchProcessorFactory
{
  private final long[] bitMasks = new long['Ā'];
  private final long successBit;
  
  public static class Processor implements SearchProcessor
  {
    private final long[] bitMasks;
    private final long successBit;
    private long currentMask;
    
    Processor(long[] bitMasks, long successBit) {
      this.bitMasks = bitMasks;
      this.successBit = successBit;
    }
    
    public boolean process(byte value)
    {
      currentMask = ((currentMask << 1 | 1L) & PlatformDependent.getLong(bitMasks, value & 0xFF));
      return (currentMask & successBit) == 0L;
    }
    
    public void reset()
    {
      currentMask = 0L;
    }
  }
  
  BitapSearchProcessorFactory(byte[] needle) {
    if (needle.length > 64) {
      throw new IllegalArgumentException("Maximum supported search pattern length is 64, got " + needle.length);
    }
    
    long bit = 1L;
    for (byte c : needle) {
      bitMasks[(c & 0xFF)] |= bit;
      bit <<= 1;
    }
    
    successBit = (1L << needle.length - 1);
  }
  



  public Processor newSearchProcessor()
  {
    return new Processor(bitMasks, successBit);
  }
}
