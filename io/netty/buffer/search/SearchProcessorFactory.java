package io.netty.buffer.search;

public abstract interface SearchProcessorFactory
{
  public abstract SearchProcessor newSearchProcessor();
}
