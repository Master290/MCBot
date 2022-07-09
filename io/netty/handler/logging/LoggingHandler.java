package io.netty.handler.logging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
























@ChannelHandler.Sharable
public class LoggingHandler
  extends ChannelDuplexHandler
{
  private static final LogLevel DEFAULT_LEVEL = LogLevel.DEBUG;
  
  protected final InternalLogger logger;
  
  protected final InternalLogLevel internalLevel;
  
  private final LogLevel level;
  
  private final ByteBufFormat byteBufFormat;
  

  public LoggingHandler()
  {
    this(DEFAULT_LEVEL);
  }
  




  public LoggingHandler(ByteBufFormat format)
  {
    this(DEFAULT_LEVEL, format);
  }
  





  public LoggingHandler(LogLevel level)
  {
    this(level, ByteBufFormat.HEX_DUMP);
  }
  






  public LoggingHandler(LogLevel level, ByteBufFormat byteBufFormat)
  {
    this.level = ((LogLevel)ObjectUtil.checkNotNull(level, "level"));
    this.byteBufFormat = ((ByteBufFormat)ObjectUtil.checkNotNull(byteBufFormat, "byteBufFormat"));
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }
  





  public LoggingHandler(Class<?> clazz)
  {
    this(clazz, DEFAULT_LEVEL);
  }
  





  public LoggingHandler(Class<?> clazz, LogLevel level)
  {
    this(clazz, level, ByteBufFormat.HEX_DUMP);
  }
  






  public LoggingHandler(Class<?> clazz, LogLevel level, ByteBufFormat byteBufFormat)
  {
    ObjectUtil.checkNotNull(clazz, "clazz");
    this.level = ((LogLevel)ObjectUtil.checkNotNull(level, "level"));
    this.byteBufFormat = ((ByteBufFormat)ObjectUtil.checkNotNull(byteBufFormat, "byteBufFormat"));
    logger = InternalLoggerFactory.getInstance(clazz);
    internalLevel = level.toInternalLevel();
  }
  




  public LoggingHandler(String name)
  {
    this(name, DEFAULT_LEVEL);
  }
  





  public LoggingHandler(String name, LogLevel level)
  {
    this(name, level, ByteBufFormat.HEX_DUMP);
  }
  






  public LoggingHandler(String name, LogLevel level, ByteBufFormat byteBufFormat)
  {
    ObjectUtil.checkNotNull(name, "name");
    
    this.level = ((LogLevel)ObjectUtil.checkNotNull(level, "level"));
    this.byteBufFormat = ((ByteBufFormat)ObjectUtil.checkNotNull(byteBufFormat, "byteBufFormat"));
    logger = InternalLoggerFactory.getInstance(name);
    internalLevel = level.toInternalLevel();
  }
  


  public LogLevel level()
  {
    return level;
  }
  


  public ByteBufFormat byteBufFormat()
  {
    return byteBufFormat;
  }
  
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "REGISTERED"));
    }
    ctx.fireChannelRegistered();
  }
  
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "UNREGISTERED"));
    }
    ctx.fireChannelUnregistered();
  }
  
  public void channelActive(ChannelHandlerContext ctx) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "ACTIVE"));
    }
    ctx.fireChannelActive();
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "INACTIVE"));
    }
    ctx.fireChannelInactive();
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "EXCEPTION", cause), cause);
    }
    ctx.fireExceptionCaught(cause);
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "USER_EVENT", evt));
    }
    ctx.fireUserEventTriggered(evt);
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "BIND", localAddress));
    }
    ctx.bind(localAddress, promise);
  }
  

  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "CONNECT", remoteAddress, localAddress));
    }
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "DISCONNECT"));
    }
    ctx.disconnect(promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "CLOSE"));
    }
    ctx.close(promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "DEREGISTER"));
    }
    ctx.deregister(promise);
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "READ COMPLETE"));
    }
    ctx.fireChannelReadComplete();
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "READ", msg));
    }
    ctx.fireChannelRead(msg);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "WRITE", msg));
    }
    ctx.write(msg, promise);
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "WRITABILITY CHANGED"));
    }
    ctx.fireChannelWritabilityChanged();
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, format(ctx, "FLUSH"));
    }
    ctx.flush();
  }
  




  protected String format(ChannelHandlerContext ctx, String eventName)
  {
    String chStr = ctx.channel().toString();
    return chStr.length() + 1 + eventName.length() + chStr + 
      ' ' + 
      eventName;
  }
  






  protected String format(ChannelHandlerContext ctx, String eventName, Object arg)
  {
    if ((arg instanceof ByteBuf))
      return formatByteBuf(ctx, eventName, (ByteBuf)arg);
    if ((arg instanceof ByteBufHolder)) {
      return formatByteBufHolder(ctx, eventName, (ByteBufHolder)arg);
    }
    return formatSimple(ctx, eventName, arg);
  }
  








  protected String format(ChannelHandlerContext ctx, String eventName, Object firstArg, Object secondArg)
  {
    if (secondArg == null) {
      return formatSimple(ctx, eventName, firstArg);
    }
    
    String chStr = ctx.channel().toString();
    String arg1Str = String.valueOf(firstArg);
    String arg2Str = secondArg.toString();
    
    StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + arg1Str.length() + 2 + arg2Str.length());
    buf.append(chStr).append(' ').append(eventName).append(": ").append(arg1Str).append(", ").append(arg2Str);
    return buf.toString();
  }
  


  private String formatByteBuf(ChannelHandlerContext ctx, String eventName, ByteBuf msg)
  {
    String chStr = ctx.channel().toString();
    int length = msg.readableBytes();
    if (length == 0) {
      StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 4);
      buf.append(chStr).append(' ').append(eventName).append(": 0B");
      return buf.toString();
    }
    int outputLength = chStr.length() + 1 + eventName.length() + 2 + 10 + 1;
    if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
      int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
      int hexDumpLength = 2 + rows * 80;
      outputLength += hexDumpLength;
    }
    StringBuilder buf = new StringBuilder(outputLength);
    buf.append(chStr).append(' ').append(eventName).append(": ").append(length).append('B');
    if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
      buf.append(StringUtil.NEWLINE);
      ByteBufUtil.appendPrettyHexDump(buf, msg);
    }
    
    return buf.toString();
  }
  



  private String formatByteBufHolder(ChannelHandlerContext ctx, String eventName, ByteBufHolder msg)
  {
    String chStr = ctx.channel().toString();
    String msgStr = msg.toString();
    ByteBuf content = msg.content();
    int length = content.readableBytes();
    if (length == 0) {
      StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + msgStr.length() + 4);
      buf.append(chStr).append(' ').append(eventName).append(", ").append(msgStr).append(", 0B");
      return buf.toString();
    }
    int outputLength = chStr.length() + 1 + eventName.length() + 2 + msgStr.length() + 2 + 10 + 1;
    if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
      int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
      int hexDumpLength = 2 + rows * 80;
      outputLength += hexDumpLength;
    }
    StringBuilder buf = new StringBuilder(outputLength);
    buf.append(chStr).append(' ').append(eventName).append(": ")
      .append(msgStr).append(", ").append(length).append('B');
    if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
      buf.append(StringUtil.NEWLINE);
      ByteBufUtil.appendPrettyHexDump(buf, content);
    }
    
    return buf.toString();
  }
  



  private static String formatSimple(ChannelHandlerContext ctx, String eventName, Object msg)
  {
    String chStr = ctx.channel().toString();
    String msgStr = String.valueOf(msg);
    StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + msgStr.length());
    return chStr + ' ' + eventName + ": " + msgStr;
  }
}
