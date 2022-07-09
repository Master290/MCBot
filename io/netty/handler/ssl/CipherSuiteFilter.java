package io.netty.handler.ssl;

import java.util.List;
import java.util.Set;

public abstract interface CipherSuiteFilter
{
  public abstract String[] filterCipherSuites(Iterable<String> paramIterable, List<String> paramList, Set<String> paramSet);
}
