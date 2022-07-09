package io.netty.util.internal;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;



















































public final class ThreadLocalRandom
  extends Random
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadLocalRandom.class);
  
  private static final AtomicLong seedUniquifier = new AtomicLong();
  








  private static volatile long initialSeedUniquifier = SystemPropertyUtil.getLong("io.netty.initialSeedUniquifier", 0L);
  static { if (initialSeedUniquifier == 0L) {
      boolean secureRandom = SystemPropertyUtil.getBoolean("java.util.secureRandomSeed", false);
      if (secureRandom) {
        seedQueue = new LinkedBlockingQueue();
        seedGeneratorStartTime = System.nanoTime();
        


        seedGeneratorThread = new Thread("initialSeedUniquifierGenerator")
        {
          public void run() {
            SecureRandom random = new SecureRandom();
            byte[] seed = random.generateSeed(8);
            ThreadLocalRandom.access$002(System.nanoTime());
            long s = (seed[0] & 0xFF) << 56 | (seed[1] & 0xFF) << 48 | (seed[2] & 0xFF) << 40 | (seed[3] & 0xFF) << 32 | (seed[4] & 0xFF) << 24 | (seed[5] & 0xFF) << 16 | (seed[6] & 0xFF) << 8 | seed[7] & 0xFF;
            






            ThreadLocalRandom.seedQueue.add(Long.valueOf(s));
          }
        };
        seedGeneratorThread.setDaemon(true);
        seedGeneratorThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
          public void uncaughtException(Thread t, Throwable e) {
            ThreadLocalRandom.logger.debug("An exception has been raised by {}", t.getName(), e);
          }
        });
        seedGeneratorThread.start();
      } else {
        initialSeedUniquifier = mix64(System.currentTimeMillis()) ^ mix64(System.nanoTime());
        seedGeneratorThread = null;
        seedQueue = null;
        seedGeneratorStartTime = 0L;
      }
    } else {
      seedGeneratorThread = null;
      seedQueue = null;
      seedGeneratorStartTime = 0L;
    }
  }
  
  public static void setInitialSeedUniquifier(long initialSeedUniquifier) {
    initialSeedUniquifier = initialSeedUniquifier;
  }
  
  public static long getInitialSeedUniquifier()
  {
    long initialSeedUniquifier = initialSeedUniquifier;
    if (initialSeedUniquifier != 0L) {
      return initialSeedUniquifier;
    }
    
    synchronized (ThreadLocalRandom.class) {
      initialSeedUniquifier = initialSeedUniquifier;
      if (initialSeedUniquifier != 0L) {
        return initialSeedUniquifier;
      }
      

      long timeoutSeconds = 3L;
      long deadLine = seedGeneratorStartTime + TimeUnit.SECONDS.toNanos(3L);
      boolean interrupted = false;
      for (;;) {
        long waitTime = deadLine - System.nanoTime();
        try { Long seed;
          Long seed;
          if (waitTime <= 0L) {
            seed = (Long)seedQueue.poll();
          } else {
            seed = (Long)seedQueue.poll(waitTime, TimeUnit.NANOSECONDS);
          }
          
          if (seed != null) {
            initialSeedUniquifier = seed.longValue();
            break;
          }
        } catch (InterruptedException e) {
          interrupted = true;
          logger.warn("Failed to generate a seed from SecureRandom due to an InterruptedException.");
          break;
        }
        
        if (waitTime <= 0L) {
          seedGeneratorThread.interrupt();
          logger.warn("Failed to generate a seed from SecureRandom within {} seconds. Not enough entropy?", 
          
            Long.valueOf(3L));
          
          break;
        }
      }
      

      initialSeedUniquifier ^= 0x3255ECDC33BAE119;
      initialSeedUniquifier ^= Long.reverse(System.nanoTime());
      
      initialSeedUniquifier = initialSeedUniquifier;
      
      if (interrupted)
      {
        Thread.currentThread().interrupt();
        


        seedGeneratorThread.interrupt();
      }
      
      if (seedGeneratorEndTime == 0L) {
        seedGeneratorEndTime = System.nanoTime();
      }
      
      return initialSeedUniquifier;
    }
  }
  
  private static long newSeed() {
    for (;;) {
      long current = seedUniquifier.get();
      long actualCurrent = current != 0L ? current : getInitialSeedUniquifier();
      

      long next = actualCurrent * 181783497276652981L;
      
      if (seedUniquifier.compareAndSet(current, next)) {
        if ((current == 0L) && (logger.isDebugEnabled())) {
          if (seedGeneratorEndTime != 0L) {
            logger.debug(String.format("-Dio.netty.initialSeedUniquifier: 0x%016x (took %d ms)", new Object[] {
            
              Long.valueOf(actualCurrent), 
              Long.valueOf(TimeUnit.NANOSECONDS.toMillis(seedGeneratorEndTime - seedGeneratorStartTime)) }));
          } else {
            logger.debug(String.format("-Dio.netty.initialSeedUniquifier: 0x%016x", new Object[] { Long.valueOf(actualCurrent) }));
          }
        }
        return next ^ System.nanoTime();
      }
    }
  }
  

  private static long mix64(long z)
  {
    z = (z ^ z >>> 33) * -49064778989728563L;
    z = (z ^ z >>> 33) * -4265267296055464877L;
    return z ^ z >>> 33;
  }
  

  private static final Thread seedGeneratorThread;
  
  private static final BlockingQueue<Long> seedQueue;
  
  private static final long seedGeneratorStartTime;
  
  private static volatile long seedGeneratorEndTime;
  
  private static final long multiplier = 25214903917L;
  
  private static final long addend = 11L;
  
  private static final long mask = 281474976710655L;
  private long rnd;
  boolean initialized;
  private long pad0;
  private long pad1;
  private long pad2;
  private long pad3;
  private long pad4;
  private long pad5;
  private long pad6;
  private long pad7;
  private static final long serialVersionUID = -5851777807851030925L;
  ThreadLocalRandom()
  {
    super(newSeed());
    initialized = true;
  }
  




  public static ThreadLocalRandom current()
  {
    return InternalThreadLocalMap.get().random();
  }
  






  public void setSeed(long seed)
  {
    if (initialized) {
      throw new UnsupportedOperationException();
    }
    rnd = ((seed ^ 0x5DEECE66D) & 0xFFFFFFFFFFFF);
  }
  
  protected int next(int bits)
  {
    rnd = (rnd * 25214903917L + 11L & 0xFFFFFFFFFFFF);
    return (int)(rnd >>> 48 - bits);
  }
  









  public int nextInt(int least, int bound)
  {
    if (least >= bound) {
      throw new IllegalArgumentException();
    }
    return nextInt(bound - least) + least;
  }
  








  public long nextLong(long n)
  {
    ObjectUtil.checkPositive(n, "n");
    





    long offset = 0L;
    while (n >= 2147483647L) {
      int bits = next(2);
      long half = n >>> 1;
      long nextn = (bits & 0x2) == 0 ? half : n - half;
      if ((bits & 0x1) == 0) {
        offset += n - nextn;
      }
      n = nextn;
    }
    return offset + nextInt((int)n);
  }
  









  public long nextLong(long least, long bound)
  {
    if (least >= bound) {
      throw new IllegalArgumentException();
    }
    return nextLong(bound - least) + least;
  }
  








  public double nextDouble(double n)
  {
    ObjectUtil.checkPositive(n, "n");
    return nextDouble() * n;
  }
  









  public double nextDouble(double least, double bound)
  {
    if (least >= bound) {
      throw new IllegalArgumentException();
    }
    return nextDouble() * (bound - least) + least;
  }
}
