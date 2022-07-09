package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import java.util.concurrent.Executor;


















public final class ImmediateExecutor
  implements Executor
{
  public static final ImmediateExecutor INSTANCE = new ImmediateExecutor();
  

  private ImmediateExecutor() {}
  

  public void execute(Runnable command)
  {
    ((Runnable)ObjectUtil.checkNotNull(command, "command")).run();
  }
}
