package io.netty.buffer;

import io.netty.util.ByteProcessor;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.RecyclableArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;






















public class CompositeByteBuf
  extends AbstractReferenceCountedByteBuf
  implements Iterable<ByteBuf>
{
  private static final ByteBuffer EMPTY_NIO_BUFFER = Unpooled.EMPTY_BUFFER.nioBuffer();
  private static final Iterator<ByteBuf> EMPTY_ITERATOR = Collections.emptyList().iterator();
  
  private final ByteBufAllocator alloc;
  
  private final boolean direct;
  private final int maxNumComponents;
  private int componentCount;
  private Component[] components;
  private boolean freed;
  
  private CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, int initSize)
  {
    super(Integer.MAX_VALUE);
    
    this.alloc = ((ByteBufAllocator)ObjectUtil.checkNotNull(alloc, "alloc"));
    if (maxNumComponents < 1) {
      throw new IllegalArgumentException("maxNumComponents: " + maxNumComponents + " (expected: >= 1)");
    }
    

    this.direct = direct;
    this.maxNumComponents = maxNumComponents;
    components = newCompArray(initSize, maxNumComponents);
  }
  
  public CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents) {
    this(alloc, direct, maxNumComponents, 0);
  }
  
  public CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, ByteBuf... buffers) {
    this(alloc, direct, maxNumComponents, buffers, 0);
  }
  
  CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, ByteBuf[] buffers, int offset)
  {
    this(alloc, direct, maxNumComponents, buffers.length - offset);
    
    addComponents0(false, 0, buffers, offset);
    consolidateIfNeeded();
    setIndex0(0, capacity());
  }
  
  public CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, Iterable<ByteBuf> buffers)
  {
    this(alloc, direct, maxNumComponents, (buffers instanceof Collection) ? ((Collection)buffers)
      .size() : 0);
    
    addComponents(false, 0, buffers);
    setIndex(0, capacity());
  }
  






  static final ByteWrapper<byte[]> BYTE_ARRAY_WRAPPER = new ByteWrapper()
  {
    public ByteBuf wrap(byte[] bytes) {
      return Unpooled.wrappedBuffer(bytes);
    }
    
    public boolean isEmpty(byte[] bytes) {
      return bytes.length == 0;
    }
  };
  
  static final ByteWrapper<ByteBuffer> BYTE_BUFFER_WRAPPER = new ByteWrapper()
  {
    public ByteBuf wrap(ByteBuffer bytes) {
      return Unpooled.wrappedBuffer(bytes);
    }
    
    public boolean isEmpty(ByteBuffer bytes) {
      return !bytes.hasRemaining();
    }
  };
  private Component lastAccessed;
  
  <T> CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, ByteWrapper<T> wrapper, T[] buffers, int offset) {
    this(alloc, direct, maxNumComponents, buffers.length - offset);
    
    addComponents0(false, 0, wrapper, buffers, offset);
    consolidateIfNeeded();
    setIndex(0, capacity());
  }
  
  private static Component[] newCompArray(int initComponents, int maxNumComponents) {
    int capacityGuess = Math.min(16, maxNumComponents);
    return new Component[Math.max(initComponents, capacityGuess)];
  }
  
  CompositeByteBuf(ByteBufAllocator alloc)
  {
    super(Integer.MAX_VALUE);
    this.alloc = alloc;
    direct = false;
    maxNumComponents = 0;
    components = null;
  }
  









  public CompositeByteBuf addComponent(ByteBuf buffer)
  {
    return addComponent(false, buffer);
  }
  










  public CompositeByteBuf addComponents(ByteBuf... buffers)
  {
    return addComponents(false, buffers);
  }
  










  public CompositeByteBuf addComponents(Iterable<ByteBuf> buffers)
  {
    return addComponents(false, buffers);
  }
  










  public CompositeByteBuf addComponent(int cIndex, ByteBuf buffer)
  {
    return addComponent(false, cIndex, buffer);
  }
  







  public CompositeByteBuf addComponent(boolean increaseWriterIndex, ByteBuf buffer)
  {
    return addComponent(increaseWriterIndex, componentCount, buffer);
  }
  








  public CompositeByteBuf addComponents(boolean increaseWriterIndex, ByteBuf... buffers)
  {
    ObjectUtil.checkNotNull(buffers, "buffers");
    addComponents0(increaseWriterIndex, componentCount, buffers, 0);
    consolidateIfNeeded();
    return this;
  }
  








  public CompositeByteBuf addComponents(boolean increaseWriterIndex, Iterable<ByteBuf> buffers)
  {
    return addComponents(increaseWriterIndex, componentCount, buffers);
  }
  








  public CompositeByteBuf addComponent(boolean increaseWriterIndex, int cIndex, ByteBuf buffer)
  {
    ObjectUtil.checkNotNull(buffer, "buffer");
    addComponent0(increaseWriterIndex, cIndex, buffer);
    consolidateIfNeeded();
    return this;
  }
  
  private static void checkForOverflow(int capacity, int readableBytes) {
    if (capacity + readableBytes < 0) {
      throw new IllegalArgumentException("Can't increase by " + readableBytes + " as capacity(" + capacity + ") would overflow " + Integer.MAX_VALUE);
    }
  }
  



  private int addComponent0(boolean increaseWriterIndex, int cIndex, ByteBuf buffer)
  {
    assert (buffer != null);
    boolean wasAdded = false;
    try {
      checkComponentIndex(cIndex);
      

      Component c = newComponent(ensureAccessible(buffer), 0);
      int readableBytes = c.length();
      


      checkForOverflow(capacity(), readableBytes);
      
      addComp(cIndex, c);
      wasAdded = true;
      if ((readableBytes > 0) && (cIndex < componentCount - 1)) {
        updateComponentOffsets(cIndex);
      } else if (cIndex > 0) {
        c.reposition(components[(cIndex - 1)].endOffset);
      }
      if (increaseWriterIndex) {
        writerIndex += readableBytes;
      }
      return cIndex;
    } finally {
      if (!wasAdded) {
        buffer.release();
      }
    }
  }
  
  private static ByteBuf ensureAccessible(ByteBuf buf) {
    if ((checkAccessible) && (!buf.isAccessible())) {
      throw new IllegalReferenceCountException(0);
    }
    return buf;
  }
  
  private Component newComponent(ByteBuf buf, int offset)
  {
    int srcIndex = buf.readerIndex();
    int len = buf.readableBytes();
    

    ByteBuf unwrapped = buf;
    int unwrappedIndex = srcIndex;
    while (((unwrapped instanceof WrappedByteBuf)) || ((unwrapped instanceof SwappedByteBuf))) {
      unwrapped = unwrapped.unwrap();
    }
    

    if ((unwrapped instanceof AbstractUnpooledSlicedByteBuf)) {
      unwrappedIndex += ((AbstractUnpooledSlicedByteBuf)unwrapped).idx(0);
      unwrapped = unwrapped.unwrap();
    } else if ((unwrapped instanceof PooledSlicedByteBuf)) {
      unwrappedIndex += adjustment;
      unwrapped = unwrapped.unwrap();
    } else if (((unwrapped instanceof DuplicatedByteBuf)) || ((unwrapped instanceof PooledDuplicatedByteBuf))) {
      unwrapped = unwrapped.unwrap();
    }
    


    ByteBuf slice = buf.capacity() == len ? buf : null;
    
    return new Component(buf.order(ByteOrder.BIG_ENDIAN), srcIndex, unwrapped
      .order(ByteOrder.BIG_ENDIAN), unwrappedIndex, offset, len, slice);
  }
  













  public CompositeByteBuf addComponents(int cIndex, ByteBuf... buffers)
  {
    ObjectUtil.checkNotNull(buffers, "buffers");
    addComponents0(false, cIndex, buffers, 0);
    consolidateIfNeeded();
    return this;
  }
  
  private CompositeByteBuf addComponents0(boolean increaseWriterIndex, int cIndex, ByteBuf[] buffers, int arrOffset)
  {
    int len = buffers.length;int count = len - arrOffset;
    
    int readableBytes = 0;
    int capacity = capacity();
    for (int i = 0; i < buffers.length; i++) {
      readableBytes += buffers[i].readableBytes();
      


      checkForOverflow(capacity, readableBytes);
    }
    
    int ci = Integer.MAX_VALUE;
    try {
      checkComponentIndex(cIndex);
      shiftComps(cIndex, count);
      int nextOffset = cIndex > 0 ? components[(cIndex - 1)].endOffset : 0;
      ByteBuf b; for (ci = cIndex; arrOffset < len; ci++) {
        b = buffers[arrOffset];
        if (b == null) {
          break;
        }
        Component c = newComponent(ensureAccessible(b), nextOffset);
        components[ci] = c;
        nextOffset = endOffset;arrOffset++;
      }
      

      return this;
    }
    finally {
      if (ci < componentCount) {
        if (ci < cIndex + count)
        {
          removeCompRange(ci, cIndex + count);
          for (; arrOffset < len; arrOffset++) {
            ReferenceCountUtil.safeRelease(buffers[arrOffset]);
          }
        }
        updateComponentOffsets(ci);
      }
      if ((increaseWriterIndex) && (ci > cIndex) && (ci <= componentCount)) {
        writerIndex += components[(ci - 1)].endOffset - components[cIndex].offset;
      }
    }
  }
  
  private <T> int addComponents0(boolean increaseWriterIndex, int cIndex, ByteWrapper<T> wrapper, T[] buffers, int offset)
  {
    checkComponentIndex(cIndex);
    

    int i = offset; for (int len = buffers.length; i < len; i++) {
      T b = buffers[i];
      if (b == null) {
        break;
      }
      if (!wrapper.isEmpty(b)) {
        cIndex = addComponent0(increaseWriterIndex, cIndex, wrapper.wrap(b)) + 1;
        int size = componentCount;
        if (cIndex > size) {
          cIndex = size;
        }
      }
    }
    return cIndex;
  }
  












  public CompositeByteBuf addComponents(int cIndex, Iterable<ByteBuf> buffers)
  {
    return addComponents(false, cIndex, buffers);
  }
  









  public CompositeByteBuf addFlattenedComponents(boolean increaseWriterIndex, ByteBuf buffer)
  {
    ObjectUtil.checkNotNull(buffer, "buffer");
    int ridx = buffer.readerIndex();
    int widx = buffer.writerIndex();
    if (ridx == widx) {
      buffer.release();
      return this;
    }
    if (!(buffer instanceof CompositeByteBuf)) {
      addComponent0(increaseWriterIndex, componentCount, buffer);
      consolidateIfNeeded();
      return this; }
    CompositeByteBuf from;
    CompositeByteBuf from;
    if ((buffer instanceof WrappedCompositeByteBuf)) {
      from = (CompositeByteBuf)buffer.unwrap();
    } else {
      from = (CompositeByteBuf)buffer;
    }
    from.checkIndex(ridx, widx - ridx);
    Component[] fromComponents = components;
    int compCountBefore = componentCount;
    int writerIndexBefore = writerIndex;
    try {
      int cidx = from.toComponentIndex0(ridx); for (int newOffset = capacity();; cidx++) {
        Component component = fromComponents[cidx];
        int compOffset = offset;
        int fromIdx = Math.max(ridx, compOffset);
        int toIdx = Math.min(widx, endOffset);
        int len = toIdx - fromIdx;
        if (len > 0) {
          addComp(componentCount, new Component(srcBuf
            .retain(), component.srcIdx(fromIdx), buf, component
            .idx(fromIdx), newOffset, len, null));
        }
        if (widx == toIdx) {
          break;
        }
        newOffset += len;
      }
      if (increaseWriterIndex) {
        writerIndex = (writerIndexBefore + (widx - ridx));
      }
      consolidateIfNeeded();
      buffer.release();
      buffer = null;
      int cidx; return this;
    } finally {
      if (buffer != null)
      {
        if (increaseWriterIndex) {
          writerIndex = writerIndexBefore;
        }
        for (int cidx = componentCount - 1; cidx >= compCountBefore; cidx--) {
          components[cidx].free();
          removeComp(cidx);
        }
      }
    }
  }
  


  private CompositeByteBuf addComponents(boolean increaseIndex, int cIndex, Iterable<ByteBuf> buffers)
  {
    if ((buffers instanceof ByteBuf))
    {
      return addComponent(increaseIndex, cIndex, (ByteBuf)buffers);
    }
    ObjectUtil.checkNotNull(buffers, "buffers");
    Iterator<ByteBuf> it = buffers.iterator();
    try {
      checkComponentIndex(cIndex);
      

      while (it.hasNext()) {
        ByteBuf b = (ByteBuf)it.next();
        if (b == null) {
          break;
        }
        cIndex = addComponent0(increaseIndex, cIndex, b) + 1;
        cIndex = Math.min(cIndex, componentCount);
      }
      
      while (it.hasNext()) {
        ReferenceCountUtil.safeRelease(it.next());
      }
      
      consolidateIfNeeded();
    }
    finally
    {
      while (it.hasNext()) {
        ReferenceCountUtil.safeRelease(it.next());
      }
    }
    
    return this;
  }
  





  private void consolidateIfNeeded()
  {
    int size = componentCount;
    if (size > maxNumComponents) {
      consolidate0(0, size);
    }
  }
  
  private void checkComponentIndex(int cIndex) {
    ensureAccessible();
    if ((cIndex < 0) || (cIndex > componentCount)) {
      throw new IndexOutOfBoundsException(String.format("cIndex: %d (expected: >= 0 && <= numComponents(%d))", new Object[] {
      
        Integer.valueOf(cIndex), Integer.valueOf(componentCount) }));
    }
  }
  
  private void checkComponentIndex(int cIndex, int numComponents) {
    ensureAccessible();
    if ((cIndex < 0) || (cIndex + numComponents > componentCount)) {
      throw new IndexOutOfBoundsException(String.format("cIndex: %d, numComponents: %d (expected: cIndex >= 0 && cIndex + numComponents <= totalNumComponents(%d))", new Object[] {
      

        Integer.valueOf(cIndex), Integer.valueOf(numComponents), Integer.valueOf(componentCount) }));
    }
  }
  
  private void updateComponentOffsets(int cIndex) {
    int size = componentCount;
    if (size <= cIndex) {
      return;
    }
    
    int nextIndex = cIndex > 0 ? components[(cIndex - 1)].endOffset : 0;
    for (; cIndex < size; cIndex++) {
      Component c = components[cIndex];
      c.reposition(nextIndex);
      nextIndex = endOffset;
    }
  }
  




  public CompositeByteBuf removeComponent(int cIndex)
  {
    checkComponentIndex(cIndex);
    Component comp = components[cIndex];
    if (lastAccessed == comp) {
      lastAccessed = null;
    }
    comp.free();
    removeComp(cIndex);
    if (comp.length() > 0)
    {
      updateComponentOffsets(cIndex);
    }
    return this;
  }
  





  public CompositeByteBuf removeComponents(int cIndex, int numComponents)
  {
    checkComponentIndex(cIndex, numComponents);
    
    if (numComponents == 0) {
      return this;
    }
    int endIndex = cIndex + numComponents;
    boolean needsUpdate = false;
    for (int i = cIndex; i < endIndex; i++) {
      Component c = components[i];
      if (c.length() > 0) {
        needsUpdate = true;
      }
      if (lastAccessed == c) {
        lastAccessed = null;
      }
      c.free();
    }
    removeCompRange(cIndex, endIndex);
    
    if (needsUpdate)
    {
      updateComponentOffsets(cIndex);
    }
    return this;
  }
  
  public Iterator<ByteBuf> iterator()
  {
    ensureAccessible();
    return componentCount == 0 ? EMPTY_ITERATOR : new CompositeByteBufIterator(null);
  }
  
  protected int forEachByteAsc0(int start, int end, ByteProcessor processor) throws Exception
  {
    if (end <= start) {
      return -1;
    }
    int i = toComponentIndex0(start); for (int length = end - start; length > 0; i++) {
      Component c = components[i];
      if (offset != endOffset)
      {

        ByteBuf s = buf;
        int localStart = c.idx(start);
        int localLength = Math.min(length, endOffset - start);
        


        int result = (s instanceof AbstractByteBuf) ? ((AbstractByteBuf)s).forEachByteAsc0(localStart, localStart + localLength, processor) : s.forEachByte(localStart, localLength, processor);
        if (result != -1) {
          return result - adjustment;
        }
        start += localLength;
        length -= localLength;
      } }
    return -1;
  }
  
  protected int forEachByteDesc0(int rStart, int rEnd, ByteProcessor processor) throws Exception
  {
    if (rEnd > rStart) {
      return -1;
    }
    int i = toComponentIndex0(rStart); for (int length = 1 + rStart - rEnd; length > 0; i--) {
      Component c = components[i];
      if (offset != endOffset)
      {

        ByteBuf s = buf;
        int localRStart = c.idx(length + rEnd);
        int localLength = Math.min(length, localRStart);int localIndex = localRStart - localLength;
        


        int result = (s instanceof AbstractByteBuf) ? ((AbstractByteBuf)s).forEachByteDesc0(localRStart - 1, localIndex, processor) : s.forEachByteDesc(localIndex, localLength, processor);
        
        if (result != -1) {
          return result - adjustment;
        }
        length -= localLength;
      } }
    return -1;
  }
  


  public List<ByteBuf> decompose(int offset, int length)
  {
    checkIndex(offset, length);
    if (length == 0) {
      return Collections.emptyList();
    }
    
    int componentId = toComponentIndex0(offset);
    int bytesToSlice = length;
    
    Component firstC = components[componentId];
    
    ByteBuf slice = buf.slice(firstC.idx(offset), Math.min(endOffset - offset, bytesToSlice));
    bytesToSlice -= slice.readableBytes();
    
    if (bytesToSlice == 0) {
      return Collections.singletonList(slice);
    }
    
    List<ByteBuf> sliceList = new ArrayList(componentCount - componentId);
    sliceList.add(slice);
    
    do
    {
      Component component = components[(++componentId)];
      slice = buf.slice(component.idx(offset), Math.min(component.length(), bytesToSlice));
      bytesToSlice -= slice.readableBytes();
      sliceList.add(slice);
    } while (bytesToSlice > 0);
    
    return sliceList;
  }
  
  public boolean isDirect()
  {
    int size = componentCount;
    if (size == 0) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!components[i].buf.isDirect()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean hasArray()
  {
    switch (componentCount) {
    case 0: 
      return true;
    case 1: 
      return components[0].buf.hasArray();
    }
    return false;
  }
  

  public byte[] array()
  {
    switch (componentCount) {
    case 0: 
      return EmptyArrays.EMPTY_BYTES;
    case 1: 
      return components[0].buf.array();
    }
    throw new UnsupportedOperationException();
  }
  

  public int arrayOffset()
  {
    switch (componentCount) {
    case 0: 
      return 0;
    case 1: 
      Component c = components[0];
      return c.idx(buf.arrayOffset());
    }
    throw new UnsupportedOperationException();
  }
  

  public boolean hasMemoryAddress()
  {
    switch (componentCount) {
    case 0: 
      return Unpooled.EMPTY_BUFFER.hasMemoryAddress();
    case 1: 
      return components[0].buf.hasMemoryAddress();
    }
    return false;
  }
  

  public long memoryAddress()
  {
    switch (componentCount) {
    case 0: 
      return Unpooled.EMPTY_BUFFER.memoryAddress();
    case 1: 
      Component c = components[0];
      return buf.memoryAddress() + adjustment;
    }
    throw new UnsupportedOperationException();
  }
  

  public int capacity()
  {
    int size = componentCount;
    return size > 0 ? components[(size - 1)].endOffset : 0;
  }
  
  public CompositeByteBuf capacity(int newCapacity)
  {
    checkNewCapacity(newCapacity);
    
    int size = componentCount;int oldCapacity = capacity();
    if (newCapacity > oldCapacity) {
      int paddingLength = newCapacity - oldCapacity;
      ByteBuf padding = allocBuffer(paddingLength).setIndex(0, paddingLength);
      addComponent0(false, size, padding);
      if (componentCount >= maxNumComponents)
      {

        consolidateIfNeeded();
      }
    } else if (newCapacity < oldCapacity) {
      lastAccessed = null;
      int i = size - 1;
      for (int bytesToTrim = oldCapacity - newCapacity; i >= 0; i--) {
        Component c = components[i];
        int cLength = c.length();
        if (bytesToTrim < cLength)
        {
          endOffset -= bytesToTrim;
          ByteBuf slice = slice;
          if (slice == null) {
            break;
          }
          slice = slice.slice(0, c.length()); break;
        }
        

        c.free();
        bytesToTrim -= cLength;
      }
      removeCompRange(i + 1, size);
      
      if (readerIndex() > newCapacity) {
        setIndex0(newCapacity, newCapacity);
      } else if (writerIndex > newCapacity) {
        writerIndex = newCapacity;
      }
    }
    return this;
  }
  
  public ByteBufAllocator alloc()
  {
    return alloc;
  }
  
  public ByteOrder order()
  {
    return ByteOrder.BIG_ENDIAN;
  }
  


  public int numComponents()
  {
    return componentCount;
  }
  


  public int maxNumComponents()
  {
    return maxNumComponents;
  }
  


  public int toComponentIndex(int offset)
  {
    checkIndex(offset);
    return toComponentIndex0(offset);
  }
  
  private int toComponentIndex0(int offset) {
    int size = componentCount;
    if (offset == 0) {
      for (int i = 0; i < size; i++) {
        if (components[i].endOffset > 0) {
          return i;
        }
      }
    }
    if (size <= 2) {
      return (size == 1) || (offset < components[0].endOffset) ? 0 : 1;
    }
    int low = 0; for (int high = size; low <= high;) {
      int mid = low + high >>> 1;
      Component c = components[mid];
      if (offset >= endOffset) {
        low = mid + 1;
      } else if (offset < offset) {
        high = mid - 1;
      } else {
        return mid;
      }
    }
    
    throw new Error("should not reach here");
  }
  
  public int toByteIndex(int cIndex) {
    checkComponentIndex(cIndex);
    return components[cIndex].offset;
  }
  
  public byte getByte(int index)
  {
    Component c = findComponent(index);
    return buf.getByte(c.idx(index));
  }
  
  protected byte _getByte(int index)
  {
    Component c = findComponent0(index);
    return buf.getByte(c.idx(index));
  }
  
  protected short _getShort(int index)
  {
    Component c = findComponent0(index);
    if (index + 2 <= endOffset)
      return buf.getShort(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (short)((_getByte(index) & 0xFF) << 8 | _getByte(index + 1) & 0xFF);
    }
    return (short)(_getByte(index) & 0xFF | (_getByte(index + 1) & 0xFF) << 8);
  }
  

  protected short _getShortLE(int index)
  {
    Component c = findComponent0(index);
    if (index + 2 <= endOffset)
      return buf.getShortLE(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (short)(_getByte(index) & 0xFF | (_getByte(index + 1) & 0xFF) << 8);
    }
    return (short)((_getByte(index) & 0xFF) << 8 | _getByte(index + 1) & 0xFF);
  }
  

  protected int _getUnsignedMedium(int index)
  {
    Component c = findComponent0(index);
    if (index + 3 <= endOffset)
      return buf.getUnsignedMedium(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getShort(index) & 0xFFFF) << 8 | _getByte(index + 2) & 0xFF;
    }
    return _getShort(index) & 0xFFFF | (_getByte(index + 2) & 0xFF) << 16;
  }
  

  protected int _getUnsignedMediumLE(int index)
  {
    Component c = findComponent0(index);
    if (index + 3 <= endOffset)
      return buf.getUnsignedMediumLE(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return _getShortLE(index) & 0xFFFF | (_getByte(index + 2) & 0xFF) << 16;
    }
    return (_getShortLE(index) & 0xFFFF) << 8 | _getByte(index + 2) & 0xFF;
  }
  

  protected int _getInt(int index)
  {
    Component c = findComponent0(index);
    if (index + 4 <= endOffset)
      return buf.getInt(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getShort(index) & 0xFFFF) << 16 | _getShort(index + 2) & 0xFFFF;
    }
    return _getShort(index) & 0xFFFF | (_getShort(index + 2) & 0xFFFF) << 16;
  }
  

  protected int _getIntLE(int index)
  {
    Component c = findComponent0(index);
    if (index + 4 <= endOffset)
      return buf.getIntLE(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return _getShortLE(index) & 0xFFFF | (_getShortLE(index + 2) & 0xFFFF) << 16;
    }
    return (_getShortLE(index) & 0xFFFF) << 16 | _getShortLE(index + 2) & 0xFFFF;
  }
  

  protected long _getLong(int index)
  {
    Component c = findComponent0(index);
    if (index + 8 <= endOffset)
      return buf.getLong(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getInt(index) & 0xFFFFFFFF) << 32 | _getInt(index + 4) & 0xFFFFFFFF;
    }
    return _getInt(index) & 0xFFFFFFFF | (_getInt(index + 4) & 0xFFFFFFFF) << 32;
  }
  

  protected long _getLongLE(int index)
  {
    Component c = findComponent0(index);
    if (index + 8 <= endOffset)
      return buf.getLongLE(c.idx(index));
    if (order() == ByteOrder.BIG_ENDIAN) {
      return _getIntLE(index) & 0xFFFFFFFF | (_getIntLE(index + 4) & 0xFFFFFFFF) << 32;
    }
    return (_getIntLE(index) & 0xFFFFFFFF) << 32 | _getIntLE(index + 4) & 0xFFFFFFFF;
  }
  

  public CompositeByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex0(index);
    while (length > 0) {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      buf.getBytes(c.idx(index), dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuffer dst)
  {
    int limit = dst.limit();
    int length = dst.remaining();
    
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex0(index);
    try {
      while (length > 0) {
        Component c = components[i];
        int localLength = Math.min(length, endOffset - index);
        dst.limit(dst.position() + localLength);
        buf.getBytes(c.idx(index), dst);
        index += localLength;
        length -= localLength;
        i++;
      }
    } finally {
      dst.limit(limit);
    }
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.capacity());
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex0(index);
    while (length > 0) {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      buf.getBytes(c.idx(index), dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length)
    throws IOException
  {
    int count = nioBufferCount();
    if (count == 1) {
      return out.write(internalNioBuffer(index, length));
    }
    long writtenBytes = out.write(nioBuffers(index, length));
    if (writtenBytes > 2147483647L) {
      return Integer.MAX_VALUE;
    }
    return (int)writtenBytes;
  }
  


  public int getBytes(int index, FileChannel out, long position, int length)
    throws IOException
  {
    int count = nioBufferCount();
    if (count == 1) {
      return out.write(internalNioBuffer(index, length), position);
    }
    long writtenBytes = 0L;
    for (ByteBuffer buf : nioBuffers(index, length)) {
      writtenBytes += out.write(buf, position + writtenBytes);
    }
    if (writtenBytes > 2147483647L) {
      return Integer.MAX_VALUE;
    }
    return (int)writtenBytes;
  }
  
  public CompositeByteBuf getBytes(int index, OutputStream out, int length)
    throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex0(index);
    while (length > 0) {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      buf.getBytes(c.idx(index), out, localLength);
      index += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public CompositeByteBuf setByte(int index, int value)
  {
    Component c = findComponent(index);
    buf.setByte(c.idx(index), value);
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    Component c = findComponent0(index);
    buf.setByte(c.idx(index), value);
  }
  
  public CompositeByteBuf setShort(int index, int value)
  {
    checkIndex(index, 2);
    _setShort(index, value);
    return this;
  }
  
  protected void _setShort(int index, int value)
  {
    Component c = findComponent0(index);
    if (index + 2 <= endOffset) {
      buf.setShort(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setByte(index, (byte)(value >>> 8));
      _setByte(index + 1, (byte)value);
    } else {
      _setByte(index, (byte)value);
      _setByte(index + 1, (byte)(value >>> 8));
    }
  }
  
  protected void _setShortLE(int index, int value)
  {
    Component c = findComponent0(index);
    if (index + 2 <= endOffset) {
      buf.setShortLE(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setByte(index, (byte)value);
      _setByte(index + 1, (byte)(value >>> 8));
    } else {
      _setByte(index, (byte)(value >>> 8));
      _setByte(index + 1, (byte)value);
    }
  }
  
  public CompositeByteBuf setMedium(int index, int value)
  {
    checkIndex(index, 3);
    _setMedium(index, value);
    return this;
  }
  
  protected void _setMedium(int index, int value)
  {
    Component c = findComponent0(index);
    if (index + 3 <= endOffset) {
      buf.setMedium(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setShort(index, (short)(value >> 8));
      _setByte(index + 2, (byte)value);
    } else {
      _setShort(index, (short)value);
      _setByte(index + 2, (byte)(value >>> 16));
    }
  }
  
  protected void _setMediumLE(int index, int value)
  {
    Component c = findComponent0(index);
    if (index + 3 <= endOffset) {
      buf.setMediumLE(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setShortLE(index, (short)value);
      _setByte(index + 2, (byte)(value >>> 16));
    } else {
      _setShortLE(index, (short)(value >> 8));
      _setByte(index + 2, (byte)value);
    }
  }
  
  public CompositeByteBuf setInt(int index, int value)
  {
    checkIndex(index, 4);
    _setInt(index, value);
    return this;
  }
  
  protected void _setInt(int index, int value)
  {
    Component c = findComponent0(index);
    if (index + 4 <= endOffset) {
      buf.setInt(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setShort(index, (short)(value >>> 16));
      _setShort(index + 2, (short)value);
    } else {
      _setShort(index, (short)value);
      _setShort(index + 2, (short)(value >>> 16));
    }
  }
  
  protected void _setIntLE(int index, int value)
  {
    Component c = findComponent0(index);
    if (index + 4 <= endOffset) {
      buf.setIntLE(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setShortLE(index, (short)value);
      _setShortLE(index + 2, (short)(value >>> 16));
    } else {
      _setShortLE(index, (short)(value >>> 16));
      _setShortLE(index + 2, (short)value);
    }
  }
  
  public CompositeByteBuf setLong(int index, long value)
  {
    checkIndex(index, 8);
    _setLong(index, value);
    return this;
  }
  
  protected void _setLong(int index, long value)
  {
    Component c = findComponent0(index);
    if (index + 8 <= endOffset) {
      buf.setLong(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setInt(index, (int)(value >>> 32));
      _setInt(index + 4, (int)value);
    } else {
      _setInt(index, (int)value);
      _setInt(index + 4, (int)(value >>> 32));
    }
  }
  
  protected void _setLongLE(int index, long value)
  {
    Component c = findComponent0(index);
    if (index + 8 <= endOffset) {
      buf.setLongLE(c.idx(index), value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setIntLE(index, (int)value);
      _setIntLE(index + 4, (int)(value >>> 32));
    } else {
      _setIntLE(index, (int)(value >>> 32));
      _setIntLE(index + 4, (int)value);
    }
  }
  
  public CompositeByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex0(index);
    while (length > 0) {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      buf.setBytes(c.idx(index), src, srcIndex, localLength);
      index += localLength;
      srcIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuffer src)
  {
    int limit = src.limit();
    int length = src.remaining();
    
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex0(index);
    try {
      while (length > 0) {
        Component c = components[i];
        int localLength = Math.min(length, endOffset - index);
        src.limit(src.position() + localLength);
        buf.setBytes(c.idx(index), src);
        index += localLength;
        length -= localLength;
        i++;
      }
    } finally {
      src.limit(limit);
    }
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.capacity());
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex0(index);
    while (length > 0) {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      buf.setBytes(c.idx(index), src, srcIndex, localLength);
      index += localLength;
      srcIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return in.read(EmptyArrays.EMPTY_BYTES);
    }
    
    int i = toComponentIndex0(index);
    int readBytes = 0;
    do {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      if (localLength == 0)
      {
        i++;
      }
      else {
        int localReadBytes = buf.setBytes(c.idx(index), in, localLength);
        if (localReadBytes < 0) {
          if (readBytes != 0) break;
          return -1;
        }
        



        index += localReadBytes;
        length -= localReadBytes;
        readBytes += localReadBytes;
        if (localReadBytes == localLength)
          i++;
      }
    } while (length > 0);
    
    return readBytes;
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return in.read(EMPTY_NIO_BUFFER);
    }
    
    int i = toComponentIndex0(index);
    int readBytes = 0;
    do {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      if (localLength == 0)
      {
        i++;
      }
      else {
        int localReadBytes = buf.setBytes(c.idx(index), in, localLength);
        
        if (localReadBytes == 0) {
          break;
        }
        
        if (localReadBytes < 0) {
          if (readBytes != 0) break;
          return -1;
        }
        



        index += localReadBytes;
        length -= localReadBytes;
        readBytes += localReadBytes;
        if (localReadBytes == localLength)
          i++;
      }
    } while (length > 0);
    
    return readBytes;
  }
  
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return in.read(EMPTY_NIO_BUFFER, position);
    }
    
    int i = toComponentIndex0(index);
    int readBytes = 0;
    do {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      if (localLength == 0)
      {
        i++;
      }
      else {
        int localReadBytes = buf.setBytes(c.idx(index), in, position + readBytes, localLength);
        
        if (localReadBytes == 0) {
          break;
        }
        
        if (localReadBytes < 0) {
          if (readBytes != 0) break;
          return -1;
        }
        



        index += localReadBytes;
        length -= localReadBytes;
        readBytes += localReadBytes;
        if (localReadBytes == localLength)
          i++;
      }
    } while (length > 0);
    
    return readBytes;
  }
  
  public ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    ByteBuf dst = allocBuffer(length);
    if (length != 0) {
      copyTo(index, length, toComponentIndex0(index), dst);
    }
    return dst;
  }
  
  private void copyTo(int index, int length, int componentId, ByteBuf dst) {
    int dstIndex = 0;
    int i = componentId;
    
    while (length > 0) {
      Component c = components[i];
      int localLength = Math.min(length, endOffset - index);
      buf.getBytes(c.idx(index), dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      i++;
    }
    
    dst.writerIndex(dst.capacity());
  }
  





  public ByteBuf component(int cIndex)
  {
    checkComponentIndex(cIndex);
    return components[cIndex].duplicate();
  }
  





  public ByteBuf componentAtOffset(int offset)
  {
    return findComponent(offset).duplicate();
  }
  





  public ByteBuf internalComponent(int cIndex)
  {
    checkComponentIndex(cIndex);
    return components[cIndex].slice();
  }
  





  public ByteBuf internalComponentAtOffset(int offset)
  {
    return findComponent(offset).slice();
  }
  


  private Component findComponent(int offset)
  {
    Component la = lastAccessed;
    if ((la != null) && (offset >= offset) && (offset < endOffset)) {
      ensureAccessible();
      return la;
    }
    checkIndex(offset);
    return findIt(offset);
  }
  
  private Component findComponent0(int offset) {
    Component la = lastAccessed;
    if ((la != null) && (offset >= offset) && (offset < endOffset)) {
      return la;
    }
    return findIt(offset);
  }
  
  private Component findIt(int offset) {
    int low = 0; for (int high = componentCount; low <= high;) {
      int mid = low + high >>> 1;
      Component c = components[mid];
      if (c == null) {
        throw new IllegalStateException("No component found for offset. Composite buffer layout might be outdated, e.g. from a discardReadBytes call.");
      }
      
      if (offset >= endOffset) {
        low = mid + 1;
      } else if (offset < offset) {
        high = mid - 1;
      } else {
        lastAccessed = c;
        return c;
      }
    }
    
    throw new Error("should not reach here");
  }
  
  public int nioBufferCount()
  {
    int size = componentCount;
    switch (size) {
    case 0: 
      return 1;
    case 1: 
      return components[0].buf.nioBufferCount();
    }
    int count = 0;
    for (int i = 0; i < size; i++) {
      count += components[i].buf.nioBufferCount();
    }
    return count;
  }
  

  public ByteBuffer internalNioBuffer(int index, int length)
  {
    switch (componentCount) {
    case 0: 
      return EMPTY_NIO_BUFFER;
    case 1: 
      return components[0].internalNioBuffer(index, length);
    }
    throw new UnsupportedOperationException();
  }
  

  public ByteBuffer nioBuffer(int index, int length)
  {
    checkIndex(index, length);
    
    switch (componentCount) {
    case 0: 
      return EMPTY_NIO_BUFFER;
    case 1: 
      Component c = components[0];
      ByteBuf buf = buf;
      if (buf.nioBufferCount() == 1) {
        return buf.nioBuffer(c.idx(index), length);
      }
      
      break;
    }
    
    
    ByteBuffer[] buffers = nioBuffers(index, length);
    
    if (buffers.length == 1) {
      return buffers[0];
    }
    
    ByteBuffer merged = ByteBuffer.allocate(length).order(order());
    for (ByteBuffer buf : buffers) {
      merged.put(buf);
    }
    
    merged.flip();
    return merged;
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    checkIndex(index, length);
    if (length == 0) {
      return new ByteBuffer[] { EMPTY_NIO_BUFFER };
    }
    
    RecyclableArrayList buffers = RecyclableArrayList.newInstance(componentCount);
    try {
      int i = toComponentIndex0(index);
      Component c; while (length > 0) {
        c = components[i];
        ByteBuf s = buf;
        int localLength = Math.min(length, endOffset - index);
        switch (s.nioBufferCount()) {
        case 0: 
          throw new UnsupportedOperationException();
        case 1: 
          buffers.add(s.nioBuffer(c.idx(index), localLength));
          break;
        default: 
          Collections.addAll(buffers, s.nioBuffers(c.idx(index), localLength));
        }
        
        index += localLength;
        length -= localLength;
        i++;
      }
      
      return (ByteBuffer[])buffers.toArray(new ByteBuffer[0]);
    } finally {
      buffers.recycle();
    }
  }
  


  public CompositeByteBuf consolidate()
  {
    ensureAccessible();
    consolidate0(0, componentCount);
    return this;
  }
  





  public CompositeByteBuf consolidate(int cIndex, int numComponents)
  {
    checkComponentIndex(cIndex, numComponents);
    consolidate0(cIndex, numComponents);
    return this;
  }
  
  private void consolidate0(int cIndex, int numComponents) {
    if (numComponents <= 1) {
      return;
    }
    
    int endCIndex = cIndex + numComponents;
    int startOffset = cIndex != 0 ? components[cIndex].offset : 0;
    int capacity = components[(endCIndex - 1)].endOffset - startOffset;
    ByteBuf consolidated = allocBuffer(capacity);
    
    for (int i = cIndex; i < endCIndex; i++) {
      components[i].transferTo(consolidated);
    }
    lastAccessed = null;
    removeCompRange(cIndex + 1, endCIndex);
    components[cIndex] = newComponent(consolidated, 0);
    if ((cIndex != 0) || (numComponents != componentCount)) {
      updateComponentOffsets(cIndex);
    }
  }
  


  public CompositeByteBuf discardReadComponents()
  {
    ensureAccessible();
    int readerIndex = readerIndex();
    if (readerIndex == 0) {
      return this;
    }
    

    int writerIndex = writerIndex();
    if ((readerIndex == writerIndex) && (writerIndex == capacity())) {
      int i = 0; for (int size = componentCount; i < size; i++) {
        components[i].free();
      }
      lastAccessed = null;
      clearComps();
      setIndex(0, 0);
      adjustMarkers(readerIndex);
      return this;
    }
    

    int firstComponentId = 0;
    Component c = null;
    for (int size = componentCount; firstComponentId < size; firstComponentId++) {
      c = components[firstComponentId];
      if (endOffset > readerIndex) {
        break;
      }
      c.free();
    }
    if (firstComponentId == 0) {
      return this;
    }
    Component la = lastAccessed;
    if ((la != null) && (endOffset <= readerIndex)) {
      lastAccessed = null;
    }
    removeCompRange(0, firstComponentId);
    

    int offset = offset;
    updateComponentOffsets(0);
    setIndex(readerIndex - offset, writerIndex - offset);
    adjustMarkers(offset);
    return this;
  }
  
  public CompositeByteBuf discardReadBytes()
  {
    ensureAccessible();
    int readerIndex = readerIndex();
    if (readerIndex == 0) {
      return this;
    }
    

    int writerIndex = writerIndex();
    if ((readerIndex == writerIndex) && (writerIndex == capacity())) {
      int i = 0; for (int size = componentCount; i < size; i++) {
        components[i].free();
      }
      lastAccessed = null;
      clearComps();
      setIndex(0, 0);
      adjustMarkers(readerIndex);
      return this;
    }
    
    int firstComponentId = 0;
    Component c = null;
    for (int size = componentCount; firstComponentId < size; firstComponentId++) {
      c = components[firstComponentId];
      if (endOffset > readerIndex) {
        break;
      }
      c.free();
    }
    

    int trimmedBytes = readerIndex - offset;
    offset = 0;
    endOffset -= readerIndex;
    srcAdjustment += readerIndex;
    adjustment += readerIndex;
    ByteBuf slice = slice;
    if (slice != null)
    {

      slice = slice.slice(trimmedBytes, c.length());
    }
    Component la = lastAccessed;
    if ((la != null) && (endOffset <= readerIndex)) {
      lastAccessed = null;
    }
    
    removeCompRange(0, firstComponentId);
    

    updateComponentOffsets(0);
    setIndex(0, writerIndex - readerIndex);
    adjustMarkers(readerIndex);
    return this;
  }
  
  private ByteBuf allocBuffer(int capacity) {
    return direct ? alloc().directBuffer(capacity) : alloc().heapBuffer(capacity);
  }
  
  public String toString()
  {
    String result = super.toString();
    result = result.substring(0, result.length() - 1);
    return result + ", components=" + componentCount + ')';
  }
  
  static abstract interface ByteWrapper<T> {
    public abstract ByteBuf wrap(T paramT);
    
    public abstract boolean isEmpty(T paramT);
  }
  
  private static final class Component { final ByteBuf srcBuf;
    final ByteBuf buf;
    int srcAdjustment;
    int adjustment;
    int offset;
    int endOffset;
    private ByteBuf slice;
    
    Component(ByteBuf srcBuf, int srcOffset, ByteBuf buf, int bufOffset, int offset, int len, ByteBuf slice) { this.srcBuf = srcBuf;
      srcAdjustment = (srcOffset - offset);
      this.buf = buf;
      adjustment = (bufOffset - offset);
      this.offset = offset;
      endOffset = (offset + len);
      this.slice = slice;
    }
    
    int srcIdx(int index) {
      return index + srcAdjustment;
    }
    
    int idx(int index) {
      return index + adjustment;
    }
    
    int length() {
      return endOffset - offset;
    }
    
    void reposition(int newOffset) {
      int move = newOffset - offset;
      endOffset += move;
      srcAdjustment -= move;
      adjustment -= move;
      offset = newOffset;
    }
    
    void transferTo(ByteBuf dst)
    {
      dst.writeBytes(buf, idx(offset), length());
      free();
    }
    
    ByteBuf slice() {
      ByteBuf s = slice;
      if (s == null) {
        slice = (s = srcBuf.slice(srcIdx(offset), length()));
      }
      return s;
    }
    
    ByteBuf duplicate() {
      return srcBuf.duplicate();
    }
    
    ByteBuffer internalNioBuffer(int index, int length)
    {
      return srcBuf.internalNioBuffer(srcIdx(index), length);
    }
    
    void free() {
      slice = null;
      

      srcBuf.release();
    }
  }
  
  public CompositeByteBuf readerIndex(int readerIndex)
  {
    super.readerIndex(readerIndex);
    return this;
  }
  
  public CompositeByteBuf writerIndex(int writerIndex)
  {
    super.writerIndex(writerIndex);
    return this;
  }
  
  public CompositeByteBuf setIndex(int readerIndex, int writerIndex)
  {
    super.setIndex(readerIndex, writerIndex);
    return this;
  }
  
  public CompositeByteBuf clear()
  {
    super.clear();
    return this;
  }
  
  public CompositeByteBuf markReaderIndex()
  {
    super.markReaderIndex();
    return this;
  }
  
  public CompositeByteBuf resetReaderIndex()
  {
    super.resetReaderIndex();
    return this;
  }
  
  public CompositeByteBuf markWriterIndex()
  {
    super.markWriterIndex();
    return this;
  }
  
  public CompositeByteBuf resetWriterIndex()
  {
    super.resetWriterIndex();
    return this;
  }
  
  public CompositeByteBuf ensureWritable(int minWritableBytes)
  {
    super.ensureWritable(minWritableBytes);
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst)
  {
    return getBytes(index, dst, dst.writableBytes());
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst, int length)
  {
    getBytes(index, dst, dst.writerIndex(), length);
    dst.writerIndex(dst.writerIndex() + length);
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, byte[] dst)
  {
    return getBytes(index, dst, 0, dst.length);
  }
  
  public CompositeByteBuf setBoolean(int index, boolean value)
  {
    return setByte(index, value ? 1 : 0);
  }
  
  public CompositeByteBuf setChar(int index, int value)
  {
    return setShort(index, value);
  }
  
  public CompositeByteBuf setFloat(int index, float value)
  {
    return setInt(index, Float.floatToRawIntBits(value));
  }
  
  public CompositeByteBuf setDouble(int index, double value)
  {
    return setLong(index, Double.doubleToRawLongBits(value));
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src)
  {
    super.setBytes(index, src, src.readableBytes());
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src, int length)
  {
    super.setBytes(index, src, length);
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, byte[] src)
  {
    return setBytes(index, src, 0, src.length);
  }
  
  public CompositeByteBuf setZero(int index, int length)
  {
    super.setZero(index, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst)
  {
    super.readBytes(dst, dst.writableBytes());
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst, int length)
  {
    super.readBytes(dst, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst, int dstIndex, int length)
  {
    super.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(byte[] dst)
  {
    super.readBytes(dst, 0, dst.length);
    return this;
  }
  
  public CompositeByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    super.readBytes(dst, dstIndex, length);
    return this;
  }
  
  public CompositeByteBuf readBytes(ByteBuffer dst)
  {
    super.readBytes(dst);
    return this;
  }
  
  public CompositeByteBuf readBytes(OutputStream out, int length) throws IOException
  {
    super.readBytes(out, length);
    return this;
  }
  
  public CompositeByteBuf skipBytes(int length)
  {
    super.skipBytes(length);
    return this;
  }
  
  public CompositeByteBuf writeBoolean(boolean value)
  {
    writeByte(value ? 1 : 0);
    return this;
  }
  
  public CompositeByteBuf writeByte(int value)
  {
    ensureWritable0(1);
    _setByte(writerIndex++, value);
    return this;
  }
  
  public CompositeByteBuf writeShort(int value)
  {
    super.writeShort(value);
    return this;
  }
  
  public CompositeByteBuf writeMedium(int value)
  {
    super.writeMedium(value);
    return this;
  }
  
  public CompositeByteBuf writeInt(int value)
  {
    super.writeInt(value);
    return this;
  }
  
  public CompositeByteBuf writeLong(long value)
  {
    super.writeLong(value);
    return this;
  }
  
  public CompositeByteBuf writeChar(int value)
  {
    super.writeShort(value);
    return this;
  }
  
  public CompositeByteBuf writeFloat(float value)
  {
    super.writeInt(Float.floatToRawIntBits(value));
    return this;
  }
  
  public CompositeByteBuf writeDouble(double value)
  {
    super.writeLong(Double.doubleToRawLongBits(value));
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src)
  {
    super.writeBytes(src, src.readableBytes());
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src, int length)
  {
    super.writeBytes(src, length);
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src, int srcIndex, int length)
  {
    super.writeBytes(src, srcIndex, length);
    return this;
  }
  
  public CompositeByteBuf writeBytes(byte[] src)
  {
    super.writeBytes(src, 0, src.length);
    return this;
  }
  
  public CompositeByteBuf writeBytes(byte[] src, int srcIndex, int length)
  {
    super.writeBytes(src, srcIndex, length);
    return this;
  }
  
  public CompositeByteBuf writeBytes(ByteBuffer src)
  {
    super.writeBytes(src);
    return this;
  }
  
  public CompositeByteBuf writeZero(int length)
  {
    super.writeZero(length);
    return this;
  }
  
  public CompositeByteBuf retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public CompositeByteBuf retain()
  {
    super.retain();
    return this;
  }
  
  public CompositeByteBuf touch()
  {
    return this;
  }
  
  public CompositeByteBuf touch(Object hint)
  {
    return this;
  }
  
  public ByteBuffer[] nioBuffers()
  {
    return nioBuffers(readerIndex(), readableBytes());
  }
  
  public CompositeByteBuf discardSomeReadBytes()
  {
    return discardReadComponents();
  }
  
  protected void deallocate()
  {
    if (freed) {
      return;
    }
    
    freed = true;
    

    int i = 0; for (int size = componentCount; i < size; i++) {
      components[i].free();
    }
  }
  
  boolean isAccessible()
  {
    return !freed;
  }
  
  public ByteBuf unwrap()
  {
    return null;
  }
  
  private final class CompositeByteBufIterator implements Iterator<ByteBuf> {
    private final int size = numComponents();
    private int index;
    
    private CompositeByteBufIterator() {}
    
    public boolean hasNext() { return size > index; }
    

    public ByteBuf next()
    {
      if (size != numComponents()) {
        throw new ConcurrentModificationException();
      }
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      try {
        return components[(index++)].slice();
      } catch (IndexOutOfBoundsException e) {
        throw new ConcurrentModificationException();
      }
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException("Read-Only");
    }
  }
  

  private void clearComps()
  {
    removeCompRange(0, componentCount);
  }
  
  private void removeComp(int i) {
    removeCompRange(i, i + 1);
  }
  
  private void removeCompRange(int from, int to) {
    if (from >= to) {
      return;
    }
    int size = componentCount;
    assert ((from >= 0) && (to <= size));
    if (to < size) {
      System.arraycopy(components, to, components, from, size - to);
    }
    int newSize = size - to + from;
    for (int i = newSize; i < size; i++) {
      components[i] = null;
    }
    componentCount = newSize;
  }
  
  private void addComp(int i, Component c) {
    shiftComps(i, 1);
    components[i] = c;
  }
  
  private void shiftComps(int i, int count) {
    int size = componentCount;int newSize = size + count;
    assert ((i >= 0) && (i <= size) && (count > 0));
    if (newSize > components.length)
    {
      int newArrSize = Math.max(size + (size >> 1), newSize);
      Component[] newArr;
      Component[] newArr; if (i == size) {
        newArr = (Component[])Arrays.copyOf(components, newArrSize, [Lio.netty.buffer.CompositeByteBuf.Component.class);
      } else {
        newArr = new Component[newArrSize];
        if (i > 0) {
          System.arraycopy(components, 0, newArr, 0, i);
        }
        if (i < size) {
          System.arraycopy(components, i, newArr, i + count, size - i);
        }
      }
      components = newArr;
    } else if (i < size) {
      System.arraycopy(components, i, components, i + count, size - i);
    }
    componentCount = newSize;
  }
}
