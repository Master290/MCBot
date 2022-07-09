package io.netty.buffer.search;

import io.netty.util.internal.PlatformDependent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;




























public class AhoCorasicSearchProcessorFactory
  extends AbstractMultiSearchProcessorFactory
{
  private final int[] jumpTable;
  private final int[] matchForNeedleId;
  static final int BITS_PER_SYMBOL = 8;
  static final int ALPHABET_SIZE = 256;
  
  public static class Processor
    implements MultiSearchProcessor
  {
    private final int[] jumpTable;
    private final int[] matchForNeedleId;
    private long currentPosition;
    
    Processor(int[] jumpTable, int[] matchForNeedleId)
    {
      this.jumpTable = jumpTable;
      this.matchForNeedleId = matchForNeedleId;
    }
    
    public boolean process(byte value)
    {
      currentPosition = PlatformDependent.getInt(jumpTable, currentPosition | value & 0xFF);
      if (currentPosition < 0L) {
        currentPosition = (-currentPosition);
        return false;
      }
      return true;
    }
    
    public int getFoundNeedleId()
    {
      return matchForNeedleId[((int)currentPosition >> 8)];
    }
    
    public void reset()
    {
      currentPosition = 0L;
    }
  }
  
  AhoCorasicSearchProcessorFactory(byte[]... needles)
  {
    for (byte[] needle : needles) {
      if (needle.length == 0) {
        throw new IllegalArgumentException("Needle must be non empty");
      }
    }
    
    Context context = buildTrie(needles);
    jumpTable = jumpTable;
    matchForNeedleId = matchForNeedleId;
    
    linkSuffixes();
    
    for (int i = 0; i < jumpTable.length; i++) {
      if (matchForNeedleId[(jumpTable[i] >> 8)] >= 0) {
        jumpTable[i] = (-jumpTable[i]);
      }
    }
  }
  
  private static Context buildTrie(byte[][] needles)
  {
    ArrayList<Integer> jumpTableBuilder = new ArrayList(256);
    for (int i = 0; i < 256; i++) {
      jumpTableBuilder.add(Integer.valueOf(-1));
    }
    
    ArrayList<Integer> matchForBuilder = new ArrayList();
    matchForBuilder.add(Integer.valueOf(-1));
    
    for (int needleId = 0; needleId < needles.length; needleId++) {
      byte[] needle = needles[needleId];
      int currentPosition = 0;
      
      for (byte ch0 : needle)
      {
        int ch = ch0 & 0xFF;
        int next = currentPosition + ch;
        
        if (((Integer)jumpTableBuilder.get(next)).intValue() == -1) {
          jumpTableBuilder.set(next, Integer.valueOf(jumpTableBuilder.size()));
          for (int i = 0; i < 256; i++) {
            jumpTableBuilder.add(Integer.valueOf(-1));
          }
          matchForBuilder.add(Integer.valueOf(-1));
        }
        
        currentPosition = ((Integer)jumpTableBuilder.get(next)).intValue();
      }
      
      matchForBuilder.set(currentPosition >> 8, Integer.valueOf(needleId));
    }
    
    Context context = new Context(null);
    
    jumpTable = new int[jumpTableBuilder.size()];
    for (int i = 0; i < jumpTableBuilder.size(); i++) {
      jumpTable[i] = ((Integer)jumpTableBuilder.get(i)).intValue();
    }
    
    matchForNeedleId = new int[matchForBuilder.size()];
    for (int i = 0; i < matchForBuilder.size(); i++) {
      matchForNeedleId[i] = ((Integer)matchForBuilder.get(i)).intValue();
    }
    
    return context;
  }
  
  private void linkSuffixes()
  {
    Queue<Integer> queue = new ArrayDeque();
    queue.add(Integer.valueOf(0));
    
    int[] suffixLinks = new int[matchForNeedleId.length];
    Arrays.fill(suffixLinks, -1);
    
    while (!queue.isEmpty())
    {
      int v = ((Integer)queue.remove()).intValue();
      int vPosition = v >> 8;
      int u = suffixLinks[vPosition] == -1 ? 0 : suffixLinks[vPosition];
      
      if (matchForNeedleId[vPosition] == -1) {
        matchForNeedleId[vPosition] = matchForNeedleId[(u >> 8)];
      }
      
      for (int ch = 0; ch < 256; ch++)
      {
        int vIndex = v | ch;
        int uIndex = u | ch;
        
        int jumpV = jumpTable[vIndex];
        int jumpU = jumpTable[uIndex];
        
        if (jumpV != -1) {
          suffixLinks[(jumpV >> 8)] = ((v > 0) && (jumpU != -1) ? jumpU : 0);
          queue.add(Integer.valueOf(jumpV));
        } else {
          jumpTable[vIndex] = (jumpU != -1 ? jumpU : 0);
        }
      }
    }
  }
  



  public Processor newSearchProcessor()
  {
    return new Processor(jumpTable, matchForNeedleId);
  }
  
  private static class Context
  {
    int[] jumpTable;
    int[] matchForNeedleId;
    
    private Context() {}
  }
}
