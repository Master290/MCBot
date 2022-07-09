package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;














































public class HttpPostStandardRequestDecoder
  implements InterfaceHttpPostRequestDecoder
{
  private final HttpDataFactory factory;
  private final HttpRequest request;
  private final Charset charset;
  private boolean isLastChunk;
  private final List<InterfaceHttpData> bodyListHttpData = new ArrayList();
  



  private final Map<String, List<InterfaceHttpData>> bodyMapHttpData = new TreeMap(CaseIgnoringComparator.INSTANCE);
  




  private ByteBuf undecodedChunk;
  



  private int bodyListHttpDataRank;
  



  private HttpPostRequestDecoder.MultiPartStatus currentStatus = HttpPostRequestDecoder.MultiPartStatus.NOTSTARTED;
  

  private Attribute currentAttribute;
  

  private boolean destroyed;
  

  private int discardThreshold = 10485760;
  









  public HttpPostStandardRequestDecoder(HttpRequest request)
  {
    this(new DefaultHttpDataFactory(16384L), request, HttpConstants.DEFAULT_CHARSET);
  }
  











  public HttpPostStandardRequestDecoder(HttpDataFactory factory, HttpRequest request)
  {
    this(factory, request, HttpConstants.DEFAULT_CHARSET);
  }
  













  public HttpPostStandardRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset)
  {
    this.request = ((HttpRequest)ObjectUtil.checkNotNull(request, "request"));
    this.charset = ((Charset)ObjectUtil.checkNotNull(charset, "charset"));
    this.factory = ((HttpDataFactory)ObjectUtil.checkNotNull(factory, "factory"));
    try {
      if ((request instanceof HttpContent))
      {

        offer((HttpContent)request);
      } else {
        parseBody();
      }
    } catch (Throwable e) {
      destroy();
      PlatformDependent.throwException(e);
    }
  }
  
  private void checkDestroyed() {
    if (destroyed) {
      throw new IllegalStateException(HttpPostStandardRequestDecoder.class.getSimpleName() + " was destroyed already");
    }
  }
  






  public boolean isMultipart()
  {
    checkDestroyed();
    return false;
  }
  





  public void setDiscardThreshold(int discardThreshold)
  {
    this.discardThreshold = ObjectUtil.checkPositiveOrZero(discardThreshold, "discardThreshold");
  }
  



  public int getDiscardThreshold()
  {
    return discardThreshold;
  }
  










  public List<InterfaceHttpData> getBodyHttpDatas()
  {
    checkDestroyed();
    
    if (!isLastChunk) {
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
    }
    return bodyListHttpData;
  }
  











  public List<InterfaceHttpData> getBodyHttpDatas(String name)
  {
    checkDestroyed();
    
    if (!isLastChunk) {
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
    }
    return (List)bodyMapHttpData.get(name);
  }
  












  public InterfaceHttpData getBodyHttpData(String name)
  {
    checkDestroyed();
    
    if (!isLastChunk) {
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
    }
    List<InterfaceHttpData> list = (List)bodyMapHttpData.get(name);
    if (list != null) {
      return (InterfaceHttpData)list.get(0);
    }
    return null;
  }
  









  public HttpPostStandardRequestDecoder offer(HttpContent content)
  {
    checkDestroyed();
    
    if ((content instanceof LastHttpContent)) {
      isLastChunk = true;
    }
    
    ByteBuf buf = content.content();
    if (undecodedChunk == null)
    {




      undecodedChunk = buf.alloc().buffer(buf.readableBytes()).writeBytes(buf);
    } else {
      undecodedChunk.writeBytes(buf);
    }
    parseBody();
    if ((undecodedChunk != null) && (undecodedChunk.writerIndex() > discardThreshold)) {
      if (undecodedChunk.refCnt() == 1)
      {
        undecodedChunk.discardReadBytes();
      }
      else
      {
        ByteBuf buffer = undecodedChunk.alloc().buffer(undecodedChunk.readableBytes());
        buffer.writeBytes(undecodedChunk);
        undecodedChunk.release();
        undecodedChunk = buffer;
      }
    }
    return this;
  }
  










  public boolean hasNext()
  {
    checkDestroyed();
    
    if (currentStatus == HttpPostRequestDecoder.MultiPartStatus.EPILOGUE)
    {
      if (bodyListHttpDataRank >= bodyListHttpData.size()) {
        throw new HttpPostRequestDecoder.EndOfDataDecoderException();
      }
    }
    return (!bodyListHttpData.isEmpty()) && (bodyListHttpDataRank < bodyListHttpData.size());
  }
  












  public InterfaceHttpData next()
  {
    checkDestroyed();
    
    if (hasNext()) {
      return (InterfaceHttpData)bodyListHttpData.get(bodyListHttpDataRank++);
    }
    return null;
  }
  
  public InterfaceHttpData currentPartialHttpData()
  {
    return currentAttribute;
  }
  






  private void parseBody()
  {
    if ((currentStatus == HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE) || (currentStatus == HttpPostRequestDecoder.MultiPartStatus.EPILOGUE)) {
      if (isLastChunk) {
        currentStatus = HttpPostRequestDecoder.MultiPartStatus.EPILOGUE;
      }
      return;
    }
    parseBodyAttributes();
  }
  


  protected void addHttpData(InterfaceHttpData data)
  {
    if (data == null) {
      return;
    }
    List<InterfaceHttpData> datas = (List)bodyMapHttpData.get(data.getName());
    if (datas == null) {
      datas = new ArrayList(1);
      bodyMapHttpData.put(data.getName(), datas);
    }
    datas.add(data);
    bodyListHttpData.add(data);
  }
  







  private void parseBodyAttributesStandard()
  {
    int firstpos = undecodedChunk.readerIndex();
    int currentpos = firstpos;
    

    if (currentStatus == HttpPostRequestDecoder.MultiPartStatus.NOTSTARTED) {
      currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
    }
    boolean contRead = true;
    try {
      while ((undecodedChunk.isReadable()) && (contRead)) {
        char read = (char)undecodedChunk.readUnsignedByte();
        currentpos++;
        switch (1.$SwitchMap$io$netty$handler$codec$http$multipart$HttpPostRequestDecoder$MultiPartStatus[currentStatus.ordinal()]) {
        case 1: 
          if (read == '=') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.FIELD;
            int equalpos = currentpos - 1;
            String key = decodeAttribute(undecodedChunk.toString(firstpos, equalpos - firstpos, charset), charset);
            
            currentAttribute = factory.createAttribute(request, key);
            firstpos = currentpos;
          } else if (read == '&') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            int ampersandpos = currentpos - 1;
            String key = decodeAttribute(undecodedChunk
              .toString(firstpos, ampersandpos - firstpos, charset), charset);
            currentAttribute = factory.createAttribute(request, key);
            currentAttribute.setValue("");
            addHttpData(currentAttribute);
            currentAttribute = null;
            firstpos = currentpos;
            contRead = true; }
          break;
        
        case 2: 
          if (read == '&') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            int ampersandpos = currentpos - 1;
            setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = true;
          } else if (read == '\r') {
            if (undecodedChunk.isReadable()) {
              read = (char)undecodedChunk.readUnsignedByte();
              currentpos++;
              if (read == '\n') {
                currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
                int ampersandpos = currentpos - 2;
                setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
                firstpos = currentpos;
                contRead = false;
              }
              else {
                throw new HttpPostRequestDecoder.ErrorDataDecoderException("Bad end of line");
              }
            } else {
              currentpos--;
            }
          } else if (read == '\n') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
            int ampersandpos = currentpos - 1;
            setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = false;
          }
          
          break;
        default: 
          contRead = false;
        }
      }
      if ((isLastChunk) && (currentAttribute != null))
      {
        int ampersandpos = currentpos;
        if (ampersandpos > firstpos) {
          setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
        } else if (!currentAttribute.isCompleted()) {
          setFinalBuffer(Unpooled.EMPTY_BUFFER);
        }
        firstpos = currentpos;
        currentStatus = HttpPostRequestDecoder.MultiPartStatus.EPILOGUE;
      } else if ((contRead) && (currentAttribute != null) && (currentStatus == HttpPostRequestDecoder.MultiPartStatus.FIELD))
      {
        currentAttribute.addContent(undecodedChunk.retainedSlice(firstpos, currentpos - firstpos), false);
        
        firstpos = currentpos;
      }
      undecodedChunk.readerIndex(firstpos);
    }
    catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
      undecodedChunk.readerIndex(firstpos);
      throw e;
    }
    catch (IOException e) {
      undecodedChunk.readerIndex(firstpos);
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
    }
    catch (IllegalArgumentException e) {
      undecodedChunk.readerIndex(firstpos);
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
    }
  }
  







  private void parseBodyAttributes()
  {
    if (undecodedChunk == null) {
      return;
    }
    if (!undecodedChunk.hasArray()) {
      parseBodyAttributesStandard();
      return;
    }
    HttpPostBodyUtil.SeekAheadOptimize sao = new HttpPostBodyUtil.SeekAheadOptimize(undecodedChunk);
    int firstpos = undecodedChunk.readerIndex();
    int currentpos = firstpos;
    

    if (currentStatus == HttpPostRequestDecoder.MultiPartStatus.NOTSTARTED) {
      currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
    }
    boolean contRead = true;
    try {
      while (pos < limit) {
        char read = (char)(bytes[(pos++)] & 0xFF);
        currentpos++;
        switch (1.$SwitchMap$io$netty$handler$codec$http$multipart$HttpPostRequestDecoder$MultiPartStatus[currentStatus.ordinal()]) {
        case 1: 
          if (read == '=') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.FIELD;
            int equalpos = currentpos - 1;
            String key = decodeAttribute(undecodedChunk.toString(firstpos, equalpos - firstpos, charset), charset);
            
            currentAttribute = factory.createAttribute(request, key);
            firstpos = currentpos;
          } else if (read == '&') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            int ampersandpos = currentpos - 1;
            String key = decodeAttribute(undecodedChunk
              .toString(firstpos, ampersandpos - firstpos, charset), charset);
            currentAttribute = factory.createAttribute(request, key);
            currentAttribute.setValue("");
            addHttpData(currentAttribute);
            currentAttribute = null;
            firstpos = currentpos;
            contRead = true; }
          break;
        
        case 2: 
          if (read == '&') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            int ampersandpos = currentpos - 1;
            setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = true;
          } else if (read == '\r') {
            if (pos < limit) {
              read = (char)(bytes[(pos++)] & 0xFF);
              currentpos++;
              if (read == '\n') {
                currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
                int ampersandpos = currentpos - 2;
                sao.setReadPosition(0);
                setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
                firstpos = currentpos;
                contRead = false;
                
                break label528;
              }
              sao.setReadPosition(0);
              throw new HttpPostRequestDecoder.ErrorDataDecoderException("Bad end of line");
            }
            
            if (limit > 0) {
              currentpos--;
            }
          }
          else if (read == '\n') {
            currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
            int ampersandpos = currentpos - 1;
            sao.setReadPosition(0);
            setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = false; }
          break;
        


        default: 
          sao.setReadPosition(0);
          contRead = false;
          break label528; }
      }
      label528:
      if ((isLastChunk) && (currentAttribute != null))
      {
        int ampersandpos = currentpos;
        if (ampersandpos > firstpos) {
          setFinalBuffer(undecodedChunk.retainedSlice(firstpos, ampersandpos - firstpos));
        } else if (!currentAttribute.isCompleted()) {
          setFinalBuffer(Unpooled.EMPTY_BUFFER);
        }
        firstpos = currentpos;
        currentStatus = HttpPostRequestDecoder.MultiPartStatus.EPILOGUE;
      } else if ((contRead) && (currentAttribute != null) && (currentStatus == HttpPostRequestDecoder.MultiPartStatus.FIELD))
      {
        currentAttribute.addContent(undecodedChunk.retainedSlice(firstpos, currentpos - firstpos), false);
        
        firstpos = currentpos;
      }
      undecodedChunk.readerIndex(firstpos);
    }
    catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
      undecodedChunk.readerIndex(firstpos);
      throw e;
    }
    catch (IOException e) {
      undecodedChunk.readerIndex(firstpos);
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
    }
    catch (IllegalArgumentException e) {
      undecodedChunk.readerIndex(firstpos);
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
    }
  }
  
  private void setFinalBuffer(ByteBuf buffer) throws IOException {
    currentAttribute.addContent(buffer, true);
    ByteBuf decodedBuf = decodeAttribute(currentAttribute.getByteBuf(), charset);
    if (decodedBuf != null) {
      currentAttribute.setContent(decodedBuf);
    }
    addHttpData(currentAttribute);
    currentAttribute = null;
  }
  



  private static String decodeAttribute(String s, Charset charset)
  {
    try
    {
      return QueryStringDecoder.decodeComponent(s, charset);
    } catch (IllegalArgumentException e) {
      throw new HttpPostRequestDecoder.ErrorDataDecoderException("Bad string: '" + s + '\'', e);
    }
  }
  
  private static ByteBuf decodeAttribute(ByteBuf b, Charset charset) {
    int firstEscaped = b.forEachByte(new UrlEncodedDetector(null));
    if (firstEscaped == -1) {
      return null;
    }
    
    ByteBuf buf = b.alloc().buffer(b.readableBytes());
    UrlDecoder urlDecode = new UrlDecoder(buf);
    int idx = b.forEachByte(urlDecode);
    if (nextEscapedIdx != 0) {
      if (idx == -1) {
        idx = b.readableBytes() - 1;
      }
      idx -= nextEscapedIdx - 1;
      buf.release();
      
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(String.format("Invalid hex byte at index '%d' in string: '%s'", new Object[] {Integer.valueOf(idx), b.toString(charset) }));
    }
    
    return buf;
  }
  





  public void destroy()
  {
    cleanFiles();
    
    for (InterfaceHttpData httpData : bodyListHttpData)
    {
      if (httpData.refCnt() > 0) {
        httpData.release();
      }
    }
    
    destroyed = true;
    
    if ((undecodedChunk != null) && (undecodedChunk.refCnt() > 0)) {
      undecodedChunk.release();
      undecodedChunk = null;
    }
  }
  



  public void cleanFiles()
  {
    checkDestroyed();
    
    factory.cleanRequestHttpData(request);
  }
  



  public void removeHttpDataFromClean(InterfaceHttpData data)
  {
    checkDestroyed();
    
    factory.removeHttpDataFromClean(request, data);
  }
  
  private static final class UrlEncodedDetector implements ByteProcessor {
    private UrlEncodedDetector() {}
    
    public boolean process(byte value) throws Exception { return (value != 37) && (value != 43); }
  }
  
  private static final class UrlDecoder implements ByteProcessor
  {
    private final ByteBuf output;
    private int nextEscapedIdx;
    private byte hiByte;
    
    UrlDecoder(ByteBuf output)
    {
      this.output = output;
    }
    
    public boolean process(byte value)
    {
      if (nextEscapedIdx != 0) {
        if (nextEscapedIdx == 1) {
          hiByte = value;
          nextEscapedIdx += 1;
        } else {
          int hi = StringUtil.decodeHexNibble((char)hiByte);
          int lo = StringUtil.decodeHexNibble((char)value);
          if ((hi == -1) || (lo == -1)) {
            nextEscapedIdx += 1;
            return false;
          }
          output.writeByte((hi << 4) + lo);
          nextEscapedIdx = 0;
        }
      } else if (value == 37) {
        nextEscapedIdx = 1;
      } else if (value == 43) {
        output.writeByte(32);
      } else {
        output.writeByte(value);
      }
      return true;
    }
  }
}
