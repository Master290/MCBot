package io.netty.handler.ssl;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.util.LinkedHashSet;
import java.util.List;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.ALPN.ClientProvider;
import org.eclipse.jetty.alpn.ALPN.ServerProvider;



















abstract class JettyAlpnSslEngine
  extends JdkSslEngine
{
  private static final boolean available = ;
  
  static boolean isAvailable() {
    return available;
  }
  
  private static boolean initAvailable() {
    if (PlatformDependent.javaVersion() <= 8) {
      try
      {
        Class.forName("sun.security.ssl.ALPNExtension", true, null);
        return true;
      }
      catch (Throwable localThrowable) {}
    }
    
    return false;
  }
  
  static JettyAlpnSslEngine newClientEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator)
  {
    return new ClientEngine(engine, applicationNegotiator);
  }
  
  static JettyAlpnSslEngine newServerEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator)
  {
    return new ServerEngine(engine, applicationNegotiator);
  }
  
  private JettyAlpnSslEngine(SSLEngine engine) {
    super(engine);
  }
  
  private static final class ClientEngine extends JettyAlpnSslEngine {
    ClientEngine(SSLEngine engine, final JdkApplicationProtocolNegotiator applicationNegotiator) {
      super(null);
      ObjectUtil.checkNotNull(applicationNegotiator, "applicationNegotiator");
      final JdkApplicationProtocolNegotiator.ProtocolSelectionListener protocolListener = (JdkApplicationProtocolNegotiator.ProtocolSelectionListener)ObjectUtil.checkNotNull(applicationNegotiator
        .protocolListenerFactory().newListener(this, applicationNegotiator.protocols()), "protocolListener");
      
      ALPN.put(engine, new ALPN.ClientProvider()
      {
        public List<String> protocols() {
          return applicationNegotiator.protocols();
        }
        
        public void selected(String protocol) throws SSLException
        {
          try {
            protocolListener.selected(protocol);
          } catch (Throwable t) {
            throw SslUtils.toSSLHandshakeException(t);
          }
        }
        
        public void unsupported()
        {
          protocolListener.unsupported();
        }
      });
    }
    
    public void closeInbound() throws SSLException
    {
      try {
        ALPN.remove(getWrappedEngine());
        
        super.closeInbound(); } finally { super.closeInbound();
      }
    }
    
    public void closeOutbound()
    {
      try {
        ALPN.remove(getWrappedEngine());
        
        super.closeOutbound(); } finally { super.closeOutbound();
      }
    }
  }
  
  private static final class ServerEngine extends JettyAlpnSslEngine {
    ServerEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator) {
      super(null);
      ObjectUtil.checkNotNull(applicationNegotiator, "applicationNegotiator");
      final JdkApplicationProtocolNegotiator.ProtocolSelector protocolSelector = (JdkApplicationProtocolNegotiator.ProtocolSelector)ObjectUtil.checkNotNull(applicationNegotiator.protocolSelectorFactory()
        .newSelector(this, new LinkedHashSet(applicationNegotiator.protocols())), "protocolSelector");
      
      ALPN.put(engine, new ALPN.ServerProvider()
      {
        public String select(List<String> protocols) throws SSLException {
          try {
            return protocolSelector.select(protocols);
          } catch (Throwable t) {
            throw SslUtils.toSSLHandshakeException(t);
          }
        }
        
        public void unsupported()
        {
          protocolSelector.unsupported();
        }
      });
    }
    
    public void closeInbound() throws SSLException
    {
      try {
        ALPN.remove(getWrappedEngine());
        
        super.closeInbound(); } finally { super.closeInbound();
      }
    }
    
    public void closeOutbound()
    {
      try {
        ALPN.remove(getWrappedEngine());
        
        super.closeOutbound(); } finally { super.closeOutbound();
      }
    }
  }
}
