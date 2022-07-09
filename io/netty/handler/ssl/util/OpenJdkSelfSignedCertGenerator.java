package io.netty.handler.ssl.util;

import io.netty.util.internal.SuppressJava6Requirement;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Date;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;






















final class OpenJdkSelfSignedCertGenerator
{
  @SuppressJava6Requirement(reason="Usage guarded by dependency check")
  static String[] generate(String fqdn, KeyPair keypair, SecureRandom random, Date notBefore, Date notAfter, String algorithm)
    throws Exception
  {
    PrivateKey key = keypair.getPrivate();
    

    X509CertInfo info = new X509CertInfo();
    X500Name owner = new X500Name("CN=" + fqdn);
    info.set("version", new CertificateVersion(2));
    info.set("serialNumber", new CertificateSerialNumber(new BigInteger(64, random)));
    try {
      info.set("subject", new CertificateSubjectName(owner));
    } catch (CertificateException ignore) {
      info.set("subject", owner);
    }
    try {
      info.set("issuer", new CertificateIssuerName(owner));
    } catch (CertificateException ignore) {
      info.set("issuer", owner);
    }
    info.set("validity", new CertificateValidity(notBefore, notAfter));
    info.set("key", new CertificateX509Key(keypair.getPublic()));
    info.set("algorithmID", new CertificateAlgorithmId(
    
      AlgorithmId.get("1.2.840.113549.1.1.11")));
    

    X509CertImpl cert = new X509CertImpl(info);
    cert.sign(key, algorithm.equalsIgnoreCase("EC") ? "SHA256withECDSA" : "SHA256withRSA");
    

    info.set("algorithmID.algorithm", cert.get("x509.algorithm"));
    cert = new X509CertImpl(info);
    cert.sign(key, algorithm.equalsIgnoreCase("EC") ? "SHA256withECDSA" : "SHA256withRSA");
    cert.verify(keypair.getPublic());
    
    return SelfSignedCertificate.newSelfSignedCertificate(fqdn, key, cert);
  }
  
  private OpenJdkSelfSignedCertGenerator() {}
}
