package io.netty.handler.ssl;

import io.netty.util.internal.SuppressJava6Requirement;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import javax.net.ssl.SSLEngine;

















@SuppressJava6Requirement(reason="Usage guarded by java version check")
final class BouncyCastleAlpnSslEngine
  extends JdkAlpnSslEngine
{
  BouncyCastleAlpnSslEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer)
  {
    super(engine, applicationNegotiator, isServer, new BiConsumer()
    


      new BiConsumer
      {

        public void accept(SSLEngine e, JdkAlpnSslEngine.AlpnSelector s) {
          BouncyCastleAlpnSslUtils.setHandshakeApplicationProtocolSelector(e, s); } }, new BiConsumer()
      {

        public void accept(SSLEngine e, List<String> p)
        {

          BouncyCastleAlpnSslUtils.setApplicationProtocols(e, p);
        }
      });
  }
  
  public String getApplicationProtocol() {
    return BouncyCastleAlpnSslUtils.getApplicationProtocol(getWrappedEngine());
  }
  
  public String getHandshakeApplicationProtocol() {
    return BouncyCastleAlpnSslUtils.getHandshakeApplicationProtocol(getWrappedEngine());
  }
  
  public void setHandshakeApplicationProtocolSelector(BiFunction<SSLEngine, List<String>, String> selector) {
    BouncyCastleAlpnSslUtils.setHandshakeApplicationProtocolSelector(getWrappedEngine(), selector);
  }
  
  public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
    return BouncyCastleAlpnSslUtils.getHandshakeApplicationProtocolSelector(getWrappedEngine());
  }
}
