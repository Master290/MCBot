package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.CharsetUtil;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.internal.ObjectUtil;
import java.security.PrivateKey;






























public final class PemPrivateKey
  extends AbstractReferenceCounted
  implements PrivateKey, PemEncoded
{
  private static final long serialVersionUID = 7978017465645018936L;
  private static final byte[] BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n".getBytes(CharsetUtil.US_ASCII);
  private static final byte[] END_PRIVATE_KEY = "\n-----END PRIVATE KEY-----\n".getBytes(CharsetUtil.US_ASCII);
  

  private static final String PKCS8_FORMAT = "PKCS#8";
  

  private final ByteBuf content;
  


  static PemEncoded toPEM(ByteBufAllocator allocator, boolean useDirect, PrivateKey key)
  {
    if ((key instanceof PemEncoded)) {
      return ((PemEncoded)key).retain();
    }
    
    byte[] bytes = key.getEncoded();
    if (bytes == null) {
      throw new IllegalArgumentException(key.getClass().getName() + " does not support encoding");
    }
    
    return toPEM(allocator, useDirect, bytes);
  }
  
  /* Error */
  static PemEncoded toPEM(ByteBufAllocator allocator, boolean useDirect, byte[] bytes)
  {
    // Byte code:
    //   0: aload_2
    //   1: invokestatic 14	io/netty/buffer/Unpooled:wrappedBuffer	([B)Lio/netty/buffer/ByteBuf;
    //   4: astore_3
    //   5: aload_0
    //   6: aload_3
    //   7: invokestatic 15	io/netty/handler/ssl/SslUtils:toBase64	(Lio/netty/buffer/ByteBufAllocator;Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;
    //   10: astore 4
    //   12: getstatic 16	io/netty/handler/ssl/PemPrivateKey:BEGIN_PRIVATE_KEY	[B
    //   15: arraylength
    //   16: aload 4
    //   18: invokevirtual 17	io/netty/buffer/ByteBuf:readableBytes	()I
    //   21: iadd
    //   22: getstatic 18	io/netty/handler/ssl/PemPrivateKey:END_PRIVATE_KEY	[B
    //   25: arraylength
    //   26: iadd
    //   27: istore 5
    //   29: iconst_0
    //   30: istore 6
    //   32: iload_1
    //   33: ifeq +14 -> 47
    //   36: aload_0
    //   37: iload 5
    //   39: invokeinterface 19 2 0
    //   44: goto +11 -> 55
    //   47: aload_0
    //   48: iload 5
    //   50: invokeinterface 20 2 0
    //   55: astore 7
    //   57: aload 7
    //   59: getstatic 16	io/netty/handler/ssl/PemPrivateKey:BEGIN_PRIVATE_KEY	[B
    //   62: invokevirtual 21	io/netty/buffer/ByteBuf:writeBytes	([B)Lio/netty/buffer/ByteBuf;
    //   65: pop
    //   66: aload 7
    //   68: aload 4
    //   70: invokevirtual 22	io/netty/buffer/ByteBuf:writeBytes	(Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;
    //   73: pop
    //   74: aload 7
    //   76: getstatic 18	io/netty/handler/ssl/PemPrivateKey:END_PRIVATE_KEY	[B
    //   79: invokevirtual 21	io/netty/buffer/ByteBuf:writeBytes	([B)Lio/netty/buffer/ByteBuf;
    //   82: pop
    //   83: new 23	io/netty/handler/ssl/PemValue
    //   86: dup
    //   87: aload 7
    //   89: iconst_1
    //   90: invokespecial 24	io/netty/handler/ssl/PemValue:<init>	(Lio/netty/buffer/ByteBuf;Z)V
    //   93: astore 8
    //   95: iconst_1
    //   96: istore 6
    //   98: aload 8
    //   100: astore 9
    //   102: iload 6
    //   104: ifne +8 -> 112
    //   107: aload 7
    //   109: invokestatic 25	io/netty/handler/ssl/SslUtils:zerooutAndRelease	(Lio/netty/buffer/ByteBuf;)V
    //   112: aload 4
    //   114: invokestatic 25	io/netty/handler/ssl/SslUtils:zerooutAndRelease	(Lio/netty/buffer/ByteBuf;)V
    //   117: aload_3
    //   118: invokestatic 25	io/netty/handler/ssl/SslUtils:zerooutAndRelease	(Lio/netty/buffer/ByteBuf;)V
    //   121: aload 9
    //   123: areturn
    //   124: astore 10
    //   126: iload 6
    //   128: ifne +8 -> 136
    //   131: aload 7
    //   133: invokestatic 25	io/netty/handler/ssl/SslUtils:zerooutAndRelease	(Lio/netty/buffer/ByteBuf;)V
    //   136: aload 10
    //   138: athrow
    //   139: astore 11
    //   141: aload 4
    //   143: invokestatic 25	io/netty/handler/ssl/SslUtils:zerooutAndRelease	(Lio/netty/buffer/ByteBuf;)V
    //   146: aload 11
    //   148: athrow
    //   149: astore 12
    //   151: aload_3
    //   152: invokestatic 25	io/netty/handler/ssl/SslUtils:zerooutAndRelease	(Lio/netty/buffer/ByteBuf;)V
    //   155: aload 12
    //   157: athrow
    // Line number table:
    //   Java source line #72	-> byte code offset #0
    //   Java source line #74	-> byte code offset #5
    //   Java source line #76	-> byte code offset #12
    //   Java source line #78	-> byte code offset #29
    //   Java source line #79	-> byte code offset #32
    //   Java source line #81	-> byte code offset #57
    //   Java source line #82	-> byte code offset #66
    //   Java source line #83	-> byte code offset #74
    //   Java source line #85	-> byte code offset #83
    //   Java source line #86	-> byte code offset #95
    //   Java source line #87	-> byte code offset #98
    //   Java source line #90	-> byte code offset #102
    //   Java source line #91	-> byte code offset #107
    //   Java source line #95	-> byte code offset #112
    //   Java source line #98	-> byte code offset #117
    //   Java source line #87	-> byte code offset #121
    //   Java source line #90	-> byte code offset #124
    //   Java source line #91	-> byte code offset #131
    //   Java source line #93	-> byte code offset #136
    //   Java source line #95	-> byte code offset #139
    //   Java source line #96	-> byte code offset #146
    //   Java source line #98	-> byte code offset #149
    //   Java source line #99	-> byte code offset #155
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	158	0	allocator	ByteBufAllocator
    //   0	158	1	useDirect	boolean
    //   0	158	2	bytes	byte[]
    //   4	148	3	encoded	ByteBuf
    //   10	132	4	base64	ByteBuf
    //   27	22	5	size	int
    //   30	97	6	success	boolean
    //   55	77	7	pem	ByteBuf
    //   93	6	8	value	PemValue
    //   100	22	9	localPemValue1	PemValue
    //   124	13	10	localObject1	Object
    //   139	8	11	localObject2	Object
    //   149	7	12	localObject3	Object
    // Exception table:
    //   from	to	target	type
    //   57	102	124	finally
    //   124	126	124	finally
    //   12	112	139	finally
    //   124	141	139	finally
    //   5	117	149	finally
    //   124	151	149	finally
  }
  
  public static PemPrivateKey valueOf(byte[] key)
  {
    return valueOf(Unpooled.wrappedBuffer(key));
  }
  





  public static PemPrivateKey valueOf(ByteBuf key)
  {
    return new PemPrivateKey(key);
  }
  

  private PemPrivateKey(ByteBuf content)
  {
    this.content = ((ByteBuf)ObjectUtil.checkNotNull(content, "content"));
  }
  
  public boolean isSensitive()
  {
    return true;
  }
  
  public ByteBuf content()
  {
    int count = refCnt();
    if (count <= 0) {
      throw new IllegalReferenceCountException(count);
    }
    
    return content;
  }
  
  public PemPrivateKey copy()
  {
    return replace(content.copy());
  }
  
  public PemPrivateKey duplicate()
  {
    return replace(content.duplicate());
  }
  
  public PemPrivateKey retainedDuplicate()
  {
    return replace(content.retainedDuplicate());
  }
  
  public PemPrivateKey replace(ByteBuf content)
  {
    return new PemPrivateKey(content);
  }
  
  public PemPrivateKey touch()
  {
    content.touch();
    return this;
  }
  
  public PemPrivateKey touch(Object hint)
  {
    content.touch(hint);
    return this;
  }
  
  public PemPrivateKey retain()
  {
    return (PemPrivateKey)super.retain();
  }
  
  public PemPrivateKey retain(int increment)
  {
    return (PemPrivateKey)super.retain(increment);
  }
  


  protected void deallocate()
  {
    SslUtils.zerooutAndRelease(content);
  }
  
  public byte[] getEncoded()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getAlgorithm()
  {
    throw new UnsupportedOperationException();
  }
  
  public String getFormat()
  {
    return "PKCS#8";
  }
  







  public void destroy()
  {
    release(refCnt());
  }
  







  public boolean isDestroyed()
  {
    return refCnt() == 0;
  }
}
