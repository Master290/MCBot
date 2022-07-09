package io.netty.handler.codec.http.multipart;

import io.netty.handler.codec.http.HttpContent;
import java.util.List;

public abstract interface InterfaceHttpPostRequestDecoder
{
  public abstract boolean isMultipart();
  
  public abstract void setDiscardThreshold(int paramInt);
  
  public abstract int getDiscardThreshold();
  
  public abstract List<InterfaceHttpData> getBodyHttpDatas();
  
  public abstract List<InterfaceHttpData> getBodyHttpDatas(String paramString);
  
  public abstract InterfaceHttpData getBodyHttpData(String paramString);
  
  public abstract InterfaceHttpPostRequestDecoder offer(HttpContent paramHttpContent);
  
  public abstract boolean hasNext();
  
  public abstract InterfaceHttpData next();
  
  public abstract InterfaceHttpData currentPartialHttpData();
  
  public abstract void destroy();
  
  public abstract void cleanFiles();
  
  public abstract void removeHttpDataFromClean(InterfaceHttpData paramInterfaceHttpData);
}
