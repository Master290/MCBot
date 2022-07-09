package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import javax.net.ssl.SSLEngine;




















@Deprecated
public final class JdkAlpnApplicationProtocolNegotiator
  extends JdkBaseApplicationProtocolNegotiator
{
  private static final boolean AVAILABLE = (Conscrypt.isAvailable()) || 
    (JdkAlpnSslUtils.supportsAlpn()) || 
    (JettyAlpnSslEngine.isAvailable()) || 
    (BouncyCastle.isAvailable());
  
  private static final JdkApplicationProtocolNegotiator.SslEngineWrapperFactory ALPN_WRAPPER = AVAILABLE ? new AlpnWrapper(null) : new FailureWrapper(null);
  



  public JdkAlpnApplicationProtocolNegotiator(Iterable<String> protocols)
  {
    this(false, protocols);
  }
  



  public JdkAlpnApplicationProtocolNegotiator(String... protocols)
  {
    this(false, protocols);
  }
  




  public JdkAlpnApplicationProtocolNegotiator(boolean failIfNoCommonProtocols, Iterable<String> protocols)
  {
    this(failIfNoCommonProtocols, failIfNoCommonProtocols, protocols);
  }
  




  public JdkAlpnApplicationProtocolNegotiator(boolean failIfNoCommonProtocols, String... protocols)
  {
    this(failIfNoCommonProtocols, failIfNoCommonProtocols, protocols);
  }
  






  public JdkAlpnApplicationProtocolNegotiator(boolean clientFailIfNoCommonProtocols, boolean serverFailIfNoCommonProtocols, Iterable<String> protocols)
  {
    this(serverFailIfNoCommonProtocols ? FAIL_SELECTOR_FACTORY : NO_FAIL_SELECTOR_FACTORY, clientFailIfNoCommonProtocols ? FAIL_SELECTION_LISTENER_FACTORY : NO_FAIL_SELECTION_LISTENER_FACTORY, protocols);
  }
  








  public JdkAlpnApplicationProtocolNegotiator(boolean clientFailIfNoCommonProtocols, boolean serverFailIfNoCommonProtocols, String... protocols)
  {
    this(serverFailIfNoCommonProtocols ? FAIL_SELECTOR_FACTORY : NO_FAIL_SELECTOR_FACTORY, clientFailIfNoCommonProtocols ? FAIL_SELECTION_LISTENER_FACTORY : NO_FAIL_SELECTION_LISTENER_FACTORY, protocols);
  }
  








  public JdkAlpnApplicationProtocolNegotiator(JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory, JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory, Iterable<String> protocols)
  {
    super(ALPN_WRAPPER, selectorFactory, listenerFactory, protocols);
  }
  






  public JdkAlpnApplicationProtocolNegotiator(JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory, JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory, String... protocols)
  {
    super(ALPN_WRAPPER, selectorFactory, listenerFactory, protocols);
  }
  
  private static final class FailureWrapper extends JdkApplicationProtocolNegotiator.AllocatorAwareSslEngineWrapperFactory {
    private FailureWrapper() {}
    
    public SSLEngine wrapSslEngine(SSLEngine engine, ByteBufAllocator alloc, JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer) {
      throw new RuntimeException("ALPN unsupported. Is your classpath configured correctly? For Conscrypt, add the appropriate Conscrypt JAR to classpath and set the security provider. For Jetty-ALPN, see https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-starting");
    }
  }
  
  private static final class AlpnWrapper
    extends JdkApplicationProtocolNegotiator.AllocatorAwareSslEngineWrapperFactory
  {
    private AlpnWrapper() {}
    
    public SSLEngine wrapSslEngine(SSLEngine engine, ByteBufAllocator alloc, JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer)
    {
      if (Conscrypt.isEngineSupported(engine)) {
        return isServer ? ConscryptAlpnSslEngine.newServerEngine(engine, alloc, applicationNegotiator) : 
          ConscryptAlpnSslEngine.newClientEngine(engine, alloc, applicationNegotiator);
      }
      if (BouncyCastle.isInUse(engine)) {
        return new BouncyCastleAlpnSslEngine(engine, applicationNegotiator, isServer);
      }
      



      if (JdkAlpnSslUtils.supportsAlpn()) {
        return new JdkAlpnSslEngine(engine, applicationNegotiator, isServer);
      }
      if (JettyAlpnSslEngine.isAvailable()) {
        return isServer ? JettyAlpnSslEngine.newServerEngine(engine, applicationNegotiator) : 
          JettyAlpnSslEngine.newClientEngine(engine, applicationNegotiator);
      }
      
      throw new UnsupportedOperationException("ALPN not supported. Unable to wrap SSLEngine of type '" + engine.getClass().getName() + "')");
    }
  }
  
  static boolean isAlpnSupported() {
    return AVAILABLE;
  }
}
