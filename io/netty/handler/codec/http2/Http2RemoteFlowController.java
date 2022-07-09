package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;

public abstract interface Http2RemoteFlowController
  extends Http2FlowController
{
  public abstract ChannelHandlerContext channelHandlerContext();
  
  public abstract void addFlowControlled(Http2Stream paramHttp2Stream, FlowControlled paramFlowControlled);
  
  public abstract boolean hasFlowControlled(Http2Stream paramHttp2Stream);
  
  public abstract void writePendingBytes()
    throws Http2Exception;
  
  public abstract void listener(Listener paramListener);
  
  public abstract boolean isWritable(Http2Stream paramHttp2Stream);
  
  public abstract void channelWritabilityChanged()
    throws Http2Exception;
  
  public abstract void updateDependencyTree(int paramInt1, int paramInt2, short paramShort, boolean paramBoolean);
  
  public static abstract interface Listener
  {
    public abstract void writabilityChanged(Http2Stream paramHttp2Stream);
  }
  
  public static abstract interface FlowControlled
  {
    public abstract int size();
    
    public abstract void error(ChannelHandlerContext paramChannelHandlerContext, Throwable paramThrowable);
    
    public abstract void writeComplete();
    
    public abstract void write(ChannelHandlerContext paramChannelHandlerContext, int paramInt);
    
    public abstract boolean merge(ChannelHandlerContext paramChannelHandlerContext, FlowControlled paramFlowControlled);
  }
}
