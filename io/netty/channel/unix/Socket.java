package io.netty.channel.unix;

import io.netty.channel.ChannelException;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;






























public class Socket
  extends FileDescriptor
{
  @Deprecated
  public static final int UDS_SUN_PATH_SIZE = 100;
  protected final boolean ipv6;
  
  public Socket(int fd)
  {
    super(fd);
    ipv6 = isIPv6(fd);
  }
  


  private boolean useIpv6(InetAddress address)
  {
    return (ipv6) || ((address instanceof Inet6Address));
  }
  
  public final void shutdown() throws IOException {
    shutdown(true, true);
  }
  

  public final void shutdown(boolean read, boolean write)
    throws IOException
  {
    for (;;)
    {
      int oldState = state;
      if (isClosed(oldState)) {
        throw new ClosedChannelException();
      }
      int newState = oldState;
      if ((read) && (!isInputShutdown(newState))) {
        newState = inputShutdown(newState);
      }
      if ((write) && (!isOutputShutdown(newState))) {
        newState = outputShutdown(newState);
      }
      

      if (newState == oldState) {
        return;
      }
      if (casState(oldState, newState)) {
        break;
      }
    }
    int res = shutdown(fd, read, write);
    if (res < 0) {
      Errors.ioResult("shutdown", res);
    }
  }
  
  public final boolean isShutdown() {
    int state = this.state;
    return (isInputShutdown(state)) && (isOutputShutdown(state));
  }
  
  public final boolean isInputShutdown() {
    return isInputShutdown(state);
  }
  
  public final boolean isOutputShutdown() {
    return isOutputShutdown(state);
  }
  
  public final int sendTo(ByteBuffer buf, int pos, int limit, InetAddress addr, int port) throws IOException {
    return sendTo(buf, pos, limit, addr, port, false);
  }
  
  public final int sendTo(ByteBuffer buf, int pos, int limit, InetAddress addr, int port, boolean fastOpen)
    throws IOException
  {
    int scopeId;
    int scopeId;
    byte[] address;
    if ((addr instanceof Inet6Address)) {
      byte[] address = addr.getAddress();
      scopeId = ((Inet6Address)addr).getScopeId();
    }
    else {
      scopeId = 0;
      address = NativeInetAddress.ipv4MappedIpv6Address(addr.getAddress());
    }
    int flags = fastOpen ? msgFastopen() : 0;
    int res = sendTo(fd, useIpv6(addr), buf, pos, limit, address, scopeId, port, flags);
    if (res >= 0) {
      return res;
    }
    if ((res == Errors.ERRNO_EINPROGRESS_NEGATIVE) && (fastOpen))
    {


      return 0;
    }
    if (res == Errors.ERROR_ECONNREFUSED_NEGATIVE) {
      throw new PortUnreachableException("sendTo failed");
    }
    return Errors.ioResult("sendTo", res);
  }
  
  public final int sendToDomainSocket(ByteBuffer buf, int pos, int limit, byte[] path) throws IOException {
    int res = sendToDomainSocket(fd, buf, pos, limit, path);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("sendToDomainSocket", res);
  }
  
  public final int sendToAddress(long memoryAddress, int pos, int limit, InetAddress addr, int port) throws IOException
  {
    return sendToAddress(memoryAddress, pos, limit, addr, port, false);
  }
  
  public final int sendToAddress(long memoryAddress, int pos, int limit, InetAddress addr, int port, boolean fastOpen)
    throws IOException
  {
    int scopeId;
    int scopeId;
    byte[] address;
    if ((addr instanceof Inet6Address)) {
      byte[] address = addr.getAddress();
      scopeId = ((Inet6Address)addr).getScopeId();
    }
    else {
      scopeId = 0;
      address = NativeInetAddress.ipv4MappedIpv6Address(addr.getAddress());
    }
    int flags = fastOpen ? msgFastopen() : 0;
    int res = sendToAddress(fd, useIpv6(addr), memoryAddress, pos, limit, address, scopeId, port, flags);
    if (res >= 0) {
      return res;
    }
    if ((res == Errors.ERRNO_EINPROGRESS_NEGATIVE) && (fastOpen))
    {


      return 0;
    }
    if (res == Errors.ERROR_ECONNREFUSED_NEGATIVE) {
      throw new PortUnreachableException("sendToAddress failed");
    }
    return Errors.ioResult("sendToAddress", res);
  }
  
  public final int sendToAddressDomainSocket(long memoryAddress, int pos, int limit, byte[] path) throws IOException {
    int res = sendToAddressDomainSocket(fd, memoryAddress, pos, limit, path);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("sendToAddressDomainSocket", res);
  }
  
  public final int sendToAddresses(long memoryAddress, int length, InetAddress addr, int port) throws IOException {
    return sendToAddresses(memoryAddress, length, addr, port, false);
  }
  
  public final int sendToAddresses(long memoryAddress, int length, InetAddress addr, int port, boolean fastOpen)
    throws IOException
  {
    int scopeId;
    int scopeId;
    byte[] address;
    if ((addr instanceof Inet6Address)) {
      byte[] address = addr.getAddress();
      scopeId = ((Inet6Address)addr).getScopeId();
    }
    else {
      scopeId = 0;
      address = NativeInetAddress.ipv4MappedIpv6Address(addr.getAddress());
    }
    int flags = fastOpen ? msgFastopen() : 0;
    int res = sendToAddresses(fd, useIpv6(addr), memoryAddress, length, address, scopeId, port, flags);
    if (res >= 0) {
      return res;
    }
    if ((res == Errors.ERRNO_EINPROGRESS_NEGATIVE) && (fastOpen))
    {


      return 0;
    }
    if (res == Errors.ERROR_ECONNREFUSED_NEGATIVE) {
      throw new PortUnreachableException("sendToAddresses failed");
    }
    return Errors.ioResult("sendToAddresses", res);
  }
  
  public final int sendToAddressesDomainSocket(long memoryAddress, int length, byte[] path) throws IOException {
    int res = sendToAddressesDomainSocket(fd, memoryAddress, length, path);
    if (res >= 0) {
      return res;
    }
    return Errors.ioResult("sendToAddressesDomainSocket", res);
  }
  
  public final DatagramSocketAddress recvFrom(ByteBuffer buf, int pos, int limit) throws IOException {
    return recvFrom(fd, buf, pos, limit);
  }
  
  public final DatagramSocketAddress recvFromAddress(long memoryAddress, int pos, int limit) throws IOException {
    return recvFromAddress(fd, memoryAddress, pos, limit);
  }
  
  public final DomainDatagramSocketAddress recvFromDomainSocket(ByteBuffer buf, int pos, int limit) throws IOException
  {
    return recvFromDomainSocket(fd, buf, pos, limit);
  }
  
  public final DomainDatagramSocketAddress recvFromAddressDomainSocket(long memoryAddress, int pos, int limit) throws IOException
  {
    return recvFromAddressDomainSocket(fd, memoryAddress, pos, limit);
  }
  
  public final int recvFd() throws IOException {
    int res = recvFd(fd);
    if (res > 0) {
      return res;
    }
    if (res == 0) {
      return -1;
    }
    
    if ((res == Errors.ERRNO_EAGAIN_NEGATIVE) || (res == Errors.ERRNO_EWOULDBLOCK_NEGATIVE))
    {
      return 0;
    }
    throw Errors.newIOException("recvFd", res);
  }
  
  public final int sendFd(int fdToSend) throws IOException {
    int res = sendFd(fd, fdToSend);
    if (res >= 0) {
      return res;
    }
    if ((res == Errors.ERRNO_EAGAIN_NEGATIVE) || (res == Errors.ERRNO_EWOULDBLOCK_NEGATIVE))
    {
      return -1;
    }
    throw Errors.newIOException("sendFd", res);
  }
  
  public final boolean connect(SocketAddress socketAddress) throws IOException {
    int res;
    if ((socketAddress instanceof InetSocketAddress)) {
      InetSocketAddress inetSocketAddress = (InetSocketAddress)socketAddress;
      InetAddress inetAddress = inetSocketAddress.getAddress();
      NativeInetAddress address = NativeInetAddress.newInstance(inetAddress);
      res = connect(fd, useIpv6(inetAddress), address, scopeId, inetSocketAddress.getPort()); } else { int res;
      if ((socketAddress instanceof DomainSocketAddress)) {
        DomainSocketAddress unixDomainSocketAddress = (DomainSocketAddress)socketAddress;
        res = connectDomainSocket(fd, unixDomainSocketAddress.path().getBytes(CharsetUtil.UTF_8));
      } else {
        throw new Error("Unexpected SocketAddress implementation " + socketAddress); } }
    int res;
    if (res < 0) {
      return Errors.handleConnectErrno("connect", res);
    }
    return true;
  }
  
  public final boolean finishConnect() throws IOException {
    int res = finishConnect(fd);
    if (res < 0) {
      return Errors.handleConnectErrno("finishConnect", res);
    }
    return true;
  }
  
  public final void disconnect() throws IOException {
    int res = disconnect(fd, ipv6);
    if (res < 0) {
      Errors.handleConnectErrno("disconnect", res);
    }
  }
  
  public final void bind(SocketAddress socketAddress) throws IOException {
    if ((socketAddress instanceof InetSocketAddress)) {
      InetSocketAddress addr = (InetSocketAddress)socketAddress;
      InetAddress inetAddress = addr.getAddress();
      NativeInetAddress address = NativeInetAddress.newInstance(inetAddress);
      int res = bind(fd, useIpv6(inetAddress), address, scopeId, addr.getPort());
      if (res < 0) {
        throw Errors.newIOException("bind", res);
      }
    } else if ((socketAddress instanceof DomainSocketAddress)) {
      DomainSocketAddress addr = (DomainSocketAddress)socketAddress;
      int res = bindDomainSocket(fd, addr.path().getBytes(CharsetUtil.UTF_8));
      if (res < 0) {
        throw Errors.newIOException("bind", res);
      }
    } else {
      throw new Error("Unexpected SocketAddress implementation " + socketAddress);
    }
  }
  
  public final void listen(int backlog) throws IOException {
    int res = listen(fd, backlog);
    if (res < 0) {
      throw Errors.newIOException("listen", res);
    }
  }
  
  public final int accept(byte[] addr) throws IOException {
    int res = accept(fd, addr);
    if (res >= 0) {
      return res;
    }
    if ((res == Errors.ERRNO_EAGAIN_NEGATIVE) || (res == Errors.ERRNO_EWOULDBLOCK_NEGATIVE))
    {
      return -1;
    }
    throw Errors.newIOException("accept", res);
  }
  
  public final InetSocketAddress remoteAddress() {
    byte[] addr = remoteAddress(fd);
    

    return addr == null ? null : NativeInetAddress.address(addr, 0, addr.length);
  }
  
  public final InetSocketAddress localAddress() {
    byte[] addr = localAddress(fd);
    

    return addr == null ? null : NativeInetAddress.address(addr, 0, addr.length);
  }
  
  public final int getReceiveBufferSize() throws IOException {
    return getReceiveBufferSize(fd);
  }
  
  public final int getSendBufferSize() throws IOException {
    return getSendBufferSize(fd);
  }
  
  public final boolean isKeepAlive() throws IOException {
    return isKeepAlive(fd) != 0;
  }
  
  public final boolean isTcpNoDelay() throws IOException {
    return isTcpNoDelay(fd) != 0;
  }
  
  public final boolean isReuseAddress() throws IOException {
    return isReuseAddress(fd) != 0;
  }
  
  public final boolean isReusePort() throws IOException {
    return isReusePort(fd) != 0;
  }
  
  public final boolean isBroadcast() throws IOException {
    return isBroadcast(fd) != 0;
  }
  
  public final int getSoLinger() throws IOException {
    return getSoLinger(fd);
  }
  
  public final int getSoError() throws IOException {
    return getSoError(fd);
  }
  
  public final int getTrafficClass() throws IOException {
    return getTrafficClass(fd, ipv6);
  }
  
  public final void setKeepAlive(boolean keepAlive) throws IOException {
    setKeepAlive(fd, keepAlive ? 1 : 0);
  }
  
  public final void setReceiveBufferSize(int receiveBufferSize) throws IOException {
    setReceiveBufferSize(fd, receiveBufferSize);
  }
  
  public final void setSendBufferSize(int sendBufferSize) throws IOException {
    setSendBufferSize(fd, sendBufferSize);
  }
  
  public final void setTcpNoDelay(boolean tcpNoDelay) throws IOException {
    setTcpNoDelay(fd, tcpNoDelay ? 1 : 0);
  }
  
  public final void setSoLinger(int soLinger) throws IOException {
    setSoLinger(fd, soLinger);
  }
  
  public final void setReuseAddress(boolean reuseAddress) throws IOException {
    setReuseAddress(fd, reuseAddress ? 1 : 0);
  }
  
  public final void setReusePort(boolean reusePort) throws IOException {
    setReusePort(fd, reusePort ? 1 : 0);
  }
  
  public final void setBroadcast(boolean broadcast) throws IOException {
    setBroadcast(fd, broadcast ? 1 : 0);
  }
  
  public final void setTrafficClass(int trafficClass) throws IOException {
    setTrafficClass(fd, ipv6, trafficClass);
  }
  
  public static native boolean isIPv6Preferred();
  
  private static native boolean isIPv6(int paramInt);
  
  public String toString()
  {
    return "Socket{fd=" + fd + '}';
  }
  


  private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
  
  public static Socket newSocketStream() {
    return new Socket(newSocketStream0());
  }
  
  public static Socket newSocketDgram() {
    return new Socket(newSocketDgram0());
  }
  
  public static Socket newSocketDomain() {
    return new Socket(newSocketDomain0());
  }
  
  public static Socket newSocketDomainDgram() {
    return new Socket(newSocketDomainDgram0());
  }
  
  public static void initialize() {
    if (INITIALIZED.compareAndSet(false, true)) {
      initialize(NetUtil.isIpV4StackPreferred());
    }
  }
  
  protected static int newSocketStream0() {
    return newSocketStream0(isIPv6Preferred());
  }
  
  protected static int newSocketStream0(boolean ipv6) {
    int res = newSocketStreamFd(ipv6);
    if (res < 0) {
      throw new ChannelException(Errors.newIOException("newSocketStream", res));
    }
    return res;
  }
  
  protected static int newSocketDgram0() {
    return newSocketDgram0(isIPv6Preferred());
  }
  
  protected static int newSocketDgram0(boolean ipv6) {
    int res = newSocketDgramFd(ipv6);
    if (res < 0) {
      throw new ChannelException(Errors.newIOException("newSocketDgram", res));
    }
    return res;
  }
  
  protected static int newSocketDomain0() {
    int res = newSocketDomainFd();
    if (res < 0) {
      throw new ChannelException(Errors.newIOException("newSocketDomain", res));
    }
    return res;
  }
  
  protected static int newSocketDomainDgram0() {
    int res = newSocketDomainDgramFd();
    if (res < 0) {
      throw new ChannelException(Errors.newIOException("newSocketDomainDgram", res));
    }
    return res;
  }
  
  private static native int shutdown(int paramInt, boolean paramBoolean1, boolean paramBoolean2);
  
  private static native int connect(int paramInt1, boolean paramBoolean, byte[] paramArrayOfByte, int paramInt2, int paramInt3);
  
  private static native int connectDomainSocket(int paramInt, byte[] paramArrayOfByte);
  
  private static native int finishConnect(int paramInt);
  
  private static native int disconnect(int paramInt, boolean paramBoolean);
  
  private static native int bind(int paramInt1, boolean paramBoolean, byte[] paramArrayOfByte, int paramInt2, int paramInt3);
  
  private static native int bindDomainSocket(int paramInt, byte[] paramArrayOfByte);
  
  private static native int listen(int paramInt1, int paramInt2);
  
  private static native int accept(int paramInt, byte[] paramArrayOfByte);
  
  private static native byte[] remoteAddress(int paramInt);
  
  private static native byte[] localAddress(int paramInt);
  
  private static native int sendTo(int paramInt1, boolean paramBoolean, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3, byte[] paramArrayOfByte, int paramInt4, int paramInt5, int paramInt6);
  
  private static native int sendToAddress(int paramInt1, boolean paramBoolean, long paramLong, int paramInt2, int paramInt3, byte[] paramArrayOfByte, int paramInt4, int paramInt5, int paramInt6);
  
  private static native int sendToAddresses(int paramInt1, boolean paramBoolean, long paramLong, int paramInt2, byte[] paramArrayOfByte, int paramInt3, int paramInt4, int paramInt5);
  
  private static native int sendToDomainSocket(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3, byte[] paramArrayOfByte);
  
  private static native int sendToAddressDomainSocket(int paramInt1, long paramLong, int paramInt2, int paramInt3, byte[] paramArrayOfByte);
  
  private static native int sendToAddressesDomainSocket(int paramInt1, long paramLong, int paramInt2, byte[] paramArrayOfByte);
  
  private static native DatagramSocketAddress recvFrom(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3)
    throws IOException;
  
  private static native DatagramSocketAddress recvFromAddress(int paramInt1, long paramLong, int paramInt2, int paramInt3)
    throws IOException;
  
  private static native DomainDatagramSocketAddress recvFromDomainSocket(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3)
    throws IOException;
  
  private static native DomainDatagramSocketAddress recvFromAddressDomainSocket(int paramInt1, long paramLong, int paramInt2, int paramInt3)
    throws IOException;
  
  private static native int recvFd(int paramInt);
  
  private static native int sendFd(int paramInt1, int paramInt2);
  
  private static native int msgFastopen();
  
  private static native int newSocketStreamFd(boolean paramBoolean);
  
  private static native int newSocketDgramFd(boolean paramBoolean);
  
  private static native int newSocketDomainFd();
  
  private static native int newSocketDomainDgramFd();
  
  private static native int isReuseAddress(int paramInt)
    throws IOException;
  
  private static native int isReusePort(int paramInt)
    throws IOException;
  
  private static native int getReceiveBufferSize(int paramInt)
    throws IOException;
  
  private static native int getSendBufferSize(int paramInt)
    throws IOException;
  
  private static native int isKeepAlive(int paramInt)
    throws IOException;
  
  private static native int isTcpNoDelay(int paramInt)
    throws IOException;
  
  private static native int isBroadcast(int paramInt)
    throws IOException;
  
  private static native int getSoLinger(int paramInt)
    throws IOException;
  
  private static native int getSoError(int paramInt)
    throws IOException;
  
  private static native int getTrafficClass(int paramInt, boolean paramBoolean)
    throws IOException;
  
  private static native void setReuseAddress(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setReusePort(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setKeepAlive(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setReceiveBufferSize(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setSendBufferSize(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setTcpNoDelay(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setSoLinger(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setBroadcast(int paramInt1, int paramInt2)
    throws IOException;
  
  private static native void setTrafficClass(int paramInt1, boolean paramBoolean, int paramInt2)
    throws IOException;
  
  private static native void initialize(boolean paramBoolean);
}
