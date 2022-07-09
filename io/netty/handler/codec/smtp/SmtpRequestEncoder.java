package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;



















public final class SmtpRequestEncoder
  extends MessageToMessageEncoder<Object>
{
  private static final int CRLF_SHORT = 3338;
  private static final byte SP = 32;
  private static final ByteBuf DOT_CRLF_BUFFER = Unpooled.unreleasableBuffer(
    Unpooled.directBuffer(3).writeByte(46).writeByte(13).writeByte(10));
  private boolean contentExpected;
  
  public SmtpRequestEncoder() {}
  
  public boolean acceptOutboundMessage(Object msg) throws Exception {
    return ((msg instanceof SmtpRequest)) || ((msg instanceof SmtpContent));
  }
  
  protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception
  {
    if ((msg instanceof SmtpRequest)) {
      SmtpRequest req = (SmtpRequest)msg;
      if (contentExpected) {
        if (req.command().equals(SmtpCommand.RSET)) {
          contentExpected = false;
        } else {
          throw new IllegalStateException("SmtpContent expected");
        }
      }
      boolean release = true;
      ByteBuf buffer = ctx.alloc().buffer();
      try {
        req.command().encode(buffer);
        boolean notEmpty = req.command() != SmtpCommand.EMPTY;
        writeParameters(req.parameters(), buffer, notEmpty);
        ByteBufUtil.writeShortBE(buffer, 3338);
        out.add(buffer);
        release = false;
        if (req.command().isContentExpected()) {
          contentExpected = true;
        }
      } finally {
        if (release) {
          buffer.release();
        }
      }
    }
    
    if ((msg instanceof SmtpContent)) {
      if (!contentExpected) {
        throw new IllegalStateException("No SmtpContent expected");
      }
      ByteBuf content = ((SmtpContent)msg).content();
      out.add(content.retain());
      if ((msg instanceof LastSmtpContent)) {
        out.add(DOT_CRLF_BUFFER.retainedDuplicate());
        contentExpected = false;
      }
    }
  }
  
  private static void writeParameters(List<CharSequence> parameters, ByteBuf out, boolean commandNotEmpty) {
    if (parameters.isEmpty()) {
      return;
    }
    if (commandNotEmpty) {
      out.writeByte(32);
    }
    if ((parameters instanceof RandomAccess)) {
      int sizeMinusOne = parameters.size() - 1;
      for (int i = 0; i < sizeMinusOne; i++) {
        ByteBufUtil.writeAscii(out, (CharSequence)parameters.get(i));
        out.writeByte(32);
      }
      ByteBufUtil.writeAscii(out, (CharSequence)parameters.get(sizeMinusOne));
    } else {
      Iterator<CharSequence> params = parameters.iterator();
      for (;;) {
        ByteBufUtil.writeAscii(out, (CharSequence)params.next());
        if (!params.hasNext()) break;
        out.writeByte(32);
      }
    }
  }
}
