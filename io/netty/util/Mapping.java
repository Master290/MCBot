package io.netty.util;

public abstract interface Mapping<IN, OUT>
{
  public abstract OUT map(IN paramIN);
}
