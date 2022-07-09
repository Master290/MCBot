package io.netty.handler.codec.http.multipart;

import io.netty.handler.codec.http.HttpRequest;
import java.nio.charset.Charset;

public abstract interface HttpDataFactory
{
  public abstract void setMaxLimit(long paramLong);
  
  public abstract Attribute createAttribute(HttpRequest paramHttpRequest, String paramString);
  
  public abstract Attribute createAttribute(HttpRequest paramHttpRequest, String paramString, long paramLong);
  
  public abstract Attribute createAttribute(HttpRequest paramHttpRequest, String paramString1, String paramString2);
  
  public abstract FileUpload createFileUpload(HttpRequest paramHttpRequest, String paramString1, String paramString2, String paramString3, String paramString4, Charset paramCharset, long paramLong);
  
  public abstract void removeHttpDataFromClean(HttpRequest paramHttpRequest, InterfaceHttpData paramInterfaceHttpData);
  
  public abstract void cleanRequestHttpData(HttpRequest paramHttpRequest);
  
  public abstract void cleanAllHttpData();
  
  @Deprecated
  public abstract void cleanRequestHttpDatas(HttpRequest paramHttpRequest);
  
  @Deprecated
  public abstract void cleanAllHttpDatas();
}
