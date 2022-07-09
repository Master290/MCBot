package io.netty.resolver.dns;

import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;





















final class ShuffledDnsServerAddressStream
  implements DnsServerAddressStream
{
  private final List<InetSocketAddress> addresses;
  private int i;
  
  ShuffledDnsServerAddressStream(List<InetSocketAddress> addresses)
  {
    this.addresses = addresses;
    
    shuffle();
  }
  
  private ShuffledDnsServerAddressStream(List<InetSocketAddress> addresses, int startIdx) {
    this.addresses = addresses;
    i = startIdx;
  }
  
  private void shuffle() {
    Collections.shuffle(addresses, PlatformDependent.threadLocalRandom());
  }
  
  public InetSocketAddress next()
  {
    int i = this.i;
    InetSocketAddress next = (InetSocketAddress)addresses.get(i);
    i++; if (i < addresses.size()) {
      this.i = i;
    } else {
      this.i = 0;
      shuffle();
    }
    return next;
  }
  
  public int size()
  {
    return addresses.size();
  }
  
  public ShuffledDnsServerAddressStream duplicate()
  {
    return new ShuffledDnsServerAddressStream(addresses, i);
  }
  
  public String toString()
  {
    return SequentialDnsServerAddressStream.toString("shuffled", i, addresses);
  }
}
