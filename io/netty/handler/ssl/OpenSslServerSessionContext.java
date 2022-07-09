package io.netty.handler.ssl;

import io.netty.internal.tcnative.SSL;
import io.netty.internal.tcnative.SSLContext;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

















public final class OpenSslServerSessionContext
  extends OpenSslSessionContext
{
  OpenSslServerSessionContext(ReferenceCountedOpenSslContext context, OpenSslKeyMaterialProvider provider)
  {
    super(context, provider, SSL.SSL_SESS_CACHE_SERVER, new OpenSslSessionCache(engineMap));
  }
  








  public boolean setSessionIdContext(byte[] sidCtx)
  {
    Lock writerLock = context.ctxLock.writeLock();
    writerLock.lock();
    try {
      return SSLContext.setSessionIdContext(context.ctx, sidCtx);
    } finally {
      writerLock.unlock();
    }
  }
}
