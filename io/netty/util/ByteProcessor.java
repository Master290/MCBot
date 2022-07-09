package io.netty.util;







public abstract interface ByteProcessor
{
  public abstract boolean process(byte paramByte)
    throws Exception;
  






  public static class IndexOfProcessor
    implements ByteProcessor
  {
    private final byte byteToFind;
    






    public IndexOfProcessor(byte byteToFind)
    {
      this.byteToFind = byteToFind;
    }
    
    public boolean process(byte value)
    {
      return value != byteToFind;
    }
  }
  
  public static class IndexNotOfProcessor
    implements ByteProcessor
  {
    private final byte byteToNotFind;
    
    public IndexNotOfProcessor(byte byteToNotFind)
    {
      this.byteToNotFind = byteToNotFind;
    }
    
    public boolean process(byte value)
    {
      return value == byteToNotFind;
    }
  }
  



  public static final ByteProcessor FIND_NUL = new IndexOfProcessor((byte)0);
  



  public static final ByteProcessor FIND_NON_NUL = new IndexNotOfProcessor((byte)0);
  



  public static final ByteProcessor FIND_CR = new IndexOfProcessor((byte)13);
  



  public static final ByteProcessor FIND_NON_CR = new IndexNotOfProcessor((byte)13);
  



  public static final ByteProcessor FIND_LF = new IndexOfProcessor((byte)10);
  



  public static final ByteProcessor FIND_NON_LF = new IndexNotOfProcessor((byte)10);
  



  public static final ByteProcessor FIND_SEMI_COLON = new IndexOfProcessor((byte)59);
  



  public static final ByteProcessor FIND_COMMA = new IndexOfProcessor((byte)44);
  



  public static final ByteProcessor FIND_ASCII_SPACE = new IndexOfProcessor((byte)32);
  



  public static final ByteProcessor FIND_CRLF = new ByteProcessor()
  {
    public boolean process(byte value) {
      return (value != 13) && (value != 10);
    }
  };
  



  public static final ByteProcessor FIND_NON_CRLF = new ByteProcessor()
  {
    public boolean process(byte value) {
      return (value == 13) || (value == 10);
    }
  };
  



  public static final ByteProcessor FIND_LINEAR_WHITESPACE = new ByteProcessor()
  {
    public boolean process(byte value) {
      return (value != 32) && (value != 9);
    }
  };
  



  public static final ByteProcessor FIND_NON_LINEAR_WHITESPACE = new ByteProcessor()
  {
    public boolean process(byte value) {
      return (value == 32) || (value == 9);
    }
  };
}
