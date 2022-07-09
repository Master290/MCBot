package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;





















public final class Version
{
  private static final String PROP_VERSION = ".version";
  private static final String PROP_BUILD_DATE = ".buildDate";
  private static final String PROP_COMMIT_DATE = ".commitDate";
  private static final String PROP_SHORT_COMMIT_HASH = ".shortCommitHash";
  private static final String PROP_LONG_COMMIT_HASH = ".longCommitHash";
  private static final String PROP_REPO_STATUS = ".repoStatus";
  private final String artifactId;
  private final String artifactVersion;
  private final long buildTimeMillis;
  private final long commitTimeMillis;
  private final String shortCommitHash;
  private final String longCommitHash;
  private final String repositoryStatus;
  
  public static Map<String, Version> identify()
  {
    return identify(null);
  }
  




  public static Map<String, Version> identify(ClassLoader classLoader)
  {
    if (classLoader == null) {
      classLoader = PlatformDependent.getContextClassLoader();
    }
    

    Properties props = new Properties();
    try {
      Enumeration<URL> resources = classLoader.getResources("META-INF/io.netty.versions.properties");
      for (;;) { if (resources.hasMoreElements()) {
          url = (URL)resources.nextElement();
          InputStream in = url.openStream();
          try {
            props.load(in);
            try
            {
              in.close(); } catch (Exception localException) {} } finally { try { in.close();
            }
            catch (Exception localException1) {}
          }
        }
      }
    }
    catch (Exception localException2) {}
    


    Set<String> artifactIds = new HashSet();
    for (URL url = props.keySet().iterator(); url.hasNext();) { o = url.next();
      String k = (String)o;
      
      int dotIndex = k.indexOf('.');
      if (dotIndex > 0)
      {


        String artifactId = k.substring(0, dotIndex);
        

        if ((props.containsKey(artifactId + ".version")) && 
          (props.containsKey(artifactId + ".buildDate")) && 
          (props.containsKey(artifactId + ".commitDate")) && 
          (props.containsKey(artifactId + ".shortCommitHash")) && 
          (props.containsKey(artifactId + ".longCommitHash")) && 
          (props.containsKey(artifactId + ".repoStatus")))
        {


          artifactIds.add(artifactId); }
      } }
    Object o;
    Map<String, Version> versions = new TreeMap();
    for (String artifactId : artifactIds) {
      versions.put(artifactId, new Version(artifactId, props
      


        .getProperty(artifactId + ".version"), 
        parseIso8601(props.getProperty(artifactId + ".buildDate")), 
        parseIso8601(props.getProperty(artifactId + ".commitDate")), props
        .getProperty(artifactId + ".shortCommitHash"), props
        .getProperty(artifactId + ".longCommitHash"), props
        .getProperty(artifactId + ".repoStatus")));
    }
    
    return versions;
  }
  
  private static long parseIso8601(String value) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(value).getTime();
    } catch (ParseException ignored) {}
    return 0L;
  }
  



  public static void main(String[] args)
  {
    for (Version v : identify().values()) {
      System.err.println(v);
    }
  }
  










  private Version(String artifactId, String artifactVersion, long buildTimeMillis, long commitTimeMillis, String shortCommitHash, String longCommitHash, String repositoryStatus)
  {
    this.artifactId = artifactId;
    this.artifactVersion = artifactVersion;
    this.buildTimeMillis = buildTimeMillis;
    this.commitTimeMillis = commitTimeMillis;
    this.shortCommitHash = shortCommitHash;
    this.longCommitHash = longCommitHash;
    this.repositoryStatus = repositoryStatus;
  }
  
  public String artifactId() {
    return artifactId;
  }
  
  public String artifactVersion() {
    return artifactVersion;
  }
  
  public long buildTimeMillis() {
    return buildTimeMillis;
  }
  
  public long commitTimeMillis() {
    return commitTimeMillis;
  }
  
  public String shortCommitHash() {
    return shortCommitHash;
  }
  
  public String longCommitHash() {
    return longCommitHash;
  }
  
  public String repositoryStatus() {
    return repositoryStatus;
  }
  
  public String toString()
  {
    return 
      artifactId + '-' + artifactVersion + '.' + shortCommitHash + ("clean".equals(repositoryStatus) ? "" : new StringBuilder().append(" (repository: ").append(repositoryStatus).append(')').toString());
  }
}
