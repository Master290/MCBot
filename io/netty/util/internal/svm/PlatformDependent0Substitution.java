package io.netty.util.internal.svm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className="io.netty.util.internal.PlatformDependent0")
final class PlatformDependent0Substitution
{
  @Alias
  @RecomputeFieldValue(kind=RecomputeFieldValue.Kind.FieldOffset, declClassName="java.nio.Buffer", name="address")
  private static long ADDRESS_FIELD_OFFSET;
  
  private PlatformDependent0Substitution() {}
}
