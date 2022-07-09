package io.netty.resolver.dns;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

















final class RotationalDnsServerAddresses
  extends DefaultDnsServerAddresses
{
  private static final AtomicIntegerFieldUpdater<RotationalDnsServerAddresses> startIdxUpdater = AtomicIntegerFieldUpdater.newUpdater(RotationalDnsServerAddresses.class, "startIdx");
  
  private volatile int startIdx;
  
  RotationalDnsServerAddresses(List<InetSocketAddress> addresses)
  {
    super("rotational", addresses);
  }
  
  public DnsServerAddressStream stream()
  {
    for (;;) {
      int curStartIdx = startIdx;
      int nextStartIdx = curStartIdx + 1;
      if (nextStartIdx >= addresses.size()) {
        nextStartIdx = 0;
      }
      if (startIdxUpdater.compareAndSet(this, curStartIdx, nextStartIdx)) {
        return new SequentialDnsServerAddressStream(addresses, curStartIdx);
      }
    }
  }
}
