package io.netty.resolver.dns;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import java.net.InetSocketAddress;
import java.util.List;















final class LoggingDnsQueryLifecycleObserver
  implements DnsQueryLifecycleObserver
{
  private final InternalLogger logger;
  private final InternalLogLevel level;
  private final DnsQuestion question;
  private InetSocketAddress dnsServerAddress;
  
  LoggingDnsQueryLifecycleObserver(DnsQuestion question, InternalLogger logger, InternalLogLevel level)
  {
    this.question = ((DnsQuestion)ObjectUtil.checkNotNull(question, "question"));
    this.logger = ((InternalLogger)ObjectUtil.checkNotNull(logger, "logger"));
    this.level = ((InternalLogLevel)ObjectUtil.checkNotNull(level, "level"));
  }
  
  public void queryWritten(InetSocketAddress dnsServerAddress, ChannelFuture future)
  {
    this.dnsServerAddress = dnsServerAddress;
  }
  
  public void queryCancelled(int queriesRemaining)
  {
    if (dnsServerAddress != null) {
      logger.log(level, "from {} : {} cancelled with {} queries remaining", new Object[] { dnsServerAddress, question, 
        Integer.valueOf(queriesRemaining) });
    } else {
      logger.log(level, "{} query never written and cancelled with {} queries remaining", question, 
        Integer.valueOf(queriesRemaining));
    }
  }
  
  public DnsQueryLifecycleObserver queryRedirected(List<InetSocketAddress> nameServers)
  {
    logger.log(level, "from {} : {} redirected", dnsServerAddress, question);
    return this;
  }
  
  public DnsQueryLifecycleObserver queryCNAMEd(DnsQuestion cnameQuestion)
  {
    logger.log(level, "from {} : {} CNAME question {}", new Object[] { dnsServerAddress, question, cnameQuestion });
    return this;
  }
  
  public DnsQueryLifecycleObserver queryNoAnswer(DnsResponseCode code)
  {
    logger.log(level, "from {} : {} no answer {}", new Object[] { dnsServerAddress, question, code });
    return this;
  }
  
  public void queryFailed(Throwable cause)
  {
    if (dnsServerAddress != null) {
      logger.log(level, "from {} : {} failure", new Object[] { dnsServerAddress, question, cause });
    } else {
      logger.log(level, "{} query never written and failed", question, cause);
    }
  }
  
  public void querySucceed() {}
}
