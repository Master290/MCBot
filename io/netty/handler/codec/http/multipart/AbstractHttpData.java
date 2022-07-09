package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



















public abstract class AbstractHttpData
  extends AbstractReferenceCounted
  implements HttpData
{
  private static final Pattern STRIP_PATTERN = Pattern.compile("(?:^\\s+|\\s+$|\\n)");
  private static final Pattern REPLACE_PATTERN = Pattern.compile("[\\r\\t]");
  
  private final String name;
  protected long definedSize;
  protected long size;
  private Charset charset = HttpConstants.DEFAULT_CHARSET;
  private boolean completed;
  private long maxSize = -1L;
  
  protected AbstractHttpData(String name, Charset charset, long size) {
    ObjectUtil.checkNotNull(name, "name");
    
    name = REPLACE_PATTERN.matcher(name).replaceAll(" ");
    name = STRIP_PATTERN.matcher(name).replaceAll("");
    
    this.name = ObjectUtil.checkNonEmpty(name, "name");
    if (charset != null) {
      setCharset(charset);
    }
    definedSize = size;
  }
  
  public long getMaxSize()
  {
    return maxSize;
  }
  
  public void setMaxSize(long maxSize)
  {
    this.maxSize = maxSize;
  }
  
  public void checkSize(long newSize) throws IOException
  {
    if ((maxSize >= 0L) && (newSize > maxSize)) {
      throw new IOException("Size exceed allowed maximum capacity");
    }
  }
  
  public String getName()
  {
    return name;
  }
  
  public boolean isCompleted()
  {
    return completed;
  }
  
  protected void setCompleted() {
    completed = true;
  }
  
  public Charset getCharset()
  {
    return charset;
  }
  
  public void setCharset(Charset charset)
  {
    this.charset = ((Charset)ObjectUtil.checkNotNull(charset, "charset"));
  }
  
  public long length()
  {
    return size;
  }
  
  public long definedLength()
  {
    return definedSize;
  }
  
  public ByteBuf content()
  {
    try {
      return getByteBuf();
    } catch (IOException e) {
      throw new ChannelException(e);
    }
  }
  
  protected void deallocate()
  {
    delete();
  }
  
  public HttpData retain()
  {
    super.retain();
    return this;
  }
  
  public HttpData retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public abstract HttpData touch();
  
  public abstract HttpData touch(Object paramObject);
}
