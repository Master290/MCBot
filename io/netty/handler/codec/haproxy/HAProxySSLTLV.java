package io.netty.handler.codec.haproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;
import java.util.Collections;
import java.util.List;




























public final class HAProxySSLTLV
  extends HAProxyTLV
{
  private final int verify;
  private final List<HAProxyTLV> tlvs;
  private final byte clientBitField;
  
  public HAProxySSLTLV(int verify, byte clientBitField, List<HAProxyTLV> tlvs)
  {
    this(verify, clientBitField, tlvs, Unpooled.EMPTY_BUFFER);
  }
  








  HAProxySSLTLV(int verify, byte clientBitField, List<HAProxyTLV> tlvs, ByteBuf rawContent)
  {
    super(HAProxyTLV.Type.PP2_TYPE_SSL, (byte)32, rawContent);
    
    this.verify = verify;
    this.tlvs = Collections.unmodifiableList(tlvs);
    this.clientBitField = clientBitField;
  }
  


  public boolean isPP2ClientCertConn()
  {
    return (clientBitField & 0x2) != 0;
  }
  


  public boolean isPP2ClientSSL()
  {
    return (clientBitField & 0x1) != 0;
  }
  


  public boolean isPP2ClientCertSess()
  {
    return (clientBitField & 0x4) != 0;
  }
  


  public byte client()
  {
    return clientBitField;
  }
  


  public int verify()
  {
    return verify;
  }
  


  public List<HAProxyTLV> encapsulatedTLVs()
  {
    return tlvs;
  }
  
  int contentNumBytes()
  {
    int tlvNumBytes = 0;
    for (int i = 0; i < tlvs.size(); i++) {
      tlvNumBytes += ((HAProxyTLV)tlvs.get(i)).totalNumBytes();
    }
    return 5 + tlvNumBytes;
  }
  
  public String toString()
  {
    return 
    



      StringUtil.simpleClassName(this) + "(type: " + type() + ", typeByteValue: " + typeByteValue() + ", client: " + client() + ", verify: " + verify() + ", numEncapsulatedTlvs: " + tlvs.size() + ')';
  }
}
