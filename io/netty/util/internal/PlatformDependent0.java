package io.netty.util.internal;

import io.netty.util.internal.logging.InternalLogger;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import sun.misc.Unsafe;

@SuppressJava6Requirement(reason="Unsafe access is guarded")
final class PlatformDependent0
{
  private static final InternalLogger logger;
  private static final long ADDRESS_FIELD_OFFSET;
  private static final long BYTE_ARRAY_BASE_OFFSET;
  private static final long INT_ARRAY_BASE_OFFSET;
  private static final long INT_ARRAY_INDEX_SCALE;
  private static final long LONG_ARRAY_BASE_OFFSET;
  private static final long LONG_ARRAY_INDEX_SCALE;
  private static final Constructor<?> DIRECT_BUFFER_CONSTRUCTOR;
  private static final Throwable EXPLICIT_NO_UNSAFE_CAUSE;
  private static final Method ALLOCATE_ARRAY_METHOD;
  private static final Method ALIGN_SLICE;
  private static final int JAVA_VERSION;
  private static final boolean IS_ANDROID;
  private static final Throwable UNSAFE_UNAVAILABILITY_CAUSE;
  private static final Object INTERNAL_UNSAFE;
  private static final boolean IS_EXPLICIT_TRY_REFLECTION_SET_ACCESSIBLE;
  private static final boolean RUNNING_IN_NATIVE_IMAGE;
  static final Unsafe UNSAFE;
  static final int HASH_CODE_ASCII_SEED = -1028477387;
  static final int HASH_CODE_C1 = -862048943;
  static final int HASH_CODE_C2 = 461845907;
  private static final long UNSAFE_COPY_THRESHOLD = 1048576L;
  private static final boolean UNALIGNED;
  
  static
  {
    logger = io.netty.util.internal.logging.InternalLoggerFactory.getInstance(PlatformDependent0.class);
    






    EXPLICIT_NO_UNSAFE_CAUSE = explicitNoUnsafeCause0();
    

    JAVA_VERSION = javaVersion0();
    IS_ANDROID = isAndroid0();
    


    IS_EXPLICIT_TRY_REFLECTION_SET_ACCESSIBLE = explicitTryReflectionSetAccessible0();
    


    RUNNING_IN_NATIVE_IMAGE = SystemPropertyUtil.contains("org.graalvm.nativeimage.imagecode");
    


















    java.lang.reflect.Field addressField = null;
    Method allocateArrayMethod = null;
    Throwable unsafeUnavailabilityCause = null;
    
    Object internalUnsafe = null;
    final ByteBuffer direct;
    Unsafe unsafe; if ((unsafeUnavailabilityCause = EXPLICIT_NO_UNSAFE_CAUSE) != null) {
      ByteBuffer direct = null;
      addressField = null;
      Unsafe unsafe = null;
      internalUnsafe = null;
    } else {
      direct = ByteBuffer.allocateDirect(1);
      

      Object maybeUnsafe = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
      {
        public Object run() {
          try {
            java.lang.reflect.Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            

            Throwable cause = ReflectionUtil.trySetAccessible(unsafeField, false);
            if (cause != null) {
              return cause;
            }
            
            return unsafeField.get(null);
          } catch (NoSuchFieldException e) {
            return e;
          } catch (SecurityException e) {
            return e;
          } catch (IllegalAccessException e) {
            return e;
          }
          catch (NoClassDefFoundError e)
          {
            return e;
          }
        }
      });
      




      if ((maybeUnsafe instanceof Throwable)) {
        Unsafe unsafe = null;
        unsafeUnavailabilityCause = (Throwable)maybeUnsafe;
        if (logger.isTraceEnabled()) {
          logger.debug("sun.misc.Unsafe.theUnsafe: unavailable", (Throwable)maybeUnsafe);
        } else {
          logger.debug("sun.misc.Unsafe.theUnsafe: unavailable: {}", ((Throwable)maybeUnsafe).getMessage());
        }
      } else {
        unsafe = (Unsafe)maybeUnsafe;
        logger.debug("sun.misc.Unsafe.theUnsafe: available");
      }
      



      if (unsafe != null) {
        Unsafe finalUnsafe = unsafe;
        Object maybeException = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
        {
          public Object run() {
            try {
              val$finalUnsafe.getClass().getDeclaredMethod("copyMemory", new Class[] { Object.class, Long.TYPE, Object.class, Long.TYPE, Long.TYPE });
              
              return null;
            } catch (NoSuchMethodException e) {
              return e;
            } catch (SecurityException e) {
              return e;
            }
          }
        });
        
        if (maybeException == null) {
          logger.debug("sun.misc.Unsafe.copyMemory: available");
        }
        else {
          unsafe = null;
          unsafeUnavailabilityCause = (Throwable)maybeException;
          if (logger.isTraceEnabled()) {
            logger.debug("sun.misc.Unsafe.copyMemory: unavailable", (Throwable)maybeException);
          } else {
            logger.debug("sun.misc.Unsafe.copyMemory: unavailable: {}", ((Throwable)maybeException)
              .getMessage());
          }
        }
      }
      
      if (unsafe != null) {
        Unsafe finalUnsafe = unsafe;
        

        Object maybeAddressField = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
        {
          public Object run() {
            try {
              java.lang.reflect.Field field = java.nio.Buffer.class.getDeclaredField("address");
              

              long offset = val$finalUnsafe.objectFieldOffset(field);
              long address = val$finalUnsafe.getLong(direct, offset);
              

              if (address == 0L) {
                return null;
              }
              return field;
            } catch (NoSuchFieldException e) {
              return e;
            } catch (SecurityException e) {
              return e;
            }
          }
        });
        
        if ((maybeAddressField instanceof java.lang.reflect.Field)) {
          addressField = (java.lang.reflect.Field)maybeAddressField;
          logger.debug("java.nio.Buffer.address: available");
        } else {
          unsafeUnavailabilityCause = (Throwable)maybeAddressField;
          if (logger.isTraceEnabled()) {
            logger.debug("java.nio.Buffer.address: unavailable", (Throwable)maybeAddressField);
          } else {
            logger.debug("java.nio.Buffer.address: unavailable: {}", ((Throwable)maybeAddressField)
              .getMessage());
          }
          


          unsafe = null;
        }
      }
      
      if (unsafe != null)
      {

        long byteArrayIndexScale = unsafe.arrayIndexScale([B.class);
        if (byteArrayIndexScale != 1L) {
          logger.debug("unsafe.arrayIndexScale is {} (expected: 1). Not using unsafe.", Long.valueOf(byteArrayIndexScale));
          unsafeUnavailabilityCause = new UnsupportedOperationException("Unexpected unsafe.arrayIndexScale");
          unsafe = null;
        }
      }
    }
    UNSAFE_UNAVAILABILITY_CAUSE = unsafeUnavailabilityCause;
    UNSAFE = unsafe;
    
    if (unsafe == null) {
      ADDRESS_FIELD_OFFSET = -1L;
      BYTE_ARRAY_BASE_OFFSET = -1L;
      LONG_ARRAY_BASE_OFFSET = -1L;
      LONG_ARRAY_INDEX_SCALE = -1L;
      INT_ARRAY_BASE_OFFSET = -1L;
      INT_ARRAY_INDEX_SCALE = -1L;
      UNALIGNED = false;
      DIRECT_BUFFER_CONSTRUCTOR = null;
      ALLOCATE_ARRAY_METHOD = null;
    }
    else {
      long address = -1L;
      try
      {
        Object maybeDirectBufferConstructor = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
        {
          public Object run()
          {
            try {
              Constructor<?> constructor = val$direct.getClass().getDeclaredConstructor(new Class[] { Long.TYPE, Integer.TYPE });
              Throwable cause = ReflectionUtil.trySetAccessible(constructor, true);
              if (cause != null) {
                return cause;
              }
              return constructor;
            } catch (NoSuchMethodException e) {
              return e;
            } catch (SecurityException e) {
              return e;
            }
          }
        });
        
        if ((maybeDirectBufferConstructor instanceof Constructor)) {
          address = UNSAFE.allocateMemory(1L);
          Constructor<?> directBufferConstructor;
          try {
            ((Constructor)maybeDirectBufferConstructor).newInstance(new Object[] { Long.valueOf(address), Integer.valueOf(1) });
            Constructor<?> directBufferConstructor = (Constructor)maybeDirectBufferConstructor;
            logger.debug("direct buffer constructor: available");
          } catch (InstantiationException e) {
            directBufferConstructor = null;
          } catch (IllegalAccessException e) { Constructor<?> directBufferConstructor;
            directBufferConstructor = null;
          } catch (InvocationTargetException e) { Constructor<?> directBufferConstructor;
            directBufferConstructor = null;
          }
        } else {
          if (logger.isTraceEnabled()) {
            logger.debug("direct buffer constructor: unavailable", (Throwable)maybeDirectBufferConstructor);
          }
          else {
            logger.debug("direct buffer constructor: unavailable: {}", ((Throwable)maybeDirectBufferConstructor)
              .getMessage());
          }
          directBufferConstructor = null;
        }
      } finally { Constructor<?> directBufferConstructor;
        if (address != -1L)
          UNSAFE.freeMemory(address);
      }
      Constructor<?> directBufferConstructor;
      DIRECT_BUFFER_CONSTRUCTOR = directBufferConstructor;
      ADDRESS_FIELD_OFFSET = objectFieldOffset(addressField);
      BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset([B.class);
      INT_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset([I.class);
      INT_ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale([I.class);
      LONG_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset([J.class);
      LONG_ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale([J.class);
      
      Object maybeUnaligned = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
      {
        public Object run()
        {
          try {
            Class<?> bitsClass = Class.forName("java.nio.Bits", false, PlatformDependent0.getSystemClassLoader());
            int version = PlatformDependent0.javaVersion();
            if ((PlatformDependent0.access$000()) && (version >= 9))
            {
              String fieldName = version >= 11 ? "UNALIGNED" : "unaligned";
              
              try
              {
                java.lang.reflect.Field unalignedField = bitsClass.getDeclaredField(fieldName);
                if (unalignedField.getType() == Boolean.TYPE) {
                  long offset = PlatformDependent0.UNSAFE.staticFieldOffset(unalignedField);
                  Object object = PlatformDependent0.UNSAFE.staticFieldBase(unalignedField);
                  return Boolean.valueOf(PlatformDependent0.UNSAFE.getBoolean(object, offset));
                }
              }
              catch (NoSuchFieldException localNoSuchFieldException) {}
            }
            


            Method unalignedMethod = bitsClass.getDeclaredMethod("unaligned", new Class[0]);
            Throwable cause = ReflectionUtil.trySetAccessible(unalignedMethod, true);
            if (cause != null) {
              return cause;
            }
            return unalignedMethod.invoke(null, new Object[0]);
          } catch (NoSuchMethodException e) {
            return e;
          } catch (SecurityException e) {
            return e;
          } catch (IllegalAccessException e) {
            return e;
          } catch (ClassNotFoundException e) {
            return e;
          } catch (InvocationTargetException e) {
            return e;
          }
        }
      });
      boolean unaligned;
      if ((maybeUnaligned instanceof Boolean)) {
        boolean unaligned = ((Boolean)maybeUnaligned).booleanValue();
        logger.debug("java.nio.Bits.unaligned: available, {}", Boolean.valueOf(unaligned));
      } else {
        String arch = SystemPropertyUtil.get("os.arch", "");
        
        unaligned = arch.matches("^(i[3-6]86|x86(_64)?|x64|amd64)$");
        Throwable t = (Throwable)maybeUnaligned;
        if (logger.isTraceEnabled()) {
          logger.debug("java.nio.Bits.unaligned: unavailable, {}", Boolean.valueOf(unaligned), t);
        } else {
          logger.debug("java.nio.Bits.unaligned: unavailable, {}, {}", Boolean.valueOf(unaligned), t.getMessage());
        }
      }
      
      UNALIGNED = unaligned;
      
      if (javaVersion() >= 9) {
        Object maybeException = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
        {

          public Object run()
          {
            try
            {
              Class<?> internalUnsafeClass = PlatformDependent0.getClassLoader(PlatformDependent0.class).loadClass("jdk.internal.misc.Unsafe");
              Method method = internalUnsafeClass.getDeclaredMethod("getUnsafe", new Class[0]);
              return method.invoke(null, new Object[0]);
            } catch (Throwable e) {
              return e;
            }
          }
        });
        if (!(maybeException instanceof Throwable)) {
          internalUnsafe = maybeException;
          Object finalInternalUnsafe = internalUnsafe;
          maybeException = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
          {
            public Object run() {
              try {
                return val$finalInternalUnsafe.getClass().getDeclaredMethod("allocateUninitializedArray", new Class[] { Class.class, Integer.TYPE });
              }
              catch (NoSuchMethodException e) {
                return e;
              } catch (SecurityException e) {
                return e;
              }
            }
          });
          
          if ((maybeException instanceof Method)) {
            try {
              Method m = (Method)maybeException;
              byte[] bytes = (byte[])m.invoke(finalInternalUnsafe, new Object[] { Byte.TYPE, Integer.valueOf(8) });
              assert (bytes.length == 8);
              allocateArrayMethod = m;
            } catch (IllegalAccessException e) {
              maybeException = e;
            } catch (InvocationTargetException e) {
              maybeException = e;
            }
          }
        }
        
        if ((maybeException instanceof Throwable)) {
          if (logger.isTraceEnabled()) {
            logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable", (Throwable)maybeException);
          }
          else {
            logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable: {}", ((Throwable)maybeException)
              .getMessage());
          }
        } else {
          logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): available");
        }
      } else {
        logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable prior to Java9");
      }
      ALLOCATE_ARRAY_METHOD = allocateArrayMethod;
    }
    
    if (javaVersion() > 9) {
      ALIGN_SLICE = (Method)java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
      {
        public Object run() {
          try {
            return ByteBuffer.class.getDeclaredMethod("alignedSlice", new Class[] { Integer.TYPE });
          } catch (Exception e) {}
          return null;
        }
        
      });
    } else {
      ALIGN_SLICE = null;
    }
    
    INTERNAL_UNSAFE = internalUnsafe;
    
    logger.debug("java.nio.DirectByteBuffer.<init>(long, int): {}", DIRECT_BUFFER_CONSTRUCTOR != null ? "available" : "unavailable");
  }
  
  private static boolean unsafeStaticFieldOffsetSupported()
  {
    return !RUNNING_IN_NATIVE_IMAGE;
  }
  
  static boolean isExplicitNoUnsafe() {
    return EXPLICIT_NO_UNSAFE_CAUSE != null;
  }
  
  private static Throwable explicitNoUnsafeCause0() {
    boolean noUnsafe = SystemPropertyUtil.getBoolean("io.netty.noUnsafe", false);
    logger.debug("-Dio.netty.noUnsafe: {}", Boolean.valueOf(noUnsafe));
    
    if (noUnsafe) {
      logger.debug("sun.misc.Unsafe: unavailable (io.netty.noUnsafe)");
      return new UnsupportedOperationException("sun.misc.Unsafe: unavailable (io.netty.noUnsafe)");
    }
    
    String unsafePropName;
    String unsafePropName;
    if (SystemPropertyUtil.contains("io.netty.tryUnsafe")) {
      unsafePropName = "io.netty.tryUnsafe";
    } else {
      unsafePropName = "org.jboss.netty.tryUnsafe";
    }
    
    if (!SystemPropertyUtil.getBoolean(unsafePropName, true)) {
      String msg = "sun.misc.Unsafe: unavailable (" + unsafePropName + ")";
      logger.debug(msg);
      return new UnsupportedOperationException(msg);
    }
    
    return null;
  }
  
  static boolean isUnaligned() {
    return UNALIGNED;
  }
  
  static boolean hasUnsafe() {
    return UNSAFE != null;
  }
  
  static Throwable getUnsafeUnavailabilityCause() {
    return UNSAFE_UNAVAILABILITY_CAUSE;
  }
  
  static boolean unalignedAccess() {
    return UNALIGNED;
  }
  
  static void throwException(Throwable cause)
  {
    UNSAFE.throwException((Throwable)ObjectUtil.checkNotNull(cause, "cause"));
  }
  
  static boolean hasDirectBufferNoCleanerConstructor() {
    return DIRECT_BUFFER_CONSTRUCTOR != null;
  }
  
  static ByteBuffer reallocateDirectNoCleaner(ByteBuffer buffer, int capacity) {
    return newDirectBuffer(UNSAFE.reallocateMemory(directBufferAddress(buffer), capacity), capacity);
  }
  


  static ByteBuffer allocateDirectNoCleaner(int capacity)
  {
    return newDirectBuffer(UNSAFE.allocateMemory(Math.max(1, capacity)), capacity);
  }
  
  static boolean hasAlignSliceMethod() {
    return ALIGN_SLICE != null;
  }
  
  static ByteBuffer alignSlice(ByteBuffer buffer, int alignment) {
    try {
      return (ByteBuffer)ALIGN_SLICE.invoke(buffer, new Object[] { Integer.valueOf(alignment) });
    } catch (IllegalAccessException e) {
      throw new Error(e);
    } catch (InvocationTargetException e) {
      throw new Error(e);
    }
  }
  
  static boolean hasAllocateArrayMethod() {
    return ALLOCATE_ARRAY_METHOD != null;
  }
  
  static byte[] allocateUninitializedArray(int size) {
    try {
      return (byte[])ALLOCATE_ARRAY_METHOD.invoke(INTERNAL_UNSAFE, new Object[] { Byte.TYPE, Integer.valueOf(size) });
    } catch (IllegalAccessException e) {
      throw new Error(e);
    } catch (InvocationTargetException e) {
      throw new Error(e);
    }
  }
  
  static ByteBuffer newDirectBuffer(long address, int capacity) {
    ObjectUtil.checkPositiveOrZero(capacity, "capacity");
    try
    {
      return (ByteBuffer)DIRECT_BUFFER_CONSTRUCTOR.newInstance(new Object[] { Long.valueOf(address), Integer.valueOf(capacity) });
    }
    catch (Throwable cause) {
      if ((cause instanceof Error)) {
        throw ((Error)cause);
      }
      throw new Error(cause);
    }
  }
  
  static long directBufferAddress(ByteBuffer buffer) {
    return getLong(buffer, ADDRESS_FIELD_OFFSET);
  }
  
  static long byteArrayBaseOffset() {
    return BYTE_ARRAY_BASE_OFFSET;
  }
  
  static Object getObject(Object object, long fieldOffset) {
    return UNSAFE.getObject(object, fieldOffset);
  }
  
  static int getInt(Object object, long fieldOffset) {
    return UNSAFE.getInt(object, fieldOffset);
  }
  
  private static long getLong(Object object, long fieldOffset) {
    return UNSAFE.getLong(object, fieldOffset);
  }
  
  static long objectFieldOffset(java.lang.reflect.Field field) {
    return UNSAFE.objectFieldOffset(field);
  }
  
  static byte getByte(long address) {
    return UNSAFE.getByte(address);
  }
  
  static short getShort(long address) {
    return UNSAFE.getShort(address);
  }
  
  static int getInt(long address) {
    return UNSAFE.getInt(address);
  }
  
  static long getLong(long address) {
    return UNSAFE.getLong(address);
  }
  
  static byte getByte(byte[] data, int index) {
    return UNSAFE.getByte(data, BYTE_ARRAY_BASE_OFFSET + index);
  }
  
  static byte getByte(byte[] data, long index) {
    return UNSAFE.getByte(data, BYTE_ARRAY_BASE_OFFSET + index);
  }
  
  static short getShort(byte[] data, int index) {
    return UNSAFE.getShort(data, BYTE_ARRAY_BASE_OFFSET + index);
  }
  
  static int getInt(byte[] data, int index) {
    return UNSAFE.getInt(data, BYTE_ARRAY_BASE_OFFSET + index);
  }
  
  static int getInt(int[] data, long index) {
    return UNSAFE.getInt(data, INT_ARRAY_BASE_OFFSET + INT_ARRAY_INDEX_SCALE * index);
  }
  
  static int getIntVolatile(long address) {
    return UNSAFE.getIntVolatile(null, address);
  }
  
  static void putIntOrdered(long adddress, int newValue) {
    UNSAFE.putOrderedInt(null, adddress, newValue);
  }
  
  static long getLong(byte[] data, int index) {
    return UNSAFE.getLong(data, BYTE_ARRAY_BASE_OFFSET + index);
  }
  
  static long getLong(long[] data, long index) {
    return UNSAFE.getLong(data, LONG_ARRAY_BASE_OFFSET + LONG_ARRAY_INDEX_SCALE * index);
  }
  
  static void putByte(long address, byte value) {
    UNSAFE.putByte(address, value);
  }
  
  static void putShort(long address, short value) {
    UNSAFE.putShort(address, value);
  }
  
  static void putInt(long address, int value) {
    UNSAFE.putInt(address, value);
  }
  
  static void putLong(long address, long value) {
    UNSAFE.putLong(address, value);
  }
  
  static void putByte(byte[] data, int index, byte value) {
    UNSAFE.putByte(data, BYTE_ARRAY_BASE_OFFSET + index, value);
  }
  
  static void putByte(Object data, long offset, byte value) {
    UNSAFE.putByte(data, offset, value);
  }
  
  static void putShort(byte[] data, int index, short value) {
    UNSAFE.putShort(data, BYTE_ARRAY_BASE_OFFSET + index, value);
  }
  
  static void putInt(byte[] data, int index, int value) {
    UNSAFE.putInt(data, BYTE_ARRAY_BASE_OFFSET + index, value);
  }
  
  static void putLong(byte[] data, int index, long value) {
    UNSAFE.putLong(data, BYTE_ARRAY_BASE_OFFSET + index, value);
  }
  
  static void putObject(Object o, long offset, Object x) {
    UNSAFE.putObject(o, offset, x);
  }
  

  static void copyMemory(long srcAddr, long dstAddr, long length)
  {
    if (javaVersion() <= 8) {
      copyMemoryWithSafePointPolling(srcAddr, dstAddr, length);
    } else {
      UNSAFE.copyMemory(srcAddr, dstAddr, length);
    }
  }
  
  private static void copyMemoryWithSafePointPolling(long srcAddr, long dstAddr, long length) {
    while (length > 0L) {
      long size = Math.min(length, 1048576L);
      UNSAFE.copyMemory(srcAddr, dstAddr, size);
      length -= size;
      srcAddr += size;
      dstAddr += size;
    }
  }
  

  static void copyMemory(Object src, long srcOffset, Object dst, long dstOffset, long length)
  {
    if (javaVersion() <= 8) {
      copyMemoryWithSafePointPolling(src, srcOffset, dst, dstOffset, length);
    } else {
      UNSAFE.copyMemory(src, srcOffset, dst, dstOffset, length);
    }
  }
  
  private static void copyMemoryWithSafePointPolling(Object src, long srcOffset, Object dst, long dstOffset, long length)
  {
    while (length > 0L) {
      long size = Math.min(length, 1048576L);
      UNSAFE.copyMemory(src, srcOffset, dst, dstOffset, size);
      length -= size;
      srcOffset += size;
      dstOffset += size;
    }
  }
  
  static void setMemory(long address, long bytes, byte value) {
    UNSAFE.setMemory(address, bytes, value);
  }
  
  static void setMemory(Object o, long offset, long bytes, byte value) {
    UNSAFE.setMemory(o, offset, bytes, value);
  }
  
  static boolean equals(byte[] bytes1, int startPos1, byte[] bytes2, int startPos2, int length) {
    int remainingBytes = length & 0x7;
    long baseOffset1 = BYTE_ARRAY_BASE_OFFSET + startPos1;
    long diff = startPos2 - startPos1;
    if (length >= 8) {
      long end = baseOffset1 + remainingBytes;
      for (long i = baseOffset1 - 8L + length; i >= end; i -= 8L) {
        if (UNSAFE.getLong(bytes1, i) != UNSAFE.getLong(bytes2, i + diff)) {
          return false;
        }
      }
    }
    if (remainingBytes >= 4) {
      remainingBytes -= 4;
      long pos = baseOffset1 + remainingBytes;
      if (UNSAFE.getInt(bytes1, pos) != UNSAFE.getInt(bytes2, pos + diff)) {
        return false;
      }
    }
    long baseOffset2 = baseOffset1 + diff;
    if (remainingBytes >= 2) {
      return (UNSAFE.getChar(bytes1, baseOffset1) == UNSAFE.getChar(bytes2, baseOffset2)) && ((remainingBytes == 2) || 
      
        (UNSAFE.getByte(bytes1, baseOffset1 + 2L) == UNSAFE.getByte(bytes2, baseOffset2 + 2L)));
    }
    return (remainingBytes == 0) || 
      (UNSAFE.getByte(bytes1, baseOffset1) == UNSAFE.getByte(bytes2, baseOffset2));
  }
  
  static int equalsConstantTime(byte[] bytes1, int startPos1, byte[] bytes2, int startPos2, int length) {
    long result = 0L;
    long remainingBytes = length & 0x7;
    long baseOffset1 = BYTE_ARRAY_BASE_OFFSET + startPos1;
    long end = baseOffset1 + remainingBytes;
    long diff = startPos2 - startPos1;
    for (long i = baseOffset1 - 8L + length; i >= end; i -= 8L) {
      result |= UNSAFE.getLong(bytes1, i) ^ UNSAFE.getLong(bytes2, i + diff);
    }
    if (remainingBytes >= 4L) {
      result |= UNSAFE.getInt(bytes1, baseOffset1) ^ UNSAFE.getInt(bytes2, baseOffset1 + diff);
      remainingBytes -= 4L;
    }
    if (remainingBytes >= 2L) {
      long pos = end - remainingBytes;
      result |= UNSAFE.getChar(bytes1, pos) ^ UNSAFE.getChar(bytes2, pos + diff);
      remainingBytes -= 2L;
    }
    if (remainingBytes == 1L) {
      long pos = end - 1L;
      result |= UNSAFE.getByte(bytes1, pos) ^ UNSAFE.getByte(bytes2, pos + diff);
    }
    return ConstantTimeUtils.equalsConstantTime(result, 0L);
  }
  
  static boolean isZero(byte[] bytes, int startPos, int length) {
    if (length <= 0) {
      return true;
    }
    long baseOffset = BYTE_ARRAY_BASE_OFFSET + startPos;
    int remainingBytes = length & 0x7;
    long end = baseOffset + remainingBytes;
    for (long i = baseOffset - 8L + length; i >= end; i -= 8L) {
      if (UNSAFE.getLong(bytes, i) != 0L) {
        return false;
      }
    }
    
    if (remainingBytes >= 4) {
      remainingBytes -= 4;
      if (UNSAFE.getInt(bytes, baseOffset + remainingBytes) != 0) {
        return false;
      }
    }
    if (remainingBytes >= 2) {
      return (UNSAFE.getChar(bytes, baseOffset) == 0) && ((remainingBytes == 2) || (bytes[(startPos + 2)] == 0));
    }
    
    return bytes[startPos] == 0;
  }
  
  static int hashCodeAscii(byte[] bytes, int startPos, int length) {
    int hash = -1028477387;
    long baseOffset = BYTE_ARRAY_BASE_OFFSET + startPos;
    int remainingBytes = length & 0x7;
    long end = baseOffset + remainingBytes;
    for (long i = baseOffset - 8L + length; i >= end; i -= 8L) {
      hash = hashCodeAsciiCompute(UNSAFE.getLong(bytes, i), hash);
    }
    if (remainingBytes == 0) {
      return hash;
    }
    int hcConst = -862048943;
    if (((remainingBytes != 2 ? 1 : 0) & (remainingBytes != 4 ? 1 : 0) & (remainingBytes != 6 ? 1 : 0)) != 0) {
      hash = hash * -862048943 + hashCodeAsciiSanitize(UNSAFE.getByte(bytes, baseOffset));
      hcConst = 461845907;
      baseOffset += 1L;
    }
    if (((remainingBytes != 1 ? 1 : 0) & (remainingBytes != 4 ? 1 : 0) & (remainingBytes != 5 ? 1 : 0)) != 0) {
      hash = hash * hcConst + hashCodeAsciiSanitize(UNSAFE.getShort(bytes, baseOffset));
      hcConst = hcConst == -862048943 ? 461845907 : -862048943;
      baseOffset += 2L;
    }
    if (remainingBytes >= 4) {
      return hash * hcConst + hashCodeAsciiSanitize(UNSAFE.getInt(bytes, baseOffset));
    }
    return hash;
  }
  

  static int hashCodeAsciiCompute(long value, int hash)
  {
    return 
    
      hash * -862048943 + hashCodeAsciiSanitize((int)value) * 461845907 + (int)((value & 0x1F1F1F1F00000000) >>> 32);
  }
  

  static int hashCodeAsciiSanitize(int value)
  {
    return value & 0x1F1F1F1F;
  }
  
  static int hashCodeAsciiSanitize(short value) {
    return value & 0x1F1F;
  }
  
  static int hashCodeAsciiSanitize(byte value) {
    return value & 0x1F;
  }
  
  static ClassLoader getClassLoader(Class<?> clazz) {
    if (System.getSecurityManager() == null) {
      return clazz.getClassLoader();
    }
    (ClassLoader)java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
    {
      public ClassLoader run() {
        return val$clazz.getClassLoader();
      }
    });
  }
  
  static ClassLoader getContextClassLoader()
  {
    if (System.getSecurityManager() == null) {
      return Thread.currentThread().getContextClassLoader();
    }
    (ClassLoader)java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
    {
      public ClassLoader run() {
        return Thread.currentThread().getContextClassLoader();
      }
    });
  }
  
  static ClassLoader getSystemClassLoader()
  {
    if (System.getSecurityManager() == null) {
      return ClassLoader.getSystemClassLoader();
    }
    (ClassLoader)java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
    {
      public ClassLoader run() {
        return ClassLoader.getSystemClassLoader();
      }
    });
  }
  
  static int addressSize()
  {
    return UNSAFE.addressSize();
  }
  
  static long allocateMemory(long size) {
    return UNSAFE.allocateMemory(size);
  }
  
  static void freeMemory(long address) {
    UNSAFE.freeMemory(address);
  }
  
  static long reallocateMemory(long address, long newSize) {
    return UNSAFE.reallocateMemory(address, newSize);
  }
  
  static boolean isAndroid() {
    return IS_ANDROID;
  }
  






  private static boolean isAndroid0()
  {
    String vmName = SystemPropertyUtil.get("java.vm.name");
    boolean isAndroid = "Dalvik".equals(vmName);
    if (isAndroid) {
      logger.debug("Platform: Android");
    }
    return isAndroid;
  }
  
  private static boolean explicitTryReflectionSetAccessible0()
  {
    return SystemPropertyUtil.getBoolean("io.netty.tryReflectionSetAccessible", javaVersion() < 9);
  }
  
  static boolean isExplicitTryReflectionSetAccessible() {
    return IS_EXPLICIT_TRY_REFLECTION_SET_ACCESSIBLE;
  }
  
  static int javaVersion() {
    return JAVA_VERSION;
  }
  
  private static int javaVersion0() {
    int majorVersion;
    int majorVersion;
    if (isAndroid0()) {
      majorVersion = 6;
    } else {
      majorVersion = majorVersionFromJavaSpecificationVersion();
    }
    
    logger.debug("Java version: {}", Integer.valueOf(majorVersion));
    
    return majorVersion;
  }
  
  static int majorVersionFromJavaSpecificationVersion()
  {
    return majorVersion(SystemPropertyUtil.get("java.specification.version", "1.6"));
  }
  
  static int majorVersion(String javaSpecVersion)
  {
    String[] components = javaSpecVersion.split("\\.");
    int[] version = new int[components.length];
    for (int i = 0; i < components.length; i++) {
      version[i] = Integer.parseInt(components[i]);
    }
    
    if (version[0] == 1) {
      assert (version[1] >= 6);
      return version[1];
    }
    return version[0];
  }
  
  private PlatformDependent0() {}
}
