package io.netty.handler.codec.http2;

import io.netty.util.collection.IntCollections;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.collection.IntObjectMap.PrimitiveEntry;
import io.netty.util.internal.DefaultPriorityQueue;
import io.netty.util.internal.EmptyPriorityQueue;
import io.netty.util.internal.MathUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PriorityQueue;
import io.netty.util.internal.PriorityQueueNode;
import io.netty.util.internal.SystemPropertyUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
















































public final class WeightedFairQueueByteDistributor
  implements StreamByteDistributor
{
  static final int INITIAL_CHILDREN_MAP_SIZE = Math.max(1, SystemPropertyUtil.getInt("io.netty.http2.childrenMapSize", 2));
  


  private static final int DEFAULT_MAX_STATE_ONLY_SIZE = 5;
  


  private final Http2Connection.PropertyKey stateKey;
  

  private final IntObjectMap<State> stateOnlyMap;
  

  private final PriorityQueue<State> stateOnlyRemovalQueue;
  

  private final Http2Connection connection;
  

  private final State connectionState;
  

  private int allocationQuantum = 1024;
  private final int maxStateOnlySize;
  
  public WeightedFairQueueByteDistributor(Http2Connection connection) {
    this(connection, 5);
  }
  
  public WeightedFairQueueByteDistributor(Http2Connection connection, int maxStateOnlySize) {
    ObjectUtil.checkPositiveOrZero(maxStateOnlySize, "maxStateOnlySize");
    if (maxStateOnlySize == 0) {
      stateOnlyMap = IntCollections.emptyMap();
      stateOnlyRemovalQueue = EmptyPriorityQueue.instance();
    } else {
      stateOnlyMap = new IntObjectHashMap(maxStateOnlySize);
      

      stateOnlyRemovalQueue = new DefaultPriorityQueue(StateOnlyComparator.INSTANCE, maxStateOnlySize + 2);
    }
    this.maxStateOnlySize = maxStateOnlySize;
    
    this.connection = connection;
    stateKey = connection.newKey();
    Http2Stream connectionStream = connection.connectionStream();
    connectionStream.setProperty(stateKey, this.connectionState = new State(connectionStream, 16));
    

    connection.addListener(new Http2ConnectionAdapter()
    {
      public void onStreamAdded(Http2Stream stream) {
        WeightedFairQueueByteDistributor.State state = (WeightedFairQueueByteDistributor.State)stateOnlyMap.remove(stream.id());
        if (state == null) {
          state = new WeightedFairQueueByteDistributor.State(WeightedFairQueueByteDistributor.this, stream);
          
          List<WeightedFairQueueByteDistributor.ParentChangedEvent> events = new ArrayList(1);
          connectionState.takeChild(state, false, events);
          notifyParentChanged(events);
        } else {
          stateOnlyRemovalQueue.removeTyped(state);
          stream = stream;
        }
        switch (WeightedFairQueueByteDistributor.2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()]) {
        case 1: 
        case 2: 
          state.setStreamReservedOrActivated();
          

          break;
        }
        
        
        stream.setProperty(stateKey, state);
      }
      
      public void onStreamActive(Http2Stream stream)
      {
        WeightedFairQueueByteDistributor.this.state(stream).setStreamReservedOrActivated();
      }
      


      public void onStreamClosed(Http2Stream stream)
      {
        WeightedFairQueueByteDistributor.this.state(stream).close();
      }
      



      public void onStreamRemoved(Http2Stream stream)
      {
        WeightedFairQueueByteDistributor.State state = WeightedFairQueueByteDistributor.this.state(stream);
        



        stream = null;
        
        if (maxStateOnlySize == 0) {
          parent.removeChild(state);
          return;
        }
        if (stateOnlyRemovalQueue.size() == maxStateOnlySize) {
          WeightedFairQueueByteDistributor.State stateToRemove = (WeightedFairQueueByteDistributor.State)stateOnlyRemovalQueue.peek();
          if (WeightedFairQueueByteDistributor.StateOnlyComparator.INSTANCE.compare(stateToRemove, state) >= 0)
          {

            parent.removeChild(state);
            return;
          }
          stateOnlyRemovalQueue.poll();
          parent.removeChild(stateToRemove);
          stateOnlyMap.remove(streamId);
        }
        stateOnlyRemovalQueue.add(state);
        stateOnlyMap.put(streamId, state);
      }
    });
  }
  
  public void updateStreamableBytes(StreamByteDistributor.StreamState state)
  {
    state(state.stream()).updateStreamableBytes(Http2CodecUtil.streamableBytes(state), 
      (state.hasFrame()) && (state.windowSize() >= 0));
  }
  
  public void updateDependencyTree(int childStreamId, int parentStreamId, short weight, boolean exclusive)
  {
    State state = state(childStreamId);
    if (state == null)
    {


      if (maxStateOnlySize == 0) {
        return;
      }
      state = new State(childStreamId);
      stateOnlyRemovalQueue.add(state);
      stateOnlyMap.put(childStreamId, state);
    }
    
    State newParent = state(parentStreamId);
    if (newParent == null)
    {


      if (maxStateOnlySize == 0) {
        return;
      }
      newParent = new State(parentStreamId);
      stateOnlyRemovalQueue.add(newParent);
      stateOnlyMap.put(parentStreamId, newParent);
      
      List<ParentChangedEvent> events = new ArrayList(1);
      connectionState.takeChild(newParent, false, events);
      notifyParentChanged(events);
    }
    


    if ((activeCountForTree != 0) && (parent != null)) {
      parent.totalQueuedWeights += weight - weight;
    }
    weight = weight;
    
    if ((newParent != parent) || ((exclusive) && (children.size() != 1))) {
      List<ParentChangedEvent> events;
      if (newParent.isDescendantOf(state)) {
        List<ParentChangedEvent> events = new ArrayList(2 + (exclusive ? children.size() : 0));
        parent.takeChild(newParent, false, events);
      } else {
        events = new ArrayList(1 + (exclusive ? children.size() : 0));
      }
      newParent.takeChild(state, exclusive, events);
      notifyParentChanged(events);
    }
    



    while (stateOnlyRemovalQueue.size() > maxStateOnlySize) {
      State stateToRemove = (State)stateOnlyRemovalQueue.poll();
      parent.removeChild(stateToRemove);
      stateOnlyMap.remove(streamId);
    }
  }
  
  public boolean distribute(int maxBytes, StreamByteDistributor.Writer writer)
    throws Http2Exception
  {
    if (connectionState.activeCountForTree == 0) {
      return false;
    }
    

    int oldIsActiveCountForTree;
    
    do
    {
      oldIsActiveCountForTree = connectionState.activeCountForTree;
      
      maxBytes -= distributeToChildren(maxBytes, writer, connectionState);
    } while ((connectionState.activeCountForTree != 0) && ((maxBytes > 0) || (oldIsActiveCountForTree != connectionState.activeCountForTree)));
    

    return connectionState.activeCountForTree != 0;
  }
  



  public void allocationQuantum(int allocationQuantum)
  {
    ObjectUtil.checkPositive(allocationQuantum, "allocationQuantum");
    this.allocationQuantum = allocationQuantum;
  }
  
  private int distribute(int maxBytes, StreamByteDistributor.Writer writer, State state) throws Http2Exception {
    if (state.isActive()) {
      int nsent = Math.min(maxBytes, streamableBytes);
      state.write(nsent, writer);
      if ((nsent == 0) && (maxBytes != 0))
      {



        state.updateStreamableBytes(streamableBytes, false);
      }
      return nsent;
    }
    
    return distributeToChildren(maxBytes, writer, state);
  }
  








  private int distributeToChildren(int maxBytes, StreamByteDistributor.Writer writer, State state)
    throws Http2Exception
  {
    long oldTotalQueuedWeights = totalQueuedWeights;
    State childState = state.pollPseudoTimeQueue();
    State nextChildState = state.peekPseudoTimeQueue();
    childState.setDistributing();
    try {
      assert ((nextChildState == null) || (pseudoTimeToWrite >= pseudoTimeToWrite)) : ("nextChildState[" + streamId + "].pseudoTime(" + pseudoTimeToWrite + ") <  childState[" + streamId + "].pseudoTime(" + pseudoTimeToWrite + ")");
      

      int nsent = distribute(nextChildState == null ? maxBytes : 
        Math.min(maxBytes, (int)Math.min((pseudoTimeToWrite - pseudoTimeToWrite) * weight / oldTotalQueuedWeights + allocationQuantum, 2147483647L)), writer, childState);
      



      pseudoTime += nsent;
      childState.updatePseudoTime(state, nsent, oldTotalQueuedWeights);
      return nsent;
    } finally {
      childState.unsetDistributing();
      


      if (activeCountForTree != 0) {
        state.offerPseudoTimeQueue(childState);
      }
    }
  }
  
  private State state(Http2Stream stream) {
    return (State)stream.getProperty(stateKey);
  }
  
  private State state(int streamId) {
    Http2Stream stream = connection.stream(streamId);
    return stream != null ? state(stream) : (State)stateOnlyMap.get(streamId);
  }
  


  boolean isChild(int childId, int parentId, short weight)
  {
    State parent = state(parentId);
    State child;
    return (children.containsKey(childId)) && 
      (stateparent == parent) && (weight == weight);
  }
  


  int numChildren(int streamId)
  {
    State state = state(streamId);
    return state == null ? 0 : children.size();
  }
  



  void notifyParentChanged(List<ParentChangedEvent> events)
  {
    for (int i = 0; i < events.size(); i++) {
      ParentChangedEvent event = (ParentChangedEvent)events.get(i);
      stateOnlyRemovalQueue.priorityChanged(state);
      if ((state.parent != null) && (state.activeCountForTree != 0)) {
        state.parent.offerAndInitializePseudoTime(state);
        state.parent.activeCountChangeForTree(state.activeCountForTree);
      }
    }
  }
  




  private static final class StateOnlyComparator
    implements Comparator<WeightedFairQueueByteDistributor.State>, Serializable
  {
    private static final long serialVersionUID = -4806936913002105966L;
    


    static final StateOnlyComparator INSTANCE = new StateOnlyComparator();
    

    private StateOnlyComparator() {}
    

    public int compare(WeightedFairQueueByteDistributor.State o1, WeightedFairQueueByteDistributor.State o2)
    {
      boolean o1Actived = o1.wasStreamReservedOrActivated();
      if (o1Actived != o2.wasStreamReservedOrActivated()) {
        return o1Actived ? -1 : 1;
      }
      
      int x = dependencyTreeDepth - dependencyTreeDepth;
      







      return x != 0 ? x : streamId - streamId;
    }
  }
  
  private static final class StatePseudoTimeComparator implements Comparator<WeightedFairQueueByteDistributor.State>, Serializable
  {
    private static final long serialVersionUID = -1437548640227161828L;
    static final StatePseudoTimeComparator INSTANCE = new StatePseudoTimeComparator();
    

    private StatePseudoTimeComparator() {}
    
    public int compare(WeightedFairQueueByteDistributor.State o1, WeightedFairQueueByteDistributor.State o2)
    {
      return MathUtil.compare(pseudoTimeToWrite, pseudoTimeToWrite);
    }
  }
  

  private final class State
    implements PriorityQueueNode
  {
    private static final byte STATE_IS_ACTIVE = 1;
    
    private static final byte STATE_IS_DISTRIBUTING = 2;
    
    private static final byte STATE_STREAM_ACTIVATED = 4;
    
    Http2Stream stream;
    
    State parent;
    IntObjectMap<State> children = IntCollections.emptyMap();
    
    private final PriorityQueue<State> pseudoTimeQueue;
    
    final int streamId;
    
    int streamableBytes;
    int dependencyTreeDepth;
    int activeCountForTree;
    private int pseudoTimeQueueIndex = -1;
    private int stateOnlyQueueIndex = -1;
    

    long pseudoTimeToWrite;
    
    long pseudoTime;
    
    long totalQueuedWeights;
    
    private byte flags;
    
    short weight = 16;
    
    State(int streamId) {
      this(streamId, null, 0);
    }
    
    State(Http2Stream stream) {
      this(stream, 0);
    }
    
    State(Http2Stream stream, int initialSize) {
      this(stream.id(), stream, initialSize);
    }
    
    State(int streamId, Http2Stream stream, int initialSize) {
      this.stream = stream;
      this.streamId = streamId;
      pseudoTimeQueue = new DefaultPriorityQueue(WeightedFairQueueByteDistributor.StatePseudoTimeComparator.INSTANCE, initialSize);
    }
    
    boolean isDescendantOf(State state) {
      State next = parent;
      while (next != null) {
        if (next == state) {
          return true;
        }
        next = parent;
      }
      return false;
    }
    
    void takeChild(State child, boolean exclusive, List<WeightedFairQueueByteDistributor.ParentChangedEvent> events) {
      takeChild(null, child, exclusive, events);
    }
    




    void takeChild(Iterator<IntObjectMap.PrimitiveEntry<State>> childItr, State child, boolean exclusive, List<WeightedFairQueueByteDistributor.ParentChangedEvent> events)
    {
      State oldParent = parent;
      
      if (oldParent != this) {
        events.add(new WeightedFairQueueByteDistributor.ParentChangedEvent(child, oldParent));
        child.setParent(this);
        


        if (childItr != null) {
          childItr.remove();
        } else if (oldParent != null) {
          children.remove(streamId);
        }
        

        initChildrenIfEmpty();
        
        State oldChild = (State)children.put(streamId, child);
        assert (oldChild == null) : "A stream with the same stream ID was already in the child map.";
      }
      
      if ((exclusive) && (!children.isEmpty()))
      {

        Iterator<IntObjectMap.PrimitiveEntry<State>> itr = removeAllChildrenExcept(child).entries().iterator();
        while (itr.hasNext()) {
          child.takeChild(itr, (State)((IntObjectMap.PrimitiveEntry)itr.next()).value(), false, events);
        }
      }
    }
    


    void removeChild(State child)
    {
      if (children.remove(streamId) != null) {
        List<WeightedFairQueueByteDistributor.ParentChangedEvent> events = new ArrayList(1 + children.size());
        events.add(new WeightedFairQueueByteDistributor.ParentChangedEvent(child, parent));
        child.setParent(null);
        
        if (!children.isEmpty())
        {
          Iterator<IntObjectMap.PrimitiveEntry<State>> itr = children.entries().iterator();
          long totalWeight = child.getTotalWeight();
          do
          {
            State dependency = (State)((IntObjectMap.PrimitiveEntry)itr.next()).value();
            weight = ((short)(int)Math.max(1L, weight * weight / totalWeight));
            takeChild(itr, dependency, false, events);
          } while (itr.hasNext());
        }
        
        notifyParentChanged(events);
      }
    }
    
    private long getTotalWeight() {
      long totalWeight = 0L;
      for (State state : children.values()) {
        totalWeight += weight;
      }
      return totalWeight;
    }
    




    private IntObjectMap<State> removeAllChildrenExcept(State stateToRetain)
    {
      stateToRetain = (State)children.remove(streamId);
      IntObjectMap<State> prevChildren = children;
      

      initChildren();
      if (stateToRetain != null) {
        children.put(streamId, stateToRetain);
      }
      return prevChildren;
    }
    
    private void setParent(State newParent)
    {
      if ((activeCountForTree != 0) && (parent != null)) {
        parent.removePseudoTimeQueue(this);
        parent.activeCountChangeForTree(-activeCountForTree);
      }
      parent = newParent;
      
      dependencyTreeDepth = (newParent == null ? Integer.MAX_VALUE : dependencyTreeDepth + 1);
    }
    
    private void initChildrenIfEmpty() {
      if (children == IntCollections.emptyMap()) {
        initChildren();
      }
    }
    
    private void initChildren() {
      children = new IntObjectHashMap(WeightedFairQueueByteDistributor.INITIAL_CHILDREN_MAP_SIZE);
    }
    
    void write(int numBytes, StreamByteDistributor.Writer writer) throws Http2Exception {
      assert (stream != null);
      try {
        writer.write(stream, numBytes);
      } catch (Throwable t) {
        throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, t, "byte distribution write error", new Object[0]);
      }
    }
    
    void activeCountChangeForTree(int increment) {
      assert (activeCountForTree + increment >= 0);
      activeCountForTree += increment;
      if (parent != null) {
        if ((!$assertionsDisabled) && (activeCountForTree == increment) && (pseudoTimeQueueIndex != -1))
        {
          if (!parent.pseudoTimeQueue.containsTyped(this)) {
            throw new AssertionError("State[" + streamId + "].activeCountForTree changed from 0 to " + increment + " is in a pseudoTimeQueue, but not in parent[ " + parent.streamId + "]'s pseudoTimeQueue");
          }
        }
        

        if (activeCountForTree == 0) {
          parent.removePseudoTimeQueue(this);
        } else if ((activeCountForTree == increment) && (!isDistributing()))
        {







          parent.offerAndInitializePseudoTime(this);
        }
        parent.activeCountChangeForTree(increment);
      }
    }
    
    void updateStreamableBytes(int newStreamableBytes, boolean isActive) {
      if (isActive() != isActive) {
        if (isActive) {
          activeCountChangeForTree(1);
          setActive();
        } else {
          activeCountChangeForTree(-1);
          unsetActive();
        }
      }
      
      streamableBytes = newStreamableBytes;
    }
    


    void updatePseudoTime(State parentState, int nsent, long totalQueuedWeights)
    {
      assert ((streamId != 0) && (nsent >= 0));
      

      pseudoTimeToWrite = (Math.min(pseudoTimeToWrite, pseudoTime) + nsent * totalQueuedWeights / weight);
    }
    




    void offerAndInitializePseudoTime(State state)
    {
      pseudoTimeToWrite = pseudoTime;
      offerPseudoTimeQueue(state);
    }
    
    void offerPseudoTimeQueue(State state) {
      pseudoTimeQueue.offer(state);
      totalQueuedWeights += weight;
    }
    


    State pollPseudoTimeQueue()
    {
      State state = (State)pseudoTimeQueue.poll();
      
      totalQueuedWeights -= weight;
      return state;
    }
    
    void removePseudoTimeQueue(State state) {
      if (pseudoTimeQueue.removeTyped(state)) {
        totalQueuedWeights -= weight;
      }
    }
    
    State peekPseudoTimeQueue() {
      return (State)pseudoTimeQueue.peek();
    }
    
    void close() {
      updateStreamableBytes(0, false);
      stream = null;
    }
    
    boolean wasStreamReservedOrActivated() {
      return (flags & 0x4) != 0;
    }
    
    void setStreamReservedOrActivated() {
      flags = ((byte)(flags | 0x4));
    }
    
    boolean isActive() {
      return (flags & 0x1) != 0;
    }
    
    private void setActive() {
      flags = ((byte)(flags | 0x1));
    }
    
    private void unsetActive() {
      flags = ((byte)(flags & 0xFFFFFFFE));
    }
    
    boolean isDistributing() {
      return (flags & 0x2) != 0;
    }
    
    void setDistributing() {
      flags = ((byte)(flags | 0x2));
    }
    
    void unsetDistributing() {
      flags = ((byte)(flags & 0xFFFFFFFD));
    }
    
    public int priorityQueueIndex(DefaultPriorityQueue<?> queue)
    {
      return queue == stateOnlyRemovalQueue ? stateOnlyQueueIndex : pseudoTimeQueueIndex;
    }
    
    public void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i)
    {
      if (queue == stateOnlyRemovalQueue) {
        stateOnlyQueueIndex = i;
      } else {
        pseudoTimeQueueIndex = i;
      }
    }
    

    public String toString()
    {
      StringBuilder sb = new StringBuilder('Ā' * (activeCountForTree > 0 ? activeCountForTree : 1));
      toString(sb);
      return sb.toString();
    }
    








    private void toString(StringBuilder sb)
    {
      sb.append("{streamId ").append(streamId).append(" streamableBytes ").append(streamableBytes).append(" activeCountForTree ").append(activeCountForTree).append(" pseudoTimeQueueIndex ").append(pseudoTimeQueueIndex).append(" pseudoTimeToWrite ").append(pseudoTimeToWrite).append(" pseudoTime ").append(pseudoTime).append(" flags ").append(flags).append(" pseudoTimeQueue.size() ").append(pseudoTimeQueue.size()).append(" stateOnlyQueueIndex ").append(stateOnlyQueueIndex).append(" parent.streamId ").append(parent == null ? -1 : parent.streamId).append("} [");
      
      if (!pseudoTimeQueue.isEmpty()) {
        for (State s : pseudoTimeQueue) {
          s.toString(sb);
          sb.append(", ");
        }
        
        sb.setLength(sb.length() - 2);
      }
      sb.append(']');
    }
  }
  


  private static final class ParentChangedEvent
  {
    final WeightedFairQueueByteDistributor.State state;
    

    final WeightedFairQueueByteDistributor.State oldParent;
    


    ParentChangedEvent(WeightedFairQueueByteDistributor.State state, WeightedFairQueueByteDistributor.State oldParent)
    {
      this.state = state;
      this.oldParent = oldParent;
    }
  }
}
