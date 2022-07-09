package io.netty.channel;

import java.util.Queue;

public abstract interface EventLoopTaskQueueFactory
{
  public abstract Queue<Runnable> newTaskQueue(int paramInt);
}
