package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;






























































































































public class Http2FrameCodec
  extends Http2ConnectionHandler
{
  private static final InternalLogger LOG = InternalLoggerFactory.getInstance(Http2FrameCodec.class);
  

  protected final Http2Connection.PropertyKey streamKey;
  
  private final Http2Connection.PropertyKey upgradeKey;
  
  private final Integer initialFlowControlWindowSize;
  
  ChannelHandlerContext ctx;
  
  private int numBufferedStreams;
  
  private final IntObjectMap<DefaultHttp2FrameStream> frameStreamToInitializeMap = new IntObjectHashMap(8);
  

  Http2FrameCodec(Http2ConnectionEncoder encoder, Http2ConnectionDecoder decoder, Http2Settings initialSettings, boolean decoupleCloseAndGoAway)
  {
    super(decoder, encoder, initialSettings, decoupleCloseAndGoAway);
    
    decoder.frameListener(new FrameListener(null));
    connection().addListener(new ConnectionListener(null));
    ((Http2RemoteFlowController)connection().remote().flowController()).listener(new Http2RemoteFlowControllerListener(null));
    streamKey = connection().newKey();
    upgradeKey = connection().newKey();
    initialFlowControlWindowSize = initialSettings.initialWindowSize();
  }
  


  DefaultHttp2FrameStream newStream()
  {
    return new DefaultHttp2FrameStream();
  }
  



  final void forEachActiveStream(final Http2FrameStreamVisitor streamVisitor)
    throws Http2Exception
  {
    assert (ctx.executor().inEventLoop());
    if (connection().numActiveStreams() > 0) {
      connection().forEachActiveStream(new Http2StreamVisitor()
      {
        public boolean visit(Http2Stream stream) {
          try {
            return streamVisitor.visit((Http2FrameStream)stream.getProperty(streamKey));
          } catch (Throwable cause) {
            onError(ctx, false, cause); }
          return false;
        }
      });
    }
  }
  





  int numInitializingStreams()
  {
    return frameStreamToInitializeMap.size();
  }
  
  public final void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
    super.handlerAdded(ctx);
    handlerAdded0(ctx);
    

    Http2Connection connection = connection();
    if (connection.isServer()) {
      tryExpandConnectionFlowControlWindow(connection);
    }
  }
  
  private void tryExpandConnectionFlowControlWindow(Http2Connection connection) throws Http2Exception {
    if (initialFlowControlWindowSize != null)
    {

      Http2Stream connectionStream = connection.connectionStream();
      Http2LocalFlowController localFlowController = (Http2LocalFlowController)connection.local().flowController();
      int delta = initialFlowControlWindowSize.intValue() - localFlowController.initialWindowSize(connectionStream);
      
      if (delta > 0)
      {
        localFlowController.incrementWindowSize(connectionStream, Math.max(delta << 1, delta));
        flush(ctx);
      }
    }
  }
  


  void handlerAdded0(ChannelHandlerContext ctx)
    throws Exception
  {}
  

  public final void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
    throws Exception
  {
    if (evt == Http2ConnectionPrefaceAndSettingsFrameWrittenEvent.INSTANCE)
    {
      tryExpandConnectionFlowControlWindow(connection());
      



      ctx.executor().execute(new Runnable()
      {
        public void run() {
          ctx.fireUserEventTriggered(evt);
        }
      });
    } else if ((evt instanceof HttpServerUpgradeHandler.UpgradeEvent)) {
      HttpServerUpgradeHandler.UpgradeEvent upgrade = (HttpServerUpgradeHandler.UpgradeEvent)evt;
      try {
        onUpgradeEvent(ctx, upgrade.retain());
        Http2Stream stream = connection().stream(1);
        if (stream.getProperty(streamKey) == null)
        {


          onStreamActive0(stream);
        }
        upgrade.upgradeRequest().headers().setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID
          .text(), 1);
        stream.setProperty(upgradeKey, Boolean.valueOf(true));
        InboundHttpToHttp2Adapter.handle(ctx, 
          connection(), decoder().frameListener(), upgrade.upgradeRequest().retain());
      } finally {
        upgrade.release();
      }
    } else {
      ctx.fireUserEventTriggered(evt);
    }
  }
  




  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    if ((msg instanceof Http2DataFrame)) {
      Http2DataFrame dataFrame = (Http2DataFrame)msg;
      encoder().writeData(ctx, dataFrame.stream().id(), dataFrame.content(), dataFrame
        .padding(), dataFrame.isEndStream(), promise);
    } else if ((msg instanceof Http2HeadersFrame)) {
      writeHeadersFrame(ctx, (Http2HeadersFrame)msg, promise);
    } else if ((msg instanceof Http2WindowUpdateFrame)) {
      Http2WindowUpdateFrame frame = (Http2WindowUpdateFrame)msg;
      Http2FrameStream frameStream = frame.stream();
      
      try
      {
        if (frameStream == null) {
          increaseInitialConnectionWindow(frame.windowSizeIncrement());
        } else {
          consumeBytes(frameStream.id(), frame.windowSizeIncrement());
        }
        promise.setSuccess();
      } catch (Throwable t) {
        promise.setFailure(t);
      }
    } else if ((msg instanceof Http2ResetFrame)) {
      Http2ResetFrame rstFrame = (Http2ResetFrame)msg;
      int id = rstFrame.stream().id();
      

      if (connection().streamMayHaveExisted(id)) {
        encoder().writeRstStream(ctx, rstFrame.stream().id(), rstFrame.errorCode(), promise);
      } else {
        ReferenceCountUtil.release(rstFrame);
        promise.setFailure(Http2Exception.streamError(rstFrame
          .stream().id(), Http2Error.PROTOCOL_ERROR, "Stream never existed", new Object[0]));
      }
    } else if ((msg instanceof Http2PingFrame)) {
      Http2PingFrame frame = (Http2PingFrame)msg;
      encoder().writePing(ctx, frame.ack(), frame.content(), promise);
    } else if ((msg instanceof Http2SettingsFrame)) {
      encoder().writeSettings(ctx, ((Http2SettingsFrame)msg).settings(), promise);
    } else if ((msg instanceof Http2SettingsAckFrame))
    {

      encoder().writeSettingsAck(ctx, promise);
    } else if ((msg instanceof Http2GoAwayFrame)) {
      writeGoAwayFrame(ctx, (Http2GoAwayFrame)msg, promise);
    } else if ((msg instanceof Http2PushPromiseFrame)) {
      Http2PushPromiseFrame pushPromiseFrame = (Http2PushPromiseFrame)msg;
      writePushPromise(ctx, pushPromiseFrame, promise);
    } else if ((msg instanceof Http2PriorityFrame)) {
      Http2PriorityFrame priorityFrame = (Http2PriorityFrame)msg;
      encoder().writePriority(ctx, priorityFrame.stream().id(), priorityFrame.streamDependency(), priorityFrame
        .weight(), priorityFrame.exclusive(), promise);
    } else if ((msg instanceof Http2UnknownFrame)) {
      Http2UnknownFrame unknownFrame = (Http2UnknownFrame)msg;
      encoder().writeFrame(ctx, unknownFrame.frameType(), unknownFrame.stream().id(), unknownFrame
        .flags(), unknownFrame.content(), promise);
    } else if (!(msg instanceof Http2Frame)) {
      ctx.write(msg, promise);
    } else {
      ReferenceCountUtil.release(msg);
      throw new UnsupportedMessageTypeException(msg, new Class[0]);
    }
  }
  
  private void increaseInitialConnectionWindow(int deltaBytes) throws Http2Exception
  {
    ((Http2LocalFlowController)connection().local().flowController()).incrementWindowSize(connection().connectionStream(), deltaBytes);
  }
  
  final boolean consumeBytes(int streamId, int bytes) throws Http2Exception {
    Http2Stream stream = connection().stream(streamId);
    

    if ((stream != null) && (streamId == 1)) {
      Boolean upgraded = (Boolean)stream.getProperty(upgradeKey);
      if (Boolean.TRUE.equals(upgraded)) {
        return false;
      }
    }
    
    return ((Http2LocalFlowController)connection().local().flowController()).consumeBytes(stream, bytes);
  }
  
  private void writeGoAwayFrame(ChannelHandlerContext ctx, Http2GoAwayFrame frame, ChannelPromise promise) {
    if (frame.lastStreamId() > -1) {
      frame.release();
      throw new IllegalArgumentException("Last stream id must not be set on GOAWAY frame");
    }
    
    int lastStreamCreated = connection().remote().lastStreamCreated();
    long lastStreamId = lastStreamCreated + frame.extraStreamIds() * 2L;
    
    if (lastStreamId > 2147483647L) {
      lastStreamId = 2147483647L;
    }
    goAway(ctx, (int)lastStreamId, frame.errorCode(), frame.content(), promise);
  }
  

  private void writeHeadersFrame(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame, ChannelPromise promise)
  {
    if (Http2CodecUtil.isStreamIdValid(headersFrame.stream().id())) {
      encoder().writeHeaders(ctx, headersFrame.stream().id(), headersFrame.headers(), headersFrame.padding(), headersFrame
        .isEndStream(), promise);
    } else if (initializeNewStream(ctx, (DefaultHttp2FrameStream)headersFrame.stream(), promise)) {
      final int streamId = headersFrame.stream().id();
      
      encoder().writeHeaders(ctx, streamId, headersFrame.headers(), headersFrame.padding(), headersFrame
        .isEndStream(), promise);
      
      if (!promise.isDone()) {
        numBufferedStreams += 1;
        

        promise.addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture channelFuture) {
            Http2FrameCodec.access$310(Http2FrameCodec.this);
            Http2FrameCodec.this.handleHeaderFuture(channelFuture, streamId);
          }
        });
      } else {
        handleHeaderFuture(promise, streamId);
      }
    }
  }
  
  private void writePushPromise(ChannelHandlerContext ctx, Http2PushPromiseFrame pushPromiseFrame, ChannelPromise promise)
  {
    if (Http2CodecUtil.isStreamIdValid(pushPromiseFrame.pushStream().id())) {
      encoder().writePushPromise(ctx, pushPromiseFrame.stream().id(), pushPromiseFrame.pushStream().id(), pushPromiseFrame
        .http2Headers(), pushPromiseFrame.padding(), promise);
    } else if (initializeNewStream(ctx, (DefaultHttp2FrameStream)pushPromiseFrame.pushStream(), promise)) {
      final int streamId = pushPromiseFrame.stream().id();
      encoder().writePushPromise(ctx, streamId, pushPromiseFrame.pushStream().id(), pushPromiseFrame
        .http2Headers(), pushPromiseFrame.padding(), promise);
      
      if (promise.isDone()) {
        handleHeaderFuture(promise, streamId);
      } else {
        numBufferedStreams += 1;
        

        promise.addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture channelFuture) {
            Http2FrameCodec.access$310(Http2FrameCodec.this);
            Http2FrameCodec.this.handleHeaderFuture(channelFuture, streamId);
          }
        });
      }
    }
  }
  
  private boolean initializeNewStream(ChannelHandlerContext ctx, DefaultHttp2FrameStream http2FrameStream, ChannelPromise promise)
  {
    Http2Connection connection = connection();
    int streamId = connection.local().incrementAndGetNextStreamId();
    if (streamId < 0) {
      promise.setFailure(new Http2NoMoreStreamIdsException());
      


      onHttp2Frame(ctx, new DefaultHttp2GoAwayFrame(connection.isServer() ? Integer.MAX_VALUE : 2147483646, Http2Error.NO_ERROR
        .code(), 
        ByteBufUtil.writeAscii(ctx.alloc(), "Stream IDs exhausted on local stream creation")));
      
      return false;
    }
    id = streamId;
    





    Object old = frameStreamToInitializeMap.put(streamId, http2FrameStream);
    

    assert (old == null);
    return true;
  }
  
  private void handleHeaderFuture(ChannelFuture channelFuture, int streamId) {
    if (!channelFuture.isSuccess()) {
      frameStreamToInitializeMap.remove(streamId);
    }
  }
  
  private void onStreamActive0(Http2Stream stream) {
    if ((stream.id() != 1) && 
      (connection().local().isValidStreamId(stream.id()))) {
      return;
    }
    
    DefaultHttp2FrameStream stream2 = newStream().setStreamAndProperty(streamKey, stream);
    onHttp2StreamStateChanged(ctx, stream2);
  }
  
  private final class ConnectionListener extends Http2ConnectionAdapter {
    private ConnectionListener() {}
    
    public void onStreamAdded(Http2Stream stream) { Http2FrameCodec.DefaultHttp2FrameStream frameStream = (Http2FrameCodec.DefaultHttp2FrameStream)frameStreamToInitializeMap.remove(stream.id());
      
      if (frameStream != null) {
        frameStream.setStreamAndProperty(streamKey, stream);
      }
    }
    
    public void onStreamActive(Http2Stream stream)
    {
      Http2FrameCodec.this.onStreamActive0(stream);
    }
    
    public void onStreamClosed(Http2Stream stream)
    {
      onHttp2StreamStateChanged0(stream);
    }
    
    public void onStreamHalfClosed(Http2Stream stream)
    {
      onHttp2StreamStateChanged0(stream);
    }
    
    private void onHttp2StreamStateChanged0(Http2Stream stream) {
      Http2FrameCodec.DefaultHttp2FrameStream stream2 = (Http2FrameCodec.DefaultHttp2FrameStream)stream.getProperty(streamKey);
      if (stream2 != null) {
        onHttp2StreamStateChanged(ctx, stream2);
      }
    }
  }
  

  protected void onConnectionError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception http2Ex)
  {
    if (!outbound)
    {



      ctx.fireExceptionCaught(cause);
    }
    super.onConnectionError(ctx, outbound, cause, http2Ex);
  }
  





  protected final void onStreamError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception.StreamException streamException)
  {
    int streamId = streamException.streamId();
    Http2Stream connectionStream = connection().stream(streamId);
    if (connectionStream == null) {
      onHttp2UnknownStreamError(ctx, cause, streamException);
      
      super.onStreamError(ctx, outbound, cause, streamException);
      return;
    }
    
    Http2FrameStream stream = (Http2FrameStream)connectionStream.getProperty(streamKey);
    if (stream == null) {
      LOG.warn("Stream exception thrown without stream object attached.", cause);
      
      super.onStreamError(ctx, outbound, cause, streamException);
      return;
    }
    
    if (!outbound)
    {
      onHttp2FrameStreamException(ctx, new Http2FrameStreamException(stream, streamException.error(), cause));
    }
  }
  




  private void onHttp2UnknownStreamError(ChannelHandlerContext ctx, Throwable cause, Http2Exception.StreamException streamException)
  {
    InternalLogLevel level = streamException.error() == Http2Error.STREAM_CLOSED ? InternalLogLevel.DEBUG : InternalLogLevel.WARN;
    LOG.log(level, "Stream exception thrown for unknown stream {}.", Integer.valueOf(streamException.streamId()), cause);
  }
  
  protected final boolean isGracefulShutdownComplete()
  {
    return (super.isGracefulShutdownComplete()) && (numBufferedStreams == 0);
  }
  
  private final class FrameListener implements Http2FrameListener
  {
    private FrameListener() {}
    
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) {
      if (streamId == 0)
      {
        return;
      }
      onHttp2Frame(ctx, new DefaultHttp2UnknownFrame(frameType, flags, payload)
        .stream(requireStream(streamId)).retain());
    }
    
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings)
    {
      onHttp2Frame(ctx, new DefaultHttp2SettingsFrame(settings));
    }
    
    public void onPingRead(ChannelHandlerContext ctx, long data)
    {
      onHttp2Frame(ctx, new DefaultHttp2PingFrame(data, false));
    }
    
    public void onPingAckRead(ChannelHandlerContext ctx, long data)
    {
      onHttp2Frame(ctx, new DefaultHttp2PingFrame(data, true));
    }
    
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode)
    {
      onHttp2Frame(ctx, new DefaultHttp2ResetFrame(errorCode).stream(requireStream(streamId)));
    }
    
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement)
    {
      if (streamId == 0)
      {
        return;
      }
      onHttp2Frame(ctx, new DefaultHttp2WindowUpdateFrame(windowSizeIncrement).stream(requireStream(streamId)));
    }
    


    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream)
    {
      onHeadersRead(ctx, streamId, headers, padding, endStream);
    }
    

    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream)
    {
      onHttp2Frame(ctx, new DefaultHttp2HeadersFrame(headers, endOfStream, padding)
        .stream(requireStream(streamId)));
    }
    

    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
    {
      onHttp2Frame(ctx, new DefaultHttp2DataFrame(data, endOfStream, padding)
        .stream(requireStream(streamId)).retain());
      
      return 0;
    }
    
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData)
    {
      onHttp2Frame(ctx, new DefaultHttp2GoAwayFrame(lastStreamId, errorCode, debugData).retain());
    }
    


    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive)
    {
      Http2Stream stream = connection().stream(streamId);
      if (stream == null)
      {
        return;
      }
      onHttp2Frame(ctx, new DefaultHttp2PriorityFrame(streamDependency, weight, exclusive)
        .stream(requireStream(streamId)));
    }
    
    public void onSettingsAckRead(ChannelHandlerContext ctx)
    {
      onHttp2Frame(ctx, Http2SettingsAckFrame.INSTANCE);
    }
    

    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding)
    {
      onHttp2Frame(ctx, new DefaultHttp2PushPromiseFrame(headers, padding, promisedStreamId)
        .pushStream(new Http2FrameCodec.DefaultHttp2FrameStream()
        .setStreamAndProperty(streamKey, connection().stream(promisedStreamId)))
        .stream(requireStream(streamId)));
    }
    
    private Http2FrameStream requireStream(int streamId) {
      Http2FrameStream stream = (Http2FrameStream)connection().stream(streamId).getProperty(streamKey);
      if (stream == null) {
        throw new IllegalStateException("Stream object required for identifier: " + streamId);
      }
      return stream;
    }
  }
  
  private void onUpgradeEvent(ChannelHandlerContext ctx, HttpServerUpgradeHandler.UpgradeEvent evt) {
    ctx.fireUserEventTriggered(evt);
  }
  
  private void onHttp2StreamWritabilityChanged(ChannelHandlerContext ctx, DefaultHttp2FrameStream stream, boolean writable)
  {
    ctx.fireUserEventTriggered(writabilityChanged);
  }
  
  void onHttp2StreamStateChanged(ChannelHandlerContext ctx, DefaultHttp2FrameStream stream) {
    ctx.fireUserEventTriggered(stateChanged);
  }
  
  void onHttp2Frame(ChannelHandlerContext ctx, Http2Frame frame) {
    ctx.fireChannelRead(frame);
  }
  

  void onHttp2FrameStreamException(ChannelHandlerContext ctx, Http2FrameStreamException cause) { ctx.fireExceptionCaught(cause); }
  
  private final class Http2RemoteFlowControllerListener implements Http2RemoteFlowController.Listener {
    private Http2RemoteFlowControllerListener() {}
    
    public void writabilityChanged(Http2Stream stream) {
      Http2FrameCodec.DefaultHttp2FrameStream frameStream = (Http2FrameCodec.DefaultHttp2FrameStream)stream.getProperty(streamKey);
      if (frameStream == null) {
        return;
      }
      Http2FrameCodec.this.onHttp2StreamWritabilityChanged(ctx, frameStream, 
        ((Http2RemoteFlowController)connection().remote().flowController()).isWritable(stream));
    }
  }
  



  static class DefaultHttp2FrameStream
    implements Http2FrameStream
  {
    private volatile int id = -1;
    
    private volatile Http2Stream stream;
    final Http2FrameStreamEvent stateChanged = Http2FrameStreamEvent.stateChanged(this);
    final Http2FrameStreamEvent writabilityChanged = Http2FrameStreamEvent.writabilityChanged(this);
    Channel attachment;
    
    DefaultHttp2FrameStream() {}
    
    DefaultHttp2FrameStream setStreamAndProperty(Http2Connection.PropertyKey streamKey, Http2Stream stream) { assert ((id == -1) || (stream.id() == id));
      this.stream = stream;
      stream.setProperty(streamKey, this);
      return this;
    }
    
    public int id()
    {
      Http2Stream stream = this.stream;
      return stream == null ? id : stream.id();
    }
    
    public Http2Stream.State state()
    {
      Http2Stream stream = this.stream;
      return stream == null ? Http2Stream.State.IDLE : stream.state();
    }
    
    public String toString()
    {
      return String.valueOf(id());
    }
  }
}
