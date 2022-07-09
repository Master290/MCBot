package io.netty.channel.unix;

import io.netty.util.internal.ObjectUtil;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;




























public class FileDescriptor
{
  private static final AtomicIntegerFieldUpdater<FileDescriptor> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(FileDescriptor.class, "state");
  
  private static final int STATE_CLOSED_MASK = 1;
  
  private static final int STATE_INPUT_SHUTDOWN_MASK = 2;
  
  private static final int STATE_OUTPUT_SHUTDOWN_MASK = 4;
  
  private static final int STATE_ALL_MASK = 7;
  
  volatile int state;
  
  final int fd;
  
  public FileDescriptor(int fd)
  {
    ObjectUtil.checkPositiveOrZero(fd, "fd");
    this.fd = fd;
  }
  


  public final int intValue()
  {
    return fd;
  }
  
  protected boolean markClosed() {
    for (;;) {
      int state = this.state;
      if (isClosed(state)) {
        return false;
      }
      
      if (casState(state, state | 0x7)) {
        return true;
      }
    }
  }
  

  public void close()
    throws IOException
  {
    if (markClosed()) {
      int res = close(fd);
      if (res < 0) {
        throw Errors.newIOException("close", res);
      }
    }
  }
  


  public boolean isOpen()
  {
    return !isClosed(state);
  }
  
  public final int write(ByteBuffer buf, int pos, int limit) throws IOException {
    int res = write(fd, buf, pos, limit);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("write", res);
  }
  
  public final int writeAddress(long address, int pos, int limit) throws IOException {
    int res = writeAddress(fd, address, pos, limit);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("writeAddress", res);
  }
  
  public final long writev(ByteBuffer[] buffers, int offset, int length, long maxBytesToWrite) throws IOException {
    long res = writev(fd, buffers, offset, Math.min(Limits.IOV_MAX, length), maxBytesToWrite);
    if (res >= 0L) {
      return res;
    }
    return Errors.ioResult("writev", (int)res);
  }
  
  public final long writevAddresses(long memoryAddress, int length) throws IOException {
    long res = writevAddresses(fd, memoryAddress, length);
    if (res >= 0L) {
      return res;
    }
    return Errors.ioResult("writevAddresses", (int)res);
  }
  
  public final int read(ByteBuffer buf, int pos, int limit) throws IOException {
    int res = read(fd, buf, pos, limit);
    if (res > 0) {
      return res;
    }
    if (res == 0) {
      return -1;
    }
    return Errors.ioResult("read", res);
  }
  
  public final int readAddress(long address, int pos, int limit) throws IOException {
    int res = readAddress(fd, address, pos, limit);
    if (res > 0) {
      return res;
    }
    if (res == 0) {
      return -1;
    }
    return Errors.ioResult("readAddress", res);
  }
  
  public String toString()
  {
    return "FileDescriptor{fd=" + fd + '}';
  }
  


  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FileDescriptor)) {
      return false;
    }
    
    return fd == fd;
  }
  
  public int hashCode()
  {
    return fd;
  }
  

  public static FileDescriptor from(String path)
    throws IOException
  {
    int res = open((String)ObjectUtil.checkNotNull(path, "path"));
    if (res < 0) {
      throw Errors.newIOException("open", res);
    }
    return new FileDescriptor(res);
  }
  

  public static FileDescriptor from(File file)
    throws IOException
  {
    return from(((File)ObjectUtil.checkNotNull(file, "file")).getPath());
  }
  

  public static FileDescriptor[] pipe()
    throws IOException
  {
    long res = newPipe();
    if (res < 0L) {
      throw Errors.newIOException("newPipe", (int)res);
    }
    return new FileDescriptor[] { new FileDescriptor((int)(res >>> 32)), new FileDescriptor((int)res) };
  }
  
  final boolean casState(int expected, int update) {
    return stateUpdater.compareAndSet(this, expected, update);
  }
  
  static boolean isClosed(int state) {
    return (state & 0x1) != 0;
  }
  
  static boolean isInputShutdown(int state) {
    return (state & 0x2) != 0;
  }
  
  static boolean isOutputShutdown(int state) {
    return (state & 0x4) != 0;
  }
  
  static int inputShutdown(int state) {
    return state | 0x2;
  }
  
  static int outputShutdown(int state) {
    return state | 0x4;
  }
  
  private static native int open(String paramString);
  
  private static native int close(int paramInt);
  
  private static native int write(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3);
  
  private static native int writeAddress(int paramInt1, long paramLong, int paramInt2, int paramInt3);
  
  private static native long writev(int paramInt1, ByteBuffer[] paramArrayOfByteBuffer, int paramInt2, int paramInt3, long paramLong);
  
  private static native long writevAddresses(int paramInt1, long paramLong, int paramInt2);
  
  private static native int read(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3);
  
  private static native int readAddress(int paramInt1, long paramLong, int paramInt2, int paramInt3);
  
  private static native long newPipe();
}
