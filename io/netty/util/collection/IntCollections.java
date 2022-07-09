package io.netty.util.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

















public final class IntCollections
{
  private static final IntObjectMap<Object> EMPTY_MAP = new EmptyMap(null);
  


  private IntCollections() {}
  


  public static <V> IntObjectMap<V> emptyMap()
  {
    return EMPTY_MAP;
  }
  


  public static <V> IntObjectMap<V> unmodifiableMap(IntObjectMap<V> map)
  {
    return new UnmodifiableMap(map);
  }
  
  private static final class EmptyMap implements IntObjectMap<Object>
  {
    private EmptyMap() {}
    
    public Object get(int key)
    {
      return null;
    }
    
    public Object put(int key, Object value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public Object remove(int key)
    {
      return null;
    }
    
    public int size()
    {
      return 0;
    }
    
    public boolean isEmpty()
    {
      return true;
    }
    
    public boolean containsKey(Object key)
    {
      return false;
    }
    


    public void clear() {}
    

    public Set<Integer> keySet()
    {
      return Collections.emptySet();
    }
    
    public boolean containsKey(int key)
    {
      return false;
    }
    
    public boolean containsValue(Object value)
    {
      return false;
    }
    
    public Iterable<IntObjectMap.PrimitiveEntry<Object>> entries()
    {
      return Collections.emptySet();
    }
    
    public Object get(Object key)
    {
      return null;
    }
    
    public Object put(Integer key, Object value)
    {
      throw new UnsupportedOperationException();
    }
    
    public Object remove(Object key)
    {
      return null;
    }
    
    public void putAll(Map<? extends Integer, ?> m)
    {
      throw new UnsupportedOperationException();
    }
    
    public Collection<Object> values()
    {
      return Collections.emptyList();
    }
    
    public Set<Map.Entry<Integer, Object>> entrySet()
    {
      return Collections.emptySet();
    }
  }
  

  private static final class UnmodifiableMap<V>
    implements IntObjectMap<V>
  {
    private final IntObjectMap<V> map;
    
    private Set<Integer> keySet;
    private Set<Map.Entry<Integer, V>> entrySet;
    private Collection<V> values;
    private Iterable<IntObjectMap.PrimitiveEntry<V>> entries;
    
    UnmodifiableMap(IntObjectMap<V> map)
    {
      this.map = map;
    }
    
    public V get(int key)
    {
      return map.get(key);
    }
    
    public V put(int key, V value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public V remove(int key)
    {
      throw new UnsupportedOperationException("remove");
    }
    
    public int size()
    {
      return map.size();
    }
    
    public boolean isEmpty()
    {
      return map.isEmpty();
    }
    
    public void clear()
    {
      throw new UnsupportedOperationException("clear");
    }
    
    public boolean containsKey(int key)
    {
      return map.containsKey(key);
    }
    
    public boolean containsValue(Object value)
    {
      return map.containsValue(value);
    }
    
    public boolean containsKey(Object key)
    {
      return map.containsKey(key);
    }
    
    public V get(Object key)
    {
      return map.get(key);
    }
    
    public V put(Integer key, V value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public V remove(Object key)
    {
      throw new UnsupportedOperationException("remove");
    }
    
    public void putAll(Map<? extends Integer, ? extends V> m)
    {
      throw new UnsupportedOperationException("putAll");
    }
    
    public Iterable<IntObjectMap.PrimitiveEntry<V>> entries()
    {
      if (entries == null) {
        entries = new Iterable()
        {
          public Iterator<IntObjectMap.PrimitiveEntry<V>> iterator() {
            return new IntCollections.UnmodifiableMap.IteratorImpl(IntCollections.UnmodifiableMap.this, map.entries().iterator());
          }
        };
      }
      
      return entries;
    }
    
    public Set<Integer> keySet()
    {
      if (keySet == null) {
        keySet = Collections.unmodifiableSet(map.keySet());
      }
      return keySet;
    }
    
    public Set<Map.Entry<Integer, V>> entrySet()
    {
      if (entrySet == null) {
        entrySet = Collections.unmodifiableSet(map.entrySet());
      }
      return entrySet;
    }
    
    public Collection<V> values()
    {
      if (values == null) {
        values = Collections.unmodifiableCollection(map.values());
      }
      return values;
    }
    
    private class IteratorImpl
      implements Iterator<IntObjectMap.PrimitiveEntry<V>>
    {
      final Iterator<IntObjectMap.PrimitiveEntry<V>> iter;
      
      IteratorImpl()
      {
        this.iter = iter;
      }
      
      public boolean hasNext()
      {
        return iter.hasNext();
      }
      
      public IntObjectMap.PrimitiveEntry<V> next()
      {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return new IntCollections.UnmodifiableMap.EntryImpl(IntCollections.UnmodifiableMap.this, (IntObjectMap.PrimitiveEntry)iter.next());
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException("remove");
      }
    }
    
    private class EntryImpl
      implements IntObjectMap.PrimitiveEntry<V>
    {
      private final IntObjectMap.PrimitiveEntry<V> entry;
      
      EntryImpl()
      {
        this.entry = entry;
      }
      
      public int key()
      {
        return entry.key();
      }
      
      public V value()
      {
        return entry.value();
      }
      
      public void setValue(V value)
      {
        throw new UnsupportedOperationException("setValue");
      }
    }
  }
}
