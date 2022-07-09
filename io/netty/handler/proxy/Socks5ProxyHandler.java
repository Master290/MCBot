package io.netty.handler.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import io.netty.util.NetUtil;
import io.netty.util.internal.StringUtil;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;

















public final class Socks5ProxyHandler
  extends ProxyHandler
{
  private static final String PROTOCOL = "socks5";
  private static final String AUTH_PASSWORD = "password";
  private static final Socks5InitialRequest INIT_REQUEST_NO_AUTH = new DefaultSocks5InitialRequest(
    Collections.singletonList(Socks5AuthMethod.NO_AUTH));
  
  private static final Socks5InitialRequest INIT_REQUEST_PASSWORD = new DefaultSocks5InitialRequest(
    Arrays.asList(new Socks5AuthMethod[] { Socks5AuthMethod.NO_AUTH, Socks5AuthMethod.PASSWORD }));
  
  private final String username;
  private final String password;
  private String decoderName;
  private String encoderName;
  
  public Socks5ProxyHandler(SocketAddress proxyAddress)
  {
    this(proxyAddress, null, null);
  }
  
  public Socks5ProxyHandler(SocketAddress proxyAddress, String username, String password) {
    super(proxyAddress);
    if ((username != null) && (username.isEmpty())) {
      username = null;
    }
    if ((password != null) && (password.isEmpty())) {
      password = null;
    }
    this.username = username;
    this.password = password;
  }
  
  public String protocol()
  {
    return "socks5";
  }
  
  public String authScheme()
  {
    return socksAuthMethod() == Socks5AuthMethod.PASSWORD ? "password" : "none";
  }
  
  public String username() {
    return username;
  }
  
  public String password() {
    return password;
  }
  
  protected void addCodec(ChannelHandlerContext ctx) throws Exception
  {
    ChannelPipeline p = ctx.pipeline();
    String name = ctx.name();
    
    Socks5InitialResponseDecoder decoder = new Socks5InitialResponseDecoder();
    p.addBefore(name, null, decoder);
    
    decoderName = p.context(decoder).name();
    encoderName = (decoderName + ".encoder");
    
    p.addBefore(name, encoderName, Socks5ClientEncoder.DEFAULT);
  }
  
  protected void removeEncoder(ChannelHandlerContext ctx) throws Exception
  {
    ctx.pipeline().remove(encoderName);
  }
  
  protected void removeDecoder(ChannelHandlerContext ctx) throws Exception
  {
    ChannelPipeline p = ctx.pipeline();
    if (p.context(decoderName) != null) {
      p.remove(decoderName);
    }
  }
  
  protected Object newInitialMessage(ChannelHandlerContext ctx) throws Exception
  {
    return socksAuthMethod() == Socks5AuthMethod.PASSWORD ? INIT_REQUEST_PASSWORD : INIT_REQUEST_NO_AUTH;
  }
  
  protected boolean handleResponse(ChannelHandlerContext ctx, Object response) throws Exception
  {
    if ((response instanceof Socks5InitialResponse)) {
      Socks5InitialResponse res = (Socks5InitialResponse)response;
      Socks5AuthMethod authMethod = socksAuthMethod();
      
      if ((res.authMethod() != Socks5AuthMethod.NO_AUTH) && (res.authMethod() != authMethod))
      {
        throw new ProxyConnectException(exceptionMessage("unexpected authMethod: " + res.authMethod()));
      }
      
      if (authMethod == Socks5AuthMethod.NO_AUTH) {
        sendConnectCommand(ctx);
      } else if (authMethod == Socks5AuthMethod.PASSWORD)
      {
        ctx.pipeline().replace(decoderName, decoderName, new Socks5PasswordAuthResponseDecoder());
        sendToProxyServer(new DefaultSocks5PasswordAuthRequest(username != null ? username : "", password != null ? password : ""));
      }
      else
      {
        throw new Error();
      }
      
      return false;
    }
    
    if ((response instanceof Socks5PasswordAuthResponse))
    {
      Socks5PasswordAuthResponse res = (Socks5PasswordAuthResponse)response;
      if (res.status() != Socks5PasswordAuthStatus.SUCCESS) {
        throw new ProxyConnectException(exceptionMessage("authStatus: " + res.status()));
      }
      
      sendConnectCommand(ctx);
      return false;
    }
    

    Socks5CommandResponse res = (Socks5CommandResponse)response;
    if (res.status() != Socks5CommandStatus.SUCCESS) {
      throw new ProxyConnectException(exceptionMessage("status: " + res.status()));
    }
    
    return true;
  }
  
  private Socks5AuthMethod socksAuthMethod() { Socks5AuthMethod authMethod;
    Socks5AuthMethod authMethod;
    if ((username == null) && (password == null)) {
      authMethod = Socks5AuthMethod.NO_AUTH;
    } else {
      authMethod = Socks5AuthMethod.PASSWORD;
    }
    return authMethod;
  }
  
  private void sendConnectCommand(ChannelHandlerContext ctx) throws Exception {
    InetSocketAddress raddr = (InetSocketAddress)destinationAddress();
    String rhost;
    String rhost;
    if (raddr.isUnresolved()) {
      Socks5AddressType addrType = Socks5AddressType.DOMAIN;
      rhost = raddr.getHostString();
    } else {
      rhost = raddr.getAddress().getHostAddress();
      Socks5AddressType addrType; if (NetUtil.isValidIpV4Address(rhost)) {
        addrType = Socks5AddressType.IPv4; } else { Socks5AddressType addrType;
        if (NetUtil.isValidIpV6Address(rhost)) {
          addrType = Socks5AddressType.IPv6;
        }
        else
          throw new ProxyConnectException(exceptionMessage("unknown address type: " + StringUtil.simpleClassName(rhost)));
      }
    }
    Socks5AddressType addrType;
    ctx.pipeline().replace(decoderName, decoderName, new Socks5CommandResponseDecoder());
    sendToProxyServer(new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, addrType, rhost, raddr.getPort()));
  }
}
