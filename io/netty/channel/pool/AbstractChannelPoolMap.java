package io.netty.channel.pool;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReadOnlyIterator;
import java.io.Closeable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;




















public abstract class AbstractChannelPoolMap<K, P extends ChannelPool>
  implements ChannelPoolMap<K, P>, Iterable<Map.Entry<K, P>>, Closeable
{
  private final ConcurrentMap<K, P> map = PlatformDependent.newConcurrentHashMap();
  
  public AbstractChannelPoolMap() {}
  
  public final P get(K key) { P pool = (ChannelPool)map.get(ObjectUtil.checkNotNull(key, "key"));
    if (pool == null) {
      pool = newPool(key);
      P old = (ChannelPool)map.putIfAbsent(key, pool);
      if (old != null)
      {
        poolCloseAsyncIfSupported(pool);
        pool = old;
      }
    }
    return pool;
  }
  







  public final boolean remove(K key)
  {
    P pool = (ChannelPool)map.remove(ObjectUtil.checkNotNull(key, "key"));
    if (pool != null) {
      poolCloseAsyncIfSupported(pool);
      return true;
    }
    return false;
  }
  






  private Future<Boolean> removeAsyncIfSupported(K key)
  {
    P pool = (ChannelPool)map.remove(ObjectUtil.checkNotNull(key, "key"));
    if (pool != null) {
      final Promise<Boolean> removePromise = GlobalEventExecutor.INSTANCE.newPromise();
      poolCloseAsyncIfSupported(pool).addListener(new GenericFutureListener()
      {
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            removePromise.setSuccess(Boolean.TRUE);
          } else {
            removePromise.setFailure(future.cause());
          }
        }
      });
      return removePromise;
    }
    return GlobalEventExecutor.INSTANCE.newSucceededFuture(Boolean.FALSE);
  }
  





  private static Future<Void> poolCloseAsyncIfSupported(ChannelPool pool)
  {
    if ((pool instanceof SimpleChannelPool)) {
      return ((SimpleChannelPool)pool).closeAsync();
    }
    try {
      pool.close();
      return GlobalEventExecutor.INSTANCE.newSucceededFuture(null);
    } catch (Exception e) {
      return GlobalEventExecutor.INSTANCE.newFailedFuture(e);
    }
  }
  

  public final Iterator<Map.Entry<K, P>> iterator()
  {
    return new ReadOnlyIterator(map.entrySet().iterator());
  }
  


  public final int size()
  {
    return map.size();
  }
  


  public final boolean isEmpty()
  {
    return map.isEmpty();
  }
  
  public final boolean contains(K key)
  {
    return map.containsKey(ObjectUtil.checkNotNull(key, "key"));
  }
  


  protected abstract P newPool(K paramK);
  

  public final void close()
  {
    for (K key : map.keySet())
    {
      removeAsyncIfSupported(key).syncUninterruptibly();
    }
  }
}
