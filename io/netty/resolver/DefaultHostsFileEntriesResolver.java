package io.netty.resolver;

import io.netty.util.CharsetUtil;
import io.netty.util.internal.PlatformDependent;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


















public final class DefaultHostsFileEntriesResolver
  implements HostsFileEntriesResolver
{
  private final Map<String, List<InetAddress>> inet4Entries;
  private final Map<String, List<InetAddress>> inet6Entries;
  
  public DefaultHostsFileEntriesResolver()
  {
    this(parseEntries());
  }
  
  DefaultHostsFileEntriesResolver(HostsFileEntriesProvider entries)
  {
    inet4Entries = entries.ipv4Entries();
    inet6Entries = entries.ipv6Entries();
  }
  
  public InetAddress address(String inetHost, ResolvedAddressTypes resolvedAddressTypes)
  {
    String normalized = normalize(inetHost);
    switch (1.$SwitchMap$io$netty$resolver$ResolvedAddressTypes[resolvedAddressTypes.ordinal()]) {
    case 1: 
      return firstAddress((List)inet4Entries.get(normalized));
    case 2: 
      return firstAddress((List)inet6Entries.get(normalized));
    case 3: 
      InetAddress inet4Address = firstAddress((List)inet4Entries.get(normalized));
      return inet4Address != null ? inet4Address : firstAddress((List)inet6Entries.get(normalized));
    case 4: 
      InetAddress inet6Address = firstAddress((List)inet6Entries.get(normalized));
      return inet6Address != null ? inet6Address : firstAddress((List)inet4Entries.get(normalized));
    }
    throw new IllegalArgumentException("Unknown ResolvedAddressTypes " + resolvedAddressTypes);
  }
  








  public List<InetAddress> addresses(String inetHost, ResolvedAddressTypes resolvedAddressTypes)
  {
    String normalized = normalize(inetHost);
    switch (1.$SwitchMap$io$netty$resolver$ResolvedAddressTypes[resolvedAddressTypes.ordinal()]) {
    case 1: 
      return (List)inet4Entries.get(normalized);
    case 2: 
      return (List)inet6Entries.get(normalized);
    case 3: 
      List<InetAddress> allInet4Addresses = (List)inet4Entries.get(normalized);
      return allInet4Addresses != null ? allAddresses(allInet4Addresses, (List)inet6Entries.get(normalized)) : 
        (List)inet6Entries.get(normalized);
    case 4: 
      List<InetAddress> allInet6Addresses = (List)inet6Entries.get(normalized);
      return allInet6Addresses != null ? allAddresses(allInet6Addresses, (List)inet4Entries.get(normalized)) : 
        (List)inet4Entries.get(normalized);
    }
    throw new IllegalArgumentException("Unknown ResolvedAddressTypes " + resolvedAddressTypes);
  }
  

  String normalize(String inetHost)
  {
    return inetHost.toLowerCase(Locale.ENGLISH);
  }
  
  private static List<InetAddress> allAddresses(List<InetAddress> a, List<InetAddress> b) {
    List<InetAddress> result = new ArrayList(a.size() + (b == null ? 0 : b.size()));
    result.addAll(a);
    if (b != null) {
      result.addAll(b);
    }
    return result;
  }
  
  private static InetAddress firstAddress(List<InetAddress> addresses) {
    return (addresses != null) && (!addresses.isEmpty()) ? (InetAddress)addresses.get(0) : null;
  }
  
  private static HostsFileEntriesProvider parseEntries() {
    if (PlatformDependent.isWindows())
    {


      return 
        HostsFileEntriesProvider.parser().parseSilently(new Charset[] { Charset.defaultCharset(), CharsetUtil.UTF_16, CharsetUtil.UTF_8 });
    }
    return HostsFileEntriesProvider.parser().parseSilently();
  }
}
