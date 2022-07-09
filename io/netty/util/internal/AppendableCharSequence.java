package io.netty.util.internal;

import java.util.Arrays;
















public final class AppendableCharSequence
  implements CharSequence, Appendable
{
  private char[] chars;
  private int pos;
  
  public AppendableCharSequence(int length)
  {
    chars = new char[ObjectUtil.checkPositive(length, "length")];
  }
  
  private AppendableCharSequence(char[] chars) {
    this.chars = ObjectUtil.checkNonEmpty(chars, "chars");
    pos = chars.length;
  }
  
  public void setLength(int length) {
    if ((length < 0) || (length > pos)) {
      throw new IllegalArgumentException("length: " + length + " (length: >= 0, <= " + pos + ')');
    }
    pos = length;
  }
  
  public int length()
  {
    return pos;
  }
  
  public char charAt(int index)
  {
    if (index > pos) {
      throw new IndexOutOfBoundsException();
    }
    return chars[index];
  }
  






  public char charAtUnsafe(int index)
  {
    return chars[index];
  }
  
  public AppendableCharSequence subSequence(int start, int end)
  {
    if (start == end)
    {


      return new AppendableCharSequence(Math.min(16, chars.length));
    }
    return new AppendableCharSequence(Arrays.copyOfRange(chars, start, end));
  }
  
  public AppendableCharSequence append(char c)
  {
    if (pos == chars.length) {
      char[] old = chars;
      chars = new char[old.length << 1];
      System.arraycopy(old, 0, chars, 0, old.length);
    }
    chars[(pos++)] = c;
    return this;
  }
  
  public AppendableCharSequence append(CharSequence csq)
  {
    return append(csq, 0, csq.length());
  }
  
  public AppendableCharSequence append(CharSequence csq, int start, int end)
  {
    if (csq.length() < end)
    {
      throw new IndexOutOfBoundsException("expected: csq.length() >= (" + end + "),but actual is (" + csq.length() + ")");
    }
    int length = end - start;
    if (length > chars.length - pos) {
      chars = expand(chars, pos + length, pos);
    }
    if ((csq instanceof AppendableCharSequence))
    {
      AppendableCharSequence seq = (AppendableCharSequence)csq;
      char[] src = chars;
      System.arraycopy(src, start, chars, pos, length);
      pos += length;
      return this;
    }
    for (int i = start; i < end; i++) {
      chars[(pos++)] = csq.charAt(i);
    }
    
    return this;
  }
  



  public void reset()
  {
    pos = 0;
  }
  
  public String toString()
  {
    return new String(chars, 0, pos);
  }
  


  public String substring(int start, int end)
  {
    int length = end - start;
    if ((start > pos) || (length > pos)) {
      throw new IndexOutOfBoundsException("expected: start and length <= (" + pos + ")");
    }
    
    return new String(chars, start, length);
  }
  




  public String subStringUnsafe(int start, int end)
  {
    return new String(chars, start, end - start);
  }
  
  private static char[] expand(char[] array, int neededSpace, int size) {
    int newCapacity = array.length;
    do
    {
      newCapacity <<= 1;
      
      if (newCapacity < 0) {
        throw new IllegalStateException();
      }
      
    } while (neededSpace > newCapacity);
    
    char[] newArray = new char[newCapacity];
    System.arraycopy(array, 0, newArray, 0, size);
    
    return newArray;
  }
}
