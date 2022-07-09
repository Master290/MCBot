package io.netty.handler.codec.http.cookie;




public final class CookieHeaderNames
{
  public static final String PATH = "Path";
  


  public static final String EXPIRES = "Expires";
  


  public static final String MAX_AGE = "Max-Age";
  

  public static final String DOMAIN = "Domain";
  

  public static final String SECURE = "Secure";
  

  public static final String HTTPONLY = "HTTPOnly";
  

  public static final String SAMESITE = "SameSite";
  


  private CookieHeaderNames() {}
  


  public static enum SameSite
  {
    Lax, 
    Strict, 
    None;
    


    private SameSite() {}
    

    static SameSite of(String name)
    {
      if (name != null) {
        for (SameSite each : (SameSite[])SameSite.class.getEnumConstants()) {
          if (each.name().equalsIgnoreCase(name)) {
            return each;
          }
        }
      }
      return null;
    }
  }
}
