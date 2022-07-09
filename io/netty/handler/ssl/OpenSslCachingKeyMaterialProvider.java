package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.net.ssl.X509KeyManager;



















final class OpenSslCachingKeyMaterialProvider
  extends OpenSslKeyMaterialProvider
{
  private final int maxCachedEntries;
  private volatile boolean full;
  private final ConcurrentMap<String, OpenSslKeyMaterial> cache = new ConcurrentHashMap();
  
  OpenSslCachingKeyMaterialProvider(X509KeyManager keyManager, String password, int maxCachedEntries) {
    super(keyManager, password);
    this.maxCachedEntries = maxCachedEntries;
  }
  
  OpenSslKeyMaterial chooseKeyMaterial(ByteBufAllocator allocator, String alias) throws Exception
  {
    OpenSslKeyMaterial material = (OpenSslKeyMaterial)cache.get(alias);
    if (material == null) {
      material = super.chooseKeyMaterial(allocator, alias);
      if (material == null)
      {
        return null;
      }
      
      if (full) {
        return material;
      }
      if (cache.size() > maxCachedEntries) {
        full = true;
        
        return material;
      }
      OpenSslKeyMaterial old = (OpenSslKeyMaterial)cache.putIfAbsent(alias, material);
      if (old != null) {
        material.release();
        material = old;
      }
    }
    
    return material.retain();
  }
  
  void destroy()
  {
    do
    {
      Iterator<OpenSslKeyMaterial> iterator = cache.values().iterator();
      while (iterator.hasNext()) {
        ((OpenSslKeyMaterial)iterator.next()).release();
        iterator.remove();
      }
    } while (!cache.isEmpty());
  }
}
