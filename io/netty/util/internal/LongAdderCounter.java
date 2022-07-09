package io.netty.util.internal;

import java.util.concurrent.atomic.LongAdder;












@SuppressJava6Requirement(reason="Usage guarded by java version check")
final class LongAdderCounter
  extends LongAdder
  implements LongCounter
{
  LongAdderCounter() {}
  
  public long value()
  {
    return longValue();
  }
}
