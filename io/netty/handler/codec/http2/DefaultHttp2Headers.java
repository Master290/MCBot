package io.netty.handler.codec.http2;

import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.DefaultHeaders.HeaderEntry;
import io.netty.handler.codec.DefaultHeaders.NameValidator;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.PlatformDependent;




















public class DefaultHttp2Headers
  extends DefaultHeaders<CharSequence, CharSequence, Http2Headers>
  implements Http2Headers
{
  private static final ByteProcessor HTTP2_NAME_VALIDATOR_PROCESSOR = new ByteProcessor()
  {
    public boolean process(byte value) {
      return !AsciiString.isUpperCase(value);
    }
  };
  static final DefaultHeaders.NameValidator<CharSequence> HTTP2_NAME_VALIDATOR = new DefaultHeaders.NameValidator()
  {
    public void validateName(CharSequence name) {
      if ((name == null) || (name.length() == 0)) {
        PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "empty headers are not allowed [%s]", new Object[] { name }));
      }
      
      if ((name instanceof AsciiString))
      {
        try {
          index = ((AsciiString)name).forEachByte(DefaultHttp2Headers.HTTP2_NAME_VALIDATOR_PROCESSOR);
        } catch (Http2Exception e) { int index;
          PlatformDependent.throwException(e);
          return;
        } catch (Throwable t) {
          PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, t, "unexpected error. invalid header name [%s]", new Object[] { name })); return;
        }
        
        int index;
        
        if (index != -1) {
          PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "invalid header name [%s]", new Object[] { name }));
        }
      }
      else {
        for (int i = 0; i < name.length(); i++) {
          if (AsciiString.isUpperCase(name.charAt(i))) {
            PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "invalid header name [%s]", new Object[] { name }));
          }
        }
      }
    }
  };
  

  private DefaultHeaders.HeaderEntry<CharSequence, CharSequence> firstNonPseudo = head;
  





  public DefaultHttp2Headers()
  {
    this(true);
  }
  







  public DefaultHttp2Headers(boolean validate)
  {
    super(AsciiString.CASE_SENSITIVE_HASHER, CharSequenceValueConverter.INSTANCE, validate ? HTTP2_NAME_VALIDATOR : DefaultHeaders.NameValidator.NOT_NULL);
  }
  











  public DefaultHttp2Headers(boolean validate, int arraySizeHint)
  {
    super(AsciiString.CASE_SENSITIVE_HASHER, CharSequenceValueConverter.INSTANCE, validate ? HTTP2_NAME_VALIDATOR : DefaultHeaders.NameValidator.NOT_NULL, arraySizeHint);
  }
  



  public Http2Headers clear()
  {
    firstNonPseudo = head;
    return (Http2Headers)super.clear();
  }
  
  public boolean equals(Object o)
  {
    return ((o instanceof Http2Headers)) && (equals((Http2Headers)o, AsciiString.CASE_SENSITIVE_HASHER));
  }
  
  public int hashCode()
  {
    return hashCode(AsciiString.CASE_SENSITIVE_HASHER);
  }
  
  public Http2Headers method(CharSequence value)
  {
    set(Http2Headers.PseudoHeaderName.METHOD.value(), value);
    return this;
  }
  
  public Http2Headers scheme(CharSequence value)
  {
    set(Http2Headers.PseudoHeaderName.SCHEME.value(), value);
    return this;
  }
  
  public Http2Headers authority(CharSequence value)
  {
    set(Http2Headers.PseudoHeaderName.AUTHORITY.value(), value);
    return this;
  }
  
  public Http2Headers path(CharSequence value)
  {
    set(Http2Headers.PseudoHeaderName.PATH.value(), value);
    return this;
  }
  
  public Http2Headers status(CharSequence value)
  {
    set(Http2Headers.PseudoHeaderName.STATUS.value(), value);
    return this;
  }
  
  public CharSequence method()
  {
    return (CharSequence)get(Http2Headers.PseudoHeaderName.METHOD.value());
  }
  
  public CharSequence scheme()
  {
    return (CharSequence)get(Http2Headers.PseudoHeaderName.SCHEME.value());
  }
  
  public CharSequence authority()
  {
    return (CharSequence)get(Http2Headers.PseudoHeaderName.AUTHORITY.value());
  }
  
  public CharSequence path()
  {
    return (CharSequence)get(Http2Headers.PseudoHeaderName.PATH.value());
  }
  
  public CharSequence status()
  {
    return (CharSequence)get(Http2Headers.PseudoHeaderName.STATUS.value());
  }
  
  public boolean contains(CharSequence name, CharSequence value)
  {
    return contains(name, value, false);
  }
  
  public boolean contains(CharSequence name, CharSequence value, boolean caseInsensitive)
  {
    return contains(name, value, caseInsensitive ? AsciiString.CASE_INSENSITIVE_HASHER : AsciiString.CASE_SENSITIVE_HASHER);
  }
  

  protected final DefaultHeaders.HeaderEntry<CharSequence, CharSequence> newHeaderEntry(int h, CharSequence name, CharSequence value, DefaultHeaders.HeaderEntry<CharSequence, CharSequence> next)
  {
    return new Http2HeaderEntry(h, name, value, next);
  }
  
  private final class Http2HeaderEntry extends DefaultHeaders.HeaderEntry<CharSequence, CharSequence>
  {
    protected Http2HeaderEntry(CharSequence hash, CharSequence key, DefaultHeaders.HeaderEntry<CharSequence, CharSequence> value) {
      super(key);
      this.value = value;
      this.next = next;
      

      if (Http2Headers.PseudoHeaderName.hasPseudoHeaderFormat(key)) {
        after = firstNonPseudo;
        before = firstNonPseudo.before();
      } else {
        after = head;
        before = head.before();
        if (firstNonPseudo == head) {
          firstNonPseudo = this;
        }
      }
      pointNeighborsToThis();
    }
    
    protected void remove()
    {
      if (this == firstNonPseudo) {
        firstNonPseudo = firstNonPseudo.after();
      }
      super.remove();
    }
  }
}
