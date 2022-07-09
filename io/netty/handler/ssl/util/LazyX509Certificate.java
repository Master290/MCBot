package io.netty.handler.ssl.util;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SuppressJava6Requirement;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;











public final class LazyX509Certificate
  extends X509Certificate
{
  static final CertificateFactory X509_CERT_FACTORY;
  private final byte[] bytes;
  private X509Certificate wrapped;
  
  static
  {
    try
    {
      X509_CERT_FACTORY = CertificateFactory.getInstance("X.509");
    } catch (CertificateException e) {
      throw new ExceptionInInitializerError(e);
    }
  }
  





  public LazyX509Certificate(byte[] bytes)
  {
    this.bytes = ((byte[])ObjectUtil.checkNotNull(bytes, "bytes"));
  }
  
  public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException
  {
    unwrap().checkValidity();
  }
  
  public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException
  {
    unwrap().checkValidity(date);
  }
  
  public X500Principal getIssuerX500Principal()
  {
    return unwrap().getIssuerX500Principal();
  }
  
  public X500Principal getSubjectX500Principal()
  {
    return unwrap().getSubjectX500Principal();
  }
  
  public List<String> getExtendedKeyUsage() throws CertificateParsingException
  {
    return unwrap().getExtendedKeyUsage();
  }
  
  public Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException
  {
    return unwrap().getSubjectAlternativeNames();
  }
  
  public Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException
  {
    return unwrap().getSubjectAlternativeNames();
  }
  
  @SuppressJava6Requirement(reason="Can only be called from Java8 as class is package-private")
  public void verify(PublicKey key, Provider sigProvider)
    throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    unwrap().verify(key, sigProvider);
  }
  
  public int getVersion()
  {
    return unwrap().getVersion();
  }
  
  public BigInteger getSerialNumber()
  {
    return unwrap().getSerialNumber();
  }
  
  public Principal getIssuerDN()
  {
    return unwrap().getIssuerDN();
  }
  
  public Principal getSubjectDN()
  {
    return unwrap().getSubjectDN();
  }
  
  public Date getNotBefore()
  {
    return unwrap().getNotBefore();
  }
  
  public Date getNotAfter()
  {
    return unwrap().getNotAfter();
  }
  
  public byte[] getTBSCertificate() throws CertificateEncodingException
  {
    return unwrap().getTBSCertificate();
  }
  
  public byte[] getSignature()
  {
    return unwrap().getSignature();
  }
  
  public String getSigAlgName()
  {
    return unwrap().getSigAlgName();
  }
  
  public String getSigAlgOID()
  {
    return unwrap().getSigAlgOID();
  }
  
  public byte[] getSigAlgParams()
  {
    return unwrap().getSigAlgParams();
  }
  
  public boolean[] getIssuerUniqueID()
  {
    return unwrap().getIssuerUniqueID();
  }
  
  public boolean[] getSubjectUniqueID()
  {
    return unwrap().getSubjectUniqueID();
  }
  
  public boolean[] getKeyUsage()
  {
    return unwrap().getKeyUsage();
  }
  
  public int getBasicConstraints()
  {
    return unwrap().getBasicConstraints();
  }
  
  public byte[] getEncoded()
  {
    return (byte[])bytes.clone();
  }
  

  public void verify(PublicKey key)
    throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
  {
    unwrap().verify(key);
  }
  

  public void verify(PublicKey key, String sigProvider)
    throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
  {
    unwrap().verify(key, sigProvider);
  }
  
  public String toString()
  {
    return unwrap().toString();
  }
  
  public PublicKey getPublicKey()
  {
    return unwrap().getPublicKey();
  }
  
  public boolean hasUnsupportedCriticalExtension()
  {
    return unwrap().hasUnsupportedCriticalExtension();
  }
  
  public Set<String> getCriticalExtensionOIDs()
  {
    return unwrap().getCriticalExtensionOIDs();
  }
  
  public Set<String> getNonCriticalExtensionOIDs()
  {
    return unwrap().getNonCriticalExtensionOIDs();
  }
  
  public byte[] getExtensionValue(String oid)
  {
    return unwrap().getExtensionValue(oid);
  }
  
  private X509Certificate unwrap() {
    X509Certificate wrapped = this.wrapped;
    if (wrapped == null) {
      try {
        wrapped = this.wrapped = (X509Certificate)X509_CERT_FACTORY.generateCertificate(new ByteArrayInputStream(bytes));
      }
      catch (CertificateException e) {
        throw new IllegalStateException(e);
      }
    }
    return wrapped;
  }
}
