package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.nio.charset.Charset;



















public class DiskAttribute
  extends AbstractDiskHttpData
  implements Attribute
{
  public static String baseDirectory;
  public static boolean deleteOnExitTemporaryFile = true;
  

  public static final String prefix = "Attr_";
  
  public static final String postfix = ".att";
  
  private String baseDir;
  
  private boolean deleteOnExit;
  

  public DiskAttribute(String name)
  {
    this(name, HttpConstants.DEFAULT_CHARSET);
  }
  
  public DiskAttribute(String name, String baseDir, boolean deleteOnExit) {
    this(name, HttpConstants.DEFAULT_CHARSET);
    this.baseDir = (baseDir == null ? baseDirectory : baseDir);
    this.deleteOnExit = deleteOnExit;
  }
  
  public DiskAttribute(String name, long definedSize) {
    this(name, definedSize, HttpConstants.DEFAULT_CHARSET, baseDirectory, deleteOnExitTemporaryFile);
  }
  
  public DiskAttribute(String name, long definedSize, String baseDir, boolean deleteOnExit) {
    this(name, definedSize, HttpConstants.DEFAULT_CHARSET);
    this.baseDir = (baseDir == null ? baseDirectory : baseDir);
    this.deleteOnExit = deleteOnExit;
  }
  
  public DiskAttribute(String name, Charset charset) {
    this(name, charset, baseDirectory, deleteOnExitTemporaryFile);
  }
  
  public DiskAttribute(String name, Charset charset, String baseDir, boolean deleteOnExit) {
    super(name, charset, 0L);
    this.baseDir = (baseDir == null ? baseDirectory : baseDir);
    this.deleteOnExit = deleteOnExit;
  }
  
  public DiskAttribute(String name, long definedSize, Charset charset) {
    this(name, definedSize, charset, baseDirectory, deleteOnExitTemporaryFile);
  }
  
  public DiskAttribute(String name, long definedSize, Charset charset, String baseDir, boolean deleteOnExit) {
    super(name, charset, definedSize);
    this.baseDir = (baseDir == null ? baseDirectory : baseDir);
    this.deleteOnExit = deleteOnExit;
  }
  
  public DiskAttribute(String name, String value) throws IOException {
    this(name, value, HttpConstants.DEFAULT_CHARSET);
  }
  
  public DiskAttribute(String name, String value, Charset charset) throws IOException {
    this(name, value, charset, baseDirectory, deleteOnExitTemporaryFile);
  }
  
  public DiskAttribute(String name, String value, Charset charset, String baseDir, boolean deleteOnExit) throws IOException
  {
    super(name, charset, 0L);
    setValue(value);
    this.baseDir = (baseDir == null ? baseDirectory : baseDir);
    this.deleteOnExit = deleteOnExit;
  }
  
  public InterfaceHttpData.HttpDataType getHttpDataType()
  {
    return InterfaceHttpData.HttpDataType.Attribute;
  }
  
  public String getValue() throws IOException
  {
    byte[] bytes = get();
    return new String(bytes, getCharset());
  }
  
  public void setValue(String value) throws IOException
  {
    ObjectUtil.checkNotNull(value, "value");
    byte[] bytes = value.getBytes(getCharset());
    checkSize(bytes.length);
    ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
    if (definedSize > 0L) {
      definedSize = buffer.readableBytes();
    }
    setContent(buffer);
  }
  
  public void addContent(ByteBuf buffer, boolean last) throws IOException
  {
    long newDefinedSize = size + buffer.readableBytes();
    checkSize(newDefinedSize);
    if ((definedSize > 0L) && (definedSize < newDefinedSize)) {
      definedSize = newDefinedSize;
    }
    super.addContent(buffer, last);
  }
  
  public int hashCode()
  {
    return getName().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Attribute)) {
      return false;
    }
    Attribute attribute = (Attribute)o;
    return getName().equalsIgnoreCase(attribute.getName());
  }
  
  public int compareTo(InterfaceHttpData o)
  {
    if (!(o instanceof Attribute))
    {
      throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + o.getHttpDataType());
    }
    return compareTo((Attribute)o);
  }
  
  public int compareTo(Attribute o) {
    return getName().compareToIgnoreCase(o.getName());
  }
  
  public String toString()
  {
    try {
      return getName() + '=' + getValue();
    } catch (IOException e) {
      return getName() + '=' + e;
    }
  }
  
  protected boolean deleteOnExit()
  {
    return deleteOnExit;
  }
  
  protected String getBaseDirectory()
  {
    return baseDir;
  }
  
  protected String getDiskFilename()
  {
    return getName() + ".att";
  }
  
  protected String getPostfix()
  {
    return ".att";
  }
  
  protected String getPrefix()
  {
    return "Attr_";
  }
  
  public Attribute copy()
  {
    ByteBuf content = content();
    return replace(content != null ? content.copy() : null);
  }
  
  public Attribute duplicate()
  {
    ByteBuf content = content();
    return replace(content != null ? content.duplicate() : null);
  }
  
  public Attribute retainedDuplicate()
  {
    ByteBuf content = content();
    if (content != null) {
      content = content.retainedDuplicate();
      boolean success = false;
      try {
        Attribute duplicate = replace(content);
        success = true;
        return duplicate;
      } finally {
        if (!success) {
          content.release();
        }
      }
    }
    return replace(null);
  }
  

  public Attribute replace(ByteBuf content)
  {
    DiskAttribute attr = new DiskAttribute(getName(), baseDir, deleteOnExit);
    attr.setCharset(getCharset());
    if (content != null) {
      try {
        attr.setContent(content);
      } catch (IOException e) {
        throw new ChannelException(e);
      }
    }
    return attr;
  }
  
  public Attribute retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public Attribute retain()
  {
    super.retain();
    return this;
  }
  
  public Attribute touch()
  {
    super.touch();
    return this;
  }
  
  public Attribute touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
