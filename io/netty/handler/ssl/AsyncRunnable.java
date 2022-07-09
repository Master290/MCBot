package io.netty.handler.ssl;

abstract interface AsyncRunnable
  extends Runnable
{
  public abstract void run(Runnable paramRunnable);
}
