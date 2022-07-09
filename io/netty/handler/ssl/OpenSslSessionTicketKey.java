package io.netty.handler.ssl;

import io.netty.internal.tcnative.SessionTicketKey;






































public final class OpenSslSessionTicketKey
{
  public static final int NAME_SIZE = 16;
  public static final int HMAC_KEY_SIZE = 16;
  public static final int AES_KEY_SIZE = 16;
  public static final int TICKET_KEY_SIZE = 48;
  final SessionTicketKey key;
  
  public OpenSslSessionTicketKey(byte[] name, byte[] hmacKey, byte[] aesKey)
  {
    key = new SessionTicketKey((byte[])name.clone(), (byte[])hmacKey.clone(), (byte[])aesKey.clone());
  }
  



  public byte[] name()
  {
    return (byte[])key.getName().clone();
  }
  



  public byte[] hmacKey()
  {
    return (byte[])key.getHmacKey().clone();
  }
  



  public byte[] aesKey()
  {
    return (byte[])key.getAesKey().clone();
  }
}
