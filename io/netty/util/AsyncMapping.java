package io.netty.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public abstract interface AsyncMapping<IN, OUT>
{
  public abstract Future<OUT> map(IN paramIN, Promise<OUT> paramPromise);
}
