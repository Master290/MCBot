package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Signal;
import io.netty.util.internal.StringUtil;
import java.util.List;


































































































































































































































































public abstract class ReplayingDecoder<S>
  extends ByteToMessageDecoder
{
  static final Signal REPLAY = Signal.valueOf(ReplayingDecoder.class, "REPLAY");
  
  private final ReplayingDecoderByteBuf replayable = new ReplayingDecoderByteBuf();
  private S state;
  private int checkpoint = -1;
  


  protected ReplayingDecoder()
  {
    this(null);
  }
  


  protected ReplayingDecoder(S initialState)
  {
    state = initialState;
  }
  


  protected void checkpoint()
  {
    checkpoint = internalBuffer().readerIndex();
  }
  



  protected void checkpoint(S state)
  {
    checkpoint();
    state(state);
  }
  



  protected S state()
  {
    return state;
  }
  



  protected S state(S newState)
  {
    S oldState = state;
    state = newState;
    return oldState;
  }
  
  final void channelInputClosed(ChannelHandlerContext ctx, List<Object> out) throws Exception
  {
    try {
      replayable.terminate();
      if (cumulation != null) {
        callDecode(ctx, internalBuffer(), out);
      } else {
        replayable.setCumulation(Unpooled.EMPTY_BUFFER);
      }
      decodeLast(ctx, replayable, out);
    }
    catch (Signal replay) {
      replay.expect(REPLAY);
    }
  }
  
  protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
  {
    replayable.setCumulation(in);
    try {
      while (in.isReadable()) {
        int oldReaderIndex = this.checkpoint = in.readerIndex();
        int outSize = out.size();
        
        if (outSize > 0) {
          fireChannelRead(ctx, out, outSize);
          out.clear();
          





          if (!ctx.isRemoved())
          {

            outSize = 0;
          }
        } else {
          S oldState = state;
          int oldInputLength = in.readableBytes();
          try {
            decodeRemovalReentryProtection(ctx, replayable, out);
            




            if (ctx.isRemoved()) {
              break;
            }
            
            if (outSize == out.size()) {
              if ((oldInputLength == in.readableBytes()) && (oldState == state))
              {
                throw new DecoderException(StringUtil.simpleClassName(getClass()) + ".decode() must consume the inbound data or change its state if it did not decode anything.");
              }
              


              continue;
            }
          }
          catch (Signal replay) {
            replay.expect(REPLAY);
            




            if (!ctx.isRemoved()) break label191; }
          break;
          
          label191:
          
          int checkpoint = this.checkpoint;
          if (checkpoint >= 0) {
            in.readerIndex(checkpoint);
          }
          


          break;
          

          if ((oldReaderIndex == in.readerIndex()) && (oldState == state))
          {
            throw new DecoderException(StringUtil.simpleClassName(getClass()) + ".decode() method must consume the inbound data or change its state if it decoded something.");
          }
          
          if (isSingleDecode())
            break;
        }
      }
    } catch (DecoderException e) {
      throw e;
    } catch (Exception cause) {
      throw new DecoderException(cause);
    }
  }
}
