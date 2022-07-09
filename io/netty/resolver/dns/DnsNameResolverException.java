package io.netty.resolver.dns;

import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import java.net.InetSocketAddress;



















public class DnsNameResolverException
  extends RuntimeException
{
  private static final long serialVersionUID = -8826717909627131850L;
  private final InetSocketAddress remoteAddress;
  private final DnsQuestion question;
  
  public DnsNameResolverException(InetSocketAddress remoteAddress, DnsQuestion question, String message)
  {
    super(message);
    this.remoteAddress = validateRemoteAddress(remoteAddress);
    this.question = validateQuestion(question);
  }
  
  public DnsNameResolverException(InetSocketAddress remoteAddress, DnsQuestion question, String message, Throwable cause)
  {
    super(message, cause);
    this.remoteAddress = validateRemoteAddress(remoteAddress);
    this.question = validateQuestion(question);
  }
  
  private static InetSocketAddress validateRemoteAddress(InetSocketAddress remoteAddress) {
    return (InetSocketAddress)ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");
  }
  
  private static DnsQuestion validateQuestion(DnsQuestion question) {
    return (DnsQuestion)ObjectUtil.checkNotNull(question, "question");
  }
  


  public InetSocketAddress remoteAddress()
  {
    return remoteAddress;
  }
  


  public DnsQuestion question()
  {
    return question;
  }
  

  public Throwable fillInStackTrace()
  {
    setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    return this;
  }
}
