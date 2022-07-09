package io.netty.channel.unix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;








public abstract class SocketWritableByteChannel
  implements WritableByteChannel
{
  private final FileDescriptor fd;
  
  protected SocketWritableByteChannel(FileDescriptor fd)
  {
    this.fd = ((FileDescriptor)ObjectUtil.checkNotNull(fd, "fd"));
  }
  
  public final int write(ByteBuffer src)
    throws IOException
  {
    int position = src.position();
    int limit = src.limit();
    int written; if (src.isDirect()) {
      written = fd.write(src, position, src.limit());
    } else {
      int readableBytes = limit - position;
      ByteBuf buffer = null;
      try {
        if (readableBytes == 0) {
          buffer = Unpooled.EMPTY_BUFFER;
        } else {
          ByteBufAllocator alloc = alloc();
          if (alloc.isDirectBufferPooled()) {
            buffer = alloc.directBuffer(readableBytes);
          } else {
            buffer = ByteBufUtil.threadLocalDirectBuffer();
            if (buffer == null) {
              buffer = Unpooled.directBuffer(readableBytes);
            }
          }
        }
        buffer.writeBytes(src.duplicate());
        ByteBuffer nioBuffer = buffer.internalNioBuffer(buffer.readerIndex(), readableBytes);
        written = fd.write(nioBuffer, nioBuffer.position(), nioBuffer.limit());
      } finally { int written;
        if (buffer != null)
          buffer.release();
      }
    }
    int written;
    if (written > 0) {
      src.position(position + written);
    }
    return written;
  }
  
  public final boolean isOpen()
  {
    return fd.isOpen();
  }
  
  public final void close() throws IOException
  {
    fd.close();
  }
  
  protected abstract ByteBufAllocator alloc();
}
