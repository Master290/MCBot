package io.netty.handler.codec.http2;

import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.DefaultHeaders.NameValidator;
import io.netty.handler.codec.Headers;
import io.netty.util.AsciiString;
import io.netty.util.HashingStrategy;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;







































public final class ReadOnlyHttp2Headers
  implements Http2Headers
{
  private static final byte PSEUDO_HEADER_TOKEN = 58;
  private final AsciiString[] pseudoHeaders;
  private final AsciiString[] otherHeaders;
  
  public static ReadOnlyHttp2Headers trailers(boolean validateHeaders, AsciiString... otherHeaders)
  {
    return new ReadOnlyHttp2Headers(validateHeaders, EmptyArrays.EMPTY_ASCII_STRINGS, otherHeaders);
  }
  

















  public static ReadOnlyHttp2Headers clientHeaders(boolean validateHeaders, AsciiString method, AsciiString path, AsciiString scheme, AsciiString authority, AsciiString... otherHeaders)
  {
    return new ReadOnlyHttp2Headers(validateHeaders, new AsciiString[] {Http2Headers.PseudoHeaderName.METHOD
    
      .value(), method, Http2Headers.PseudoHeaderName.PATH.value(), path, Http2Headers.PseudoHeaderName.SCHEME
      .value(), scheme, Http2Headers.PseudoHeaderName.AUTHORITY.value(), authority }, otherHeaders);
  }
  















  public static ReadOnlyHttp2Headers serverHeaders(boolean validateHeaders, AsciiString status, AsciiString... otherHeaders)
  {
    return new ReadOnlyHttp2Headers(validateHeaders, new AsciiString[] {Http2Headers.PseudoHeaderName.STATUS
      .value(), status }, otherHeaders);
  }
  
  private ReadOnlyHttp2Headers(boolean validateHeaders, AsciiString[] pseudoHeaders, AsciiString... otherHeaders)
  {
    assert ((pseudoHeaders.length & 0x1) == 0);
    if ((otherHeaders.length & 0x1) != 0) {
      throw newInvalidArraySizeException();
    }
    if (validateHeaders) {
      validateHeaders(pseudoHeaders, otherHeaders);
    }
    this.pseudoHeaders = pseudoHeaders;
    this.otherHeaders = otherHeaders;
  }
  
  private static IllegalArgumentException newInvalidArraySizeException() {
    return new IllegalArgumentException("pseudoHeaders and otherHeaders must be arrays of [name, value] pairs");
  }
  
  private static void validateHeaders(AsciiString[] pseudoHeaders, AsciiString... otherHeaders)
  {
    for (int i = 1; i < pseudoHeaders.length; i += 2)
    {
      ObjectUtil.checkNotNullArrayParam(pseudoHeaders[i], i, "pseudoHeaders");
    }
    
    boolean seenNonPseudoHeader = false;
    int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      AsciiString name = otherHeaders[i];
      DefaultHttp2Headers.HTTP2_NAME_VALIDATOR.validateName(name);
      if ((!seenNonPseudoHeader) && (!name.isEmpty()) && (name.byteAt(0) != 58)) {
        seenNonPseudoHeader = true;
      } else if ((seenNonPseudoHeader) && (!name.isEmpty()) && (name.byteAt(0) == 58)) {
        throw new IllegalArgumentException("otherHeaders name at index " + i + " is a pseudo header that appears after non-pseudo headers.");
      }
      
      ObjectUtil.checkNotNullArrayParam(otherHeaders[(i + 1)], i + 1, "otherHeaders");
    }
  }
  
  private AsciiString get0(CharSequence name) {
    int nameHash = AsciiString.hashCode(name);
    
    int pseudoHeadersEnd = pseudoHeaders.length - 1;
    for (int i = 0; i < pseudoHeadersEnd; i += 2) {
      AsciiString roName = pseudoHeaders[i];
      if ((roName.hashCode() == nameHash) && (roName.contentEqualsIgnoreCase(name))) {
        return pseudoHeaders[(i + 1)];
      }
    }
    
    int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      AsciiString roName = otherHeaders[i];
      if ((roName.hashCode() == nameHash) && (roName.contentEqualsIgnoreCase(name))) {
        return otherHeaders[(i + 1)];
      }
    }
    return null;
  }
  
  public CharSequence get(CharSequence name)
  {
    return get0(name);
  }
  
  public CharSequence get(CharSequence name, CharSequence defaultValue)
  {
    CharSequence value = get(name);
    return value != null ? value : defaultValue;
  }
  
  public CharSequence getAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public CharSequence getAndRemove(CharSequence name, CharSequence defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public List<CharSequence> getAll(CharSequence name)
  {
    int nameHash = AsciiString.hashCode(name);
    List<CharSequence> values = new ArrayList();
    
    int pseudoHeadersEnd = pseudoHeaders.length - 1;
    for (int i = 0; i < pseudoHeadersEnd; i += 2) {
      AsciiString roName = pseudoHeaders[i];
      if ((roName.hashCode() == nameHash) && (roName.contentEqualsIgnoreCase(name))) {
        values.add(pseudoHeaders[(i + 1)]);
      }
    }
    
    int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      AsciiString roName = otherHeaders[i];
      if ((roName.hashCode() == nameHash) && (roName.contentEqualsIgnoreCase(name))) {
        values.add(otherHeaders[(i + 1)]);
      }
    }
    
    return values;
  }
  
  public List<CharSequence> getAllAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Boolean getBoolean(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Boolean.valueOf(CharSequenceValueConverter.INSTANCE.convertToBoolean(value)) : null;
  }
  
  public boolean getBoolean(CharSequence name, boolean defaultValue)
  {
    Boolean value = getBoolean(name);
    return value != null ? value.booleanValue() : defaultValue;
  }
  
  public Byte getByte(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Byte.valueOf(CharSequenceValueConverter.INSTANCE.convertToByte(value)) : null;
  }
  
  public byte getByte(CharSequence name, byte defaultValue)
  {
    Byte value = getByte(name);
    return value != null ? value.byteValue() : defaultValue;
  }
  
  public Character getChar(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Character.valueOf(CharSequenceValueConverter.INSTANCE.convertToChar(value)) : null;
  }
  
  public char getChar(CharSequence name, char defaultValue)
  {
    Character value = getChar(name);
    return value != null ? value.charValue() : defaultValue;
  }
  
  public Short getShort(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Short.valueOf(CharSequenceValueConverter.INSTANCE.convertToShort(value)) : null;
  }
  
  public short getShort(CharSequence name, short defaultValue)
  {
    Short value = getShort(name);
    return value != null ? value.shortValue() : defaultValue;
  }
  
  public Integer getInt(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Integer.valueOf(CharSequenceValueConverter.INSTANCE.convertToInt(value)) : null;
  }
  
  public int getInt(CharSequence name, int defaultValue)
  {
    Integer value = getInt(name);
    return value != null ? value.intValue() : defaultValue;
  }
  
  public Long getLong(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Long.valueOf(CharSequenceValueConverter.INSTANCE.convertToLong(value)) : null;
  }
  
  public long getLong(CharSequence name, long defaultValue)
  {
    Long value = getLong(name);
    return value != null ? value.longValue() : defaultValue;
  }
  
  public Float getFloat(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Float.valueOf(CharSequenceValueConverter.INSTANCE.convertToFloat(value)) : null;
  }
  
  public float getFloat(CharSequence name, float defaultValue)
  {
    Float value = getFloat(name);
    return value != null ? value.floatValue() : defaultValue;
  }
  
  public Double getDouble(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Double.valueOf(CharSequenceValueConverter.INSTANCE.convertToDouble(value)) : null;
  }
  
  public double getDouble(CharSequence name, double defaultValue)
  {
    Double value = getDouble(name);
    return value != null ? value.doubleValue() : defaultValue;
  }
  
  public Long getTimeMillis(CharSequence name)
  {
    AsciiString value = get0(name);
    return value != null ? Long.valueOf(CharSequenceValueConverter.INSTANCE.convertToTimeMillis(value)) : null;
  }
  
  public long getTimeMillis(CharSequence name, long defaultValue)
  {
    Long value = getTimeMillis(name);
    return value != null ? value.longValue() : defaultValue;
  }
  
  public Boolean getBooleanAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public boolean getBooleanAndRemove(CharSequence name, boolean defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Byte getByteAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public byte getByteAndRemove(CharSequence name, byte defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Character getCharAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public char getCharAndRemove(CharSequence name, char defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Short getShortAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public short getShortAndRemove(CharSequence name, short defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Integer getIntAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public int getIntAndRemove(CharSequence name, int defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Long getLongAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public long getLongAndRemove(CharSequence name, long defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Float getFloatAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public float getFloatAndRemove(CharSequence name, float defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Double getDoubleAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public double getDoubleAndRemove(CharSequence name, double defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Long getTimeMillisAndRemove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public long getTimeMillisAndRemove(CharSequence name, long defaultValue)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public boolean contains(CharSequence name)
  {
    return get(name) != null;
  }
  
  public boolean contains(CharSequence name, CharSequence value)
  {
    return contains(name, value, false);
  }
  
  public boolean containsObject(CharSequence name, Object value)
  {
    if ((value instanceof CharSequence)) {
      return contains(name, (CharSequence)value);
    }
    return contains(name, value.toString());
  }
  
  public boolean containsBoolean(CharSequence name, boolean value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public boolean containsByte(CharSequence name, byte value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public boolean containsChar(CharSequence name, char value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public boolean containsShort(CharSequence name, short value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public boolean containsInt(CharSequence name, int value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public boolean containsLong(CharSequence name, long value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public boolean containsFloat(CharSequence name, float value)
  {
    return false;
  }
  
  public boolean containsDouble(CharSequence name, double value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public boolean containsTimeMillis(CharSequence name, long value)
  {
    return contains(name, String.valueOf(value));
  }
  
  public int size()
  {
    return pseudoHeaders.length + otherHeaders.length >>> 1;
  }
  
  public boolean isEmpty()
  {
    return (pseudoHeaders.length == 0) && (otherHeaders.length == 0);
  }
  
  public Set<CharSequence> names()
  {
    if (isEmpty()) {
      return Collections.emptySet();
    }
    Set<CharSequence> names = new LinkedHashSet(size());
    int pseudoHeadersEnd = pseudoHeaders.length - 1;
    for (int i = 0; i < pseudoHeadersEnd; i += 2) {
      names.add(pseudoHeaders[i]);
    }
    
    int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      names.add(otherHeaders[i]);
    }
    return names;
  }
  
  public Http2Headers add(CharSequence name, CharSequence value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers add(CharSequence name, Iterable<? extends CharSequence> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers add(CharSequence name, CharSequence... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addObject(CharSequence name, Object value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addObject(CharSequence name, Iterable<?> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addObject(CharSequence name, Object... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addBoolean(CharSequence name, boolean value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addByte(CharSequence name, byte value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addChar(CharSequence name, char value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addShort(CharSequence name, short value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addInt(CharSequence name, int value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addLong(CharSequence name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addFloat(CharSequence name, float value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addDouble(CharSequence name, double value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers addTimeMillis(CharSequence name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers add(Headers<? extends CharSequence, ? extends CharSequence, ?> headers)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers set(CharSequence name, CharSequence value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers set(CharSequence name, Iterable<? extends CharSequence> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers set(CharSequence name, CharSequence... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setObject(CharSequence name, Object value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setObject(CharSequence name, Iterable<?> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setObject(CharSequence name, Object... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setBoolean(CharSequence name, boolean value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setByte(CharSequence name, byte value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setChar(CharSequence name, char value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setShort(CharSequence name, short value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setInt(CharSequence name, int value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setLong(CharSequence name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setFloat(CharSequence name, float value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setDouble(CharSequence name, double value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setTimeMillis(CharSequence name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers set(Headers<? extends CharSequence, ? extends CharSequence, ?> headers)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers setAll(Headers<? extends CharSequence, ? extends CharSequence, ?> headers)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public boolean remove(CharSequence name)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers clear()
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Iterator<Map.Entry<CharSequence, CharSequence>> iterator()
  {
    return new ReadOnlyIterator(null);
  }
  
  public Iterator<CharSequence> valueIterator(CharSequence name)
  {
    return new ReadOnlyValueIterator(name);
  }
  
  public Http2Headers method(CharSequence value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers scheme(CharSequence value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers authority(CharSequence value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers path(CharSequence value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Http2Headers status(CharSequence value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public CharSequence method()
  {
    return get(Http2Headers.PseudoHeaderName.METHOD.value());
  }
  
  public CharSequence scheme()
  {
    return get(Http2Headers.PseudoHeaderName.SCHEME.value());
  }
  
  public CharSequence authority()
  {
    return get(Http2Headers.PseudoHeaderName.AUTHORITY.value());
  }
  
  public CharSequence path()
  {
    return get(Http2Headers.PseudoHeaderName.PATH.value());
  }
  
  public CharSequence status()
  {
    return get(Http2Headers.PseudoHeaderName.STATUS.value());
  }
  
  public boolean contains(CharSequence name, CharSequence value, boolean caseInsensitive)
  {
    int nameHash = AsciiString.hashCode(name);
    HashingStrategy<CharSequence> strategy = caseInsensitive ? AsciiString.CASE_INSENSITIVE_HASHER : AsciiString.CASE_SENSITIVE_HASHER;
    
    int valueHash = strategy.hashCode(value);
    
    return (contains(name, nameHash, value, valueHash, strategy, otherHeaders)) || 
      (contains(name, nameHash, value, valueHash, strategy, pseudoHeaders));
  }
  
  private static boolean contains(CharSequence name, int nameHash, CharSequence value, int valueHash, HashingStrategy<CharSequence> hashingStrategy, AsciiString[] headers)
  {
    int headersEnd = headers.length - 1;
    for (int i = 0; i < headersEnd; i += 2) {
      AsciiString roName = headers[i];
      AsciiString roValue = headers[(i + 1)];
      if ((roName.hashCode() == nameHash) && (roValue.hashCode() == valueHash) && 
        (roName.contentEqualsIgnoreCase(name)) && (hashingStrategy.equals(roValue, value))) {
        return true;
      }
    }
    return false;
  }
  
  public String toString()
  {
    StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('[');
    String separator = "";
    for (Map.Entry<CharSequence, CharSequence> entry : this) {
      builder.append(separator);
      builder.append((CharSequence)entry.getKey()).append(": ").append((CharSequence)entry.getValue());
      separator = ", ";
    }
    return ']';
  }
  
  private final class ReadOnlyValueIterator implements Iterator<CharSequence> {
    private int i;
    private final int nameHash;
    private final CharSequence name;
    private AsciiString[] current = pseudoHeaders.length != 0 ? pseudoHeaders : otherHeaders;
    private AsciiString next;
    
    ReadOnlyValueIterator(CharSequence name) {
      nameHash = AsciiString.hashCode(name);
      this.name = name;
      calculateNext();
    }
    
    public boolean hasNext()
    {
      return next != null;
    }
    
    public CharSequence next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      CharSequence current = next;
      calculateNext();
      return current;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("read only");
    }
    
    private void calculateNext() {
      for (; i < current.length; i += 2) {
        AsciiString roName = current[i];
        if ((roName.hashCode() == nameHash) && (roName.contentEqualsIgnoreCase(name))) {
          if (i + 1 < current.length) {
            next = current[(i + 1)];
            i += 2;
          }
          return;
        }
      }
      if (current == pseudoHeaders) {
        i = 0;
        current = otherHeaders;
        calculateNext();
      } else {
        next = null;
      }
    }
  }
  
  private final class ReadOnlyIterator implements Map.Entry<CharSequence, CharSequence>, Iterator<Map.Entry<CharSequence, CharSequence>>
  {
    private int i;
    private AsciiString[] current = pseudoHeaders.length != 0 ? pseudoHeaders : otherHeaders;
    private AsciiString key;
    private AsciiString value;
    
    private ReadOnlyIterator() {}
    
    public boolean hasNext() { return i != current.length; }
    

    public Map.Entry<CharSequence, CharSequence> next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      key = current[i];
      value = current[(i + 1)];
      i += 2;
      if ((i == current.length) && (current == pseudoHeaders)) {
        current = otherHeaders;
        i = 0;
      }
      return this;
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
    
    public void remove()
    {
      throw new UnsupportedOperationException("read only");
    }
    
    public String toString()
    {
      return key.toString() + '=' + value.toString();
    }
  }
}
