package io.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.util.internal.ObjectUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

















public class ChannelGroupException
  extends ChannelException
  implements Iterable<Map.Entry<Channel, Throwable>>
{
  private static final long serialVersionUID = -4093064295562629453L;
  private final Collection<Map.Entry<Channel, Throwable>> failed;
  
  public ChannelGroupException(Collection<Map.Entry<Channel, Throwable>> causes)
  {
    ObjectUtil.checkNonEmpty(causes, "causes");
    
    failed = Collections.unmodifiableCollection(causes);
  }
  




  public Iterator<Map.Entry<Channel, Throwable>> iterator()
  {
    return failed.iterator();
  }
}
