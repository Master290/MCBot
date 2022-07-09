package io.netty.handler.ssl;

import io.netty.internal.tcnative.SSL;
import io.netty.internal.tcnative.SSLContext;
import io.netty.internal.tcnative.SessionTicketKey;
import io.netty.util.internal.ObjectUtil;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;



























public abstract class OpenSslSessionContext
  implements SSLSessionContext
{
  private final OpenSslSessionStats stats;
  private final OpenSslKeyMaterialProvider provider;
  final ReferenceCountedOpenSslContext context;
  private final OpenSslSessionCache sessionCache;
  private final long mask;
  
  OpenSslSessionContext(ReferenceCountedOpenSslContext context, OpenSslKeyMaterialProvider provider, long mask, OpenSslSessionCache cache)
  {
    this.context = context;
    this.provider = provider;
    this.mask = mask;
    stats = new OpenSslSessionStats(context);
    sessionCache = cache;
    SSLContext.setSSLSessionCache(ctx, cache);
  }
  
  final boolean useKeyManager() {
    return provider != null;
  }
  
  public void setSessionCacheSize(int size)
  {
    ObjectUtil.checkPositiveOrZero(size, "size");
    sessionCache.setSessionCacheSize(size);
  }
  
  public int getSessionCacheSize()
  {
    return sessionCache.getSessionCacheSize();
  }
  
  public void setSessionTimeout(int seconds)
  {
    ObjectUtil.checkPositiveOrZero(seconds, "seconds");
    
    Lock writerLock = context.ctxLock.writeLock();
    writerLock.lock();
    try {
      SSLContext.setSessionCacheTimeout(context.ctx, seconds);
      sessionCache.setSessionTimeout(seconds);
      
      writerLock.unlock(); } finally { writerLock.unlock();
    }
  }
  
  public int getSessionTimeout()
  {
    return sessionCache.getSessionTimeout();
  }
  
  public SSLSession getSession(byte[] bytes)
  {
    return sessionCache.getSession(new OpenSslSessionId(bytes));
  }
  
  public Enumeration<byte[]> getIds()
  {
    new Enumeration() {
      private final Iterator<OpenSslSessionId> ids = sessionCache.getIds().iterator();
      
      public boolean hasMoreElements() {
        return ids.hasNext();
      }
      
      public byte[] nextElement()
      {
        return ((OpenSslSessionId)ids.next()).cloneBytes();
      }
    };
  }
  



  @Deprecated
  public void setTicketKeys(byte[] keys)
  {
    if (keys.length % 48 != 0) {
      throw new IllegalArgumentException("keys.length % 48 != 0");
    }
    SessionTicketKey[] tickets = new SessionTicketKey[keys.length / 48];
    int i = 0; for (int a = 0; i < tickets.length; i++) {
      byte[] name = Arrays.copyOfRange(keys, a, 16);
      a += 16;
      byte[] hmacKey = Arrays.copyOfRange(keys, a, 16);
      i += 16;
      byte[] aesKey = Arrays.copyOfRange(keys, a, 16);
      a += 16;
      tickets[i] = new SessionTicketKey(name, hmacKey, aesKey);
    }
    Lock writerLock = context.ctxLock.writeLock();
    writerLock.lock();
    try {
      SSLContext.clearOptions(context.ctx, SSL.SSL_OP_NO_TICKET);
      SSLContext.setSessionTicketKeys(context.ctx, tickets);
    } finally {
      writerLock.unlock();
    }
  }
  






  public void setTicketKeys(OpenSslSessionTicketKey... keys)
  {
    ObjectUtil.checkNotNull(keys, "keys");
    SessionTicketKey[] ticketKeys = new SessionTicketKey[keys.length];
    for (int i = 0; i < ticketKeys.length; i++) {
      ticketKeys[i] = key;
    }
    Lock writerLock = context.ctxLock.writeLock();
    writerLock.lock();
    try {
      SSLContext.clearOptions(context.ctx, SSL.SSL_OP_NO_TICKET);
      if (ticketKeys.length > 0) {
        SSLContext.setSessionTicketKeys(context.ctx, ticketKeys);
      }
    } finally {
      writerLock.unlock();
    }
  }
  


  public void setSessionCacheEnabled(boolean enabled)
  {
    long mode = enabled ? mask | SSL.SSL_SESS_CACHE_NO_INTERNAL_LOOKUP | SSL.SSL_SESS_CACHE_NO_INTERNAL_STORE : SSL.SSL_SESS_CACHE_OFF;
    
    Lock writerLock = context.ctxLock.writeLock();
    writerLock.lock();
    try {
      SSLContext.setSessionCacheMode(context.ctx, mode);
      if (!enabled) {
        sessionCache.clear();
      }
    } finally {
      writerLock.unlock();
    }
  }
  


  public boolean isSessionCacheEnabled()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return (SSLContext.getSessionCacheMode(context.ctx) & mask) != 0L;
    } finally {
      readerLock.unlock();
    }
  }
  


  public OpenSslSessionStats stats()
  {
    return stats;
  }
  


  final void removeFromCache(OpenSslSessionId id)
  {
    sessionCache.removeSessionWithId(id);
  }
  
  final boolean isInCache(OpenSslSessionId id) {
    return sessionCache.containsSessionWithId(id);
  }
  
  void setSessionFromCache(String host, int port, long ssl) {
    sessionCache.setSession(ssl, host, port);
  }
  
  final void destroy() {
    if (provider != null) {
      provider.destroy();
    }
    sessionCache.clear();
  }
}
