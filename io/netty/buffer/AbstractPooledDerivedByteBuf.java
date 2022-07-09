package io.netty.buffer;

import io.netty.util.internal.ObjectPool.Handle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


























abstract class AbstractPooledDerivedByteBuf
  extends AbstractReferenceCountedByteBuf
{
  private final ObjectPool.Handle<AbstractPooledDerivedByteBuf> recyclerHandle;
  private AbstractByteBuf rootParent;
  private ByteBuf parent;
  
  AbstractPooledDerivedByteBuf(ObjectPool.Handle<? extends AbstractPooledDerivedByteBuf> recyclerHandle)
  {
    super(0);
    this.recyclerHandle = recyclerHandle;
  }
  
  final void parent(ByteBuf newParent)
  {
    assert ((newParent instanceof SimpleLeakAwareByteBuf));
    parent = newParent;
  }
  
  public final AbstractByteBuf unwrap()
  {
    return rootParent;
  }
  
  final <U extends AbstractPooledDerivedByteBuf> U init(AbstractByteBuf unwrapped, ByteBuf wrapped, int readerIndex, int writerIndex, int maxCapacity)
  {
    wrapped.retain();
    parent = wrapped;
    rootParent = unwrapped;
    try
    {
      maxCapacity(maxCapacity);
      setIndex0(readerIndex, writerIndex);
      resetRefCnt();
      

      U castThis = this;
      wrapped = null;
      return castThis;
    } finally {
      if (wrapped != null) {
        parent = (this.rootParent = null);
        wrapped.release();
      }
    }
  }
  



  protected final void deallocate()
  {
    ByteBuf parent = this.parent;
    recyclerHandle.recycle(this);
    parent.release();
  }
  
  public final ByteBufAllocator alloc()
  {
    return unwrap().alloc();
  }
  
  @Deprecated
  public final ByteOrder order()
  {
    return unwrap().order();
  }
  
  public boolean isReadOnly()
  {
    return unwrap().isReadOnly();
  }
  
  public final boolean isDirect()
  {
    return unwrap().isDirect();
  }
  
  public boolean hasArray()
  {
    return unwrap().hasArray();
  }
  
  public byte[] array()
  {
    return unwrap().array();
  }
  
  public boolean hasMemoryAddress()
  {
    return unwrap().hasMemoryAddress();
  }
  
  public boolean isContiguous()
  {
    return unwrap().isContiguous();
  }
  
  public final int nioBufferCount()
  {
    return unwrap().nioBufferCount();
  }
  
  public final ByteBuffer internalNioBuffer(int index, int length)
  {
    return nioBuffer(index, length);
  }
  
  public final ByteBuf retainedSlice()
  {
    int index = readerIndex();
    return retainedSlice(index, writerIndex() - index);
  }
  
  public ByteBuf slice(int index, int length)
  {
    ensureAccessible();
    
    return new PooledNonRetainedSlicedByteBuf(this, unwrap(), index, length);
  }
  
  final ByteBuf duplicate0() {
    ensureAccessible();
    
    return new PooledNonRetainedDuplicateByteBuf(this, unwrap());
  }
  
  private static final class PooledNonRetainedDuplicateByteBuf extends UnpooledDuplicatedByteBuf {
    private final ByteBuf referenceCountDelegate;
    
    PooledNonRetainedDuplicateByteBuf(ByteBuf referenceCountDelegate, AbstractByteBuf buffer) {
      super();
      this.referenceCountDelegate = referenceCountDelegate;
    }
    
    boolean isAccessible0()
    {
      return referenceCountDelegate.isAccessible();
    }
    
    int refCnt0()
    {
      return referenceCountDelegate.refCnt();
    }
    
    ByteBuf retain0()
    {
      referenceCountDelegate.retain();
      return this;
    }
    
    ByteBuf retain0(int increment)
    {
      referenceCountDelegate.retain(increment);
      return this;
    }
    
    ByteBuf touch0()
    {
      referenceCountDelegate.touch();
      return this;
    }
    
    ByteBuf touch0(Object hint)
    {
      referenceCountDelegate.touch(hint);
      return this;
    }
    
    boolean release0()
    {
      return referenceCountDelegate.release();
    }
    
    boolean release0(int decrement)
    {
      return referenceCountDelegate.release(decrement);
    }
    
    public ByteBuf duplicate()
    {
      ensureAccessible();
      return new PooledNonRetainedDuplicateByteBuf(referenceCountDelegate, this);
    }
    
    public ByteBuf retainedDuplicate()
    {
      return PooledDuplicatedByteBuf.newInstance(unwrap(), this, readerIndex(), writerIndex());
    }
    
    public ByteBuf slice(int index, int length)
    {
      checkIndex(index, length);
      return new AbstractPooledDerivedByteBuf.PooledNonRetainedSlicedByteBuf(referenceCountDelegate, unwrap(), index, length);
    }
    

    public ByteBuf retainedSlice()
    {
      return retainedSlice(readerIndex(), capacity());
    }
    
    public ByteBuf retainedSlice(int index, int length)
    {
      return PooledSlicedByteBuf.newInstance(unwrap(), this, index, length);
    }
  }
  
  private static final class PooledNonRetainedSlicedByteBuf extends UnpooledSlicedByteBuf
  {
    private final ByteBuf referenceCountDelegate;
    
    PooledNonRetainedSlicedByteBuf(ByteBuf referenceCountDelegate, AbstractByteBuf buffer, int index, int length) {
      super(index, length);
      this.referenceCountDelegate = referenceCountDelegate;
    }
    
    boolean isAccessible0()
    {
      return referenceCountDelegate.isAccessible();
    }
    
    int refCnt0()
    {
      return referenceCountDelegate.refCnt();
    }
    
    ByteBuf retain0()
    {
      referenceCountDelegate.retain();
      return this;
    }
    
    ByteBuf retain0(int increment)
    {
      referenceCountDelegate.retain(increment);
      return this;
    }
    
    ByteBuf touch0()
    {
      referenceCountDelegate.touch();
      return this;
    }
    
    ByteBuf touch0(Object hint)
    {
      referenceCountDelegate.touch(hint);
      return this;
    }
    
    boolean release0()
    {
      return referenceCountDelegate.release();
    }
    
    boolean release0(int decrement)
    {
      return referenceCountDelegate.release(decrement);
    }
    
    public ByteBuf duplicate()
    {
      ensureAccessible();
      return new AbstractPooledDerivedByteBuf.PooledNonRetainedDuplicateByteBuf(referenceCountDelegate, unwrap())
        .setIndex(idx(readerIndex()), idx(writerIndex()));
    }
    
    public ByteBuf retainedDuplicate()
    {
      return PooledDuplicatedByteBuf.newInstance(unwrap(), this, idx(readerIndex()), idx(writerIndex()));
    }
    
    public ByteBuf slice(int index, int length)
    {
      checkIndex(index, length);
      return new PooledNonRetainedSlicedByteBuf(referenceCountDelegate, unwrap(), idx(index), length);
    }
    

    public ByteBuf retainedSlice()
    {
      return retainedSlice(0, capacity());
    }
    
    public ByteBuf retainedSlice(int index, int length)
    {
      return PooledSlicedByteBuf.newInstance(unwrap(), this, idx(index), length);
    }
  }
}
