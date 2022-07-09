package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.PlatformDependent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;




































public class FastThreadLocal<V>
{
  private static final int variablesToRemoveIndex = ;
  

  private final int index;
  

  public static void removeAll()
  {
    InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
    if (threadLocalMap == null) {
      return;
    }
    try
    {
      Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
      if ((v != null) && (v != InternalThreadLocalMap.UNSET))
      {
        Set<FastThreadLocal<?>> variablesToRemove = (Set)v;
        
        FastThreadLocal<?>[] variablesToRemoveArray = (FastThreadLocal[])variablesToRemove.toArray(new FastThreadLocal[0]);
        for (FastThreadLocal<?> tlv : variablesToRemoveArray) {
          tlv.remove(threadLocalMap);
        }
      }
    } finally {
      InternalThreadLocalMap.remove();
    }
  }
  


  public static int size()
  {
    InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
    if (threadLocalMap == null) {
      return 0;
    }
    return threadLocalMap.size();
  }
  





  public static void destroy() {}
  




  private static void addToVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable)
  {
    Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
    Set<FastThreadLocal<?>> variablesToRemove;
    if ((v == InternalThreadLocalMap.UNSET) || (v == null)) {
      Set<FastThreadLocal<?>> variablesToRemove = Collections.newSetFromMap(new IdentityHashMap());
      threadLocalMap.setIndexedVariable(variablesToRemoveIndex, variablesToRemove);
    } else {
      variablesToRemove = (Set)v;
    }
    
    variablesToRemove.add(variable);
  }
  

  private static void removeFromVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable)
  {
    Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
    
    if ((v == InternalThreadLocalMap.UNSET) || (v == null)) {
      return;
    }
    

    Set<FastThreadLocal<?>> variablesToRemove = (Set)v;
    variablesToRemove.remove(variable);
  }
  

  public FastThreadLocal()
  {
    index = InternalThreadLocalMap.nextVariableIndex();
  }
  



  public final V get()
  {
    InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
    Object v = threadLocalMap.indexedVariable(index);
    if (v != InternalThreadLocalMap.UNSET) {
      return v;
    }
    
    return initialize(threadLocalMap);
  }
  



  public final V getIfExists()
  {
    InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
    if (threadLocalMap != null) {
      Object v = threadLocalMap.indexedVariable(index);
      if (v != InternalThreadLocalMap.UNSET) {
        return v;
      }
    }
    return null;
  }
  




  public final V get(InternalThreadLocalMap threadLocalMap)
  {
    Object v = threadLocalMap.indexedVariable(index);
    if (v != InternalThreadLocalMap.UNSET) {
      return v;
    }
    
    return initialize(threadLocalMap);
  }
  
  private V initialize(InternalThreadLocalMap threadLocalMap) {
    V v = null;
    try {
      v = initialValue();
    } catch (Exception e) {
      PlatformDependent.throwException(e);
    }
    
    threadLocalMap.setIndexedVariable(index, v);
    addToVariablesToRemove(threadLocalMap, this);
    return v;
  }
  


  public final void set(V value)
  {
    if (value != InternalThreadLocalMap.UNSET) {
      InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
      setKnownNotUnset(threadLocalMap, value);
    } else {
      remove();
    }
  }
  


  public final void set(InternalThreadLocalMap threadLocalMap, V value)
  {
    if (value != InternalThreadLocalMap.UNSET) {
      setKnownNotUnset(threadLocalMap, value);
    } else {
      remove(threadLocalMap);
    }
  }
  


  private void setKnownNotUnset(InternalThreadLocalMap threadLocalMap, V value)
  {
    if (threadLocalMap.setIndexedVariable(index, value)) {
      addToVariablesToRemove(threadLocalMap, this);
    }
  }
  


  public final boolean isSet()
  {
    return isSet(InternalThreadLocalMap.getIfSet());
  }
  



  public final boolean isSet(InternalThreadLocalMap threadLocalMap)
  {
    return (threadLocalMap != null) && (threadLocalMap.isIndexedVariableSet(index));
  }
  


  public final void remove()
  {
    remove(InternalThreadLocalMap.getIfSet());
  }
  





  public final void remove(InternalThreadLocalMap threadLocalMap)
  {
    if (threadLocalMap == null) {
      return;
    }
    
    Object v = threadLocalMap.removeIndexedVariable(index);
    removeFromVariablesToRemove(threadLocalMap, this);
    
    if (v != InternalThreadLocalMap.UNSET) {
      try {
        onRemoval(v);
      } catch (Exception e) {
        PlatformDependent.throwException(e);
      }
    }
  }
  

  protected V initialValue()
    throws Exception
  {
    return null;
  }
  
  protected void onRemoval(V value)
    throws Exception
  {}
}
