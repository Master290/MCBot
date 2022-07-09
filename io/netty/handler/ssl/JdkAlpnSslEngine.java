package io.netty.handler.ssl;

import io.netty.util.internal.SuppressJava6Requirement;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

















@SuppressJava6Requirement(reason="Usage guarded by java version check")
class JdkAlpnSslEngine
  extends JdkSslEngine
{
  private final JdkApplicationProtocolNegotiator.ProtocolSelectionListener selectionListener;
  private final AlpnSelector alpnSelector;
  
  final class AlpnSelector
    implements BiFunction<SSLEngine, List<String>, String>
  {
    private final JdkApplicationProtocolNegotiator.ProtocolSelector selector;
    private boolean called;
    
    AlpnSelector(JdkApplicationProtocolNegotiator.ProtocolSelector selector)
    {
      this.selector = selector;
    }
    
    public String apply(SSLEngine sslEngine, List<String> strings)
    {
      assert (!called);
      called = true;
      try
      {
        String selected = selector.select(strings);
        return selected == null ? "" : selected;
      }
      catch (Exception cause) {}
      


      return null;
    }
    
    void checkUnsupported()
    {
      if (called)
      {



        return;
      }
      String protocol = getApplicationProtocol();
      assert (protocol != null);
      
      if (protocol.isEmpty())
      {
        selector.unsupported();
      }
    }
  }
  


  JdkAlpnSslEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer, BiConsumer<SSLEngine, AlpnSelector> setHandshakeApplicationProtocolSelector, BiConsumer<SSLEngine, List<String>> setApplicationProtocols)
  {
    super(engine);
    if (isServer) {
      selectionListener = null;
      
      alpnSelector = new AlpnSelector(applicationNegotiator.protocolSelectorFactory().newSelector(this, new LinkedHashSet(applicationNegotiator.protocols())));
      setHandshakeApplicationProtocolSelector.accept(engine, alpnSelector);
    }
    else {
      selectionListener = applicationNegotiator.protocolListenerFactory().newListener(this, applicationNegotiator.protocols());
      alpnSelector = null;
      setApplicationProtocols.accept(engine, applicationNegotiator.protocols());
    }
  }
  

  JdkAlpnSslEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer)
  {
    this(engine, applicationNegotiator, isServer, new BiConsumer()
    


      new BiConsumer
      {

        public void accept(SSLEngine e, JdkAlpnSslEngine.AlpnSelector s) {
          JdkAlpnSslUtils.setHandshakeApplicationProtocolSelector(e, s); } }, new BiConsumer()
      {

        public void accept(SSLEngine e, List<String> p)
        {

          JdkAlpnSslUtils.setApplicationProtocols(e, p);
        }
      });
  }
  
  private SSLEngineResult verifyProtocolSelection(SSLEngineResult result) throws SSLException {
    if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
      if (alpnSelector == null)
      {
        try {
          String protocol = getApplicationProtocol();
          assert (protocol != null);
          if (protocol.isEmpty())
          {



            selectionListener.unsupported();
          } else {
            selectionListener.selected(protocol);
          }
        } catch (Throwable e) {
          throw SslUtils.toSSLHandshakeException(e);
        }
      } else {
        assert (selectionListener == null);
        alpnSelector.checkUnsupported();
      }
    }
    return result;
  }
  
  public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException
  {
    return verifyProtocolSelection(super.wrap(src, dst));
  }
  
  public SSLEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws SSLException
  {
    return verifyProtocolSelection(super.wrap(srcs, dst));
  }
  
  public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int len, ByteBuffer dst) throws SSLException
  {
    return verifyProtocolSelection(super.wrap(srcs, offset, len, dst));
  }
  
  public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException
  {
    return verifyProtocolSelection(super.unwrap(src, dst));
  }
  
  public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException
  {
    return verifyProtocolSelection(super.unwrap(src, dsts));
  }
  
  public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dst, int offset, int len) throws SSLException
  {
    return verifyProtocolSelection(super.unwrap(src, dst, offset, len));
  }
  


  void setNegotiatedApplicationProtocol(String applicationProtocol) {}
  

  public String getNegotiatedApplicationProtocol()
  {
    String protocol = getApplicationProtocol();
    if (protocol != null) {
      return protocol.isEmpty() ? null : protocol;
    }
    return null;
  }
  

  public String getApplicationProtocol()
  {
    return JdkAlpnSslUtils.getApplicationProtocol(getWrappedEngine());
  }
  
  public String getHandshakeApplicationProtocol() {
    return JdkAlpnSslUtils.getHandshakeApplicationProtocol(getWrappedEngine());
  }
  
  public void setHandshakeApplicationProtocolSelector(BiFunction<SSLEngine, List<String>, String> selector) {
    JdkAlpnSslUtils.setHandshakeApplicationProtocolSelector(getWrappedEngine(), selector);
  }
  
  public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
    return JdkAlpnSslUtils.getHandshakeApplicationProtocolSelector(getWrappedEngine());
  }
}
