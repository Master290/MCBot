package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.TypeParameterMatcher;
import java.net.SocketAddress;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Collections;
import java.util.List;
























public abstract class AbstractAddressResolver<T extends SocketAddress>
  implements AddressResolver<T>
{
  private final EventExecutor executor;
  private final TypeParameterMatcher matcher;
  
  protected AbstractAddressResolver(EventExecutor executor)
  {
    this.executor = ((EventExecutor)ObjectUtil.checkNotNull(executor, "executor"));
    matcher = TypeParameterMatcher.find(this, AbstractAddressResolver.class, "T");
  }
  




  protected AbstractAddressResolver(EventExecutor executor, Class<? extends T> addressType)
  {
    this.executor = ((EventExecutor)ObjectUtil.checkNotNull(executor, "executor"));
    matcher = TypeParameterMatcher.get(addressType);
  }
  



  protected EventExecutor executor()
  {
    return executor;
  }
  
  public boolean isSupported(SocketAddress address)
  {
    return matcher.match(address);
  }
  
  public final boolean isResolved(SocketAddress address)
  {
    if (!isSupported(address)) {
      throw new UnsupportedAddressTypeException();
    }
    

    T castAddress = address;
    return doIsResolved(castAddress);
  }
  


  protected abstract boolean doIsResolved(T paramT);
  


  public final Future<T> resolve(SocketAddress address)
  {
    if (!isSupported((SocketAddress)ObjectUtil.checkNotNull(address, "address")))
    {
      return executor().newFailedFuture(new UnsupportedAddressTypeException());
    }
    
    if (isResolved(address))
    {

      T cast = address;
      return executor.newSucceededFuture(cast);
    }
    
    try
    {
      T cast = address;
      Promise<T> promise = executor().newPromise();
      doResolve(cast, promise);
      return promise;
    } catch (Exception e) {
      return executor().newFailedFuture(e);
    }
  }
  
  public final Future<T> resolve(SocketAddress address, Promise<T> promise)
  {
    ObjectUtil.checkNotNull(address, "address");
    ObjectUtil.checkNotNull(promise, "promise");
    
    if (!isSupported(address))
    {
      return promise.setFailure(new UnsupportedAddressTypeException());
    }
    
    if (isResolved(address))
    {

      T cast = address;
      return promise.setSuccess(cast);
    }
    
    try
    {
      T cast = address;
      doResolve(cast, promise);
      return promise;
    } catch (Exception e) {
      return promise.setFailure(e);
    }
  }
  
  public final Future<List<T>> resolveAll(SocketAddress address)
  {
    if (!isSupported((SocketAddress)ObjectUtil.checkNotNull(address, "address")))
    {
      return executor().newFailedFuture(new UnsupportedAddressTypeException());
    }
    
    if (isResolved(address))
    {

      T cast = address;
      return executor.newSucceededFuture(Collections.singletonList(cast));
    }
    
    try
    {
      T cast = address;
      Promise<List<T>> promise = executor().newPromise();
      doResolveAll(cast, promise);
      return promise;
    } catch (Exception e) {
      return executor().newFailedFuture(e);
    }
  }
  
  public final Future<List<T>> resolveAll(SocketAddress address, Promise<List<T>> promise)
  {
    ObjectUtil.checkNotNull(address, "address");
    ObjectUtil.checkNotNull(promise, "promise");
    
    if (!isSupported(address))
    {
      return promise.setFailure(new UnsupportedAddressTypeException());
    }
    
    if (isResolved(address))
    {

      T cast = address;
      return promise.setSuccess(Collections.singletonList(cast));
    }
    
    try
    {
      T cast = address;
      doResolveAll(cast, promise);
      return promise;
    } catch (Exception e) {
      return promise.setFailure(e);
    }
  }
  
  protected abstract void doResolve(T paramT, Promise<T> paramPromise)
    throws Exception;
  
  protected abstract void doResolveAll(T paramT, Promise<List<T>> paramPromise)
    throws Exception;
  
  public void close() {}
}
