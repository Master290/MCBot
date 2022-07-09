package io.netty.channel.nio;

import java.nio.channels.SelectionKey;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;














final class SelectedSelectionKeySet
  extends AbstractSet<SelectionKey>
{
  SelectionKey[] keys;
  int size;
  
  SelectedSelectionKeySet()
  {
    keys = new SelectionKey['Ѐ'];
  }
  
  public boolean add(SelectionKey o)
  {
    if (o == null) {
      return false;
    }
    
    keys[(size++)] = o;
    if (size == keys.length) {
      increaseCapacity();
    }
    
    return true;
  }
  
  public boolean remove(Object o)
  {
    return false;
  }
  
  public boolean contains(Object o)
  {
    return false;
  }
  
  public int size()
  {
    return size;
  }
  
  public Iterator<SelectionKey> iterator()
  {
    new Iterator()
    {
      private int idx;
      
      public boolean hasNext() {
        return idx < size;
      }
      
      public SelectionKey next()
      {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return keys[(idx++)];
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }
  
  void reset() {
    reset(0);
  }
  
  void reset(int start) {
    Arrays.fill(keys, start, size, null);
    size = 0;
  }
  
  private void increaseCapacity() {
    SelectionKey[] newKeys = new SelectionKey[keys.length << 1];
    System.arraycopy(keys, 0, newKeys, 0, size);
    keys = newKeys;
  }
}
