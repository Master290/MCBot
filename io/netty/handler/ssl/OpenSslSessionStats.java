package io.netty.handler.ssl;

import io.netty.internal.tcnative.SSLContext;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

























public final class OpenSslSessionStats
{
  private final ReferenceCountedOpenSslContext context;
  
  OpenSslSessionStats(ReferenceCountedOpenSslContext context)
  {
    this.context = context;
  }
  


  public long number()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionNumber(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long connect()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionConnect(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long connectGood()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionConnectGood(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long connectRenegotiate()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionConnectRenegotiate(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long accept()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionAccept(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long acceptGood()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionAcceptGood(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long acceptRenegotiate()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionAcceptRenegotiate(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  




  public long hits()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionHits(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long cbHits()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionCbHits(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  



  public long misses()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionMisses(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  




  public long timeouts()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionTimeouts(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long cacheFull()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionCacheFull(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long ticketKeyFail()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionTicketKeyFail(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long ticketKeyNew()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionTicketKeyNew(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  



  public long ticketKeyRenew()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionTicketKeyRenew(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
  


  public long ticketKeyResume()
  {
    Lock readerLock = context.ctxLock.readLock();
    readerLock.lock();
    try {
      return SSLContext.sessionTicketKeyResume(context.ctx);
    } finally {
      readerLock.unlock();
    }
  }
}
