package io.netty.handler.codec.compression;





final class Bzip2DivSufSort
{
  private static final int STACK_SIZE = 64;
  



  private static final int BUCKET_A_SIZE = 256;
  



  private static final int BUCKET_B_SIZE = 65536;
  



  private static final int SS_BLOCKSIZE = 1024;
  



  private static final int INSERTIONSORT_THRESHOLD = 8;
  


  private static final int[] LOG_2_TABLE = { -1, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 };
  



  private final int[] SA;
  



  private final byte[] T;
  


  private final int n;
  



  Bzip2DivSufSort(byte[] block, int[] bwtBlock, int blockLength)
  {
    T = block;
    SA = bwtBlock;
    n = blockLength;
  }
  
  private static void swapElements(int[] array1, int idx1, int[] array2, int idx2) {
    int temp = array1[idx1];
    array1[idx1] = array2[idx2];
    array2[idx2] = temp;
  }
  
  private int ssCompare(int p1, int p2, int depth) {
    int[] SA = this.SA;
    byte[] T = this.T;
    

    int U1n = SA[(p1 + 1)] + 2;
    int U2n = SA[(p2 + 1)] + 2;
    
    int U1 = depth + SA[p1];
    int U2 = depth + SA[p2];
    
    while ((U1 < U1n) && (U2 < U2n) && (T[U1] == T[U2])) {
      U1++;
      U2++;
    }
    
    return U2 < U2n ? -1 : U1 < U1n ? 1 : U2 < U2n ? (T[U1] & 0xFF) - (T[U2] & 0xFF) : 0;
  }
  

  private int ssCompareLast(int pa, int p1, int p2, int depth, int size)
  {
    int[] SA = this.SA;
    byte[] T = this.T;
    
    int U1 = depth + SA[p1];
    int U2 = depth + SA[p2];
    int U1n = size;
    int U2n = SA[(p2 + 1)] + 2;
    
    while ((U1 < U1n) && (U2 < U2n) && (T[U1] == T[U2])) {
      U1++;
      U2++;
    }
    
    if (U1 < U1n) {
      return U2 < U2n ? (T[U1] & 0xFF) - (T[U2] & 0xFF) : 1;
    }
    if (U2 == U2n) {
      return 1;
    }
    
    U1 %= size;
    U1n = SA[pa] + 2;
    while ((U1 < U1n) && (U2 < U2n) && (T[U1] == T[U2])) {
      U1++;
      U2++;
    }
    
    return U2 < U2n ? -1 : U1 < U1n ? 1 : U2 < U2n ? (T[U1] & 0xFF) - (T[U2] & 0xFF) : 0;
  }
  

  private void ssInsertionSort(int pa, int first, int last, int depth)
  {
    int[] SA = this.SA;
    




    for (int i = last - 2; first <= i; i--) {
      int t = SA[i]; int r; for (int j = i + 1; 0 < (r = ssCompare(pa + t, pa + SA[j], depth));) {
        do {
          SA[(j - 1)] = SA[j];
          j++; } while ((j < last) && (SA[j] < 0));
        if (last <= j) {
          break;
        }
      }
      if (r == 0) {
        SA[j] ^= 0xFFFFFFFF;
      }
      SA[(j - 1)] = t;
    }
  }
  
  private void ssFixdown(int td, int pa, int sa, int i, int size) {
    int[] SA = this.SA;
    byte[] T = this.T;
    




    int v = SA[(sa + i)]; int j; int k; for (int c = T[(td + SA[(pa + v)])] & 0xFF; (j = 2 * i + 1) < size; i = k) {
      int d = T[(td + SA[(pa + SA[(sa + (k = j++))])])] & 0xFF;
      int e; if (d < (e = T[(td + SA[(pa + SA[(sa + j)])])] & 0xFF)) {
        k = j;
        d = e;
      }
      if (d <= c) {
        break;
      }
      SA[(sa + i)] = SA[(sa + k)];
    }
    







    SA[(sa + i)] = v;
  }
  
  private void ssHeapSort(int td, int pa, int sa, int size) {
    int[] SA = this.SA;
    byte[] T = this.T;
    



    int m = size;
    if (size % 2 == 0) {
      m--;
      if ((T[(td + SA[(pa + SA[(sa + m / 2)])])] & 0xFF) < (T[(td + SA[(pa + SA[(sa + m)])])] & 0xFF)) {
        swapElements(SA, sa + m, SA, sa + m / 2);
      }
    }
    
    for (int i = m / 2 - 1; 0 <= i; i--) {
      ssFixdown(td, pa, sa, i, m);
    }
    
    if (size % 2 == 0) {
      swapElements(SA, sa, SA, sa + m);
      ssFixdown(td, pa, sa, 0, m);
    }
    
    for (i = m - 1; 0 < i; i--) {
      int t = SA[sa];
      SA[sa] = SA[(sa + i)];
      ssFixdown(td, pa, sa, 0, i);
      SA[(sa + i)] = t;
    }
  }
  
  private int ssMedian3(int td, int pa, int v1, int v2, int v3) {
    int[] SA = this.SA;
    byte[] T = this.T;
    
    int T_v1 = T[(td + SA[(pa + SA[v1])])] & 0xFF;
    int T_v2 = T[(td + SA[(pa + SA[v2])])] & 0xFF;
    int T_v3 = T[(td + SA[(pa + SA[v3])])] & 0xFF;
    
    if (T_v1 > T_v2) {
      int temp = v1;
      v1 = v2;
      v2 = temp;
      int T_vtemp = T_v1;
      T_v1 = T_v2;
      T_v2 = T_vtemp;
    }
    if (T_v2 > T_v3) {
      if (T_v1 > T_v3) {
        return v1;
      }
      return v3;
    }
    return v2;
  }
  
  private int ssMedian5(int td, int pa, int v1, int v2, int v3, int v4, int v5) {
    int[] SA = this.SA;
    byte[] T = this.T;
    
    int T_v1 = T[(td + SA[(pa + SA[v1])])] & 0xFF;
    int T_v2 = T[(td + SA[(pa + SA[v2])])] & 0xFF;
    int T_v3 = T[(td + SA[(pa + SA[v3])])] & 0xFF;
    int T_v4 = T[(td + SA[(pa + SA[v4])])] & 0xFF;
    int T_v5 = T[(td + SA[(pa + SA[v5])])] & 0xFF;
    


    if (T_v2 > T_v3) {
      int temp = v2;
      v2 = v3;
      v3 = temp;
      int T_vtemp = T_v2;
      T_v2 = T_v3;
      T_v3 = T_vtemp;
    }
    if (T_v4 > T_v5) {
      int temp = v4;
      v4 = v5;
      v5 = temp;
      int T_vtemp = T_v4;
      T_v4 = T_v5;
      T_v5 = T_vtemp;
    }
    if (T_v2 > T_v4) {
      int temp = v2;
      v4 = temp;
      int T_vtemp = T_v2;
      T_v4 = T_vtemp;
      temp = v3;
      v3 = v5;
      v5 = temp;
      T_vtemp = T_v3;
      T_v3 = T_v5;
      T_v5 = T_vtemp;
    }
    if (T_v1 > T_v3) {
      int temp = v1;
      v1 = v3;
      v3 = temp;
      int T_vtemp = T_v1;
      T_v1 = T_v3;
      T_v3 = T_vtemp;
    }
    if (T_v1 > T_v4) {
      int temp = v1;
      v4 = temp;
      int T_vtemp = T_v1;
      T_v4 = T_vtemp;
      v3 = v5;
      T_v3 = T_v5;
    }
    if (T_v3 > T_v4) {
      return v4;
    }
    return v3;
  }
  


  private int ssPivot(int td, int pa, int first, int last)
  {
    int t = last - first;
    int middle = first + t / 2;
    
    if (t <= 512) {
      if (t <= 32) {
        return ssMedian3(td, pa, first, middle, last - 1);
      }
      t >>= 2;
      return ssMedian5(td, pa, first, first + t, middle, last - 1 - t, last - 1);
    }
    t >>= 3;
    return ssMedian3(td, pa, 
    
      ssMedian3(td, pa, first, first + t, first + (t << 1)), 
      ssMedian3(td, pa, middle - t, middle, middle + t), 
      ssMedian3(td, pa, last - 1 - (t << 1), last - 1 - t, last - 1));
  }
  
  private static int ssLog(int n)
  {
    return (n & 0xFF00) != 0 ? 8 + LOG_2_TABLE[(n >> 8 & 0xFF)] : LOG_2_TABLE[(n & 0xFF)];
  }
  

  private int ssSubstringPartition(int pa, int first, int last, int depth)
  {
    int[] SA = this.SA;
    



    int a = first - 1;int b = last;
    for (;;) { a++; if ((a < b) && (SA[(pa + SA[a])] + depth >= SA[(pa + SA[a] + 1)] + 1)) {
        SA[a] ^= 0xFFFFFFFF;
      } else {
        b--;
        while ((a < b) && (SA[(pa + SA[b])] + depth < SA[(pa + SA[b] + 1)] + 1)) {
          b--;
        }
        
        if (b <= a) {
          break;
        }
        int t = SA[b] ^ 0xFFFFFFFF;
        SA[b] = SA[a];
        SA[a] = t;
      } }
    if (first < a) {
      SA[first] ^= 0xFFFFFFFF;
    }
    return a;
  }
  
  private static class StackEntry {
    final int a;
    final int b;
    final int c;
    final int d;
    
    StackEntry(int a, int b, int c, int d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
    }
  }
  
  private void ssMultiKeyIntroSort(int pa, int first, int last, int depth) {
    int[] SA = this.SA;
    byte[] T = this.T;
    
    StackEntry[] stack = new StackEntry[64];
    





    int x = 0;
    
    int ssize = 0;int limit = ssLog(last - first);
    for (;;) { if (last - first <= 8) {
        if (1 < last - first) {
          ssInsertionSort(pa, first, last, depth);
        }
        if (ssize == 0) {
          return;
        }
        StackEntry entry = stack[(--ssize)];
        first = a;
        last = b;
        depth = c;
        limit = d;
      }
      else
      {
        int Td = depth;
        if (limit-- == 0) {
          ssHeapSort(Td, pa, first, last - first);
        }
        if (limit < 0) {
          int a = first + 1; for (int v = T[(Td + SA[(pa + SA[first])])] & 0xFF; a < last; a++) {
            if ((x = T[(Td + SA[(pa + SA[a])])] & 0xFF) != v) {
              if (1 < a - first) {
                break;
              }
              v = x;
              first = a;
            }
          }
          if ((T[(Td + SA[(pa + SA[first])] - 1)] & 0xFF) < v) {
            first = ssSubstringPartition(pa, first, a, depth);
          }
          if (a - first <= last - a) {
            if (1 < a - first) {
              stack[(ssize++)] = new StackEntry(a, last, depth, -1);
              last = a;
              depth++;
              limit = ssLog(a - first);
            } else {
              first = a;
              limit = -1;
            }
          }
          else if (1 < last - a) {
            stack[(ssize++)] = new StackEntry(first, a, depth + 1, ssLog(a - first));
            first = a;
            limit = -1;
          } else {
            last = a;
            depth++;
            limit = ssLog(a - first);
          }
          
        }
        else
        {
          int a = ssPivot(Td, pa, first, last);
          int v = T[(Td + SA[(pa + SA[a])])] & 0xFF;
          swapElements(SA, first, SA, a);
          
          int b = first + 1;
          while ((b < last) && ((x = T[(Td + SA[(pa + SA[b])])] & 0xFF) == v)) {
            b++;
          }
          if (((a = b) < last) && (x < v)) {
            for (;;) { b++; if ((b >= last) || ((x = T[(Td + SA[(pa + SA[b])])] & 0xFF) > v)) break;
              if (x == v) {
                swapElements(SA, b, SA, a);
                a++;
              }
            }
          }
          
          int c = last - 1;
          while ((b < c) && ((x = T[(Td + SA[(pa + SA[c])])] & 0xFF) == v))
            c--;
          int d;
          if ((b < (d = c)) && (x > v)) {
            while ((b < --c) && ((x = T[(Td + SA[(pa + SA[c])])] & 0xFF) >= v)) {
              if (x == v) {
                swapElements(SA, c, SA, d);
                d--;
              }
            }
          }
          for (; b < c; 
              









              goto 752)
          {
            swapElements(SA, b, SA, c);
            for (;;) { b++; if ((b >= c) || ((x = T[(Td + SA[(pa + SA[b])])] & 0xFF) > v)) break;
              if (x == v) {
                swapElements(SA, b, SA, a);
                a++;
              }
            }
            while ((b < --c) && ((x = T[(Td + SA[(pa + SA[c])])] & 0xFF) >= v)) {
              if (x == v) {
                swapElements(SA, c, SA, d);
                d--;
              }
            }
          }
          
          if (a <= d) {
            c = b - 1;
            int s;
            int t; if ((s = a - first) > (t = b - a)) {
              s = t;
            }
            int e = first; for (int f = b - s; 0 < s; f++) {
              swapElements(SA, e, SA, f);s--;e++;
            }
            if ((s = d - c) > (t = last - d - 1)) {
              s = t;
            }
            e = b; for (f = last - s; 0 < s; f++) {
              swapElements(SA, e, SA, f);s--;e++;
            }
            
            a = first + (b - a);
            c = last - (d - c);
            b = v <= (T[(Td + SA[(pa + SA[a])] - 1)] & 0xFF) ? a : ssSubstringPartition(pa, a, c, depth);
            
            if (a - first <= last - c) {
              if (last - c <= c - b) {
                stack[(ssize++)] = new StackEntry(b, c, depth + 1, ssLog(c - b));
                stack[(ssize++)] = new StackEntry(c, last, depth, limit);
                last = a;
              } else if (a - first <= c - b) {
                stack[(ssize++)] = new StackEntry(c, last, depth, limit);
                stack[(ssize++)] = new StackEntry(b, c, depth + 1, ssLog(c - b));
                last = a;
              } else {
                stack[(ssize++)] = new StackEntry(c, last, depth, limit);
                stack[(ssize++)] = new StackEntry(first, a, depth, limit);
                first = b;
                last = c;
                depth++;
                limit = ssLog(c - b);
              }
            }
            else if (a - first <= c - b) {
              stack[(ssize++)] = new StackEntry(b, c, depth + 1, ssLog(c - b));
              stack[(ssize++)] = new StackEntry(first, a, depth, limit);
              first = c;
            } else if (last - c <= c - b) {
              stack[(ssize++)] = new StackEntry(first, a, depth, limit);
              stack[(ssize++)] = new StackEntry(b, c, depth + 1, ssLog(c - b));
              first = c;
            } else {
              stack[(ssize++)] = new StackEntry(first, a, depth, limit);
              stack[(ssize++)] = new StackEntry(c, last, depth, limit);
              first = b;
              last = c;
              depth++;
              limit = ssLog(c - b);
            }
          }
          else {
            limit++;
            if ((T[(Td + SA[(pa + SA[first])] - 1)] & 0xFF) < v) {
              first = ssSubstringPartition(pa, first, last, depth);
              limit = ssLog(last - first);
            }
            depth++;
          }
        }
      }
    }
  }
  
  private static void ssBlockSwap(int[] array1, int first1, int[] array2, int first2, int size)
  {
    int i = size;int a = first1; for (int b = first2; 0 < i; b++) {
      swapElements(array1, a, array2, b);i--;a++;
    }
  }
  
  private void ssMergeForward(int pa, int[] buf, int bufoffset, int first, int middle, int last, int depth)
  {
    int[] SA = this.SA;
    





    int bufend = bufoffset + (middle - first) - 1;
    ssBlockSwap(buf, bufoffset, SA, first, middle - first);
    
    int t = SA[first];int i = first;int j = bufoffset;int k = middle;
    for (;;) { int r = ssCompare(pa + buf[j], pa + SA[k], depth);
      if (r < 0) {
        do {
          SA[(i++)] = buf[j];
          if (bufend <= j) {
            buf[j] = t;
            return;
          }
          buf[(j++)] = SA[i];
        } while (buf[j] < 0);
      } else if (r > 0) {
        do {
          SA[(i++)] = SA[k];
          SA[(k++)] = SA[i];
          if (last <= k) {
            for (; j < bufend; 
                buf[(j++)] = SA[i]) { SA[(i++)] = buf[j];
            }
            SA[i] = buf[j];buf[j] = t;
            return;
          }
        } while (SA[k] < 0);
      } else {
        SA[k] ^= 0xFFFFFFFF;
        do {
          SA[(i++)] = buf[j];
          if (bufend <= j) {
            buf[j] = t;
            return;
          }
          buf[(j++)] = SA[i];
        } while (buf[j] < 0);
        do
        {
          SA[(i++)] = SA[k];
          SA[(k++)] = SA[i];
          if (last <= k) {
            while (j < bufend) {
              SA[(i++)] = buf[j];
              buf[(j++)] = SA[i];
            }
            SA[i] = buf[j];buf[j] = t;
            return;
          }
        } while (SA[k] < 0);
      }
    }
  }
  
  private void ssMergeBackward(int pa, int[] buf, int bufoffset, int first, int middle, int last, int depth)
  {
    int[] SA = this.SA;
    







    int bufend = bufoffset + (last - middle);
    ssBlockSwap(buf, bufoffset, SA, middle, last - middle);
    
    int x = 0;
    int p1; int p1; if (buf[(bufend - 1)] < 0) {
      x |= 0x1;
      p1 = pa + (buf[(bufend - 1)] ^ 0xFFFFFFFF);
    } else {
      p1 = pa + buf[(bufend - 1)]; }
    int p2;
    int p2; if (SA[(middle - 1)] < 0) {
      x |= 0x2;
      p2 = pa + (SA[(middle - 1)] ^ 0xFFFFFFFF);
    } else {
      p2 = pa + SA[(middle - 1)];
    }
    int t = SA[(last - 1)];int i = last - 1;int j = bufend - 1;int k = middle - 1;
    for (;;) {
      int r = ssCompare(p1, p2, depth);
      if (r > 0) {
        if ((x & 0x1) != 0) {
          do {
            SA[(i--)] = buf[j];
            buf[(j--)] = SA[i];
          } while (buf[j] < 0);
          x ^= 0x1;
        }
        SA[(i--)] = buf[j];
        if (j <= bufoffset) {
          buf[j] = t;
          return;
        }
        buf[(j--)] = SA[i];
        
        if (buf[j] < 0) {
          x |= 0x1;
          p1 = pa + (buf[j] ^ 0xFFFFFFFF);
        } else {
          p1 = pa + buf[j];
        }
      } else if (r < 0) {
        if ((x & 0x2) != 0) {
          do {
            SA[(i--)] = SA[k];
            SA[(k--)] = SA[i];
          } while (SA[k] < 0);
          x ^= 0x2;
        }
        SA[(i--)] = SA[k];
        SA[(k--)] = SA[i];
        if (k < first) {
          while (bufoffset < j) {
            SA[(i--)] = buf[j];
            buf[(j--)] = SA[i];
          }
          SA[i] = buf[j];
          buf[j] = t;
          return;
        }
        
        if (SA[k] < 0) {
          x |= 0x2;
          p2 = pa + (SA[k] ^ 0xFFFFFFFF);
        } else {
          p2 = pa + SA[k];
        }
      } else {
        if ((x & 0x1) != 0) {
          do {
            SA[(i--)] = buf[j];
            buf[(j--)] = SA[i];
          } while (buf[j] < 0);
          x ^= 0x1;
        }
        SA[(i--)] = (buf[j] ^ 0xFFFFFFFF);
        if (j <= bufoffset) {
          buf[j] = t;
          return;
        }
        buf[(j--)] = SA[i];
        
        if ((x & 0x2) != 0) {
          do {
            SA[(i--)] = SA[k];
            SA[(k--)] = SA[i];
          } while (SA[k] < 0);
          x ^= 0x2;
        }
        SA[(i--)] = SA[k];
        SA[(k--)] = SA[i];
        if (k < first) {
          while (bufoffset < j) {
            SA[(i--)] = buf[j];
            buf[(j--)] = SA[i];
          }
          SA[i] = buf[j];
          buf[j] = t;
          return;
        }
        
        if (buf[j] < 0) {
          x |= 0x1;
          p1 = pa + (buf[j] ^ 0xFFFFFFFF);
        } else {
          p1 = pa + buf[j];
        }
        if (SA[k] < 0) {
          x |= 0x2;
          p2 = pa + (SA[k] ^ 0xFFFFFFFF);
        } else {
          p2 = pa + SA[k];
        }
      }
    }
  }
  
  private static int getIDX(int a) {
    return 0 <= a ? a : a ^ 0xFFFFFFFF;
  }
  
  private void ssMergeCheckEqual(int pa, int depth, int a) {
    int[] SA = this.SA;
    
    if ((0 <= SA[a]) && (ssCompare(pa + getIDX(SA[(a - 1)]), pa + SA[a], depth) == 0)) {
      SA[a] ^= 0xFFFFFFFF;
    }
  }
  
  private void ssMerge(int pa, int first, int middle, int last, int[] buf, int bufoffset, int bufsize, int depth)
  {
    int[] SA = this.SA;
    
    StackEntry[] stack = new StackEntry[64];
    





    int check = 0;int ssize = 0;
    for (;;) {
      if (last - middle <= bufsize) {
        if ((first < middle) && (middle < last)) {
          ssMergeBackward(pa, buf, bufoffset, first, middle, last, depth);
        }
        
        if ((check & 0x1) != 0) {
          ssMergeCheckEqual(pa, depth, first);
        }
        if ((check & 0x2) != 0) {
          ssMergeCheckEqual(pa, depth, last);
        }
        if (ssize == 0) {
          return;
        }
        StackEntry entry = stack[(--ssize)];
        first = a;
        middle = b;
        last = c;
        check = d;


      }
      else if (middle - first <= bufsize) {
        if (first < middle) {
          ssMergeForward(pa, buf, bufoffset, first, middle, last, depth);
        }
        if ((check & 0x1) != 0) {
          ssMergeCheckEqual(pa, depth, first);
        }
        if ((check & 0x2) != 0) {
          ssMergeCheckEqual(pa, depth, last);
        }
        if (ssize == 0) {
          return;
        }
        StackEntry entry = stack[(--ssize)];
        first = a;
        middle = b;
        last = c;
        check = d;
      }
      else
      {
        int m = 0;int len = Math.min(middle - first, last - middle);int half = len >> 1;
        for (; 0 < len; 
            half >>= 1)
        {
          if (ssCompare(pa + getIDX(SA[(middle + m + half)]), pa + 
            getIDX(SA[(middle - m - half - 1)]), depth) < 0) {
            m += half + 1;
            half -= (len & 0x1 ^ 0x1);
          }
          len = half;
        }
        






        if (0 < m) {
          ssBlockSwap(SA, middle - m, SA, middle, m);
          int j; int i = j = middle;
          int next = 0;
          if (middle + m < last) {
            if (SA[(middle + m)] < 0) {
              while (SA[(i - 1)] < 0) {
                i--;
              }
              SA[(middle + m)] ^= 0xFFFFFFFF;
            }
            for (j = middle; SA[j] < 0;) {
              j++;
            }
            next = 1;
          }
          if (i - first <= last - j) {
            stack[(ssize++)] = new StackEntry(j, middle + m, last, check & 0x2 | next & 0x1);
            middle -= m;
            last = i;
            check &= 0x1;
          } else {
            if ((i == middle) && (middle == j)) {
              next <<= 1;
            }
            stack[(ssize++)] = new StackEntry(first, middle - m, i, check & 0x1 | next & 0x2);
            first = j;
            middle += m;
            check = check & 0x2 | next & 0x1;
          }
        } else {
          if ((check & 0x1) != 0) {
            ssMergeCheckEqual(pa, depth, first);
          }
          ssMergeCheckEqual(pa, depth, middle);
          if ((check & 0x2) != 0) {
            ssMergeCheckEqual(pa, depth, last);
          }
          if (ssize == 0) {
            return;
          }
          StackEntry entry = stack[(--ssize)];
          first = a;
          middle = b;
          last = c;
          check = d;
        }
      }
    }
  }
  
  private void subStringSort(int pa, int first, int last, int[] buf, int bufoffset, int bufsize, int depth, boolean lastsuffix, int size)
  {
    int[] SA = this.SA;
    






    if (lastsuffix) {
      first++;
    }
    int a = first; for (int i = 0; a + 1024 < last; i++) {
      ssMultiKeyIntroSort(pa, a, a + 1024, depth);
      int[] curbuf = SA;
      int curbufoffset = a + 1024;
      int curbufsize = last - (a + 1024);
      if (curbufsize <= bufsize) {
        curbufsize = bufsize;
        curbuf = buf;
        curbufoffset = bufoffset;
      }
      int b = a;int k = 1024; for (int j = i; (j & 0x1) != 0; j >>>= 1) {
        ssMerge(pa, b - k, b, b + k, curbuf, curbufoffset, curbufsize, depth);b -= k;k <<= 1;
      }
      a += 1024;
    }
    












    ssMultiKeyIntroSort(pa, a, last, depth);
    
    for (int k = 1024; i != 0; i >>= 1) {
      if ((i & 0x1) != 0) {
        ssMerge(pa, a - k, a, last, buf, bufoffset, bufsize, depth);
        a -= k;
      }
      k <<= 1;
    }
    




    if (lastsuffix)
    {
      a = first;i = SA[(first - 1)];int r = 1;
      for (; (a < last) && ((SA[a] < 0) || (0 < (r = ssCompareLast(pa, pa + i, pa + SA[a], depth, size)))); 
          a++) {
        SA[(a - 1)] = SA[a];
      }
      if (r == 0) {
        SA[a] ^= 0xFFFFFFFF;
      }
      SA[(a - 1)] = i;
    }
  }
  

  private int trGetC(int isa, int isaD, int isaN, int p)
  {
    return isaD + p < isaN ? SA[(isaD + p)] : SA[(isa + (isaD - isa + p) % (isaN - isa))];
  }
  

  private void trFixdown(int isa, int isaD, int isaN, int sa, int i, int size)
  {
    int[] SA = this.SA;
    




    int v = SA[(sa + i)]; int j; int k; for (int c = trGetC(isa, isaD, isaN, v); (j = 2 * i + 1) < size; i = k) {
      k = j++;
      int d = trGetC(isa, isaD, isaN, SA[(sa + k)]);
      int e; if (d < (e = trGetC(isa, isaD, isaN, SA[(sa + j)]))) {
        k = j;
        d = e;
      }
      if (d <= c) {
        break;
      }
      SA[(sa + i)] = SA[(sa + k)];
    }
    








    SA[(sa + i)] = v;
  }
  
  private void trHeapSort(int isa, int isaD, int isaN, int sa, int size) {
    int[] SA = this.SA;
    



    int m = size;
    if (size % 2 == 0) {
      m--;
      if (trGetC(isa, isaD, isaN, SA[(sa + m / 2)]) < trGetC(isa, isaD, isaN, SA[(sa + m)])) {
        swapElements(SA, sa + m, SA, sa + m / 2);
      }
    }
    
    for (int i = m / 2 - 1; 0 <= i; i--) {
      trFixdown(isa, isaD, isaN, sa, i, m);
    }
    
    if (size % 2 == 0) {
      swapElements(SA, sa, SA, sa + m);
      trFixdown(isa, isaD, isaN, sa, 0, m);
    }
    
    for (i = m - 1; 0 < i; i--) {
      int t = SA[sa];
      SA[sa] = SA[(sa + i)];
      trFixdown(isa, isaD, isaN, sa, 0, i);
      SA[(sa + i)] = t;
    }
  }
  
  private void trInsertionSort(int isa, int isaD, int isaN, int first, int last) {
    int[] SA = this.SA;
    



    for (int a = first + 1; a < last; a++) {
      int t = SA[a]; int r; for (int b = a - 1; 0 > (r = trGetC(isa, isaD, isaN, t) - trGetC(isa, isaD, isaN, SA[b]));) {
        do {
          SA[(b + 1)] = SA[b];
        } while ((first <= --b) && (SA[b] < 0));
        if (b < first) {
          break;
        }
      }
      if (r == 0) {
        SA[b] ^= 0xFFFFFFFF;
      }
      SA[(b + 1)] = t;
    }
  }
  
  private static int trLog(int n) {
    return (n & 0xFF00) != 0 ? 8 + LOG_2_TABLE[(n >> 8 & 0xFF)] : (n & 0xFFFF0000) != 0 ? LOG_2_TABLE[(n >> 16 & 0x10F)] : (n & 0xFF000000) != 0 ? 24 + LOG_2_TABLE[(n >> 24 & 0xFF)] : LOG_2_TABLE[(n & 0xFF)];
  }
  

  private int trMedian3(int isa, int isaD, int isaN, int v1, int v2, int v3)
  {
    int[] SA = this.SA;
    
    int SA_v1 = trGetC(isa, isaD, isaN, SA[v1]);
    int SA_v2 = trGetC(isa, isaD, isaN, SA[v2]);
    int SA_v3 = trGetC(isa, isaD, isaN, SA[v3]);
    
    if (SA_v1 > SA_v2) {
      int temp = v1;
      v1 = v2;
      v2 = temp;
      int SA_vtemp = SA_v1;
      SA_v1 = SA_v2;
      SA_v2 = SA_vtemp;
    }
    if (SA_v2 > SA_v3) {
      if (SA_v1 > SA_v3) {
        return v1;
      }
      return v3;
    }
    
    return v2;
  }
  
  private int trMedian5(int isa, int isaD, int isaN, int v1, int v2, int v3, int v4, int v5) {
    int[] SA = this.SA;
    
    int SA_v1 = trGetC(isa, isaD, isaN, SA[v1]);
    int SA_v2 = trGetC(isa, isaD, isaN, SA[v2]);
    int SA_v3 = trGetC(isa, isaD, isaN, SA[v3]);
    int SA_v4 = trGetC(isa, isaD, isaN, SA[v4]);
    int SA_v5 = trGetC(isa, isaD, isaN, SA[v5]);
    


    if (SA_v2 > SA_v3) {
      int temp = v2;
      v2 = v3;
      v3 = temp;
      int SA_vtemp = SA_v2;
      SA_v2 = SA_v3;
      SA_v3 = SA_vtemp;
    }
    if (SA_v4 > SA_v5) {
      int temp = v4;
      v4 = v5;
      v5 = temp;
      int SA_vtemp = SA_v4;
      SA_v4 = SA_v5;
      SA_v5 = SA_vtemp;
    }
    if (SA_v2 > SA_v4) {
      int temp = v2;
      v4 = temp;
      int SA_vtemp = SA_v2;
      SA_v4 = SA_vtemp;
      temp = v3;
      v3 = v5;
      v5 = temp;
      SA_vtemp = SA_v3;
      SA_v3 = SA_v5;
      SA_v5 = SA_vtemp;
    }
    if (SA_v1 > SA_v3) {
      int temp = v1;
      v1 = v3;
      v3 = temp;
      int SA_vtemp = SA_v1;
      SA_v1 = SA_v3;
      SA_v3 = SA_vtemp;
    }
    if (SA_v1 > SA_v4) {
      int temp = v1;
      v4 = temp;
      int SA_vtemp = SA_v1;
      SA_v4 = SA_vtemp;
      v3 = v5;
      SA_v3 = SA_v5;
    }
    if (SA_v3 > SA_v4) {
      return v4;
    }
    return v3;
  }
  


  private int trPivot(int isa, int isaD, int isaN, int first, int last)
  {
    int t = last - first;
    int middle = first + t / 2;
    
    if (t <= 512) {
      if (t <= 32) {
        return trMedian3(isa, isaD, isaN, first, middle, last - 1);
      }
      t >>= 2;
      return trMedian5(isa, isaD, isaN, first, first + t, middle, last - 1 - t, last - 1);
    }
    




    t >>= 3;
    return trMedian3(isa, isaD, isaN, 
    
      trMedian3(isa, isaD, isaN, first, first + t, first + (t << 1)), 
      trMedian3(isa, isaD, isaN, middle - t, middle, middle + t), 
      trMedian3(isa, isaD, isaN, last - 1 - (t << 1), last - 1 - t, last - 1));
  }
  


  private void lsUpdateGroup(int isa, int first, int last)
  {
    int[] SA = this.SA;
    



    for (int a = first; a < last; a++) {
      if (0 <= SA[a]) {
        int b = a;
        do {
          SA[(isa + SA[a])] = a;
          a++; } while ((a < last) && (0 <= SA[a]));
        SA[b] = (b - a);
        if (last <= a) {
          break;
        }
      }
      int b = a;
      do {
        SA[a] ^= 0xFFFFFFFF;
      } while (SA[(++a)] < 0);
      int t = a;
      do {
        SA[(isa + SA[b])] = t;
        b++; } while (b <= a);
    }
  }
  
  private void lsIntroSort(int isa, int isaD, int isaN, int first, int last) {
    int[] SA = this.SA;
    
    StackEntry[] stack = new StackEntry[64];
    



    int x = 0;
    

    int ssize = 0;int limit = trLog(last - first);
    for (;;) { if (last - first <= 8) {
        if (1 < last - first) {
          trInsertionSort(isa, isaD, isaN, first, last);
          lsUpdateGroup(isa, first, last);
        } else if (last - first == 1) {
          SA[first] = -1;
        }
        if (ssize == 0) {
          return;
        }
        StackEntry entry = stack[(--ssize)];
        first = a;
        last = b;
        limit = c;


      }
      else if (limit-- == 0) {
        trHeapSort(isa, isaD, isaN, first, last - first);
        int b; for (int a = last - 1; first < a; a = b) {
          x = trGetC(isa, isaD, isaN, SA[a]); for (b = a - 1; 
              (first <= b) && (trGetC(isa, isaD, isaN, SA[b]) == x); 
              b--) {
            SA[b] ^= 0xFFFFFFFF;
          }
        }
        lsUpdateGroup(isa, first, last);
        if (ssize == 0) {
          return;
        }
        StackEntry entry = stack[(--ssize)];
        first = a;
        last = b;
        limit = c;
      }
      else
      {
        int a = trPivot(isa, isaD, isaN, first, last);
        swapElements(SA, first, SA, a);
        int v = trGetC(isa, isaD, isaN, SA[first]);
        
        int b = first + 1;
        while ((b < last) && ((x = trGetC(isa, isaD, isaN, SA[b])) == v)) {
          b++;
        }
        if (((a = b) < last) && (x < v)) {
          for (;;) { b++; if ((b >= last) || ((x = trGetC(isa, isaD, isaN, SA[b])) > v)) break;
            if (x == v) {
              swapElements(SA, b, SA, a);
              a++;
            }
          }
        }
        
        int c = last - 1;
        while ((b < c) && ((x = trGetC(isa, isaD, isaN, SA[c])) == v))
          c--;
        int d;
        if ((b < (d = c)) && (x > v)) {
          while ((b < --c) && ((x = trGetC(isa, isaD, isaN, SA[c])) >= v)) {
            if (x == v) {
              swapElements(SA, c, SA, d);
              d--;
            }
          }
        }
        for (; b < c; 
            









            goto 609)
        {
          swapElements(SA, b, SA, c);
          for (;;) { b++; if ((b >= c) || ((x = trGetC(isa, isaD, isaN, SA[b])) > v)) break;
            if (x == v) {
              swapElements(SA, b, SA, a);
              a++;
            }
          }
          while ((b < --c) && ((x = trGetC(isa, isaD, isaN, SA[c])) >= v)) {
            if (x == v) {
              swapElements(SA, c, SA, d);
              d--;
            }
          }
        }
        
        if (a <= d) {
          c = b - 1;
          int s;
          int t; if ((s = a - first) > (t = b - a)) {
            s = t;
          }
          int e = first; for (int f = b - s; 0 < s; f++) {
            swapElements(SA, e, SA, f);s--;e++;
          }
          if ((s = d - c) > (t = last - d - 1)) {
            s = t;
          }
          e = b; for (f = last - s; 0 < s; f++) {
            swapElements(SA, e, SA, f);s--;e++;
          }
          
          a = first + (b - a);
          b = last - (d - c);
          
          c = first; for (v = a - 1; c < a; c++) {
            SA[(isa + SA[c])] = v;
          }
          if (b < last) {
            c = a; for (v = b - 1; c < b; c++) {
              SA[(isa + SA[c])] = v;
            }
          }
          if (b - a == 1) {
            SA[a] = -1;
          }
          
          if (a - first <= last - b) {
            if (first < a) {
              stack[(ssize++)] = new StackEntry(b, last, limit, 0);
              last = a;
            } else {
              first = b;
            }
          }
          else if (b < last) {
            stack[(ssize++)] = new StackEntry(first, a, limit, 0);
            first = b;
          } else {
            last = a;
          }
        }
        else {
          if (ssize == 0) {
            return;
          }
          StackEntry entry = stack[(--ssize)];
          first = a;
          last = b;
          limit = c;
        }
      }
    }
  }
  
  private void lsSort(int isa, int n, int depth) { int[] SA = this.SA;
    




    for (int isaD = isa + depth; -n < SA[0]; isaD += isaD - isa) {
      int first = 0;
      int skip = 0;
      int t;
      do { if ((t = SA[first]) < 0) {
          first -= t;
          skip += t;
        } else {
          if (skip != 0) {
            SA[(first + skip)] = skip;
            skip = 0;
          }
          int last = SA[(isa + t)] + 1;
          lsIntroSort(isa, isaD, isa + n, first, last);
          first = last;
        }
      } while (first < n);
      if (skip != 0) {
        SA[(first + skip)] = skip;
      }
      if (n < isaD - isa) {
        first = 0;
        do {
          if ((t = SA[first]) < 0) {
            first -= t;
          } else {
            int last = SA[(isa + t)] + 1;
            for (int i = first; i < last; i++) {
              SA[(isa + SA[i])] = i;
            }
            first = last;
          }
        } while (first < n);
        break;
      }
    }
  }
  
  private static class PartitionResult
  {
    final int first;
    final int last;
    
    PartitionResult(int first, int last)
    {
      this.first = first;
      this.last = last;
    }
  }
  
  private PartitionResult trPartition(int isa, int isaD, int isaN, int first, int last, int v)
  {
    int[] SA = this.SA;
    


    int x = 0;
    
    int b = first;
    while ((b < last) && ((x = trGetC(isa, isaD, isaN, SA[b])) == v))
      b++;
    int a;
    if (((a = b) < last) && (x < v)) {
      for (;;) { b++; if ((b >= last) || ((x = trGetC(isa, isaD, isaN, SA[b])) > v)) break;
        if (x == v) {
          swapElements(SA, b, SA, a);
          a++;
        }
      }
    }
    
    int c = last - 1;
    while ((b < c) && ((x = trGetC(isa, isaD, isaN, SA[c])) == v))
      c--;
    int d;
    if ((b < (d = c)) && (x > v)) {
      while ((b < --c) && ((x = trGetC(isa, isaD, isaN, SA[c])) >= v)) {
        if (x == v) {
          swapElements(SA, c, SA, d);
          d--;
        }
      }
    }
    for (; b < c; 
        









        goto 299)
    {
      swapElements(SA, b, SA, c);
      for (;;) { b++; if ((b >= c) || ((x = trGetC(isa, isaD, isaN, SA[b])) > v)) break;
        if (x == v) {
          swapElements(SA, b, SA, a);
          a++;
        }
      }
      while ((b < --c) && ((x = trGetC(isa, isaD, isaN, SA[c])) >= v)) {
        if (x == v) {
          swapElements(SA, c, SA, d);
          d--;
        }
      }
    }
    
    if (a <= d) {
      c = b - 1;
      int s; int t; if ((s = a - first) > (t = b - a)) {
        s = t;
      }
      int e = first; for (int f = b - s; 0 < s; f++) {
        swapElements(SA, e, SA, f);s--;e++;
      }
      if ((s = d - c) > (t = last - d - 1)) {
        s = t;
      }
      e = b; for (f = last - s; 0 < s; f++) {
        swapElements(SA, e, SA, f);s--;e++;
      }
      first += b - a;
      last -= d - c;
    }
    return new PartitionResult(first, last);
  }
  
  private void trCopy(int isa, int isaN, int first, int a, int b, int last, int depth)
  {
    int[] SA = this.SA;
    



    int v = b - 1;
    
    int c = first; for (int d = a - 1; c <= d; c++) { int s;
      if ((s = SA[c] - depth) < 0) {
        s += isaN - isa;
      }
      if (SA[(isa + s)] == v) {
        SA[(++d)] = s;
        SA[(isa + s)] = d;
      }
    }
    c = last - 1;int e = d + 1; for (d = b; e < d; c--) { int s;
      if ((s = SA[c] - depth) < 0) {
        s += isaN - isa;
      }
      if (SA[(isa + s)] == v) {
        SA[(--d)] = s;
        SA[(isa + s)] = d;
      }
    }
  }
  
  private void trIntroSort(int isa, int isaD, int isaN, int first, int last, TRBudget budget, int size)
  {
    int[] SA = this.SA;
    
    StackEntry[] stack = new StackEntry[64];
    


    int x = 0;
    


    int ssize = 0;int limit = trLog(last - first);
    for (;;) { if (limit < 0) {
        if (limit == -1) {
          if (!budget.update(size, last - first)) {
            break;
          }
          PartitionResult result = trPartition(isa, isaD - 1, isaN, first, last, last - 1);
          int a = first;
          int b = last;
          if ((first < a) || (b < last)) {
            if (a < last) {
              int c = first; for (int v = a - 1; c < a; c++) {
                SA[(isa + SA[c])] = v;
              }
            }
            if (b < last) {
              int c = a; for (int v = b - 1; c < b; c++) {
                SA[(isa + SA[c])] = v;
              }
            }
            
            stack[(ssize++)] = new StackEntry(0, a, b, 0);
            stack[(ssize++)] = new StackEntry(isaD - 1, first, last, -2);
            if (a - first <= last - b) {
              if (1 < a - first) {
                stack[(ssize++)] = new StackEntry(isaD, b, last, trLog(last - b));
                last = a;limit = trLog(a - first);
              } else if (1 < last - b) {
                first = b;limit = trLog(last - b);
              } else {
                if (ssize == 0) {
                  return;
                }
                StackEntry entry = stack[(--ssize)];
                isaD = a;
                first = b;
                last = c;
                limit = d;
              }
            }
            else if (1 < last - b) {
              stack[(ssize++)] = new StackEntry(isaD, first, a, trLog(a - first));
              first = b;
              limit = trLog(last - b);
            } else if (1 < a - first) {
              last = a;
              limit = trLog(a - first);
            } else {
              if (ssize == 0) {
                return;
              }
              StackEntry entry = stack[(--ssize)];
              isaD = a;
              first = b;
              last = c;
              limit = d;
            }
          }
          else {
            for (int c = first; c < last; c++) {
              SA[(isa + SA[c])] = c;
            }
            if (ssize == 0) {
              return;
            }
            StackEntry entry = stack[(--ssize)];
            isaD = a;
            first = b;
            last = c;
            limit = d;
          }
        } else if (limit == -2) {
          int a = b;
          int b = c;
          trCopy(isa, isaN, first, a, b, last, isaD - isa);
          if (ssize == 0) {
            return;
          }
          StackEntry entry = stack[(--ssize)];
          isaD = a;
          first = b;
          last = c;
          limit = d;
        } else {
          if (0 <= SA[first]) {
            int a = first;
            do {
              SA[(isa + SA[a])] = a;
              a++; } while ((a < last) && (0 <= SA[a]));
            first = a;
          }
          if (first < last) {
            int a = first;
            do {
              SA[a] ^= 0xFFFFFFFF;
            } while (SA[(++a)] < 0);
            int next = SA[(isa + SA[a])] != SA[(isaD + SA[a])] ? trLog(a - first + 1) : -1;
            a++; if (a < last) {
              int b = first; for (int v = a - 1; b < a; b++) {
                SA[(isa + SA[b])] = v;
              }
            }
            
            if (a - first <= last - a) {
              stack[(ssize++)] = new StackEntry(isaD, a, last, -3);
              isaD++;last = a;limit = next;
            }
            else if (1 < last - a) {
              stack[(ssize++)] = new StackEntry(isaD + 1, first, a, next);
              first = a;limit = -3;
            } else {
              isaD++;last = a;limit = next;
            }
          }
          else {
            if (ssize == 0) {
              return;
            }
            StackEntry entry = stack[(--ssize)];
            isaD = a;
            first = b;
            last = c;
            limit = d;
          }
          
        }
        
      }
      else if (last - first <= 8) {
        if (!budget.update(size, last - first)) {
          break;
        }
        trInsertionSort(isa, isaD, isaN, first, last);
        limit = -3;


      }
      else if (limit-- == 0) {
        if (!budget.update(size, last - first)) {
          break;
        }
        trHeapSort(isa, isaD, isaN, first, last - first);
        int b; for (int a = last - 1; first < a; a = b) {
          x = trGetC(isa, isaD, isaN, SA[a]); for (b = a - 1; 
              (first <= b) && (trGetC(isa, isaD, isaN, SA[b]) == x); 
              b--) {
            SA[b] ^= 0xFFFFFFFF;
          }
        }
        limit = -3;
      }
      else
      {
        int a = trPivot(isa, isaD, isaN, first, last);
        
        swapElements(SA, first, SA, a);
        int v = trGetC(isa, isaD, isaN, SA[first]);
        
        int b = first + 1;
        while ((b < last) && ((x = trGetC(isa, isaD, isaN, SA[b])) == v)) {
          b++;
        }
        if (((a = b) < last) && (x < v)) {
          for (;;) { b++; if ((b >= last) || ((x = trGetC(isa, isaD, isaN, SA[b])) > v)) break;
            if (x == v) {
              swapElements(SA, b, SA, a);
              a++;
            }
          }
        }
        
        int c = last - 1;
        while ((b < c) && ((x = trGetC(isa, isaD, isaN, SA[c])) == v))
          c--;
        int d;
        if ((b < (d = c)) && (x > v)) {
          while ((b < --c) && ((x = trGetC(isa, isaD, isaN, SA[c])) >= v)) {
            if (x == v) {
              swapElements(SA, c, SA, d);
              d--;
            }
          }
        }
        for (; b < c; 
            









            goto 1491)
        {
          swapElements(SA, b, SA, c);
          for (;;) { b++; if ((b >= c) || ((x = trGetC(isa, isaD, isaN, SA[b])) > v)) break;
            if (x == v) {
              swapElements(SA, b, SA, a);
              a++;
            }
          }
          while ((b < --c) && ((x = trGetC(isa, isaD, isaN, SA[c])) >= v)) {
            if (x == v) {
              swapElements(SA, c, SA, d);
              d--;
            }
          }
        }
        
        if (a <= d) {
          c = b - 1;
          int s;
          int t; if ((s = a - first) > (t = b - a)) {
            s = t;
          }
          int e = first; for (int f = b - s; 0 < s; f++) {
            swapElements(SA, e, SA, f);s--;e++;
          }
          if ((s = d - c) > (t = last - d - 1)) {
            s = t;
          }
          e = b; for (f = last - s; 0 < s; f++) {
            swapElements(SA, e, SA, f);s--;e++;
          }
          
          a = first + (b - a);
          b = last - (d - c);
          int next = SA[(isa + SA[a])] != v ? trLog(b - a) : -1;
          
          c = first; for (v = a - 1; c < a; c++) {
            SA[(isa + SA[c])] = v;
          }
          if (b < last) {
            c = a; for (v = b - 1; c < b; c++) {
              SA[(isa + SA[c])] = v;
            }
          }
          if (a - first <= last - b) {
            if (last - b <= b - a) {
              if (1 < a - first) {
                stack[(ssize++)] = new StackEntry(isaD + 1, a, b, next);
                stack[(ssize++)] = new StackEntry(isaD, b, last, limit);
                last = a;
              } else if (1 < last - b) {
                stack[(ssize++)] = new StackEntry(isaD + 1, a, b, next);
                first = b;
              } else if (1 < b - a) {
                isaD++;
                first = a;
                last = b;
                limit = next;
              } else {
                if (ssize == 0) {
                  return;
                }
                StackEntry entry = stack[(--ssize)];
                isaD = a;
                first = b;
                last = c;
                limit = d;
              }
            } else if (a - first <= b - a) {
              if (1 < a - first) {
                stack[(ssize++)] = new StackEntry(isaD, b, last, limit);
                stack[(ssize++)] = new StackEntry(isaD + 1, a, b, next);
                last = a;
              } else if (1 < b - a) {
                stack[(ssize++)] = new StackEntry(isaD, b, last, limit);
                isaD++;
                first = a;
                last = b;
                limit = next;
              } else {
                first = b;
              }
            }
            else if (1 < b - a) {
              stack[(ssize++)] = new StackEntry(isaD, b, last, limit);
              stack[(ssize++)] = new StackEntry(isaD, first, a, limit);
              isaD++;
              first = a;
              last = b;
              limit = next;
            } else {
              stack[(ssize++)] = new StackEntry(isaD, b, last, limit);
              last = a;
            }
            
          }
          else if (a - first <= b - a) {
            if (1 < last - b) {
              stack[(ssize++)] = new StackEntry(isaD + 1, a, b, next);
              stack[(ssize++)] = new StackEntry(isaD, first, a, limit);
              first = b;
            } else if (1 < a - first) {
              stack[(ssize++)] = new StackEntry(isaD + 1, a, b, next);
              last = a;
            } else if (1 < b - a) {
              isaD++;
              first = a;
              last = b;
              limit = next;
            } else {
              stack[(ssize++)] = new StackEntry(isaD, first, last, limit);
            }
          } else if (last - b <= b - a) {
            if (1 < last - b) {
              stack[(ssize++)] = new StackEntry(isaD, first, a, limit);
              stack[(ssize++)] = new StackEntry(isaD + 1, a, b, next);
              first = b;
            } else if (1 < b - a) {
              stack[(ssize++)] = new StackEntry(isaD, first, a, limit);
              isaD++;
              first = a;
              last = b;
              limit = next;
            } else {
              last = a;
            }
          }
          else if (1 < b - a) {
            stack[(ssize++)] = new StackEntry(isaD, first, a, limit);
            stack[(ssize++)] = new StackEntry(isaD, b, last, limit);
            isaD++;
            first = a;
            last = b;
            limit = next;
          } else {
            stack[(ssize++)] = new StackEntry(isaD, first, a, limit);
            first = b;
          }
        }
        else
        {
          if (!budget.update(size, last - first)) {
            break;
          }
          limit++;isaD++;
        }
      }
    }
    for (int s = 0; s < ssize; s++) {
      if (d == -3) {
        lsUpdateGroup(isa, b, c);
      }
    }
  }
  
  private static class TRBudget {
    int budget;
    int chance;
    
    TRBudget(int budget, int chance) {
      this.budget = budget;
      this.chance = chance;
    }
    
    boolean update(int size, int n) {
      budget -= n;
      if (budget <= 0) {
        if (--chance == 0) {
          return false;
        }
        budget += size;
      }
      return true;
    }
  }
  
  private void trSort(int isa, int n, int depth) {
    int[] SA = this.SA;
    
    int first = 0;
    

    if (-n < SA[0]) {
      TRBudget budget = new TRBudget(n, trLog(n) * 2 / 3 + 1);
      do { int t;
        if ((t = SA[first]) < 0) {
          first -= t;
        } else {
          int last = SA[(isa + t)] + 1;
          if (1 < last - first) {
            trIntroSort(isa, isa + depth, isa + n, first, last, budget, n);
            if (chance == 0)
            {
              if (0 < first) {
                SA[0] = (-first);
              }
              lsSort(isa, n, depth);
              break;
            }
          }
          first = last;
        }
      } while (first < n);
    }
  }
  

  private static int BUCKET_B(int c0, int c1)
  {
    return c1 << 8 | c0;
  }
  
  private static int BUCKET_BSTAR(int c0, int c1) {
    return c0 << 8 | c1;
  }
  
  private int sortTypeBstar(int[] bucketA, int[] bucketB) {
    byte[] T = this.T;
    int[] SA = this.SA;
    int n = this.n;
    int[] tempbuf = new int['Ā'];
    






    int i = 1; for (int flag = 1; i < n; i++) {
      if (T[(i - 1)] != T[i]) {
        if ((T[(i - 1)] & 0xFF) <= (T[i] & 0xFF)) break;
        flag = 0; break;
      }
    }
    

    i = n - 1;
    int m = n;
    int ti;
    int t0;
    if (((ti = T[i] & 0xFF) < (t0 = T[0] & 0xFF)) || ((T[i] == T[0]) && (flag != 0))) {
      if (flag == 0) {
        bucketB[BUCKET_BSTAR(ti, t0)] += 1;
        SA[(--m)] = i;
      } else {
        bucketB[BUCKET_B(ti, t0)] += 1; }
      int ti1;
      for (i--; (0 <= i) && ((ti = T[i] & 0xFF) <= (ti1 = T[(i + 1)] & 0xFF)); i--) {
        bucketB[BUCKET_B(ti, ti1)] += 1;
      }
    }
    
    while (0 <= i) {
      do {
        bucketA[(T[i] & 0xFF)] += 1;
      } while ((0 <= --i) && ((T[i] & 0xFF) >= (T[(i + 1)] & 0xFF)));
      if (0 <= i) {
        bucketB[BUCKET_BSTAR(T[i] & 0xFF, T[(i + 1)] & 0xFF)] += 1;
        SA[(--m)] = i;
        int ti1; for (i--; (0 <= i) && ((ti = T[i] & 0xFF) <= (ti1 = T[(i + 1)] & 0xFF)); i--) {
          bucketB[BUCKET_B(ti, ti1)] += 1;
        }
      }
    }
    m = n - m;
    if (m == 0) {
      for (i = 0; i < n; i++) {
        SA[i] = i;
      }
      return 0;
    }
    
    int c0 = 0;i = -1; for (int j = 0; c0 < 256; c0++) {
      int t = i + bucketA[c0];
      bucketA[c0] = (i + j);
      i = t + bucketB[BUCKET_B(c0, c0)];
      for (int c1 = c0 + 1; c1 < 256; c1++) {
        j += bucketB[BUCKET_BSTAR(c0, c1)];
        bucketB[(c0 << 8 | c1)] = j;
        i += bucketB[BUCKET_B(c0, c1)];
      }
    }
    
    int PAb = n - m;
    int ISAb = m;
    for (i = m - 2; 0 <= i; i--) {
      int t = SA[(PAb + i)];
      c0 = T[t] & 0xFF;
      int c1 = T[(t + 1)] & 0xFF; int 
        tmp596_593 = BUCKET_BSTAR(c0, c1); int[] tmp596_588 = bucketB; int tmp600_599 = (tmp596_588[tmp596_593] - 1);tmp596_588[tmp596_593] = tmp600_599;SA[tmp600_599] = i;
    }
    int t = SA[(PAb + m - 1)];
    c0 = T[t] & 0xFF;
    int c1 = T[(t + 1)] & 0xFF; int 
      tmp655_652 = BUCKET_BSTAR(c0, c1); int[] tmp655_647 = bucketB; int tmp659_658 = (tmp655_647[tmp655_652] - 1);tmp655_647[tmp655_652] = tmp659_658;SA[tmp659_658] = (m - 1);
    
    int[] buf = SA;
    int bufoffset = m;
    int bufsize = n - 2 * m;
    if (bufsize <= 256) {
      buf = tempbuf;
      bufoffset = 0;
      bufsize = 256;
    }
    
    c0 = 255; for (j = m; 0 < j; c0--) {
      for (c1 = 255; c0 < c1; c1--) {
        i = bucketB[BUCKET_BSTAR(c0, c1)];
        if (1 < j - i) {
          subStringSort(PAb, i, j, buf, bufoffset, bufsize, 2, SA[i] == m - 1, n);
        }
        j = i;
      }
    }
    




    for (i = m - 1; 0 <= i; i--) {
      if (0 <= SA[i]) {
        j = i;
        do {
          SA[(ISAb + SA[i])] = i;
        } while ((0 <= --i) && (0 <= SA[i]));
        SA[(i + 1)] = (i - j);
        if (i <= 0) {
          break;
        }
      }
      j = i;
      do {
        int tmp897_896 = (SA[i] ^ 0xFFFFFFFF);SA[i] = tmp897_896;SA[(ISAb + tmp897_896)] = j;
      } while (SA[(--i)] < 0);
      SA[(ISAb + SA[i])] = j;
    }
    
    trSort(ISAb, m, 1);
    
    i = n - 1;j = m;
    if (((T[i] & 0xFF) < (T[0] & 0xFF)) || ((T[i] == T[0]) && (flag != 0))) {
      if (flag == 0) {
        SA[SA[(ISAb + --j)]] = i;
      }
      for (i--; (0 <= i) && ((T[i] & 0xFF) <= (T[(i + 1)] & 0xFF));) {
        i--;
      }
    }
    while (0 <= i) {
      for (i--; (0 <= i) && ((T[i] & 0xFF) >= (T[(i + 1)] & 0xFF));) {
        i--;
      }
      if (0 <= i) {
        SA[SA[(ISAb + --j)]] = i;
        for (i--; (0 <= i) && ((T[i] & 0xFF) <= (T[(i + 1)] & 0xFF));) {
          i--;
        }
      }
    }
    
    c0 = 255;i = n - 1; for (int k = m - 1; 0 <= c0; c0--) {
      for (c1 = 255; c0 < c1; c1--) {
        t = i - bucketB[BUCKET_B(c0, c1)];
        bucketB[BUCKET_B(c0, c1)] = (i + 1);
        
        i = t; for (j = bucketB[BUCKET_BSTAR(c0, c1)]; j <= k; k--) {
          SA[i] = SA[k];i--;
        }
      }
      t = i - bucketB[BUCKET_B(c0, c0)];
      bucketB[BUCKET_B(c0, c0)] = (i + 1);
      if (c0 < 255) {
        bucketB[BUCKET_BSTAR(c0, c0 + 1)] = (t + 1);
      }
      i = bucketA[c0];
    }
    return m;
  }
  
  private int constructBWT(int[] bucketA, int[] bucketB) {
    byte[] T = this.T;
    int[] SA = this.SA;
    int n = this.n;
    
    int t = 0;
    
    int c2 = 0;
    int orig = -1;
    
    for (int c1 = 254; 0 <= c1; c1--) {
      int i = bucketB[BUCKET_BSTAR(c1, c1 + 1)];int j = bucketA[(c1 + 1)];t = 0;c2 = -1;
      for (; i <= j; 
          j--) { int s;
        int s1; if (0 <= (s1 = s = SA[j])) {
          s--; if (s < 0)
            s = n - 1;
          int c0;
          if ((c0 = T[s] & 0xFF) <= c1) {
            SA[j] = (s1 ^ 0xFFFFFFFF);
            if ((0 < s) && ((T[(s - 1)] & 0xFF) > c0)) {
              s ^= 0xFFFFFFFF;
            }
            if (c2 == c0) {
              SA[(--t)] = s;
            } else {
              if (0 <= c2) {
                bucketB[BUCKET_B(c2, c1)] = t;
              }
              int tmp205_204 = (bucketB[BUCKET_B(c2 = c0, c1)] - 1);t = tmp205_204;SA[tmp205_204] = s;
            }
          }
        } else {
          SA[j] = (s ^ 0xFFFFFFFF);
        }
      }
    }
    
    for (int i = 0; i < n; i++) { int s;
      int s1; if (0 <= (s1 = s = SA[i])) {
        s--; if (s < 0)
          s = n - 1;
        int c0;
        if ((c0 = T[s] & 0xFF) >= (T[(s + 1)] & 0xFF)) {
          if ((0 < s) && ((T[(s - 1)] & 0xFF) < c0)) {
            s ^= 0xFFFFFFFF;
          }
          if (c0 == c2) {
            SA[(++t)] = s;
          } else {
            if (c2 != -1) {
              bucketA[c2] = t;
            }
            int tmp368_367 = (bucketA[(c2 = c0)] + 1);t = tmp368_367;SA[tmp368_367] = s;
          }
        }
      } else {
        s1 ^= 0xFFFFFFFF;
      }
      
      if (s1 == 0) {
        SA[i] = T[(n - 1)];
        orig = i;
      } else {
        SA[i] = T[(s1 - 1)];
      }
    }
    return orig;
  }
  



  public int bwt()
  {
    int[] SA = this.SA;
    byte[] T = this.T;
    int n = this.n;
    
    int[] bucketA = new int['Ā'];
    int[] bucketB = new int[65536];
    
    if (n == 0) {
      return 0;
    }
    if (n == 1) {
      SA[0] = T[0];
      return 0;
    }
    
    int m = sortTypeBstar(bucketA, bucketB);
    if (0 < m) {
      return constructBWT(bucketA, bucketB);
    }
    return 0;
  }
}
