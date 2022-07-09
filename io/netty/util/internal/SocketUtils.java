package io.netty.util.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Enumeration;























public final class SocketUtils
{
  private static final Enumeration<Object> EMPTY = Collections.enumeration(Collections.emptyList());
  

  private SocketUtils() {}
  
  private static <T> Enumeration<T> empty()
  {
    return EMPTY;
  }
  
  public static void connect(Socket socket, final SocketAddress remoteAddress, final int timeout) throws IOException
  {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public Void run() throws IOException {
          val$socket.connect(remoteAddress, timeout);
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((IOException)e.getCause());
    }
  }
  
  public static void bind(Socket socket, final SocketAddress bindpoint) throws IOException {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public Void run() throws IOException {
          val$socket.bind(bindpoint);
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((IOException)e.getCause());
    }
  }
  
  public static boolean connect(SocketChannel socketChannel, final SocketAddress remoteAddress) throws IOException
  {
    try {
      ((Boolean)AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public Boolean run() throws IOException {
          return Boolean.valueOf(val$socketChannel.connect(remoteAddress));
        }
      })).booleanValue();
    } catch (PrivilegedActionException e) {
      throw ((IOException)e.getCause());
    }
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  public static void bind(SocketChannel socketChannel, final SocketAddress address) throws IOException {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public Void run() throws IOException {
          val$socketChannel.bind(address);
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((IOException)e.getCause());
    }
  }
  
  public static SocketChannel accept(ServerSocketChannel serverSocketChannel) throws IOException {
    try {
      (SocketChannel)AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public SocketChannel run() throws IOException {
          return val$serverSocketChannel.accept();
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((IOException)e.getCause());
    }
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  public static void bind(DatagramChannel networkChannel, final SocketAddress address) throws IOException {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public Void run() throws IOException {
          val$networkChannel.bind(address);
          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((IOException)e.getCause());
    }
  }
  
  public static SocketAddress localSocketAddress(ServerSocket socket) {
    (SocketAddress)AccessController.doPrivileged(new PrivilegedAction()
    {
      public SocketAddress run() {
        return val$socket.getLocalSocketAddress();
      }
    });
  }
  
  public static InetAddress addressByName(String hostname) throws UnknownHostException {
    try {
      (InetAddress)AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public InetAddress run() throws UnknownHostException {
          return InetAddress.getByName(val$hostname);
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((UnknownHostException)e.getCause());
    }
  }
  
  public static InetAddress[] allAddressesByName(String hostname) throws UnknownHostException {
    try {
      (InetAddress[])AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public InetAddress[] run() throws UnknownHostException {
          return InetAddress.getAllByName(val$hostname);
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((UnknownHostException)e.getCause());
    }
  }
  
  public static InetSocketAddress socketAddress(String hostname, final int port) {
    (InetSocketAddress)AccessController.doPrivileged(new PrivilegedAction()
    {
      public InetSocketAddress run() {
        return new InetSocketAddress(val$hostname, port);
      }
    });
  }
  
  public static Enumeration<InetAddress> addressesFromNetworkInterface(NetworkInterface intf)
  {
    Enumeration<InetAddress> addresses = (Enumeration)AccessController.doPrivileged(new PrivilegedAction()
    {
      public Enumeration<InetAddress> run() {
        return val$intf.getInetAddresses();
      }
    });
    


    if (addresses == null) {
      return empty();
    }
    return addresses;
  }
  
  @SuppressJava6Requirement(reason="Usage guarded by java version check")
  public static InetAddress loopbackAddress() {
    (InetAddress)AccessController.doPrivileged(new PrivilegedAction()
    {
      public InetAddress run() {
        if (PlatformDependent.javaVersion() >= 7) {
          return InetAddress.getLoopbackAddress();
        }
        try {
          return InetAddress.getByName(null);
        } catch (UnknownHostException e) {
          throw new IllegalStateException(e);
        }
      }
    });
  }
  
  public static byte[] hardwareAddressFromNetworkInterface(NetworkInterface intf) throws SocketException {
    try {
      (byte[])AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public byte[] run() throws SocketException {
          return val$intf.getHardwareAddress();
        }
      });
    } catch (PrivilegedActionException e) {
      throw ((SocketException)e.getCause());
    }
  }
}
