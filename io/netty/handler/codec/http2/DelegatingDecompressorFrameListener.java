package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.BrotliDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;
import io.netty.util.internal.ObjectUtil;



























public class DelegatingDecompressorFrameListener
  extends Http2FrameListenerDecorator
{
  private final Http2Connection connection;
  private final boolean strict;
  private boolean flowControllerInitialized;
  private final Http2Connection.PropertyKey propertyKey;
  
  public DelegatingDecompressorFrameListener(Http2Connection connection, Http2FrameListener listener)
  {
    this(connection, listener, true);
  }
  
  public DelegatingDecompressorFrameListener(Http2Connection connection, Http2FrameListener listener, boolean strict)
  {
    super(listener);
    this.connection = connection;
    this.strict = strict;
    
    propertyKey = connection.newKey();
    connection.addListener(new Http2ConnectionAdapter()
    {
      public void onStreamRemoved(Http2Stream stream) {
        DelegatingDecompressorFrameListener.Http2Decompressor decompressor = decompressor(stream);
        if (decompressor != null) {
          DelegatingDecompressorFrameListener.cleanup(decompressor);
        }
      }
    });
  }
  
  /* Error */
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
    throws Http2Exception
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 4	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:connection	Lio/netty/handler/codec/http2/Http2Connection;
    //   4: iload_2
    //   5: invokeinterface 11 2 0
    //   10: astore 6
    //   12: aload_0
    //   13: aload 6
    //   15: invokevirtual 12	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:decompressor	(Lio/netty/handler/codec/http2/Http2Stream;)Lio/netty/handler/codec/http2/DelegatingDecompressorFrameListener$Http2Decompressor;
    //   18: astore 7
    //   20: aload 7
    //   22: ifnonnull +20 -> 42
    //   25: aload_0
    //   26: getfield 13	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:listener	Lio/netty/handler/codec/http2/Http2FrameListener;
    //   29: aload_1
    //   30: iload_2
    //   31: aload_3
    //   32: iload 4
    //   34: iload 5
    //   36: invokeinterface 14 6 0
    //   41: ireturn
    //   42: aload 7
    //   44: invokevirtual 15	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener$Http2Decompressor:decompressor	()Lio/netty/channel/embedded/EmbeddedChannel;
    //   47: astore 8
    //   49: aload_3
    //   50: invokevirtual 16	io/netty/buffer/ByteBuf:readableBytes	()I
    //   53: iload 4
    //   55: iadd
    //   56: istore 9
    //   58: aload 7
    //   60: iload 9
    //   62: invokevirtual 17	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener$Http2Decompressor:incrementCompressedBytes	(I)V
    //   65: aload 8
    //   67: iconst_1
    //   68: anewarray 18	java/lang/Object
    //   71: dup
    //   72: iconst_0
    //   73: aload_3
    //   74: invokevirtual 19	io/netty/buffer/ByteBuf:retain	()Lio/netty/buffer/ByteBuf;
    //   77: aastore
    //   78: invokevirtual 20	io/netty/channel/embedded/EmbeddedChannel:writeInbound	([Ljava/lang/Object;)Z
    //   81: pop
    //   82: aload 8
    //   84: invokestatic 21	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:nextReadableBuf	(Lio/netty/channel/embedded/EmbeddedChannel;)Lio/netty/buffer/ByteBuf;
    //   87: astore 10
    //   89: aload 10
    //   91: ifnonnull +23 -> 114
    //   94: iload 5
    //   96: ifeq +18 -> 114
    //   99: aload 8
    //   101: invokevirtual 22	io/netty/channel/embedded/EmbeddedChannel:finish	()Z
    //   104: ifeq +10 -> 114
    //   107: aload 8
    //   109: invokestatic 21	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:nextReadableBuf	(Lio/netty/channel/embedded/EmbeddedChannel;)Lio/netty/buffer/ByteBuf;
    //   112: astore 10
    //   114: aload 10
    //   116: ifnonnull +36 -> 152
    //   119: iload 5
    //   121: ifeq +21 -> 142
    //   124: aload_0
    //   125: getfield 13	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:listener	Lio/netty/handler/codec/http2/Http2FrameListener;
    //   128: aload_1
    //   129: iload_2
    //   130: getstatic 23	io/netty/buffer/Unpooled:EMPTY_BUFFER	Lio/netty/buffer/ByteBuf;
    //   133: iload 4
    //   135: iconst_1
    //   136: invokeinterface 14 6 0
    //   141: pop
    //   142: aload 7
    //   144: iload 9
    //   146: invokevirtual 24	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener$Http2Decompressor:incrementDecompressedBytes	(I)V
    //   149: iload 9
    //   151: ireturn
    //   152: aload_0
    //   153: getfield 4	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:connection	Lio/netty/handler/codec/http2/Http2Connection;
    //   156: invokeinterface 25 1 0
    //   161: invokeinterface 26 1 0
    //   166: checkcast 27	io/netty/handler/codec/http2/Http2LocalFlowController
    //   169: astore 11
    //   171: aload 7
    //   173: iload 4
    //   175: invokevirtual 24	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener$Http2Decompressor:incrementDecompressedBytes	(I)V
    //   178: aload 8
    //   180: invokestatic 21	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:nextReadableBuf	(Lio/netty/channel/embedded/EmbeddedChannel;)Lio/netty/buffer/ByteBuf;
    //   183: astore 12
    //   185: aload 12
    //   187: ifnonnull +12 -> 199
    //   190: iload 5
    //   192: ifeq +7 -> 199
    //   195: iconst_1
    //   196: goto +4 -> 200
    //   199: iconst_0
    //   200: istore 13
    //   202: iload 13
    //   204: ifeq +30 -> 234
    //   207: aload 8
    //   209: invokevirtual 22	io/netty/channel/embedded/EmbeddedChannel:finish	()Z
    //   212: ifeq +22 -> 234
    //   215: aload 8
    //   217: invokestatic 21	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:nextReadableBuf	(Lio/netty/channel/embedded/EmbeddedChannel;)Lio/netty/buffer/ByteBuf;
    //   220: astore 12
    //   222: aload 12
    //   224: ifnonnull +7 -> 231
    //   227: iconst_1
    //   228: goto +4 -> 232
    //   231: iconst_0
    //   232: istore 13
    //   234: aload 7
    //   236: aload 10
    //   238: invokevirtual 16	io/netty/buffer/ByteBuf:readableBytes	()I
    //   241: invokevirtual 24	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener$Http2Decompressor:incrementDecompressedBytes	(I)V
    //   244: aload 11
    //   246: aload 6
    //   248: aload_0
    //   249: getfield 13	io/netty/handler/codec/http2/DelegatingDecompressorFrameListener:listener	Lio/netty/handler/codec/http2/Http2FrameListener;
    //   252: aload_1
    //   253: iload_2
    //   254: aload 10
    //   256: iload 4
    //   258: iload 13
    //   260: invokeinterface 14 6 0
    //   265: invokeinterface 28 3 0
    //   270: pop
    //   271: aload 12
    //   273: ifnonnull +6 -> 279
    //   276: goto +19 -> 295
    //   279: iconst_0
    //   280: istore 4
    //   282: aload 10
    //   284: invokevirtual 29	io/netty/buffer/ByteBuf:release	()Z
    //   287: pop
    //   288: aload 12
    //   290: astore 10
    //   292: goto -114 -> 178
    //   295: iconst_0
    //   296: istore 12
    //   298: aload 10
    //   300: invokevirtual 29	io/netty/buffer/ByteBuf:release	()Z
    //   303: pop
    //   304: iload 12
    //   306: ireturn
    //   307: astore 14
    //   309: aload 10
    //   311: invokevirtual 29	io/netty/buffer/ByteBuf:release	()Z
    //   314: pop
    //   315: aload 14
    //   317: athrow
    //   318: astore 10
    //   320: aload 10
    //   322: athrow
    //   323: astore 10
    //   325: aload 6
    //   327: invokeinterface 32 1 0
    //   332: getstatic 33	io/netty/handler/codec/http2/Http2Error:INTERNAL_ERROR	Lio/netty/handler/codec/http2/Http2Error;
    //   335: aload 10
    //   337: ldc 34
    //   339: iconst_1
    //   340: anewarray 18	java/lang/Object
    //   343: dup
    //   344: iconst_0
    //   345: aload 6
    //   347: invokeinterface 32 1 0
    //   352: invokestatic 35	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   355: aastore
    //   356: invokestatic 36	io/netty/handler/codec/http2/Http2Exception:streamError	(ILio/netty/handler/codec/http2/Http2Error;Ljava/lang/Throwable;Ljava/lang/String;[Ljava/lang/Object;)Lio/netty/handler/codec/http2/Http2Exception;
    //   359: athrow
    // Line number table:
    //   Java source line #78	-> byte code offset #0
    //   Java source line #79	-> byte code offset #12
    //   Java source line #80	-> byte code offset #20
    //   Java source line #82	-> byte code offset #25
    //   Java source line #85	-> byte code offset #42
    //   Java source line #86	-> byte code offset #49
    //   Java source line #87	-> byte code offset #58
    //   Java source line #90	-> byte code offset #65
    //   Java source line #91	-> byte code offset #82
    //   Java source line #92	-> byte code offset #89
    //   Java source line #93	-> byte code offset #107
    //   Java source line #95	-> byte code offset #114
    //   Java source line #96	-> byte code offset #119
    //   Java source line #97	-> byte code offset #124
    //   Java source line #103	-> byte code offset #142
    //   Java source line #104	-> byte code offset #149
    //   Java source line #107	-> byte code offset #152
    //   Java source line #108	-> byte code offset #171
    //   Java source line #110	-> byte code offset #178
    //   Java source line #111	-> byte code offset #185
    //   Java source line #112	-> byte code offset #202
    //   Java source line #113	-> byte code offset #215
    //   Java source line #114	-> byte code offset #222
    //   Java source line #117	-> byte code offset #234
    //   Java source line #121	-> byte code offset #244
    //   Java source line #122	-> byte code offset #260
    //   Java source line #121	-> byte code offset #265
    //   Java source line #123	-> byte code offset #271
    //   Java source line #124	-> byte code offset #276
    //   Java source line #127	-> byte code offset #279
    //   Java source line #128	-> byte code offset #282
    //   Java source line #129	-> byte code offset #288
    //   Java source line #130	-> byte code offset #292
    //   Java source line #134	-> byte code offset #295
    //   Java source line #136	-> byte code offset #298
    //   Java source line #134	-> byte code offset #304
    //   Java source line #136	-> byte code offset #307
    //   Java source line #137	-> byte code offset #315
    //   Java source line #138	-> byte code offset #318
    //   Java source line #139	-> byte code offset #320
    //   Java source line #140	-> byte code offset #323
    //   Java source line #141	-> byte code offset #325
    //   Java source line #142	-> byte code offset #347
    //   Java source line #141	-> byte code offset #356
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	360	0	this	DelegatingDecompressorFrameListener
    //   0	360	1	ctx	ChannelHandlerContext
    //   0	360	2	streamId	int
    //   0	360	3	data	ByteBuf
    //   0	360	4	padding	int
    //   0	360	5	endOfStream	boolean
    //   10	336	6	stream	Http2Stream
    //   18	217	7	decompressor	Http2Decompressor
    //   47	169	8	channel	EmbeddedChannel
    //   56	94	9	compressedBytes	int
    //   87	223	10	buf	ByteBuf
    //   318	3	10	e	Http2Exception
    //   323	13	10	t	Throwable
    //   169	76	11	flowController	Http2LocalFlowController
    //   183	122	12	nextBuf	ByteBuf
    //   200	59	13	decompressedEndOfStream	boolean
    //   307	9	14	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   152	298	307	finally
    //   307	309	307	finally
    //   65	151	318	io/netty/handler/codec/http2/Http2Exception
    //   152	304	318	io/netty/handler/codec/http2/Http2Exception
    //   307	318	318	io/netty/handler/codec/http2/Http2Exception
    //   65	151	323	java/lang/Throwable
    //   152	304	323	java/lang/Throwable
    //   307	318	323	java/lang/Throwable
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream)
    throws Http2Exception
  {
    initDecompressor(ctx, streamId, headers, endStream);
    listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream)
    throws Http2Exception
  {
    initDecompressor(ctx, streamId, headers, endStream);
    listener.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
  }
  








  protected EmbeddedChannel newContentDecompressor(ChannelHandlerContext ctx, CharSequence contentEncoding)
    throws Http2Exception
  {
    if ((HttpHeaderValues.GZIP.contentEqualsIgnoreCase(contentEncoding)) || (HttpHeaderValues.X_GZIP.contentEqualsIgnoreCase(contentEncoding))) {
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
        .channel().config(), new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP) });
    }
    if ((HttpHeaderValues.DEFLATE.contentEqualsIgnoreCase(contentEncoding)) || (HttpHeaderValues.X_DEFLATE.contentEqualsIgnoreCase(contentEncoding))) {
      ZlibWrapper wrapper = strict ? ZlibWrapper.ZLIB : ZlibWrapper.ZLIB_OR_NONE;
      
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
        .channel().config(), new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(wrapper) });
    }
    if ((Brotli.isAvailable()) && (HttpHeaderValues.BR.contentEqualsIgnoreCase(contentEncoding))) {
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(), ctx
        .channel().config(), new ChannelHandler[] { new BrotliDecoder() });
    }
    
    return null;
  }
  







  protected CharSequence getTargetContentEncoding(CharSequence contentEncoding)
    throws Http2Exception
  {
    return HttpHeaderValues.IDENTITY;
  }
  









  private void initDecompressor(ChannelHandlerContext ctx, int streamId, Http2Headers headers, boolean endOfStream)
    throws Http2Exception
  {
    Http2Stream stream = connection.stream(streamId);
    if (stream == null) {
      return;
    }
    
    Http2Decompressor decompressor = decompressor(stream);
    if ((decompressor == null) && (!endOfStream))
    {
      CharSequence contentEncoding = (CharSequence)headers.get(HttpHeaderNames.CONTENT_ENCODING);
      if (contentEncoding == null) {
        contentEncoding = HttpHeaderValues.IDENTITY;
      }
      EmbeddedChannel channel = newContentDecompressor(ctx, contentEncoding);
      if (channel != null) {
        decompressor = new Http2Decompressor(channel);
        stream.setProperty(propertyKey, decompressor);
        

        CharSequence targetContentEncoding = getTargetContentEncoding(contentEncoding);
        if (HttpHeaderValues.IDENTITY.contentEqualsIgnoreCase(targetContentEncoding)) {
          headers.remove(HttpHeaderNames.CONTENT_ENCODING);
        } else {
          headers.set(HttpHeaderNames.CONTENT_ENCODING, targetContentEncoding);
        }
      }
    }
    
    if (decompressor != null)
    {


      headers.remove(HttpHeaderNames.CONTENT_LENGTH);
      


      if (!flowControllerInitialized) {
        flowControllerInitialized = true;
        connection.local().flowController(new ConsumedBytesConverter((Http2LocalFlowController)connection.local().flowController()));
      }
    }
  }
  
  Http2Decompressor decompressor(Http2Stream stream) {
    return stream == null ? null : (Http2Decompressor)stream.getProperty(propertyKey);
  }
  




  private static void cleanup(Http2Decompressor decompressor)
  {
    decompressor.decompressor().finishAndReleaseAll();
  }
  


  private static ByteBuf nextReadableBuf(EmbeddedChannel decompressor)
  {
    ByteBuf buf;
    

    for (;;)
    {
      buf = (ByteBuf)decompressor.readInbound();
      if (buf == null) {
        return null;
      }
      if (buf.isReadable()) break;
      buf.release();
    }
    
    return buf;
  }
  

  private final class ConsumedBytesConverter
    implements Http2LocalFlowController
  {
    private final Http2LocalFlowController flowController;
    
    ConsumedBytesConverter(Http2LocalFlowController flowController)
    {
      this.flowController = ((Http2LocalFlowController)ObjectUtil.checkNotNull(flowController, "flowController"));
    }
    
    public Http2LocalFlowController frameWriter(Http2FrameWriter frameWriter)
    {
      return flowController.frameWriter(frameWriter);
    }
    
    public void channelHandlerContext(ChannelHandlerContext ctx) throws Http2Exception
    {
      flowController.channelHandlerContext(ctx);
    }
    
    public void initialWindowSize(int newWindowSize) throws Http2Exception
    {
      flowController.initialWindowSize(newWindowSize);
    }
    
    public int initialWindowSize()
    {
      return flowController.initialWindowSize();
    }
    
    public int windowSize(Http2Stream stream)
    {
      return flowController.windowSize(stream);
    }
    
    public void incrementWindowSize(Http2Stream stream, int delta) throws Http2Exception
    {
      flowController.incrementWindowSize(stream, delta);
    }
    
    public void receiveFlowControlledFrame(Http2Stream stream, ByteBuf data, int padding, boolean endOfStream)
      throws Http2Exception
    {
      flowController.receiveFlowControlledFrame(stream, data, padding, endOfStream);
    }
    
    public boolean consumeBytes(Http2Stream stream, int numBytes) throws Http2Exception
    {
      DelegatingDecompressorFrameListener.Http2Decompressor decompressor = decompressor(stream);
      if (decompressor != null)
      {
        numBytes = decompressor.consumeBytes(stream.id(), numBytes);
      }
      try {
        return flowController.consumeBytes(stream, numBytes);
      } catch (Http2Exception e) {
        throw e;
      }
      catch (Throwable t)
      {
        throw Http2Exception.streamError(stream.id(), Http2Error.INTERNAL_ERROR, t, "Error while returning bytes to flow control window", new Object[0]);
      }
    }
    
    public int unconsumedBytes(Http2Stream stream)
    {
      return flowController.unconsumedBytes(stream);
    }
    
    public int initialWindowSize(Http2Stream stream)
    {
      return flowController.initialWindowSize(stream);
    }
  }
  

  private static final class Http2Decompressor
  {
    private final EmbeddedChannel decompressor;
    private int compressed;
    private int decompressed;
    
    Http2Decompressor(EmbeddedChannel decompressor)
    {
      this.decompressor = decompressor;
    }
    


    EmbeddedChannel decompressor()
    {
      return decompressor;
    }
    


    void incrementCompressedBytes(int delta)
    {
      assert (delta >= 0);
      compressed += delta;
    }
    


    void incrementDecompressedBytes(int delta)
    {
      assert (delta >= 0);
      decompressed += delta;
    }
    






    int consumeBytes(int streamId, int decompressedBytes)
      throws Http2Exception
    {
      ObjectUtil.checkPositiveOrZero(decompressedBytes, "decompressedBytes");
      if (decompressed - decompressedBytes < 0) {
        throw Http2Exception.streamError(streamId, Http2Error.INTERNAL_ERROR, "Attempting to return too many bytes for stream %d. decompressed: %d decompressedBytes: %d", new Object[] {
        
          Integer.valueOf(streamId), Integer.valueOf(decompressed), Integer.valueOf(decompressedBytes) });
      }
      double consumedRatio = decompressedBytes / decompressed;
      int consumedCompressed = Math.min(compressed, (int)Math.ceil(compressed * consumedRatio));
      if (compressed - consumedCompressed < 0) {
        throw Http2Exception.streamError(streamId, Http2Error.INTERNAL_ERROR, "overflow when converting decompressed bytes to compressed bytes for stream %d.decompressedBytes: %d decompressed: %d compressed: %d consumedCompressed: %d", new Object[] {
        

          Integer.valueOf(streamId), Integer.valueOf(decompressedBytes), Integer.valueOf(decompressed), Integer.valueOf(compressed), Integer.valueOf(consumedCompressed) });
      }
      decompressed -= decompressedBytes;
      compressed -= consumedCompressed;
      
      return consumedCompressed;
    }
  }
}
