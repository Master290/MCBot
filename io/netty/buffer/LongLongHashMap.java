package io.netty.buffer;




final class LongLongHashMap
{
  private static final int MASK_TEMPLATE = -2;
  


  private int mask;
  


  private long[] array;
  

  private int maxProbe;
  

  private long zeroVal;
  

  private final long emptyVal;
  


  LongLongHashMap(long emptyVal)
  {
    this.emptyVal = emptyVal;
    zeroVal = emptyVal;
    int initialSize = 32;
    array = new long[initialSize];
    mask = (initialSize - 1);
    computeMaskAndProbe();
  }
  
  public long put(long key, long value) {
    if (key == 0L) {
      long prev = zeroVal;
      zeroVal = value;
      return prev;
    }
    for (;;)
    {
      int index = index(key);
      for (int i = 0; i < maxProbe; i++) {
        long existing = array[index];
        if ((existing == key) || (existing == 0L)) {
          long prev = existing == 0L ? emptyVal : array[(index + 1)];
          array[index] = key;
          array[(index + 1)] = value;
          for (; i < maxProbe; i++) {
            index = index + 2 & mask;
            if (array[index] == key) {
              array[index] = 0L;
              prev = array[(index + 1)];
              break;
            }
          }
          return prev;
        }
        index = index + 2 & mask;
      }
      expand();
    }
  }
  
  public void remove(long key) {
    if (key == 0L) {
      zeroVal = emptyVal;
      return;
    }
    int index = index(key);
    for (int i = 0; i < maxProbe; i++) {
      long existing = array[index];
      if (existing == key) {
        array[index] = 0L;
        break;
      }
      index = index + 2 & mask;
    }
  }
  
  public long get(long key) {
    if (key == 0L) {
      return zeroVal;
    }
    int index = index(key);
    for (int i = 0; i < maxProbe; i++) {
      long existing = array[index];
      if (existing == key) {
        return array[(index + 1)];
      }
      index = index + 2 & mask;
    }
    return emptyVal;
  }
  
  private int index(long key)
  {
    key ^= key >>> 33;
    key *= -49064778989728563L;
    key ^= key >>> 33;
    key *= -4265267296055464877L;
    key ^= key >>> 33;
    return (int)key & mask;
  }
  
  private void expand() {
    long[] prev = array;
    array = new long[prev.length * 2];
    computeMaskAndProbe();
    for (int i = 0; i < prev.length; i += 2) {
      long key = prev[i];
      if (key != 0L) {
        long val = prev[(i + 1)];
        put(key, val);
      }
    }
  }
  
  private void computeMaskAndProbe() {
    int length = array.length;
    mask = (length - 1 & 0xFFFFFFFE);
    maxProbe = ((int)Math.log(length));
  }
}
