package io.netty.channel;

import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.List;






























public class AdaptiveRecvByteBufAllocator
  extends DefaultMaxMessagesRecvByteBufAllocator
{
  static final int DEFAULT_MINIMUM = 64;
  static final int DEFAULT_INITIAL = 2048;
  static final int DEFAULT_MAXIMUM = 65536;
  private static final int INDEX_INCREMENT = 4;
  private static final int INDEX_DECREMENT = 1;
  private static final int[] SIZE_TABLE;
  
  static
  {
    List<Integer> sizeTable = new ArrayList();
    for (int i = 16; i < 512; i += 16) {
      sizeTable.add(Integer.valueOf(i));
    }
    

    for (int i = 512; i > 0; i <<= 1) {
      sizeTable.add(Integer.valueOf(i));
    }
    
    SIZE_TABLE = new int[sizeTable.size()];
    for (int i = 0; i < SIZE_TABLE.length; i++) {
      SIZE_TABLE[i] = ((Integer)sizeTable.get(i)).intValue();
    }
  }
  



  @Deprecated
  public static final AdaptiveRecvByteBufAllocator DEFAULT = new AdaptiveRecvByteBufAllocator();
  private final int minIndex;
  
  private static int getSizeTableIndex(int size) { int low = 0;int high = SIZE_TABLE.length - 1;
    for (;;) { if (high < low) {
        return low;
      }
      if (high == low) {
        return high;
      }
      
      int mid = low + high >>> 1;
      int a = SIZE_TABLE[mid];
      int b = SIZE_TABLE[(mid + 1)];
      if (size > b) {
        low = mid + 1;
      } else if (size < a) {
        high = mid - 1;
      } else { if (size == a) {
          return mid;
        }
        return mid + 1;
      }
    }
  }
  
  private final class HandleImpl extends DefaultMaxMessagesRecvByteBufAllocator.MaxMessageHandle {
    private final int minIndex;
    private final int maxIndex;
    private int index;
    private int nextReceiveBufferSize;
    private boolean decreaseNow;
    
    HandleImpl(int minIndex, int maxIndex, int initial) { super();
      this.minIndex = minIndex;
      this.maxIndex = maxIndex;
      
      index = AdaptiveRecvByteBufAllocator.getSizeTableIndex(initial);
      nextReceiveBufferSize = AdaptiveRecvByteBufAllocator.SIZE_TABLE[index];
    }
    




    public void lastBytesRead(int bytes)
    {
      if (bytes == attemptedBytesRead()) {
        record(bytes);
      }
      super.lastBytesRead(bytes);
    }
    
    public int guess()
    {
      return nextReceiveBufferSize;
    }
    
    private void record(int actualReadBytes) {
      if (actualReadBytes <= AdaptiveRecvByteBufAllocator.SIZE_TABLE[Math.max(0, index - 1)]) {
        if (decreaseNow) {
          index = Math.max(index - 1, minIndex);
          nextReceiveBufferSize = AdaptiveRecvByteBufAllocator.SIZE_TABLE[index];
          decreaseNow = false;
        } else {
          decreaseNow = true;
        }
      } else if (actualReadBytes >= nextReceiveBufferSize) {
        index = Math.min(index + 4, maxIndex);
        nextReceiveBufferSize = AdaptiveRecvByteBufAllocator.SIZE_TABLE[index];
        decreaseNow = false;
      }
    }
    
    public void readComplete()
    {
      record(totalBytesRead());
    }
  }
  


  private final int maxIndex;
  

  private final int initial;
  

  public AdaptiveRecvByteBufAllocator()
  {
    this(64, 2048, 65536);
  }
  






  public AdaptiveRecvByteBufAllocator(int minimum, int initial, int maximum)
  {
    ObjectUtil.checkPositive(minimum, "minimum");
    if (initial < minimum) {
      throw new IllegalArgumentException("initial: " + initial);
    }
    if (maximum < initial) {
      throw new IllegalArgumentException("maximum: " + maximum);
    }
    
    int minIndex = getSizeTableIndex(minimum);
    if (SIZE_TABLE[minIndex] < minimum) {
      this.minIndex = (minIndex + 1);
    } else {
      this.minIndex = minIndex;
    }
    
    int maxIndex = getSizeTableIndex(maximum);
    if (SIZE_TABLE[maxIndex] > maximum) {
      this.maxIndex = (maxIndex - 1);
    } else {
      this.maxIndex = maxIndex;
    }
    
    this.initial = initial;
  }
  

  public RecvByteBufAllocator.Handle newHandle()
  {
    return new HandleImpl(minIndex, maxIndex, initial);
  }
  
  public AdaptiveRecvByteBufAllocator respectMaybeMoreData(boolean respectMaybeMoreData)
  {
    super.respectMaybeMoreData(respectMaybeMoreData);
    return this;
  }
}
