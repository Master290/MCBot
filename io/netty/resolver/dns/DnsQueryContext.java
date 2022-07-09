package io.netty.resolver.dns;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.dns.AbstractDnsOptPseudoRrRecord;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
















abstract class DnsQueryContext
  implements FutureListener<AddressedEnvelope<DnsResponse, InetSocketAddress>>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DnsQueryContext.class);
  
  private final DnsNameResolver parent;
  
  private final Promise<AddressedEnvelope<DnsResponse, InetSocketAddress>> promise;
  
  private final int id;
  
  private final DnsQuestion question;
  
  private final DnsRecord[] additionals;
  
  private final DnsRecord optResource;
  private final InetSocketAddress nameServerAddr;
  private final boolean recursionDesired;
  private volatile ScheduledFuture<?> timeoutFuture;
  
  DnsQueryContext(DnsNameResolver parent, InetSocketAddress nameServerAddr, DnsQuestion question, DnsRecord[] additionals, Promise<AddressedEnvelope<DnsResponse, InetSocketAddress>> promise)
  {
    this.parent = ((DnsNameResolver)ObjectUtil.checkNotNull(parent, "parent"));
    this.nameServerAddr = ((InetSocketAddress)ObjectUtil.checkNotNull(nameServerAddr, "nameServerAddr"));
    this.question = ((DnsQuestion)ObjectUtil.checkNotNull(question, "question"));
    this.additionals = ((DnsRecord[])ObjectUtil.checkNotNull(additionals, "additionals"));
    this.promise = ((Promise)ObjectUtil.checkNotNull(promise, "promise"));
    recursionDesired = parent.isRecursionDesired();
    id = queryContextManager.add(this);
    

    promise.addListener(this);
    
    if (parent.isOptResourceEnabled()) {
      optResource = new AbstractDnsOptPseudoRrRecord(parent.maxPayloadSize(), 0, 0) {};
    }
    else
    {
      optResource = null;
    }
  }
  
  InetSocketAddress nameServerAddr() {
    return nameServerAddr;
  }
  
  DnsQuestion question() {
    return question;
  }
  

  DnsNameResolver parent() { return parent; }
  
  protected abstract DnsQuery newQuery(int paramInt);
  
  protected abstract Channel channel();
  
  protected abstract String protocol();
  
  void query(boolean flush, ChannelPromise writePromise) { DnsQuestion question = question();
    InetSocketAddress nameServerAddr = nameServerAddr();
    DnsQuery query = newQuery(id);
    
    query.setRecursionDesired(recursionDesired);
    
    query.addRecord(DnsSection.QUESTION, question);
    
    for (DnsRecord record : additionals) {
      query.addRecord(DnsSection.ADDITIONAL, record);
    }
    
    if (optResource != null) {
      query.addRecord(DnsSection.ADDITIONAL, optResource);
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("{} WRITE: {}, [{}: {}], {}", new Object[] { channel(), protocol(), Integer.valueOf(id), nameServerAddr, question });
    }
    
    sendQuery(query, flush, writePromise);
  }
  
  private void sendQuery(final DnsQuery query, boolean flush, final ChannelPromise writePromise) {
    if (parent.channelFuture.isDone()) {
      writeQuery(query, flush, writePromise);
    } else {
      parent.channelFuture.addListener(new GenericFutureListener()
      {
        public void operationComplete(Future<? super Channel> future) {
          if (future.isSuccess())
          {


            DnsQueryContext.this.writeQuery(query, true, writePromise);
          } else {
            Throwable cause = future.cause();
            promise.tryFailure(cause);
            writePromise.setFailure(cause);
          }
        }
      });
    }
  }
  
  private void writeQuery(DnsQuery query, boolean flush, ChannelPromise writePromise)
  {
    final ChannelFuture writeFuture = flush ? channel().writeAndFlush(query, writePromise) : channel().write(query, writePromise);
    if (writeFuture.isDone()) {
      onQueryWriteCompletion(writeFuture);
    } else {
      writeFuture.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future) {
          DnsQueryContext.this.onQueryWriteCompletion(writeFuture);
        }
      });
    }
  }
  
  private void onQueryWriteCompletion(ChannelFuture writeFuture) {
    if (!writeFuture.isSuccess()) {
      tryFailure("failed to send a query via " + protocol(), writeFuture.cause(), false);
      return;
    }
    

    final long queryTimeoutMillis = parent.queryTimeoutMillis();
    if (queryTimeoutMillis > 0L) {
      timeoutFuture = parent.ch.eventLoop().schedule(new Runnable()
      {
        public void run() {
          if (promise.isDone())
          {
            return;
          }
          
          tryFailure("query via " + protocol() + " timed out after " + queryTimeoutMillis + " milliseconds", null, true); } }, queryTimeoutMillis, TimeUnit.MILLISECONDS);
    }
  }
  





  void finish(AddressedEnvelope<? extends DnsResponse, InetSocketAddress> envelope)
  {
    DnsResponse res = (DnsResponse)envelope.content();
    if (res.count(DnsSection.QUESTION) != 1) {
      logger.warn("Received a DNS response with invalid number of questions: {}", envelope);
    } else if (!question().equals(res.recordAt(DnsSection.QUESTION))) {
      logger.warn("Received a mismatching DNS response: {}", envelope);
    } else if (trySuccess(envelope)) {
      return;
    }
    envelope.release();
  }
  
  private boolean trySuccess(AddressedEnvelope<? extends DnsResponse, InetSocketAddress> envelope)
  {
    return promise.trySuccess(envelope);
  }
  
  boolean tryFailure(String message, Throwable cause, boolean timeout) {
    if (promise.isDone()) {
      return false;
    }
    InetSocketAddress nameServerAddr = nameServerAddr();
    
    StringBuilder buf = new StringBuilder(message.length() + 64);
    buf.append('[')
      .append(nameServerAddr)
      .append("] ")
      .append(message)
      .append(" (no stack trace available)");
    DnsNameResolverException e;
    DnsNameResolverException e;
    if (timeout)
    {

      e = new DnsNameResolverTimeoutException(nameServerAddr, question(), buf.toString());
    } else {
      e = new DnsNameResolverException(nameServerAddr, question(), buf.toString(), cause);
    }
    return promise.tryFailure(e);
  }
  

  public void operationComplete(Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> future)
  {
    ScheduledFuture<?> timeoutFuture = this.timeoutFuture;
    if (timeoutFuture != null) {
      this.timeoutFuture = null;
      timeoutFuture.cancel(false);
    }
    


    parent.queryContextManager.remove(nameServerAddr, id);
  }
}
