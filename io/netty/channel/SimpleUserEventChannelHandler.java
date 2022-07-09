package io.netty.channel;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;






































public abstract class SimpleUserEventChannelHandler<I>
  extends ChannelInboundHandlerAdapter
{
  private final TypeParameterMatcher matcher;
  private final boolean autoRelease;
  
  protected SimpleUserEventChannelHandler()
  {
    this(true);
  }
  





  protected SimpleUserEventChannelHandler(boolean autoRelease)
  {
    matcher = TypeParameterMatcher.find(this, SimpleUserEventChannelHandler.class, "I");
    this.autoRelease = autoRelease;
  }
  


  protected SimpleUserEventChannelHandler(Class<? extends I> eventType)
  {
    this(eventType, true);
  }
  






  protected SimpleUserEventChannelHandler(Class<? extends I> eventType, boolean autoRelease)
  {
    matcher = TypeParameterMatcher.get(eventType);
    this.autoRelease = autoRelease;
  }
  


  protected boolean acceptEvent(Object evt)
    throws Exception
  {
    return matcher.match(evt);
  }
  
  public final void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    boolean release = true;
    try {
      if (acceptEvent(evt))
      {
        I ievt = evt;
        eventReceived(ctx, ievt);
      } else {
        release = false;
        ctx.fireUserEventTriggered(evt);
      }
    } finally {
      if ((autoRelease) && (release)) {
        ReferenceCountUtil.release(evt);
      }
    }
  }
  
  protected abstract void eventReceived(ChannelHandlerContext paramChannelHandlerContext, I paramI)
    throws Exception;
}
