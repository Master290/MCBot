package io.netty.channel;

import io.netty.util.IntSupplier;

public abstract interface SelectStrategy
{
  public static final int SELECT = -1;
  public static final int CONTINUE = -2;
  public static final int BUSY_WAIT = -3;
  
  public abstract int calculateStrategy(IntSupplier paramIntSupplier, boolean paramBoolean)
    throws Exception;
}
