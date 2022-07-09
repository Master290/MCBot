package io.netty.util.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

















public final class LongCollections
{
  private static final LongObjectMap<Object> EMPTY_MAP = new EmptyMap(null);
  


  private LongCollections() {}
  


  public static <V> LongObjectMap<V> emptyMap()
  {
    return EMPTY_MAP;
  }
  


  public static <V> LongObjectMap<V> unmodifiableMap(LongObjectMap<V> map)
  {
    return new UnmodifiableMap(map);
  }
  
  private static final class EmptyMap implements LongObjectMap<Object>
  {
    private EmptyMap() {}
    
    public Object get(long key)
    {
      return null;
    }
    
    public Object put(long key, Object value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public Object remove(long key)
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
    

    public Set<Long> keySet()
    {
      return Collections.emptySet();
    }
    
    public boolean containsKey(long key)
    {
      return false;
    }
    
    public boolean containsValue(Object value)
    {
      return false;
    }
    
    public Iterable<LongObjectMap.PrimitiveEntry<Object>> entries()
    {
      return Collections.emptySet();
    }
    
    public Object get(Object key)
    {
      return null;
    }
    
    public Object put(Long key, Object value)
    {
      throw new UnsupportedOperationException();
    }
    
    public Object remove(Object key)
    {
      return null;
    }
    
    public void putAll(Map<? extends Long, ?> m)
    {
      throw new UnsupportedOperationException();
    }
    
    public Collection<Object> values()
    {
      return Collections.emptyList();
    }
    
    public Set<Map.Entry<Long, Object>> entrySet()
    {
      return Collections.emptySet();
    }
  }
  

  private static final class UnmodifiableMap<V>
    implements LongObjectMap<V>
  {
    private final LongObjectMap<V> map;
    
    private Set<Long> keySet;
    private Set<Map.Entry<Long, V>> entrySet;
    private Collection<V> values;
    private Iterable<LongObjectMap.PrimitiveEntry<V>> entries;
    
    UnmodifiableMap(LongObjectMap<V> map)
    {
      this.map = map;
    }
    
    public V get(long key)
    {
      return map.get(key);
    }
    
    public V put(long key, V value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public V remove(long key)
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
    
    public boolean containsKey(long key)
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
    
    public V put(Long key, V value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public V remove(Object key)
    {
      throw new UnsupportedOperationException("remove");
    }
    
    public void putAll(Map<? extends Long, ? extends V> m)
    {
      throw new UnsupportedOperationException("putAll");
    }
    
    public Iterable<LongObjectMap.PrimitiveEntry<V>> entries()
    {
      if (entries == null) {
        entries = new Iterable()
        {
          public Iterator<LongObjectMap.PrimitiveEntry<V>> iterator() {
            return new LongCollections.UnmodifiableMap.IteratorImpl(LongCollections.UnmodifiableMap.this, map.entries().iterator());
          }
        };
      }
      
      return entries;
    }
    
    public Set<Long> keySet()
    {
      if (keySet == null) {
        keySet = Collections.unmodifiableSet(map.keySet());
      }
      return keySet;
    }
    
    public Set<Map.Entry<Long, V>> entrySet()
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
      implements Iterator<LongObjectMap.PrimitiveEntry<V>>
    {
      final Iterator<LongObjectMap.PrimitiveEntry<V>> iter;
      
      IteratorImpl()
      {
        this.iter = iter;
      }
      
      public boolean hasNext()
      {
        return iter.hasNext();
      }
      
      public LongObjectMap.PrimitiveEntry<V> next()
      {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return new LongCollections.UnmodifiableMap.EntryImpl(LongCollections.UnmodifiableMap.this, (LongObjectMap.PrimitiveEntry)iter.next());
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException("remove");
      }
    }
    
    private class EntryImpl
      implements LongObjectMap.PrimitiveEntry<V>
    {
      private final LongObjectMap.PrimitiveEntry<V> entry;
      
      EntryImpl()
      {
        this.entry = entry;
      }
      
      public long key()
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
