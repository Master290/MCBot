package io.netty.handler.codec.http2;


public abstract interface Http2Stream
{
  public abstract int id();
  

  public abstract State state();
  

  public abstract Http2Stream open(boolean paramBoolean)
    throws Http2Exception;
  

  public abstract Http2Stream close();
  
  public abstract Http2Stream closeLocalSide();
  
  public abstract Http2Stream closeRemoteSide();
  
  public abstract boolean isResetSent();
  
  public abstract Http2Stream resetSent();
  
  public abstract <V> V setProperty(Http2Connection.PropertyKey paramPropertyKey, V paramV);
  
  public static enum State
  {
    IDLE(false, false), 
    RESERVED_LOCAL(false, false), 
    RESERVED_REMOTE(false, false), 
    OPEN(true, true), 
    HALF_CLOSED_LOCAL(false, true), 
    HALF_CLOSED_REMOTE(true, false), 
    CLOSED(false, false);
    
    private final boolean localSideOpen;
    private final boolean remoteSideOpen;
    
    private State(boolean localSideOpen, boolean remoteSideOpen) {
      this.localSideOpen = localSideOpen;
      this.remoteSideOpen = remoteSideOpen;
    }
    



    public boolean localSideOpen()
    {
      return localSideOpen;
    }
    



    public boolean remoteSideOpen()
    {
      return remoteSideOpen;
    }
  }
  
  public abstract <V> V getProperty(Http2Connection.PropertyKey paramPropertyKey);
  
  public abstract <V> V removeProperty(Http2Connection.PropertyKey paramPropertyKey);
  
  public abstract Http2Stream headersSent(boolean paramBoolean);
  
  public abstract boolean isHeadersSent();
  
  public abstract boolean isTrailersSent();
  
  public abstract Http2Stream headersReceived(boolean paramBoolean);
  
  public abstract boolean isHeadersReceived();
  
  public abstract boolean isTrailersReceived();
  
  public abstract Http2Stream pushPromiseSent();
  
  public abstract boolean isPushPromiseSent();
}
