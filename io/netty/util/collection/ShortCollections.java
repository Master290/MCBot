package io.netty.util.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

















public final class ShortCollections
{
  private static final ShortObjectMap<Object> EMPTY_MAP = new EmptyMap(null);
  


  private ShortCollections() {}
  


  public static <V> ShortObjectMap<V> emptyMap()
  {
    return EMPTY_MAP;
  }
  


  public static <V> ShortObjectMap<V> unmodifiableMap(ShortObjectMap<V> map)
  {
    return new UnmodifiableMap(map);
  }
  
  private static final class EmptyMap implements ShortObjectMap<Object>
  {
    private EmptyMap() {}
    
    public Object get(short key)
    {
      return null;
    }
    
    public Object put(short key, Object value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public Object remove(short key)
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
    

    public Set<Short> keySet()
    {
      return Collections.emptySet();
    }
    
    public boolean containsKey(short key)
    {
      return false;
    }
    
    public boolean containsValue(Object value)
    {
      return false;
    }
    
    public Iterable<ShortObjectMap.PrimitiveEntry<Object>> entries()
    {
      return Collections.emptySet();
    }
    
    public Object get(Object key)
    {
      return null;
    }
    
    public Object put(Short key, Object value)
    {
      throw new UnsupportedOperationException();
    }
    
    public Object remove(Object key)
    {
      return null;
    }
    
    public void putAll(Map<? extends Short, ?> m)
    {
      throw new UnsupportedOperationException();
    }
    
    public Collection<Object> values()
    {
      return Collections.emptyList();
    }
    
    public Set<Map.Entry<Short, Object>> entrySet()
    {
      return Collections.emptySet();
    }
  }
  

  private static final class UnmodifiableMap<V>
    implements ShortObjectMap<V>
  {
    private final ShortObjectMap<V> map;
    
    private Set<Short> keySet;
    private Set<Map.Entry<Short, V>> entrySet;
    private Collection<V> values;
    private Iterable<ShortObjectMap.PrimitiveEntry<V>> entries;
    
    UnmodifiableMap(ShortObjectMap<V> map)
    {
      this.map = map;
    }
    
    public V get(short key)
    {
      return map.get(key);
    }
    
    public V put(short key, V value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public V remove(short key)
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
    
    public boolean containsKey(short key)
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
    
    public V put(Short key, V value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public V remove(Object key)
    {
      throw new UnsupportedOperationException("remove");
    }
    
    public void putAll(Map<? extends Short, ? extends V> m)
    {
      throw new UnsupportedOperationException("putAll");
    }
    
    public Iterable<ShortObjectMap.PrimitiveEntry<V>> entries()
    {
      if (entries == null) {
        entries = new Iterable()
        {
          public Iterator<ShortObjectMap.PrimitiveEntry<V>> iterator() {
            return new ShortCollections.UnmodifiableMap.IteratorImpl(ShortCollections.UnmodifiableMap.this, map.entries().iterator());
          }
        };
      }
      
      return entries;
    }
    
    public Set<Short> keySet()
    {
      if (keySet == null) {
        keySet = Collections.unmodifiableSet(map.keySet());
      }
      return keySet;
    }
    
    public Set<Map.Entry<Short, V>> entrySet()
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
      implements Iterator<ShortObjectMap.PrimitiveEntry<V>>
    {
      final Iterator<ShortObjectMap.PrimitiveEntry<V>> iter;
      
      IteratorImpl()
      {
        this.iter = iter;
      }
      
      public boolean hasNext()
      {
        return iter.hasNext();
      }
      
      public ShortObjectMap.PrimitiveEntry<V> next()
      {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return new ShortCollections.UnmodifiableMap.EntryImpl(ShortCollections.UnmodifiableMap.this, (ShortObjectMap.PrimitiveEntry)iter.next());
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException("remove");
      }
    }
    
    private class EntryImpl
      implements ShortObjectMap.PrimitiveEntry<V>
    {
      private final ShortObjectMap.PrimitiveEntry<V> entry;
      
      EntryImpl()
      {
        this.entry = entry;
      }
      
      public short key()
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
