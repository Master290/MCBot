package io.netty.handler.codec.http2;

import io.netty.handler.codec.UnsupportedValueConverter;
import io.netty.util.AsciiString;
import java.util.Arrays;
import java.util.List;






































final class HpackStaticTable
{
  static final int NOT_FOUND = -1;
  private static final List<HpackHeaderField> STATIC_TABLE = Arrays.asList(new HpackHeaderField[] {
    newEmptyHeaderField(":authority"), 
    newHeaderField(":method", "GET"), 
    newHeaderField(":method", "POST"), 
    newHeaderField(":path", "/"), 
    newHeaderField(":path", "/index.html"), 
    newHeaderField(":scheme", "http"), 
    newHeaderField(":scheme", "https"), 
    newHeaderField(":status", "200"), 
    newHeaderField(":status", "204"), 
    newHeaderField(":status", "206"), 
    newHeaderField(":status", "304"), 
    newHeaderField(":status", "400"), 
    newHeaderField(":status", "404"), 
    newHeaderField(":status", "500"), 
    newEmptyHeaderField("accept-charset"), 
    newHeaderField("accept-encoding", "gzip, deflate"), 
    newEmptyHeaderField("accept-language"), 
    newEmptyHeaderField("accept-ranges"), 
    newEmptyHeaderField("accept"), 
    newEmptyHeaderField("access-control-allow-origin"), 
    newEmptyHeaderField("age"), 
    newEmptyHeaderField("allow"), 
    newEmptyHeaderField("authorization"), 
    newEmptyHeaderField("cache-control"), 
    newEmptyHeaderField("content-disposition"), 
    newEmptyHeaderField("content-encoding"), 
    newEmptyHeaderField("content-language"), 
    newEmptyHeaderField("content-length"), 
    newEmptyHeaderField("content-location"), 
    newEmptyHeaderField("content-range"), 
    newEmptyHeaderField("content-type"), 
    newEmptyHeaderField("cookie"), 
    newEmptyHeaderField("date"), 
    newEmptyHeaderField("etag"), 
    newEmptyHeaderField("expect"), 
    newEmptyHeaderField("expires"), 
    newEmptyHeaderField("from"), 
    newEmptyHeaderField("host"), 
    newEmptyHeaderField("if-match"), 
    newEmptyHeaderField("if-modified-since"), 
    newEmptyHeaderField("if-none-match"), 
    newEmptyHeaderField("if-range"), 
    newEmptyHeaderField("if-unmodified-since"), 
    newEmptyHeaderField("last-modified"), 
    newEmptyHeaderField("link"), 
    newEmptyHeaderField("location"), 
    newEmptyHeaderField("max-forwards"), 
    newEmptyHeaderField("proxy-authenticate"), 
    newEmptyHeaderField("proxy-authorization"), 
    newEmptyHeaderField("range"), 
    newEmptyHeaderField("referer"), 
    newEmptyHeaderField("refresh"), 
    newEmptyHeaderField("retry-after"), 
    newEmptyHeaderField("server"), 
    newEmptyHeaderField("set-cookie"), 
    newEmptyHeaderField("strict-transport-security"), 
    newEmptyHeaderField("transfer-encoding"), 
    newEmptyHeaderField("user-agent"), 
    newEmptyHeaderField("vary"), 
    newEmptyHeaderField("via"), 
    newEmptyHeaderField("www-authenticate") });
  
  private static HpackHeaderField newEmptyHeaderField(String name)
  {
    return new HpackHeaderField(AsciiString.cached(name), AsciiString.EMPTY_STRING);
  }
  
  private static HpackHeaderField newHeaderField(String name, String value) {
    return new HpackHeaderField(AsciiString.cached(name), AsciiString.cached(value));
  }
  
  private static final CharSequenceMap<Integer> STATIC_INDEX_BY_NAME = createMap();
  
  private static final int MAX_SAME_NAME_FIELD_INDEX = maxSameNameFieldIndex();
  



  static final int length = STATIC_TABLE.size();
  


  static HpackHeaderField getEntry(int index)
  {
    return (HpackHeaderField)STATIC_TABLE.get(index - 1);
  }
  



  static int getIndex(CharSequence name)
  {
    Integer index = (Integer)STATIC_INDEX_BY_NAME.get(name);
    if (index == null) {
      return -1;
    }
    return index.intValue();
  }
  



  static int getIndexInsensitive(CharSequence name, CharSequence value)
  {
    int index = getIndex(name);
    if (index == -1) {
      return -1;
    }
    

    HpackHeaderField entry = getEntry(index);
    if (HpackUtil.equalsVariableTime(value, value)) {
      return index;
    }
    

    index++;
    while (index <= MAX_SAME_NAME_FIELD_INDEX) {
      entry = getEntry(index);
      if (!HpackUtil.equalsVariableTime(name, name))
      {


        return -1;
      }
      if (HpackUtil.equalsVariableTime(value, value)) {
        return index;
      }
      index++;
    }
    
    return -1;
  }
  
  private static CharSequenceMap<Integer> createMap()
  {
    int length = STATIC_TABLE.size();
    

    CharSequenceMap<Integer> ret = new CharSequenceMap(true, UnsupportedValueConverter.instance(), length);
    

    for (int index = length; index > 0; index--) {
      HpackHeaderField entry = getEntry(index);
      CharSequence name = name;
      ret.set(name, Integer.valueOf(index));
    }
    return ret;
  }
  





  private static int maxSameNameFieldIndex()
  {
    int length = STATIC_TABLE.size();
    HpackHeaderField cursor = getEntry(length);
    for (int index = length - 1; index > 0; index--) {
      HpackHeaderField entry = getEntry(index);
      if (HpackUtil.equalsVariableTime(name, name)) {
        return index + 1;
      }
      cursor = entry;
    }
    
    return length;
  }
  
  private HpackStaticTable() {}
}
