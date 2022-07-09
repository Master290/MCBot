package io.netty.handler.codec.http2;

import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Deque;






























public final class UniformStreamByteDistributor
  implements StreamByteDistributor
{
  private final Http2Connection.PropertyKey stateKey;
  private final Deque<State> queue = new ArrayDeque(4);
  




  private int minAllocationChunk = 1024;
  private long totalStreamableBytes;
  
  public UniformStreamByteDistributor(Http2Connection connection)
  {
    stateKey = connection.newKey();
    Http2Stream connectionStream = connection.connectionStream();
    connectionStream.setProperty(stateKey, new State(connectionStream));
    

    connection.addListener(new Http2ConnectionAdapter()
    {
      public void onStreamAdded(Http2Stream stream) {
        stream.setProperty(stateKey, new UniformStreamByteDistributor.State(UniformStreamByteDistributor.this, stream));
      }
      
      public void onStreamClosed(Http2Stream stream)
      {
        UniformStreamByteDistributor.this.state(stream).close();
      }
    });
  }
  





  public void minAllocationChunk(int minAllocationChunk)
  {
    ObjectUtil.checkPositive(minAllocationChunk, "minAllocationChunk");
    this.minAllocationChunk = minAllocationChunk;
  }
  
  public void updateStreamableBytes(StreamByteDistributor.StreamState streamState)
  {
    state(streamState.stream()).updateStreamableBytes(Http2CodecUtil.streamableBytes(streamState), streamState
      .hasFrame(), streamState
      .windowSize());
  }
  

  public void updateDependencyTree(int childStreamId, int parentStreamId, short weight, boolean exclusive) {}
  

  public boolean distribute(int maxBytes, StreamByteDistributor.Writer writer)
    throws Http2Exception
  {
    int size = queue.size();
    if (size == 0) {
      return totalStreamableBytes > 0L;
    }
    
    int chunkSize = Math.max(minAllocationChunk, maxBytes / size);
    
    State state = (State)queue.pollFirst();
    do {
      enqueued = false;
      if (!windowNegative)
      {

        if ((maxBytes == 0) && (streamableBytes > 0))
        {


          queue.addFirst(state);
          enqueued = true;
          break;
        }
        

        int chunk = Math.min(chunkSize, Math.min(maxBytes, streamableBytes));
        maxBytes -= chunk;
        

        state.write(chunk, writer);
      } } while ((state = (State)queue.pollFirst()) != null);
    
    return totalStreamableBytes > 0L;
  }
  
  private State state(Http2Stream stream) {
    return (State)((Http2Stream)ObjectUtil.checkNotNull(stream, "stream")).getProperty(stateKey);
  }
  

  private final class State
  {
    final Http2Stream stream;
    int streamableBytes;
    boolean windowNegative;
    boolean enqueued;
    boolean writing;
    
    State(Http2Stream stream)
    {
      this.stream = stream;
    }
    
    void updateStreamableBytes(int newStreamableBytes, boolean hasFrame, int windowSize) {
      assert ((hasFrame) || (newStreamableBytes == 0)) : ("hasFrame: " + hasFrame + " newStreamableBytes: " + newStreamableBytes);
      

      int delta = newStreamableBytes - streamableBytes;
      if (delta != 0) {
        streamableBytes = newStreamableBytes;
        totalStreamableBytes = (totalStreamableBytes + delta);
      }
      






      windowNegative = (windowSize < 0);
      if ((hasFrame) && ((windowSize > 0) || ((windowSize == 0) && (!writing)))) {
        addToQueue();
      }
    }
    


    void write(int numBytes, StreamByteDistributor.Writer writer)
      throws Http2Exception
    {
      writing = true;
      try
      {
        writer.write(stream, numBytes);
      } catch (Throwable t) {
        throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, t, "byte distribution write error", new Object[0]);
      } finally {
        writing = false;
      }
    }
    
    void addToQueue() {
      if (!enqueued) {
        enqueued = true;
        queue.addLast(this);
      }
    }
    
    void removeFromQueue() {
      if (enqueued) {
        enqueued = false;
        queue.remove(this);
      }
    }
    
    void close()
    {
      removeFromQueue();
      

      updateStreamableBytes(0, false, 0);
    }
  }
}
