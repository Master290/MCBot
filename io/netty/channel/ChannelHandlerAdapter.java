package io.netty.channel;

import io.netty.util.internal.InternalThreadLocalMap;
import java.util.Map;






















public abstract class ChannelHandlerAdapter
  implements ChannelHandler
{
  boolean added;
  
  public ChannelHandlerAdapter() {}
  
  protected void ensureNotSharable()
  {
    if (isSharable()) {
      throw new IllegalStateException("ChannelHandler " + getClass().getName() + " is not allowed to be shared");
    }
  }
  











  public boolean isSharable()
  {
    Class<?> clazz = getClass();
    Map<Class<?>, Boolean> cache = InternalThreadLocalMap.get().handlerSharableCache();
    Boolean sharable = (Boolean)cache.get(clazz);
    if (sharable == null) {
      sharable = Boolean.valueOf(clazz.isAnnotationPresent(ChannelHandler.Sharable.class));
      cache.put(clazz, sharable);
    }
    return sharable.booleanValue();
  }
  





  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {}
  





  public void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {}
  





  @ChannelHandlerMask.Skip
  @Deprecated
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    ctx.fireExceptionCaught(cause);
  }
}
