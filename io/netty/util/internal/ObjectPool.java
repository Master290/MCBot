package io.netty.util.internal;

import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;






















































public abstract class ObjectPool<T>
{
  ObjectPool() {}
  
  public abstract T get();
  
  public static <T> ObjectPool<T> newPool(ObjectCreator<T> creator)
  {
    return new RecyclerObjectPool((ObjectCreator)ObjectUtil.checkNotNull(creator, "creator"));
  }
  
  private static final class RecyclerObjectPool<T> extends ObjectPool<T> {
    private final Recycler<T> recycler;
    
    RecyclerObjectPool(final ObjectPool.ObjectCreator<T> creator) {
      recycler = new Recycler()
      {
        protected T newObject(Recycler.Handle<T> handle) {
          return creator.newObject(handle);
        }
      };
    }
    
    public T get()
    {
      return recycler.get();
    }
  }
  
  public static abstract interface ObjectCreator<T>
  {
    public abstract T newObject(ObjectPool.Handle<T> paramHandle);
  }
  
  public static abstract interface Handle<T>
  {
    public abstract void recycle(T paramT);
  }
}
