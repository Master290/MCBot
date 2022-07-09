package io.netty.handler.codec.redis;

import io.netty.buffer.ByteBuf;





















public enum RedisMessageType
{
  INLINE_COMMAND(null, true), 
  SIMPLE_STRING(Byte.valueOf((byte)43), true), 
  ERROR(Byte.valueOf((byte)45), true), 
  INTEGER(Byte.valueOf((byte)58), true), 
  BULK_STRING(Byte.valueOf((byte)36), false), 
  ARRAY_HEADER(Byte.valueOf((byte)42), false);
  
  private final Byte value;
  private final boolean inline;
  
  private RedisMessageType(Byte value, boolean inline) {
    this.value = value;
    this.inline = inline;
  }
  


  public int length()
  {
    return value != null ? 1 : 0;
  }
  



  public boolean isInline()
  {
    return inline;
  }
  


  public static RedisMessageType readFrom(ByteBuf in, boolean decodeInlineCommands)
  {
    int initialIndex = in.readerIndex();
    RedisMessageType type = valueOf(in.readByte());
    if (type == INLINE_COMMAND) {
      if (!decodeInlineCommands) {
        throw new RedisCodecException("Decoding of inline commands is disabled");
      }
      
      in.readerIndex(initialIndex);
    }
    return type;
  }
  


  public void writeTo(ByteBuf out)
  {
    if (value == null) {
      return;
    }
    out.writeByte(value.byteValue());
  }
  
  private static RedisMessageType valueOf(byte value) {
    switch (value) {
    case 43: 
      return SIMPLE_STRING;
    case 45: 
      return ERROR;
    case 58: 
      return INTEGER;
    case 36: 
      return BULK_STRING;
    case 42: 
      return ARRAY_HEADER;
    }
    return INLINE_COMMAND;
  }
}
