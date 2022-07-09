package io.netty.channel.epoll;






























public final class EpollTcpInfo
{
  public EpollTcpInfo() {}
  




























  final long[] info = new long[32];
  
  public int state() {
    return (int)info[0];
  }
  
  public int caState() {
    return (int)info[1];
  }
  
  public int retransmits() {
    return (int)info[2];
  }
  
  public int probes() {
    return (int)info[3];
  }
  
  public int backoff() {
    return (int)info[4];
  }
  
  public int options() {
    return (int)info[5];
  }
  
  public int sndWscale() {
    return (int)info[6];
  }
  
  public int rcvWscale() {
    return (int)info[7];
  }
  
  public long rto() {
    return info[8];
  }
  
  public long ato() {
    return info[9];
  }
  
  public long sndMss() {
    return info[10];
  }
  
  public long rcvMss() {
    return info[11];
  }
  
  public long unacked() {
    return info[12];
  }
  
  public long sacked() {
    return info[13];
  }
  
  public long lost() {
    return info[14];
  }
  
  public long retrans() {
    return info[15];
  }
  
  public long fackets() {
    return info[16];
  }
  
  public long lastDataSent() {
    return info[17];
  }
  
  public long lastAckSent() {
    return info[18];
  }
  
  public long lastDataRecv() {
    return info[19];
  }
  
  public long lastAckRecv() {
    return info[20];
  }
  
  public long pmtu() {
    return info[21];
  }
  
  public long rcvSsthresh() {
    return info[22];
  }
  
  public long rtt() {
    return info[23];
  }
  
  public long rttvar() {
    return info[24];
  }
  
  public long sndSsthresh() {
    return info[25];
  }
  
  public long sndCwnd() {
    return info[26];
  }
  
  public long advmss() {
    return info[27];
  }
  
  public long reordering() {
    return info[28];
  }
  
  public long rcvRtt() {
    return info[29];
  }
  
  public long rcvSpace() {
    return info[30];
  }
  
  public long totalRetrans() {
    return info[31];
  }
}
