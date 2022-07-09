package io.netty.channel.kqueue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import java.io.IOException;
















abstract class AbstractKQueueDatagramChannel
  extends AbstractKQueueChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(true);
  
  AbstractKQueueDatagramChannel(Channel parent, BsdSocket fd, boolean active) {
    super(parent, fd, active);
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  protected abstract boolean doWriteMessage(Object paramObject) throws Exception;
  
  protected void doWrite(ChannelOutboundBuffer in) throws Exception
  {
    int maxMessagesPerWrite = maxMessagesPerWrite();
    while (maxMessagesPerWrite > 0) {
      Object msg = in.current();
      if (msg == null) {
        break;
      }
      try
      {
        boolean done = false;
        for (int i = config().getWriteSpinCount(); i > 0; i--) {
          if (doWriteMessage(msg)) {
            done = true;
            break;
          }
        }
        
        if (done) {
          in.remove();
          maxMessagesPerWrite--;
        } else {
          break;
        }
      } catch (IOException e) {
        maxMessagesPerWrite--;
        



        in.remove(e);
      }
    }
    

    writeFilter(!in.isEmpty());
  }
}
