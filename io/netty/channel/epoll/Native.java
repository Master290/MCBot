package io.netty.channel.epoll;

import io.netty.channel.DefaultFileRegion;
import io.netty.channel.unix.Errors;
import io.netty.channel.unix.PeerCredentials;
import io.netty.channel.unix.Socket;
import io.netty.channel.unix.Unix;
import io.netty.util.internal.ClassInitializerUtil;
import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;


































public final class Native
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(Native.class);
  
  static {
    Selector selector = null;
    


    try
    {
      selector = Selector.open();
    }
    catch (IOException localIOException) {}
    






    ClassInitializerUtil.tryLoadClasses(Native.class, new Class[] { PeerCredentials.class, DefaultFileRegion.class, FileChannel.class, java.io.FileDescriptor.class, NativeDatagramPacketArray.NativeDatagramPacket.class });
    






    try
    {
      offsetofEpollData();
      


      try
      {
        if (selector != null) {
          selector.close();
        }
      }
      catch (IOException localIOException1) {}
      

      Unix.registerInternal(new Runnable()
      {
        public void run() {
          Native.access$000();
        }
      });
    }
    catch (UnsatisfiedLinkError ignore)
    {
      loadNativeLibrary();
    } finally {
      try {
        if (selector != null) {
          selector.close();
        }
      }
      catch (IOException localIOException3) {}
    }
  }
  










  public static final int EPOLLIN = NativeStaticallyReferencedJniMethods.epollin();
  public static final int EPOLLOUT = NativeStaticallyReferencedJniMethods.epollout();
  public static final int EPOLLRDHUP = NativeStaticallyReferencedJniMethods.epollrdhup();
  public static final int EPOLLET = NativeStaticallyReferencedJniMethods.epollet();
  public static final int EPOLLERR = NativeStaticallyReferencedJniMethods.epollerr();
  
  public static final boolean IS_SUPPORTING_SENDMMSG = NativeStaticallyReferencedJniMethods.isSupportingSendmmsg();
  static final boolean IS_SUPPORTING_RECVMMSG = NativeStaticallyReferencedJniMethods.isSupportingRecvmmsg();
  static final boolean IS_SUPPORTING_UDP_SEGMENT = isSupportingUdpSegment();
  private static final int TFO_ENABLED_CLIENT_MASK = 1;
  private static final int TFO_ENABLED_SERVER_MASK = 2;
  private static final int TCP_FASTOPEN_MODE = NativeStaticallyReferencedJniMethods.tcpFastopenMode();
  



  static final boolean IS_SUPPORTING_TCP_FASTOPEN_CLIENT = (TCP_FASTOPEN_MODE & 0x1) == 1;
  




  static final boolean IS_SUPPORTING_TCP_FASTOPEN_SERVER = (TCP_FASTOPEN_MODE & 0x2) == 2;
  



  @Deprecated
  public static final boolean IS_SUPPORTING_TCP_FASTOPEN = (IS_SUPPORTING_TCP_FASTOPEN_CLIENT) || (IS_SUPPORTING_TCP_FASTOPEN_SERVER);
  
  public static final int TCP_MD5SIG_MAXKEYLEN = NativeStaticallyReferencedJniMethods.tcpMd5SigMaxKeyLen();
  public static final String KERNEL_VERSION = NativeStaticallyReferencedJniMethods.kernelVersion();
  
  public static io.netty.channel.unix.FileDescriptor newEventFd() {
    return new io.netty.channel.unix.FileDescriptor(eventFd());
  }
  
  public static io.netty.channel.unix.FileDescriptor newTimerFd() {
    return new io.netty.channel.unix.FileDescriptor(timerFd());
  }
  







  public static io.netty.channel.unix.FileDescriptor newEpollCreate()
  {
    return new io.netty.channel.unix.FileDescriptor(epollCreate());
  }
  




  @Deprecated
  public static int epollWait(io.netty.channel.unix.FileDescriptor epollFd, EpollEventArray events, io.netty.channel.unix.FileDescriptor timerFd, int timeoutSec, int timeoutNs)
    throws IOException
  {
    if ((timeoutSec == 0) && (timeoutNs == 0))
    {
      return epollWait(epollFd, events, 0);
    }
    if (timeoutSec == Integer.MAX_VALUE)
    {
      timeoutSec = 0;
      timeoutNs = 0;
    }
    int ready = epollWait0(epollFd.intValue(), events.memoryAddress(), events.length(), timerFd.intValue(), timeoutSec, timeoutNs);
    
    if (ready < 0) {
      throw Errors.newIOException("epoll_wait", ready);
    }
    return ready;
  }
  
  static int epollWait(io.netty.channel.unix.FileDescriptor epollFd, EpollEventArray events, boolean immediatePoll) throws IOException {
    return epollWait(epollFd, events, immediatePoll ? 0 : -1);
  }
  

  static int epollWait(io.netty.channel.unix.FileDescriptor epollFd, EpollEventArray events, int timeoutMillis)
    throws IOException
  {
    int ready = epollWait(epollFd.intValue(), events.memoryAddress(), events.length(), timeoutMillis);
    if (ready < 0) {
      throw Errors.newIOException("epoll_wait", ready);
    }
    return ready;
  }
  



  public static int epollBusyWait(io.netty.channel.unix.FileDescriptor epollFd, EpollEventArray events)
    throws IOException
  {
    int ready = epollBusyWait0(epollFd.intValue(), events.memoryAddress(), events.length());
    if (ready < 0) {
      throw Errors.newIOException("epoll_wait", ready);
    }
    return ready;
  }
  


  public static void epollCtlAdd(int efd, int fd, int flags)
    throws IOException
  {
    int res = epollCtlAdd0(efd, fd, flags);
    if (res < 0) {
      throw Errors.newIOException("epoll_ctl", res);
    }
  }
  
  public static void epollCtlMod(int efd, int fd, int flags) throws IOException
  {
    int res = epollCtlMod0(efd, fd, flags);
    if (res < 0) {
      throw Errors.newIOException("epoll_ctl", res);
    }
  }
  
  public static void epollCtlDel(int efd, int fd) throws IOException
  {
    int res = epollCtlDel0(efd, fd);
    if (res < 0) {
      throw Errors.newIOException("epoll_ctl", res);
    }
  }
  
  public static int splice(int fd, long offIn, int fdOut, long offOut, long len)
    throws IOException
  {
    int res = splice0(fd, offIn, fdOut, offOut, len);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("splice", res);
  }
  

  @Deprecated
  public static int sendmmsg(int fd, NativeDatagramPacketArray.NativeDatagramPacket[] msgs, int offset, int len)
    throws IOException
  {
    return sendmmsg(fd, Socket.isIPv6Preferred(), msgs, offset, len);
  }
  
  static int sendmmsg(int fd, boolean ipv6, NativeDatagramPacketArray.NativeDatagramPacket[] msgs, int offset, int len) throws IOException
  {
    int res = sendmmsg0(fd, ipv6, msgs, offset, len);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("sendmmsg", res);
  }
  


  static int recvmmsg(int fd, boolean ipv6, NativeDatagramPacketArray.NativeDatagramPacket[] msgs, int offset, int len)
    throws IOException
  {
    int res = recvmmsg0(fd, ipv6, msgs, offset, len);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("recvmmsg", res);
  }
  

  static int recvmsg(int fd, boolean ipv6, NativeDatagramPacketArray.NativeDatagramPacket packet)
    throws IOException
  {
    int res = recvmsg0(fd, ipv6, packet);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("recvmsg", res);
  }
  






  private static void loadNativeLibrary()
  {
    String name = PlatformDependent.normalizedOs();
    if (!"linux".equals(name)) {
      throw new IllegalStateException("Only supported on Linux");
    }
    String staticLibName = "netty_transport_native_epoll";
    String sharedLibName = staticLibName + '_' + PlatformDependent.normalizedArch();
    ClassLoader cl = PlatformDependent.getClassLoader(Native.class);
    try {
      NativeLibraryLoader.load(sharedLibName, cl);
    } catch (UnsatisfiedLinkError e1) {
      try {
        NativeLibraryLoader.load(staticLibName, cl);
        logger.debug("Failed to load {}", sharedLibName, e1);
      } catch (UnsatisfiedLinkError e2) {
        ThrowableUtil.addSuppressed(e1, e2);
        throw e1;
      }
    }
  }
  
  private static native int registerUnix();
  
  private static native boolean isSupportingUdpSegment();
  
  private static native int eventFd();
  
  private static native int timerFd();
  
  public static native void eventFdWrite(int paramInt, long paramLong);
  
  public static native void eventFdRead(int paramInt);
  
  static native void timerFdRead(int paramInt);
  
  static native void timerFdSetTime(int paramInt1, int paramInt2, int paramInt3)
    throws IOException;
  
  private static native int epollCreate();
  
  private static native int epollWait0(int paramInt1, long paramLong, int paramInt2, int paramInt3, int paramInt4, int paramInt5);
  
  private static native int epollWait(int paramInt1, long paramLong, int paramInt2, int paramInt3);
  
  private static native int epollBusyWait0(int paramInt1, long paramLong, int paramInt2);
  
  private static native int epollCtlAdd0(int paramInt1, int paramInt2, int paramInt3);
  
  private static native int epollCtlMod0(int paramInt1, int paramInt2, int paramInt3);
  
  private static native int epollCtlDel0(int paramInt1, int paramInt2);
  
  private static native int splice0(int paramInt1, long paramLong1, int paramInt2, long paramLong2, long paramLong3);
  
  private static native int sendmmsg0(int paramInt1, boolean paramBoolean, NativeDatagramPacketArray.NativeDatagramPacket[] paramArrayOfNativeDatagramPacket, int paramInt2, int paramInt3);
  
  private static native int recvmmsg0(int paramInt1, boolean paramBoolean, NativeDatagramPacketArray.NativeDatagramPacket[] paramArrayOfNativeDatagramPacket, int paramInt2, int paramInt3);
  
  private static native int recvmsg0(int paramInt, boolean paramBoolean, NativeDatagramPacketArray.NativeDatagramPacket paramNativeDatagramPacket);
  
  public static native int sizeofEpollEvent();
  
  public static native int offsetofEpollData();
  
  private Native() {}
}
