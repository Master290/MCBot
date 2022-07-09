package io.netty.util.internal.svm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className="io.netty.util.internal.PlatformDependent")
final class PlatformDependentSubstitution
{
  @Alias
  @RecomputeFieldValue(kind=RecomputeFieldValue.Kind.ArrayBaseOffset, declClass=byte[].class)
  private static long BYTE_ARRAY_BASE_OFFSET;
  
  private PlatformDependentSubstitution() {}
}
