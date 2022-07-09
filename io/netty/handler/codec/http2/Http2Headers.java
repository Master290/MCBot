package io.netty.handler.codec.http2;

import io.netty.handler.codec.Headers;
import io.netty.util.AsciiString;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract interface Http2Headers extends Headers<CharSequence, CharSequence, Http2Headers>
{
  public abstract Iterator<Map.Entry<CharSequence, CharSequence>> iterator();
  
  public abstract Iterator<CharSequence> valueIterator(CharSequence paramCharSequence);
  
  public abstract Http2Headers method(CharSequence paramCharSequence);
  
  public abstract Http2Headers scheme(CharSequence paramCharSequence);
  
  public abstract Http2Headers authority(CharSequence paramCharSequence);
  
  public abstract Http2Headers path(CharSequence paramCharSequence);
  
  public abstract Http2Headers status(CharSequence paramCharSequence);
  
  public abstract CharSequence method();
  
  public abstract CharSequence scheme();
  
  public abstract CharSequence authority();
  
  public abstract CharSequence path();
  
  public abstract CharSequence status();
  
  public abstract boolean contains(CharSequence paramCharSequence1, CharSequence paramCharSequence2, boolean paramBoolean);
  
  public static enum PseudoHeaderName
  {
    METHOD(":method", true), 
    



    SCHEME(":scheme", true), 
    



    AUTHORITY(":authority", true), 
    



    PATH(":path", true), 
    



    STATUS(":status", false), 
    




    PROTOCOL(":protocol", true);
    
    private static final char PSEUDO_HEADER_PREFIX = ':';
    private static final byte PSEUDO_HEADER_PREFIX_BYTE = 58;
    
    static
    {
      PSEUDO_HEADERS = new CharSequenceMap();
      

      for (PseudoHeaderName pseudoHeader : values()) {
        PSEUDO_HEADERS.add(pseudoHeader.value(), pseudoHeader);
      }
    }
    
    private PseudoHeaderName(String value, boolean requestOnly) {
      this.value = AsciiString.cached(value);
      this.requestOnly = requestOnly;
    }
    
    public AsciiString value()
    {
      return value;
    }
    

    private final AsciiString value;
    private final boolean requestOnly;
    private static final CharSequenceMap<PseudoHeaderName> PSEUDO_HEADERS;
    public static boolean hasPseudoHeaderFormat(CharSequence headerName)
    {
      if ((headerName instanceof AsciiString)) {
        AsciiString asciiHeaderName = (AsciiString)headerName;
        return (asciiHeaderName.length() > 0) && (asciiHeaderName.byteAt(0) == 58);
      }
      return (headerName.length() > 0) && (headerName.charAt(0) == ':');
    }
    



    public static boolean isPseudoHeader(CharSequence header)
    {
      return PSEUDO_HEADERS.contains(header);
    }
    




    public static PseudoHeaderName getPseudoHeader(CharSequence header)
    {
      return (PseudoHeaderName)PSEUDO_HEADERS.get(header);
    }
    




    public boolean isRequestOnly()
    {
      return requestOnly;
    }
  }
}
