package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;




@Deprecated
public abstract interface JdkApplicationProtocolNegotiator
  extends ApplicationProtocolNegotiator
{
  public abstract SslEngineWrapperFactory wrapperFactory();
  
  public abstract ProtocolSelectorFactory protocolSelectorFactory();
  
  public abstract ProtocolSelectionListenerFactory protocolListenerFactory();
  
  public static abstract interface ProtocolSelectionListenerFactory
  {
    public abstract JdkApplicationProtocolNegotiator.ProtocolSelectionListener newListener(SSLEngine paramSSLEngine, List<String> paramList);
  }
  
  public static abstract interface ProtocolSelectorFactory
  {
    public abstract JdkApplicationProtocolNegotiator.ProtocolSelector newSelector(SSLEngine paramSSLEngine, Set<String> paramSet);
  }
  
  public static abstract interface ProtocolSelectionListener
  {
    public abstract void unsupported();
    
    public abstract void selected(String paramString)
      throws Exception;
  }
  
  public static abstract interface ProtocolSelector
  {
    public abstract void unsupported();
    
    public abstract String select(List<String> paramList)
      throws Exception;
  }
  
  public static abstract class AllocatorAwareSslEngineWrapperFactory
    implements JdkApplicationProtocolNegotiator.SslEngineWrapperFactory
  {
    public AllocatorAwareSslEngineWrapperFactory() {}
    
    public final SSLEngine wrapSslEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer)
    {
      return wrapSslEngine(engine, ByteBufAllocator.DEFAULT, applicationNegotiator, isServer);
    }
    
    abstract SSLEngine wrapSslEngine(SSLEngine paramSSLEngine, ByteBufAllocator paramByteBufAllocator, JdkApplicationProtocolNegotiator paramJdkApplicationProtocolNegotiator, boolean paramBoolean);
  }
  
  public static abstract interface SslEngineWrapperFactory
  {
    public abstract SSLEngine wrapSslEngine(SSLEngine paramSSLEngine, JdkApplicationProtocolNegotiator paramJdkApplicationProtocolNegotiator, boolean paramBoolean);
  }
}
