package io.netty.util;

import io.netty.util.internal.ReferenceCountUpdater;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;



















public abstract class AbstractReferenceCounted
  implements ReferenceCounted
{
  private static final long REFCNT_FIELD_OFFSET = ReferenceCountUpdater.getUnsafeOffset(AbstractReferenceCounted.class, "refCnt");
  
  private static final AtomicIntegerFieldUpdater<AbstractReferenceCounted> AIF_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCounted.class, "refCnt");
  
  private static final ReferenceCountUpdater<AbstractReferenceCounted> updater = new ReferenceCountUpdater()
  {
    protected AtomicIntegerFieldUpdater<AbstractReferenceCounted> updater()
    {
      return AbstractReferenceCounted.AIF_UPDATER;
    }
    
    protected long unsafeOffset() {
      return AbstractReferenceCounted.REFCNT_FIELD_OFFSET;
    }
  };
  

  private volatile int refCnt = updater
    .initialValue();
  
  public AbstractReferenceCounted() {}
  
  public int refCnt() { return updater.refCnt(this); }
  



  protected final void setRefCnt(int refCnt)
  {
    updater.setRefCnt(this, refCnt);
  }
  
  public ReferenceCounted retain()
  {
    return updater.retain(this);
  }
  
  public ReferenceCounted retain(int increment)
  {
    return updater.retain(this, increment);
  }
  
  public ReferenceCounted touch()
  {
    return touch(null);
  }
  
  public boolean release()
  {
    return handleRelease(updater.release(this));
  }
  
  public boolean release(int decrement)
  {
    return handleRelease(updater.release(this, decrement));
  }
  
  private boolean handleRelease(boolean result) {
    if (result) {
      deallocate();
    }
    return result;
  }
  
  protected abstract void deallocate();
}
