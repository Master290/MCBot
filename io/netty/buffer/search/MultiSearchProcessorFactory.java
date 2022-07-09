package io.netty.buffer.search;

public abstract interface MultiSearchProcessorFactory
  extends SearchProcessorFactory
{
  public abstract MultiSearchProcessor newSearchProcessor();
}
