package io.netty.util.collection;

import io.netty.util.internal.MathUtil;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;































public class LongObjectHashMap<V>
  implements LongObjectMap<V>
{
  public static final int DEFAULT_CAPACITY = 8;
  public static final float DEFAULT_LOAD_FACTOR = 0.5F;
  private static final Object NULL_VALUE = new Object();
  
  private int maxSize;
  
  private final float loadFactor;
  
  private long[] keys;
  
  private V[] values;
  
  private int size;
  
  private int mask;
  private final Set<Long> keySet = new KeySet(null);
  private final Set<Map.Entry<Long, V>> entrySet = new EntrySet(null);
  private final Iterable<LongObjectMap.PrimitiveEntry<V>> entries = new Iterable()
  {
    public Iterator<LongObjectMap.PrimitiveEntry<V>> iterator() {
      return new LongObjectHashMap.PrimitiveIterator(LongObjectHashMap.this, null);
    }
  };
  
  public LongObjectHashMap() {
    this(8, 0.5F);
  }
  
  public LongObjectHashMap(int initialCapacity) {
    this(initialCapacity, 0.5F);
  }
  
  public LongObjectHashMap(int initialCapacity, float loadFactor) {
    if ((loadFactor <= 0.0F) || (loadFactor > 1.0F))
    {

      throw new IllegalArgumentException("loadFactor must be > 0 and <= 1");
    }
    
    this.loadFactor = loadFactor;
    

    int capacity = MathUtil.safeFindNextPositivePowerOfTwo(initialCapacity);
    mask = (capacity - 1);
    

    keys = new long[capacity];
    
    V[] temp = (Object[])new Object[capacity];
    values = temp;
    

    maxSize = calcMaxSize(capacity);
  }
  
  private static <T> T toExternal(T value) {
    assert (value != null) : "null is not a legitimate internal value. Concurrent Modification?";
    return value == NULL_VALUE ? null : value;
  }
  
  private static <T> T toInternal(T value)
  {
    return value == null ? NULL_VALUE : value;
  }
  
  public V get(long key)
  {
    int index = indexOf(key);
    return index == -1 ? null : toExternal(values[index]);
  }
  
  public V put(long key, V value)
  {
    int startIndex = hashIndex(key);
    int index = startIndex;
    do
    {
      if (values[index] == null)
      {
        keys[index] = key;
        values[index] = toInternal(value);
        growSize();
        return null;
      }
      if (keys[index] == key)
      {
        V previousValue = values[index];
        values[index] = toInternal(value);
        return toExternal(previousValue);
      }
      
    }
    while ((index = probeNext(index)) != startIndex);
    
    throw new IllegalStateException("Unable to insert");
  }
  

  public void putAll(Map<? extends Long, ? extends V> sourceMap)
  {
    LongObjectHashMap<V> source;
    if ((sourceMap instanceof LongObjectHashMap))
    {

      source = (LongObjectHashMap)sourceMap;
      for (int i = 0; i < values.length; i++) {
        V sourceValue = values[i];
        if (sourceValue != null) {
          put(keys[i], sourceValue);
        }
      }
      return;
    }
    

    for (Map.Entry<? extends Long, ? extends V> entry : sourceMap.entrySet()) {
      put((Long)entry.getKey(), entry.getValue());
    }
  }
  
  public V remove(long key)
  {
    int index = indexOf(key);
    if (index == -1) {
      return null;
    }
    
    V prev = values[index];
    removeAt(index);
    return toExternal(prev);
  }
  
  public int size()
  {
    return size;
  }
  
  public boolean isEmpty()
  {
    return size == 0;
  }
  
  public void clear()
  {
    Arrays.fill(keys, 0L);
    Arrays.fill(values, null);
    size = 0;
  }
  
  public boolean containsKey(long key)
  {
    return indexOf(key) >= 0;
  }
  

  public boolean containsValue(Object value)
  {
    V v1 = toInternal(value);
    for (V v2 : values)
    {
      if ((v2 != null) && (v2.equals(v1))) {
        return true;
      }
    }
    return false;
  }
  
  public Iterable<LongObjectMap.PrimitiveEntry<V>> entries()
  {
    return entries;
  }
  
  public Collection<V> values()
  {
    new AbstractCollection()
    {
      public Iterator<V> iterator() {
        new Iterator() {
          final LongObjectHashMap<V>.PrimitiveIterator iter = new LongObjectHashMap.PrimitiveIterator(LongObjectHashMap.this, null);
          
          public boolean hasNext()
          {
            return iter.hasNext();
          }
          
          public V next()
          {
            return iter.next().value();
          }
          
          public void remove()
          {
            iter.remove();
          }
        };
      }
      
      public int size()
      {
        return size;
      }
    };
  }
  



  public int hashCode()
  {
    int hash = size;
    for (long key : keys)
    {






      hash ^= hashCode(key);
    }
    return hash;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LongObjectMap)) {
      return false;
    }
    
    LongObjectMap other = (LongObjectMap)obj;
    if (size != other.size()) {
      return false;
    }
    for (int i = 0; i < values.length; i++) {
      V value = values[i];
      if (value != null) {
        long key = keys[i];
        Object otherValue = other.get(key);
        if (value == NULL_VALUE) {
          if (otherValue != null) {
            return false;
          }
        } else if (!value.equals(otherValue)) {
          return false;
        }
      }
    }
    return true;
  }
  
  public boolean containsKey(Object key)
  {
    return containsKey(objectToKey(key));
  }
  
  public V get(Object key)
  {
    return get(objectToKey(key));
  }
  
  public V put(Long key, V value)
  {
    return put(objectToKey(key), value);
  }
  
  public V remove(Object key)
  {
    return remove(objectToKey(key));
  }
  
  public Set<Long> keySet()
  {
    return keySet;
  }
  
  public Set<Map.Entry<Long, V>> entrySet()
  {
    return entrySet;
  }
  
  private long objectToKey(Object key) {
    return ((Long)key).longValue();
  }
  





  private int indexOf(long key)
  {
    int startIndex = hashIndex(key);
    int index = startIndex;
    do
    {
      if (values[index] == null)
      {
        return -1;
      }
      if (key == keys[index]) {
        return index;
      }
      
    }
    while ((index = probeNext(index)) != startIndex);
    return -1;
  }
  





  private int hashIndex(long key)
  {
    return hashCode(key) & mask;
  }
  


  private static int hashCode(long key)
  {
    return (int)(key ^ key >>> 32);
  }
  



  private int probeNext(int index)
  {
    return index + 1 & mask;
  }
  


  private void growSize()
  {
    size += 1;
    
    if (size > maxSize) {
      if (keys.length == Integer.MAX_VALUE) {
        throw new IllegalStateException("Max capacity reached at size=" + size);
      }
      

      rehash(keys.length << 1);
    }
  }
  






  private boolean removeAt(int index)
  {
    size -= 1;
    

    keys[index] = 0L;
    values[index] = null;
    





    int nextFree = index;
    int i = probeNext(index);
    for (V value = values[i]; value != null; value = values[(i = probeNext(i))]) {
      long key = keys[i];
      int bucket = hashIndex(key);
      if (((i < bucket) && ((bucket <= nextFree) || (nextFree <= i))) || ((bucket <= nextFree) && (nextFree <= i)))
      {

        keys[nextFree] = key;
        values[nextFree] = value;
        
        keys[i] = 0L;
        values[i] = null;
        nextFree = i;
      }
    }
    return nextFree != index;
  }
  



  private int calcMaxSize(int capacity)
  {
    int upperBound = capacity - 1;
    return Math.min(upperBound, (int)(capacity * loadFactor));
  }
  




  private void rehash(int newCapacity)
  {
    long[] oldKeys = keys;
    V[] oldVals = values;
    
    keys = new long[newCapacity];
    
    V[] temp = (Object[])new Object[newCapacity];
    values = temp;
    
    maxSize = calcMaxSize(newCapacity);
    mask = (newCapacity - 1);
    

    for (int i = 0; i < oldVals.length; i++) {
      V oldVal = oldVals[i];
      if (oldVal != null)
      {

        long oldKey = oldKeys[i];
        int index = hashIndex(oldKey);
        for (;;)
        {
          if (values[index] == null) {
            keys[index] = oldKey;
            values[index] = oldVal;
            break;
          }
          

          index = probeNext(index);
        }
      }
    }
  }
  
  public String toString()
  {
    if (isEmpty()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder(4 * size);
    sb.append('{');
    boolean first = true;
    for (int i = 0; i < values.length; i++) {
      V value = values[i];
      if (value != null) {
        if (!first) {
          sb.append(", ");
        }
        sb.append(keyToString(keys[i])).append('=').append(value == this ? "(this Map)" : 
          toExternal(value));
        first = false;
      }
    }
    return '}';
  }
  



  protected String keyToString(long key)
  {
    return Long.toString(key);
  }
  
  private final class EntrySet extends AbstractSet<Map.Entry<Long, V>>
  {
    private EntrySet() {}
    
    public Iterator<Map.Entry<Long, V>> iterator()
    {
      return new LongObjectHashMap.MapIterator(LongObjectHashMap.this, null);
    }
    
    public int size()
    {
      return LongObjectHashMap.this.size();
    }
  }
  
  private final class KeySet extends AbstractSet<Long>
  {
    private KeySet() {}
    
    public int size()
    {
      return LongObjectHashMap.this.size();
    }
    
    public boolean contains(Object o)
    {
      return containsKey(o);
    }
    
    public boolean remove(Object o)
    {
      return remove(o) != null;
    }
    
    public boolean retainAll(Collection<?> retainedKeys)
    {
      boolean changed = false;
      for (Iterator<LongObjectMap.PrimitiveEntry<V>> iter = entries().iterator(); iter.hasNext();) {
        LongObjectMap.PrimitiveEntry<V> entry = (LongObjectMap.PrimitiveEntry)iter.next();
        if (!retainedKeys.contains(Long.valueOf(entry.key()))) {
          changed = true;
          iter.remove();
        }
      }
      return changed;
    }
    
    public void clear()
    {
      LongObjectHashMap.this.clear();
    }
    
    public Iterator<Long> iterator()
    {
      new Iterator() {
        private final Iterator<Map.Entry<Long, V>> iter = entrySet.iterator();
        
        public boolean hasNext()
        {
          return iter.hasNext();
        }
        
        public Long next()
        {
          return (Long)((Map.Entry)iter.next()).getKey();
        }
        
        public void remove()
        {
          iter.remove();
        }
      };
    }
  }
  
  private final class PrimitiveIterator implements Iterator<LongObjectMap.PrimitiveEntry<V>>, LongObjectMap.PrimitiveEntry<V>
  {
    private PrimitiveIterator() {}
    
    private int prevIndex = -1;
    private int nextIndex = -1;
    private int entryIndex = -1;
    
    private void scanNext() {
      while ((++nextIndex != values.length) && (values[nextIndex] == null)) {}
    }
    

    public boolean hasNext()
    {
      if (nextIndex == -1) {
        scanNext();
      }
      return nextIndex != values.length;
    }
    
    public LongObjectMap.PrimitiveEntry<V> next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      
      prevIndex = nextIndex;
      scanNext();
      

      entryIndex = prevIndex;
      return this;
    }
    
    public void remove()
    {
      if (prevIndex == -1) {
        throw new IllegalStateException("next must be called before each remove.");
      }
      if (LongObjectHashMap.this.removeAt(prevIndex))
      {


        nextIndex = prevIndex;
      }
      prevIndex = -1;
    }
    



    public long key()
    {
      return keys[entryIndex];
    }
    
    public V value()
    {
      return LongObjectHashMap.toExternal(values[entryIndex]);
    }
    
    public void setValue(V value)
    {
      values[entryIndex] = LongObjectHashMap.toInternal(value);
    }
  }
  

  private final class MapIterator
    implements Iterator<Map.Entry<Long, V>>
  {
    private final LongObjectHashMap<V>.PrimitiveIterator iter = new LongObjectHashMap.PrimitiveIterator(LongObjectHashMap.this, null);
    
    private MapIterator() {}
    
    public boolean hasNext() { return iter.hasNext(); }
    

    public Map.Entry<Long, V> next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      
      iter.next();
      
      return new LongObjectHashMap.MapEntry(LongObjectHashMap.this, LongObjectHashMap.PrimitiveIterator.access$1100(iter));
    }
    
    public void remove()
    {
      iter.remove();
    }
  }
  
  final class MapEntry
    implements Map.Entry<Long, V>
  {
    private final int entryIndex;
    
    MapEntry(int entryIndex)
    {
      this.entryIndex = entryIndex;
    }
    
    public Long getKey()
    {
      verifyExists();
      return Long.valueOf(keys[entryIndex]);
    }
    
    public V getValue()
    {
      verifyExists();
      return LongObjectHashMap.toExternal(values[entryIndex]);
    }
    
    public V setValue(V value)
    {
      verifyExists();
      V prevValue = LongObjectHashMap.toExternal(values[entryIndex]);
      values[entryIndex] = LongObjectHashMap.toInternal(value);
      return prevValue;
    }
    
    private void verifyExists() {
      if (values[entryIndex] == null) {
        throw new IllegalStateException("The map entry has been removed");
      }
    }
  }
}
