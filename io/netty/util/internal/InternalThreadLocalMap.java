package io.netty.util.internal;

import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;























public final class InternalThreadLocalMap
  extends UnpaddedInternalThreadLocalMap
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(InternalThreadLocalMap.class);
  private static final ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = new ThreadLocal();
  
  private static final AtomicInteger nextIndex = new AtomicInteger();
  
  private static final int DEFAULT_ARRAY_LIST_INITIAL_CAPACITY = 8;
  
  private static final int STRING_BUILDER_INITIAL_SIZE;
  private static final int STRING_BUILDER_MAX_SIZE;
  private static final int HANDLER_SHARABLE_CACHE_INITIAL_CAPACITY = 4;
  private static final int INDEXED_VARIABLE_TABLE_INITIAL_SIZE = 32;
  public static final Object UNSET = new Object();
  private Object[] indexedVariables;
  private int futureListenerStackDepth;
  private int localChannelReaderStackDepth;
  private Map<Class<?>, Boolean> handlerSharableCache;
  private IntegerHolder counterHashCode;
  private ThreadLocalRandom random;
  private Map<Class<?>, TypeParameterMatcher> typeParameterMatcherGetCache;
  private Map<Class<?>, Map<String, TypeParameterMatcher>> typeParameterMatcherFindCache;
  private StringBuilder stringBuilder;
  private Map<Charset, CharsetEncoder> charsetEncoderCache;
  private Map<Charset, CharsetDecoder> charsetDecoderCache;
  private ArrayList<Object> arrayList;
  private BitSet cleanerFlags;
  /**
   * @deprecated
   */
  public long rp1;
  /**
   * @deprecated
   */
  public long rp2;
  /**
   * @deprecated
   */
  public long rp3;
  
  static
  {
    STRING_BUILDER_INITIAL_SIZE = SystemPropertyUtil.getInt("io.netty.threadLocalMap.stringBuilder.initialSize", 1024);
    logger.debug("-Dio.netty.threadLocalMap.stringBuilder.initialSize: {}", Integer.valueOf(STRING_BUILDER_INITIAL_SIZE));
    
    STRING_BUILDER_MAX_SIZE = SystemPropertyUtil.getInt("io.netty.threadLocalMap.stringBuilder.maxSize", 4096);
    logger.debug("-Dio.netty.threadLocalMap.stringBuilder.maxSize: {}", Integer.valueOf(STRING_BUILDER_MAX_SIZE));
  }
  
  public static InternalThreadLocalMap getIfSet()
  {
    Thread thread = Thread.currentThread();
    if ((thread instanceof FastThreadLocalThread)) {
      return ((FastThreadLocalThread)thread).threadLocalMap();
    }
    return (InternalThreadLocalMap)slowThreadLocalMap.get();
  }
  
  public static InternalThreadLocalMap get()
  {
    Thread thread = Thread.currentThread();
    if ((thread instanceof FastThreadLocalThread)) {
      return fastGet((FastThreadLocalThread)thread);
    }
    return slowGet();
  }
  
  private static InternalThreadLocalMap fastGet(FastThreadLocalThread thread)
  {
    InternalThreadLocalMap threadLocalMap = thread.threadLocalMap();
    if (threadLocalMap == null) {
      thread.setThreadLocalMap(threadLocalMap = new InternalThreadLocalMap());
    }
    return threadLocalMap;
  }
  
  private static InternalThreadLocalMap slowGet()
  {
    InternalThreadLocalMap ret = (InternalThreadLocalMap)slowThreadLocalMap.get();
    if (ret == null) {
      ret = new InternalThreadLocalMap();
      slowThreadLocalMap.set(ret);
    }
    return ret;
  }
  
  public static void remove()
  {
    Thread thread = Thread.currentThread();
    if ((thread instanceof FastThreadLocalThread)) {
      ((FastThreadLocalThread)thread).setThreadLocalMap(null);
    } else {
      slowThreadLocalMap.remove();
    }
  }
  
  public static void destroy()
  {
    slowThreadLocalMap.remove();
  }
  
  public static int nextVariableIndex()
  {
    int index = nextIndex.getAndIncrement();
    if (index < 0) {
      nextIndex.decrementAndGet();
      throw new IllegalStateException("too many thread-local indexed variables");
    }
    return index;
  }
  
  public static int lastVariableIndex()
  {
    return nextIndex.get() - 1;
  }
  
  private InternalThreadLocalMap()
  {
    indexedVariables = newIndexedVariableTable();
  }
  
  private static Object[] newIndexedVariableTable()
  {
    Object[] array = new Object[32];
    Arrays.fill(array, UNSET);
    return array;
  }
  
  public int size()
  {
    int count = 0;
    
    if (futureListenerStackDepth != 0) {
      count++;
    }
    if (localChannelReaderStackDepth != 0) {
      count++;
    }
    if (handlerSharableCache != null) {
      count++;
    }
    if (counterHashCode != null) {
      count++;
    }
    if (random != null) {
      count++;
    }
    if (typeParameterMatcherGetCache != null) {
      count++;
    }
    if (typeParameterMatcherFindCache != null) {
      count++;
    }
    if (stringBuilder != null) {
      count++;
    }
    if (charsetEncoderCache != null) {
      count++;
    }
    if (charsetDecoderCache != null) {
      count++;
    }
    if (arrayList != null) {
      count++;
    }
    
    for (Object o : indexedVariables) {
      if (o != UNSET) {
        count++;
      }
    }
    


    return count - 1;
  }
  
  public StringBuilder stringBuilder()
  {
    StringBuilder sb = stringBuilder;
    if (sb == null) {
      return this.stringBuilder = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);
    }
    if (sb.capacity() > STRING_BUILDER_MAX_SIZE) {
      sb.setLength(STRING_BUILDER_INITIAL_SIZE);
      sb.trimToSize();
    }
    sb.setLength(0);
    return sb;
  }
  
  public Map<Charset, CharsetEncoder> charsetEncoderCache()
  {
    Map<Charset, CharsetEncoder> cache = charsetEncoderCache;
    if (cache == null) {
      charsetEncoderCache = (cache = new IdentityHashMap());
    }
    return cache;
  }
  
  public Map<Charset, CharsetDecoder> charsetDecoderCache()
  {
    Map<Charset, CharsetDecoder> cache = charsetDecoderCache;
    if (cache == null) {
      charsetDecoderCache = (cache = new IdentityHashMap());
    }
    return cache;
  }
  
  public <E> ArrayList<E> arrayList()
  {
    return arrayList(8);
  }
  
  public <E> ArrayList<E> arrayList(int minCapacity)
  {
    ArrayList<E> list = arrayList;
    if (list == null) {
      arrayList = new ArrayList(minCapacity);
      return arrayList;
    }
    list.clear();
    list.ensureCapacity(minCapacity);
    return list;
  }
  
  public int futureListenerStackDepth()
  {
    return futureListenerStackDepth;
  }
  
  public void setFutureListenerStackDepth(int futureListenerStackDepth)
  {
    this.futureListenerStackDepth = futureListenerStackDepth;
  }
  
  public ThreadLocalRandom random()
  {
    ThreadLocalRandom r = random;
    if (r == null) {
      random = (r = new ThreadLocalRandom());
    }
    return r;
  }
  
  public Map<Class<?>, TypeParameterMatcher> typeParameterMatcherGetCache()
  {
    Map<Class<?>, TypeParameterMatcher> cache = typeParameterMatcherGetCache;
    if (cache == null) {
      typeParameterMatcherGetCache = (cache = new IdentityHashMap());
    }
    return cache;
  }
  
  public Map<Class<?>, Map<String, TypeParameterMatcher>> typeParameterMatcherFindCache()
  {
    Map<Class<?>, Map<String, TypeParameterMatcher>> cache = typeParameterMatcherFindCache;
    if (cache == null) {
      typeParameterMatcherFindCache = (cache = new IdentityHashMap());
    }
    return cache;
  }
  
  @Deprecated
  public IntegerHolder counterHashCode()
  {
    return counterHashCode;
  }
  
  @Deprecated
  public void setCounterHashCode(IntegerHolder counterHashCode)
  {
    this.counterHashCode = counterHashCode;
  }
  
  public Map<Class<?>, Boolean> handlerSharableCache()
  {
    Map<Class<?>, Boolean> cache = handlerSharableCache;
    if (cache == null)
    {
      handlerSharableCache = (cache = new WeakHashMap(4));
    }
    return cache;
  }
  
  public int localChannelReaderStackDepth()
  {
    return localChannelReaderStackDepth;
  }
  
  public void setLocalChannelReaderStackDepth(int localChannelReaderStackDepth)
  {
    this.localChannelReaderStackDepth = localChannelReaderStackDepth;
  }
  
  public Object indexedVariable(int index)
  {
    Object[] lookup = indexedVariables;
    return index < lookup.length ? lookup[index] : UNSET;
  }
  
  /**
   * @deprecated
   */
  public long rp4;
  
  public boolean setIndexedVariable(int index, Object value)
  {
    Object[] lookup = indexedVariables;
    if (index < lookup.length) {
      Object oldValue = lookup[index];
      lookup[index] = value;
      return oldValue == UNSET;
    }
    expandIndexedVariableTableAndSet(index, value);
    return true;
  }
  /**
   * @deprecated
   */
  public long rp5;
  
  private void expandIndexedVariableTableAndSet(int index, Object value)
  {
    Object[] oldArray = indexedVariables;
    int oldCapacity = oldArray.length;
    int newCapacity = index;
    newCapacity |= newCapacity >>> 1;
    newCapacity |= newCapacity >>> 2;
    newCapacity |= newCapacity >>> 4;
    newCapacity |= newCapacity >>> 8;
    newCapacity |= newCapacity >>> 16;
    newCapacity++;
    
    Object[] newArray = Arrays.copyOf(oldArray, newCapacity);
    Arrays.fill(newArray, oldCapacity, newArray.length, UNSET);
    newArray[index] = value;
    indexedVariables = newArray;
  }
  
  public Object removeIndexedVariable(int index)
  {
    Object[] lookup = indexedVariables;
    if (index < lookup.length) {
      Object v = lookup[index];
      lookup[index] = UNSET;
      return v;
    }
    return UNSET;
  }
  
  public boolean isIndexedVariableSet(int index)
  {
    Object[] lookup = indexedVariables;
    return (index < lookup.length) && (lookup[index] != UNSET);
  }
  
  public boolean isCleanerFlagSet(int index) {
    return (cleanerFlags != null) && (cleanerFlags.get(index)); }
  
  public void setCleanerFlag(int index)
  {
    if (cleanerFlags == null) {
      cleanerFlags = new BitSet();
    }
    cleanerFlags.set(index);
  }
  
  /**
   * @deprecated
   */
  public long rp6;
  /**
   * @deprecated
   */
  public long rp7;
  /**
   * @deprecated
   */
  public long rp8;
  /**
   * @deprecated
   */
  public long rp9;
}
