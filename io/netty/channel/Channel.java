package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AttributeMap;
import java.net.SocketAddress;

public abstract interface Channel
  extends AttributeMap, ChannelOutboundInvoker, Comparable<Channel>
{
  public abstract ChannelId id();
  
  public abstract EventLoop eventLoop();
  
  public abstract Channel parent();
  
  public abstract ChannelConfig config();
  
  public abstract boolean isOpen();
  
  public abstract boolean isRegistered();
  
  public abstract boolean isActive();
  
  public abstract ChannelMetadata metadata();
  
  public abstract SocketAddress localAddress();
  
  public abstract SocketAddress remoteAddress();
  
  public abstract ChannelFuture closeFuture();
  
  public abstract boolean isWritable();
  
  public abstract long bytesBeforeUnwritable();
  
  public abstract long bytesBeforeWritable();
  
  public abstract Unsafe unsafe();
  
  public abstract ChannelPipeline pipeline();
  
  public abstract ByteBufAllocator alloc();
  
  public abstract Channel read();
  
  public abstract Channel flush();
  
  public static abstract interface Unsafe
  {
    public abstract RecvByteBufAllocator.Handle recvBufAllocHandle();
    
    public abstract SocketAddress localAddress();
    
    public abstract SocketAddress remoteAddress();
    
    public abstract void register(EventLoop paramEventLoop, ChannelPromise paramChannelPromise);
    
    public abstract void bind(SocketAddress paramSocketAddress, ChannelPromise paramChannelPromise);
    
    public abstract void connect(SocketAddress paramSocketAddress1, SocketAddress paramSocketAddress2, ChannelPromise paramChannelPromise);
    
    public abstract void disconnect(ChannelPromise paramChannelPromise);
    
    public abstract void close(ChannelPromise paramChannelPromise);
    
    public abstract void closeForcibly();
    
    public abstract void deregister(ChannelPromise paramChannelPromise);
    
    public abstract void beginRead();
    
    public abstract void write(Object paramObject, ChannelPromise paramChannelPromise);
    
    public abstract void flush();
    
    public abstract ChannelPromise voidPromise();
    
    public abstract ChannelOutboundBuffer outboundBuffer();
  }
}
