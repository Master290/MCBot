package io.netty.resolver;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


























public final class HostsFileParser
{
  public static HostsFileEntries parseSilently()
  {
    return hostsFileEntries(HostsFileEntriesProvider.parser().parseSilently());
  }
  






  public static HostsFileEntries parseSilently(Charset... charsets)
  {
    return hostsFileEntries(HostsFileEntriesProvider.parser().parseSilently(charsets));
  }
  




  public static HostsFileEntries parse()
    throws IOException
  {
    return hostsFileEntries(HostsFileEntriesProvider.parser().parse());
  }
  





  public static HostsFileEntries parse(File file)
    throws IOException
  {
    return hostsFileEntries(HostsFileEntriesProvider.parser().parse(file, new Charset[0]));
  }
  






  public static HostsFileEntries parse(File file, Charset... charsets)
    throws IOException
  {
    return hostsFileEntries(HostsFileEntriesProvider.parser().parse(file, charsets));
  }
  





  public static HostsFileEntries parse(Reader reader)
    throws IOException
  {
    return hostsFileEntries(HostsFileEntriesProvider.parser().parse(reader));
  }
  


  private HostsFileParser() {}
  


  private static HostsFileEntries hostsFileEntries(HostsFileEntriesProvider provider)
  {
    return provider == HostsFileEntriesProvider.EMPTY ? HostsFileEntries.EMPTY : new HostsFileEntries(
      toMapWithSingleValue(provider.ipv4Entries()), 
      toMapWithSingleValue(provider.ipv6Entries()));
  }
  
  private static Map<String, ?> toMapWithSingleValue(Map<String, List<InetAddress>> fromMapWithListValue) {
    Map<String, InetAddress> result = new HashMap();
    for (Map.Entry<String, List<InetAddress>> entry : fromMapWithListValue.entrySet()) {
      List<InetAddress> value = (List)entry.getValue();
      if (!value.isEmpty()) {
        result.put(entry.getKey(), value.get(0));
      }
    }
    return result;
  }
}
