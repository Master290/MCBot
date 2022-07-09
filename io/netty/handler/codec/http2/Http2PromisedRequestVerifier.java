package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;




















































public abstract interface Http2PromisedRequestVerifier
{
  public static final Http2PromisedRequestVerifier ALWAYS_VERIFY = new Http2PromisedRequestVerifier()
  {
    public boolean isAuthoritative(ChannelHandlerContext ctx, Http2Headers headers) {
      return true;
    }
    
    public boolean isCacheable(Http2Headers headers)
    {
      return true;
    }
    
    public boolean isSafe(Http2Headers headers)
    {
      return true;
    }
  };
  
  public abstract boolean isAuthoritative(ChannelHandlerContext paramChannelHandlerContext, Http2Headers paramHttp2Headers);
  
  public abstract boolean isCacheable(Http2Headers paramHttp2Headers);
  
  public abstract boolean isSafe(Http2Headers paramHttp2Headers);
}
