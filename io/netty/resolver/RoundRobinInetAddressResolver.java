package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.PlatformDependent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

























public class RoundRobinInetAddressResolver
  extends InetNameResolver
{
  private final NameResolver<InetAddress> nameResolver;
  
  public RoundRobinInetAddressResolver(EventExecutor executor, NameResolver<InetAddress> nameResolver)
  {
    super(executor);
    this.nameResolver = nameResolver;
  }
  


  protected void doResolve(final String inetHost, final Promise<InetAddress> promise)
    throws Exception
  {
    nameResolver.resolveAll(inetHost).addListener(new FutureListener()
    {
      public void operationComplete(Future<List<InetAddress>> future) throws Exception {
        if (future.isSuccess()) {
          List<InetAddress> inetAddresses = (List)future.getNow();
          int numAddresses = inetAddresses.size();
          if (numAddresses > 0)
          {

            promise.setSuccess(inetAddresses.get(RoundRobinInetAddressResolver.randomIndex(numAddresses)));
          } else {
            promise.setFailure(new UnknownHostException(inetHost));
          }
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
  }
  
  protected void doResolveAll(String inetHost, final Promise<List<InetAddress>> promise) throws Exception
  {
    nameResolver.resolveAll(inetHost).addListener(new FutureListener()
    {
      public void operationComplete(Future<List<InetAddress>> future) throws Exception {
        if (future.isSuccess()) {
          List<InetAddress> inetAddresses = (List)future.getNow();
          if (!inetAddresses.isEmpty())
          {
            List<InetAddress> result = new ArrayList(inetAddresses);
            
            Collections.rotate(result, RoundRobinInetAddressResolver.randomIndex(inetAddresses.size()));
            promise.setSuccess(result);
          } else {
            promise.setSuccess(inetAddresses);
          }
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
  }
  
  private static int randomIndex(int numAddresses) {
    return numAddresses == 1 ? 0 : PlatformDependent.threadLocalRandom().nextInt(numAddresses);
  }
  
  public void close()
  {
    nameResolver.close();
  }
}
