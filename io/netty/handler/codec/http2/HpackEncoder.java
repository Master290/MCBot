package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.MathUtil;
import java.util.Arrays;
import java.util.Map.Entry;























































final class HpackEncoder
{
  static final int NOT_FOUND = -1;
  static final int HUFF_CODE_THRESHOLD = 512;
  private final HeaderEntry[] headerFields;
  private final HeaderEntry head = new HeaderEntry(-1, AsciiString.EMPTY_STRING, AsciiString.EMPTY_STRING, Integer.MAX_VALUE, null);
  
  private final HpackHuffmanEncoder hpackHuffmanEncoder = new HpackHuffmanEncoder();
  
  private final byte hashMask;
  
  private final boolean ignoreMaxHeaderListSize;
  private final int huffCodeThreshold;
  private long size;
  private long maxHeaderTableSize;
  private long maxHeaderListSize;
  
  HpackEncoder()
  {
    this(false);
  }
  


  HpackEncoder(boolean ignoreMaxHeaderListSize)
  {
    this(ignoreMaxHeaderListSize, 16, 512);
  }
  


  HpackEncoder(boolean ignoreMaxHeaderListSize, int arraySizeHint, int huffCodeThreshold)
  {
    this.ignoreMaxHeaderListSize = ignoreMaxHeaderListSize;
    maxHeaderTableSize = 4096L;
    maxHeaderListSize = 4294967295L;
    

    headerFields = new HeaderEntry[MathUtil.findNextPositivePowerOfTwo(Math.max(2, Math.min(arraySizeHint, 128)))];
    hashMask = ((byte)(headerFields.length - 1));
    head.before = (head.after = head);
    this.huffCodeThreshold = huffCodeThreshold;
  }
  




  public void encodeHeaders(int streamId, ByteBuf out, Http2Headers headers, Http2HeadersEncoder.SensitivityDetector sensitivityDetector)
    throws Http2Exception
  {
    if (ignoreMaxHeaderListSize) {
      encodeHeadersIgnoreMaxHeaderListSize(out, headers, sensitivityDetector);
    } else {
      encodeHeadersEnforceMaxHeaderListSize(streamId, out, headers, sensitivityDetector);
    }
  }
  
  private void encodeHeadersEnforceMaxHeaderListSize(int streamId, ByteBuf out, Http2Headers headers, Http2HeadersEncoder.SensitivityDetector sensitivityDetector)
    throws Http2Exception
  {
    long headerSize = 0L;
    
    for (Map.Entry<CharSequence, CharSequence> header : headers) {
      CharSequence name = (CharSequence)header.getKey();
      CharSequence value = (CharSequence)header.getValue();
      

      headerSize += HpackHeaderField.sizeOf(name, value);
      if (headerSize > maxHeaderListSize) {
        Http2CodecUtil.headerListSizeExceeded(streamId, maxHeaderListSize, false);
      }
    }
    encodeHeadersIgnoreMaxHeaderListSize(out, headers, sensitivityDetector);
  }
  
  private void encodeHeadersIgnoreMaxHeaderListSize(ByteBuf out, Http2Headers headers, Http2HeadersEncoder.SensitivityDetector sensitivityDetector) throws Http2Exception
  {
    for (Map.Entry<CharSequence, CharSequence> header : headers) {
      CharSequence name = (CharSequence)header.getKey();
      CharSequence value = (CharSequence)header.getValue();
      encodeHeader(out, name, value, sensitivityDetector.isSensitive(name, value), 
        HpackHeaderField.sizeOf(name, value));
    }
  }
  





  private void encodeHeader(ByteBuf out, CharSequence name, CharSequence value, boolean sensitive, long headerSize)
  {
    if (sensitive) {
      int nameIndex = getNameIndex(name);
      encodeLiteral(out, name, value, HpackUtil.IndexType.NEVER, nameIndex);
      return;
    }
    

    if (maxHeaderTableSize == 0L) {
      int staticTableIndex = HpackStaticTable.getIndexInsensitive(name, value);
      if (staticTableIndex == -1) {
        int nameIndex = HpackStaticTable.getIndex(name);
        encodeLiteral(out, name, value, HpackUtil.IndexType.NONE, nameIndex);
      } else {
        encodeInteger(out, 128, 7, staticTableIndex);
      }
      return;
    }
    

    if (headerSize > maxHeaderTableSize) {
      int nameIndex = getNameIndex(name);
      encodeLiteral(out, name, value, HpackUtil.IndexType.NONE, nameIndex);
      return;
    }
    
    HeaderEntry headerField = getEntryInsensitive(name, value);
    if (headerField != null) {
      int index = getIndex(index) + HpackStaticTable.length;
      
      encodeInteger(out, 128, 7, index);
    } else {
      int staticTableIndex = HpackStaticTable.getIndexInsensitive(name, value);
      if (staticTableIndex != -1)
      {
        encodeInteger(out, 128, 7, staticTableIndex);
      } else {
        ensureCapacity(headerSize);
        encodeLiteral(out, name, value, HpackUtil.IndexType.INCREMENTAL, getNameIndex(name));
        add(name, value, headerSize);
      }
    }
  }
  

  public void setMaxHeaderTableSize(ByteBuf out, long maxHeaderTableSize)
    throws Http2Exception
  {
    if ((maxHeaderTableSize < 0L) || (maxHeaderTableSize > 4294967295L)) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Header Table Size must be >= %d and <= %d but was %d", new Object[] {
        Long.valueOf(0L), Long.valueOf(4294967295L), Long.valueOf(maxHeaderTableSize) });
    }
    if (this.maxHeaderTableSize == maxHeaderTableSize) {
      return;
    }
    this.maxHeaderTableSize = maxHeaderTableSize;
    ensureCapacity(0L);
    
    encodeInteger(out, 32, 5, maxHeaderTableSize);
  }
  


  public long getMaxHeaderTableSize()
  {
    return maxHeaderTableSize;
  }
  
  public void setMaxHeaderListSize(long maxHeaderListSize) throws Http2Exception {
    if ((maxHeaderListSize < 0L) || (maxHeaderListSize > 4294967295L)) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Header List Size must be >= %d and <= %d but was %d", new Object[] {
        Long.valueOf(0L), Long.valueOf(4294967295L), Long.valueOf(maxHeaderListSize) });
    }
    this.maxHeaderListSize = maxHeaderListSize;
  }
  
  public long getMaxHeaderListSize() {
    return maxHeaderListSize;
  }
  


  private static void encodeInteger(ByteBuf out, int mask, int n, int i)
  {
    encodeInteger(out, mask, n, i);
  }
  


  private static void encodeInteger(ByteBuf out, int mask, int n, long i)
  {
    assert ((n >= 0) && (n <= 8)) : ("N: " + n);
    int nbits = 255 >>> 8 - n;
    if (i < nbits) {
      out.writeByte((int)(mask | i));
    } else {
      out.writeByte(mask | nbits);
      for (long length = i - nbits; 
          (length & 0xFFFFFFFFFFFFFF80) != 0L; length >>>= 7) {
        out.writeByte((int)(length & 0x7F | 0x80));
      }
      out.writeByte((int)length);
    }
  }
  

  private void encodeStringLiteral(ByteBuf out, CharSequence string)
  {
    int huffmanLength;
    
    if ((string.length() >= huffCodeThreshold) && 
      ((huffmanLength = hpackHuffmanEncoder.getEncodedLength(string)) < string.length())) {
      encodeInteger(out, 128, 7, huffmanLength);
      hpackHuffmanEncoder.encode(out, string);
    } else {
      encodeInteger(out, 0, 7, string.length());
      if ((string instanceof AsciiString))
      {
        AsciiString asciiString = (AsciiString)string;
        out.writeBytes(asciiString.array(), asciiString.arrayOffset(), asciiString.length());
      }
      else
      {
        out.writeCharSequence(string, CharsetUtil.ISO_8859_1);
      }
    }
  }
  



  private void encodeLiteral(ByteBuf out, CharSequence name, CharSequence value, HpackUtil.IndexType indexType, int nameIndex)
  {
    boolean nameIndexValid = nameIndex != -1;
    switch (1.$SwitchMap$io$netty$handler$codec$http2$HpackUtil$IndexType[indexType.ordinal()]) {
    case 1: 
      encodeInteger(out, 64, 6, nameIndexValid ? nameIndex : 0);
      break;
    case 2: 
      encodeInteger(out, 0, 4, nameIndexValid ? nameIndex : 0);
      break;
    case 3: 
      encodeInteger(out, 16, 4, nameIndexValid ? nameIndex : 0);
      break;
    default: 
      throw new Error("should not reach here");
    }
    if (!nameIndexValid) {
      encodeStringLiteral(out, name);
    }
    encodeStringLiteral(out, value);
  }
  
  private int getNameIndex(CharSequence name) {
    int index = HpackStaticTable.getIndex(name);
    if (index == -1) {
      index = getIndex(name);
      if (index >= 0) {
        index += HpackStaticTable.length;
      }
    }
    return index;
  }
  



  private void ensureCapacity(long headerSize)
  {
    while (maxHeaderTableSize - size < headerSize) {
      int index = length();
      if (index == 0) {
        break;
      }
      remove();
    }
  }
  


  int length()
  {
    return size == 0L ? 0 : head.after.index - head.before.index + 1;
  }
  


  long size()
  {
    return size;
  }
  


  HpackHeaderField getHeaderField(int index)
  {
    HeaderEntry entry = head;
    while (index-- >= 0) {
      entry = before;
    }
    return entry;
  }
  



  private HeaderEntry getEntryInsensitive(CharSequence name, CharSequence value)
  {
    if ((length() == 0) || (name == null) || (value == null)) {
      return null;
    }
    int h = AsciiString.hashCode(name);
    int i = index(h);
    for (HeaderEntry e = headerFields[i]; e != null; e = next)
    {

      if ((hash == h) && (HpackUtil.equalsVariableTime(value, value)) && (HpackUtil.equalsVariableTime(name, name))) {
        return e;
      }
    }
    return null;
  }
  



  private int getIndex(CharSequence name)
  {
    if ((length() == 0) || (name == null)) {
      return -1;
    }
    int h = AsciiString.hashCode(name);
    int i = index(h);
    for (HeaderEntry e = headerFields[i]; e != null; e = next) {
      if ((hash == h) && (HpackUtil.equalsConstantTime(name, name) != 0)) {
        return getIndex(index);
      }
    }
    return -1;
  }
  


  private int getIndex(int index)
  {
    return index == -1 ? -1 : index - head.before.index + 1;
  }
  





  private void add(CharSequence name, CharSequence value, long headerSize)
  {
    if (headerSize > maxHeaderTableSize) {
      clear();
      return;
    }
    

    while (maxHeaderTableSize - size < headerSize) {
      remove();
    }
    
    int h = AsciiString.hashCode(name);
    int i = index(h);
    HeaderEntry old = headerFields[i];
    HeaderEntry e = new HeaderEntry(h, name, value, head.before.index - 1, old);
    headerFields[i] = e;
    e.addBefore(head);
    size += headerSize;
  }
  


  private HpackHeaderField remove()
  {
    if (size == 0L) {
      return null;
    }
    HeaderEntry eldest = head.after;
    int h = hash;
    int i = index(h);
    HeaderEntry prev = headerFields[i];
    HeaderEntry e = prev;
    while (e != null) {
      HeaderEntry next = next;
      if (e == eldest) {
        if (prev == eldest) {
          headerFields[i] = next;
        } else {
          next = next;
        }
        eldest.remove();
        size -= eldest.size();
        return eldest;
      }
      prev = e;
      e = next;
    }
    return null;
  }
  


  private void clear()
  {
    Arrays.fill(headerFields, null);
    head.before = (head.after = head);
    size = 0L;
  }
  


  private int index(int h)
  {
    return h & hashMask;
  }
  


  private static final class HeaderEntry
    extends HpackHeaderField
  {
    HeaderEntry before;
    
    HeaderEntry after;
    
    HeaderEntry next;
    
    int hash;
    
    int index;
    

    HeaderEntry(int hash, CharSequence name, CharSequence value, int index, HeaderEntry next)
    {
      super(value);
      this.index = index;
      this.hash = hash;
      this.next = next;
    }
    


    private void remove()
    {
      before.after = after;
      after.before = before;
      before = null;
      after = null;
      next = null;
    }
    


    private void addBefore(HeaderEntry existingEntry)
    {
      after = existingEntry;
      before = before;
      before.after = this;
      after.before = this;
    }
  }
}
