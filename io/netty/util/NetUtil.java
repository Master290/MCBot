package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;




































































































public final class NetUtil
{
  private static final boolean IPV4_PREFERRED = SystemPropertyUtil.getBoolean("java.net.preferIPv4Stack", false);
  




  private static final boolean IPV6_ADDRESSES_PREFERRED = SystemPropertyUtil.getBoolean("java.net.preferIPv6Addresses", false);
  



  private static final InternalLogger logger = InternalLoggerFactory.getInstance(NetUtil.class);
  
  static {
    logger.debug("-Djava.net.preferIPv4Stack: {}", Boolean.valueOf(IPV4_PREFERRED));
    logger.debug("-Djava.net.preferIPv6Addresses: {}", Boolean.valueOf(IPV6_ADDRESSES_PREFERRED));
    

    LOCALHOST4 = NetUtilInitializations.createLocalhost4();
    

    LOCALHOST6 = NetUtilInitializations.createLocalhost6();
    
    NetUtilInitializations.NetworkIfaceAndInetAddress loopback = NetUtilInitializations.determineLoopback(LOCALHOST4, LOCALHOST6);
    LOOPBACK_IF = loopback.iface();
    LOCALHOST = loopback.address();
  }
  


  public static final int SOMAXCONN = ((Integer)AccessController.doPrivileged(new PrivilegedAction()
  {


    public Integer run()
    {

      somaxconn = PlatformDependent.isWindows() ? 200 : 128;
      File file = new File("/proc/sys/net/core/somaxconn");
      BufferedReader in = null;
      

      try
      {
        if (file.exists()) {
          in = new BufferedReader(new FileReader(file));
          somaxconn = Integer.parseInt(in.readLine());
          if (NetUtil.logger.isDebugEnabled()) {
            NetUtil.logger.debug("{}: {}", file, Integer.valueOf(somaxconn));
          }
        }
        else {
          Integer tmp = null;
          if (SystemPropertyUtil.getBoolean("io.netty.net.somaxconn.trySysctl", false)) {
            tmp = NetUtil.sysctlGetInt("kern.ipc.somaxconn");
            if (tmp == null) {
              tmp = NetUtil.sysctlGetInt("kern.ipc.soacceptqueue");
              if (tmp != null) {
                somaxconn = tmp.intValue();
              }
            } else {
              somaxconn = tmp.intValue();
            }
          }
          
          if (tmp == null) {
            NetUtil.logger.debug("Failed to get SOMAXCONN from sysctl and file {}. Default: {}", file, 
              Integer.valueOf(somaxconn));
          }
        }
        













        return Integer.valueOf(somaxconn);
      }
      catch (Exception e)
      {
        if (NetUtil.logger.isDebugEnabled()) {
          NetUtil.logger.debug("Failed to get SOMAXCONN from sysctl and file {}. Default: {}", new Object[] { file, 
            Integer.valueOf(somaxconn), e });
        }
      } finally {
        if (in != null) {
          try {
            in.close();
          }
          catch (Exception localException3) {}
        }
      }
    }
  })).intValue();
  






  public static final Inet4Address LOCALHOST4;
  






  public static final Inet6Address LOCALHOST6;
  






  public static final InetAddress LOCALHOST;
  






  public static final NetworkInterface LOOPBACK_IF;
  






  private static final int IPV6_WORD_COUNT = 8;
  






  private static final int IPV6_MAX_CHAR_COUNT = 39;
  





  private static final int IPV6_BYTE_COUNT = 16;
  





  private static final int IPV6_MAX_CHAR_BETWEEN_SEPARATOR = 4;
  





  private static final int IPV6_MIN_SEPARATORS = 2;
  





  private static final int IPV6_MAX_SEPARATORS = 8;
  





  private static final int IPV4_MAX_CHAR_BETWEEN_SEPARATOR = 3;
  





  private static final int IPV4_SEPARATORS = 3;
  






  public static boolean isIpV4StackPreferred()
  {
    return IPV4_PREFERRED;
  }
  






  public static boolean isIpV6AddressesPreferred()
  {
    return IPV6_ADDRESSES_PREFERRED;
  }
  



  public static byte[] createByteArrayFromIpAddressString(String ipAddressString)
  {
    if (isValidIpV4Address(ipAddressString)) {
      return validIpV4ToBytes(ipAddressString);
    }
    
    if (isValidIpV6Address(ipAddressString)) {
      if (ipAddressString.charAt(0) == '[') {
        ipAddressString = ipAddressString.substring(1, ipAddressString.length() - 1);
      }
      
      int percentPos = ipAddressString.indexOf('%');
      if (percentPos >= 0) {
        ipAddressString = ipAddressString.substring(0, percentPos);
      }
      
      return getIPv6ByName(ipAddressString, true);
    }
    return null;
  }
  
  private static int decimalDigit(String str, int pos) {
    return str.charAt(pos) - '0';
  }
  
  private static byte ipv4WordToByte(String ip, int from, int toExclusive) {
    int ret = decimalDigit(ip, from);
    from++;
    if (from == toExclusive) {
      return (byte)ret;
    }
    ret = ret * 10 + decimalDigit(ip, from);
    from++;
    if (from == toExclusive) {
      return (byte)ret;
    }
    return (byte)(ret * 10 + decimalDigit(ip, from));
  }
  

  static byte[] validIpV4ToBytes(String ip)
  {
    byte[] tmp3_1 = new byte[4]; int 
      tmp14_11 = ip.indexOf('.', 1);int i = tmp14_11;tmp3_1[0] = ipv4WordToByte(ip, 0, tmp14_11); byte[] tmp20_3 = tmp3_1; int 
      tmp35_32 = ip.indexOf('.', i + 2);i = tmp35_32;tmp20_3[1] = ipv4WordToByte(ip, i + 1, tmp35_32); byte[] tmp41_20 = tmp20_3; int 
      tmp56_53 = ip.indexOf('.', i + 2);i = tmp56_53;tmp41_20[2] = ipv4WordToByte(ip, i + 1, tmp56_53); byte[] tmp62_41 = tmp41_20;tmp62_41[3] = 
      ipv4WordToByte(ip, i + 1, ip.length());return tmp62_41;
  }
  




  public static int ipv4AddressToInt(Inet4Address ipAddress)
  {
    byte[] octets = ipAddress.getAddress();
    
    return (octets[0] & 0xFF) << 24 | (octets[1] & 0xFF) << 16 | (octets[2] & 0xFF) << 8 | octets[3] & 0xFF;
  }
  





  public static String intToIpAddress(int i)
  {
    StringBuilder buf = new StringBuilder(15);
    buf.append(i >> 24 & 0xFF);
    buf.append('.');
    buf.append(i >> 16 & 0xFF);
    buf.append('.');
    buf.append(i >> 8 & 0xFF);
    buf.append('.');
    buf.append(i & 0xFF);
    return buf.toString();
  }
  





  public static String bytesToIpAddress(byte[] bytes)
  {
    return bytesToIpAddress(bytes, 0, bytes.length);
  }
  





  public static String bytesToIpAddress(byte[] bytes, int offset, int length)
  {
    switch (length) {
    case 4: 
      return 
      





        15 + (bytes[offset] & 0xFF) + '.' + (bytes[(offset + 1)] & 0xFF) + '.' + (bytes[(offset + 2)] & 0xFF) + '.' + (bytes[(offset + 3)] & 0xFF);
    
    case 16: 
      return toAddressString(bytes, offset, false);
    }
    throw new IllegalArgumentException("length: " + length + " (expected: 4 or 16)");
  }
  
  public static boolean isValidIpV6Address(String ip)
  {
    return isValidIpV6Address(ip);
  }
  
  public static boolean isValidIpV6Address(CharSequence ip) {
    int end = ip.length();
    if (end < 2) {
      return false;
    }
    


    char c = ip.charAt(0);
    int start; if (c == '[') {
      end--;
      if (ip.charAt(end) != ']')
      {
        return false;
      }
      int start = 1;
      c = ip.charAt(1);
    } else {
      start = 0;
    }
    
    int colons;
    int compressBegin;
    if (c == ':')
    {
      if (ip.charAt(start + 1) != ':') {
        return false;
      }
      int colons = 2;
      int compressBegin = start;
      start += 2;
    } else {
      colons = 0;
      compressBegin = -1;
    }
    
    int wordLen = 0;
    
    for (int i = start; i < end; i++) {
      c = ip.charAt(i);
      if (isValidHexChar(c)) {
        if (wordLen < 4) {
          wordLen++;
        }
        else {
          return false;
        }
      } else {
        switch (c) {
        case ':': 
          if (colons > 7) {
            return false;
          }
          if (ip.charAt(i - 1) == ':') {
            if (compressBegin >= 0) {
              return false;
            }
            compressBegin = i - 1;
          } else {
            wordLen = 0;
          }
          colons++;
          break;
        


        case '.': 
          if (((compressBegin < 0) && (colons != 6)) || ((colons == 7) && (compressBegin >= start)) || (colons > 7))
          {


            return false;
          }
          



          int ipv4Start = i - wordLen;
          int j = ipv4Start - 2;
          if (isValidIPv4MappedChar(ip.charAt(j))) {
            if ((!isValidIPv4MappedChar(ip.charAt(j - 1))) || 
              (!isValidIPv4MappedChar(ip.charAt(j - 2))) || 
              (!isValidIPv4MappedChar(ip.charAt(j - 3)))) {
              return false;
            }
            j -= 5;
          }
          for (; 
              j >= start; j--) {
            char tmpChar = ip.charAt(j);
            if ((tmpChar != '0') && (tmpChar != ':')) {
              return false;
            }
          }
          

          int ipv4End = AsciiString.indexOf(ip, '%', ipv4Start + 7);
          if (ipv4End < 0) {
            ipv4End = end;
          }
          return isValidIpV4Address(ip, ipv4Start, ipv4End);
        
        case '%': 
          end = i;
          break;
        default: 
          return false;
        }
        
      }
    }
    if (compressBegin < 0) {
      return (colons == 7) && (wordLen > 0);
    }
    
    return (compressBegin + 2 == end) || ((wordLen > 0) && ((colons < 8) || (compressBegin <= start)));
  }
  

  private static boolean isValidIpV4Word(CharSequence word, int from, int toExclusive)
  {
    int len = toExclusive - from;
    char c0;
    if ((len < 1) || (len > 3) || ((c0 = word.charAt(from)) < '0'))
      return false;
    char c0;
    if (len == 3) { char c1;
      char c2; return ((c1 = word.charAt(from + 1)) >= '0') && 
        ((c2 = word.charAt(from + 2)) >= '0') && (((c0 <= '1') && (c1 <= '9') && (c2 <= '9')) || ((c0 == '2') && (c1 <= '5') && ((c2 <= '5') || ((c1 < '5') && (c2 <= '9')))));
    }
    

    return (c0 <= '9') && ((len == 1) || (isValidNumericChar(word.charAt(from + 1))));
  }
  
  private static boolean isValidHexChar(char c) {
    return ((c >= '0') && (c <= '9')) || ((c >= 'A') && (c <= 'F')) || ((c >= 'a') && (c <= 'f'));
  }
  
  private static boolean isValidNumericChar(char c) {
    return (c >= '0') && (c <= '9');
  }
  
  private static boolean isValidIPv4MappedChar(char c) {
    return (c == 'f') || (c == 'F');
  }
  


  private static boolean isValidIPv4MappedSeparators(byte b0, byte b1, boolean mustBeZero)
  {
    return (b0 == b1) && ((b0 == 0) || ((!mustBeZero) && (b1 == -1)));
  }
  
  private static boolean isValidIPv4Mapped(byte[] bytes, int currentIndex, int compressBegin, int compressLength) {
    boolean mustBeZero = compressBegin + compressLength >= 14;
    return (currentIndex <= 12) && (currentIndex >= 2) && ((!mustBeZero) || (compressBegin < 12)) && 
      (isValidIPv4MappedSeparators(bytes[(currentIndex - 1)], bytes[(currentIndex - 2)], mustBeZero)) && 
      (PlatformDependent.isZero(bytes, 0, currentIndex - 3));
  }
  





  public static boolean isValidIpV4Address(CharSequence ip)
  {
    return isValidIpV4Address(ip, 0, ip.length());
  }
  





  public static boolean isValidIpV4Address(String ip)
  {
    return isValidIpV4Address(ip, 0, ip.length());
  }
  
  private static boolean isValidIpV4Address(CharSequence ip, int from, int toExcluded) {
    return (ip instanceof AsciiString) ? 
      isValidIpV4Address((AsciiString)ip, from, toExcluded) : (ip instanceof String) ? isValidIpV4Address((String)ip, from, toExcluded) : 
      isValidIpV4Address0(ip, from, toExcluded);
  }
  
  private static boolean isValidIpV4Address(String ip, int from, int toExcluded)
  {
    int len = toExcluded - from;
    int i;
    return (len <= 15) && (len >= 7) && 
      ((i = ip.indexOf('.', from + 1)) > 0) && (isValidIpV4Word(ip, from, i)) && 
      ((i = ip.indexOf('.', from = i + 2)) > 0) && (isValidIpV4Word(ip, from - 1, i)) && 
      ((i = ip.indexOf('.', from = i + 2)) > 0) && (isValidIpV4Word(ip, from - 1, i)) && 
      (isValidIpV4Word(ip, i + 1, toExcluded));
  }
  
  private static boolean isValidIpV4Address(AsciiString ip, int from, int toExcluded)
  {
    int len = toExcluded - from;
    int i;
    return (len <= 15) && (len >= 7) && 
      ((i = ip.indexOf('.', from + 1)) > 0) && (isValidIpV4Word(ip, from, i)) && 
      ((i = ip.indexOf('.', from = i + 2)) > 0) && (isValidIpV4Word(ip, from - 1, i)) && 
      ((i = ip.indexOf('.', from = i + 2)) > 0) && (isValidIpV4Word(ip, from - 1, i)) && 
      (isValidIpV4Word(ip, i + 1, toExcluded));
  }
  
  private static boolean isValidIpV4Address0(CharSequence ip, int from, int toExcluded)
  {
    int len = toExcluded - from;
    int i;
    return (len <= 15) && (len >= 7) && 
      ((i = AsciiString.indexOf(ip, '.', from + 1)) > 0) && (isValidIpV4Word(ip, from, i)) && 
      ((i = AsciiString.indexOf(ip, '.', from = i + 2)) > 0) && (isValidIpV4Word(ip, from - 1, i)) && 
      ((i = AsciiString.indexOf(ip, '.', from = i + 2)) > 0) && (isValidIpV4Word(ip, from - 1, i)) && 
      (isValidIpV4Word(ip, i + 1, toExcluded));
  }
  






  public static Inet6Address getByName(CharSequence ip)
  {
    return getByName(ip, true);
  }
  













  public static Inet6Address getByName(CharSequence ip, boolean ipv4Mapped)
  {
    byte[] bytes = getIPv6ByName(ip, ipv4Mapped);
    if (bytes == null) {
      return null;
    }
    try {
      return Inet6Address.getByAddress(null, bytes, -1);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }
  













  private static byte[] getIPv6ByName(CharSequence ip, boolean ipv4Mapped)
  {
    byte[] bytes = new byte[16];
    int ipLength = ip.length();
    int compressBegin = 0;
    int compressLength = 0;
    int currentIndex = 0;
    int value = 0;
    int begin = -1;
    int i = 0;
    int ipv6Separators = 0;
    int ipv4Separators = 0;
    
    boolean needsShift = false;
    for (; i < ipLength; i++) {
      char c = ip.charAt(i);
      switch (c) {
      case ':': 
        ipv6Separators++;
        if ((i - begin > 4) || (ipv4Separators > 0) || (ipv6Separators > 8) || (currentIndex + 1 >= bytes.length))
        {

          return null;
        }
        value <<= 4 - (i - begin) << 2;
        
        if (compressLength > 0) {
          compressLength -= 2;
        }
        



        bytes[(currentIndex++)] = ((byte)((value & 0xF) << 4 | value >> 4 & 0xF));
        bytes[(currentIndex++)] = ((byte)((value >> 8 & 0xF) << 4 | value >> 12 & 0xF));
        int tmp = i + 1;
        if ((tmp < ipLength) && (ip.charAt(tmp) == ':')) {
          tmp++;
          if ((compressBegin != 0) || ((tmp < ipLength) && (ip.charAt(tmp) == ':'))) {
            return null;
          }
          ipv6Separators++;
          needsShift = (ipv6Separators == 2) && (value == 0);
          compressBegin = currentIndex;
          compressLength = bytes.length - compressBegin - 2;
          i++;
        }
        value = 0;
        begin = -1;
        break;
      case '.': 
        ipv4Separators++;
        int tmp = i - begin;
        if ((tmp > 3) || (begin < 0) || (ipv4Separators > 3) || ((ipv6Separators > 0) && (currentIndex + compressLength < 12)) || (i + 1 >= ipLength) || (currentIndex >= bytes.length) || ((ipv4Separators == 1) && ((!ipv4Mapped) || ((currentIndex != 0) && 
        






          (!isValidIPv4Mapped(bytes, currentIndex, compressBegin, compressLength))) || ((tmp == 3) && (
          
          (!isValidNumericChar(ip.charAt(i - 1))) || 
          (!isValidNumericChar(ip.charAt(i - 2))) || 
          (!isValidNumericChar(ip.charAt(i - 3))))) || ((tmp == 2) && (
          (!isValidNumericChar(ip.charAt(i - 1))) || 
          (!isValidNumericChar(ip.charAt(i - 2))))) || ((tmp == 1) && 
          (!isValidNumericChar(ip.charAt(i - 1))))))) {
          return null;
        }
        value <<= 3 - tmp << 2;
        



        begin = (value & 0xF) * 100 + (value >> 4 & 0xF) * 10 + (value >> 8 & 0xF);
        if ((begin < 0) || (begin > 255)) {
          return null;
        }
        bytes[(currentIndex++)] = ((byte)begin);
        value = 0;
        begin = -1;
        break;
      default: 
        if ((!isValidHexChar(c)) || ((ipv4Separators > 0) && (!isValidNumericChar(c)))) {
          return null;
        }
        if (begin < 0) {
          begin = i;
        } else if (i - begin > 4) {
          return null;
        }
        



        value += (StringUtil.decodeHexNibble(c) << (i - begin << 2));
      }
      
    }
    
    boolean isCompressed = compressBegin > 0;
    
    if (ipv4Separators > 0) {
      if (((begin > 0) && (i - begin > 3)) || (ipv4Separators != 3) || (currentIndex >= bytes.length))
      {

        return null;
      }
      if (ipv6Separators == 0) {
        compressLength = 12;
      } else { if (ipv6Separators >= 2) if ((isCompressed) || (ipv6Separators != 6) || 
            (ip.charAt(0) == ':')) { if ((isCompressed) && (ipv6Separators < 8))
            {
              if ((ip.charAt(0) == ':') && (compressBegin > 2)) {} }
          } else { compressLength -= 2;
            break label763; }
        return null; }
      label763:
      value <<= 3 - (i - begin) << 2;
      



      begin = (value & 0xF) * 100 + (value >> 4 & 0xF) * 10 + (value >> 8 & 0xF);
      if ((begin < 0) || (begin > 255)) {
        return null;
      }
      bytes[(currentIndex++)] = ((byte)begin);
    } else {
      int tmp = ipLength - 1;
      if (((begin > 0) && (i - begin > 4)) || (ipv6Separators < 2) || ((!isCompressed) && ((ipv6Separators + 1 != 8) || 
      

        (ip.charAt(0) == ':') || (ip.charAt(tmp) == ':'))) || ((isCompressed) && ((ipv6Separators > 8) || ((ipv6Separators == 8) && (((compressBegin <= 2) && 
        

        (ip.charAt(0) != ':')) || ((compressBegin >= 14) && 
        (ip.charAt(tmp) != ':')))))) || (currentIndex + 1 >= bytes.length) || ((begin < 0) && 
        
        (ip.charAt(tmp - 1) != ':')) || ((compressBegin > 2) && 
        (ip.charAt(0) == ':'))) {
        return null;
      }
      if ((begin >= 0) && (i - begin <= 4)) {
        value <<= 4 - (i - begin) << 2;
      }
      


      bytes[(currentIndex++)] = ((byte)((value & 0xF) << 4 | value >> 4 & 0xF));
      bytes[(currentIndex++)] = ((byte)((value >> 8 & 0xF) << 4 | value >> 12 & 0xF));
    }
    
    i = currentIndex + compressLength;
    if ((needsShift) || (i >= bytes.length))
    {
      if (i >= bytes.length) {
        compressBegin++;
      }
      for (i = currentIndex; i < bytes.length;) {
        for (begin = bytes.length - 1; begin >= compressBegin; begin--) {
          bytes[begin] = bytes[(begin - 1)];
        }
        bytes[begin] = 0;
        compressBegin++;i++; continue;
        



        for (i = 0; i < compressLength; i++) {
          begin = i + compressBegin;
          currentIndex = begin + compressLength;
          if (currentIndex >= bytes.length) break;
          bytes[currentIndex] = bytes[begin];
          bytes[begin] = 0;
        }
      }
    }
    


    if (ipv4Separators > 0)
    {


      byte tmp1228_1227 = -1;bytes[11] = tmp1228_1227;bytes[10] = tmp1228_1227;
    }
    
    return bytes;
  }
  






  public static String toSocketAddressString(InetSocketAddress addr)
  {
    String port = String.valueOf(addr.getPort());
    StringBuilder sb;
    StringBuilder sb;
    if (addr.isUnresolved()) {
      String hostname = getHostname(addr);
      sb = newSocketAddressStringBuilder(hostname, port, !isValidIpV6Address(hostname));
    } else {
      InetAddress address = addr.getAddress();
      String hostString = toAddressString(address);
      sb = newSocketAddressStringBuilder(hostString, port, address instanceof Inet4Address);
    }
    return ':' + port;
  }
  


  public static String toSocketAddressString(String host, int port)
  {
    String portStr = String.valueOf(port);
    return ':' + 
      portStr;
  }
  
  private static StringBuilder newSocketAddressStringBuilder(String host, String port, boolean ipv4) {
    int hostLen = host.length();
    if (ipv4)
    {
      return new StringBuilder(hostLen + 1 + port.length()).append(host);
    }
    
    StringBuilder stringBuilder = new StringBuilder(hostLen + 3 + port.length());
    if ((hostLen > 1) && (host.charAt(0) == '[') && (host.charAt(hostLen - 1) == ']')) {
      return stringBuilder.append(host);
    }
    return stringBuilder.append('[').append(host).append(']');
  }
  











  public static String toAddressString(InetAddress ip)
  {
    return toAddressString(ip, false);
  }
  























  public static String toAddressString(InetAddress ip, boolean ipv4Mapped)
  {
    if ((ip instanceof Inet4Address)) {
      return ip.getHostAddress();
    }
    if (!(ip instanceof Inet6Address)) {
      throw new IllegalArgumentException("Unhandled type: " + ip);
    }
    
    return toAddressString(ip.getAddress(), 0, ipv4Mapped);
  }
  
  private static String toAddressString(byte[] bytes, int offset, boolean ipv4Mapped) {
    int[] words = new int[8];
    
    int end = offset + words.length;
    for (int i = offset; i < end; i++) {
      words[i] = ((bytes[(i << 1)] & 0xFF) << 8 | bytes[((i << 1) + 1)] & 0xFF);
    }
    

    int currentStart = -1;
    
    int shortestStart = -1;
    int shortestLength = 0;
    for (i = 0; i < words.length; i++) {
      if (words[i] == 0) {
        if (currentStart < 0) {
          currentStart = i;
        }
      } else if (currentStart >= 0) {
        int currentLength = i - currentStart;
        if (currentLength > shortestLength) {
          shortestStart = currentStart;
          shortestLength = currentLength;
        }
        currentStart = -1;
      }
    }
    
    if (currentStart >= 0) {
      int currentLength = i - currentStart;
      if (currentLength > shortestLength) {
        shortestStart = currentStart;
        shortestLength = currentLength;
      }
    }
    
    if (shortestLength == 1) {
      shortestLength = 0;
      shortestStart = -1;
    }
    

    int shortestEnd = shortestStart + shortestLength;
    StringBuilder b = new StringBuilder(39);
    if (shortestEnd < 0) {
      b.append(Integer.toHexString(words[0]));
      for (i = 1; i < words.length; i++) {
        b.append(':');
        b.append(Integer.toHexString(words[i]));
      }
    }
    boolean isIpv4Mapped;
    boolean isIpv4Mapped;
    if (inRangeEndExclusive(0, shortestStart, shortestEnd)) {
      b.append("::");
      isIpv4Mapped = (ipv4Mapped) && (shortestEnd == 5) && (words[5] == 65535);
    } else {
      b.append(Integer.toHexString(words[0]));
      isIpv4Mapped = false;
    }
    for (i = 1; i < words.length; i++) {
      if (!inRangeEndExclusive(i, shortestStart, shortestEnd)) {
        if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd))
        {
          if ((!isIpv4Mapped) || (i == 6)) {
            b.append(':');
          } else {
            b.append('.');
          }
        }
        if ((isIpv4Mapped) && (i > 5)) {
          b.append(words[i] >> 8);
          b.append('.');
          b.append(words[i] & 0xFF);
        } else {
          b.append(Integer.toHexString(words[i]));
        }
      } else if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd))
      {
        b.append("::");
      }
    }
    

    return b.toString();
  }
  





  public static String getHostname(InetSocketAddress addr)
  {
    return PlatformDependent.javaVersion() >= 7 ? addr.getHostString() : addr.getHostName();
  }
  










  private static boolean inRangeEndExclusive(int value, int start, int end)
  {
    return (value >= start) && (value < end);
  }
  
  /* Error */
  private static Integer sysctlGetInt(String sysctlKey)
    throws java.io.IOException
  {
    // Byte code:
    //   0: new 47	java/lang/ProcessBuilder
    //   3: dup
    //   4: iconst_2
    //   5: anewarray 49	java/lang/String
    //   8: dup
    //   9: iconst_0
    //   10: ldc 51
    //   12: aastore
    //   13: dup
    //   14: iconst_1
    //   15: aload_0
    //   16: aastore
    //   17: invokespecial 55	java/lang/ProcessBuilder:<init>	([Ljava/lang/String;)V
    //   20: invokevirtual 59	java/lang/ProcessBuilder:start	()Ljava/lang/Process;
    //   23: astore_1
    //   24: aload_1
    //   25: invokevirtual 65	java/lang/Process:getInputStream	()Ljava/io/InputStream;
    //   28: astore_2
    //   29: new 67	java/io/InputStreamReader
    //   32: dup
    //   33: aload_2
    //   34: invokespecial 70	java/io/InputStreamReader:<init>	(Ljava/io/InputStream;)V
    //   37: astore_3
    //   38: new 72	java/io/BufferedReader
    //   41: dup
    //   42: aload_3
    //   43: invokespecial 75	java/io/BufferedReader:<init>	(Ljava/io/Reader;)V
    //   46: astore 4
    //   48: aload 4
    //   50: invokevirtual 79	java/io/BufferedReader:readLine	()Ljava/lang/String;
    //   53: astore 5
    //   55: aload 5
    //   57: ifnull +79 -> 136
    //   60: aload 5
    //   62: aload_0
    //   63: invokevirtual 83	java/lang/String:startsWith	(Ljava/lang/String;)Z
    //   66: ifeq +70 -> 136
    //   69: aload 5
    //   71: invokevirtual 87	java/lang/String:length	()I
    //   74: iconst_1
    //   75: isub
    //   76: istore 6
    //   78: iload 6
    //   80: aload_0
    //   81: invokevirtual 87	java/lang/String:length	()I
    //   84: if_icmple +52 -> 136
    //   87: aload 5
    //   89: iload 6
    //   91: invokevirtual 93	java/lang/String:charAt	(I)C
    //   94: invokestatic 99	java/lang/Character:isDigit	(C)Z
    //   97: ifne +33 -> 130
    //   100: aload 5
    //   102: iload 6
    //   104: iconst_1
    //   105: iadd
    //   106: invokevirtual 103	java/lang/String:substring	(I)Ljava/lang/String;
    //   109: invokestatic 108	java/lang/Integer:valueOf	(Ljava/lang/String;)Ljava/lang/Integer;
    //   112: astore 7
    //   114: aload 4
    //   116: invokevirtual 112	java/io/BufferedReader:close	()V
    //   119: aload_1
    //   120: ifnull +7 -> 127
    //   123: aload_1
    //   124: invokevirtual 115	java/lang/Process:destroy	()V
    //   127: aload 7
    //   129: areturn
    //   130: iinc 6 -1
    //   133: goto -55 -> 78
    //   136: aconst_null
    //   137: astore 6
    //   139: aload 4
    //   141: invokevirtual 112	java/io/BufferedReader:close	()V
    //   144: aload_1
    //   145: ifnull +7 -> 152
    //   148: aload_1
    //   149: invokevirtual 115	java/lang/Process:destroy	()V
    //   152: aload 6
    //   154: areturn
    //   155: astore 8
    //   157: aload 4
    //   159: invokevirtual 112	java/io/BufferedReader:close	()V
    //   162: aload 8
    //   164: athrow
    //   165: astore 9
    //   167: aload_1
    //   168: ifnull +7 -> 175
    //   171: aload_1
    //   172: invokevirtual 115	java/lang/Process:destroy	()V
    //   175: aload 9
    //   177: athrow
    // Line number table:
    //   Java source line #217	-> byte code offset #0
    //   Java source line #220	-> byte code offset #24
    //   Java source line #221	-> byte code offset #29
    //   Java source line #222	-> byte code offset #38
    //   Java source line #224	-> byte code offset #48
    //   Java source line #225	-> byte code offset #55
    //   Java source line #226	-> byte code offset #69
    //   Java source line #227	-> byte code offset #87
    //   Java source line #228	-> byte code offset #100
    //   Java source line #234	-> byte code offset #114
    //   Java source line #237	-> byte code offset #119
    //   Java source line #238	-> byte code offset #123
    //   Java source line #228	-> byte code offset #127
    //   Java source line #226	-> byte code offset #130
    //   Java source line #232	-> byte code offset #136
    //   Java source line #234	-> byte code offset #139
    //   Java source line #237	-> byte code offset #144
    //   Java source line #238	-> byte code offset #148
    //   Java source line #232	-> byte code offset #152
    //   Java source line #234	-> byte code offset #155
    //   Java source line #235	-> byte code offset #162
    //   Java source line #237	-> byte code offset #165
    //   Java source line #238	-> byte code offset #171
    //   Java source line #240	-> byte code offset #175
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	178	0	sysctlKey	String
    //   23	149	1	process	Process
    //   28	6	2	is	java.io.InputStream
    //   37	6	3	isr	java.io.InputStreamReader
    //   46	112	4	br	BufferedReader
    //   53	48	5	line	String
    //   76	77	6	i	int
    //   112	16	7	localInteger	Integer
    //   155	8	8	localObject1	Object
    //   165	11	9	localObject2	Object
    // Exception table:
    //   from	to	target	type
    //   48	114	155	finally
    //   130	139	155	finally
    //   155	157	155	finally
    //   24	119	165	finally
    //   130	144	165	finally
    //   155	167	165	finally
  }
  
  private NetUtil() {}
}
