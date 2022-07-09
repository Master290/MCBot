package io.netty.handler.codec.http.multipart;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;












































public class DefaultHttpDataFactory
  implements HttpDataFactory
{
  public static final long MINSIZE = 16384L;
  public static final long MAXSIZE = -1L;
  private final boolean useDisk;
  private final boolean checkSize;
  private long minSize;
  private long maxSize = -1L;
  
  private Charset charset = HttpConstants.DEFAULT_CHARSET;
  



  private String baseDir;
  



  private boolean deleteOnExit;
  


  private final Map<HttpRequest, List<HttpData>> requestFileDeleteMap = Collections.synchronizedMap(new IdentityHashMap());
  



  public DefaultHttpDataFactory()
  {
    useDisk = false;
    checkSize = true;
    minSize = 16384L;
  }
  
  public DefaultHttpDataFactory(Charset charset) {
    this();
    this.charset = charset;
  }
  


  public DefaultHttpDataFactory(boolean useDisk)
  {
    this.useDisk = useDisk;
    checkSize = false;
  }
  
  public DefaultHttpDataFactory(boolean useDisk, Charset charset) {
    this(useDisk);
    this.charset = charset;
  }
  


  public DefaultHttpDataFactory(long minSize)
  {
    useDisk = false;
    checkSize = true;
    this.minSize = minSize;
  }
  
  public DefaultHttpDataFactory(long minSize, Charset charset) {
    this(minSize);
    this.charset = charset;
  }
  




  public void setBaseDir(String baseDir)
  {
    this.baseDir = baseDir;
  }
  





  public void setDeleteOnExit(boolean deleteOnExit)
  {
    this.deleteOnExit = deleteOnExit;
  }
  
  public void setMaxLimit(long maxSize)
  {
    this.maxSize = maxSize;
  }
  


  private List<HttpData> getList(HttpRequest request)
  {
    List<HttpData> list = (List)requestFileDeleteMap.get(request);
    if (list == null) {
      list = new ArrayList();
      requestFileDeleteMap.put(request, list);
    }
    return list;
  }
  
  public Attribute createAttribute(HttpRequest request, String name)
  {
    if (useDisk) {
      Attribute attribute = new DiskAttribute(name, charset, baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
      List<HttpData> list = getList(request);
      list.add(attribute);
      return attribute;
    }
    if (checkSize) {
      Attribute attribute = new MixedAttribute(name, minSize, charset, baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
      List<HttpData> list = getList(request);
      list.add(attribute);
      return attribute;
    }
    MemoryAttribute attribute = new MemoryAttribute(name);
    attribute.setMaxSize(maxSize);
    return attribute;
  }
  
  public Attribute createAttribute(HttpRequest request, String name, long definedSize)
  {
    if (useDisk) {
      Attribute attribute = new DiskAttribute(name, definedSize, charset, baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
      List<HttpData> list = getList(request);
      list.add(attribute);
      return attribute;
    }
    if (checkSize) {
      Attribute attribute = new MixedAttribute(name, definedSize, minSize, charset, baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
      List<HttpData> list = getList(request);
      list.add(attribute);
      return attribute;
    }
    MemoryAttribute attribute = new MemoryAttribute(name, definedSize);
    attribute.setMaxSize(maxSize);
    return attribute;
  }
  

  private static void checkHttpDataSize(HttpData data)
  {
    try
    {
      data.checkSize(data.length());
    } catch (IOException ignored) {
      throw new IllegalArgumentException("Attribute bigger than maxSize allowed");
    }
  }
  
  public Attribute createAttribute(HttpRequest request, String name, String value)
  {
    if (useDisk) {
      Attribute attribute;
      try {
        Attribute attribute = new DiskAttribute(name, value, charset, baseDir, deleteOnExit);
        attribute.setMaxSize(maxSize);
      }
      catch (IOException e) {
        attribute = new MixedAttribute(name, value, minSize, charset, baseDir, deleteOnExit);
        attribute.setMaxSize(maxSize);
      }
      checkHttpDataSize(attribute);
      List<HttpData> list = getList(request);
      list.add(attribute);
      return attribute;
    }
    if (checkSize) {
      Attribute attribute = new MixedAttribute(name, value, minSize, charset, baseDir, deleteOnExit);
      attribute.setMaxSize(maxSize);
      checkHttpDataSize(attribute);
      List<HttpData> list = getList(request);
      list.add(attribute);
      return attribute;
    }
    try {
      MemoryAttribute attribute = new MemoryAttribute(name, value, charset);
      attribute.setMaxSize(maxSize);
      checkHttpDataSize(attribute);
      return attribute;
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
  


  public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size)
  {
    if (useDisk) {
      FileUpload fileUpload = new DiskFileUpload(name, filename, contentType, contentTransferEncoding, charset, size, baseDir, deleteOnExit);
      
      fileUpload.setMaxSize(maxSize);
      checkHttpDataSize(fileUpload);
      List<HttpData> list = getList(request);
      list.add(fileUpload);
      return fileUpload;
    }
    if (checkSize) {
      FileUpload fileUpload = new MixedFileUpload(name, filename, contentType, contentTransferEncoding, charset, size, minSize, baseDir, deleteOnExit);
      
      fileUpload.setMaxSize(maxSize);
      checkHttpDataSize(fileUpload);
      List<HttpData> list = getList(request);
      list.add(fileUpload);
      return fileUpload;
    }
    MemoryFileUpload fileUpload = new MemoryFileUpload(name, filename, contentType, contentTransferEncoding, charset, size);
    
    fileUpload.setMaxSize(maxSize);
    checkHttpDataSize(fileUpload);
    return fileUpload;
  }
  
  public void removeHttpDataFromClean(HttpRequest request, InterfaceHttpData data)
  {
    if (!(data instanceof HttpData)) {
      return;
    }
    


    List<HttpData> list = (List)requestFileDeleteMap.get(request);
    if (list == null) {
      return;
    }
    


    Iterator<HttpData> i = list.iterator();
    while (i.hasNext()) {
      HttpData n = (HttpData)i.next();
      if (n == data) {
        i.remove();
        

        if (list.isEmpty()) {
          requestFileDeleteMap.remove(request);
        }
        
        return;
      }
    }
  }
  
  public void cleanRequestHttpData(HttpRequest request)
  {
    List<HttpData> list = (List)requestFileDeleteMap.remove(request);
    if (list != null) {
      for (HttpData data : list) {
        data.release();
      }
    }
  }
  
  public void cleanAllHttpData()
  {
    Iterator<Map.Entry<HttpRequest, List<HttpData>>> i = requestFileDeleteMap.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry<HttpRequest, List<HttpData>> e = (Map.Entry)i.next();
      



      List<HttpData> list = (List)e.getValue();
      for (HttpData data : list) {
        data.release();
      }
      
      i.remove();
    }
  }
  
  public void cleanRequestHttpDatas(HttpRequest request)
  {
    cleanRequestHttpData(request);
  }
  
  public void cleanAllHttpDatas()
  {
    cleanAllHttpData();
  }
}
