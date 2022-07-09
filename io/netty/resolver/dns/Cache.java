package io.netty.resolver.dns;

import io.netty.channel.EventLoop;
import io.netty.util.internal.PlatformDependent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
























abstract class Cache<E>
{
  private static final AtomicReferenceFieldUpdater<Entries, ScheduledFuture> FUTURE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(Entries.class, ScheduledFuture.class, "expirationFuture");
  
  private static final ScheduledFuture<?> CANCELLED = new ScheduledFuture()
  {
    public boolean cancel(boolean mayInterruptIfRunning)
    {
      return false;
    }
    


    public long getDelay(TimeUnit unit)
    {
      return Long.MIN_VALUE;
    }
    
    public int compareTo(Delayed o)
    {
      throw new UnsupportedOperationException();
    }
    
    public boolean isCancelled()
    {
      return true;
    }
    
    public boolean isDone()
    {
      return true;
    }
    
    public Object get()
    {
      throw new UnsupportedOperationException();
    }
    
    public Object get(long timeout, TimeUnit unit)
    {
      throw new UnsupportedOperationException();
    }
  };
  


  static final int MAX_SUPPORTED_TTL_SECS = (int)TimeUnit.DAYS.toSeconds(730L);
  
  private final ConcurrentMap<String, Cache<E>.Entries> resolveCache = PlatformDependent.newConcurrentHashMap();
  
  Cache() {}
  
  final void clear() {
    Iterator<Map.Entry<String, Cache<E>.Entries>> i;
    while (!resolveCache.isEmpty()) {
      for (i = resolveCache.entrySet().iterator(); i.hasNext();) {
        Map.Entry<String, Cache<E>.Entries> e = (Map.Entry)i.next();
        i.remove();
        
        ((Entries)e.getValue()).clearAndCancel();
      }
    }
  }
  


  final boolean clear(String hostname)
  {
    Cache<E>.Entries entries = (Entries)resolveCache.remove(hostname);
    return (entries != null) && (entries.clearAndCancel());
  }
  


  final List<? extends E> get(String hostname)
  {
    Cache<E>.Entries entries = (Entries)resolveCache.get(hostname);
    return entries == null ? null : (List)entries.get();
  }
  


  final void cache(String hostname, E value, int ttl, EventLoop loop)
  {
    Cache<E>.Entries entries = (Entries)resolveCache.get(hostname);
    if (entries == null) {
      entries = new Entries(hostname);
      Cache<E>.Entries oldEntries = (Entries)resolveCache.putIfAbsent(hostname, entries);
      if (oldEntries != null) {
        entries = oldEntries;
      }
    }
    entries.add(value, ttl, loop);
  }
  


  final int size()
  {
    return resolveCache.size();
  }
  


  protected abstract boolean shouldReplaceAll(E paramE);
  


  protected void sortEntries(String hostname, List<E> entries) {}
  


  protected abstract boolean equals(E paramE1, E paramE2);
  


  private final class Entries
    extends AtomicReference<List<E>>
    implements Runnable
  {
    private final String hostname;
    

    volatile ScheduledFuture<?> expirationFuture;
    

    Entries(String hostname)
    {
      super();
      this.hostname = hostname;
    }
    
    void add(E e, int ttl, EventLoop loop) {
      if (!shouldReplaceAll(e)) {
        for (;;) {
          List<E> entries = (List)get();
          if (!entries.isEmpty()) {
            E firstEntry = entries.get(0);
            if (shouldReplaceAll(firstEntry)) {
              assert (entries.size() == 1);
              
              if (compareAndSet(entries, Collections.singletonList(e))) {
                scheduleCacheExpirationIfNeeded(ttl, loop);

              }
              

            }
            else
            {

              List<E> newEntries = new ArrayList(entries.size() + 1);
              int i = 0;
              E replacedEntry = null;
              do {
                E entry = entries.get(i);
                


                if (!equals(e, entry)) {
                  newEntries.add(entry);
                } else {
                  replacedEntry = entry;
                  newEntries.add(e);
                  
                  i++;
                  for (; i < entries.size(); i++) {
                    newEntries.add(entries.get(i));
                  }
                }
                
                i++; } while (i < entries.size());
              if (replacedEntry == null) {
                newEntries.add(e);
              }
              sortEntries(hostname, newEntries);
              
              if (compareAndSet(entries, Collections.unmodifiableList(newEntries))) {
                scheduleCacheExpirationIfNeeded(ttl, loop);
                return;
              }
            } } else if (compareAndSet(entries, Collections.singletonList(e))) {
            scheduleCacheExpirationIfNeeded(ttl, loop);
            return;
          }
        }
      }
      set(Collections.singletonList(e));
      scheduleCacheExpirationIfNeeded(ttl, loop);
    }
    


    private void scheduleCacheExpirationIfNeeded(int ttl, EventLoop loop)
    {
      for (;;)
      {
        ScheduledFuture<?> oldFuture = (ScheduledFuture)Cache.FUTURE_UPDATER.get(this);
        if ((oldFuture != null) && (oldFuture.getDelay(TimeUnit.SECONDS) <= ttl)) break;
        ScheduledFuture<?> newFuture = loop.schedule(this, ttl, TimeUnit.SECONDS);
        





        if (Cache.FUTURE_UPDATER.compareAndSet(this, oldFuture, newFuture)) {
          if (oldFuture == null) break;
          oldFuture.cancel(true); break;
        }
        


        newFuture.cancel(true);
      }
    }
    



    boolean clearAndCancel()
    {
      List<E> entries = (List)getAndSet(Collections.emptyList());
      if (entries.isEmpty()) {
        return false;
      }
      
      ScheduledFuture<?> expirationFuture = (ScheduledFuture)Cache.FUTURE_UPDATER.getAndSet(this, Cache.CANCELLED);
      if (expirationFuture != null) {
        expirationFuture.cancel(false);
      }
      
      return true;
    }
    









    public void run()
    {
      resolveCache.remove(hostname, this);
      
      clearAndCancel();
    }
  }
}
