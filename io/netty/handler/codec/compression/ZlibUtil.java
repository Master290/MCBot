package io.netty.handler.codec.compression;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.Inflater;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.JZlib.WrapperType;

















final class ZlibUtil
{
  static void fail(Inflater z, String message, int resultCode)
  {
    throw inflaterException(z, message, resultCode);
  }
  
  static void fail(Deflater z, String message, int resultCode) {
    throw deflaterException(z, message, resultCode);
  }
  
  static DecompressionException inflaterException(Inflater z, String message, int resultCode) {
    return new DecompressionException(message + " (" + resultCode + ')' + (msg != null ? ": " + msg : ""));
  }
  

  static CompressionException deflaterException(Deflater z, String message, int resultCode) { return new CompressionException(message + " (" + resultCode + ')' + (msg != null ? ": " + msg : "")); }
  
  static JZlib.WrapperType convertWrapperType(ZlibWrapper wrapper) { JZlib.WrapperType convertedWrapperType;
    JZlib.WrapperType convertedWrapperType;
    JZlib.WrapperType convertedWrapperType;
    JZlib.WrapperType convertedWrapperType; switch (1.$SwitchMap$io$netty$handler$codec$compression$ZlibWrapper[wrapper.ordinal()]) {
    case 1: 
      convertedWrapperType = JZlib.W_NONE;
      break;
    case 2: 
      convertedWrapperType = JZlib.W_ZLIB;
      break;
    case 3: 
      convertedWrapperType = JZlib.W_GZIP;
      break;
    case 4: 
      convertedWrapperType = JZlib.W_ANY;
      break;
    default: 
      throw new Error(); }
    JZlib.WrapperType convertedWrapperType;
    return convertedWrapperType; }
  
  static int wrapperOverhead(ZlibWrapper wrapper) { int overhead;
    int overhead;
    int overhead;
    switch (1.$SwitchMap$io$netty$handler$codec$compression$ZlibWrapper[wrapper.ordinal()]) {
    case 1: 
      overhead = 0;
      break;
    case 2: 
    case 4: 
      overhead = 2;
      break;
    case 3: 
      overhead = 10;
      break;
    default: 
      throw new Error(); }
    int overhead;
    return overhead;
  }
  
  private ZlibUtil() {}
}
