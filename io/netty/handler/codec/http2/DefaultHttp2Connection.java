package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPromise;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.collection.IntObjectMap.PrimitiveEntry;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.UnaryPromiseNotifier;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;







































public class DefaultHttp2Connection
  implements Http2Connection
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultHttp2Connection.class);
  
  final IntObjectMap<Http2Stream> streamMap = new IntObjectHashMap();
  final PropertyKeyRegistry propertyKeyRegistry = new PropertyKeyRegistry(null);
  final ConnectionStream connectionStream = new ConnectionStream();
  


  final DefaultEndpoint<Http2LocalFlowController> localEndpoint;
  


  final DefaultEndpoint<Http2RemoteFlowController> remoteEndpoint;
  


  final List<Http2Connection.Listener> listeners = new ArrayList(4);
  
  final ActiveStreams activeStreams;
  
  Promise<Void> closePromise;
  

  public DefaultHttp2Connection(boolean server)
  {
    this(server, 100);
  }
  




  public DefaultHttp2Connection(boolean server, int maxReservedStreams)
  {
    activeStreams = new ActiveStreams(listeners);
    




    localEndpoint = new DefaultEndpoint(server, server ? Integer.MAX_VALUE : maxReservedStreams);
    remoteEndpoint = new DefaultEndpoint(!server, maxReservedStreams);
    

    streamMap.put(connectionStream.id(), connectionStream);
  }
  


  final boolean isClosed()
  {
    return closePromise != null;
  }
  
  public Future<Void> close(Promise<Void> promise)
  {
    ObjectUtil.checkNotNull(promise, "promise");
    

    if (closePromise != null) {
      if (closePromise != promise)
      {
        if (((promise instanceof ChannelPromise)) && (((ChannelPromise)closePromise).isVoid())) {
          closePromise = promise;
        } else
          closePromise.addListener(new UnaryPromiseNotifier(promise));
      }
    } else {
      closePromise = promise;
    }
    if (isStreamMapEmpty()) {
      promise.trySuccess(null);
      return promise;
    }
    
    Iterator<IntObjectMap.PrimitiveEntry<Http2Stream>> itr = streamMap.entries().iterator();
    

    if (activeStreams.allowModifications()) {
      activeStreams.incrementPendingIterations();
      try {
        while (itr.hasNext()) {
          DefaultStream stream = (DefaultStream)((IntObjectMap.PrimitiveEntry)itr.next()).value();
          if (stream.id() != 0)
          {


            stream.close(itr);
          }
        }
      } finally {
        activeStreams.decrementPendingIterations();
      }
    } else {
      while (itr.hasNext()) {
        Http2Stream stream = (Http2Stream)((IntObjectMap.PrimitiveEntry)itr.next()).value();
        if (stream.id() != 0)
        {

          stream.close();
        }
      }
    }
    return closePromise;
  }
  
  public void addListener(Http2Connection.Listener listener)
  {
    listeners.add(listener);
  }
  
  public void removeListener(Http2Connection.Listener listener)
  {
    listeners.remove(listener);
  }
  
  public boolean isServer()
  {
    return localEndpoint.isServer();
  }
  
  public Http2Stream connectionStream()
  {
    return connectionStream;
  }
  
  public Http2Stream stream(int streamId)
  {
    return (Http2Stream)streamMap.get(streamId);
  }
  
  public boolean streamMayHaveExisted(int streamId)
  {
    return (remoteEndpoint.mayHaveCreatedStream(streamId)) || (localEndpoint.mayHaveCreatedStream(streamId));
  }
  
  public int numActiveStreams()
  {
    return activeStreams.size();
  }
  
  public Http2Stream forEachActiveStream(Http2StreamVisitor visitor) throws Http2Exception
  {
    return activeStreams.forEachActiveStream(visitor);
  }
  
  public Http2Connection.Endpoint<Http2LocalFlowController> local()
  {
    return localEndpoint;
  }
  
  public Http2Connection.Endpoint<Http2RemoteFlowController> remote()
  {
    return remoteEndpoint;
  }
  
  public boolean goAwayReceived()
  {
    return localEndpoint.lastStreamKnownByPeer >= 0;
  }
  
  public void goAwayReceived(int lastKnownStream, long errorCode, ByteBuf debugData) throws Http2Exception
  {
    if ((localEndpoint.lastStreamKnownByPeer() >= 0) && (localEndpoint.lastStreamKnownByPeer() < lastKnownStream)) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "lastStreamId MUST NOT increase. Current value: %d new value: %d", new Object[] {
        Integer.valueOf(localEndpoint.lastStreamKnownByPeer()), Integer.valueOf(lastKnownStream) });
    }
    
    localEndpoint.lastStreamKnownByPeer(lastKnownStream);
    for (int i = 0; i < listeners.size(); i++) {
      try {
        ((Http2Connection.Listener)listeners.get(i)).onGoAwayReceived(lastKnownStream, errorCode, debugData);
      } catch (Throwable cause) {
        logger.error("Caught Throwable from listener onGoAwayReceived.", cause);
      }
    }
    
    closeStreamsGreaterThanLastKnownStreamId(lastKnownStream, localEndpoint);
  }
  
  public boolean goAwaySent()
  {
    return remoteEndpoint.lastStreamKnownByPeer >= 0;
  }
  
  public boolean goAwaySent(int lastKnownStream, long errorCode, ByteBuf debugData) throws Http2Exception
  {
    if (remoteEndpoint.lastStreamKnownByPeer() >= 0)
    {

      if (lastKnownStream == remoteEndpoint.lastStreamKnownByPeer()) {
        return false;
      }
      if (lastKnownStream > remoteEndpoint.lastStreamKnownByPeer()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Last stream identifier must not increase between sending multiple GOAWAY frames (was '%d', is '%d').", new Object[] {
        
          Integer.valueOf(remoteEndpoint.lastStreamKnownByPeer()), Integer.valueOf(lastKnownStream) });
      }
    }
    
    remoteEndpoint.lastStreamKnownByPeer(lastKnownStream);
    for (int i = 0; i < listeners.size(); i++) {
      try {
        ((Http2Connection.Listener)listeners.get(i)).onGoAwaySent(lastKnownStream, errorCode, debugData);
      } catch (Throwable cause) {
        logger.error("Caught Throwable from listener onGoAwaySent.", cause);
      }
    }
    
    closeStreamsGreaterThanLastKnownStreamId(lastKnownStream, remoteEndpoint);
    return true;
  }
  
  private void closeStreamsGreaterThanLastKnownStreamId(final int lastKnownStream, final DefaultEndpoint<?> endpoint) throws Http2Exception
  {
    forEachActiveStream(new Http2StreamVisitor()
    {
      public boolean visit(Http2Stream stream) {
        if ((stream.id() > lastKnownStream) && (endpoint.isValidStreamId(stream.id()))) {
          stream.close();
        }
        return true;
      }
    });
  }
  


  private boolean isStreamMapEmpty()
  {
    return streamMap.size() == 1;
  }
  


  void removeStream(DefaultStream stream, Iterator<?> itr)
  {
    boolean removed;
    
    boolean removed;
    
    if (itr == null) {
      removed = streamMap.remove(stream.id()) != null;
    } else {
      itr.remove();
      removed = true;
    }
    
    if (removed) {
      for (int i = 0; i < listeners.size(); i++) {
        try {
          ((Http2Connection.Listener)listeners.get(i)).onStreamRemoved(stream);
        } catch (Throwable cause) {
          logger.error("Caught Throwable from listener onStreamRemoved.", cause);
        }
      }
      
      if ((closePromise != null) && (isStreamMapEmpty())) {
        closePromise.trySuccess(null);
      }
    }
  }
  
  static Http2Stream.State activeState(int streamId, Http2Stream.State initialState, boolean isLocal, boolean halfClosed) throws Http2Exception
  {
    switch (2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[initialState.ordinal()]) {
    case 1: 
      return halfClosed ? Http2Stream.State.HALF_CLOSED_REMOTE : isLocal ? Http2Stream.State.HALF_CLOSED_LOCAL : Http2Stream.State.OPEN;
    case 2: 
      return Http2Stream.State.HALF_CLOSED_REMOTE;
    case 3: 
      return Http2Stream.State.HALF_CLOSED_LOCAL;
    }
    throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Attempting to open a stream in an invalid state: " + initialState, new Object[0]);
  }
  

  void notifyHalfClosed(Http2Stream stream)
  {
    for (int i = 0; i < listeners.size(); i++) {
      try {
        ((Http2Connection.Listener)listeners.get(i)).onStreamHalfClosed(stream);
      } catch (Throwable cause) {
        logger.error("Caught Throwable from listener onStreamHalfClosed.", cause);
      }
    }
  }
  
  void notifyClosed(Http2Stream stream) {
    for (int i = 0; i < listeners.size(); i++) {
      try {
        ((Http2Connection.Listener)listeners.get(i)).onStreamClosed(stream);
      } catch (Throwable cause) {
        logger.error("Caught Throwable from listener onStreamClosed.", cause);
      }
    }
  }
  
  public Http2Connection.PropertyKey newKey()
  {
    return propertyKeyRegistry.newKey();
  }
  






  final DefaultPropertyKey verifyKey(Http2Connection.PropertyKey key)
  {
    return ((DefaultPropertyKey)ObjectUtil.checkNotNull((DefaultPropertyKey)key, "key")).verifyConnection(this);
  }
  

  private class DefaultStream
    implements Http2Stream
  {
    private static final byte META_STATE_SENT_RST = 1;
    private static final byte META_STATE_SENT_HEADERS = 2;
    private static final byte META_STATE_SENT_TRAILERS = 4;
    private static final byte META_STATE_SENT_PUSHPROMISE = 8;
    private static final byte META_STATE_RECV_HEADERS = 16;
    private static final byte META_STATE_RECV_TRAILERS = 32;
    private final int id;
    private final PropertyMap properties = new PropertyMap(null);
    private Http2Stream.State state;
    private byte metaState;
    
    DefaultStream(int id, Http2Stream.State state) {
      this.id = id;
      this.state = state;
    }
    
    public final int id()
    {
      return id;
    }
    
    public final Http2Stream.State state()
    {
      return state;
    }
    
    public boolean isResetSent()
    {
      return (metaState & 0x1) != 0;
    }
    
    public Http2Stream resetSent()
    {
      metaState = ((byte)(metaState | 0x1));
      return this;
    }
    
    public Http2Stream headersSent(boolean isInformational)
    {
      if (!isInformational) {
        metaState = ((byte)(metaState | (isHeadersSent() ? 4 : 2)));
      }
      return this;
    }
    
    public boolean isHeadersSent()
    {
      return (metaState & 0x2) != 0;
    }
    
    public boolean isTrailersSent()
    {
      return (metaState & 0x4) != 0;
    }
    
    public Http2Stream headersReceived(boolean isInformational)
    {
      if (!isInformational) {
        metaState = ((byte)(metaState | (isHeadersReceived() ? 32 : 16)));
      }
      return this;
    }
    
    public boolean isHeadersReceived()
    {
      return (metaState & 0x10) != 0;
    }
    
    public boolean isTrailersReceived()
    {
      return (metaState & 0x20) != 0;
    }
    
    public Http2Stream pushPromiseSent()
    {
      metaState = ((byte)(metaState | 0x8));
      return this;
    }
    
    public boolean isPushPromiseSent()
    {
      return (metaState & 0x8) != 0;
    }
    
    public final <V> V setProperty(Http2Connection.PropertyKey key, V value)
    {
      return properties.add(verifyKey(key), value);
    }
    
    public final <V> V getProperty(Http2Connection.PropertyKey key)
    {
      return properties.get(verifyKey(key));
    }
    
    public final <V> V removeProperty(Http2Connection.PropertyKey key)
    {
      return properties.remove(verifyKey(key));
    }
    
    public Http2Stream open(boolean halfClosed) throws Http2Exception
    {
      state = DefaultHttp2Connection.activeState(id, state, isLocal(), halfClosed);
      if (!createdBy().canOpenStream()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Maximum active streams violated for this endpoint.", new Object[0]);
      }
      
      activate();
      return this;
    }
    

    void activate()
    {
      if (state == Http2Stream.State.HALF_CLOSED_LOCAL) {
        headersSent(false);
      } else if (state == Http2Stream.State.HALF_CLOSED_REMOTE) {
        headersReceived(false);
      }
      activeStreams.activate(this);
    }
    
    Http2Stream close(Iterator<?> itr) {
      if (state == Http2Stream.State.CLOSED) {
        return this;
      }
      
      state = Http2Stream.State.CLOSED;
      
      createdBynumStreams -= 1;
      activeStreams.deactivate(this, itr);
      return this;
    }
    
    public Http2Stream close()
    {
      return close(null);
    }
    
    public Http2Stream closeLocalSide()
    {
      switch (DefaultHttp2Connection.2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[state.ordinal()]) {
      case 4: 
        state = Http2Stream.State.HALF_CLOSED_LOCAL;
        notifyHalfClosed(this);
        break;
      case 5: 
        break;
      default: 
        close();
      }
      
      return this;
    }
    
    public Http2Stream closeRemoteSide()
    {
      switch (DefaultHttp2Connection.2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[state.ordinal()]) {
      case 4: 
        state = Http2Stream.State.HALF_CLOSED_REMOTE;
        notifyHalfClosed(this);
        break;
      case 6: 
        break;
      default: 
        close();
      }
      
      return this;
    }
    
    DefaultHttp2Connection.DefaultEndpoint<? extends Http2FlowController> createdBy() {
      return localEndpoint.isValidStreamId(id) ? localEndpoint : remoteEndpoint;
    }
    
    final boolean isLocal() {
      return localEndpoint.isValidStreamId(id);
    }
    
    private class PropertyMap
    {
      private PropertyMap() {}
      
      Object[] values = EmptyArrays.EMPTY_OBJECTS;
      
      <V> V add(DefaultHttp2Connection.DefaultPropertyKey key, V value) {
        resizeIfNecessary(index);
        
        V prevValue = values[index];
        values[index] = value;
        return prevValue;
      }
      
      <V> V get(DefaultHttp2Connection.DefaultPropertyKey key)
      {
        if (index >= values.length) {
          return null;
        }
        return values[index];
      }
      
      <V> V remove(DefaultHttp2Connection.DefaultPropertyKey key)
      {
        V prevValue = null;
        if (index < values.length) {
          prevValue = values[index];
          values[index] = null;
        }
        return prevValue;
      }
      
      void resizeIfNecessary(int index) {
        if (index >= values.length) {
          values = Arrays.copyOf(values, propertyKeyRegistry.size());
        }
      }
    }
  }
  
  private final class ConnectionStream
    extends DefaultHttp2Connection.DefaultStream
  {
    ConnectionStream()
    {
      super(0, Http2Stream.State.IDLE);
    }
    
    public boolean isResetSent()
    {
      return false;
    }
    
    DefaultHttp2Connection.DefaultEndpoint<? extends Http2FlowController> createdBy()
    {
      return null;
    }
    
    public Http2Stream resetSent()
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream open(boolean halfClosed)
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream close()
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream closeLocalSide()
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream closeRemoteSide()
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream headersSent(boolean isInformational)
    {
      throw new UnsupportedOperationException();
    }
    
    public boolean isHeadersSent()
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream pushPromiseSent()
    {
      throw new UnsupportedOperationException();
    }
    
    public boolean isPushPromiseSent()
    {
      throw new UnsupportedOperationException();
    }
  }
  



  private final class DefaultEndpoint<F extends Http2FlowController>
    implements Http2Connection.Endpoint<F>
  {
    private final boolean server;
    


    private int nextStreamIdToCreate;
    


    private int nextReservationStreamId;
    


    private int lastStreamKnownByPeer = -1;
    private boolean pushToAllowed = true;
    private F flowController;
    private int maxStreams;
    private int maxActiveStreams;
    private final int maxReservedStreams;
    int numActiveStreams;
    int numStreams;
    
    DefaultEndpoint(boolean server, int maxReservedStreams)
    {
      this.server = server;
      




      if (server) {
        nextStreamIdToCreate = 2;
        nextReservationStreamId = 0;
      } else {
        nextStreamIdToCreate = 1;
        
        nextReservationStreamId = 1;
      }
      

      pushToAllowed = (!server);
      maxActiveStreams = Integer.MAX_VALUE;
      this.maxReservedStreams = ObjectUtil.checkPositiveOrZero(maxReservedStreams, "maxReservedStreams");
      updateMaxStreams();
    }
    
    public int incrementAndGetNextStreamId()
    {
      return nextReservationStreamId >= 0 ? this.nextReservationStreamId += 2 : nextReservationStreamId;
    }
    
    private void incrementExpectedStreamId(int streamId) {
      if ((streamId > nextReservationStreamId) && (nextReservationStreamId >= 0)) {
        nextReservationStreamId = streamId;
      }
      nextStreamIdToCreate = (streamId + 2);
      numStreams += 1;
    }
    
    public boolean isValidStreamId(int streamId)
    {
      if (streamId > 0) {} return server == ((streamId & 0x1) == 0);
    }
    
    public boolean mayHaveCreatedStream(int streamId)
    {
      return (isValidStreamId(streamId)) && (streamId <= lastStreamCreated());
    }
    
    public boolean canOpenStream()
    {
      return numActiveStreams < maxActiveStreams;
    }
    
    public DefaultHttp2Connection.DefaultStream createStream(int streamId, boolean halfClosed) throws Http2Exception
    {
      Http2Stream.State state = DefaultHttp2Connection.activeState(streamId, Http2Stream.State.IDLE, isLocal(), halfClosed);
      
      checkNewStreamAllowed(streamId, state);
      

      DefaultHttp2Connection.DefaultStream stream = new DefaultHttp2Connection.DefaultStream(DefaultHttp2Connection.this, streamId, state);
      
      incrementExpectedStreamId(streamId);
      
      addStream(stream);
      
      stream.activate();
      return stream;
    }
    
    public boolean created(Http2Stream stream)
    {
      return ((stream instanceof DefaultHttp2Connection.DefaultStream)) && (((DefaultHttp2Connection.DefaultStream)stream).createdBy() == this);
    }
    
    public boolean isServer()
    {
      return server;
    }
    
    public DefaultHttp2Connection.DefaultStream reservePushStream(int streamId, Http2Stream parent) throws Http2Exception
    {
      if (parent == null) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Parent stream missing", new Object[0]);
      }
      if (isLocal() ? !parent.state().localSideOpen() : !parent.state().remoteSideOpen()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream %d is not open for sending push promise", new Object[] { Integer.valueOf(parent.id()) });
      }
      if (!opposite().allowPushTo()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server push not allowed to opposite endpoint", new Object[0]);
      }
      Http2Stream.State state = isLocal() ? Http2Stream.State.RESERVED_LOCAL : Http2Stream.State.RESERVED_REMOTE;
      checkNewStreamAllowed(streamId, state);
      

      DefaultHttp2Connection.DefaultStream stream = new DefaultHttp2Connection.DefaultStream(DefaultHttp2Connection.this, streamId, state);
      
      incrementExpectedStreamId(streamId);
      

      addStream(stream);
      return stream;
    }
    
    private void addStream(DefaultHttp2Connection.DefaultStream stream)
    {
      streamMap.put(stream.id(), stream);
      

      for (int i = 0; i < listeners.size(); i++) {
        try {
          ((Http2Connection.Listener)listeners.get(i)).onStreamAdded(stream);
        } catch (Throwable cause) {
          DefaultHttp2Connection.logger.error("Caught Throwable from listener onStreamAdded.", cause);
        }
      }
    }
    
    public void allowPushTo(boolean allow)
    {
      if ((allow) && (server)) {
        throw new IllegalArgumentException("Servers do not allow push");
      }
      pushToAllowed = allow;
    }
    
    public boolean allowPushTo()
    {
      return pushToAllowed;
    }
    
    public int numActiveStreams()
    {
      return numActiveStreams;
    }
    
    public int maxActiveStreams()
    {
      return maxActiveStreams;
    }
    
    public void maxActiveStreams(int maxActiveStreams)
    {
      this.maxActiveStreams = maxActiveStreams;
      updateMaxStreams();
    }
    
    public int lastStreamCreated()
    {
      return nextStreamIdToCreate > 1 ? nextStreamIdToCreate - 2 : 0;
    }
    
    public int lastStreamKnownByPeer()
    {
      return lastStreamKnownByPeer;
    }
    
    private void lastStreamKnownByPeer(int lastKnownStream) {
      lastStreamKnownByPeer = lastKnownStream;
    }
    
    public F flowController()
    {
      return flowController;
    }
    
    public void flowController(F flowController)
    {
      this.flowController = ((Http2FlowController)ObjectUtil.checkNotNull(flowController, "flowController"));
    }
    
    public Http2Connection.Endpoint<? extends Http2FlowController> opposite()
    {
      return isLocal() ? remoteEndpoint : localEndpoint;
    }
    
    private void updateMaxStreams() {
      maxStreams = ((int)Math.min(2147483647L, maxActiveStreams + maxReservedStreams));
    }
    
    private void checkNewStreamAllowed(int streamId, Http2Stream.State state) throws Http2Exception {
      assert (state != Http2Stream.State.IDLE);
      if ((lastStreamKnownByPeer >= 0) && (streamId > lastStreamKnownByPeer)) {
        throw Http2Exception.streamError(streamId, Http2Error.REFUSED_STREAM, "Cannot create stream %d greater than Last-Stream-ID %d from GOAWAY.", new Object[] {
        
          Integer.valueOf(streamId), Integer.valueOf(lastStreamKnownByPeer) });
      }
      if (!isValidStreamId(streamId)) {
        if (streamId < 0) {
          throw new Http2NoMoreStreamIdsException();
        }
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Request stream %d is not correct for %s connection", new Object[] { Integer.valueOf(streamId), server ? "server" : "client" });
      }
      


      if (streamId < nextStreamIdToCreate) {
        throw Http2Exception.closedStreamError(Http2Error.PROTOCOL_ERROR, "Request stream %d is behind the next expected stream %d", new Object[] {
          Integer.valueOf(streamId), Integer.valueOf(nextStreamIdToCreate) });
      }
      if (nextStreamIdToCreate <= 0)
      {

        throw new Http2Exception(Http2Error.REFUSED_STREAM, "Stream IDs are exhausted for this endpoint.", Http2Exception.ShutdownHint.GRACEFUL_SHUTDOWN);
      }
      
      boolean isReserved = (state == Http2Stream.State.RESERVED_LOCAL) || (state == Http2Stream.State.RESERVED_REMOTE);
      if (((!isReserved) && (!canOpenStream())) || ((isReserved) && (numStreams >= maxStreams))) {
        throw Http2Exception.streamError(streamId, Http2Error.REFUSED_STREAM, "Maximum active streams violated for this endpoint.", new Object[0]);
      }
      if (isClosed()) {
        throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, "Attempted to create stream id %d after connection was closed", new Object[] {
          Integer.valueOf(streamId) });
      }
    }
    
    private boolean isLocal() {
      return this == localEndpoint;
    }
  }
  




  static abstract interface Event
  {
    public abstract void process();
  }
  




  private final class ActiveStreams
  {
    private final List<Http2Connection.Listener> listeners;
    



    private final Queue<DefaultHttp2Connection.Event> pendingEvents = new ArrayDeque(4);
    private final Set<Http2Stream> streams = new LinkedHashSet();
    private int pendingIterations;
    
    ActiveStreams() {
      this.listeners = listeners;
    }
    
    public int size() {
      return streams.size();
    }
    
    public void activate(final DefaultHttp2Connection.DefaultStream stream) {
      if (allowModifications()) {
        addToActiveStreams(stream);
      } else {
        pendingEvents.add(new DefaultHttp2Connection.Event()
        {
          public void process() {
            addToActiveStreams(stream);
          }
        });
      }
    }
    
    public void deactivate(final DefaultHttp2Connection.DefaultStream stream, final Iterator<?> itr) {
      if ((allowModifications()) || (itr != null)) {
        removeFromActiveStreams(stream, itr);
      } else {
        pendingEvents.add(new DefaultHttp2Connection.Event()
        {
          public void process() {
            removeFromActiveStreams(stream, itr);
          }
        });
      }
    }
    
    public Http2Stream forEachActiveStream(Http2StreamVisitor visitor) throws Http2Exception {
      incrementPendingIterations();
      try {
        for (Http2Stream stream : streams) {
          if (!visitor.visit(stream)) {
            return stream;
          }
        }
        return null;
      } finally {
        decrementPendingIterations();
      }
    }
    
    void addToActiveStreams(DefaultHttp2Connection.DefaultStream stream) {
      if (streams.add(stream))
      {
        createdBynumActiveStreams += 1;
        
        for (int i = 0; i < listeners.size(); i++) {
          try {
            ((Http2Connection.Listener)listeners.get(i)).onStreamActive(stream);
          } catch (Throwable cause) {
            DefaultHttp2Connection.logger.error("Caught Throwable from listener onStreamActive.", cause);
          }
        }
      }
    }
    
    void removeFromActiveStreams(DefaultHttp2Connection.DefaultStream stream, Iterator<?> itr) {
      if (streams.remove(stream))
      {
        createdBynumActiveStreams -= 1;
        notifyClosed(stream);
      }
      removeStream(stream, itr);
    }
    
    boolean allowModifications() {
      return pendingIterations == 0;
    }
    
    void incrementPendingIterations() {
      pendingIterations += 1;
    }
    
    void decrementPendingIterations() {
      pendingIterations -= 1;
      if (allowModifications()) {
        for (;;) {
          DefaultHttp2Connection.Event event = (DefaultHttp2Connection.Event)pendingEvents.poll();
          if (event == null) {
            break;
          }
          try {
            event.process();
          } catch (Throwable cause) {
            DefaultHttp2Connection.logger.error("Caught Throwable while processing pending ActiveStreams$Event.", cause);
          }
        }
      }
    }
  }
  
  final class DefaultPropertyKey
    implements Http2Connection.PropertyKey
  {
    final int index;
    
    DefaultPropertyKey(int index)
    {
      this.index = index;
    }
    
    DefaultPropertyKey verifyConnection(Http2Connection connection) {
      if (connection != DefaultHttp2Connection.this) {
        throw new IllegalArgumentException("Using a key that was not created by this connection");
      }
      return this;
    }
  }
  







  private final class PropertyKeyRegistry
  {
    final List<DefaultHttp2Connection.DefaultPropertyKey> keys = new ArrayList(4);
    
    private PropertyKeyRegistry() {}
    
    DefaultHttp2Connection.DefaultPropertyKey newKey()
    {
      DefaultHttp2Connection.DefaultPropertyKey key = new DefaultHttp2Connection.DefaultPropertyKey(DefaultHttp2Connection.this, keys.size());
      keys.add(key);
      return key;
    }
    
    int size() {
      return keys.size();
    }
  }
}
