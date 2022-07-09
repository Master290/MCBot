package io.netty.resolver;

import java.net.InetAddress;






















public abstract interface HostsFileEntriesResolver
{
  public static final HostsFileEntriesResolver DEFAULT = new DefaultHostsFileEntriesResolver();
  
  public abstract InetAddress address(String paramString, ResolvedAddressTypes paramResolvedAddressTypes);
}
