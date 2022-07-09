package io.netty.channel.unix;

import io.netty.util.internal.EmptyArrays;


























public final class PeerCredentials
{
  private final int pid;
  private final int uid;
  private final int[] gids;
  
  PeerCredentials(int p, int u, int... gids)
  {
    pid = p;
    uid = u;
    this.gids = (gids == null ? EmptyArrays.EMPTY_INTS : gids);
  }
  





  public int pid()
  {
    return pid;
  }
  
  public int uid() {
    return uid;
  }
  
  public int[] gids() {
    return (int[])gids.clone();
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder(128);
    sb.append("UserCredentials[pid=").append(pid).append("; uid=").append(uid).append("; gids=[");
    if (gids.length > 0) {
      sb.append(gids[0]);
      for (int i = 1; i < gids.length; i++) {
        sb.append(", ").append(gids[i]);
      }
    }
    sb.append(']');
    return sb.toString();
  }
}
