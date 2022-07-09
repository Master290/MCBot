package xyz.mcbot.minecraftutils;

import java.net.IDN;
import java.util.Hashtable;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

public class ServerAddress
{
  private final String ipAddress;
  private final int serverPort;
  
  private ServerAddress(String ip, int port)
  {
    ipAddress = ip;
    serverPort = port;
  }
  
  public String getIP() {
    try {
      return IDN.toASCII(ipAddress);
    }
    catch (Exception e) {}
    return "";
  }
  
  public int getPort()
  {
    return serverPort;
  }
  
  public static ServerAddress getAddrss(String string) {
    if (string == null) {
      return null;
    }
    String[] astring = string.split(":");
    if (string.startsWith("[")) {
      int i = string.indexOf("]");
      if (i > 0) {
        String s = string.substring(1, i);
        String s2 = string.substring(i + 1).trim();
        if ((s2.startsWith(":")) && (s2.length() > 0)) {
          s2 = s2.substring(1);
          astring = new String[] { s, s2 };
        }
        else {
          astring = new String[] { s };
        }
      }
    }
    if (astring.length > 2) {
      astring = new String[] { string };
    }
    String s3 = astring[0];
    int j = astring.length > 1 ? parseIntWithDefault(astring[1], 25565) : 25565;
    if (j == 25565) {
      String[] astring2 = getServerAddress(s3);
      s3 = astring2[0];
      j = parseIntWithDefault(astring2[1], 25565);
    }
    return new ServerAddress(s3, j);
  }
  
  private static String[] getServerAddress(String p_78863_0_) {
    try {
      Class.forName("com.sun.jndi.dns.DnsContextFactory");
      Hashtable<String, String> hashtable = new Hashtable();
      hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
      hashtable.put("java.naming.provider.url", "dns:");
      hashtable.put("com.sun.jndi.dns.timeout.retries", "1");
      DirContext dircontext = new javax.naming.directory.InitialDirContext(hashtable);
      Attributes attributes = dircontext.getAttributes("_minecraft._tcp." + p_78863_0_, new String[] { "SRV" });
      String[] astring = attributes.get("srv").get().toString().split(" ", 4);
      return new String[] { astring[3], astring[2] };
    }
    catch (Throwable var6) {}
    return tmp134_130;
  }
  
  private static int parseIntWithDefault(String p_78862_0_, int p_78862_1_)
  {
    try {
      return Integer.parseInt(p_78862_0_.trim());
    }
    catch (Exception var3) {}
    return p_78862_1_;
  }
}
