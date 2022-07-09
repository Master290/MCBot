package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.util.List;


















public abstract class SslClientHelloHandler<T>
  extends ByteToMessageDecoder
  implements ChannelOutboundHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslClientHelloHandler.class);
  private boolean handshakeFailed;
  private boolean suppressRead;
  private boolean readPending;
  private ByteBuf handshakeBuffer;
  
  public SslClientHelloHandler() {}
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if ((!suppressRead) && (!handshakeFailed)) {
      try {
        int readerIndex = in.readerIndex();
        int readableBytes = in.readableBytes();
        int handshakeLength = -1;
        
        label423:
        while (readableBytes >= 5) {
          int contentType = in.getUnsignedByte(readerIndex);
          switch (contentType)
          {
          case 20: 
          case 21: 
            int len = SslUtils.getEncryptedPacketLength(in, readerIndex);
            

            if (len == -2) {
              handshakeFailed = true;
              
              NotSslRecordException e = new NotSslRecordException("not an SSL/TLS record: " + ByteBufUtil.hexDump(in));
              in.skipBytes(in.readableBytes());
              ctx.fireUserEventTriggered(new SniCompletionEvent(e));
              SslUtils.handleHandshakeFailure(ctx, e, true);
              throw e;
            }
            if (len == -1)
            {
              return;
            }
            
            select(ctx, null);
            return;
          case 22: 
            int majorVersion = in.getUnsignedByte(readerIndex + 1);
            
            if (majorVersion == 3) {
              int packetLength = in.getUnsignedShort(readerIndex + 3) + 5;
              

              if (readableBytes < packetLength)
              {
                return; }
              if (packetLength == 5) {
                select(ctx, null);
                return;
              }
              
              int endOffset = readerIndex + packetLength;
              

              if (handshakeLength == -1) {
                if (readerIndex + 4 > endOffset)
                {
                  return;
                }
                
                int handshakeType = in.getUnsignedByte(readerIndex + 5);
                



                if (handshakeType != 1) {
                  select(ctx, null);
                  return;
                }
                


                handshakeLength = in.getUnsignedMedium(readerIndex + 5 + 1);
                


                readerIndex += 4;
                packetLength -= 4;
                
                if (handshakeLength + 4 + 5 <= packetLength)
                {

                  readerIndex += 5;
                  select(ctx, in.retainedSlice(readerIndex, handshakeLength));
                  return;
                }
                if (handshakeBuffer == null) {
                  handshakeBuffer = ctx.alloc().buffer(handshakeLength);
                }
                else {
                  handshakeBuffer.clear();
                }
              }
              


              handshakeBuffer.writeBytes(in, readerIndex + 5, packetLength - 5);
              
              readerIndex += packetLength;
              readableBytes -= packetLength;
              if (handshakeLength > handshakeBuffer.readableBytes()) break label423;
              ByteBuf clientHello = handshakeBuffer.setIndex(0, handshakeLength);
              handshakeBuffer = null;
              
              select(ctx, clientHello);
              return;
            }
            
            break;
          }
          
          
          select(ctx, null);
          return;
        }
      }
      catch (NotSslRecordException e)
      {
        throw e;
      }
      catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Unexpected client hello packet: " + ByteBufUtil.hexDump(in), e);
        }
        select(ctx, null);
      }
    }
  }
  
  private void releaseHandshakeBuffer() {
    releaseIfNotNull(handshakeBuffer);
    handshakeBuffer = null;
  }
  
  private static void releaseIfNotNull(ByteBuf buffer) {
    if (buffer != null) {
      buffer.release();
    }
  }
  
  private void select(final ChannelHandlerContext ctx, ByteBuf clientHello) throws Exception
  {
    try {
      Future<T> future = lookup(ctx, clientHello);
      if (future.isDone()) {
        onLookupComplete(ctx, future);
      } else {
        suppressRead = true;
        final ByteBuf finalClientHello = clientHello;
        future.addListener(new FutureListener()
        {
          public void operationComplete(Future<T> future) {
            SslClientHelloHandler.releaseIfNotNull(finalClientHello);
            try {
              suppressRead = false;
              try {
                onLookupComplete(ctx, future);
              } catch (DecoderException err) {
                ctx.fireExceptionCaught(err);
              } catch (Exception cause) {
                ctx.fireExceptionCaught(new DecoderException(cause));
              } catch (Throwable cause) {
                ctx.fireExceptionCaught(cause);
              }
              
              if (readPending) {
                readPending = false;
                ctx.read();
              }
            }
            finally
            {
              if (readPending) {
                readPending = false;
                ctx.read();
              }
              
            }
            
          }
        });
        clientHello = null;
      }
    } catch (Throwable cause) {
      PlatformDependent.throwException(cause);
    } finally {
      releaseIfNotNull(clientHello);
    }
  }
  
  protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception
  {
    releaseHandshakeBuffer();
    
    super.handlerRemoved0(ctx);
  }
  









  protected abstract Future<T> lookup(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf)
    throws Exception;
  









  protected abstract void onLookupComplete(ChannelHandlerContext paramChannelHandlerContext, Future<T> paramFuture)
    throws Exception;
  








  public void read(ChannelHandlerContext ctx)
    throws Exception
  {
    if (suppressRead) {
      readPending = true;
    } else {
      ctx.read();
    }
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.disconnect(promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.close(promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    ctx.deregister(promise);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    ctx.write(msg, promise);
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    ctx.flush();
  }
}
