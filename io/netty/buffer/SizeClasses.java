package io.netty.buffer;




abstract class SizeClasses
  implements SizeClassesMetric
{
  static final int LOG2_QUANTUM = 4;
  


  private static final int LOG2_SIZE_CLASS_GROUP = 2;
  


  private static final int LOG2_MAX_LOOKUP_SIZE = 12;
  


  private static final int INDEX_IDX = 0;
  


  private static final int LOG2GROUP_IDX = 1;
  


  private static final int LOG2DELTA_IDX = 2;
  


  private static final int NDELTA_IDX = 3;
  


  private static final int PAGESIZE_IDX = 4;
  


  private static final int SUBPAGE_IDX = 5;
  


  private static final int LOG2_DELTA_LOOKUP_IDX = 6;
  


  private static final byte no = 0;
  


  private static final byte yes = 1;
  


  protected final int pageSize;
  

  protected final int pageShifts;
  

  protected final int chunkSize;
  

  protected final int directMemoryCacheAlignment;
  

  final int nSizes;
  

  int nSubpages;
  

  int nPSizes;
  

  int smallMaxSizeIdx;
  

  private int lookupMaxSize;
  

  private final short[][] sizeClasses;
  

  private final int[] pageIdx2sizeTab;
  

  private final int[] sizeIdx2sizeTab;
  

  private final int[] size2idxTab;
  


  protected SizeClasses(int pageSize, int pageShifts, int chunkSize, int directMemoryCacheAlignment)
  {
    this.pageSize = pageSize;
    this.pageShifts = pageShifts;
    this.chunkSize = chunkSize;
    this.directMemoryCacheAlignment = directMemoryCacheAlignment;
    
    int group = PoolThreadCache.log2(chunkSize) + 1 - 4;
    


    sizeClasses = new short[group << 2][7];
    nSizes = sizeClasses();
    

    sizeIdx2sizeTab = new int[nSizes];
    pageIdx2sizeTab = new int[nPSizes];
    idx2SizeTab(sizeIdx2sizeTab, pageIdx2sizeTab);
    
    size2idxTab = new int[lookupMaxSize >> 4];
    size2idxTab(size2idxTab);
  }
  























  private int sizeClasses()
  {
    int normalMaxSize = -1;
    
    int index = 0;
    int size = 0;
    
    int log2Group = 4;
    int log2Delta = 4;
    int ndeltaLimit = 4;
    


    int nDelta = 0;
    while (nDelta < ndeltaLimit) {
      size = sizeClass(index++, log2Group, log2Delta, nDelta++);
    }
    log2Group += 2;
    

    while (size < chunkSize) {
      nDelta = 1;
      
      while ((nDelta <= ndeltaLimit) && (size < chunkSize)) {
        size = sizeClass(index++, log2Group, log2Delta, nDelta++);
        normalMaxSize = size;
      }
      
      log2Group++;
      log2Delta++;
    }
    

    assert (chunkSize == normalMaxSize);
    

    return index;
  }
  
  private int sizeClass(int index, int log2Group, int log2Delta, int nDelta) {
    short isMultiPageSize;
    short isMultiPageSize;
    if (log2Delta >= pageShifts) {
      isMultiPageSize = 1;
    } else {
      int pageSize = 1 << pageShifts;
      int size = (1 << log2Group) + (1 << log2Delta) * nDelta;
      
      isMultiPageSize = size == size / pageSize * pageSize ? 1 : 0;
    }
    
    int log2Ndelta = nDelta == 0 ? 0 : PoolThreadCache.log2(nDelta);
    
    byte remove = 1 << log2Ndelta < nDelta ? 1 : 0;
    
    int log2Size = log2Delta + log2Ndelta == log2Group ? log2Group + 1 : log2Group;
    if (log2Size == log2Group) {
      remove = 1;
    }
    
    short isSubpage = log2Size < pageShifts + 2 ? 1 : 0;
    
    int log2DeltaLookup = (log2Size < 12) || ((log2Size == 12) && (remove == 0)) ? log2Delta : 0;
    


    short[] sz = { (short)index, (short)log2Group, (short)log2Delta, (short)nDelta, isMultiPageSize, isSubpage, (short)log2DeltaLookup };
    



    sizeClasses[index] = sz;
    int size = (1 << log2Group) + (nDelta << log2Delta);
    
    if (sz[4] == 1) {
      nPSizes += 1;
    }
    if (sz[5] == 1) {
      nSubpages += 1;
      smallMaxSizeIdx = index;
    }
    if (sz[6] != 0) {
      lookupMaxSize = size;
    }
    return size;
  }
  
  private void idx2SizeTab(int[] sizeIdx2sizeTab, int[] pageIdx2sizeTab) {
    int pageIdx = 0;
    
    for (int i = 0; i < nSizes; i++) {
      short[] sizeClass = sizeClasses[i];
      int log2Group = sizeClass[1];
      int log2Delta = sizeClass[2];
      int nDelta = sizeClass[3];
      
      int size = (1 << log2Group) + (nDelta << log2Delta);
      sizeIdx2sizeTab[i] = size;
      
      if (sizeClass[4] == 1) {
        pageIdx2sizeTab[(pageIdx++)] = size;
      }
    }
  }
  
  private void size2idxTab(int[] size2idxTab) {
    int idx = 0;
    int size = 0;
    
    for (int i = 0; size <= lookupMaxSize; i++) {
      int log2Delta = sizeClasses[i][2];
      int times = 1 << log2Delta - 4;
      
      while ((size <= lookupMaxSize) && (times-- > 0)) {
        size2idxTab[(idx++)] = i;
        size = idx + 1 << 4;
      }
    }
  }
  
  public int sizeIdx2size(int sizeIdx)
  {
    return sizeIdx2sizeTab[sizeIdx];
  }
  
  public int sizeIdx2sizeCompute(int sizeIdx)
  {
    int group = sizeIdx >> 2;
    int mod = sizeIdx & 0x3;
    
    int groupSize = group == 0 ? 0 : 32 << group;
    

    int shift = group == 0 ? 1 : group;
    int lgDelta = shift + 4 - 1;
    int modSize = mod + 1 << lgDelta;
    
    return groupSize + modSize;
  }
  
  public long pageIdx2size(int pageIdx)
  {
    return pageIdx2sizeTab[pageIdx];
  }
  
  public long pageIdx2sizeCompute(int pageIdx)
  {
    int group = pageIdx >> 2;
    int mod = pageIdx & 0x3;
    
    long groupSize = group == 0 ? 0L : 1L << pageShifts + 2 - 1 << group;
    

    int shift = group == 0 ? 1 : group;
    int log2Delta = shift + pageShifts - 1;
    int modSize = mod + 1 << log2Delta;
    
    return groupSize + modSize;
  }
  
  public int size2SizeIdx(int size)
  {
    if (size == 0) {
      return 0;
    }
    if (size > chunkSize) {
      return nSizes;
    }
    
    if (directMemoryCacheAlignment > 0) {
      size = alignSize(size);
    }
    
    if (size <= lookupMaxSize)
    {
      return size2idxTab[(size - 1 >> 4)];
    }
    
    int x = PoolThreadCache.log2((size << 1) - 1);
    int shift = x < 7 ? 0 : x - 6;
    

    int group = shift << 2;
    
    int log2Delta = x < 7 ? 4 : x - 2 - 1;
    

    int deltaInverseMask = -1 << log2Delta;
    int mod = (size - 1 & deltaInverseMask) >> log2Delta & 0x3;
    

    return group + mod;
  }
  
  public int pages2pageIdx(int pages)
  {
    return pages2pageIdxCompute(pages, false);
  }
  
  public int pages2pageIdxFloor(int pages)
  {
    return pages2pageIdxCompute(pages, true);
  }
  
  private int pages2pageIdxCompute(int pages, boolean floor) {
    int pageSize = pages << pageShifts;
    if (pageSize > chunkSize) {
      return nPSizes;
    }
    
    int x = PoolThreadCache.log2((pageSize << 1) - 1);
    
    int shift = x < 2 + pageShifts ? 0 : x - (2 + pageShifts);
    

    int group = shift << 2;
    
    int log2Delta = x < 2 + pageShifts + 1 ? pageShifts : x - 2 - 1;
    

    int deltaInverseMask = -1 << log2Delta;
    int mod = (pageSize - 1 & deltaInverseMask) >> log2Delta & 0x3;
    

    int pageIdx = group + mod;
    
    if ((floor) && (pageIdx2sizeTab[pageIdx] > pages << pageShifts)) {
      pageIdx--;
    }
    
    return pageIdx;
  }
  
  private int alignSize(int size)
  {
    int delta = size & directMemoryCacheAlignment - 1;
    return delta == 0 ? size : size + directMemoryCacheAlignment - delta;
  }
  
  public int normalizeSize(int size)
  {
    if (size == 0) {
      return sizeIdx2sizeTab[0];
    }
    if (directMemoryCacheAlignment > 0) {
      size = alignSize(size);
    }
    
    if (size <= lookupMaxSize) {
      int ret = sizeIdx2sizeTab[size2idxTab[(size - 1 >> 4)]];
      assert (ret == normalizeSizeCompute(size));
      return ret;
    }
    return normalizeSizeCompute(size);
  }
  
  private static int normalizeSizeCompute(int size) {
    int x = PoolThreadCache.log2((size << 1) - 1);
    int log2Delta = x < 7 ? 4 : x - 2 - 1;
    
    int delta = 1 << log2Delta;
    int delta_mask = delta - 1;
    return size + delta_mask & (delta_mask ^ 0xFFFFFFFF);
  }
}
