package io.netty.handler.ssl;

import io.netty.internal.tcnative.SSL;
import io.netty.util.AsciiString;
import java.util.HashMap;
import java.util.Map;




















final class OpenSslClientSessionCache
  extends OpenSslSessionCache
{
  private final Map<HostPort, OpenSslSessionCache.NativeSslSession> sessions = new HashMap();
  
  OpenSslClientSessionCache(OpenSslEngineMap engineMap) {
    super(engineMap);
  }
  
  protected boolean sessionCreated(OpenSslSessionCache.NativeSslSession session)
  {
    assert (Thread.holdsLock(this));
    HostPort hostPort = keyFor(session.getPeerHost(), session.getPeerPort());
    if ((hostPort == null) || (sessions.containsKey(hostPort))) {
      return false;
    }
    sessions.put(hostPort, session);
    return true;
  }
  
  protected void sessionRemoved(OpenSslSessionCache.NativeSslSession session)
  {
    assert (Thread.holdsLock(this));
    HostPort hostPort = keyFor(session.getPeerHost(), session.getPeerPort());
    if (hostPort == null) {
      return;
    }
    sessions.remove(hostPort);
  }
  
  void setSession(long ssl, String host, int port)
  {
    HostPort hostPort = keyFor(host, port);
    if (hostPort == null) {
      return;
    }
    
    boolean reused;
    synchronized (this) {
      OpenSslSessionCache.NativeSslSession session = (OpenSslSessionCache.NativeSslSession)sessions.get(hostPort);
      if (session == null) {
        return;
      }
      if (!session.isValid()) {
        removeSessionWithId(session.sessionId());
        return;
      }
      

      reused = SSL.setSession(ssl, session.session()); }
    boolean reused;
    OpenSslSessionCache.NativeSslSession session;
    if (reused) {
      if (session.shouldBeSingleUse())
      {
        session.invalidate();
      }
      session.updateLastAccessedTime();
    }
  }
  
  private static HostPort keyFor(String host, int port) {
    if ((host == null) && (port < 1)) {
      return null;
    }
    return new HostPort(host, port);
  }
  
  synchronized void clear()
  {
    super.clear();
    sessions.clear();
  }
  

  private static final class HostPort
  {
    private final int hash;
    private final String host;
    private final int port;
    
    HostPort(String host, int port)
    {
      this.host = host;
      this.port = port;
      
      hash = (31 * AsciiString.hashCode(host) + port);
    }
    
    public int hashCode()
    {
      return hash;
    }
    
    public boolean equals(Object obj)
    {
      if (!(obj instanceof HostPort)) {
        return false;
      }
      HostPort other = (HostPort)obj;
      return (port == port) && (host.equalsIgnoreCase(host));
    }
    
    public String toString()
    {
      return "HostPort{host='" + host + '\'' + ", port=" + port + '}';
    }
  }
}
