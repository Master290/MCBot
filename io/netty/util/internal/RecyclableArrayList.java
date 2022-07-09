package io.netty.util.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
























public final class RecyclableArrayList
  extends ArrayList<Object>
{
  private static final long serialVersionUID = -8605125654176467947L;
  private static final int DEFAULT_INITIAL_CAPACITY = 8;
  private static final ObjectPool<RecyclableArrayList> RECYCLER = ObjectPool.newPool(new ObjectPool.ObjectCreator()
  {
    public RecyclableArrayList newObject(ObjectPool.Handle<RecyclableArrayList> handle)
    {
      return new RecyclableArrayList(handle, null);
    }
  });
  


  private boolean insertSinceRecycled;
  


  private final ObjectPool.Handle<RecyclableArrayList> handle;
  


  public static RecyclableArrayList newInstance()
  {
    return newInstance(8);
  }
  


  public static RecyclableArrayList newInstance(int minCapacity)
  {
    RecyclableArrayList ret = (RecyclableArrayList)RECYCLER.get();
    ret.ensureCapacity(minCapacity);
    return ret;
  }
  

  private RecyclableArrayList(ObjectPool.Handle<RecyclableArrayList> handle)
  {
    this(handle, 8);
  }
  
  private RecyclableArrayList(ObjectPool.Handle<RecyclableArrayList> handle, int initialCapacity) {
    super(initialCapacity);
    this.handle = handle;
  }
  
  public boolean addAll(Collection<?> c)
  {
    checkNullElements(c);
    if (super.addAll(c)) {
      insertSinceRecycled = true;
      return true;
    }
    return false;
  }
  
  public boolean addAll(int index, Collection<?> c)
  {
    checkNullElements(c);
    if (super.addAll(index, c)) {
      insertSinceRecycled = true;
      return true;
    }
    return false;
  }
  
  private static void checkNullElements(Collection<?> c) { List<?> list;
    if (((c instanceof RandomAccess)) && ((c instanceof List)))
    {
      list = (List)c;
      int size = list.size();
      for (int i = 0; i < size; i++) {
        if (list.get(i) == null) {
          throw new IllegalArgumentException("c contains null values");
        }
      }
    } else {
      for (Object element : c) {
        if (element == null) {
          throw new IllegalArgumentException("c contains null values");
        }
      }
    }
  }
  
  public boolean add(Object element)
  {
    if (super.add(ObjectUtil.checkNotNull(element, "element"))) {
      insertSinceRecycled = true;
      return true;
    }
    return false;
  }
  
  public void add(int index, Object element)
  {
    super.add(index, ObjectUtil.checkNotNull(element, "element"));
    insertSinceRecycled = true;
  }
  
  public Object set(int index, Object element)
  {
    Object old = super.set(index, ObjectUtil.checkNotNull(element, "element"));
    insertSinceRecycled = true;
    return old;
  }
  


  public boolean insertSinceRecycled()
  {
    return insertSinceRecycled;
  }
  


  public boolean recycle()
  {
    clear();
    insertSinceRecycled = false;
    handle.recycle(this);
    return true;
  }
}
