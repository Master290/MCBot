package io.netty.util.internal.svm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className="io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess")
final class UnsafeRefArrayAccessSubstitution
{
  @Alias
  @RecomputeFieldValue(kind=RecomputeFieldValue.Kind.ArrayIndexShift, declClass=Object[].class)
  public static int REF_ELEMENT_SHIFT;
  
  private UnsafeRefArrayAccessSubstitution() {}
}
