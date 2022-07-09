package io.netty.handler.ssl.util;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


























































public final class FingerprintTrustManagerFactory
  extends SimpleTrustManagerFactory
{
  private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("^[0-9a-fA-F:]+$");
  private static final Pattern FINGERPRINT_STRIP_PATTERN = Pattern.compile(":");
  

  private final FastThreadLocal<MessageDigest> tlmd;
  


  public static FingerprintTrustManagerFactoryBuilder builder(String algorithm)
  {
    return new FingerprintTrustManagerFactoryBuilder(algorithm);
  }
  


  private final TrustManager tm = new X509TrustManager()
  {
    public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException
    {
      checkTrusted("client", chain);
    }
    
    public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException
    {
      checkTrusted("server", chain);
    }
    
    private void checkTrusted(String type, X509Certificate[] chain) throws CertificateException {
      X509Certificate cert = chain[0];
      byte[] fingerprint = fingerprint(cert);
      boolean found = false;
      for (byte[] allowedFingerprint : fingerprints) {
        if (Arrays.equals(fingerprint, allowedFingerprint)) {
          found = true;
          break;
        }
      }
      
      if (!found)
      {
        throw new CertificateException(type + " certificate with unknown fingerprint: " + cert.getSubjectDN());
      }
    }
    
    private byte[] fingerprint(X509Certificate cert) throws CertificateEncodingException {
      MessageDigest md = (MessageDigest)tlmd.get();
      md.reset();
      return md.digest(cert.getEncoded());
    }
    
    public X509Certificate[] getAcceptedIssuers()
    {
      return EmptyArrays.EMPTY_X509_CERTIFICATES;
    }
  };
  




  private final byte[][] fingerprints;
  




  @Deprecated
  public FingerprintTrustManagerFactory(Iterable<String> fingerprints)
  {
    this("SHA1", toFingerprintArray(fingerprints));
  }
  








  @Deprecated
  public FingerprintTrustManagerFactory(String... fingerprints)
  {
    this("SHA1", toFingerprintArray(Arrays.asList(fingerprints)));
  }
  








  @Deprecated
  public FingerprintTrustManagerFactory(byte[]... fingerprints)
  {
    this("SHA1", fingerprints);
  }
  





  FingerprintTrustManagerFactory(final String algorithm, byte[][] fingerprints)
  {
    ObjectUtil.checkNotNull(algorithm, "algorithm");
    ObjectUtil.checkNotNull(fingerprints, "fingerprints");
    
    if (fingerprints.length == 0) {
      throw new IllegalArgumentException("No fingerprints provided");
    }
    

    try
    {
      md = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      MessageDigest md;
      throw new IllegalArgumentException(String.format("Unsupported hash algorithm: %s", new Object[] { algorithm }), e);
    }
    MessageDigest md;
    int hashLength = md.getDigestLength();
    List<byte[]> list = new ArrayList(fingerprints.length);
    for (byte[] f : fingerprints) {
      if (f == null) {
        break;
      }
      if (f.length != hashLength)
      {
        throw new IllegalArgumentException(String.format("malformed fingerprint (length is %d but expected %d): %s", new Object[] {
          Integer.valueOf(f.length), Integer.valueOf(hashLength), ByteBufUtil.hexDump(Unpooled.wrappedBuffer(f)) }));
      }
      list.add(f.clone());
    }
    
    tlmd = new FastThreadLocal()
    {
      protected MessageDigest initialValue()
      {
        try {
          return MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e) {
          throw new IllegalArgumentException(String.format("Unsupported hash algorithm: %s", new Object[] { algorithm }), e);
        }
        
      }
    };
    this.fingerprints = ((byte[][])list.toArray(new byte[0][]));
  }
  
  static byte[][] toFingerprintArray(Iterable<String> fingerprints) {
    ObjectUtil.checkNotNull(fingerprints, "fingerprints");
    
    List<byte[]> list = new ArrayList();
    for (String f : fingerprints) {
      if (f == null) {
        break;
      }
      
      if (!FINGERPRINT_PATTERN.matcher(f).matches()) {
        throw new IllegalArgumentException("malformed fingerprint: " + f);
      }
      f = FINGERPRINT_STRIP_PATTERN.matcher(f).replaceAll("");
      
      list.add(StringUtil.decodeHexDump(f));
    }
    
    return (byte[][])list.toArray(new byte[0][]);
  }
  
  protected void engineInit(KeyStore keyStore) throws Exception
  {}
  
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception
  {}
  
  protected TrustManager[] engineGetTrustManagers()
  {
    return new TrustManager[] { tm };
  }
}
