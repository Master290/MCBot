package io.netty.handler.ssl.util;

import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;





























public final class FingerprintTrustManagerFactoryBuilder
{
  private final String algorithm;
  private final List<String> fingerprints = new ArrayList();
  




  FingerprintTrustManagerFactoryBuilder(String algorithm)
  {
    this.algorithm = ((String)ObjectUtil.checkNotNull(algorithm, "algorithm"));
  }
  





  public FingerprintTrustManagerFactoryBuilder fingerprints(CharSequence... fingerprints)
  {
    return fingerprints(Arrays.asList((Object[])ObjectUtil.checkNotNull(fingerprints, "fingerprints")));
  }
  





  public FingerprintTrustManagerFactoryBuilder fingerprints(Iterable<? extends CharSequence> fingerprints)
  {
    ObjectUtil.checkNotNull(fingerprints, "fingerprints");
    for (CharSequence fingerprint : fingerprints) {
      ObjectUtil.checkNotNullWithIAE(fingerprint, "fingerprint");
      this.fingerprints.add(fingerprint.toString());
    }
    return this;
  }
  




  public FingerprintTrustManagerFactory build()
  {
    if (fingerprints.isEmpty()) {
      throw new IllegalStateException("No fingerprints provided");
    }
    return new FingerprintTrustManagerFactory(algorithm, 
      FingerprintTrustManagerFactory.toFingerprintArray(fingerprints));
  }
}
