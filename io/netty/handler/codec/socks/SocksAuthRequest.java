package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.nio.charset.CharsetEncoder;





















public final class SocksAuthRequest
  extends SocksRequest
{
  private static final SocksSubnegotiationVersion SUBNEGOTIATION_VERSION = SocksSubnegotiationVersion.AUTH_PASSWORD;
  private final String username;
  private final String password;
  
  public SocksAuthRequest(String username, String password) {
    super(SocksRequestType.AUTH);
    ObjectUtil.checkNotNull(username, "username");
    ObjectUtil.checkNotNull(password, "password");
    CharsetEncoder asciiEncoder = CharsetUtil.encoder(CharsetUtil.US_ASCII);
    if ((!asciiEncoder.canEncode(username)) || (!asciiEncoder.canEncode(password))) {
      throw new IllegalArgumentException("username: " + username + " or password: **** values should be in pure ascii");
    }
    
    if (username.length() > 255) {
      throw new IllegalArgumentException("username: " + username + " exceeds 255 char limit");
    }
    if (password.length() > 255) {
      throw new IllegalArgumentException("password: **** exceeds 255 char limit");
    }
    this.username = username;
    this.password = password;
  }
  




  public String username()
  {
    return username;
  }
  




  public String password()
  {
    return password;
  }
  
  public void encodeAsByteBuf(ByteBuf byteBuf)
  {
    byteBuf.writeByte(SUBNEGOTIATION_VERSION.byteValue());
    byteBuf.writeByte(username.length());
    byteBuf.writeCharSequence(username, CharsetUtil.US_ASCII);
    byteBuf.writeByte(password.length());
    byteBuf.writeCharSequence(password, CharsetUtil.US_ASCII);
  }
}
