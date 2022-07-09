package io.netty.handler.codec;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.MathUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.AbstractList;
import java.util.RandomAccess;



















final class CodecOutputList
  extends AbstractList<Object>
  implements RandomAccess
{
  private static final CodecOutputListRecycler NOOP_RECYCLER = new CodecOutputListRecycler()
  {
    public void recycle(CodecOutputList object) {}
  };
  


  private static final FastThreadLocal<CodecOutputLists> CODEC_OUTPUT_LISTS_POOL = new FastThreadLocal()
  {
    protected CodecOutputList.CodecOutputLists initialValue()
      throws Exception
    {
      return new CodecOutputList.CodecOutputLists(16);
    }
  };
  private final CodecOutputListRecycler recycler;
  private int size;
  private Object[] array;
  private boolean insertSinceRecycled;
  
  private static final class CodecOutputLists implements CodecOutputList.CodecOutputListRecycler
  {
    private final CodecOutputList[] elements;
    private final int mask;
    private int currentIdx;
    private int count;
    
    CodecOutputLists(int numElements) {
      elements = new CodecOutputList[MathUtil.safeFindNextPositivePowerOfTwo(numElements)];
      for (int i = 0; i < elements.length; i++)
      {
        elements[i] = new CodecOutputList(this, 16, null);
      }
      count = elements.length;
      currentIdx = elements.length;
      mask = (elements.length - 1);
    }
    
    public CodecOutputList getOrCreate() {
      if (count == 0)
      {

        return new CodecOutputList(CodecOutputList.NOOP_RECYCLER, 4, null);
      }
      count -= 1;
      
      int idx = currentIdx - 1 & mask;
      CodecOutputList list = elements[idx];
      currentIdx = idx;
      return list;
    }
    
    public void recycle(CodecOutputList codecOutputList)
    {
      int idx = currentIdx;
      elements[idx] = codecOutputList;
      currentIdx = (idx + 1 & mask);
      count += 1;
      assert (count <= elements.length);
    }
  }
  
  static CodecOutputList newInstance() {
    return ((CodecOutputLists)CODEC_OUTPUT_LISTS_POOL.get()).getOrCreate();
  }
  




  private CodecOutputList(CodecOutputListRecycler recycler, int size)
  {
    this.recycler = recycler;
    array = new Object[size];
  }
  
  public Object get(int index)
  {
    checkIndex(index);
    return array[index];
  }
  
  public int size()
  {
    return size;
  }
  
  public boolean add(Object element)
  {
    ObjectUtil.checkNotNull(element, "element");
    try {
      insert(size, element);
    }
    catch (IndexOutOfBoundsException ignore) {
      expandArray();
      insert(size, element);
    }
    size += 1;
    return true;
  }
  
  public Object set(int index, Object element)
  {
    ObjectUtil.checkNotNull(element, "element");
    checkIndex(index);
    
    Object old = array[index];
    insert(index, element);
    return old;
  }
  
  public void add(int index, Object element)
  {
    ObjectUtil.checkNotNull(element, "element");
    checkIndex(index);
    
    if (size == array.length) {
      expandArray();
    }
    
    if (index != size) {
      System.arraycopy(array, index, array, index + 1, size - index);
    }
    
    insert(index, element);
    size += 1;
  }
  
  public Object remove(int index)
  {
    checkIndex(index);
    Object old = array[index];
    
    int len = size - index - 1;
    if (len > 0) {
      System.arraycopy(array, index + 1, array, index, len);
    }
    array[(--size)] = null;
    
    return old;
  }
  


  public void clear()
  {
    size = 0;
  }
  


  boolean insertSinceRecycled()
  {
    return insertSinceRecycled;
  }
  


  void recycle()
  {
    for (int i = 0; i < size; i++) {
      array[i] = null;
    }
    size = 0;
    insertSinceRecycled = false;
    
    recycler.recycle(this);
  }
  


  Object getUnsafe(int index)
  {
    return array[index];
  }
  
  private void checkIndex(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("expected: index < (" + size + "),but actual is (" + size + ")");
    }
  }
  
  private void insert(int index, Object element)
  {
    array[index] = element;
    insertSinceRecycled = true;
  }
  
  private void expandArray()
  {
    int newCapacity = array.length << 1;
    
    if (newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    
    Object[] newArray = new Object[newCapacity];
    System.arraycopy(array, 0, newArray, 0, array.length);
    
    array = newArray;
  }
  
  private static abstract interface CodecOutputListRecycler
  {
    public abstract void recycle(CodecOutputList paramCodecOutputList);
  }
}
