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































public class CharObjectHashMap<V>
  implements CharObjectMap<V>
{
  public static final int DEFAULT_CAPACITY = 8;
  public static final float DEFAULT_LOAD_FACTOR = 0.5F;
  private static final Object NULL_VALUE = new Object();
  
  private int maxSize;
  
  private final float loadFactor;
  
  private char[] keys;
  
  private V[] values;
  
  private int size;
  
  private int mask;
  private final Set<Character> keySet = new KeySet(null);
  private final Set<Map.Entry<Character, V>> entrySet = new EntrySet(null);
  private final Iterable<CharObjectMap.PrimitiveEntry<V>> entries = new Iterable()
  {
    public Iterator<CharObjectMap.PrimitiveEntry<V>> iterator() {
      return new CharObjectHashMap.PrimitiveIterator(CharObjectHashMap.this, null);
    }
  };
  
  public CharObjectHashMap() {
    this(8, 0.5F);
  }
  
  public CharObjectHashMap(int initialCapacity) {
    this(initialCapacity, 0.5F);
  }
  
  public CharObjectHashMap(int initialCapacity, float loadFactor) {
    if ((loadFactor <= 0.0F) || (loadFactor > 1.0F))
    {

      throw new IllegalArgumentException("loadFactor must be > 0 and <= 1");
    }
    
    this.loadFactor = loadFactor;
    

    int capacity = MathUtil.safeFindNextPositivePowerOfTwo(initialCapacity);
    mask = (capacity - 1);
    

    keys = new char[capacity];
    
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
  
  public V get(char key)
  {
    int index = indexOf(key);
    return index == -1 ? null : toExternal(values[index]);
  }
  
  public V put(char key, V value)
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
  

  public void putAll(Map<? extends Character, ? extends V> sourceMap)
  {
    CharObjectHashMap<V> source;
    if ((sourceMap instanceof CharObjectHashMap))
    {

      source = (CharObjectHashMap)sourceMap;
      for (int i = 0; i < values.length; i++) {
        V sourceValue = values[i];
        if (sourceValue != null) {
          put(keys[i], sourceValue);
        }
      }
      return;
    }
    

    for (Map.Entry<? extends Character, ? extends V> entry : sourceMap.entrySet()) {
      put((Character)entry.getKey(), entry.getValue());
    }
  }
  
  public V remove(char key)
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
    Arrays.fill(keys, '\000');
    Arrays.fill(values, null);
    size = 0;
  }
  
  public boolean containsKey(char key)
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
  
  public Iterable<CharObjectMap.PrimitiveEntry<V>> entries()
  {
    return entries;
  }
  
  public Collection<V> values()
  {
    new AbstractCollection()
    {
      public Iterator<V> iterator() {
        new Iterator() {
          final CharObjectHashMap<V>.PrimitiveIterator iter = new CharObjectHashMap.PrimitiveIterator(CharObjectHashMap.this, null);
          
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
    for (char key : keys)
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
    if (!(obj instanceof CharObjectMap)) {
      return false;
    }
    
    CharObjectMap other = (CharObjectMap)obj;
    if (size != other.size()) {
      return false;
    }
    for (int i = 0; i < values.length; i++) {
      V value = values[i];
      if (value != null) {
        char key = keys[i];
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
  
  public V put(Character key, V value)
  {
    return put(objectToKey(key), value);
  }
  
  public V remove(Object key)
  {
    return remove(objectToKey(key));
  }
  
  public Set<Character> keySet()
  {
    return keySet;
  }
  
  public Set<Map.Entry<Character, V>> entrySet()
  {
    return entrySet;
  }
  
  private char objectToKey(Object key) {
    return ((Character)key).charValue();
  }
  





  private int indexOf(char key)
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
  





  private int hashIndex(char key)
  {
    return hashCode(key) & mask;
  }
  


  private static int hashCode(char key)
  {
    return key;
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
    

    keys[index] = '\000';
    values[index] = null;
    





    int nextFree = index;
    int i = probeNext(index);
    for (V value = values[i]; value != null; value = values[(i = probeNext(i))]) {
      char key = keys[i];
      int bucket = hashIndex(key);
      if (((i < bucket) && ((bucket <= nextFree) || (nextFree <= i))) || ((bucket <= nextFree) && (nextFree <= i)))
      {

        keys[nextFree] = key;
        values[nextFree] = value;
        
        keys[i] = '\000';
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
    char[] oldKeys = keys;
    V[] oldVals = values;
    
    keys = new char[newCapacity];
    
    V[] temp = (Object[])new Object[newCapacity];
    values = temp;
    
    maxSize = calcMaxSize(newCapacity);
    mask = (newCapacity - 1);
    

    for (int i = 0; i < oldVals.length; i++) {
      V oldVal = oldVals[i];
      if (oldVal != null)
      {

        char oldKey = oldKeys[i];
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
  



  protected String keyToString(char key)
  {
    return Character.toString(key);
  }
  
  private final class EntrySet extends AbstractSet<Map.Entry<Character, V>>
  {
    private EntrySet() {}
    
    public Iterator<Map.Entry<Character, V>> iterator()
    {
      return new CharObjectHashMap.MapIterator(CharObjectHashMap.this, null);
    }
    
    public int size()
    {
      return CharObjectHashMap.this.size();
    }
  }
  
  private final class KeySet extends AbstractSet<Character>
  {
    private KeySet() {}
    
    public int size()
    {
      return CharObjectHashMap.this.size();
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
      for (Iterator<CharObjectMap.PrimitiveEntry<V>> iter = entries().iterator(); iter.hasNext();) {
        CharObjectMap.PrimitiveEntry<V> entry = (CharObjectMap.PrimitiveEntry)iter.next();
        if (!retainedKeys.contains(Character.valueOf(entry.key()))) {
          changed = true;
          iter.remove();
        }
      }
      return changed;
    }
    
    public void clear()
    {
      CharObjectHashMap.this.clear();
    }
    
    public Iterator<Character> iterator()
    {
      new Iterator() {
        private final Iterator<Map.Entry<Character, V>> iter = entrySet.iterator();
        
        public boolean hasNext()
        {
          return iter.hasNext();
        }
        
        public Character next()
        {
          return (Character)((Map.Entry)iter.next()).getKey();
        }
        
        public void remove()
        {
          iter.remove();
        }
      };
    }
  }
  
  private final class PrimitiveIterator implements Iterator<CharObjectMap.PrimitiveEntry<V>>, CharObjectMap.PrimitiveEntry<V>
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
    
    public CharObjectMap.PrimitiveEntry<V> next()
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
      if (CharObjectHashMap.this.removeAt(prevIndex))
      {


        nextIndex = prevIndex;
      }
      prevIndex = -1;
    }
    



    public char key()
    {
      return keys[entryIndex];
    }
    
    public V value()
    {
      return CharObjectHashMap.toExternal(values[entryIndex]);
    }
    
    public void setValue(V value)
    {
      values[entryIndex] = CharObjectHashMap.toInternal(value);
    }
  }
  

  private final class MapIterator
    implements Iterator<Map.Entry<Character, V>>
  {
    private final CharObjectHashMap<V>.PrimitiveIterator iter = new CharObjectHashMap.PrimitiveIterator(CharObjectHashMap.this, null);
    
    private MapIterator() {}
    
    public boolean hasNext() { return iter.hasNext(); }
    

    public Map.Entry<Character, V> next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      
      iter.next();
      
      return new CharObjectHashMap.MapEntry(CharObjectHashMap.this, CharObjectHashMap.PrimitiveIterator.access$1100(iter));
    }
    
    public void remove()
    {
      iter.remove();
    }
  }
  
  final class MapEntry
    implements Map.Entry<Character, V>
  {
    private final int entryIndex;
    
    MapEntry(int entryIndex)
    {
      this.entryIndex = entryIndex;
    }
    
    public Character getKey()
    {
      verifyExists();
      return Character.valueOf(keys[entryIndex]);
    }
    
    public V getValue()
    {
      verifyExists();
      return CharObjectHashMap.toExternal(values[entryIndex]);
    }
    
    public V setValue(V value)
    {
      verifyExists();
      V prevValue = CharObjectHashMap.toExternal(values[entryIndex]);
      values[entryIndex] = CharObjectHashMap.toInternal(value);
      return prevValue;
    }
    
    private void verifyExists() {
      if (values[entryIndex] == null) {
        throw new IllegalStateException("The map entry has been removed");
      }
    }
  }
}
