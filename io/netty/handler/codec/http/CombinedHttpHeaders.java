package io.netty.handler.codec.http;

import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.DefaultHeaders.NameValidator;
import io.netty.handler.codec.Headers;
import io.netty.handler.codec.ValueConverter;
import io.netty.util.AsciiString;
import io.netty.util.HashingStrategy;
import io.netty.util.internal.StringUtil;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;






















public class CombinedHttpHeaders
  extends DefaultHttpHeaders
{
  public CombinedHttpHeaders(boolean validate)
  {
    super(new CombinedHttpHeadersImpl(AsciiString.CASE_INSENSITIVE_HASHER, valueConverter(validate), nameValidator(validate)));
  }
  
  public boolean containsValue(CharSequence name, CharSequence value, boolean ignoreCase)
  {
    return super.containsValue(name, StringUtil.trimOws(value), ignoreCase);
  }
  

  private static final class CombinedHttpHeadersImpl
    extends DefaultHeaders<CharSequence, CharSequence, CombinedHttpHeadersImpl>
  {
    private static final int VALUE_LENGTH_ESTIMATE = 10;
    private CsvValueEscaper<Object> objectEscaper;
    private CsvValueEscaper<CharSequence> charSequenceEscaper;
    
    private CsvValueEscaper<Object> objectEscaper()
    {
      if (objectEscaper == null) {
        objectEscaper = new CsvValueEscaper()
        {
          public CharSequence escape(Object value) {
            return StringUtil.escapeCsv((CharSequence)valueConverter().convertObject(value), true);
          }
        };
      }
      return objectEscaper;
    }
    
    private CsvValueEscaper<CharSequence> charSequenceEscaper() {
      if (charSequenceEscaper == null) {
        charSequenceEscaper = new CsvValueEscaper()
        {
          public CharSequence escape(CharSequence value) {
            return StringUtil.escapeCsv(value, true);
          }
        };
      }
      return charSequenceEscaper;
    }
    

    CombinedHttpHeadersImpl(HashingStrategy<CharSequence> nameHashingStrategy, ValueConverter<CharSequence> valueConverter, DefaultHeaders.NameValidator<CharSequence> nameValidator)
    {
      super(valueConverter, nameValidator);
    }
    
    public Iterator<CharSequence> valueIterator(CharSequence name)
    {
      Iterator<CharSequence> itr = super.valueIterator(name);
      if ((!itr.hasNext()) || (cannotBeCombined(name))) {
        return itr;
      }
      Iterator<CharSequence> unescapedItr = StringUtil.unescapeCsvFields((CharSequence)itr.next()).iterator();
      if (itr.hasNext()) {
        throw new IllegalStateException("CombinedHttpHeaders should only have one value");
      }
      return unescapedItr;
    }
    
    public List<CharSequence> getAll(CharSequence name)
    {
      List<CharSequence> values = super.getAll(name);
      if ((values.isEmpty()) || (cannotBeCombined(name))) {
        return values;
      }
      if (values.size() != 1) {
        throw new IllegalStateException("CombinedHttpHeaders should only have one value");
      }
      return StringUtil.unescapeCsvFields((CharSequence)values.get(0));
    }
    

    public CombinedHttpHeadersImpl add(Headers<? extends CharSequence, ? extends CharSequence, ?> headers)
    {
      if (headers == this) {
        throw new IllegalArgumentException("can't add to itself.");
      }
      if ((headers instanceof CombinedHttpHeadersImpl)) {
        if (isEmpty())
        {
          addImpl(headers);
        }
        else {
          for (Map.Entry<? extends CharSequence, ? extends CharSequence> header : headers) {
            addEscapedValue((CharSequence)header.getKey(), (CharSequence)header.getValue());
          }
        }
      } else {
        for (Map.Entry<? extends CharSequence, ? extends CharSequence> header : headers) {
          add((CharSequence)header.getKey(), (CharSequence)header.getValue());
        }
      }
      return this;
    }
    
    public CombinedHttpHeadersImpl set(Headers<? extends CharSequence, ? extends CharSequence, ?> headers)
    {
      if (headers == this) {
        return this;
      }
      clear();
      return add(headers);
    }
    
    public CombinedHttpHeadersImpl setAll(Headers<? extends CharSequence, ? extends CharSequence, ?> headers)
    {
      if (headers == this) {
        return this;
      }
      for (CharSequence key : headers.names()) {
        remove(key);
      }
      return add(headers);
    }
    
    public CombinedHttpHeadersImpl add(CharSequence name, CharSequence value)
    {
      return addEscapedValue(name, charSequenceEscaper().escape(value));
    }
    
    public CombinedHttpHeadersImpl add(CharSequence name, CharSequence... values)
    {
      return addEscapedValue(name, commaSeparate(charSequenceEscaper(), values));
    }
    
    public CombinedHttpHeadersImpl add(CharSequence name, Iterable<? extends CharSequence> values)
    {
      return addEscapedValue(name, commaSeparate(charSequenceEscaper(), values));
    }
    
    public CombinedHttpHeadersImpl addObject(CharSequence name, Object value)
    {
      return addEscapedValue(name, commaSeparate(objectEscaper(), new Object[] { value }));
    }
    
    public CombinedHttpHeadersImpl addObject(CharSequence name, Iterable<?> values)
    {
      return addEscapedValue(name, commaSeparate(objectEscaper(), values));
    }
    
    public CombinedHttpHeadersImpl addObject(CharSequence name, Object... values)
    {
      return addEscapedValue(name, commaSeparate(objectEscaper(), values));
    }
    
    public CombinedHttpHeadersImpl set(CharSequence name, CharSequence... values)
    {
      super.set(name, commaSeparate(charSequenceEscaper(), values));
      return this;
    }
    
    public CombinedHttpHeadersImpl set(CharSequence name, Iterable<? extends CharSequence> values)
    {
      super.set(name, commaSeparate(charSequenceEscaper(), values));
      return this;
    }
    
    public CombinedHttpHeadersImpl setObject(CharSequence name, Object value)
    {
      super.set(name, commaSeparate(objectEscaper(), new Object[] { value }));
      return this;
    }
    
    public CombinedHttpHeadersImpl setObject(CharSequence name, Object... values)
    {
      super.set(name, commaSeparate(objectEscaper(), values));
      return this;
    }
    
    public CombinedHttpHeadersImpl setObject(CharSequence name, Iterable<?> values)
    {
      super.set(name, commaSeparate(objectEscaper(), values));
      return this;
    }
    
    private static boolean cannotBeCombined(CharSequence name) {
      return HttpHeaderNames.SET_COOKIE.contentEqualsIgnoreCase(name);
    }
    
    private CombinedHttpHeadersImpl addEscapedValue(CharSequence name, CharSequence escapedValue) {
      CharSequence currentValue = (CharSequence)super.get(name);
      if ((currentValue == null) || (cannotBeCombined(name))) {
        super.add(name, escapedValue);
      } else {
        super.set(name, commaSeparateEscapedValues(currentValue, escapedValue));
      }
      return this;
    }
    
    private static <T> CharSequence commaSeparate(CsvValueEscaper<T> escaper, T... values) {
      StringBuilder sb = new StringBuilder(values.length * 10);
      if (values.length > 0) {
        int end = values.length - 1;
        for (int i = 0; i < end; i++) {
          sb.append(escaper.escape(values[i])).append(',');
        }
        sb.append(escaper.escape(values[end]));
      }
      return sb;
    }
    

    private static <T> CharSequence commaSeparate(CsvValueEscaper<T> escaper, Iterable<? extends T> values)
    {
      StringBuilder sb = (values instanceof Collection) ? new StringBuilder(((Collection)values).size() * 10) : new StringBuilder();
      Iterator<? extends T> iterator = values.iterator();
      if (iterator.hasNext()) {
        T next = iterator.next();
        while (iterator.hasNext()) {
          sb.append(escaper.escape(next)).append(',');
          next = iterator.next();
        }
        sb.append(escaper.escape(next));
      }
      return sb;
    }
    
    private static CharSequence commaSeparateEscapedValues(CharSequence currentValue, CharSequence value) {
      return 
      

        new StringBuilder(currentValue.length() + 1 + value.length()).append(currentValue).append(',').append(value);
    }
    
    private static abstract interface CsvValueEscaper<T>
    {
      public abstract CharSequence escape(T paramT);
    }
  }
}
