package io.netty.handler.codec;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public abstract interface Headers<K, V, T extends Headers<K, V, T>>
  extends Iterable<Map.Entry<K, V>>
{
  public abstract V get(K paramK);
  
  public abstract V get(K paramK, V paramV);
  
  public abstract V getAndRemove(K paramK);
  
  public abstract V getAndRemove(K paramK, V paramV);
  
  public abstract List<V> getAll(K paramK);
  
  public abstract List<V> getAllAndRemove(K paramK);
  
  public abstract Boolean getBoolean(K paramK);
  
  public abstract boolean getBoolean(K paramK, boolean paramBoolean);
  
  public abstract Byte getByte(K paramK);
  
  public abstract byte getByte(K paramK, byte paramByte);
  
  public abstract Character getChar(K paramK);
  
  public abstract char getChar(K paramK, char paramChar);
  
  public abstract Short getShort(K paramK);
  
  public abstract short getShort(K paramK, short paramShort);
  
  public abstract Integer getInt(K paramK);
  
  public abstract int getInt(K paramK, int paramInt);
  
  public abstract Long getLong(K paramK);
  
  public abstract long getLong(K paramK, long paramLong);
  
  public abstract Float getFloat(K paramK);
  
  public abstract float getFloat(K paramK, float paramFloat);
  
  public abstract Double getDouble(K paramK);
  
  public abstract double getDouble(K paramK, double paramDouble);
  
  public abstract Long getTimeMillis(K paramK);
  
  public abstract long getTimeMillis(K paramK, long paramLong);
  
  public abstract Boolean getBooleanAndRemove(K paramK);
  
  public abstract boolean getBooleanAndRemove(K paramK, boolean paramBoolean);
  
  public abstract Byte getByteAndRemove(K paramK);
  
  public abstract byte getByteAndRemove(K paramK, byte paramByte);
  
  public abstract Character getCharAndRemove(K paramK);
  
  public abstract char getCharAndRemove(K paramK, char paramChar);
  
  public abstract Short getShortAndRemove(K paramK);
  
  public abstract short getShortAndRemove(K paramK, short paramShort);
  
  public abstract Integer getIntAndRemove(K paramK);
  
  public abstract int getIntAndRemove(K paramK, int paramInt);
  
  public abstract Long getLongAndRemove(K paramK);
  
  public abstract long getLongAndRemove(K paramK, long paramLong);
  
  public abstract Float getFloatAndRemove(K paramK);
  
  public abstract float getFloatAndRemove(K paramK, float paramFloat);
  
  public abstract Double getDoubleAndRemove(K paramK);
  
  public abstract double getDoubleAndRemove(K paramK, double paramDouble);
  
  public abstract Long getTimeMillisAndRemove(K paramK);
  
  public abstract long getTimeMillisAndRemove(K paramK, long paramLong);
  
  public abstract boolean contains(K paramK);
  
  public abstract boolean contains(K paramK, V paramV);
  
  public abstract boolean containsObject(K paramK, Object paramObject);
  
  public abstract boolean containsBoolean(K paramK, boolean paramBoolean);
  
  public abstract boolean containsByte(K paramK, byte paramByte);
  
  public abstract boolean containsChar(K paramK, char paramChar);
  
  public abstract boolean containsShort(K paramK, short paramShort);
  
  public abstract boolean containsInt(K paramK, int paramInt);
  
  public abstract boolean containsLong(K paramK, long paramLong);
  
  public abstract boolean containsFloat(K paramK, float paramFloat);
  
  public abstract boolean containsDouble(K paramK, double paramDouble);
  
  public abstract boolean containsTimeMillis(K paramK, long paramLong);
  
  public abstract int size();
  
  public abstract boolean isEmpty();
  
  public abstract Set<K> names();
  
  public abstract T add(K paramK, V paramV);
  
  public abstract T add(K paramK, Iterable<? extends V> paramIterable);
  
  public abstract T add(K paramK, V... paramVarArgs);
  
  public abstract T addObject(K paramK, Object paramObject);
  
  public abstract T addObject(K paramK, Iterable<?> paramIterable);
  
  public abstract T addObject(K paramK, Object... paramVarArgs);
  
  public abstract T addBoolean(K paramK, boolean paramBoolean);
  
  public abstract T addByte(K paramK, byte paramByte);
  
  public abstract T addChar(K paramK, char paramChar);
  
  public abstract T addShort(K paramK, short paramShort);
  
  public abstract T addInt(K paramK, int paramInt);
  
  public abstract T addLong(K paramK, long paramLong);
  
  public abstract T addFloat(K paramK, float paramFloat);
  
  public abstract T addDouble(K paramK, double paramDouble);
  
  public abstract T addTimeMillis(K paramK, long paramLong);
  
  public abstract T add(Headers<? extends K, ? extends V, ?> paramHeaders);
  
  public abstract T set(K paramK, V paramV);
  
  public abstract T set(K paramK, Iterable<? extends V> paramIterable);
  
  public abstract T set(K paramK, V... paramVarArgs);
  
  public abstract T setObject(K paramK, Object paramObject);
  
  public abstract T setObject(K paramK, Iterable<?> paramIterable);
  
  public abstract T setObject(K paramK, Object... paramVarArgs);
  
  public abstract T setBoolean(K paramK, boolean paramBoolean);
  
  public abstract T setByte(K paramK, byte paramByte);
  
  public abstract T setChar(K paramK, char paramChar);
  
  public abstract T setShort(K paramK, short paramShort);
  
  public abstract T setInt(K paramK, int paramInt);
  
  public abstract T setLong(K paramK, long paramLong);
  
  public abstract T setFloat(K paramK, float paramFloat);
  
  public abstract T setDouble(K paramK, double paramDouble);
  
  public abstract T setTimeMillis(K paramK, long paramLong);
  
  public abstract T set(Headers<? extends K, ? extends V, ?> paramHeaders);
  
  public abstract T setAll(Headers<? extends K, ? extends V, ?> paramHeaders);
  
  public abstract boolean remove(K paramK);
  
  public abstract T clear();
  
  public abstract Iterator<Map.Entry<K, V>> iterator();
}
