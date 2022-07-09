package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.MessageToByteEncoder;

abstract interface CompressionEncoderFactory
{
  public abstract MessageToByteEncoder<ByteBuf> createEncoder();
}
