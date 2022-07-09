package io.netty.util.internal.svm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className="io.netty.util.internal.CleanerJava6")
final class CleanerJava6Substitution
{
  @Alias
  @RecomputeFieldValue(kind=RecomputeFieldValue.Kind.FieldOffset, declClassName="java.nio.DirectByteBuffer", name="cleaner")
  private static long CLEANER_FIELD_OFFSET;
  
  private CleanerJava6Substitution() {}
}
