package io.netty.handler.codec.spdy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.ObjectUtil;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;





















public class SpdySessionHandler
  extends ChannelDuplexHandler
{
  private static final SpdyProtocolException PROTOCOL_EXCEPTION = SpdyProtocolException.newStatic(null, SpdySessionHandler.class, "handleOutboundMessage(...)");
  
  private static final SpdyProtocolException STREAM_CLOSED = SpdyProtocolException.newStatic("Stream closed", SpdySessionHandler.class, "removeStream(...)");
  
  private static final int DEFAULT_WINDOW_SIZE = 65536;
  private int initialSendWindowSize = 65536;
  private int initialReceiveWindowSize = 65536;
  private volatile int initialSessionReceiveWindowSize = 65536;
  
  private final SpdySession spdySession = new SpdySession(initialSendWindowSize, initialReceiveWindowSize);
  
  private int lastGoodStreamId;
  private static final int DEFAULT_MAX_CONCURRENT_STREAMS = Integer.MAX_VALUE;
  private int remoteConcurrentStreams = Integer.MAX_VALUE;
  private int localConcurrentStreams = Integer.MAX_VALUE;
  
  private final AtomicInteger pings = new AtomicInteger();
  

  private boolean sentGoAwayFrame;
  

  private boolean receivedGoAwayFrame;
  

  private ChannelFutureListener closeSessionFutureListener;
  

  private final boolean server;
  

  private final int minorVersion;
  

  public SpdySessionHandler(SpdyVersion version, boolean server)
  {
    minorVersion = ((SpdyVersion)ObjectUtil.checkNotNull(version, "version")).getMinorVersion();
    this.server = server;
  }
  
  public void setSessionReceiveWindowSize(int sessionReceiveWindowSize) {
    ObjectUtil.checkPositiveOrZero(sessionReceiveWindowSize, "sessionReceiveWindowSize");
    





    initialSessionReceiveWindowSize = sessionReceiveWindowSize;
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if ((msg instanceof SpdyDataFrame))
    {






















      SpdyDataFrame spdyDataFrame = (SpdyDataFrame)msg;
      int streamId = spdyDataFrame.streamId();
      
      int deltaWindowSize = -1 * spdyDataFrame.content().readableBytes();
      
      int newSessionWindowSize = spdySession.updateReceiveWindowSize(0, deltaWindowSize);
      

      if (newSessionWindowSize < 0) {
        issueSessionError(ctx, SpdySessionStatus.PROTOCOL_ERROR);
        return;
      }
      

      if (newSessionWindowSize <= initialSessionReceiveWindowSize / 2) {
        int sessionDeltaWindowSize = initialSessionReceiveWindowSize - newSessionWindowSize;
        spdySession.updateReceiveWindowSize(0, sessionDeltaWindowSize);
        SpdyWindowUpdateFrame spdyWindowUpdateFrame = new DefaultSpdyWindowUpdateFrame(0, sessionDeltaWindowSize);
        
        ctx.writeAndFlush(spdyWindowUpdateFrame);
      }
      


      if (!spdySession.isActiveStream(streamId)) {
        spdyDataFrame.release();
        if (streamId <= lastGoodStreamId) {
          issueStreamError(ctx, streamId, SpdyStreamStatus.PROTOCOL_ERROR);
        } else if (!sentGoAwayFrame) {
          issueStreamError(ctx, streamId, SpdyStreamStatus.INVALID_STREAM);
        }
        return;
      }
      


      if (spdySession.isRemoteSideClosed(streamId)) {
        spdyDataFrame.release();
        issueStreamError(ctx, streamId, SpdyStreamStatus.STREAM_ALREADY_CLOSED);
        return;
      }
      

      if ((!isRemoteInitiatedId(streamId)) && (!spdySession.hasReceivedReply(streamId))) {
        spdyDataFrame.release();
        issueStreamError(ctx, streamId, SpdyStreamStatus.PROTOCOL_ERROR);
        return;
      }
      







      int newWindowSize = spdySession.updateReceiveWindowSize(streamId, deltaWindowSize);
      





      if (newWindowSize < spdySession.getReceiveWindowSizeLowerBound(streamId)) {
        spdyDataFrame.release();
        issueStreamError(ctx, streamId, SpdyStreamStatus.FLOW_CONTROL_ERROR);
        return;
      }
      


      if (newWindowSize < 0) {
        while (spdyDataFrame.content().readableBytes() > initialReceiveWindowSize)
        {
          SpdyDataFrame partialDataFrame = new DefaultSpdyDataFrame(streamId, spdyDataFrame.content().readRetainedSlice(initialReceiveWindowSize));
          ctx.writeAndFlush(partialDataFrame);
        }
      }
      

      if ((newWindowSize <= initialReceiveWindowSize / 2) && (!spdyDataFrame.isLast())) {
        int streamDeltaWindowSize = initialReceiveWindowSize - newWindowSize;
        spdySession.updateReceiveWindowSize(streamId, streamDeltaWindowSize);
        SpdyWindowUpdateFrame spdyWindowUpdateFrame = new DefaultSpdyWindowUpdateFrame(streamId, streamDeltaWindowSize);
        
        ctx.writeAndFlush(spdyWindowUpdateFrame);
      }
      

      if (spdyDataFrame.isLast()) {
        halfCloseStream(streamId, true, ctx.newSucceededFuture());
      }
    }
    else if ((msg instanceof SpdySynStreamFrame))
    {














      SpdySynStreamFrame spdySynStreamFrame = (SpdySynStreamFrame)msg;
      int streamId = spdySynStreamFrame.streamId();
      

      if ((spdySynStreamFrame.isInvalid()) || 
        (!isRemoteInitiatedId(streamId)) || 
        (spdySession.isActiveStream(streamId))) {
        issueStreamError(ctx, streamId, SpdyStreamStatus.PROTOCOL_ERROR);
        return;
      }
      

      if (streamId <= lastGoodStreamId) {
        issueSessionError(ctx, SpdySessionStatus.PROTOCOL_ERROR);
        return;
      }
      

      byte priority = spdySynStreamFrame.priority();
      boolean remoteSideClosed = spdySynStreamFrame.isLast();
      boolean localSideClosed = spdySynStreamFrame.isUnidirectional();
      if (!acceptStream(streamId, priority, remoteSideClosed, localSideClosed)) {
        issueStreamError(ctx, streamId, SpdyStreamStatus.REFUSED_STREAM);
        return;
      }
    }
    else if ((msg instanceof SpdySynReplyFrame))
    {







      SpdySynReplyFrame spdySynReplyFrame = (SpdySynReplyFrame)msg;
      int streamId = spdySynReplyFrame.streamId();
      

      if ((spdySynReplyFrame.isInvalid()) || 
        (isRemoteInitiatedId(streamId)) || 
        (spdySession.isRemoteSideClosed(streamId))) {
        issueStreamError(ctx, streamId, SpdyStreamStatus.INVALID_STREAM);
        return;
      }
      

      if (spdySession.hasReceivedReply(streamId)) {
        issueStreamError(ctx, streamId, SpdyStreamStatus.STREAM_IN_USE);
        return;
      }
      
      spdySession.receivedReply(streamId);
      

      if (spdySynReplyFrame.isLast()) {
        halfCloseStream(streamId, true, ctx.newSucceededFuture());
      }
    }
    else if ((msg instanceof SpdyRstStreamFrame))
    {









      SpdyRstStreamFrame spdyRstStreamFrame = (SpdyRstStreamFrame)msg;
      removeStream(spdyRstStreamFrame.streamId(), ctx.newSucceededFuture());
    }
    else if ((msg instanceof SpdySettingsFrame))
    {
      SpdySettingsFrame spdySettingsFrame = (SpdySettingsFrame)msg;
      
      int settingsMinorVersion = spdySettingsFrame.getValue(0);
      if ((settingsMinorVersion >= 0) && (settingsMinorVersion != minorVersion))
      {
        issueSessionError(ctx, SpdySessionStatus.PROTOCOL_ERROR);
        return;
      }
      

      int newConcurrentStreams = spdySettingsFrame.getValue(4);
      if (newConcurrentStreams >= 0) {
        remoteConcurrentStreams = newConcurrentStreams;
      }
      



      if (spdySettingsFrame.isPersisted(7)) {
        spdySettingsFrame.removeValue(7);
      }
      spdySettingsFrame.setPersistValue(7, false);
      

      int newInitialWindowSize = spdySettingsFrame.getValue(7);
      if (newInitialWindowSize >= 0) {
        updateInitialSendWindowSize(newInitialWindowSize);
      }
    }
    else if ((msg instanceof SpdyPingFrame))
    {









      SpdyPingFrame spdyPingFrame = (SpdyPingFrame)msg;
      
      if (isRemoteInitiatedId(spdyPingFrame.id())) {
        ctx.writeAndFlush(spdyPingFrame);
        return;
      }
      

      if (pings.get() == 0) {
        return;
      }
      pings.getAndDecrement();
    }
    else if ((msg instanceof SpdyGoAwayFrame))
    {
      receivedGoAwayFrame = true;
    }
    else if ((msg instanceof SpdyHeadersFrame))
    {
      SpdyHeadersFrame spdyHeadersFrame = (SpdyHeadersFrame)msg;
      int streamId = spdyHeadersFrame.streamId();
      

      if (spdyHeadersFrame.isInvalid()) {
        issueStreamError(ctx, streamId, SpdyStreamStatus.PROTOCOL_ERROR);
        return;
      }
      
      if (spdySession.isRemoteSideClosed(streamId)) {
        issueStreamError(ctx, streamId, SpdyStreamStatus.INVALID_STREAM);
        return;
      }
      

      if (spdyHeadersFrame.isLast()) {
        halfCloseStream(streamId, true, ctx.newSucceededFuture());
      }
    }
    else if ((msg instanceof SpdyWindowUpdateFrame))
    {










      SpdyWindowUpdateFrame spdyWindowUpdateFrame = (SpdyWindowUpdateFrame)msg;
      int streamId = spdyWindowUpdateFrame.streamId();
      int deltaWindowSize = spdyWindowUpdateFrame.deltaWindowSize();
      

      if ((streamId != 0) && (spdySession.isLocalSideClosed(streamId))) {
        return;
      }
      

      if (spdySession.getSendWindowSize(streamId) > Integer.MAX_VALUE - deltaWindowSize) {
        if (streamId == 0) {
          issueSessionError(ctx, SpdySessionStatus.PROTOCOL_ERROR);
        } else {
          issueStreamError(ctx, streamId, SpdyStreamStatus.FLOW_CONTROL_ERROR);
        }
        return;
      }
      
      updateSendWindowSize(ctx, streamId, deltaWindowSize);
    }
    
    ctx.fireChannelRead(msg);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    for (Integer streamId : spdySession.activeStreams().keySet()) {
      removeStream(streamId.intValue(), ctx.newSucceededFuture());
    }
    ctx.fireChannelInactive();
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    if ((cause instanceof SpdyProtocolException)) {
      issueSessionError(ctx, SpdySessionStatus.PROTOCOL_ERROR);
    }
    
    ctx.fireExceptionCaught(cause);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    sendGoAwayFrame(ctx, promise);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (((msg instanceof SpdyDataFrame)) || ((msg instanceof SpdySynStreamFrame)) || ((msg instanceof SpdySynReplyFrame)) || ((msg instanceof SpdyRstStreamFrame)) || ((msg instanceof SpdySettingsFrame)) || ((msg instanceof SpdyPingFrame)) || ((msg instanceof SpdyGoAwayFrame)) || ((msg instanceof SpdyHeadersFrame)) || ((msg instanceof SpdyWindowUpdateFrame)))
    {








      handleOutboundMessage(ctx, msg, promise);
    } else {
      ctx.write(msg, promise);
    }
  }
  
  private void handleOutboundMessage(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if ((msg instanceof SpdyDataFrame))
    {
      SpdyDataFrame spdyDataFrame = (SpdyDataFrame)msg;
      int streamId = spdyDataFrame.streamId();
      

      if (spdySession.isLocalSideClosed(streamId)) {
        spdyDataFrame.release();
        promise.setFailure(PROTOCOL_EXCEPTION);
        return;
      }
      













      int dataLength = spdyDataFrame.content().readableBytes();
      int sendWindowSize = spdySession.getSendWindowSize(streamId);
      int sessionSendWindowSize = spdySession.getSendWindowSize(0);
      sendWindowSize = Math.min(sendWindowSize, sessionSendWindowSize);
      
      if (sendWindowSize <= 0)
      {
        spdySession.putPendingWrite(streamId, new SpdySession.PendingWrite(spdyDataFrame, promise));
        return; }
      if (sendWindowSize < dataLength)
      {
        spdySession.updateSendWindowSize(streamId, -1 * sendWindowSize);
        spdySession.updateSendWindowSize(0, -1 * sendWindowSize);
        


        SpdyDataFrame partialDataFrame = new DefaultSpdyDataFrame(streamId, spdyDataFrame.content().readRetainedSlice(sendWindowSize));
        

        spdySession.putPendingWrite(streamId, new SpdySession.PendingWrite(spdyDataFrame, promise));
        


        final ChannelHandlerContext context = ctx;
        ctx.write(partialDataFrame).addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              SpdySessionHandler.this.issueSessionError(context, SpdySessionStatus.INTERNAL_ERROR);
            }
          }
        });
        return;
      }
      
      spdySession.updateSendWindowSize(streamId, -1 * dataLength);
      spdySession.updateSendWindowSize(0, -1 * dataLength);
      


      final ChannelHandlerContext context = ctx;
      promise.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) throws Exception {
          if (!future.isSuccess()) {
            SpdySessionHandler.this.issueSessionError(context, SpdySessionStatus.INTERNAL_ERROR);
          }
        }
      });
      


      if (spdyDataFrame.isLast()) {
        halfCloseStream(streamId, false, promise);
      }
    }
    else if ((msg instanceof SpdySynStreamFrame))
    {
      SpdySynStreamFrame spdySynStreamFrame = (SpdySynStreamFrame)msg;
      int streamId = spdySynStreamFrame.streamId();
      
      if (isRemoteInitiatedId(streamId)) {
        promise.setFailure(PROTOCOL_EXCEPTION);
        return;
      }
      
      byte priority = spdySynStreamFrame.priority();
      boolean remoteSideClosed = spdySynStreamFrame.isUnidirectional();
      boolean localSideClosed = spdySynStreamFrame.isLast();
      if (!acceptStream(streamId, priority, remoteSideClosed, localSideClosed)) {
        promise.setFailure(PROTOCOL_EXCEPTION);
        return;
      }
    }
    else if ((msg instanceof SpdySynReplyFrame))
    {
      SpdySynReplyFrame spdySynReplyFrame = (SpdySynReplyFrame)msg;
      int streamId = spdySynReplyFrame.streamId();
      

      if ((!isRemoteInitiatedId(streamId)) || (spdySession.isLocalSideClosed(streamId))) {
        promise.setFailure(PROTOCOL_EXCEPTION);
        return;
      }
      

      if (spdySynReplyFrame.isLast()) {
        halfCloseStream(streamId, false, promise);
      }
    }
    else if ((msg instanceof SpdyRstStreamFrame))
    {
      SpdyRstStreamFrame spdyRstStreamFrame = (SpdyRstStreamFrame)msg;
      removeStream(spdyRstStreamFrame.streamId(), promise);
    }
    else if ((msg instanceof SpdySettingsFrame))
    {
      SpdySettingsFrame spdySettingsFrame = (SpdySettingsFrame)msg;
      
      int settingsMinorVersion = spdySettingsFrame.getValue(0);
      if ((settingsMinorVersion >= 0) && (settingsMinorVersion != minorVersion))
      {
        promise.setFailure(PROTOCOL_EXCEPTION);
        return;
      }
      

      int newConcurrentStreams = spdySettingsFrame.getValue(4);
      if (newConcurrentStreams >= 0) {
        localConcurrentStreams = newConcurrentStreams;
      }
      



      if (spdySettingsFrame.isPersisted(7)) {
        spdySettingsFrame.removeValue(7);
      }
      spdySettingsFrame.setPersistValue(7, false);
      

      int newInitialWindowSize = spdySettingsFrame.getValue(7);
      if (newInitialWindowSize >= 0) {
        updateInitialReceiveWindowSize(newInitialWindowSize);
      }
    }
    else if ((msg instanceof SpdyPingFrame))
    {
      SpdyPingFrame spdyPingFrame = (SpdyPingFrame)msg;
      if (isRemoteInitiatedId(spdyPingFrame.id())) {
        ctx.fireExceptionCaught(new IllegalArgumentException("invalid PING ID: " + spdyPingFrame
          .id()));
        return;
      }
      pings.getAndIncrement();
    } else {
      if ((msg instanceof SpdyGoAwayFrame))
      {


        promise.setFailure(PROTOCOL_EXCEPTION);
        return;
      }
      if ((msg instanceof SpdyHeadersFrame))
      {
        SpdyHeadersFrame spdyHeadersFrame = (SpdyHeadersFrame)msg;
        int streamId = spdyHeadersFrame.streamId();
        

        if (spdySession.isLocalSideClosed(streamId)) {
          promise.setFailure(PROTOCOL_EXCEPTION);
          return;
        }
        

        if (spdyHeadersFrame.isLast()) {
          halfCloseStream(streamId, false, promise);
        }
      }
      else if ((msg instanceof SpdyWindowUpdateFrame))
      {

        promise.setFailure(PROTOCOL_EXCEPTION);
        return;
      }
    }
    ctx.write(msg, promise);
  }
  










  private void issueSessionError(ChannelHandlerContext ctx, SpdySessionStatus status)
  {
    sendGoAwayFrame(ctx, status).addListener(new ClosingChannelFutureListener(ctx, ctx.newPromise()));
  }
  










  private void issueStreamError(ChannelHandlerContext ctx, int streamId, SpdyStreamStatus status)
  {
    boolean fireChannelRead = !spdySession.isRemoteSideClosed(streamId);
    ChannelPromise promise = ctx.newPromise();
    removeStream(streamId, promise);
    
    SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, status);
    ctx.writeAndFlush(spdyRstStreamFrame, promise);
    if (fireChannelRead) {
      ctx.fireChannelRead(spdyRstStreamFrame);
    }
  }
  



  private boolean isRemoteInitiatedId(int id)
  {
    boolean serverId = SpdyCodecUtil.isServerId(id);
    return ((server) && (!serverId)) || ((!server) && (serverId));
  }
  
  private void updateInitialSendWindowSize(int newInitialWindowSize)
  {
    int deltaWindowSize = newInitialWindowSize - initialSendWindowSize;
    initialSendWindowSize = newInitialWindowSize;
    spdySession.updateAllSendWindowSizes(deltaWindowSize);
  }
  
  private void updateInitialReceiveWindowSize(int newInitialWindowSize)
  {
    int deltaWindowSize = newInitialWindowSize - initialReceiveWindowSize;
    initialReceiveWindowSize = newInitialWindowSize;
    spdySession.updateAllReceiveWindowSizes(deltaWindowSize);
  }
  


  private boolean acceptStream(int streamId, byte priority, boolean remoteSideClosed, boolean localSideClosed)
  {
    if ((receivedGoAwayFrame) || (sentGoAwayFrame)) {
      return false;
    }
    
    boolean remote = isRemoteInitiatedId(streamId);
    int maxConcurrentStreams = remote ? localConcurrentStreams : remoteConcurrentStreams;
    if (spdySession.numActiveStreams(remote) >= maxConcurrentStreams) {
      return false;
    }
    spdySession.acceptStream(streamId, priority, remoteSideClosed, localSideClosed, initialSendWindowSize, initialReceiveWindowSize, remote);
    

    if (remote) {
      lastGoodStreamId = streamId;
    }
    return true;
  }
  
  private void halfCloseStream(int streamId, boolean remote, ChannelFuture future) {
    if (remote) {
      spdySession.closeRemoteSide(streamId, isRemoteInitiatedId(streamId));
    } else {
      spdySession.closeLocalSide(streamId, isRemoteInitiatedId(streamId));
    }
    if ((closeSessionFutureListener != null) && (spdySession.noActiveStreams())) {
      future.addListener(closeSessionFutureListener);
    }
  }
  
  private void removeStream(int streamId, ChannelFuture future) {
    spdySession.removeStream(streamId, STREAM_CLOSED, isRemoteInitiatedId(streamId));
    
    if ((closeSessionFutureListener != null) && (spdySession.noActiveStreams())) {
      future.addListener(closeSessionFutureListener);
    }
  }
  
  private void updateSendWindowSize(final ChannelHandlerContext ctx, int streamId, int deltaWindowSize) {
    spdySession.updateSendWindowSize(streamId, deltaWindowSize);
    
    for (;;)
    {
      SpdySession.PendingWrite pendingWrite = spdySession.getPendingWrite(streamId);
      if (pendingWrite == null) {
        return;
      }
      
      SpdyDataFrame spdyDataFrame = spdyDataFrame;
      int dataFrameSize = spdyDataFrame.content().readableBytes();
      int writeStreamId = spdyDataFrame.streamId();
      int sendWindowSize = spdySession.getSendWindowSize(writeStreamId);
      int sessionSendWindowSize = spdySession.getSendWindowSize(0);
      sendWindowSize = Math.min(sendWindowSize, sessionSendWindowSize);
      
      if (sendWindowSize <= 0)
        return;
      if (sendWindowSize < dataFrameSize)
      {
        spdySession.updateSendWindowSize(writeStreamId, -1 * sendWindowSize);
        spdySession.updateSendWindowSize(0, -1 * sendWindowSize);
        


        SpdyDataFrame partialDataFrame = new DefaultSpdyDataFrame(writeStreamId, spdyDataFrame.content().readRetainedSlice(sendWindowSize));
        


        ctx.writeAndFlush(partialDataFrame).addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              SpdySessionHandler.this.issueSessionError(ctx, SpdySessionStatus.INTERNAL_ERROR);
            }
          }
        });
      }
      else {
        spdySession.removePendingWrite(writeStreamId);
        spdySession.updateSendWindowSize(writeStreamId, -1 * dataFrameSize);
        spdySession.updateSendWindowSize(0, -1 * dataFrameSize);
        

        if (spdyDataFrame.isLast()) {
          halfCloseStream(writeStreamId, false, promise);
        }
        


        ctx.writeAndFlush(spdyDataFrame, promise).addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              SpdySessionHandler.this.issueSessionError(ctx, SpdySessionStatus.INTERNAL_ERROR);
            }
          }
        });
      }
    }
  }
  
  private void sendGoAwayFrame(ChannelHandlerContext ctx, ChannelPromise future)
  {
    if (!ctx.channel().isActive()) {
      ctx.close(future);
      return;
    }
    
    ChannelFuture f = sendGoAwayFrame(ctx, SpdySessionStatus.OK);
    if (spdySession.noActiveStreams()) {
      f.addListener(new ClosingChannelFutureListener(ctx, future));
    } else {
      closeSessionFutureListener = new ClosingChannelFutureListener(ctx, future);
    }
  }
  

  private ChannelFuture sendGoAwayFrame(ChannelHandlerContext ctx, SpdySessionStatus status)
  {
    if (!sentGoAwayFrame) {
      sentGoAwayFrame = true;
      SpdyGoAwayFrame spdyGoAwayFrame = new DefaultSpdyGoAwayFrame(lastGoodStreamId, status);
      return ctx.writeAndFlush(spdyGoAwayFrame);
    }
    return ctx.newSucceededFuture();
  }
  
  private static final class ClosingChannelFutureListener implements ChannelFutureListener
  {
    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise;
    
    ClosingChannelFutureListener(ChannelHandlerContext ctx, ChannelPromise promise) {
      this.ctx = ctx;
      this.promise = promise;
    }
    
    public void operationComplete(ChannelFuture sentGoAwayFuture) throws Exception
    {
      ctx.close(promise);
    }
  }
}
