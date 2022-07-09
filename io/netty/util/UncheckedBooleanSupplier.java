package io.netty.util;



























public abstract interface UncheckedBooleanSupplier
  extends BooleanSupplier
{
  public static final UncheckedBooleanSupplier FALSE_SUPPLIER = new UncheckedBooleanSupplier()
  {
    public boolean get() {
      return false;
    }
  };
  



  public static final UncheckedBooleanSupplier TRUE_SUPPLIER = new UncheckedBooleanSupplier()
  {
    public boolean get() {
      return true;
    }
  };
  
  public abstract boolean get();
}
