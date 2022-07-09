package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;







































public class DefaultHttp2LocalFlowController
  implements Http2LocalFlowController
{
  public static final float DEFAULT_WINDOW_UPDATE_RATIO = 0.5F;
  private final Http2Connection connection;
  private final Http2Connection.PropertyKey stateKey;
  private Http2FrameWriter frameWriter;
  private ChannelHandlerContext ctx;
  private float windowUpdateRatio;
  private int initialWindowSize = 65535;
  
  public DefaultHttp2LocalFlowController(Http2Connection connection) {
    this(connection, 0.5F, false);
  }
  














  public DefaultHttp2LocalFlowController(Http2Connection connection, float windowUpdateRatio, boolean autoRefillConnectionWindow)
  {
    this.connection = ((Http2Connection)ObjectUtil.checkNotNull(connection, "connection"));
    windowUpdateRatio(windowUpdateRatio);
    

    stateKey = connection.newKey();
    

    FlowState connectionState = autoRefillConnectionWindow ? new AutoRefillState(connection.connectionStream(), initialWindowSize) : new DefaultState(connection.connectionStream(), initialWindowSize);
    connection.connectionStream().setProperty(stateKey, connectionState);
    

    connection.addListener(new Http2ConnectionAdapter()
    {

      public void onStreamAdded(Http2Stream stream)
      {
        stream.setProperty(stateKey, DefaultHttp2LocalFlowController.REDUCED_FLOW_STATE);
      }
      


      public void onStreamActive(Http2Stream stream)
      {
        stream.setProperty(stateKey, new DefaultHttp2LocalFlowController.DefaultState(DefaultHttp2LocalFlowController.this, stream, initialWindowSize));
      }
      

      public void onStreamClosed(Http2Stream stream)
      {
        try
        {
          DefaultHttp2LocalFlowController.FlowState state = DefaultHttp2LocalFlowController.this.state(stream);
          int unconsumedBytes = state.unconsumedBytes();
          if ((ctx != null) && (unconsumedBytes > 0) && 
            (DefaultHttp2LocalFlowController.this.consumeAllBytes(state, unconsumedBytes)))
          {

            ctx.flush();
          }
        }
        catch (Http2Exception e) {
          PlatformDependent.throwException(e);

        }
        finally
        {
          stream.setProperty(stateKey, DefaultHttp2LocalFlowController.REDUCED_FLOW_STATE);
        }
      }
    });
  }
  
  public DefaultHttp2LocalFlowController frameWriter(Http2FrameWriter frameWriter)
  {
    this.frameWriter = ((Http2FrameWriter)ObjectUtil.checkNotNull(frameWriter, "frameWriter"));
    return this;
  }
  
  public void channelHandlerContext(ChannelHandlerContext ctx)
  {
    this.ctx = ((ChannelHandlerContext)ObjectUtil.checkNotNull(ctx, "ctx"));
  }
  
  public void initialWindowSize(int newWindowSize) throws Http2Exception
  {
    assert ((ctx == null) || (ctx.executor().inEventLoop()));
    int delta = newWindowSize - initialWindowSize;
    initialWindowSize = newWindowSize;
    
    WindowUpdateVisitor visitor = new WindowUpdateVisitor(delta);
    connection.forEachActiveStream(visitor);
    visitor.throwIfError();
  }
  
  public int initialWindowSize()
  {
    return initialWindowSize;
  }
  
  public int windowSize(Http2Stream stream)
  {
    return state(stream).windowSize();
  }
  
  public int initialWindowSize(Http2Stream stream)
  {
    return state(stream).initialWindowSize();
  }
  
  public void incrementWindowSize(Http2Stream stream, int delta) throws Http2Exception
  {
    assert ((ctx != null) && (ctx.executor().inEventLoop()));
    FlowState state = state(stream);
    

    state.incrementInitialStreamWindow(delta);
    state.writeWindowUpdateIfNeeded();
  }
  
  public boolean consumeBytes(Http2Stream stream, int numBytes) throws Http2Exception
  {
    assert ((ctx != null) && (ctx.executor().inEventLoop()));
    ObjectUtil.checkPositiveOrZero(numBytes, "numBytes");
    if (numBytes == 0) {
      return false;
    }
    


    if ((stream != null) && (!isClosed(stream))) {
      if (stream.id() == 0) {
        throw new UnsupportedOperationException("Returning bytes for the connection window is not supported");
      }
      
      return consumeAllBytes(state(stream), numBytes);
    }
    return false;
  }
  
  private boolean consumeAllBytes(FlowState state, int numBytes) throws Http2Exception {
    return connectionState().consumeBytes(numBytes) | state.consumeBytes(numBytes);
  }
  
  public int unconsumedBytes(Http2Stream stream)
  {
    return state(stream).unconsumedBytes();
  }
  
  private static void checkValidRatio(float ratio) {
    if ((Double.compare(ratio, 0.0D) <= 0) || (Double.compare(ratio, 1.0D) >= 0)) {
      throw new IllegalArgumentException("Invalid ratio: " + ratio);
    }
  }
  






  public void windowUpdateRatio(float ratio)
  {
    assert ((ctx == null) || (ctx.executor().inEventLoop()));
    checkValidRatio(ratio);
    windowUpdateRatio = ratio;
  }
  




  public float windowUpdateRatio()
  {
    return windowUpdateRatio;
  }
  











  public void windowUpdateRatio(Http2Stream stream, float ratio)
    throws Http2Exception
  {
    assert ((ctx != null) && (ctx.executor().inEventLoop()));
    checkValidRatio(ratio);
    FlowState state = state(stream);
    state.windowUpdateRatio(ratio);
    state.writeWindowUpdateIfNeeded();
  }
  




  public float windowUpdateRatio(Http2Stream stream)
    throws Http2Exception
  {
    return state(stream).windowUpdateRatio();
  }
  
  public void receiveFlowControlledFrame(Http2Stream stream, ByteBuf data, int padding, boolean endOfStream)
    throws Http2Exception
  {
    assert ((ctx != null) && (ctx.executor().inEventLoop()));
    int dataLength = data.readableBytes() + padding;
    

    FlowState connectionState = connectionState();
    connectionState.receiveFlowControlledFrame(dataLength);
    
    if ((stream != null) && (!isClosed(stream)))
    {
      FlowState state = state(stream);
      state.endOfStream(endOfStream);
      state.receiveFlowControlledFrame(dataLength);
    } else if (dataLength > 0)
    {
      connectionState.consumeBytes(dataLength);
    }
  }
  
  private FlowState connectionState() {
    return (FlowState)connection.connectionStream().getProperty(stateKey);
  }
  
  private FlowState state(Http2Stream stream) {
    return (FlowState)stream.getProperty(stateKey);
  }
  
  private static boolean isClosed(Http2Stream stream) {
    return stream.state() == Http2Stream.State.CLOSED;
  }
  

  private final class AutoRefillState
    extends DefaultHttp2LocalFlowController.DefaultState
  {
    AutoRefillState(Http2Stream stream, int initialWindowSize)
    {
      super(stream, initialWindowSize);
    }
    
    public void receiveFlowControlledFrame(int dataLength) throws Http2Exception
    {
      super.receiveFlowControlledFrame(dataLength);
      
      super.consumeBytes(dataLength);
    }
    
    public boolean consumeBytes(int numBytes)
      throws Http2Exception
    {
      return false;
    }
  }
  



  private class DefaultState
    implements DefaultHttp2LocalFlowController.FlowState
  {
    private final Http2Stream stream;
    


    private int window;
    


    private int processedWindow;
    


    private int initialStreamWindowSize;
    


    private float streamWindowUpdateRatio;
    


    private int lowerBound;
    


    private boolean endOfStream;
    


    DefaultState(Http2Stream stream, int initialWindowSize)
    {
      this.stream = stream;
      window(initialWindowSize);
      streamWindowUpdateRatio = windowUpdateRatio;
    }
    
    public void window(int initialWindowSize)
    {
      assert ((ctx == null) || (ctx.executor().inEventLoop()));
      window = (this.processedWindow = this.initialStreamWindowSize = initialWindowSize);
    }
    
    public int windowSize()
    {
      return window;
    }
    
    public int initialWindowSize()
    {
      return initialStreamWindowSize;
    }
    
    public void endOfStream(boolean endOfStream)
    {
      this.endOfStream = endOfStream;
    }
    
    public float windowUpdateRatio()
    {
      return streamWindowUpdateRatio;
    }
    
    public void windowUpdateRatio(float ratio)
    {
      assert ((ctx == null) || (ctx.executor().inEventLoop()));
      streamWindowUpdateRatio = ratio;
    }
    

    public void incrementInitialStreamWindow(int delta)
    {
      int newValue = (int)Math.min(2147483647L, 
        Math.max(0L, initialStreamWindowSize + delta));
      delta = newValue - initialStreamWindowSize;
      
      initialStreamWindowSize += delta;
    }
    
    public void incrementFlowControlWindows(int delta) throws Http2Exception
    {
      if ((delta > 0) && (window > Integer.MAX_VALUE - delta)) {
        throw Http2Exception.streamError(stream.id(), Http2Error.FLOW_CONTROL_ERROR, "Flow control window overflowed for stream: %d", new Object[] {
          Integer.valueOf(stream.id()) });
      }
      
      window += delta;
      processedWindow += delta;
      lowerBound = (delta < 0 ? delta : 0);
    }
    
    public void receiveFlowControlledFrame(int dataLength) throws Http2Exception
    {
      assert (dataLength >= 0);
      

      window -= dataLength;
      





      if (window < lowerBound) {
        throw Http2Exception.streamError(stream.id(), Http2Error.FLOW_CONTROL_ERROR, "Flow control window exceeded for stream: %d", new Object[] {
          Integer.valueOf(stream.id()) });
      }
    }
    
    private void returnProcessedBytes(int delta) throws Http2Exception {
      if (processedWindow - delta < window) {
        throw Http2Exception.streamError(stream.id(), Http2Error.INTERNAL_ERROR, "Attempting to return too many bytes for stream %d", new Object[] {
          Integer.valueOf(stream.id()) });
      }
      processedWindow -= delta;
    }
    
    public boolean consumeBytes(int numBytes)
      throws Http2Exception
    {
      returnProcessedBytes(numBytes);
      return writeWindowUpdateIfNeeded();
    }
    
    public int unconsumedBytes()
    {
      return processedWindow - window;
    }
    
    public boolean writeWindowUpdateIfNeeded() throws Http2Exception
    {
      if ((endOfStream) || (initialStreamWindowSize <= 0) || 
      
        (DefaultHttp2LocalFlowController.isClosed(stream))) {
        return false;
      }
      
      int threshold = (int)(initialStreamWindowSize * streamWindowUpdateRatio);
      if (processedWindow <= threshold) {
        writeWindowUpdate();
        return true;
      }
      return false;
    }
    



    private void writeWindowUpdate()
      throws Http2Exception
    {
      int deltaWindowSize = initialStreamWindowSize - processedWindow;
      try {
        incrementFlowControlWindows(deltaWindowSize);
      } catch (Throwable t) {
        throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, t, "Attempting to return too many bytes for stream %d", new Object[] {
          Integer.valueOf(stream.id()) });
      }
      

      frameWriter.writeWindowUpdate(ctx, stream.id(), deltaWindowSize, ctx.newPromise());
    }
  }
  




  private static final FlowState REDUCED_FLOW_STATE = new FlowState()
  {
    public int windowSize()
    {
      return 0;
    }
    
    public int initialWindowSize()
    {
      return 0;
    }
    
    public void window(int initialWindowSize)
    {
      throw new UnsupportedOperationException();
    }
    


    public void incrementInitialStreamWindow(int delta) {}
    

    public boolean writeWindowUpdateIfNeeded()
      throws Http2Exception
    {
      throw new UnsupportedOperationException();
    }
    
    public boolean consumeBytes(int numBytes) throws Http2Exception
    {
      return false;
    }
    
    public int unconsumedBytes()
    {
      return 0;
    }
    
    public float windowUpdateRatio()
    {
      throw new UnsupportedOperationException();
    }
    
    public void windowUpdateRatio(float ratio)
    {
      throw new UnsupportedOperationException();
    }
    
    public void receiveFlowControlledFrame(int dataLength) throws Http2Exception
    {
      throw new UnsupportedOperationException();
    }
    

    public void incrementFlowControlWindows(int delta)
      throws Http2Exception
    {}
    

    public void endOfStream(boolean endOfStream)
    {
      throw new UnsupportedOperationException();
    }
  };
  


  private static abstract interface FlowState
  {
    public abstract int windowSize();
    


    public abstract int initialWindowSize();
    


    public abstract void window(int paramInt);
    


    public abstract void incrementInitialStreamWindow(int paramInt);
    


    public abstract boolean writeWindowUpdateIfNeeded()
      throws Http2Exception;
    


    public abstract boolean consumeBytes(int paramInt)
      throws Http2Exception;
    


    public abstract int unconsumedBytes();
    


    public abstract float windowUpdateRatio();
    


    public abstract void windowUpdateRatio(float paramFloat);
    


    public abstract void receiveFlowControlledFrame(int paramInt)
      throws Http2Exception;
    


    public abstract void incrementFlowControlWindows(int paramInt)
      throws Http2Exception;
    

    public abstract void endOfStream(boolean paramBoolean);
  }
  

  private final class WindowUpdateVisitor
    implements Http2StreamVisitor
  {
    private Http2Exception.CompositeStreamException compositeException;
    
    private final int delta;
    

    WindowUpdateVisitor(int delta)
    {
      this.delta = delta;
    }
    
    public boolean visit(Http2Stream stream) throws Http2Exception
    {
      try
      {
        DefaultHttp2LocalFlowController.FlowState state = DefaultHttp2LocalFlowController.this.state(stream);
        state.incrementFlowControlWindows(delta);
        state.incrementInitialStreamWindow(delta);
      } catch (Http2Exception.StreamException e) {
        if (compositeException == null) {
          compositeException = new Http2Exception.CompositeStreamException(e.error(), 4);
        }
        compositeException.add(e);
      }
      return true;
    }
    
    public void throwIfError() throws Http2Exception.CompositeStreamException {
      if (compositeException != null) {
        throw compositeException;
      }
    }
  }
}
