package io.netty.util.internal;

import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;









public final class NativeLibraryLoader
{
  private static final InternalLogger logger;
  private static final String NATIVE_RESOURCE_HOME = "META-INF/native/";
  private static final File WORKDIR;
  private static final boolean DELETE_NATIVE_LIB_AFTER_LOADING;
  private static final boolean TRY_TO_PATCH_SHADED_ID;
  private static final byte[] UNIQUE_ID_BYTES;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(NativeLibraryLoader.class);
    







    UNIQUE_ID_BYTES = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(CharsetUtil.US_ASCII);
    

    String workdir = SystemPropertyUtil.get("io.netty.native.workdir");
    if (workdir != null) {
      File f = new File(workdir);
      f.mkdirs();
      try
      {
        f = f.getAbsoluteFile();
      }
      catch (Exception localException) {}
      

      WORKDIR = f;
      logger.debug("-Dio.netty.native.workdir: " + WORKDIR);
    } else {
      WORKDIR = PlatformDependent.tmpdir();
      logger.debug("-Dio.netty.native.workdir: " + WORKDIR + " (io.netty.tmpdir)");
    }
    
    DELETE_NATIVE_LIB_AFTER_LOADING = SystemPropertyUtil.getBoolean("io.netty.native.deleteLibAfterLoading", true);
    
    logger.debug("-Dio.netty.native.deleteLibAfterLoading: {}", Boolean.valueOf(DELETE_NATIVE_LIB_AFTER_LOADING));
    
    TRY_TO_PATCH_SHADED_ID = SystemPropertyUtil.getBoolean("io.netty.native.tryPatchShadedId", true);
    
    logger.debug("-Dio.netty.native.tryPatchShadedId: {}", Boolean.valueOf(TRY_TO_PATCH_SHADED_ID));
  }
  






  public static void loadFirstAvailable(ClassLoader loader, String... names)
  {
    List<Throwable> suppressed = new ArrayList();
    for (String name : names) {
      try {
        load(name, loader);
        return;
      } catch (Throwable t) {
        suppressed.add(t);
      }
    }
    

    IllegalArgumentException iae = new IllegalArgumentException("Failed to load any of the given libraries: " + Arrays.toString(names));
    ThrowableUtil.addSuppressedAndClear(iae, suppressed);
    throw iae;
  }
  




  private static String calculatePackagePrefix()
  {
    String maybeShaded = NativeLibraryLoader.class.getName();
    
    String expected = "io!netty!util!internal!NativeLibraryLoader".replace('!', '.');
    if (!maybeShaded.endsWith(expected)) {
      throw new UnsatisfiedLinkError(String.format("Could not find prefix added to %s to get %s. When shading, only adding a package prefix is supported", new Object[] { expected, maybeShaded }));
    }
    

    return maybeShaded.substring(0, maybeShaded.length() - expected.length());
  }
  



  public static void load(String originalName, ClassLoader loader)
  {
    String packagePrefix = calculatePackagePrefix().replace('.', '_');
    String name = packagePrefix + originalName;
    List<Throwable> suppressed = new ArrayList();
    try
    {
      loadLibrary(loader, name, false);
      return;
    } catch (Throwable ex) {
      suppressed.add(ex);
      

      String libname = System.mapLibraryName(name);
      String path = "META-INF/native/" + libname;
      
      InputStream in = null;
      OutputStream out = null;
      File tmpFile = null;
      URL url;
      URL url; if (loader == null) {
        url = ClassLoader.getSystemResource(path);
      } else {
        url = loader.getResource(path);
      }
      try {
        if (url == null) {
          if (PlatformDependent.isOsx()) {
            String fileName = "META-INF/native/lib" + name + ".jnilib";
            
            if (loader == null) {
              url = ClassLoader.getSystemResource(fileName);
            } else {
              url = loader.getResource(fileName);
            }
            if (url == null) {
              FileNotFoundException fnf = new FileNotFoundException(fileName);
              ThrowableUtil.addSuppressedAndClear(fnf, suppressed);
              throw fnf;
            }
          } else {
            FileNotFoundException fnf = new FileNotFoundException(path);
            ThrowableUtil.addSuppressedAndClear(fnf, suppressed);
            throw fnf;
          }
        }
        
        int index = libname.lastIndexOf('.');
        String prefix = libname.substring(0, index);
        String suffix = libname.substring(index);
        
        tmpFile = PlatformDependent.createTempFile(prefix, suffix, WORKDIR);
        in = url.openStream();
        out = new FileOutputStream(tmpFile);
        
        if (shouldShadedLibraryIdBePatched(packagePrefix)) {
          patchShadedLibraryId(in, out, originalName, name);
        } else {
          byte[] buffer = new byte[' '];
          int length;
          while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
          }
        }
        
        out.flush();
        


        closeQuietly(out);
        out = null;
        loadLibrary(loader, tmpFile.getPath(), true);
      } catch (UnsatisfiedLinkError e) {
        try {
          if ((tmpFile != null) && (tmpFile.isFile()) && (tmpFile.canRead()) && 
            (!NoexecVolumeDetector.canExecuteExecutable(tmpFile)))
          {


            logger.info("{} exists but cannot be executed even when execute permissions set; check volume for \"noexec\" flag; use -D{}=[path] to set native working directory separately.", tmpFile
            

              .getPath(), "io.netty.native.workdir");
          }
        } catch (Throwable t) {
          suppressed.add(t);
          logger.debug("Error checking if {} is on a file store mounted with noexec", tmpFile, t);
        }
        
        ThrowableUtil.addSuppressedAndClear(e, suppressed);
        throw e;
      } catch (Exception e) {
        UnsatisfiedLinkError ule = new UnsatisfiedLinkError("could not load a native library: " + name);
        ule.initCause(e);
        ThrowableUtil.addSuppressedAndClear(ule, suppressed);
        throw ule;
      } finally {
        closeQuietly(in);
        closeQuietly(out);
        


        if ((tmpFile != null) && ((!DELETE_NATIVE_LIB_AFTER_LOADING) || (!tmpFile.delete()))) {
          tmpFile.deleteOnExit();
        }
      }
    }
  }
  
  static boolean patchShadedLibraryId(InputStream in, OutputStream out, String originalName, String name) throws IOException
  {
    byte[] buffer = new byte[' '];
    

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(in.available());
    int length;
    while ((length = in.read(buffer)) > 0) {
      byteArrayOutputStream.write(buffer, 0, length);
    }
    byteArrayOutputStream.flush();
    byte[] bytes = byteArrayOutputStream.toByteArray();
    byteArrayOutputStream.close();
    
    boolean patched;
    boolean patched;
    if (!patchShadedLibraryId(bytes, originalName, name))
    {

      String os = PlatformDependent.normalizedOs();
      String arch = PlatformDependent.normalizedArch();
      String osArch = "_" + os + "_" + arch;
      boolean patched; if (originalName.endsWith(osArch)) {
        patched = patchShadedLibraryId(bytes, originalName
          .substring(0, originalName.length() - osArch.length()), name);
      } else {
        patched = false;
      }
    } else {
      patched = true;
    }
    out.write(bytes, 0, bytes.length);
    return patched;
  }
  
  private static boolean shouldShadedLibraryIdBePatched(String packagePrefix) {
    return (TRY_TO_PATCH_SHADED_ID) && (PlatformDependent.isOsx()) && (!packagePrefix.isEmpty());
  }
  




  private static boolean patchShadedLibraryId(byte[] bytes, String originalName, String name)
  {
    byte[] nameBytes = originalName.getBytes(CharsetUtil.UTF_8);
    int idIdx = -1;
    

    int j;
    

    for (int i = 0; (i < bytes.length) && (bytes.length - i >= nameBytes.length); i++) {
      int idx = i;
      for (j = 0; (j < nameBytes.length) && 
            (bytes[(idx++)] == nameBytes[(j++)]);)
      {

        if (j == nameBytes.length)
        {
          idIdx = i;
          break label85;
        }
      }
    }
    label85:
    if (idIdx == -1) {
      logger.debug("Was not able to find the ID of the shaded native library {}, can't adjust it.", name);
      return false;
    }
    
    for (int i = 0; i < nameBytes.length; i++)
    {

      bytes[(idIdx + i)] = UNIQUE_ID_BYTES[PlatformDependent.threadLocalRandom().nextInt(UNIQUE_ID_BYTES.length)];
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("Found the ID of the shaded native library {}. Replacing ID part {} with {}", new Object[] { name, originalName, new String(bytes, idIdx, nameBytes.length, CharsetUtil.UTF_8) });
    }
    

    return true;
  }
  






  private static void loadLibrary(ClassLoader loader, String name, boolean absolute)
  {
    Throwable suppressed = null;
    try
    {
      try {
        Class<?> newHelper = tryToLoadClass(loader, NativeLibraryUtil.class);
        loadLibraryByHelper(newHelper, name, absolute);
        logger.debug("Successfully loaded the library {}", name);
        return;
      } catch (UnsatisfiedLinkError e) {
        suppressed = e;
      } catch (Exception e) {
        suppressed = e;
      }
      NativeLibraryUtil.loadLibrary(name, absolute);
      logger.debug("Successfully loaded the library {}", name);
    } catch (NoSuchMethodError nsme) {
      if (suppressed != null) {
        ThrowableUtil.addSuppressed(nsme, suppressed);
      }
      rethrowWithMoreDetailsIfPossible(name, nsme);
    } catch (UnsatisfiedLinkError ule) {
      if (suppressed != null) {
        ThrowableUtil.addSuppressed(ule, suppressed);
      }
      throw ule;
    }
  }
  
  @SuppressJava6Requirement(reason="Guarded by version check")
  private static void rethrowWithMoreDetailsIfPossible(String name, NoSuchMethodError error) {
    if (PlatformDependent.javaVersion() >= 7) {
      throw new LinkageError("Possible multiple incompatible native libraries on the classpath for '" + name + "'?", error);
    }
    
    throw error;
  }
  
  private static void loadLibraryByHelper(Class<?> helper, final String name, final boolean absolute) throws UnsatisfiedLinkError
  {
    Object ret = AccessController.doPrivileged(new PrivilegedAction()
    {
      public Object run()
      {
        try
        {
          Method method = val$helper.getMethod("loadLibrary", new Class[] { String.class, Boolean.TYPE });
          method.setAccessible(true);
          return method.invoke(null, new Object[] { name, Boolean.valueOf(absolute) });
        } catch (Exception e) {
          return e;
        }
      }
    });
    if ((ret instanceof Throwable)) {
      Throwable t = (Throwable)ret;
      assert (!(t instanceof UnsatisfiedLinkError)) : (t + " should be a wrapper throwable");
      Throwable cause = t.getCause();
      if ((cause instanceof UnsatisfiedLinkError)) {
        throw ((UnsatisfiedLinkError)cause);
      }
      UnsatisfiedLinkError ule = new UnsatisfiedLinkError(t.getMessage());
      ule.initCause(t);
      throw ule;
    }
  }
  





  private static Class<?> tryToLoadClass(ClassLoader loader, final Class<?> helper)
    throws ClassNotFoundException
  {
    try
    {
      return Class.forName(helper.getName(), false, loader);
    } catch (ClassNotFoundException e1) {
      if (loader == null)
      {
        throw e1;
      }
      try
      {
        final byte[] classBinary = classToByteArray(helper);
        (Class)AccessController.doPrivileged(new PrivilegedAction()
        {
          public Class<?> run()
          {
            try
            {
              Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] { String.class, [B.class, Integer.TYPE, Integer.TYPE });
              
              defineClass.setAccessible(true);
              return (Class)defineClass.invoke(val$loader, new Object[] { helper.getName(), classBinary, Integer.valueOf(0), 
                Integer.valueOf(classBinary.length) });
            } catch (Exception e) {
              throw new IllegalStateException("Define class failed!", e);
            }
          }
        });
      } catch (ClassNotFoundException e2) {
        ThrowableUtil.addSuppressed(e2, e1);
        throw e2;
      } catch (RuntimeException e2) {
        ThrowableUtil.addSuppressed(e2, e1);
        throw e2;
      } catch (Error e2) {
        ThrowableUtil.addSuppressed(e2, e1);
        throw e2;
      }
    }
  }
  




  private static byte[] classToByteArray(Class<?> clazz)
    throws ClassNotFoundException
  {
    String fileName = clazz.getName();
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot > 0) {
      fileName = fileName.substring(lastDot + 1);
    }
    URL classUrl = clazz.getResource(fileName + ".class");
    if (classUrl == null) {
      throw new ClassNotFoundException(clazz.getName());
    }
    byte[] buf = new byte['Ѐ'];
    ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
    InputStream in = null;
    try {
      in = classUrl.openStream();
      int r; while ((r = in.read(buf)) != -1) {
        out.write(buf, 0, r);
      }
      return out.toByteArray();
    } catch (IOException ex) {
      throw new ClassNotFoundException(clazz.getName(), ex);
    } finally {
      closeQuietly(in);
      closeQuietly(out);
    }
  }
  
  private static void closeQuietly(Closeable c) {
    if (c != null) {
      try {
        c.close();
      }
      catch (IOException localIOException) {}
    }
  }
  

  private NativeLibraryLoader() {}
  
  private static final class NoexecVolumeDetector
  {
    @SuppressJava6Requirement(reason="Usage guarded by java version check")
    private static boolean canExecuteExecutable(File file)
      throws IOException
    {
      if (PlatformDependent.javaVersion() < 7)
      {

        return true;
      }
      

      if (file.canExecute()) {
        return true;
      }
      







      Set<PosixFilePermission> existingFilePermissions = Files.getPosixFilePermissions(file.toPath(), new LinkOption[0]);
      
      Set<PosixFilePermission> executePermissions = EnumSet.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE);
      

      if (existingFilePermissions.containsAll(executePermissions)) {
        return false;
      }
      
      Set<PosixFilePermission> newPermissions = EnumSet.copyOf(existingFilePermissions);
      newPermissions.addAll(executePermissions);
      Files.setPosixFilePermissions(file.toPath(), newPermissions);
      return file.canExecute();
    }
    
    private NoexecVolumeDetector() {}
  }
}
