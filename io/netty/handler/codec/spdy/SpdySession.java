package io.netty.handler.codec.spdy;

import io.netty.channel.ChannelPromise;
import io.netty.util.internal.PlatformDependent;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


















final class SpdySession
{
  private final AtomicInteger activeLocalStreams = new AtomicInteger();
  private final AtomicInteger activeRemoteStreams = new AtomicInteger();
  private final Map<Integer, StreamState> activeStreams = PlatformDependent.newConcurrentHashMap();
  private final StreamComparator streamComparator = new StreamComparator();
  private final AtomicInteger sendWindowSize;
  private final AtomicInteger receiveWindowSize;
  
  SpdySession(int sendWindowSize, int receiveWindowSize) {
    this.sendWindowSize = new AtomicInteger(sendWindowSize);
    this.receiveWindowSize = new AtomicInteger(receiveWindowSize);
  }
  
  int numActiveStreams(boolean remote) {
    if (remote) {
      return activeRemoteStreams.get();
    }
    return activeLocalStreams.get();
  }
  
  boolean noActiveStreams()
  {
    return activeStreams.isEmpty();
  }
  
  boolean isActiveStream(int streamId) {
    return activeStreams.containsKey(Integer.valueOf(streamId));
  }
  
  Map<Integer, StreamState> activeStreams()
  {
    Map<Integer, StreamState> streams = new TreeMap(streamComparator);
    streams.putAll(activeStreams);
    return streams;
  }
  

  void acceptStream(int streamId, byte priority, boolean remoteSideClosed, boolean localSideClosed, int sendWindowSize, int receiveWindowSize, boolean remote)
  {
    if ((!remoteSideClosed) || (!localSideClosed)) {
      StreamState state = (StreamState)activeStreams.put(Integer.valueOf(streamId), new StreamState(priority, remoteSideClosed, localSideClosed, sendWindowSize, receiveWindowSize));
      
      if (state == null) {
        if (remote) {
          activeRemoteStreams.incrementAndGet();
        } else {
          activeLocalStreams.incrementAndGet();
        }
      }
    }
  }
  
  private StreamState removeActiveStream(int streamId, boolean remote) {
    StreamState state = (StreamState)activeStreams.remove(Integer.valueOf(streamId));
    if (state != null) {
      if (remote) {
        activeRemoteStreams.decrementAndGet();
      } else {
        activeLocalStreams.decrementAndGet();
      }
    }
    return state;
  }
  
  void removeStream(int streamId, Throwable cause, boolean remote) {
    StreamState state = removeActiveStream(streamId, remote);
    if (state != null) {
      state.clearPendingWrites(cause);
    }
  }
  
  boolean isRemoteSideClosed(int streamId) {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return (state == null) || (state.isRemoteSideClosed());
  }
  
  void closeRemoteSide(int streamId, boolean remote) {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    if (state != null) {
      state.closeRemoteSide();
      if (state.isLocalSideClosed()) {
        removeActiveStream(streamId, remote);
      }
    }
  }
  
  boolean isLocalSideClosed(int streamId) {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return (state == null) || (state.isLocalSideClosed());
  }
  
  void closeLocalSide(int streamId, boolean remote) {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    if (state != null) {
      state.closeLocalSide();
      if (state.isRemoteSideClosed()) {
        removeActiveStream(streamId, remote);
      }
    }
  }
  



  boolean hasReceivedReply(int streamId)
  {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return (state != null) && (state.hasReceivedReply());
  }
  
  void receivedReply(int streamId) {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    if (state != null) {
      state.receivedReply();
    }
  }
  
  int getSendWindowSize(int streamId) {
    if (streamId == 0) {
      return sendWindowSize.get();
    }
    
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return state != null ? state.getSendWindowSize() : -1;
  }
  
  int updateSendWindowSize(int streamId, int deltaWindowSize) {
    if (streamId == 0) {
      return sendWindowSize.addAndGet(deltaWindowSize);
    }
    
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return state != null ? state.updateSendWindowSize(deltaWindowSize) : -1;
  }
  
  int updateReceiveWindowSize(int streamId, int deltaWindowSize) {
    if (streamId == 0) {
      return receiveWindowSize.addAndGet(deltaWindowSize);
    }
    
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    if (state == null) {
      return -1;
    }
    if (deltaWindowSize > 0) {
      state.setReceiveWindowSizeLowerBound(0);
    }
    return state.updateReceiveWindowSize(deltaWindowSize);
  }
  
  int getReceiveWindowSizeLowerBound(int streamId) {
    if (streamId == 0) {
      return 0;
    }
    
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return state != null ? state.getReceiveWindowSizeLowerBound() : 0;
  }
  
  void updateAllSendWindowSizes(int deltaWindowSize) {
    for (StreamState state : activeStreams.values()) {
      state.updateSendWindowSize(deltaWindowSize);
    }
  }
  
  void updateAllReceiveWindowSizes(int deltaWindowSize) {
    for (StreamState state : activeStreams.values()) {
      state.updateReceiveWindowSize(deltaWindowSize);
      if (deltaWindowSize < 0) {
        state.setReceiveWindowSizeLowerBound(deltaWindowSize);
      }
    }
  }
  
  boolean putPendingWrite(int streamId, PendingWrite pendingWrite) {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return (state != null) && (state.putPendingWrite(pendingWrite));
  }
  
  PendingWrite getPendingWrite(int streamId) {
    if (streamId == 0) {
      for (Map.Entry<Integer, StreamState> e : activeStreams().entrySet()) {
        StreamState state = (StreamState)e.getValue();
        if (state.getSendWindowSize() > 0) {
          PendingWrite pendingWrite = state.getPendingWrite();
          if (pendingWrite != null) {
            return pendingWrite;
          }
        }
      }
      return null;
    }
    
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return state != null ? state.getPendingWrite() : null;
  }
  
  PendingWrite removePendingWrite(int streamId) {
    StreamState state = (StreamState)activeStreams.get(Integer.valueOf(streamId));
    return state != null ? state.removePendingWrite() : null;
  }
  
  private static final class StreamState
  {
    private final byte priority;
    private boolean remoteSideClosed;
    private boolean localSideClosed;
    private boolean receivedReply;
    private final AtomicInteger sendWindowSize;
    private final AtomicInteger receiveWindowSize;
    private int receiveWindowSizeLowerBound;
    private final Queue<SpdySession.PendingWrite> pendingWriteQueue = new ConcurrentLinkedQueue();
    

    StreamState(byte priority, boolean remoteSideClosed, boolean localSideClosed, int sendWindowSize, int receiveWindowSize)
    {
      this.priority = priority;
      this.remoteSideClosed = remoteSideClosed;
      this.localSideClosed = localSideClosed;
      this.sendWindowSize = new AtomicInteger(sendWindowSize);
      this.receiveWindowSize = new AtomicInteger(receiveWindowSize);
    }
    
    byte getPriority() {
      return priority;
    }
    
    boolean isRemoteSideClosed() {
      return remoteSideClosed;
    }
    
    void closeRemoteSide() {
      remoteSideClosed = true;
    }
    
    boolean isLocalSideClosed() {
      return localSideClosed;
    }
    
    void closeLocalSide() {
      localSideClosed = true;
    }
    
    boolean hasReceivedReply() {
      return receivedReply;
    }
    
    void receivedReply() {
      receivedReply = true;
    }
    
    int getSendWindowSize() {
      return sendWindowSize.get();
    }
    
    int updateSendWindowSize(int deltaWindowSize) {
      return sendWindowSize.addAndGet(deltaWindowSize);
    }
    
    int updateReceiveWindowSize(int deltaWindowSize) {
      return receiveWindowSize.addAndGet(deltaWindowSize);
    }
    
    int getReceiveWindowSizeLowerBound() {
      return receiveWindowSizeLowerBound;
    }
    
    void setReceiveWindowSizeLowerBound(int receiveWindowSizeLowerBound) {
      this.receiveWindowSizeLowerBound = receiveWindowSizeLowerBound;
    }
    
    boolean putPendingWrite(SpdySession.PendingWrite msg) {
      return pendingWriteQueue.offer(msg);
    }
    
    SpdySession.PendingWrite getPendingWrite() {
      return (SpdySession.PendingWrite)pendingWriteQueue.peek();
    }
    
    SpdySession.PendingWrite removePendingWrite() {
      return (SpdySession.PendingWrite)pendingWriteQueue.poll();
    }
    
    void clearPendingWrites(Throwable cause) {
      for (;;) {
        SpdySession.PendingWrite pendingWrite = (SpdySession.PendingWrite)pendingWriteQueue.poll();
        if (pendingWrite == null) {
          break;
        }
        pendingWrite.fail(cause);
      }
    }
  }
  
  private final class StreamComparator implements Comparator<Integer>
  {
    StreamComparator() {}
    
    public int compare(Integer id1, Integer id2)
    {
      SpdySession.StreamState state1 = (SpdySession.StreamState)activeStreams.get(id1);
      SpdySession.StreamState state2 = (SpdySession.StreamState)activeStreams.get(id2);
      
      int result = state1.getPriority() - state2.getPriority();
      if (result != 0) {
        return result;
      }
      
      return id1.intValue() - id2.intValue();
    }
  }
  
  public static final class PendingWrite {
    final SpdyDataFrame spdyDataFrame;
    final ChannelPromise promise;
    
    PendingWrite(SpdyDataFrame spdyDataFrame, ChannelPromise promise) {
      this.spdyDataFrame = spdyDataFrame;
      this.promise = promise;
    }
    
    void fail(Throwable cause) {
      spdyDataFrame.release();
      promise.setFailure(cause);
    }
  }
}
