package io.netty.handler.ssl.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;


































public final class SelfSignedCertificate
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(SelfSignedCertificate.class);
  

  private static final Date DEFAULT_NOT_BEFORE = new Date(SystemPropertyUtil.getLong("io.netty.selfSignedCertificate.defaultNotBefore", 
    System.currentTimeMillis() - 31536000000L));
  
  private static final Date DEFAULT_NOT_AFTER = new Date(SystemPropertyUtil.getLong("io.netty.selfSignedCertificate.defaultNotAfter", 253402300799000L));
  







  private static final int DEFAULT_KEY_LENGTH_BITS = SystemPropertyUtil.getInt("io.netty.handler.ssl.util.selfSignedKeyStrength", 2048);
  
  private final File certificate;
  
  private final File privateKey;
  
  private final X509Certificate cert;
  private final PrivateKey key;
  
  public SelfSignedCertificate()
    throws CertificateException
  {
    this(DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER, "RSA", DEFAULT_KEY_LENGTH_BITS);
  }
  






  public SelfSignedCertificate(Date notBefore, Date notAfter)
    throws CertificateException
  {
    this("localhost", notBefore, notAfter, "RSA", DEFAULT_KEY_LENGTH_BITS);
  }
  







  public SelfSignedCertificate(Date notBefore, Date notAfter, String algorithm, int bits)
    throws CertificateException
  {
    this("localhost", notBefore, notAfter, algorithm, bits);
  }
  




  public SelfSignedCertificate(String fqdn)
    throws CertificateException
  {
    this(fqdn, DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER, "RSA", DEFAULT_KEY_LENGTH_BITS);
  }
  





  public SelfSignedCertificate(String fqdn, String algorithm, int bits)
    throws CertificateException
  {
    this(fqdn, DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER, algorithm, bits);
  }
  








  public SelfSignedCertificate(String fqdn, Date notBefore, Date notAfter)
    throws CertificateException
  {
    this(fqdn, ThreadLocalInsecureRandom.current(), DEFAULT_KEY_LENGTH_BITS, notBefore, notAfter, "RSA");
  }
  










  public SelfSignedCertificate(String fqdn, Date notBefore, Date notAfter, String algorithm, int bits)
    throws CertificateException
  {
    this(fqdn, ThreadLocalInsecureRandom.current(), bits, notBefore, notAfter, algorithm);
  }
  







  public SelfSignedCertificate(String fqdn, SecureRandom random, int bits)
    throws CertificateException
  {
    this(fqdn, random, bits, DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER, "RSA");
  }
  







  public SelfSignedCertificate(String fqdn, SecureRandom random, String algorithm, int bits)
    throws CertificateException
  {
    this(fqdn, random, bits, DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER, algorithm);
  }
  









  public SelfSignedCertificate(String fqdn, SecureRandom random, int bits, Date notBefore, Date notAfter)
    throws CertificateException
  {
    this(fqdn, random, bits, notBefore, notAfter, "RSA");
  }
  










  public SelfSignedCertificate(String fqdn, SecureRandom random, int bits, Date notBefore, Date notAfter, String algorithm)
    throws CertificateException
  {
    if ((!algorithm.equalsIgnoreCase("EC")) && (!algorithm.equalsIgnoreCase("RSA"))) {
      throw new IllegalArgumentException("Algorithm not valid: " + algorithm);
    }
    
    try
    {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
      keyGen.initialize(bits, random);
      keypair = keyGen.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      KeyPair keypair;
      throw new Error(e);
    }
    
    KeyPair keypair;
    try
    {
      paths = BouncyCastleSelfSignedCertGenerator.generate(fqdn, keypair, random, notBefore, notAfter, algorithm);
    } catch (Throwable t) {
      String[] paths;
      logger.debug("Failed to generate a self-signed X.509 certificate using Bouncy Castle:", t);
      try
      {
        paths = OpenJdkSelfSignedCertGenerator.generate(fqdn, keypair, random, notBefore, notAfter, algorithm);
      } catch (Throwable t2) { String[] paths;
        logger.debug("Failed to generate a self-signed X.509 certificate using sun.security.x509:", t2);
        CertificateException certificateException = new CertificateException("No provider succeeded to generate a self-signed certificate. See debug log for the root cause.", t2);
        

        ThrowableUtil.addSuppressed(certificateException, t);
        throw certificateException;
      }
    }
    String[] paths;
    certificate = new File(paths[0]);
    privateKey = new File(paths[1]);
    key = keypair.getPrivate();
    FileInputStream certificateInput = null;
    try {
      certificateInput = new FileInputStream(certificate);
      cert = ((X509Certificate)CertificateFactory.getInstance("X509").generateCertificate(certificateInput)); return;
    } catch (Exception e) {
      throw new CertificateEncodingException(e);
    } finally {
      if (certificateInput != null) {
        try {
          certificateInput.close();
        } catch (IOException e) {
          if (logger.isWarnEnabled()) {
            logger.warn("Failed to close a file: " + certificate, e);
          }
        }
      }
    }
  }
  


  public File certificate()
  {
    return certificate;
  }
  


  public File privateKey()
  {
    return privateKey;
  }
  


  public X509Certificate cert()
  {
    return cert;
  }
  


  public PrivateKey key()
  {
    return key;
  }
  


  public void delete()
  {
    safeDelete(certificate);
    safeDelete(privateKey);
  }
  
  static String[] newSelfSignedCertificate(String fqdn, PrivateKey key, X509Certificate cert)
    throws IOException, CertificateEncodingException
  {
    ByteBuf wrappedBuf = Unpooled.wrappedBuffer(key.getEncoded());
    
    try
    {
      ByteBuf encodedBuf = Base64.encode(wrappedBuf, true);
      String keyText;
      try {
        keyText = "-----BEGIN PRIVATE KEY-----\n" + encodedBuf.toString(CharsetUtil.US_ASCII) + "\n-----END PRIVATE KEY-----\n";
      }
      finally {}
    }
    finally {
      String keyText;
      wrappedBuf.release();
    }
    String keyText;
    ByteBuf encodedBuf;
    fqdn = fqdn.replaceAll("[^\\w.-]", "x");
    
    File keyFile = PlatformDependent.createTempFile("keyutil_" + fqdn + '_', ".key", null);
    keyFile.deleteOnExit();
    
    Object keyOut = new FileOutputStream(keyFile);
    try {
      ((OutputStream)keyOut).write(keyText.getBytes(CharsetUtil.US_ASCII));
      ((OutputStream)keyOut).close();
      keyOut = null;
    } finally {
      if (keyOut != null) {
        safeClose(keyFile, (OutputStream)keyOut);
        safeDelete(keyFile);
      }
    }
    
    wrappedBuf = Unpooled.wrappedBuffer(cert.getEncoded());
    try
    {
      encodedBuf = Base64.encode(wrappedBuf, true);
      String certText;
      try
      {
        certText = "-----BEGIN CERTIFICATE-----\n" + encodedBuf.toString(CharsetUtil.US_ASCII) + "\n-----END CERTIFICATE-----\n";
      }
      finally {}
    }
    finally {
      String certText;
      wrappedBuf.release();
    }
    String certText;
    File certFile = PlatformDependent.createTempFile("keyutil_" + fqdn + '_', ".crt", null);
    certFile.deleteOnExit();
    
    Object certOut = new FileOutputStream(certFile);
    try {
      ((OutputStream)certOut).write(certText.getBytes(CharsetUtil.US_ASCII));
      ((OutputStream)certOut).close();
      certOut = null;
    } finally {
      if (certOut != null) {
        safeClose(certFile, (OutputStream)certOut);
        safeDelete(certFile);
        safeDelete(keyFile);
      }
    }
    
    return new String[] { certFile.getPath(), keyFile.getPath() };
  }
  
  private static void safeDelete(File certFile) {
    if ((!certFile.delete()) && 
      (logger.isWarnEnabled())) {
      logger.warn("Failed to delete a file: " + certFile);
    }
  }
  
  private static void safeClose(File keyFile, OutputStream keyOut)
  {
    try {
      keyOut.close();
    } catch (IOException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to close a file: " + keyFile, e);
      }
    }
  }
}
