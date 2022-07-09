package io.netty.util;











public final class Signal
  extends Error
  implements Constant<Signal>
{
  private static final long serialVersionUID = -221145131122459977L;
  








  private static final ConstantPool<Signal> pool = new ConstantPool()
  {
    protected Signal newConstant(int id, String name) {
      return new Signal(id, name, null);
    }
  };
  
  private final SignalConstant constant;
  
  public static Signal valueOf(String name)
  {
    return (Signal)pool.valueOf(name);
  }
  


  public static Signal valueOf(Class<?> firstNameComponent, String secondNameComponent)
  {
    return (Signal)pool.valueOf(firstNameComponent, secondNameComponent);
  }
  




  private Signal(int id, String name)
  {
    constant = new SignalConstant(id, name);
  }
  



  public void expect(Signal signal)
  {
    if (this != signal) {
      throw new IllegalStateException("unexpected signal: " + signal);
    }
  }
  

  public Throwable initCause(Throwable cause)
  {
    return this;
  }
  

  public Throwable fillInStackTrace()
  {
    return this;
  }
  
  public int id()
  {
    return constant.id();
  }
  
  public String name()
  {
    return constant.name();
  }
  
  public boolean equals(Object obj)
  {
    return this == obj;
  }
  
  public int hashCode()
  {
    return System.identityHashCode(this);
  }
  
  public int compareTo(Signal other)
  {
    if (this == other) {
      return 0;
    }
    
    return constant.compareTo(constant);
  }
  
  public String toString()
  {
    return name();
  }
  
  private static final class SignalConstant extends AbstractConstant<SignalConstant> {
    SignalConstant(int id, String name) {
      super(name);
    }
  }
}
