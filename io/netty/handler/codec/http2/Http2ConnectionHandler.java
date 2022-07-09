package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;








































public class Http2ConnectionHandler
  extends ByteToMessageDecoder
  implements Http2LifecycleManager, ChannelOutboundHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(Http2ConnectionHandler.class);
  
  private static final Http2Headers HEADERS_TOO_LARGE_HEADERS = ReadOnlyHttp2Headers.serverHeaders(false, HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
    .codeAsText(), new AsciiString[0]);
  private static final ByteBuf HTTP_1_X_BUF = Unpooled.unreleasableBuffer(
    Unpooled.wrappedBuffer(new byte[] { 72, 84, 84, 80, 47, 49, 46 })).asReadOnly();
  
  private final Http2ConnectionDecoder decoder;
  private final Http2ConnectionEncoder encoder;
  private final Http2Settings initialSettings;
  private final boolean decoupleCloseAndGoAway;
  private ChannelFutureListener closeListener;
  private BaseDecoder byteDecoder;
  private long gracefulShutdownTimeoutMillis;
  
  protected Http2ConnectionHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings)
  {
    this(decoder, encoder, initialSettings, false);
  }
  
  protected Http2ConnectionHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings, boolean decoupleCloseAndGoAway)
  {
    this.initialSettings = ((Http2Settings)ObjectUtil.checkNotNull(initialSettings, "initialSettings"));
    this.decoder = ((Http2ConnectionDecoder)ObjectUtil.checkNotNull(decoder, "decoder"));
    this.encoder = ((Http2ConnectionEncoder)ObjectUtil.checkNotNull(encoder, "encoder"));
    this.decoupleCloseAndGoAway = decoupleCloseAndGoAway;
    if (encoder.connection() != decoder.connection()) {
      throw new IllegalArgumentException("Encoder and Decoder do not share the same connection object");
    }
  }
  




  public long gracefulShutdownTimeoutMillis()
  {
    return gracefulShutdownTimeoutMillis;
  }
  





  public void gracefulShutdownTimeoutMillis(long gracefulShutdownTimeoutMillis)
  {
    if (gracefulShutdownTimeoutMillis < -1L) {
      throw new IllegalArgumentException("gracefulShutdownTimeoutMillis: " + gracefulShutdownTimeoutMillis + " (expected: -1 for indefinite or >= 0)");
    }
    
    this.gracefulShutdownTimeoutMillis = gracefulShutdownTimeoutMillis;
  }
  
  public Http2Connection connection() {
    return encoder.connection();
  }
  
  public Http2ConnectionDecoder decoder() {
    return decoder;
  }
  
  public Http2ConnectionEncoder encoder() {
    return encoder;
  }
  
  private boolean prefaceSent() {
    return (byteDecoder != null) && (byteDecoder.prefaceSent());
  }
  


  public void onHttpClientUpgrade()
    throws Http2Exception
  {
    if (connection().isServer()) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Client-side HTTP upgrade requested for a server", new Object[0]);
    }
    if (!prefaceSent())
    {

      throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, "HTTP upgrade must occur after preface was sent", new Object[0]);
    }
    if (decoder.prefaceReceived()) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is received", new Object[0]);
    }
    

    connection().local().createStream(1, true);
  }
  


  public void onHttpServerUpgrade(Http2Settings settings)
    throws Http2Exception
  {
    if (!connection().isServer()) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server-side HTTP upgrade requested for a client", new Object[0]);
    }
    if (!prefaceSent())
    {

      throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, "HTTP upgrade must occur after preface was sent", new Object[0]);
    }
    if (decoder.prefaceReceived()) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is received", new Object[0]);
    }
    

    encoder.remoteSettings(settings);
    

    connection().remote().createStream(1, true);
  }
  
  public void flush(ChannelHandlerContext ctx)
  {
    try
    {
      encoder.flowController().writePendingBytes();
      ctx.flush();
    } catch (Http2Exception e) {
      onError(ctx, true, e);
    } catch (Throwable cause) {
      onError(ctx, true, Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, cause, "Error flushing", new Object[0])); } }
  
  private abstract class BaseDecoder { private BaseDecoder() {}
    
    public abstract void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList) throws Exception;
    
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {}
    
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {}
    public void channelInactive(ChannelHandlerContext ctx) throws Exception { encoder().close();
      decoder().close();
      


      connection().close(ctx.voidPromise());
    }
    




    public boolean prefaceSent() { return true; }
  }
  
  private final class PrefaceDecoder extends Http2ConnectionHandler.BaseDecoder {
    private ByteBuf clientPrefaceString;
    private boolean prefaceSent;
    
    PrefaceDecoder(ChannelHandlerContext ctx) throws Exception {
      super(null);
      clientPrefaceString = Http2ConnectionHandler.clientPrefaceString(encoder.connection());
      

      sendPreface(ctx);
    }
    
    public boolean prefaceSent()
    {
      return prefaceSent;
    }
    
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
      try {
        if ((ctx.channel().isActive()) && (readClientPrefaceString(in)) && (verifyFirstFrameIsSettings(in)))
        {
          byteDecoder = new Http2ConnectionHandler.FrameDecoder(Http2ConnectionHandler.this, null);
          byteDecoder.decode(ctx, in, out);
        }
      } catch (Throwable e) {
        onError(ctx, false, e);
      }
    }
    
    public void channelActive(ChannelHandlerContext ctx)
      throws Exception
    {
      sendPreface(ctx);
    }
    
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
      cleanup();
      super.channelInactive(ctx);
    }
    


    public void handlerRemoved(ChannelHandlerContext ctx)
      throws Exception
    {
      cleanup();
    }
    


    private void cleanup()
    {
      if (clientPrefaceString != null) {
        clientPrefaceString.release();
        clientPrefaceString = null;
      }
    }
    




    private boolean readClientPrefaceString(ByteBuf in)
      throws Http2Exception
    {
      if (clientPrefaceString == null) {
        return true;
      }
      
      int prefaceRemaining = clientPrefaceString.readableBytes();
      int bytesRead = Math.min(in.readableBytes(), prefaceRemaining);
      

      if ((bytesRead == 0) || (!ByteBufUtil.equals(in, in.readerIndex(), clientPrefaceString, clientPrefaceString
        .readerIndex(), bytesRead)))
      {
        int maxSearch = 1024;
        
        int http1Index = ByteBufUtil.indexOf(Http2ConnectionHandler.HTTP_1_X_BUF, in.slice(in.readerIndex(), Math.min(in.readableBytes(), maxSearch)));
        if (http1Index != -1) {
          String chunk = in.toString(in.readerIndex(), http1Index - in.readerIndex(), CharsetUtil.US_ASCII);
          throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Unexpected HTTP/1.x request: %s", new Object[] { chunk });
        }
        String receivedBytes = ByteBufUtil.hexDump(in, in.readerIndex(), 
          Math.min(in.readableBytes(), clientPrefaceString.readableBytes()));
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP/2 client preface string missing or corrupt. Hex dump for received bytes: %s", new Object[] { receivedBytes });
      }
      
      in.skipBytes(bytesRead);
      clientPrefaceString.skipBytes(bytesRead);
      
      if (!clientPrefaceString.isReadable())
      {
        clientPrefaceString.release();
        clientPrefaceString = null;
        return true;
      }
      return false;
    }
    






    private boolean verifyFirstFrameIsSettings(ByteBuf in)
      throws Http2Exception
    {
      if (in.readableBytes() < 5)
      {
        return false;
      }
      
      short frameType = in.getUnsignedByte(in.readerIndex() + 3);
      short flags = in.getUnsignedByte(in.readerIndex() + 4);
      if ((frameType != 4) || ((flags & 0x1) != 0)) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "First received frame was not SETTINGS. Hex dump for first 5 bytes: %s", new Object[] {
        
          ByteBufUtil.hexDump(in, in.readerIndex(), 5) });
      }
      return true;
    }
    

    private void sendPreface(ChannelHandlerContext ctx)
      throws Exception
    {
      if ((prefaceSent) || (!ctx.channel().isActive())) {
        return;
      }
      
      prefaceSent = true;
      
      boolean isClient = !connection().isServer();
      if (isClient)
      {
        ctx.write(Http2CodecUtil.connectionPrefaceBuf()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      }
      

      encoder.writeSettings(ctx, initialSettings, ctx.newPromise()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      

      if (isClient)
      {


        userEventTriggered(ctx, Http2ConnectionPrefaceAndSettingsFrameWrittenEvent.INSTANCE); }
    }
  }
  
  private final class FrameDecoder extends Http2ConnectionHandler.BaseDecoder {
    private FrameDecoder() { super(null); }
    
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
        decoder.decodeFrame(ctx, in, out);
      } catch (Throwable e) {
        onError(ctx, false, e);
      }
    }
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    encoder.lifecycleManager(this);
    decoder.lifecycleManager(this);
    encoder.flowController().channelHandlerContext(ctx);
    decoder.flowController().channelHandlerContext(ctx);
    byteDecoder = new PrefaceDecoder(ctx);
  }
  
  protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception
  {
    if (byteDecoder != null) {
      byteDecoder.handlerRemoved(ctx);
      byteDecoder = null;
    }
  }
  
  public void channelActive(ChannelHandlerContext ctx) throws Exception
  {
    if (byteDecoder == null) {
      byteDecoder = new PrefaceDecoder(ctx);
    }
    byteDecoder.channelActive(ctx);
    super.channelActive(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    super.channelInactive(ctx);
    if (byteDecoder != null) {
      byteDecoder.channelInactive(ctx);
      byteDecoder = null;
    }
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx)
    throws Exception
  {
    try
    {
      if (ctx.channel().isWritable()) {
        flush(ctx);
      }
      encoder.flowController().channelWritabilityChanged();
      
      super.channelWritabilityChanged(ctx); } finally { super.channelWritabilityChanged(ctx);
    }
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
  {
    byteDecoder.decode(ctx, in, out);
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.disconnect(promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    if (decoupleCloseAndGoAway) {
      ctx.close(promise);
      return;
    }
    promise = promise.unvoid();
    
    if ((!ctx.channel().isActive()) || (!prefaceSent())) {
      ctx.close(promise);
      return;
    }
    





    ChannelFuture f = connection().goAwaySent() ? ctx.write(Unpooled.EMPTY_BUFFER) : goAway(ctx, null, ctx.newPromise());
    ctx.flush();
    doGracefulShutdown(ctx, f, promise);
  }
  
  private ChannelFutureListener newClosingChannelFutureListener(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    long gracefulShutdownTimeoutMillis = this.gracefulShutdownTimeoutMillis;
    return gracefulShutdownTimeoutMillis < 0L ? new ClosingChannelFutureListener(ctx, promise) : new ClosingChannelFutureListener(ctx, promise, gracefulShutdownTimeoutMillis, TimeUnit.MILLISECONDS);
  }
  

  private void doGracefulShutdown(ChannelHandlerContext ctx, ChannelFuture future, ChannelPromise promise)
  {
    final ChannelFutureListener listener = newClosingChannelFutureListener(ctx, promise);
    if (isGracefulShutdownComplete())
    {

      future.addListener(listener);




    }
    else if (closeListener == null) {
      closeListener = listener;
    } else if (promise != null) {
      final ChannelFutureListener oldCloseListener = closeListener;
      closeListener = new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) throws Exception {
          try {
            oldCloseListener.operationComplete(future);
            
            listener.operationComplete(future); } finally { listener.operationComplete(future);
          }
        }
      };
    }
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.deregister(promise);
  }
  
  public void read(ChannelHandlerContext ctx) throws Exception
  {
    ctx.read();
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    ctx.write(msg, promise);
  }
  

  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    try
    {
      channelReadComplete0(ctx);
      
      flush(ctx); } finally { flush(ctx);
    }
  }
  
  final void channelReadComplete0(ChannelHandlerContext ctx)
  {
    discardSomeReadBytes();
    



    if (!ctx.channel().config().isAutoRead()) {
      ctx.read();
    }
    
    ctx.fireChannelReadComplete();
  }
  


  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    if (Http2CodecUtil.getEmbeddedHttp2Exception(cause) != null)
    {
      onError(ctx, false, cause);
    } else {
      super.exceptionCaught(ctx, cause);
    }
  }
  







  public void closeStreamLocal(Http2Stream stream, ChannelFuture future)
  {
    switch (6.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()]) {
    case 1: 
    case 2: 
      stream.closeLocalSide();
      break;
    default: 
      closeStream(stream, future);
    }
    
  }
  







  public void closeStreamRemote(Http2Stream stream, ChannelFuture future)
  {
    switch (6.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()]) {
    case 2: 
    case 3: 
      stream.closeRemoteSide();
      break;
    default: 
      closeStream(stream, future);
    }
    
  }
  
  public void closeStream(Http2Stream stream, ChannelFuture future)
  {
    stream.close();
    
    if (future.isDone()) {
      checkCloseConnection(future);
    } else {
      future.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) throws Exception {
          Http2ConnectionHandler.this.checkCloseConnection(future);
        }
      });
    }
  }
  



  public void onError(ChannelHandlerContext ctx, boolean outbound, Throwable cause)
  {
    Http2Exception embedded = Http2CodecUtil.getEmbeddedHttp2Exception(cause);
    if (Http2Exception.isStreamError(embedded)) {
      onStreamError(ctx, outbound, cause, (Http2Exception.StreamException)embedded);
    } else if ((embedded instanceof Http2Exception.CompositeStreamException)) {
      Http2Exception.CompositeStreamException compositException = (Http2Exception.CompositeStreamException)embedded;
      for (Http2Exception.StreamException streamException : compositException) {
        onStreamError(ctx, outbound, cause, streamException);
      }
    } else {
      onConnectionError(ctx, outbound, cause, embedded);
    }
    ctx.flush();
  }
  




  protected boolean isGracefulShutdownComplete()
  {
    return connection().numActiveStreams() == 0;
  }
  










  protected void onConnectionError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception http2Ex)
  {
    if (http2Ex == null) {
      http2Ex = new Http2Exception(Http2Error.INTERNAL_ERROR, cause.getMessage(), cause);
    }
    
    ChannelPromise promise = ctx.newPromise();
    ChannelFuture future = goAway(ctx, http2Ex, ctx.newPromise());
    if (http2Ex.shutdownHint() == Http2Exception.ShutdownHint.GRACEFUL_SHUTDOWN) {
      doGracefulShutdown(ctx, future, promise);
    } else {
      future.addListener(newClosingChannelFutureListener(ctx, promise));
    }
  }
  









  protected void onStreamError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception.StreamException http2Ex)
  {
    int streamId = http2Ex.streamId();
    Http2Stream stream = connection().stream(streamId);
    

    if (((http2Ex instanceof Http2Exception.HeaderListSizeException)) && 
      (((Http2Exception.HeaderListSizeException)http2Ex).duringDecode()) && 
      (connection().isServer()))
    {






      if (stream == null) {
        try {
          stream = encoder.connection().remote().createStream(streamId, true);
        } catch (Http2Exception e) {
          resetUnknownStream(ctx, streamId, http2Ex.error().code(), ctx.newPromise());
          return;
        }
      }
      

      if ((stream != null) && (!stream.isHeadersSent())) {
        try {
          handleServerHeaderDecodeSizeError(ctx, stream);
        } catch (Throwable cause2) {
          onError(ctx, outbound, Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, cause2, "Error DecodeSizeError", new Object[0]));
        }
      }
    }
    
    if (stream == null) {
      if ((!outbound) || (connection().local().mayHaveCreatedStream(streamId))) {
        resetUnknownStream(ctx, streamId, http2Ex.error().code(), ctx.newPromise());
      }
    } else {
      resetStream(ctx, stream, http2Ex.error().code(), ctx.newPromise());
    }
  }
  






  protected void handleServerHeaderDecodeSizeError(ChannelHandlerContext ctx, Http2Stream stream)
  {
    encoder().writeHeaders(ctx, stream.id(), HEADERS_TOO_LARGE_HEADERS, 0, true, ctx.newPromise());
  }
  
  protected Http2FrameWriter frameWriter() {
    return encoder().frameWriter();
  }
  





  private ChannelFuture resetUnknownStream(final ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    ChannelFuture future = frameWriter().writeRstStream(ctx, streamId, errorCode, promise);
    if (future.isDone()) {
      closeConnectionOnError(ctx, future);
    } else {
      future.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) throws Exception {
          Http2ConnectionHandler.this.closeConnectionOnError(ctx, future);
        }
      });
    }
    return future;
  }
  

  public ChannelFuture resetStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    Http2Stream stream = connection().stream(streamId);
    if (stream == null) {
      return resetUnknownStream(ctx, streamId, errorCode, promise.unvoid());
    }
    
    return resetStream(ctx, stream, errorCode, promise);
  }
  
  private ChannelFuture resetStream(final ChannelHandlerContext ctx, final Http2Stream stream, long errorCode, ChannelPromise promise)
  {
    promise = promise.unvoid();
    if (stream.isResetSent())
    {
      return promise.setSuccess();
    }
    




    stream.resetSent();
    
    ChannelFuture future;
    
    ChannelFuture future;
    if ((stream.state() == Http2Stream.State.IDLE) || (
      (connection().local().created(stream)) && (!stream.isHeadersSent()) && (!stream.isPushPromiseSent()))) {
      future = promise.setSuccess();
    } else {
      future = frameWriter().writeRstStream(ctx, stream.id(), errorCode, promise);
    }
    if (future.isDone()) {
      processRstStreamWriteResult(ctx, stream, future);
    } else {
      future.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) throws Exception {
          Http2ConnectionHandler.this.processRstStreamWriteResult(ctx, stream, future);
        }
      });
    }
    
    return future;
  }
  

  public ChannelFuture goAway(final ChannelHandlerContext ctx, final int lastStreamId, final long errorCode, ByteBuf debugData, ChannelPromise promise)
  {
    promise = promise.unvoid();
    Http2Connection connection = connection();
    try {
      if (!connection.goAwaySent(lastStreamId, errorCode, debugData)) {
        debugData.release();
        promise.trySuccess();
        return promise;
      }
    } catch (Throwable cause) {
      debugData.release();
      promise.tryFailure(cause);
      return promise;
    }
    


    debugData.retain();
    ChannelFuture future = frameWriter().writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
    
    if (future.isDone()) {
      processGoAwayWriteResult(ctx, lastStreamId, errorCode, debugData, future);
    } else {
      future.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) throws Exception {
          Http2ConnectionHandler.processGoAwayWriteResult(ctx, lastStreamId, errorCode, val$debugData, future);
        }
      });
    }
    
    return future;
  }
  





  private void checkCloseConnection(ChannelFuture future)
  {
    if ((this.closeListener != null) && (isGracefulShutdownComplete())) {
      ChannelFutureListener closeListener = this.closeListener;
      

      this.closeListener = null;
      try {
        closeListener.operationComplete(future);
      } catch (Exception e) {
        throw new IllegalStateException("Close listener threw an unexpected exception", e);
      }
    }
  }
  



  private ChannelFuture goAway(ChannelHandlerContext ctx, Http2Exception cause, ChannelPromise promise)
  {
    long errorCode = cause != null ? cause.error().code() : Http2Error.NO_ERROR.code();
    int lastKnownStream;
    int lastKnownStream; if ((cause != null) && (cause.shutdownHint() == Http2Exception.ShutdownHint.HARD_SHUTDOWN))
    {



      lastKnownStream = Integer.MAX_VALUE;
    } else {
      lastKnownStream = connection().remote().lastStreamCreated();
    }
    return goAway(ctx, lastKnownStream, errorCode, Http2CodecUtil.toByteBuf(ctx, cause), promise);
  }
  
  private void processRstStreamWriteResult(ChannelHandlerContext ctx, Http2Stream stream, ChannelFuture future) {
    if (future.isSuccess()) {
      closeStream(stream, future);
    }
    else {
      onConnectionError(ctx, true, future.cause(), null);
    }
  }
  
  private void closeConnectionOnError(ChannelHandlerContext ctx, ChannelFuture future) {
    if (!future.isSuccess()) {
      onConnectionError(ctx, true, future.cause(), null);
    }
  }
  


  private static ByteBuf clientPrefaceString(Http2Connection connection)
  {
    return connection.isServer() ? Http2CodecUtil.connectionPrefaceBuf() : null;
  }
  
  private static void processGoAwayWriteResult(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData, ChannelFuture future)
  {
    try {
      if (future.isSuccess()) {
        if (errorCode != Http2Error.NO_ERROR.code()) {
          if (logger.isDebugEnabled()) {
            logger.debug("{} Sent GOAWAY: lastStreamId '{}', errorCode '{}', debugData '{}'. Forcing shutdown of the connection.", new Object[] {ctx
            
              .channel(), Integer.valueOf(lastStreamId), Long.valueOf(errorCode), debugData.toString(CharsetUtil.UTF_8), future.cause() });
          }
          ctx.close();
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("{} Sending GOAWAY failed: lastStreamId '{}', errorCode '{}', debugData '{}'. Forcing shutdown of the connection.", new Object[] {ctx
          
            .channel(), Integer.valueOf(lastStreamId), Long.valueOf(errorCode), debugData.toString(CharsetUtil.UTF_8), future.cause() });
        }
        ctx.close();
      }
    }
    finally {
      debugData.release();
    }
  }
  
  private static final class ClosingChannelFutureListener
    implements ChannelFutureListener
  {
    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise;
    private final ScheduledFuture<?> timeoutTask;
    private boolean closed;
    
    ClosingChannelFutureListener(ChannelHandlerContext ctx, ChannelPromise promise)
    {
      this.ctx = ctx;
      this.promise = promise;
      timeoutTask = null;
    }
    
    ClosingChannelFutureListener(ChannelHandlerContext ctx, ChannelPromise promise, long timeout, TimeUnit unit)
    {
      this.ctx = ctx;
      this.promise = promise;
      timeoutTask = ctx.executor().schedule(new Runnable()
      {

        public void run() { Http2ConnectionHandler.ClosingChannelFutureListener.this.doClose(); } }, timeout, unit);
    }
    


    public void operationComplete(ChannelFuture sentGoAwayFuture)
    {
      if (timeoutTask != null) {
        timeoutTask.cancel(false);
      }
      doClose();
    }
    

    private void doClose()
    {
      if (closed)
      {
        assert (timeoutTask != null);
        return;
      }
      closed = true;
      if (promise == null) {
        ctx.close();
      } else {
        ctx.close(promise);
      }
    }
  }
}
