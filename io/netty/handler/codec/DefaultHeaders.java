package io.netty.handler.codec;

import io.netty.util.HashingStrategy;
import io.netty.util.internal.MathUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;






































public class DefaultHeaders<K, V, T extends Headers<K, V, T>>
  implements Headers<K, V, T>
{
  static final int HASH_CODE_SEED = -1028477387;
  private final HeaderEntry<K, V>[] entries;
  protected final HeaderEntry<K, V> head;
  private final byte hashMask;
  private final ValueConverter<V> valueConverter;
  private final NameValidator<K> nameValidator;
  private final HashingStrategy<K> hashingStrategy;
  int size;
  
  public static abstract interface NameValidator<K>
  {
    public static final NameValidator NOT_NULL = new NameValidator()
    {
      public void validateName(Object name) {
        ObjectUtil.checkNotNull(name, "name");
      }
    };
    
    public abstract void validateName(K paramK);
  }
  
  public DefaultHeaders(ValueConverter<V> valueConverter) { this(HashingStrategy.JAVA_HASHER, valueConverter); }
  

  public DefaultHeaders(ValueConverter<V> valueConverter, NameValidator<K> nameValidator)
  {
    this(HashingStrategy.JAVA_HASHER, valueConverter, nameValidator);
  }
  
  public DefaultHeaders(HashingStrategy<K> nameHashingStrategy, ValueConverter<V> valueConverter)
  {
    this(nameHashingStrategy, valueConverter, NameValidator.NOT_NULL);
  }
  
  public DefaultHeaders(HashingStrategy<K> nameHashingStrategy, ValueConverter<V> valueConverter, NameValidator<K> nameValidator)
  {
    this(nameHashingStrategy, valueConverter, nameValidator, 16);
  }
  









  public DefaultHeaders(HashingStrategy<K> nameHashingStrategy, ValueConverter<V> valueConverter, NameValidator<K> nameValidator, int arraySizeHint)
  {
    this.valueConverter = ((ValueConverter)ObjectUtil.checkNotNull(valueConverter, "valueConverter"));
    this.nameValidator = ((NameValidator)ObjectUtil.checkNotNull(nameValidator, "nameValidator"));
    hashingStrategy = ((HashingStrategy)ObjectUtil.checkNotNull(nameHashingStrategy, "nameHashingStrategy"));
    

    entries = new HeaderEntry[MathUtil.findNextPositivePowerOfTwo(Math.max(2, Math.min(arraySizeHint, 128)))];
    hashMask = ((byte)(entries.length - 1));
    head = new HeaderEntry();
  }
  
  public V get(K name)
  {
    ObjectUtil.checkNotNull(name, "name");
    
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    HeaderEntry<K, V> e = entries[i];
    V value = null;
    
    while (e != null) {
      if ((hash == h) && (hashingStrategy.equals(name, key))) {
        value = value;
      }
      
      e = next;
    }
    return value;
  }
  
  public V get(K name, V defaultValue)
  {
    V value = get(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
  
  public V getAndRemove(K name)
  {
    int h = hashingStrategy.hashCode(name);
    return remove0(h, index(h), ObjectUtil.checkNotNull(name, "name"));
  }
  
  public V getAndRemove(K name, V defaultValue)
  {
    V value = getAndRemove(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
  
  public List<V> getAll(K name)
  {
    ObjectUtil.checkNotNull(name, "name");
    
    LinkedList<V> values = new LinkedList();
    
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    HeaderEntry<K, V> e = entries[i];
    while (e != null) {
      if ((hash == h) && (hashingStrategy.equals(name, key))) {
        values.addFirst(e.getValue());
      }
      e = next;
    }
    return values;
  }
  




  public Iterator<V> valueIterator(K name)
  {
    return new ValueIterator(name);
  }
  
  public List<V> getAllAndRemove(K name)
  {
    List<V> all = getAll(name);
    remove(name);
    return all;
  }
  
  public boolean contains(K name)
  {
    return get(name) != null;
  }
  
  public boolean containsObject(K name, Object value)
  {
    return contains(name, valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
  }
  
  public boolean containsBoolean(K name, boolean value)
  {
    return contains(name, valueConverter.convertBoolean(value));
  }
  
  public boolean containsByte(K name, byte value)
  {
    return contains(name, valueConverter.convertByte(value));
  }
  
  public boolean containsChar(K name, char value)
  {
    return contains(name, valueConverter.convertChar(value));
  }
  
  public boolean containsShort(K name, short value)
  {
    return contains(name, valueConverter.convertShort(value));
  }
  
  public boolean containsInt(K name, int value)
  {
    return contains(name, valueConverter.convertInt(value));
  }
  
  public boolean containsLong(K name, long value)
  {
    return contains(name, valueConverter.convertLong(value));
  }
  
  public boolean containsFloat(K name, float value)
  {
    return contains(name, valueConverter.convertFloat(value));
  }
  
  public boolean containsDouble(K name, double value)
  {
    return contains(name, valueConverter.convertDouble(value));
  }
  
  public boolean containsTimeMillis(K name, long value)
  {
    return contains(name, valueConverter.convertTimeMillis(value));
  }
  

  public boolean contains(K name, V value)
  {
    return contains(name, value, HashingStrategy.JAVA_HASHER);
  }
  
  public final boolean contains(K name, V value, HashingStrategy<? super V> valueHashingStrategy) {
    ObjectUtil.checkNotNull(name, "name");
    
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    HeaderEntry<K, V> e = entries[i];
    while (e != null) {
      if ((hash == h) && (hashingStrategy.equals(name, key)) && (valueHashingStrategy.equals(value, value))) {
        return true;
      }
      e = next;
    }
    return false;
  }
  
  public int size()
  {
    return size;
  }
  
  public boolean isEmpty()
  {
    return head == head.after;
  }
  
  public Set<K> names()
  {
    if (isEmpty()) {
      return Collections.emptySet();
    }
    Set<K> names = new LinkedHashSet(size());
    HeaderEntry<K, V> e = head.after;
    while (e != head) {
      names.add(e.getKey());
      e = after;
    }
    return names;
  }
  
  public T add(K name, V value)
  {
    nameValidator.validateName(name);
    ObjectUtil.checkNotNull(value, "value");
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    add0(h, i, name, value);
    return thisT();
  }
  
  public T add(K name, Iterable<? extends V> values)
  {
    nameValidator.validateName(name);
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    for (V v : values) {
      add0(h, i, name, v);
    }
    return thisT();
  }
  
  public T add(K name, V... values)
  {
    nameValidator.validateName(name);
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    for (V v : values) {
      add0(h, i, name, v);
    }
    return thisT();
  }
  
  public T addObject(K name, Object value)
  {
    return add(name, valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
  }
  
  public T addObject(K name, Iterable<?> values)
  {
    for (Object value : values) {
      addObject(name, value);
    }
    return thisT();
  }
  
  public T addObject(K name, Object... values)
  {
    for (Object value : values) {
      addObject(name, value);
    }
    return thisT();
  }
  
  public T addInt(K name, int value)
  {
    return add(name, valueConverter.convertInt(value));
  }
  
  public T addLong(K name, long value)
  {
    return add(name, valueConverter.convertLong(value));
  }
  
  public T addDouble(K name, double value)
  {
    return add(name, valueConverter.convertDouble(value));
  }
  
  public T addTimeMillis(K name, long value)
  {
    return add(name, valueConverter.convertTimeMillis(value));
  }
  
  public T addChar(K name, char value)
  {
    return add(name, valueConverter.convertChar(value));
  }
  
  public T addBoolean(K name, boolean value)
  {
    return add(name, valueConverter.convertBoolean(value));
  }
  
  public T addFloat(K name, float value)
  {
    return add(name, valueConverter.convertFloat(value));
  }
  
  public T addByte(K name, byte value)
  {
    return add(name, valueConverter.convertByte(value));
  }
  
  public T addShort(K name, short value)
  {
    return add(name, valueConverter.convertShort(value));
  }
  
  public T add(Headers<? extends K, ? extends V, ?> headers)
  {
    if (headers == this) {
      throw new IllegalArgumentException("can't add to itself.");
    }
    addImpl(headers);
    return thisT();
  }
  
  protected void addImpl(Headers<? extends K, ? extends V, ?> headers) { DefaultHeaders<? extends K, ? extends V, T> defaultHeaders;
    if ((headers instanceof DefaultHeaders))
    {
      defaultHeaders = (DefaultHeaders)headers;
      
      HeaderEntry<? extends K, ? extends V> e = head.after;
      if ((hashingStrategy == hashingStrategy) && (nameValidator == nameValidator)) {}
      

      while (e != head) {
        add0(hash, index(hash), key, value);
        e = after; continue;
        


        while (e != head) {
          add(key, value);
          e = after;
        }
      }
    }
    else {
      for (Map.Entry<? extends K, ? extends V> header : headers) {
        add(header.getKey(), header.getValue());
      }
    }
  }
  
  public T set(K name, V value)
  {
    nameValidator.validateName(name);
    ObjectUtil.checkNotNull(value, "value");
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    remove0(h, i, name);
    add0(h, i, name, value);
    return thisT();
  }
  
  public T set(K name, Iterable<? extends V> values)
  {
    nameValidator.validateName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    
    remove0(h, i, name);
    for (V v : values) {
      if (v == null) {
        break;
      }
      add0(h, i, name, v);
    }
    
    return thisT();
  }
  
  public T set(K name, V... values)
  {
    nameValidator.validateName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    
    remove0(h, i, name);
    for (V v : values) {
      if (v == null) {
        break;
      }
      add0(h, i, name, v);
    }
    
    return thisT();
  }
  
  public T setObject(K name, Object value)
  {
    ObjectUtil.checkNotNull(value, "value");
    V convertedValue = ObjectUtil.checkNotNull(valueConverter.convertObject(value), "convertedValue");
    return set(name, convertedValue);
  }
  
  public T setObject(K name, Iterable<?> values)
  {
    nameValidator.validateName(name);
    
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    
    remove0(h, i, name);
    for (Object v : values) {
      if (v == null) {
        break;
      }
      add0(h, i, name, valueConverter.convertObject(v));
    }
    
    return thisT();
  }
  
  public T setObject(K name, Object... values)
  {
    nameValidator.validateName(name);
    
    int h = hashingStrategy.hashCode(name);
    int i = index(h);
    
    remove0(h, i, name);
    for (Object v : values) {
      if (v == null) {
        break;
      }
      add0(h, i, name, valueConverter.convertObject(v));
    }
    
    return thisT();
  }
  
  public T setInt(K name, int value)
  {
    return set(name, valueConverter.convertInt(value));
  }
  
  public T setLong(K name, long value)
  {
    return set(name, valueConverter.convertLong(value));
  }
  
  public T setDouble(K name, double value)
  {
    return set(name, valueConverter.convertDouble(value));
  }
  
  public T setTimeMillis(K name, long value)
  {
    return set(name, valueConverter.convertTimeMillis(value));
  }
  
  public T setFloat(K name, float value)
  {
    return set(name, valueConverter.convertFloat(value));
  }
  
  public T setChar(K name, char value)
  {
    return set(name, valueConverter.convertChar(value));
  }
  
  public T setBoolean(K name, boolean value)
  {
    return set(name, valueConverter.convertBoolean(value));
  }
  
  public T setByte(K name, byte value)
  {
    return set(name, valueConverter.convertByte(value));
  }
  
  public T setShort(K name, short value)
  {
    return set(name, valueConverter.convertShort(value));
  }
  
  public T set(Headers<? extends K, ? extends V, ?> headers)
  {
    if (headers != this) {
      clear();
      addImpl(headers);
    }
    return thisT();
  }
  
  public T setAll(Headers<? extends K, ? extends V, ?> headers)
  {
    if (headers != this) {
      for (K key : headers.names()) {
        remove(key);
      }
      addImpl(headers);
    }
    return thisT();
  }
  
  public boolean remove(K name)
  {
    return getAndRemove(name) != null;
  }
  
  public T clear()
  {
    Arrays.fill(entries, null);
    head.before = (head.after = head);
    size = 0;
    return thisT();
  }
  
  public Iterator<Map.Entry<K, V>> iterator()
  {
    return new HeaderIterator(null);
  }
  
  public Boolean getBoolean(K name)
  {
    V v = get(name);
    try {
      return v != null ? Boolean.valueOf(valueConverter.convertToBoolean(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public boolean getBoolean(K name, boolean defaultValue)
  {
    Boolean v = getBoolean(name);
    return v != null ? v.booleanValue() : defaultValue;
  }
  
  public Byte getByte(K name)
  {
    V v = get(name);
    try {
      return v != null ? Byte.valueOf(valueConverter.convertToByte(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public byte getByte(K name, byte defaultValue)
  {
    Byte v = getByte(name);
    return v != null ? v.byteValue() : defaultValue;
  }
  
  public Character getChar(K name)
  {
    V v = get(name);
    try {
      return v != null ? Character.valueOf(valueConverter.convertToChar(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public char getChar(K name, char defaultValue)
  {
    Character v = getChar(name);
    return v != null ? v.charValue() : defaultValue;
  }
  
  public Short getShort(K name)
  {
    V v = get(name);
    try {
      return v != null ? Short.valueOf(valueConverter.convertToShort(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public short getShort(K name, short defaultValue)
  {
    Short v = getShort(name);
    return v != null ? v.shortValue() : defaultValue;
  }
  
  public Integer getInt(K name)
  {
    V v = get(name);
    try {
      return v != null ? Integer.valueOf(valueConverter.convertToInt(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public int getInt(K name, int defaultValue)
  {
    Integer v = getInt(name);
    return v != null ? v.intValue() : defaultValue;
  }
  
  public Long getLong(K name)
  {
    V v = get(name);
    try {
      return v != null ? Long.valueOf(valueConverter.convertToLong(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public long getLong(K name, long defaultValue)
  {
    Long v = getLong(name);
    return v != null ? v.longValue() : defaultValue;
  }
  
  public Float getFloat(K name)
  {
    V v = get(name);
    try {
      return v != null ? Float.valueOf(valueConverter.convertToFloat(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public float getFloat(K name, float defaultValue)
  {
    Float v = getFloat(name);
    return v != null ? v.floatValue() : defaultValue;
  }
  
  public Double getDouble(K name)
  {
    V v = get(name);
    try {
      return v != null ? Double.valueOf(valueConverter.convertToDouble(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public double getDouble(K name, double defaultValue)
  {
    Double v = getDouble(name);
    return v != null ? v.doubleValue() : defaultValue;
  }
  
  public Long getTimeMillis(K name)
  {
    V v = get(name);
    try {
      return v != null ? Long.valueOf(valueConverter.convertToTimeMillis(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public long getTimeMillis(K name, long defaultValue)
  {
    Long v = getTimeMillis(name);
    return v != null ? v.longValue() : defaultValue;
  }
  
  public Boolean getBooleanAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Boolean.valueOf(valueConverter.convertToBoolean(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public boolean getBooleanAndRemove(K name, boolean defaultValue)
  {
    Boolean v = getBooleanAndRemove(name);
    return v != null ? v.booleanValue() : defaultValue;
  }
  
  public Byte getByteAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Byte.valueOf(valueConverter.convertToByte(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public byte getByteAndRemove(K name, byte defaultValue)
  {
    Byte v = getByteAndRemove(name);
    return v != null ? v.byteValue() : defaultValue;
  }
  
  public Character getCharAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Character.valueOf(valueConverter.convertToChar(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public char getCharAndRemove(K name, char defaultValue)
  {
    Character v = getCharAndRemove(name);
    return v != null ? v.charValue() : defaultValue;
  }
  
  public Short getShortAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Short.valueOf(valueConverter.convertToShort(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public short getShortAndRemove(K name, short defaultValue)
  {
    Short v = getShortAndRemove(name);
    return v != null ? v.shortValue() : defaultValue;
  }
  
  public Integer getIntAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Integer.valueOf(valueConverter.convertToInt(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public int getIntAndRemove(K name, int defaultValue)
  {
    Integer v = getIntAndRemove(name);
    return v != null ? v.intValue() : defaultValue;
  }
  
  public Long getLongAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Long.valueOf(valueConverter.convertToLong(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public long getLongAndRemove(K name, long defaultValue)
  {
    Long v = getLongAndRemove(name);
    return v != null ? v.longValue() : defaultValue;
  }
  
  public Float getFloatAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Float.valueOf(valueConverter.convertToFloat(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public float getFloatAndRemove(K name, float defaultValue)
  {
    Float v = getFloatAndRemove(name);
    return v != null ? v.floatValue() : defaultValue;
  }
  
  public Double getDoubleAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Double.valueOf(valueConverter.convertToDouble(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public double getDoubleAndRemove(K name, double defaultValue)
  {
    Double v = getDoubleAndRemove(name);
    return v != null ? v.doubleValue() : defaultValue;
  }
  
  public Long getTimeMillisAndRemove(K name)
  {
    V v = getAndRemove(name);
    try {
      return v != null ? Long.valueOf(valueConverter.convertToTimeMillis(v)) : null;
    } catch (RuntimeException ignore) {}
    return null;
  }
  

  public long getTimeMillisAndRemove(K name, long defaultValue)
  {
    Long v = getTimeMillisAndRemove(name);
    return v != null ? v.longValue() : defaultValue;
  }
  

  public boolean equals(Object o)
  {
    if (!(o instanceof Headers)) {
      return false;
    }
    
    return equals((Headers)o, HashingStrategy.JAVA_HASHER);
  }
  

  public int hashCode()
  {
    return hashCode(HashingStrategy.JAVA_HASHER);
  }
  






  public final boolean equals(Headers<K, V, ?> h2, HashingStrategy<V> valueHashingStrategy)
  {
    if (h2.size() != size()) {
      return false;
    }
    
    if (this == h2) {
      return true;
    }
    
    for (K name : names()) {
      List<V> otherValues = h2.getAll(name);
      List<V> values = getAll(name);
      if (otherValues.size() != values.size()) {
        return false;
      }
      for (int i = 0; i < otherValues.size(); i++) {
        if (!valueHashingStrategy.equals(otherValues.get(i), values.get(i))) {
          return false;
        }
      }
    }
    return true;
  }
  




  public final int hashCode(HashingStrategy<V> valueHashingStrategy)
  {
    int result = -1028477387;
    for (K name : names()) {
      result = 31 * result + hashingStrategy.hashCode(name);
      List<V> values = getAll(name);
      for (int i = 0; i < values.size(); i++) {
        result = 31 * result + valueHashingStrategy.hashCode(values.get(i));
      }
    }
    return result;
  }
  
  public String toString()
  {
    return HeadersUtils.toString(getClass(), iterator(), size());
  }
  
  protected HeaderEntry<K, V> newHeaderEntry(int h, K name, V value, HeaderEntry<K, V> next) {
    return new HeaderEntry(h, name, value, next, head);
  }
  
  protected ValueConverter<V> valueConverter() {
    return valueConverter;
  }
  
  private int index(int hash) {
    return hash & hashMask;
  }
  
  private void add0(int h, int i, K name, V value)
  {
    entries[i] = newHeaderEntry(h, name, value, entries[i]);
    size += 1;
  }
  


  private V remove0(int h, int i, K name)
  {
    HeaderEntry<K, V> e = entries[i];
    if (e == null) {
      return null;
    }
    
    V value = null;
    HeaderEntry<K, V> next = next;
    while (next != null) {
      if ((hash == h) && (hashingStrategy.equals(name, key))) {
        value = value;
        next = next;
        next.remove();
        size -= 1;
      } else {
        e = next;
      }
      
      next = next;
    }
    
    e = entries[i];
    if ((hash == h) && (hashingStrategy.equals(name, key))) {
      if (value == null) {
        value = value;
      }
      entries[i] = next;
      e.remove();
      size -= 1;
    }
    
    return value;
  }
  
  HeaderEntry<K, V> remove0(HeaderEntry<K, V> entry, HeaderEntry<K, V> previous) {
    int i = index(hash);
    HeaderEntry<K, V> firstEntry = entries[i];
    if (firstEntry == entry) {
      entries[i] = next;
      previous = entries[i];
    } else if (previous == null)
    {
      previous = firstEntry;
      HeaderEntry<K, V> next = next;
      while ((next != null) && (next != entry)) {
        previous = next;
        next = next;
      }
      assert (next != null) : ("Entry not found in its hash bucket: " + entry);
      next = next;
    } else {
      next = next;
    }
    entry.remove();
    size -= 1;
    return previous;
  }
  
  private T thisT()
  {
    return this;
  }
  


  public DefaultHeaders<K, V, T> copy()
  {
    DefaultHeaders<K, V, T> copy = new DefaultHeaders(hashingStrategy, valueConverter, nameValidator, entries.length);
    
    copy.addImpl(this);
    return copy;
  }
  
  private final class HeaderIterator implements Iterator<Map.Entry<K, V>> {
    private DefaultHeaders.HeaderEntry<K, V> current = head;
    
    private HeaderIterator() {}
    
    public boolean hasNext() { return current.after != head; }
    

    public Map.Entry<K, V> next()
    {
      current = current.after;
      
      if (current == head) {
        throw new NoSuchElementException();
      }
      
      return current;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("read only");
    }
  }
  
  private final class ValueIterator implements Iterator<V> {
    private final K name;
    private final int hash;
    private DefaultHeaders.HeaderEntry<K, V> removalPrevious;
    private DefaultHeaders.HeaderEntry<K, V> previous;
    private DefaultHeaders.HeaderEntry<K, V> next;
    
    ValueIterator() {
      this.name = ObjectUtil.checkNotNull(name, "name");
      hash = hashingStrategy.hashCode(name);
      calculateNext(entries[DefaultHeaders.this.index(hash)]);
    }
    
    public boolean hasNext()
    {
      return next != null;
    }
    
    public V next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      if (previous != null) {
        removalPrevious = previous;
      }
      previous = next;
      calculateNext(next.next);
      return previous.value;
    }
    
    public void remove()
    {
      if (previous == null) {
        throw new IllegalStateException();
      }
      removalPrevious = remove0(previous, removalPrevious);
      previous = null;
    }
    
    private void calculateNext(DefaultHeaders.HeaderEntry<K, V> entry) {
      while (entry != null) {
        if ((hash == hash) && (hashingStrategy.equals(name, key))) {
          next = entry;
          return;
        }
        entry = next;
      }
      next = null;
    }
  }
  

  protected static class HeaderEntry<K, V>
    implements Map.Entry<K, V>
  {
    protected final int hash;
    
    protected final K key;
    protected V value;
    protected HeaderEntry<K, V> next;
    protected HeaderEntry<K, V> before;
    protected HeaderEntry<K, V> after;
    
    protected HeaderEntry(int hash, K key)
    {
      this.hash = hash;
      this.key = key;
    }
    
    HeaderEntry(int hash, K key, V value, HeaderEntry<K, V> next, HeaderEntry<K, V> head) {
      this.hash = hash;
      this.key = key;
      this.value = value;
      this.next = next;
      
      after = head;
      before = before;
      pointNeighborsToThis();
    }
    
    HeaderEntry() {
      hash = -1;
      key = null;
      after = this;before = this;
    }
    
    protected final void pointNeighborsToThis() {
      before.after = this;
      after.before = this;
    }
    
    public final HeaderEntry<K, V> before() {
      return before;
    }
    
    public final HeaderEntry<K, V> after() {
      return after;
    }
    
    protected void remove() {
      before.after = after;
      after.before = before;
    }
    
    public final K getKey()
    {
      return key;
    }
    
    public final V getValue()
    {
      return value;
    }
    
    public final V setValue(V value)
    {
      ObjectUtil.checkNotNull(value, "value");
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }
    
    public final String toString()
    {
      return key.toString() + '=' + value.toString();
    }
    
    public boolean equals(Object o)
    {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> other = (Map.Entry)o;
      return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey())) && 
        (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
    }
    
    public int hashCode()
    {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }
  }
}
