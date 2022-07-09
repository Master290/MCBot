package io.netty.handler.codec.string;

import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;






















public final class LineSeparator
{
  public static final LineSeparator DEFAULT = new LineSeparator(StringUtil.NEWLINE);
  



  public static final LineSeparator UNIX = new LineSeparator("\n");
  



  public static final LineSeparator WINDOWS = new LineSeparator("\r\n");
  

  private final String value;
  

  public LineSeparator(String lineSeparator)
  {
    value = ((String)ObjectUtil.checkNotNull(lineSeparator, "lineSeparator"));
  }
  


  public String value()
  {
    return value;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LineSeparator)) {
      return false;
    }
    LineSeparator that = (LineSeparator)o;
    return value == null ? true : value != null ? value.equals(value) : false;
  }
  
  public int hashCode()
  {
    return value != null ? value.hashCode() : 0;
  }
  



  public String toString()
  {
    return ByteBufUtil.hexDump(value.getBytes(CharsetUtil.UTF_8));
  }
}
