package io.netty.channel.sctp;

import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;
import io.netty.channel.ChannelPipeline;
import io.netty.util.internal.ObjectUtil;



















public final class SctpNotificationHandler
  extends AbstractNotificationHandler<Object>
{
  private final SctpChannel sctpChannel;
  
  public SctpNotificationHandler(SctpChannel sctpChannel)
  {
    this.sctpChannel = ((SctpChannel)ObjectUtil.checkNotNull(sctpChannel, "sctpChannel"));
  }
  
  public HandlerResult handleNotification(AssociationChangeNotification notification, Object o)
  {
    fireEvent(notification);
    return HandlerResult.CONTINUE;
  }
  
  public HandlerResult handleNotification(PeerAddressChangeNotification notification, Object o)
  {
    fireEvent(notification);
    return HandlerResult.CONTINUE;
  }
  
  public HandlerResult handleNotification(SendFailedNotification notification, Object o)
  {
    fireEvent(notification);
    return HandlerResult.CONTINUE;
  }
  
  public HandlerResult handleNotification(ShutdownNotification notification, Object o)
  {
    fireEvent(notification);
    sctpChannel.close();
    return HandlerResult.RETURN;
  }
  
  private void fireEvent(Notification notification) {
    sctpChannel.pipeline().fireUserEventTriggered(notification);
  }
}
