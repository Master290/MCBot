package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;











































public class StreamBufferingEncoder
  extends DecoratingHttp2ConnectionEncoder
{
  public static final class Http2ChannelClosedException
    extends Http2Exception
  {
    private static final long serialVersionUID = 4768543442094476971L;
    
    public Http2ChannelClosedException()
    {
      super("Connection closed");
    }
  }
  
  private static final class GoAwayDetail {
    private final int lastStreamId;
    private final long errorCode;
    private final byte[] debugData;
    
    GoAwayDetail(int lastStreamId, long errorCode, byte[] debugData) {
      this.lastStreamId = lastStreamId;
      this.errorCode = errorCode;
      this.debugData = ((byte[])debugData.clone());
    }
  }
  

  public static final class Http2GoAwayException
    extends Http2Exception
  {
    private static final long serialVersionUID = 1326785622777291198L;
    private final StreamBufferingEncoder.GoAwayDetail goAwayDetail;
    
    public Http2GoAwayException(int lastStreamId, long errorCode, byte[] debugData)
    {
      this(new StreamBufferingEncoder.GoAwayDetail(lastStreamId, errorCode, debugData));
    }
    
    Http2GoAwayException(StreamBufferingEncoder.GoAwayDetail goAwayDetail) {
      super();
      this.goAwayDetail = goAwayDetail;
    }
    
    public int lastStreamId() {
      return StreamBufferingEncoder.GoAwayDetail.access$000(goAwayDetail);
    }
    
    public long errorCode() {
      return StreamBufferingEncoder.GoAwayDetail.access$100(goAwayDetail);
    }
    
    public byte[] debugData() {
      return (byte[])StreamBufferingEncoder.GoAwayDetail.access$200(goAwayDetail).clone();
    }
  }
  




  private final TreeMap<Integer, PendingStream> pendingStreams = new TreeMap();
  private int maxConcurrentStreams;
  private boolean closed;
  private GoAwayDetail goAwayDetail;
  
  public StreamBufferingEncoder(Http2ConnectionEncoder delegate) {
    this(delegate, 100);
  }
  
  public StreamBufferingEncoder(Http2ConnectionEncoder delegate, int initialMaxConcurrentStreams) {
    super(delegate);
    maxConcurrentStreams = initialMaxConcurrentStreams;
    connection().addListener(new Http2ConnectionAdapter()
    {
      public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData)
      {
        goAwayDetail = new StreamBufferingEncoder.GoAwayDetail(lastStreamId, errorCode, 
        

          ByteBufUtil.getBytes(debugData, debugData.readerIndex(), debugData.readableBytes(), false));
        StreamBufferingEncoder.this.cancelGoAwayStreams(goAwayDetail);
      }
      
      public void onStreamClosed(Http2Stream stream)
      {
        StreamBufferingEncoder.this.tryCreatePendingStreams();
      }
    });
  }
  


  public int numBufferedStreams()
  {
    return pendingStreams.size();
  }
  

  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream, ChannelPromise promise)
  {
    return writeHeaders(ctx, streamId, headers, 0, (short)16, false, padding, endStream, promise);
  }
  



  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise)
  {
    if (closed) {
      return promise.setFailure(new Http2ChannelClosedException());
    }
    if ((isExistingStream(streamId)) || (canCreateStream())) {
      return super.writeHeaders(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream, promise);
    }
    
    if (goAwayDetail != null) {
      return promise.setFailure(new Http2GoAwayException(goAwayDetail));
    }
    PendingStream pendingStream = (PendingStream)pendingStreams.get(Integer.valueOf(streamId));
    if (pendingStream == null) {
      pendingStream = new PendingStream(ctx, streamId);
      pendingStreams.put(Integer.valueOf(streamId), pendingStream);
    }
    frames.add(new HeadersFrame(headers, streamDependency, weight, exclusive, padding, endOfStream, promise));
    
    return promise;
  }
  

  public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    if (isExistingStream(streamId)) {
      return super.writeRstStream(ctx, streamId, errorCode, promise);
    }
    

    PendingStream stream = (PendingStream)pendingStreams.remove(Integer.valueOf(streamId));
    if (stream != null)
    {



      stream.close(null);
      promise.setSuccess();
    } else {
      promise.setFailure(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream does not exist %d", new Object[] { Integer.valueOf(streamId) }));
    }
    return promise;
  }
  

  public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise)
  {
    if (isExistingStream(streamId)) {
      return super.writeData(ctx, streamId, data, padding, endOfStream, promise);
    }
    PendingStream pendingStream = (PendingStream)pendingStreams.get(Integer.valueOf(streamId));
    if (pendingStream != null) {
      frames.add(new DataFrame(data, padding, endOfStream, promise));
    } else {
      ReferenceCountUtil.safeRelease(data);
      promise.setFailure(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream does not exist %d", new Object[] { Integer.valueOf(streamId) }));
    }
    return promise;
  }
  

  public void remoteSettings(Http2Settings settings)
    throws Http2Exception
  {
    super.remoteSettings(settings);
    

    maxConcurrentStreams = connection().local().maxActiveStreams();
    

    tryCreatePendingStreams();
  }
  
  public void close()
  {
    try {
      if (!closed) {
        closed = true;
        

        Http2ChannelClosedException e = new Http2ChannelClosedException();
        while (!pendingStreams.isEmpty()) {
          PendingStream stream = (PendingStream)pendingStreams.pollFirstEntry().getValue();
          stream.close(e);
        }
      }
      
      super.close(); } finally { super.close();
    }
  }
  
  private void tryCreatePendingStreams() {
    while ((!pendingStreams.isEmpty()) && (canCreateStream())) {
      Map.Entry<Integer, PendingStream> entry = pendingStreams.pollFirstEntry();
      PendingStream pendingStream = (PendingStream)entry.getValue();
      try {
        pendingStream.sendFrames();
      } catch (Throwable t) {
        pendingStream.close(t);
      }
    }
  }
  
  private void cancelGoAwayStreams(GoAwayDetail goAwayDetail) {
    Iterator<PendingStream> iter = pendingStreams.values().iterator();
    Exception e = new Http2GoAwayException(goAwayDetail);
    while (iter.hasNext()) {
      PendingStream stream = (PendingStream)iter.next();
      if (streamId > lastStreamId) {
        iter.remove();
        stream.close(e);
      }
    }
  }
  


  private boolean canCreateStream()
  {
    return connection().local().numActiveStreams() < maxConcurrentStreams;
  }
  
  private boolean isExistingStream(int streamId) {
    return streamId <= connection().local().lastStreamCreated();
  }
  
  private static final class PendingStream {
    final ChannelHandlerContext ctx;
    final int streamId;
    final Queue<StreamBufferingEncoder.Frame> frames = new ArrayDeque(2);
    
    PendingStream(ChannelHandlerContext ctx, int streamId) {
      this.ctx = ctx;
      this.streamId = streamId;
    }
    
    void sendFrames() {
      for (StreamBufferingEncoder.Frame frame : frames) {
        frame.send(ctx, streamId);
      }
    }
    
    void close(Throwable t) {
      for (StreamBufferingEncoder.Frame frame : frames) {
        frame.release(t);
      }
    }
  }
  
  private static abstract class Frame {
    final ChannelPromise promise;
    
    Frame(ChannelPromise promise) {
      this.promise = promise;
    }
    


    void release(Throwable t)
    {
      if (t == null) {
        promise.setSuccess();
      } else {
        promise.setFailure(t);
      }
    }
    
    abstract void send(ChannelHandlerContext paramChannelHandlerContext, int paramInt);
  }
  
  private final class HeadersFrame extends StreamBufferingEncoder.Frame
  {
    final Http2Headers headers;
    final int streamDependency;
    final short weight;
    final boolean exclusive;
    final int padding;
    final boolean endOfStream;
    
    HeadersFrame(Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise) {
      super();
      this.headers = headers;
      this.streamDependency = streamDependency;
      this.weight = weight;
      this.exclusive = exclusive;
      this.padding = padding;
      this.endOfStream = endOfStream;
    }
    
    void send(ChannelHandlerContext ctx, int streamId)
    {
      writeHeaders(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream, promise);
    }
  }
  
  private final class DataFrame extends StreamBufferingEncoder.Frame {
    final ByteBuf data;
    final int padding;
    final boolean endOfStream;
    
    DataFrame(ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise) {
      super();
      this.data = data;
      this.padding = padding;
      this.endOfStream = endOfStream;
    }
    
    void release(Throwable t)
    {
      super.release(t);
      ReferenceCountUtil.safeRelease(data);
    }
    
    void send(ChannelHandlerContext ctx, int streamId)
    {
      writeData(ctx, streamId, data, padding, endOfStream, promise);
    }
  }
}
