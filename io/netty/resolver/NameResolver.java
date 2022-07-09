package io.netty.resolver;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.util.List;

public abstract interface NameResolver<T>
  extends Closeable
{
  public abstract Future<T> resolve(String paramString);
  
  public abstract Future<T> resolve(String paramString, Promise<T> paramPromise);
  
  public abstract Future<List<T>> resolveAll(String paramString);
  
  public abstract Future<List<T>> resolveAll(String paramString, Promise<List<T>> paramPromise);
  
  public abstract void close();
}
