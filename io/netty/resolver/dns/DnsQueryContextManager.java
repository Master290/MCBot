package io.netty.resolver.dns;

import io.netty.util.NetUtil;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.internal.PlatformDependent;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;





















final class DnsQueryContextManager
{
  final Map<InetSocketAddress, IntObjectMap<DnsQueryContext>> map = new HashMap();
  
  DnsQueryContextManager() {}
  
  int add(DnsQueryContext qCtx) { IntObjectMap<DnsQueryContext> contexts = getOrCreateContextMap(qCtx.nameServerAddr());
    
    int id = PlatformDependent.threadLocalRandom().nextInt(65535) + 1;
    int maxTries = 131070;
    int tries = 0;
    
    synchronized (contexts) {
      do {
        if (!contexts.containsKey(id)) {
          contexts.put(id, qCtx);
          return id;
        }
        
        id = id + 1 & 0xFFFF;
        
        tries++; } while (tries < 131070);
      throw new IllegalStateException("query ID space exhausted: " + qCtx.question());
    }
  }
  

  DnsQueryContext get(InetSocketAddress nameServerAddr, int id)
  {
    IntObjectMap<DnsQueryContext> contexts = getContextMap(nameServerAddr);
    DnsQueryContext qCtx;
    DnsQueryContext qCtx; if (contexts != null) { DnsQueryContext qCtx;
      synchronized (contexts) {
        qCtx = (DnsQueryContext)contexts.get(id);
      }
    } else {
      qCtx = null;
    }
    
    return qCtx;
  }
  
  DnsQueryContext remove(InetSocketAddress nameServerAddr, int id) {
    IntObjectMap<DnsQueryContext> contexts = getContextMap(nameServerAddr);
    if (contexts == null) {
      return null;
    }
    
    synchronized (contexts) {
      return (DnsQueryContext)contexts.remove(id);
    }
  }
  
  private IntObjectMap<DnsQueryContext> getContextMap(InetSocketAddress nameServerAddr) {
    synchronized (map) {
      return (IntObjectMap)map.get(nameServerAddr);
    }
  }
  
  private IntObjectMap<DnsQueryContext> getOrCreateContextMap(InetSocketAddress nameServerAddr) {
    synchronized (map) {
      IntObjectMap<DnsQueryContext> contexts = (IntObjectMap)map.get(nameServerAddr);
      if (contexts != null) {
        return contexts;
      }
      
      IntObjectMap<DnsQueryContext> newContexts = new IntObjectHashMap();
      InetAddress a = nameServerAddr.getAddress();
      int port = nameServerAddr.getPort();
      map.put(nameServerAddr, newContexts);
      
      if ((a instanceof Inet4Address))
      {
        Inet4Address a4 = (Inet4Address)a;
        if (a4.isLoopbackAddress()) {
          map.put(new InetSocketAddress(NetUtil.LOCALHOST6, port), newContexts);
        } else {
          map.put(new InetSocketAddress(toCompactAddress(a4), port), newContexts);
        }
      } else if ((a instanceof Inet6Address))
      {
        Inet6Address a6 = (Inet6Address)a;
        if (a6.isLoopbackAddress()) {
          map.put(new InetSocketAddress(NetUtil.LOCALHOST4, port), newContexts);
        } else if (a6.isIPv4CompatibleAddress()) {
          map.put(new InetSocketAddress(toIPv4Address(a6), port), newContexts);
        }
      }
      
      return newContexts;
    }
  }
  
  private static Inet6Address toCompactAddress(Inet4Address a4) {
    byte[] b4 = a4.getAddress();
    byte[] b6 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, b4[0], b4[1], b4[2], b4[3] };
    try {
      return (Inet6Address)InetAddress.getByAddress(b6);
    } catch (UnknownHostException e) {
      throw new Error(e);
    }
  }
  
  private static Inet4Address toIPv4Address(Inet6Address a6) {
    byte[] b6 = a6.getAddress();
    byte[] b4 = { b6[12], b6[13], b6[14], b6[15] };
    try {
      return (Inet4Address)InetAddress.getByAddress(b4);
    } catch (UnknownHostException e) {
      throw new Error(e);
    }
  }
}
