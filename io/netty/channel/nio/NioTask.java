package io.netty.channel.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract interface NioTask<C extends SelectableChannel>
{
  public abstract void channelReady(C paramC, SelectionKey paramSelectionKey)
    throws Exception;
  
  public abstract void channelUnregistered(C paramC, Throwable paramThrowable)
    throws Exception;
}
