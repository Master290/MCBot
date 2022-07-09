package io.netty.handler.codec.http2;

import io.netty.util.internal.ObjectUtil;




























public abstract class AbstractInboundHttp2ToHttpAdapterBuilder<T extends InboundHttp2ToHttpAdapter, B extends AbstractInboundHttp2ToHttpAdapterBuilder<T, B>>
{
  private final Http2Connection connection;
  private int maxContentLength;
  private boolean validateHttpHeaders;
  private boolean propagateSettings;
  
  protected AbstractInboundHttp2ToHttpAdapterBuilder(Http2Connection connection)
  {
    this.connection = ((Http2Connection)ObjectUtil.checkNotNull(connection, "connection"));
  }
  
  protected final B self()
  {
    return this;
  }
  


  protected Http2Connection connection()
  {
    return connection;
  }
  


  protected int maxContentLength()
  {
    return maxContentLength;
  }
  






  protected B maxContentLength(int maxContentLength)
  {
    this.maxContentLength = maxContentLength;
    return self();
  }
  


  protected boolean isValidateHttpHeaders()
  {
    return validateHttpHeaders;
  }
  









  protected B validateHttpHeaders(boolean validate)
  {
    validateHttpHeaders = validate;
    return self();
  }
  


  protected boolean isPropagateSettings()
  {
    return propagateSettings;
  }
  






  protected B propagateSettings(boolean propagate)
  {
    propagateSettings = propagate;
    return self();
  }
  


  protected T build()
  {
    try
    {
      instance = build(connection(), maxContentLength(), 
        isValidateHttpHeaders(), isPropagateSettings());
    } catch (Throwable t) { T instance;
      throw new IllegalStateException("failed to create a new InboundHttp2ToHttpAdapter", t); }
    T instance;
    connection.addListener(instance);
    return instance;
  }
  
  protected abstract T build(Http2Connection paramHttp2Connection, int paramInt, boolean paramBoolean1, boolean paramBoolean2)
    throws Exception;
}
