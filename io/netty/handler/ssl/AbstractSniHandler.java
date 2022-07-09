package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import java.util.Locale;






































public abstract class AbstractSniHandler<T>
  extends SslClientHelloHandler<T>
{
  private String hostname;
  
  public AbstractSniHandler() {}
  
  private static String extractSniHostname(ByteBuf in)
  {
    int offset = in.readerIndex();
    int endOffset = in.writerIndex();
    offset += 34;
    
    if (endOffset - offset >= 6) {
      int sessionIdLength = in.getUnsignedByte(offset);
      offset += sessionIdLength + 1;
      
      int cipherSuitesLength = in.getUnsignedShort(offset);
      offset += cipherSuitesLength + 2;
      
      int compressionMethodLength = in.getUnsignedByte(offset);
      offset += compressionMethodLength + 1;
      
      int extensionsLength = in.getUnsignedShort(offset);
      offset += 2;
      int extensionsLimit = offset + extensionsLength;
      

      if (extensionsLimit <= endOffset) {
        while (extensionsLimit - offset >= 4) {
          int extensionType = in.getUnsignedShort(offset);
          offset += 2;
          
          int extensionLength = in.getUnsignedShort(offset);
          offset += 2;
          
          if (extensionsLimit - offset < extensionLength) {
            break;
          }
          


          if (extensionType == 0) {
            offset += 2;
            if (extensionsLimit - offset < 3) {
              break;
            }
            
            int serverNameType = in.getUnsignedByte(offset);
            offset++;
            
            if (serverNameType != 0) break;
            int serverNameLength = in.getUnsignedShort(offset);
            offset += 2;
            
            if (extensionsLimit - offset < serverNameLength) {
              break;
            }
            
            String hostname = in.toString(offset, serverNameLength, CharsetUtil.US_ASCII);
            return hostname.toLowerCase(Locale.US);
          }
          




          offset += extensionLength;
        }
      }
    }
    return null;
  }
  

  protected Future<T> lookup(ChannelHandlerContext ctx, ByteBuf clientHello)
    throws Exception
  {
    hostname = (clientHello == null ? null : extractSniHostname(clientHello));
    
    return lookup(ctx, hostname);
  }
  
  protected void onLookupComplete(ChannelHandlerContext ctx, Future<T> future) throws Exception
  {
    try {
      onLookupComplete(ctx, hostname, future);
      
      fireSniCompletionEvent(ctx, hostname, future); } finally { fireSniCompletionEvent(ctx, hostname, future);
    }
  }
  



  protected abstract Future<T> lookup(ChannelHandlerContext paramChannelHandlerContext, String paramString)
    throws Exception;
  



  protected abstract void onLookupComplete(ChannelHandlerContext paramChannelHandlerContext, String paramString, Future<T> paramFuture)
    throws Exception;
  



  private static void fireSniCompletionEvent(ChannelHandlerContext ctx, String hostname, Future<?> future)
  {
    Throwable cause = future.cause();
    if (cause == null) {
      ctx.fireUserEventTriggered(new SniCompletionEvent(hostname));
    } else {
      ctx.fireUserEventTriggered(new SniCompletionEvent(hostname, cause));
    }
  }
}
