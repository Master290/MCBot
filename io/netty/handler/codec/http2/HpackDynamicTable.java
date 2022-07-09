package io.netty.handler.codec.http2;








final class HpackDynamicTable
{
  HpackHeaderField[] hpackHeaderFields;
  






  int head;
  






  int tail;
  






  private long size;
  






  private long capacity = -1L;
  


  HpackDynamicTable(long initialCapacity)
  {
    setCapacity(initialCapacity);
  }
  

  public int length()
  {
    int length;
    int length;
    if (head < tail) {
      length = hpackHeaderFields.length - tail + head;
    } else {
      length = head - tail;
    }
    return length;
  }
  


  public long size()
  {
    return size;
  }
  


  public long capacity()
  {
    return capacity;
  }
  



  public HpackHeaderField getEntry(int index)
  {
    if ((index <= 0) || (index > length())) {
      throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length());
    }
    int i = head - index;
    if (i < 0) {
      return hpackHeaderFields[(i + hpackHeaderFields.length)];
    }
    return hpackHeaderFields[i];
  }
  






  public void add(HpackHeaderField header)
  {
    int headerSize = header.size();
    if (headerSize > capacity) {
      clear();
      return;
    }
    while (capacity - size < headerSize) {
      remove();
    }
    hpackHeaderFields[(head++)] = header;
    size += headerSize;
    if (head == hpackHeaderFields.length) {
      head = 0;
    }
  }
  


  public HpackHeaderField remove()
  {
    HpackHeaderField removed = hpackHeaderFields[tail];
    if (removed == null) {
      return null;
    }
    size -= removed.size();
    hpackHeaderFields[(tail++)] = null;
    if (tail == hpackHeaderFields.length) {
      tail = 0;
    }
    return removed;
  }
  


  public void clear()
  {
    while (tail != head) {
      hpackHeaderFields[(tail++)] = null;
      if (tail == hpackHeaderFields.length) {
        tail = 0;
      }
    }
    head = 0;
    tail = 0;
    size = 0L;
  }
  



  public void setCapacity(long capacity)
  {
    if ((capacity < 0L) || (capacity > 4294967295L)) {
      throw new IllegalArgumentException("capacity is invalid: " + capacity);
    }
    
    if (this.capacity == capacity) {
      return;
    }
    this.capacity = capacity;
    
    if (capacity == 0L) {
      clear();
    }
    else {
      while (size > capacity) {
        remove();
      }
    }
    
    int maxEntries = (int)(capacity / 32L);
    if (capacity % 32L != 0L) {
      maxEntries++;
    }
    

    if ((hpackHeaderFields != null) && (hpackHeaderFields.length == maxEntries)) {
      return;
    }
    
    HpackHeaderField[] tmp = new HpackHeaderField[maxEntries];
    

    int len = length();
    if (hpackHeaderFields != null) {
      int cursor = tail;
      for (int i = 0; i < len; i++) {
        HpackHeaderField entry = hpackHeaderFields[(cursor++)];
        tmp[i] = entry;
        if (cursor == hpackHeaderFields.length) {
          cursor = 0;
        }
      }
    }
    
    tail = 0;
    head = (tail + len);
    hpackHeaderFields = tmp;
  }
}
