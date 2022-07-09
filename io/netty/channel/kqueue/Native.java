package io.netty.channel.kqueue;

import io.netty.channel.DefaultFileRegion;
import io.netty.channel.unix.Errors;
import io.netty.channel.unix.PeerCredentials;
import io.netty.channel.unix.Unix;
import io.netty.util.internal.ClassInitializerUtil;
import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.nio.channels.FileChannel;




































final class Native
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(Native.class);
  




  static
  {
    ClassInitializerUtil.tryLoadClasses(Native.class, new Class[] { PeerCredentials.class, DefaultFileRegion.class, FileChannel.class, java.io.FileDescriptor.class });
    




    try
    {
      sizeofKEvent();
    }
    catch (UnsatisfiedLinkError ignore) {
      loadNativeLibrary();
    }
    Unix.registerInternal(new Runnable()
    {
      public void run() {
        Native.access$000();
      }
    });
  }
  


  static final short EV_ADD = KQueueStaticallyReferencedJniMethods.evAdd();
  static final short EV_ENABLE = KQueueStaticallyReferencedJniMethods.evEnable();
  static final short EV_DISABLE = KQueueStaticallyReferencedJniMethods.evDisable();
  static final short EV_DELETE = KQueueStaticallyReferencedJniMethods.evDelete();
  static final short EV_CLEAR = KQueueStaticallyReferencedJniMethods.evClear();
  static final short EV_ERROR = KQueueStaticallyReferencedJniMethods.evError();
  static final short EV_EOF = KQueueStaticallyReferencedJniMethods.evEOF();
  
  static final int NOTE_READCLOSED = KQueueStaticallyReferencedJniMethods.noteReadClosed();
  static final int NOTE_CONNRESET = KQueueStaticallyReferencedJniMethods.noteConnReset();
  static final int NOTE_DISCONNECTED = KQueueStaticallyReferencedJniMethods.noteDisconnected();
  
  static final int NOTE_RDHUP = NOTE_READCLOSED | NOTE_CONNRESET | NOTE_DISCONNECTED;
  

  static final short EV_ADD_CLEAR_ENABLE = (short)(EV_ADD | EV_CLEAR | EV_ENABLE);
  static final short EV_DELETE_DISABLE = (short)(EV_DELETE | EV_DISABLE);
  
  static final short EVFILT_READ = KQueueStaticallyReferencedJniMethods.evfiltRead();
  static final short EVFILT_WRITE = KQueueStaticallyReferencedJniMethods.evfiltWrite();
  static final short EVFILT_USER = KQueueStaticallyReferencedJniMethods.evfiltUser();
  static final short EVFILT_SOCK = KQueueStaticallyReferencedJniMethods.evfiltSock();
  
  static io.netty.channel.unix.FileDescriptor newKQueue() {
    return new io.netty.channel.unix.FileDescriptor(kqueueCreate());
  }
  
  static int keventWait(int kqueueFd, KQueueEventArray changeList, KQueueEventArray eventList, int tvSec, int tvNsec) throws IOException
  {
    int ready = keventWait(kqueueFd, changeList.memoryAddress(), changeList.size(), eventList
      .memoryAddress(), eventList.capacity(), tvSec, tvNsec);
    if (ready < 0) {
      throw Errors.newIOException("kevent", ready);
    }
    return ready;
  }
  













  private static void loadNativeLibrary()
  {
    String name = PlatformDependent.normalizedOs();
    if ((!"osx".equals(name)) && (!name.contains("bsd"))) {
      throw new IllegalStateException("Only supported on OSX/BSD");
    }
    String staticLibName = "netty_transport_native_kqueue";
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
  
  private static native int kqueueCreate();
  
  private static native int keventWait(int paramInt1, long paramLong1, int paramInt2, long paramLong2, int paramInt3, int paramInt4, int paramInt5);
  
  static native int keventTriggerUserEvent(int paramInt1, int paramInt2);
  
  static native int keventAddUserEvent(int paramInt1, int paramInt2);
  
  static native int sizeofKEvent();
  
  static native int offsetofKEventIdent();
  
  static native int offsetofKEventFlags();
  
  static native int offsetofKEventFFlags();
  
  static native int offsetofKEventFilter();
  
  static native int offsetofKeventData();
  
  private Native() {}
}
