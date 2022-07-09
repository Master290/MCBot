package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public abstract interface Http2Connection
{
  public abstract Future<Void> close(Promise<Void> paramPromise);
  
  public abstract PropertyKey newKey();
  
  public abstract void addListener(Listener paramListener);
  
  public abstract void removeListener(Listener paramListener);
  
  public abstract Http2Stream stream(int paramInt);
  
  public abstract boolean streamMayHaveExisted(int paramInt);
  
  public abstract Http2Stream connectionStream();
  
  public abstract int numActiveStreams();
  
  public abstract Http2Stream forEachActiveStream(Http2StreamVisitor paramHttp2StreamVisitor)
    throws Http2Exception;
  
  public abstract boolean isServer();
  
  public abstract Endpoint<Http2LocalFlowController> local();
  
  public abstract Endpoint<Http2RemoteFlowController> remote();
  
  public abstract boolean goAwayReceived();
  
  public abstract void goAwayReceived(int paramInt, long paramLong, ByteBuf paramByteBuf)
    throws Http2Exception;
  
  public abstract boolean goAwaySent();
  
  public abstract boolean goAwaySent(int paramInt, long paramLong, ByteBuf paramByteBuf)
    throws Http2Exception;
  
  public static abstract interface PropertyKey {}
  
  public static abstract interface Endpoint<F extends Http2FlowController>
  {
    public abstract int incrementAndGetNextStreamId();
    
    public abstract boolean isValidStreamId(int paramInt);
    
    public abstract boolean mayHaveCreatedStream(int paramInt);
    
    public abstract boolean created(Http2Stream paramHttp2Stream);
    
    public abstract boolean canOpenStream();
    
    public abstract Http2Stream createStream(int paramInt, boolean paramBoolean)
      throws Http2Exception;
    
    public abstract Http2Stream reservePushStream(int paramInt, Http2Stream paramHttp2Stream)
      throws Http2Exception;
    
    public abstract boolean isServer();
    
    public abstract void allowPushTo(boolean paramBoolean);
    
    public abstract boolean allowPushTo();
    
    public abstract int numActiveStreams();
    
    public abstract int maxActiveStreams();
    
    public abstract void maxActiveStreams(int paramInt);
    
    public abstract int lastStreamCreated();
    
    public abstract int lastStreamKnownByPeer();
    
    public abstract F flowController();
    
    public abstract void flowController(F paramF);
    
    public abstract Endpoint<? extends Http2FlowController> opposite();
  }
  
  public static abstract interface Listener
  {
    public abstract void onStreamAdded(Http2Stream paramHttp2Stream);
    
    public abstract void onStreamActive(Http2Stream paramHttp2Stream);
    
    public abstract void onStreamHalfClosed(Http2Stream paramHttp2Stream);
    
    public abstract void onStreamClosed(Http2Stream paramHttp2Stream);
    
    public abstract void onStreamRemoved(Http2Stream paramHttp2Stream);
    
    public abstract void onGoAwaySent(int paramInt, long paramLong, ByteBuf paramByteBuf);
    
    public abstract void onGoAwayReceived(int paramInt, long paramLong, ByteBuf paramByteBuf);
  }
}
