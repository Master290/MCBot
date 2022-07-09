package io.netty.channel.rxtx;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.oio.OioByteStreamChannel;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;


























@Deprecated
public class RxtxChannel
  extends OioByteStreamChannel
{
  private static final RxtxDeviceAddress LOCAL_ADDRESS = new RxtxDeviceAddress("localhost");
  
  private final RxtxChannelConfig config;
  
  private boolean open = true;
  private RxtxDeviceAddress deviceAddress;
  private SerialPort serialPort;
  
  public RxtxChannel() {
    super(null);
    
    config = new DefaultRxtxChannelConfig(this);
  }
  
  public RxtxChannelConfig config()
  {
    return config;
  }
  
  public boolean isOpen()
  {
    return open;
  }
  
  protected AbstractChannel.AbstractUnsafe newUnsafe()
  {
    return new RxtxUnsafe(null);
  }
  
  protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception
  {
    RxtxDeviceAddress remote = (RxtxDeviceAddress)remoteAddress;
    CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(remote.value());
    CommPort commPort = cpi.open(getClass().getName(), 1000);
    commPort.enableReceiveTimeout(((Integer)config().getOption(RxtxChannelOption.READ_TIMEOUT)).intValue());
    deviceAddress = remote;
    
    serialPort = ((SerialPort)commPort);
  }
  
  protected void doInit() throws Exception {
    serialPort.setSerialPortParams(
      ((Integer)config().getOption(RxtxChannelOption.BAUD_RATE)).intValue(), 
      ((RxtxChannelConfig.Databits)config().getOption(RxtxChannelOption.DATA_BITS)).value(), 
      ((RxtxChannelConfig.Stopbits)config().getOption(RxtxChannelOption.STOP_BITS)).value(), 
      ((RxtxChannelConfig.Paritybit)config().getOption(RxtxChannelOption.PARITY_BIT)).value());
    
    serialPort.setDTR(((Boolean)config().getOption(RxtxChannelOption.DTR)).booleanValue());
    serialPort.setRTS(((Boolean)config().getOption(RxtxChannelOption.RTS)).booleanValue());
    
    activate(serialPort.getInputStream(), serialPort.getOutputStream());
  }
  
  public RxtxDeviceAddress localAddress()
  {
    return (RxtxDeviceAddress)super.localAddress();
  }
  
  public RxtxDeviceAddress remoteAddress()
  {
    return (RxtxDeviceAddress)super.remoteAddress();
  }
  
  protected RxtxDeviceAddress localAddress0()
  {
    return LOCAL_ADDRESS;
  }
  
  protected RxtxDeviceAddress remoteAddress0()
  {
    return deviceAddress;
  }
  
  protected void doBind(SocketAddress localAddress) throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected void doDisconnect() throws Exception
  {
    doClose();
  }
  
  protected void doClose() throws Exception
  {
    open = false;
    try {
      super.doClose();
      
      if (serialPort != null) {
        serialPort.removeEventListener();
        serialPort.close();
        serialPort = null;
      }
    }
    finally
    {
      if (serialPort != null) {
        serialPort.removeEventListener();
        serialPort.close();
        serialPort = null;
      }
    }
  }
  
  protected boolean isInputShutdown()
  {
    return !open;
  }
  


  protected ChannelFuture shutdownInput() { return newFailedFuture(new UnsupportedOperationException("shutdownInput")); }
  
  private final class RxtxUnsafe extends AbstractChannel.AbstractUnsafe {
    private RxtxUnsafe() { super(); }
    

    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, final ChannelPromise promise)
    {
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      try
      {
        final boolean wasActive = isActive();
        doConnect(remoteAddress, localAddress);
        
        int waitTime = ((Integer)config().getOption(RxtxChannelOption.WAIT_TIME)).intValue();
        if (waitTime > 0) {
          eventLoop().schedule(new Runnable()
          {
            public void run() {
              try {
                doInit();
                safeSetSuccess(promise);
                if ((!wasActive) && (isActive())) {
                  pipeline().fireChannelActive();
                }
              } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed(); } } }, waitTime, TimeUnit.MILLISECONDS);

        }
        else
        {
          doInit();
          safeSetSuccess(promise);
          if ((!wasActive) && (isActive())) {
            pipeline().fireChannelActive();
          }
        }
      } catch (Throwable t) {
        safeSetFailure(promise, t);
        closeIfClosed();
      }
    }
  }
}
