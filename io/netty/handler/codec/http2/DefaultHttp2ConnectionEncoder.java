package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CoalescingBufferQueue;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Queue;






























public class DefaultHttp2ConnectionEncoder
  implements Http2ConnectionEncoder, Http2SettingsReceivedConsumer
{
  private final Http2FrameWriter frameWriter;
  private final Http2Connection connection;
  private Http2LifecycleManager lifecycleManager;
  private final Queue<Http2Settings> outstandingLocalSettingsQueue = new ArrayDeque(4);
  private Queue<Http2Settings> outstandingRemoteSettingsQueue;
  
  public DefaultHttp2ConnectionEncoder(Http2Connection connection, Http2FrameWriter frameWriter) {
    this.connection = ((Http2Connection)ObjectUtil.checkNotNull(connection, "connection"));
    this.frameWriter = ((Http2FrameWriter)ObjectUtil.checkNotNull(frameWriter, "frameWriter"));
    if (connection.remote().flowController() == null) {
      connection.remote().flowController(new DefaultHttp2RemoteFlowController(connection));
    }
  }
  
  public void lifecycleManager(Http2LifecycleManager lifecycleManager)
  {
    this.lifecycleManager = ((Http2LifecycleManager)ObjectUtil.checkNotNull(lifecycleManager, "lifecycleManager"));
  }
  
  public Http2FrameWriter frameWriter()
  {
    return frameWriter;
  }
  
  public Http2Connection connection()
  {
    return connection;
  }
  
  public final Http2RemoteFlowController flowController()
  {
    return (Http2RemoteFlowController)connection().remote().flowController();
  }
  
  public void remoteSettings(Http2Settings settings) throws Http2Exception
  {
    Boolean pushEnabled = settings.pushEnabled();
    Http2FrameWriter.Configuration config = configuration();
    Http2HeadersEncoder.Configuration outboundHeaderConfig = config.headersConfiguration();
    Http2FrameSizePolicy outboundFrameSizePolicy = config.frameSizePolicy();
    if (pushEnabled != null) {
      if ((!connection.isServer()) && (pushEnabled.booleanValue())) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Client received a value of ENABLE_PUSH specified to other than 0", new Object[0]);
      }
      
      connection.remote().allowPushTo(pushEnabled.booleanValue());
    }
    
    Long maxConcurrentStreams = settings.maxConcurrentStreams();
    if (maxConcurrentStreams != null) {
      connection.local().maxActiveStreams((int)Math.min(maxConcurrentStreams.longValue(), 2147483647L));
    }
    
    Long headerTableSize = settings.headerTableSize();
    if (headerTableSize != null) {
      outboundHeaderConfig.maxHeaderTableSize((int)Math.min(headerTableSize.longValue(), 2147483647L));
    }
    
    Long maxHeaderListSize = settings.maxHeaderListSize();
    if (maxHeaderListSize != null) {
      outboundHeaderConfig.maxHeaderListSize(maxHeaderListSize.longValue());
    }
    
    Integer maxFrameSize = settings.maxFrameSize();
    if (maxFrameSize != null) {
      outboundFrameSizePolicy.maxFrameSize(maxFrameSize.intValue());
    }
    
    Integer initialWindowSize = settings.initialWindowSize();
    if (initialWindowSize != null) {
      flowController().initialWindowSize(initialWindowSize.intValue());
    }
  }
  

  public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise)
  {
    promise = promise.unvoid();
    try
    {
      Http2Stream stream = requireStream(streamId);
      

      switch (2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()])
      {
      case 1: 
      case 2: 
        break;
      default: 
        throw new IllegalStateException("Stream " + stream.id() + " in unexpected state " + stream.state());
      }
    } catch (Throwable e) {
      data.release();
      return promise.setFailure(e);
    }
    
    Http2Stream stream;
    flowController().addFlowControlled(stream, new FlowControlledData(stream, data, padding, endOfStream, promise));
    
    return promise;
  }
  

  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream, ChannelPromise promise)
  {
    return writeHeaders0(ctx, streamId, headers, false, 0, (short)0, false, padding, endStream, promise);
  }
  
  private static boolean validateHeadersSentState(Http2Stream stream, Http2Headers headers, boolean isServer, boolean endOfStream)
  {
    boolean isInformational = (isServer) && (HttpStatusClass.valueOf(headers.status()) == HttpStatusClass.INFORMATIONAL);
    if (((!isInformational) && (endOfStream)) || ((stream.isHeadersSent()) || (stream.isTrailersSent()))) {
      throw new IllegalStateException("Stream " + stream.id() + " sent too many headers EOS: " + endOfStream);
    }
    return isInformational;
  }
  


  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise)
  {
    return writeHeaders0(ctx, streamId, headers, true, streamDependency, weight, exclusive, padding, endOfStream, promise);
  }
  








  private static ChannelFuture sendHeaders(Http2FrameWriter frameWriter, ChannelHandlerContext ctx, int streamId, Http2Headers headers, boolean hasPriority, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise)
  {
    if (hasPriority) {
      return frameWriter.writeHeaders(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream, promise);
    }
    
    return frameWriter.writeHeaders(ctx, streamId, headers, padding, endOfStream, promise);
  }
  


  private ChannelFuture writeHeaders0(ChannelHandlerContext ctx, int streamId, Http2Headers headers, boolean hasPriority, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise)
  {
    try
    {
      Http2Stream stream = connection.stream(streamId);
      if (stream == null)
      {

        try
        {


          stream = connection.local().createStream(streamId, false);
        } catch (Http2Exception cause) {
          if (connection.remote().mayHaveCreatedStream(streamId)) {
            promise.tryFailure(new IllegalStateException("Stream no longer exists: " + streamId, cause));
            return promise;
          }
          throw cause;
        }
      } else {
        switch (2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()]) {
        case 3: 
          stream.open(endOfStream);
          break;
        
        case 1: 
        case 2: 
          break;
        
        default: 
          throw new IllegalStateException("Stream " + stream.id() + " in unexpected state " + stream.state());
        }
        
      }
      

      Http2RemoteFlowController flowController = flowController();
      if ((!endOfStream) || (!flowController.hasFlowControlled(stream)))
      {

        promise = promise.unvoid();
        boolean isInformational = validateHeadersSentState(stream, headers, connection.isServer(), endOfStream);
        
        ChannelFuture future = sendHeaders(frameWriter, ctx, streamId, headers, hasPriority, streamDependency, weight, exclusive, padding, endOfStream, promise);
        


        Throwable failureCause = future.cause();
        if (failureCause == null)
        {




          stream.headersSent(isInformational);
          
          if (!future.isSuccess())
          {
            notifyLifecycleManagerOnError(future, ctx);
          }
        } else {
          lifecycleManager.onError(ctx, true, failureCause);
        }
        
        if (endOfStream)
        {


          lifecycleManager.closeStreamLocal(stream, future);
        }
        
        return future;
      }
      
      flowController.addFlowControlled(stream, new FlowControlledHeaders(stream, headers, hasPriority, streamDependency, weight, exclusive, padding, true, promise));
      

      return promise;
    }
    catch (Throwable t) {
      lifecycleManager.onError(ctx, true, t);
      promise.tryFailure(t); }
    return promise;
  }
  


  public ChannelFuture writePriority(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive, ChannelPromise promise)
  {
    return frameWriter.writePriority(ctx, streamId, streamDependency, weight, exclusive, promise);
  }
  


  public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    return lifecycleManager.resetStream(ctx, streamId, errorCode, promise);
  }
  

  public ChannelFuture writeSettings(ChannelHandlerContext ctx, Http2Settings settings, ChannelPromise promise)
  {
    outstandingLocalSettingsQueue.add(settings);
    try {
      Boolean pushEnabled = settings.pushEnabled();
      if ((pushEnabled != null) && (connection.isServer())) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server sending SETTINGS frame with ENABLE_PUSH specified", new Object[0]);
      }
    } catch (Throwable e) {
      return promise.setFailure(e);
    }
    
    return frameWriter.writeSettings(ctx, settings, promise);
  }
  
  public ChannelFuture writeSettingsAck(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    if (outstandingRemoteSettingsQueue == null) {
      return frameWriter.writeSettingsAck(ctx, promise);
    }
    Http2Settings settings = (Http2Settings)outstandingRemoteSettingsQueue.poll();
    if (settings == null) {
      return promise.setFailure(new Http2Exception(Http2Error.INTERNAL_ERROR, "attempted to write a SETTINGS ACK with no  pending SETTINGS"));
    }
    

    Http2CodecUtil.SimpleChannelPromiseAggregator aggregator = new Http2CodecUtil.SimpleChannelPromiseAggregator(promise, ctx.channel(), ctx.executor());
    


    frameWriter.writeSettingsAck(ctx, aggregator.newPromise());
    


    ChannelPromise applySettingsPromise = aggregator.newPromise();
    try {
      remoteSettings(settings);
      applySettingsPromise.setSuccess();
    } catch (Throwable e) {
      applySettingsPromise.setFailure(e);
      lifecycleManager.onError(ctx, true, e);
    }
    return aggregator.doneAllocatingPromises();
  }
  
  public ChannelFuture writePing(ChannelHandlerContext ctx, boolean ack, long data, ChannelPromise promise)
  {
    return frameWriter.writePing(ctx, ack, data, promise);
  }
  
  public ChannelFuture writePushPromise(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding, ChannelPromise promise)
  {
    try
    {
      if (connection.goAwayReceived()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending PUSH_PROMISE after GO_AWAY received.", new Object[0]);
      }
      
      Http2Stream stream = requireStream(streamId);
      
      connection.local().reservePushStream(promisedStreamId, stream);
      
      promise = promise.unvoid();
      ChannelFuture future = frameWriter.writePushPromise(ctx, streamId, promisedStreamId, headers, padding, promise);
      

      Throwable failureCause = future.cause();
      if (failureCause == null)
      {

        stream.pushPromiseSent();
        
        if (!future.isSuccess())
        {
          notifyLifecycleManagerOnError(future, ctx);
        }
      } else {
        lifecycleManager.onError(ctx, true, failureCause);
      }
      return future;
    } catch (Throwable t) {
      lifecycleManager.onError(ctx, true, t);
      promise.tryFailure(t); }
    return promise;
  }
  


  public ChannelFuture writeGoAway(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData, ChannelPromise promise)
  {
    return lifecycleManager.goAway(ctx, lastStreamId, errorCode, debugData, promise);
  }
  

  public ChannelFuture writeWindowUpdate(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement, ChannelPromise promise)
  {
    return promise.setFailure(new UnsupportedOperationException("Use the Http2[Inbound|Outbound]FlowController objects to control window sizes"));
  }
  


  public ChannelFuture writeFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload, ChannelPromise promise)
  {
    return frameWriter.writeFrame(ctx, frameType, streamId, flags, payload, promise);
  }
  
  public void close()
  {
    frameWriter.close();
  }
  
  public Http2Settings pollSentSettings()
  {
    return (Http2Settings)outstandingLocalSettingsQueue.poll();
  }
  
  public Http2FrameWriter.Configuration configuration()
  {
    return frameWriter.configuration();
  }
  
  private Http2Stream requireStream(int streamId) {
    Http2Stream stream = connection.stream(streamId);
    if (stream == null) { String message;
      String message;
      if (connection.streamMayHaveExisted(streamId)) {
        message = "Stream no longer exists: " + streamId;
      } else {
        message = "Stream does not exist: " + streamId;
      }
      throw new IllegalArgumentException(message);
    }
    return stream;
  }
  
  public void consumeReceivedSettings(Http2Settings settings)
  {
    if (outstandingRemoteSettingsQueue == null) {
      outstandingRemoteSettingsQueue = new ArrayDeque(2);
    }
    outstandingRemoteSettingsQueue.add(settings);
  }
  



  private final class FlowControlledData
    extends DefaultHttp2ConnectionEncoder.FlowControlledBase
  {
    private final CoalescingBufferQueue queue;
    

    private int dataSize;
    


    FlowControlledData(Http2Stream stream, ByteBuf buf, int padding, boolean endOfStream, ChannelPromise promise)
    {
      super(stream, padding, endOfStream, promise);
      queue = new CoalescingBufferQueue(promise.channel());
      queue.add(buf, promise);
      dataSize = queue.readableBytes();
    }
    
    public int size()
    {
      return dataSize + padding;
    }
    
    public void error(ChannelHandlerContext ctx, Throwable cause)
    {
      queue.releaseAndFailAll(cause);
      




      lifecycleManager.onError(ctx, true, cause);
    }
    
    public void write(ChannelHandlerContext ctx, int allowedBytes)
    {
      int queuedData = queue.readableBytes();
      if (!endOfStream) {
        if (queuedData == 0) {
          if (queue.isEmpty())
          {





            padding = (this.dataSize = 0);

          }
          else
          {
            ChannelPromise writePromise = ctx.newPromise().addListener(this);
            ctx.write(queue.remove(0, writePromise), writePromise);
          }
          return;
        }
        
        if (allowedBytes == 0) {
          return;
        }
      }
      

      int writableData = Math.min(queuedData, allowedBytes);
      ChannelPromise writePromise = ctx.newPromise().addListener(this);
      ByteBuf toWrite = queue.remove(writableData, writePromise);
      dataSize = queue.readableBytes();
      

      int writablePadding = Math.min(allowedBytes - writableData, padding);
      padding -= writablePadding;
      

      frameWriter().writeData(ctx, stream.id(), toWrite, writablePadding, (endOfStream) && 
        (size() == 0), writePromise);
    }
    
    public boolean merge(ChannelHandlerContext ctx, Http2RemoteFlowController.FlowControlled next)
    {
      FlowControlledData nextData;
      if ((FlowControlledData.class != next.getClass()) || 
        (Integer.MAX_VALUE - (nextData = (FlowControlledData)next).size() < size()))
        return false;
      FlowControlledData nextData;
      queue.copyTo(queue);
      dataSize = queue.readableBytes();
      
      padding = Math.max(padding, padding);
      endOfStream = endOfStream;
      return true;
    }
  }
  
  private void notifyLifecycleManagerOnError(ChannelFuture future, final ChannelHandlerContext ctx) {
    future.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future) throws Exception {
        Throwable cause = future.cause();
        if (cause != null) {
          lifecycleManager.onError(ctx, true, cause);
        }
      }
    });
  }
  

  private final class FlowControlledHeaders
    extends DefaultHttp2ConnectionEncoder.FlowControlledBase
  {
    private final Http2Headers headers;
    
    private final boolean hasPriorty;
    
    private final int streamDependency;
    
    private final short weight;
    private final boolean exclusive;
    
    FlowControlledHeaders(Http2Stream stream, Http2Headers headers, boolean hasPriority, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise)
    {
      super(stream, padding, endOfStream, promise.unvoid());
      this.headers = headers;
      hasPriorty = hasPriority;
      this.streamDependency = streamDependency;
      this.weight = weight;
      this.exclusive = exclusive;
    }
    
    public int size()
    {
      return 0;
    }
    
    public void error(ChannelHandlerContext ctx, Throwable cause)
    {
      if (ctx != null) {
        lifecycleManager.onError(ctx, true, cause);
      }
      promise.tryFailure(cause);
    }
    
    public void write(ChannelHandlerContext ctx, int allowedBytes)
    {
      boolean isInformational = DefaultHttp2ConnectionEncoder.validateHeadersSentState(stream, headers, connection.isServer(), endOfStream);
      

      promise.addListener(this);
      
      ChannelFuture f = DefaultHttp2ConnectionEncoder.sendHeaders(frameWriter, ctx, stream.id(), headers, hasPriorty, streamDependency, weight, exclusive, padding, endOfStream, promise);
      

      Throwable failureCause = f.cause();
      if (failureCause == null)
      {

        stream.headersSent(isInformational);
      }
    }
    
    public boolean merge(ChannelHandlerContext ctx, Http2RemoteFlowController.FlowControlled next)
    {
      return false;
    }
  }
  

  public abstract class FlowControlledBase
    implements Http2RemoteFlowController.FlowControlled, ChannelFutureListener
  {
    protected final Http2Stream stream;
    
    protected ChannelPromise promise;
    protected boolean endOfStream;
    protected int padding;
    
    FlowControlledBase(Http2Stream stream, int padding, boolean endOfStream, ChannelPromise promise)
    {
      ObjectUtil.checkPositiveOrZero(padding, "padding");
      this.padding = padding;
      this.endOfStream = endOfStream;
      this.stream = stream;
      this.promise = promise;
    }
    
    public void writeComplete()
    {
      if (endOfStream) {
        lifecycleManager.closeStreamLocal(stream, promise);
      }
    }
    
    public void operationComplete(ChannelFuture future) throws Exception
    {
      if (!future.isSuccess()) {
        error(flowController().channelHandlerContext(), future.cause());
      }
    }
  }
}
