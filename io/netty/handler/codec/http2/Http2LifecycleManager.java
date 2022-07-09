package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public abstract interface Http2LifecycleManager
{
  public abstract void closeStreamLocal(Http2Stream paramHttp2Stream, ChannelFuture paramChannelFuture);
  
  public abstract void closeStreamRemote(Http2Stream paramHttp2Stream, ChannelFuture paramChannelFuture);
  
  public abstract void closeStream(Http2Stream paramHttp2Stream, ChannelFuture paramChannelFuture);
  
  public abstract ChannelFuture resetStream(ChannelHandlerContext paramChannelHandlerContext, int paramInt, long paramLong, ChannelPromise paramChannelPromise);
  
  public abstract ChannelFuture goAway(ChannelHandlerContext paramChannelHandlerContext, int paramInt, long paramLong, ByteBuf paramByteBuf, ChannelPromise paramChannelPromise);
  
  public abstract void onError(ChannelHandlerContext paramChannelHandlerContext, boolean paramBoolean, Throwable paramThrowable);
}
