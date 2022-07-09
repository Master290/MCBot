package io.netty.handler.codec.http;

import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.DefaultHeaders.NameValidator;
import io.netty.util.AsciiString;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;



































public final class ReadOnlyHttpHeaders
  extends HttpHeaders
{
  private final CharSequence[] nameValuePairs;
  
  public ReadOnlyHttpHeaders(boolean validateHeaders, CharSequence... nameValuePairs)
  {
    if ((nameValuePairs.length & 0x1) != 0) {
      throw newInvalidArraySizeException();
    }
    if (validateHeaders) {
      validateHeaders(nameValuePairs);
    }
    this.nameValuePairs = nameValuePairs;
  }
  
  private static IllegalArgumentException newInvalidArraySizeException() {
    return new IllegalArgumentException("nameValuePairs must be arrays of [name, value] pairs");
  }
  
  private static void validateHeaders(CharSequence... keyValuePairs) {
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      DefaultHttpHeaders.HttpNameValidator.validateName(keyValuePairs[i]);
    }
  }
  
  private CharSequence get0(CharSequence name) {
    int nameHash = AsciiString.hashCode(name);
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      CharSequence roName = nameValuePairs[i];
      if ((AsciiString.hashCode(roName) == nameHash) && (AsciiString.contentEqualsIgnoreCase(roName, name)))
      {
        return nameValuePairs[(i + 1)];
      }
    }
    return null;
  }
  
  public String get(String name)
  {
    CharSequence value = get0(name);
    return value == null ? null : value.toString();
  }
  
  public Integer getInt(CharSequence name)
  {
    CharSequence value = get0(name);
    return value == null ? null : Integer.valueOf(CharSequenceValueConverter.INSTANCE.convertToInt(value));
  }
  
  public int getInt(CharSequence name, int defaultValue)
  {
    CharSequence value = get0(name);
    return value == null ? defaultValue : CharSequenceValueConverter.INSTANCE.convertToInt(value);
  }
  
  public Short getShort(CharSequence name)
  {
    CharSequence value = get0(name);
    return value == null ? null : Short.valueOf(CharSequenceValueConverter.INSTANCE.convertToShort(value));
  }
  
  public short getShort(CharSequence name, short defaultValue)
  {
    CharSequence value = get0(name);
    return value == null ? defaultValue : CharSequenceValueConverter.INSTANCE.convertToShort(value);
  }
  
  public Long getTimeMillis(CharSequence name)
  {
    CharSequence value = get0(name);
    return value == null ? null : Long.valueOf(CharSequenceValueConverter.INSTANCE.convertToTimeMillis(value));
  }
  
  public long getTimeMillis(CharSequence name, long defaultValue)
  {
    CharSequence value = get0(name);
    return value == null ? defaultValue : CharSequenceValueConverter.INSTANCE.convertToTimeMillis(value);
  }
  
  public List<String> getAll(String name)
  {
    if (isEmpty()) {
      return Collections.emptyList();
    }
    int nameHash = AsciiString.hashCode(name);
    List<String> values = new ArrayList(4);
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      CharSequence roName = nameValuePairs[i];
      if ((AsciiString.hashCode(roName) == nameHash) && (AsciiString.contentEqualsIgnoreCase(roName, name))) {
        values.add(nameValuePairs[(i + 1)].toString());
      }
    }
    return values;
  }
  
  public List<Map.Entry<String, String>> entries()
  {
    if (isEmpty()) {
      return Collections.emptyList();
    }
    List<Map.Entry<String, String>> entries = new ArrayList(size());
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      entries.add(new AbstractMap.SimpleImmutableEntry(nameValuePairs[i].toString(), nameValuePairs[(i + 1)]
        .toString()));
    }
    return entries;
  }
  
  public boolean contains(String name)
  {
    return get0(name) != null;
  }
  
  public boolean contains(String name, String value, boolean ignoreCase)
  {
    return containsValue(name, value, ignoreCase);
  }
  
  public boolean containsValue(CharSequence name, CharSequence value, boolean ignoreCase)
  {
    if (ignoreCase) {
      for (int i = 0; i < nameValuePairs.length; i += 2) {
        if ((AsciiString.contentEqualsIgnoreCase(nameValuePairs[i], name)) && 
          (AsciiString.contentEqualsIgnoreCase(nameValuePairs[(i + 1)], value))) {
          return true;
        }
      }
    } else {
      for (int i = 0; i < nameValuePairs.length; i += 2) {
        if ((AsciiString.contentEqualsIgnoreCase(nameValuePairs[i], name)) && 
          (AsciiString.contentEquals(nameValuePairs[(i + 1)], value))) {
          return true;
        }
      }
    }
    return false;
  }
  
  public Iterator<String> valueStringIterator(CharSequence name)
  {
    return new ReadOnlyStringValueIterator(name);
  }
  
  public Iterator<CharSequence> valueCharSequenceIterator(CharSequence name)
  {
    return new ReadOnlyValueIterator(name);
  }
  
  public Iterator<Map.Entry<String, String>> iterator()
  {
    return new ReadOnlyStringIterator(null);
  }
  
  public Iterator<Map.Entry<CharSequence, CharSequence>> iteratorCharSequence()
  {
    return new ReadOnlyIterator(null);
  }
  
  public boolean isEmpty()
  {
    return nameValuePairs.length == 0;
  }
  
  public int size()
  {
    return nameValuePairs.length >>> 1;
  }
  
  public Set<String> names()
  {
    if (isEmpty()) {
      return Collections.emptySet();
    }
    Set<String> names = new LinkedHashSet(size());
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      names.add(nameValuePairs[i].toString());
    }
    return names;
  }
  
  public HttpHeaders add(String name, Object value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders add(String name, Iterable<?> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders addInt(CharSequence name, int value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders addShort(CharSequence name, short value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders set(String name, Object value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders set(String name, Iterable<?> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders setInt(CharSequence name, int value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders setShort(CharSequence name, short value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders remove(String name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public HttpHeaders clear()
  {
    throw new UnsupportedOperationException("read only");
  }
  
  private final class ReadOnlyIterator implements Map.Entry<CharSequence, CharSequence>, Iterator<Map.Entry<CharSequence, CharSequence>> {
    private CharSequence key;
    private CharSequence value;
    private int nextNameIndex;
    
    private ReadOnlyIterator() {}
    
    public boolean hasNext() {
      return nextNameIndex != nameValuePairs.length;
    }
    
    public Map.Entry<CharSequence, CharSequence> next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      key = nameValuePairs[nextNameIndex];
      value = nameValuePairs[(nextNameIndex + 1)];
      nextNameIndex += 2;
      return this;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("read only");
    }
    
    public CharSequence getKey()
    {
      return key;
    }
    
    public CharSequence getValue()
    {
      return value;
    }
    
    public CharSequence setValue(CharSequence value)
    {
      throw new UnsupportedOperationException("read only");
    }
    
    public String toString()
    {
      return key.toString() + '=' + value.toString();
    }
  }
  
  private final class ReadOnlyStringIterator implements Map.Entry<String, String>, Iterator<Map.Entry<String, String>> {
    private String key;
    private String value;
    private int nextNameIndex;
    
    private ReadOnlyStringIterator() {}
    
    public boolean hasNext() {
      return nextNameIndex != nameValuePairs.length;
    }
    
    public Map.Entry<String, String> next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      key = nameValuePairs[nextNameIndex].toString();
      value = nameValuePairs[(nextNameIndex + 1)].toString();
      nextNameIndex += 2;
      return this;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("read only");
    }
    
    public String getKey()
    {
      return key;
    }
    
    public String getValue()
    {
      return value;
    }
    
    public String setValue(String value)
    {
      throw new UnsupportedOperationException("read only");
    }
    
    public String toString()
    {
      return key + '=' + value;
    }
  }
  
  private final class ReadOnlyStringValueIterator implements Iterator<String> {
    private final CharSequence name;
    private final int nameHash;
    private int nextNameIndex;
    
    ReadOnlyStringValueIterator(CharSequence name) {
      this.name = name;
      nameHash = AsciiString.hashCode(name);
      nextNameIndex = findNextValue();
    }
    
    public boolean hasNext()
    {
      return nextNameIndex != -1;
    }
    
    public String next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      String value = nameValuePairs[(nextNameIndex + 1)].toString();
      nextNameIndex = findNextValue();
      return value;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("read only");
    }
    
    private int findNextValue() {
      for (int i = nextNameIndex; i < nameValuePairs.length; i += 2) {
        CharSequence roName = nameValuePairs[i];
        if ((nameHash == AsciiString.hashCode(roName)) && (AsciiString.contentEqualsIgnoreCase(name, roName))) {
          return i;
        }
      }
      return -1;
    }
  }
  
  private final class ReadOnlyValueIterator implements Iterator<CharSequence> {
    private final CharSequence name;
    private final int nameHash;
    private int nextNameIndex;
    
    ReadOnlyValueIterator(CharSequence name) {
      this.name = name;
      nameHash = AsciiString.hashCode(name);
      nextNameIndex = findNextValue();
    }
    
    public boolean hasNext()
    {
      return nextNameIndex != -1;
    }
    
    public CharSequence next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      CharSequence value = nameValuePairs[(nextNameIndex + 1)];
      nextNameIndex = findNextValue();
      return value;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("read only");
    }
    
    private int findNextValue() {
      for (int i = nextNameIndex; i < nameValuePairs.length; i += 2) {
        CharSequence roName = nameValuePairs[i];
        if ((nameHash == AsciiString.hashCode(roName)) && (AsciiString.contentEqualsIgnoreCase(name, roName))) {
          return i;
        }
      }
      return -1;
    }
  }
}
