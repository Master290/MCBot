package io.netty.handler.codec.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLite.Builder;
import com.google.protobuf.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.internal.ObjectUtil;
import java.util.List;















































@ChannelHandler.Sharable
public class ProtobufDecoder
  extends MessageToMessageDecoder<ByteBuf>
{
  private static final boolean HAS_PARSER;
  private final MessageLite prototype;
  private final ExtensionRegistryLite extensionRegistry;
  
  static
  {
    boolean hasParser = false;
    try
    {
      MessageLite.class.getDeclaredMethod("getParserForType", new Class[0]);
      hasParser = true;
    }
    catch (Throwable localThrowable) {}
    

    HAS_PARSER = hasParser;
  }
  





  public ProtobufDecoder(MessageLite prototype)
  {
    this(prototype, null);
  }
  
  public ProtobufDecoder(MessageLite prototype, ExtensionRegistry extensionRegistry) {
    this(prototype, extensionRegistry);
  }
  
  public ProtobufDecoder(MessageLite prototype, ExtensionRegistryLite extensionRegistry) {
    this.prototype = ((MessageLite)ObjectUtil.checkNotNull(prototype, "prototype")).getDefaultInstanceForType();
    this.extensionRegistry = extensionRegistry;
  }
  


  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
    throws Exception
  {
    int length = msg.readableBytes();
    int offset; byte[] array; int offset; if (msg.hasArray()) {
      byte[] array = msg.array();
      offset = msg.arrayOffset() + msg.readerIndex();
    } else {
      array = ByteBufUtil.getBytes(msg, msg.readerIndex(), length, false);
      offset = 0;
    }
    
    if (extensionRegistry == null) {
      if (HAS_PARSER) {
        out.add(prototype.getParserForType().parseFrom(array, offset, length));
      } else {
        out.add(prototype.newBuilderForType().mergeFrom(array, offset, length).build());
      }
    }
    else if (HAS_PARSER) {
      out.add(prototype.getParserForType().parseFrom(array, offset, length, extensionRegistry));
    }
    else {
      out.add(prototype.newBuilderForType().mergeFrom(array, offset, length, extensionRegistry)
        .build());
    }
  }
}
