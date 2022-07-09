package io.netty.util;




























public abstract interface BooleanSupplier
{
  public static final BooleanSupplier FALSE_SUPPLIER = new BooleanSupplier()
  {
    public boolean get() {
      return false;
    }
  };
  



  public static final BooleanSupplier TRUE_SUPPLIER = new BooleanSupplier()
  {
    public boolean get() {
      return true;
    }
  };
  
  public abstract boolean get()
    throws Exception;
}
