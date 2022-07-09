package io.netty.util.internal;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;























final class CleanerJava6
  implements Cleaner
{
  private static final long CLEANER_FIELD_OFFSET;
  private static final Method CLEAN_METHOD;
  private static final Field CLEANER_FIELD;
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(CleanerJava6.class);
  


  static
  {
    Throwable error = null;
    ByteBuffer direct = ByteBuffer.allocateDirect(1);
    long fieldOffset;
    Method clean; Field cleanerField; try { Object mayBeCleanerField = AccessController.doPrivileged(new PrivilegedAction()
      {
        public Object run() {
          try {
            Field cleanerField = val$direct.getClass().getDeclaredField("cleaner");
            if (!PlatformDependent.hasUnsafe())
            {

              cleanerField.setAccessible(true);
            }
            return cleanerField;
          } catch (Throwable cause) {
            return cause;
          }
        }
      });
      if ((mayBeCleanerField instanceof Throwable)) {
        throw ((Throwable)mayBeCleanerField);
      }
      
      Field cleanerField = (Field)mayBeCleanerField;
      
      Object cleaner;
      
      Object cleaner;
      
      if (PlatformDependent.hasUnsafe()) {
        long fieldOffset = PlatformDependent0.objectFieldOffset(cleanerField);
        cleaner = PlatformDependent0.getObject(direct, fieldOffset);
      } else {
        long fieldOffset = -1L;
        cleaner = cleanerField.get(direct);
      }
      Method clean = cleaner.getClass().getDeclaredMethod("clean", new Class[0]);
      clean.invoke(cleaner, new Object[0]);
    }
    catch (Throwable t) {
      fieldOffset = -1L;
      clean = null;
      error = t;
      cleanerField = null;
    }
    
    if (error == null) {
      logger.debug("java.nio.ByteBuffer.cleaner(): available");
    } else {
      logger.debug("java.nio.ByteBuffer.cleaner(): unavailable", error);
    }
    CLEANER_FIELD = cleanerField;
    CLEANER_FIELD_OFFSET = fieldOffset;
    CLEAN_METHOD = clean;
  }
  
  static boolean isSupported() {
    return (CLEANER_FIELD_OFFSET != -1L) || (CLEANER_FIELD != null);
  }
  
  public void freeDirectBuffer(ByteBuffer buffer)
  {
    if (!buffer.isDirect()) {
      return;
    }
    if (System.getSecurityManager() == null) {
      try {
        freeDirectBuffer0(buffer);
      } catch (Throwable cause) {
        PlatformDependent0.throwException(cause);
      }
    } else {
      freeDirectBufferPrivileged(buffer);
    }
  }
  
  private static void freeDirectBufferPrivileged(ByteBuffer buffer) {
    Throwable cause = (Throwable)AccessController.doPrivileged(new PrivilegedAction()
    {
      public Throwable run() {
        try {
          CleanerJava6.freeDirectBuffer0(val$buffer);
          return null;
        } catch (Throwable cause) {
          return cause;
        }
      }
    });
    if (cause != null) {
      PlatformDependent0.throwException(cause);
    }
  }
  
  private static void freeDirectBuffer0(ByteBuffer buffer) throws Exception
  {
    Object cleaner;
    Object cleaner;
    if (CLEANER_FIELD_OFFSET == -1L) {
      cleaner = CLEANER_FIELD.get(buffer);
    } else {
      cleaner = PlatformDependent0.getObject(buffer, CLEANER_FIELD_OFFSET);
    }
    if (cleaner != null) {
      CLEAN_METHOD.invoke(cleaner, new Object[0]);
    }
  }
  
  CleanerJava6() {}
}
