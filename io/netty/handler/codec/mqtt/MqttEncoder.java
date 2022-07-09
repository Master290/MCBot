package io.netty.handler.codec.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.Iterator;
import java.util.List;





























@ChannelHandler.Sharable
public final class MqttEncoder
  extends MessageToMessageEncoder<MqttMessage>
{
  public static final MqttEncoder INSTANCE = new MqttEncoder();
  
  private MqttEncoder() {}
  
  protected void encode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception
  {
    out.add(doEncode(ctx, msg));
  }
  








  static ByteBuf doEncode(ChannelHandlerContext ctx, MqttMessage message)
  {
    switch (1.$SwitchMap$io$netty$handler$codec$mqtt$MqttMessageType[message.fixedHeader().messageType().ordinal()]) {
    case 1: 
      return encodeConnectMessage(ctx, (MqttConnectMessage)message);
    
    case 2: 
      return encodeConnAckMessage(ctx, (MqttConnAckMessage)message);
    
    case 3: 
      return encodePublishMessage(ctx, (MqttPublishMessage)message);
    
    case 4: 
      return encodeSubscribeMessage(ctx, (MqttSubscribeMessage)message);
    
    case 5: 
      return encodeUnsubscribeMessage(ctx, (MqttUnsubscribeMessage)message);
    
    case 6: 
      return encodeSubAckMessage(ctx, (MqttSubAckMessage)message);
    
    case 7: 
      if ((message instanceof MqttUnsubAckMessage)) {
        return encodeUnsubAckMessage(ctx, (MqttUnsubAckMessage)message);
      }
      return encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(ctx.alloc(), message);
    
    case 8: 
    case 9: 
    case 10: 
    case 11: 
      return encodePubReplyMessage(ctx, message);
    
    case 12: 
    case 13: 
      return encodeReasonCodePlusPropertiesMessage(ctx, message);
    
    case 14: 
    case 15: 
      return encodeMessageWithOnlySingleByteFixedHeader(ctx.alloc(), message);
    }
    
    
    throw new IllegalArgumentException("Unknown message type: " + message.fixedHeader().messageType().value());
  }
  
  /* Error */
  private static ByteBuf encodeConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage message)
  {
    // Byte code:
    //   0: iconst_0
    //   1: istore_2
    //   2: aload_1
    //   3: invokevirtual 37	io/netty/handler/codec/mqtt/MqttConnectMessage:fixedHeader	()Lio/netty/handler/codec/mqtt/MqttFixedHeader;
    //   6: astore_3
    //   7: aload_1
    //   8: invokevirtual 38	io/netty/handler/codec/mqtt/MqttConnectMessage:variableHeader	()Lio/netty/handler/codec/mqtt/MqttConnectVariableHeader;
    //   11: astore 4
    //   13: aload_1
    //   14: invokevirtual 39	io/netty/handler/codec/mqtt/MqttConnectMessage:payload	()Lio/netty/handler/codec/mqtt/MqttConnectPayload;
    //   17: astore 5
    //   19: aload 4
    //   21: invokevirtual 40	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:name	()Ljava/lang/String;
    //   24: aload 4
    //   26: invokevirtual 41	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:version	()I
    //   29: i2b
    //   30: invokestatic 42	io/netty/handler/codec/mqtt/MqttVersion:fromProtocolNameAndLevel	(Ljava/lang/String;B)Lio/netty/handler/codec/mqtt/MqttVersion;
    //   33: astore 6
    //   35: aload_0
    //   36: aload 6
    //   38: invokestatic 43	io/netty/handler/codec/mqtt/MqttCodecUtil:setMqttVersion	(Lio/netty/channel/ChannelHandlerContext;Lio/netty/handler/codec/mqtt/MqttVersion;)V
    //   41: aload 4
    //   43: invokevirtual 44	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:hasUserName	()Z
    //   46: ifne +21 -> 67
    //   49: aload 4
    //   51: invokevirtual 45	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:hasPassword	()Z
    //   54: ifeq +13 -> 67
    //   57: new 46	io/netty/handler/codec/EncoderException
    //   60: dup
    //   61: ldc 47
    //   63: invokespecial 48	io/netty/handler/codec/EncoderException:<init>	(Ljava/lang/String;)V
    //   66: athrow
    //   67: aload 5
    //   69: invokevirtual 49	io/netty/handler/codec/mqtt/MqttConnectPayload:clientIdentifier	()Ljava/lang/String;
    //   72: astore 7
    //   74: aload 6
    //   76: bipush 23
    //   78: aload 7
    //   80: invokestatic 50	io/netty/handler/codec/mqtt/MqttCodecUtil:isValidClientId	(Lio/netty/handler/codec/mqtt/MqttVersion;ILjava/lang/String;)Z
    //   83: ifne +31 -> 114
    //   86: new 51	io/netty/handler/codec/mqtt/MqttIdentifierRejectedException
    //   89: dup
    //   90: new 29	java/lang/StringBuilder
    //   93: dup
    //   94: invokespecial 30	java/lang/StringBuilder:<init>	()V
    //   97: ldc 52
    //   99: invokevirtual 32	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   102: aload 7
    //   104: invokevirtual 32	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   107: invokevirtual 35	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   110: invokespecial 53	io/netty/handler/codec/mqtt/MqttIdentifierRejectedException:<init>	(Ljava/lang/String;)V
    //   113: athrow
    //   114: aload 7
    //   116: invokestatic 54	io/netty/buffer/ByteBufUtil:utf8Bytes	(Ljava/lang/CharSequence;)I
    //   119: istore 8
    //   121: iload_2
    //   122: iconst_2
    //   123: iload 8
    //   125: iadd
    //   126: iadd
    //   127: istore_2
    //   128: aload 5
    //   130: invokevirtual 55	io/netty/handler/codec/mqtt/MqttConnectPayload:willTopic	()Ljava/lang/String;
    //   133: astore 9
    //   135: aload 9
    //   137: invokestatic 56	io/netty/handler/codec/mqtt/MqttEncoder:nullableUtf8Bytes	(Ljava/lang/String;)I
    //   140: istore 10
    //   142: aload 5
    //   144: invokevirtual 57	io/netty/handler/codec/mqtt/MqttConnectPayload:willMessageInBytes	()[B
    //   147: astore 11
    //   149: aload 11
    //   151: ifnull +8 -> 159
    //   154: aload 11
    //   156: goto +6 -> 162
    //   159: getstatic 58	io/netty/util/internal/EmptyArrays:EMPTY_BYTES	[B
    //   162: astore 12
    //   164: aload 4
    //   166: invokevirtual 59	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:isWillFlag	()Z
    //   169: ifeq +18 -> 187
    //   172: iload_2
    //   173: iconst_2
    //   174: iload 10
    //   176: iadd
    //   177: iadd
    //   178: istore_2
    //   179: iload_2
    //   180: iconst_2
    //   181: aload 12
    //   183: arraylength
    //   184: iadd
    //   185: iadd
    //   186: istore_2
    //   187: aload 5
    //   189: invokevirtual 60	io/netty/handler/codec/mqtt/MqttConnectPayload:userName	()Ljava/lang/String;
    //   192: astore 13
    //   194: aload 13
    //   196: invokestatic 56	io/netty/handler/codec/mqtt/MqttEncoder:nullableUtf8Bytes	(Ljava/lang/String;)I
    //   199: istore 14
    //   201: aload 4
    //   203: invokevirtual 44	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:hasUserName	()Z
    //   206: ifeq +10 -> 216
    //   209: iload_2
    //   210: iconst_2
    //   211: iload 14
    //   213: iadd
    //   214: iadd
    //   215: istore_2
    //   216: aload 5
    //   218: invokevirtual 61	io/netty/handler/codec/mqtt/MqttConnectPayload:passwordInBytes	()[B
    //   221: astore 15
    //   223: aload 15
    //   225: ifnull +8 -> 233
    //   228: aload 15
    //   230: goto +6 -> 236
    //   233: getstatic 58	io/netty/util/internal/EmptyArrays:EMPTY_BYTES	[B
    //   236: astore 16
    //   238: aload 4
    //   240: invokevirtual 45	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:hasPassword	()Z
    //   243: ifeq +11 -> 254
    //   246: iload_2
    //   247: iconst_2
    //   248: aload 16
    //   250: arraylength
    //   251: iadd
    //   252: iadd
    //   253: istore_2
    //   254: aload 6
    //   256: invokevirtual 62	io/netty/handler/codec/mqtt/MqttVersion:protocolNameBytes	()[B
    //   259: astore 17
    //   261: aload 6
    //   263: aload_0
    //   264: invokeinterface 23 1 0
    //   269: aload_1
    //   270: invokevirtual 38	io/netty/handler/codec/mqtt/MqttConnectMessage:variableHeader	()Lio/netty/handler/codec/mqtt/MqttConnectVariableHeader;
    //   273: invokevirtual 63	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:properties	()Lio/netty/handler/codec/mqtt/MqttProperties;
    //   276: invokestatic 64	io/netty/handler/codec/mqtt/MqttEncoder:encodePropertiesIfNeeded	(Lio/netty/handler/codec/mqtt/MqttVersion;Lio/netty/buffer/ByteBufAllocator;Lio/netty/handler/codec/mqtt/MqttProperties;)Lio/netty/buffer/ByteBuf;
    //   279: astore 18
    //   281: aload 4
    //   283: invokevirtual 59	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:isWillFlag	()Z
    //   286: ifeq +32 -> 318
    //   289: aload 6
    //   291: aload_0
    //   292: invokeinterface 23 1 0
    //   297: aload 5
    //   299: invokevirtual 65	io/netty/handler/codec/mqtt/MqttConnectPayload:willProperties	()Lio/netty/handler/codec/mqtt/MqttProperties;
    //   302: invokestatic 64	io/netty/handler/codec/mqtt/MqttEncoder:encodePropertiesIfNeeded	(Lio/netty/handler/codec/mqtt/MqttVersion;Lio/netty/buffer/ByteBufAllocator;Lio/netty/handler/codec/mqtt/MqttProperties;)Lio/netty/buffer/ByteBuf;
    //   305: astore 19
    //   307: iload_2
    //   308: aload 19
    //   310: invokevirtual 66	io/netty/buffer/ByteBuf:readableBytes	()I
    //   313: iadd
    //   314: istore_2
    //   315: goto +8 -> 323
    //   318: getstatic 67	io/netty/buffer/Unpooled:EMPTY_BUFFER	Lio/netty/buffer/ByteBuf;
    //   321: astore 19
    //   323: iconst_2
    //   324: aload 17
    //   326: arraylength
    //   327: iadd
    //   328: iconst_4
    //   329: iadd
    //   330: aload 18
    //   332: invokevirtual 66	io/netty/buffer/ByteBuf:readableBytes	()I
    //   335: iadd
    //   336: istore 20
    //   338: iload 20
    //   340: iload_2
    //   341: iadd
    //   342: istore 21
    //   344: iconst_1
    //   345: iload 21
    //   347: invokestatic 68	io/netty/handler/codec/mqtt/MqttEncoder:getVariableLengthInt	(I)I
    //   350: iadd
    //   351: istore 22
    //   353: aload_0
    //   354: invokeinterface 23 1 0
    //   359: iload 22
    //   361: iload 21
    //   363: iadd
    //   364: invokeinterface 69 2 0
    //   369: astore 23
    //   371: aload 23
    //   373: aload_3
    //   374: invokestatic 70	io/netty/handler/codec/mqtt/MqttEncoder:getFixedHeaderByte1	(Lio/netty/handler/codec/mqtt/MqttFixedHeader;)I
    //   377: invokevirtual 71	io/netty/buffer/ByteBuf:writeByte	(I)Lio/netty/buffer/ByteBuf;
    //   380: pop
    //   381: aload 23
    //   383: iload 21
    //   385: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   388: aload 23
    //   390: aload 17
    //   392: arraylength
    //   393: invokevirtual 73	io/netty/buffer/ByteBuf:writeShort	(I)Lio/netty/buffer/ByteBuf;
    //   396: pop
    //   397: aload 23
    //   399: aload 17
    //   401: invokevirtual 74	io/netty/buffer/ByteBuf:writeBytes	([B)Lio/netty/buffer/ByteBuf;
    //   404: pop
    //   405: aload 23
    //   407: aload 4
    //   409: invokevirtual 41	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:version	()I
    //   412: invokevirtual 71	io/netty/buffer/ByteBuf:writeByte	(I)Lio/netty/buffer/ByteBuf;
    //   415: pop
    //   416: aload 23
    //   418: aload 4
    //   420: invokestatic 75	io/netty/handler/codec/mqtt/MqttEncoder:getConnVariableHeaderFlag	(Lio/netty/handler/codec/mqtt/MqttConnectVariableHeader;)I
    //   423: invokevirtual 71	io/netty/buffer/ByteBuf:writeByte	(I)Lio/netty/buffer/ByteBuf;
    //   426: pop
    //   427: aload 23
    //   429: aload 4
    //   431: invokevirtual 76	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:keepAliveTimeSeconds	()I
    //   434: invokevirtual 73	io/netty/buffer/ByteBuf:writeShort	(I)Lio/netty/buffer/ByteBuf;
    //   437: pop
    //   438: aload 23
    //   440: aload 18
    //   442: invokevirtual 77	io/netty/buffer/ByteBuf:writeBytes	(Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;
    //   445: pop
    //   446: aload 23
    //   448: aload 7
    //   450: iload 8
    //   452: invokestatic 78	io/netty/handler/codec/mqtt/MqttEncoder:writeExactUTF8String	(Lio/netty/buffer/ByteBuf;Ljava/lang/String;I)V
    //   455: aload 4
    //   457: invokevirtual 59	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:isWillFlag	()Z
    //   460: ifeq +41 -> 501
    //   463: aload 23
    //   465: aload 19
    //   467: invokevirtual 77	io/netty/buffer/ByteBuf:writeBytes	(Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;
    //   470: pop
    //   471: aload 23
    //   473: aload 9
    //   475: iload 10
    //   477: invokestatic 78	io/netty/handler/codec/mqtt/MqttEncoder:writeExactUTF8String	(Lio/netty/buffer/ByteBuf;Ljava/lang/String;I)V
    //   480: aload 23
    //   482: aload 12
    //   484: arraylength
    //   485: invokevirtual 73	io/netty/buffer/ByteBuf:writeShort	(I)Lio/netty/buffer/ByteBuf;
    //   488: pop
    //   489: aload 23
    //   491: aload 12
    //   493: iconst_0
    //   494: aload 12
    //   496: arraylength
    //   497: invokevirtual 79	io/netty/buffer/ByteBuf:writeBytes	([BII)Lio/netty/buffer/ByteBuf;
    //   500: pop
    //   501: aload 4
    //   503: invokevirtual 44	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:hasUserName	()Z
    //   506: ifeq +12 -> 518
    //   509: aload 23
    //   511: aload 13
    //   513: iload 14
    //   515: invokestatic 78	io/netty/handler/codec/mqtt/MqttEncoder:writeExactUTF8String	(Lio/netty/buffer/ByteBuf;Ljava/lang/String;I)V
    //   518: aload 4
    //   520: invokevirtual 45	io/netty/handler/codec/mqtt/MqttConnectVariableHeader:hasPassword	()Z
    //   523: ifeq +24 -> 547
    //   526: aload 23
    //   528: aload 16
    //   530: arraylength
    //   531: invokevirtual 73	io/netty/buffer/ByteBuf:writeShort	(I)Lio/netty/buffer/ByteBuf;
    //   534: pop
    //   535: aload 23
    //   537: aload 16
    //   539: iconst_0
    //   540: aload 16
    //   542: arraylength
    //   543: invokevirtual 79	io/netty/buffer/ByteBuf:writeBytes	([BII)Lio/netty/buffer/ByteBuf;
    //   546: pop
    //   547: aload 23
    //   549: astore 24
    //   551: aload 19
    //   553: invokevirtual 80	io/netty/buffer/ByteBuf:release	()Z
    //   556: pop
    //   557: aload 18
    //   559: invokevirtual 80	io/netty/buffer/ByteBuf:release	()Z
    //   562: pop
    //   563: aload 24
    //   565: areturn
    //   566: astore 25
    //   568: aload 19
    //   570: invokevirtual 80	io/netty/buffer/ByteBuf:release	()Z
    //   573: pop
    //   574: aload 25
    //   576: athrow
    //   577: astore 26
    //   579: aload 18
    //   581: invokevirtual 80	io/netty/buffer/ByteBuf:release	()Z
    //   584: pop
    //   585: aload 26
    //   587: athrow
    // Line number table:
    //   Java source line #112	-> byte code offset #0
    //   Java source line #114	-> byte code offset #2
    //   Java source line #115	-> byte code offset #7
    //   Java source line #116	-> byte code offset #13
    //   Java source line #117	-> byte code offset #19
    //   Java source line #118	-> byte code offset #26
    //   Java source line #117	-> byte code offset #30
    //   Java source line #119	-> byte code offset #35
    //   Java source line #122	-> byte code offset #41
    //   Java source line #123	-> byte code offset #57
    //   Java source line #127	-> byte code offset #67
    //   Java source line #128	-> byte code offset #74
    //   Java source line #129	-> byte code offset #86
    //   Java source line #131	-> byte code offset #114
    //   Java source line #132	-> byte code offset #121
    //   Java source line #135	-> byte code offset #128
    //   Java source line #136	-> byte code offset #135
    //   Java source line #137	-> byte code offset #142
    //   Java source line #138	-> byte code offset #149
    //   Java source line #139	-> byte code offset #164
    //   Java source line #140	-> byte code offset #172
    //   Java source line #141	-> byte code offset #179
    //   Java source line #144	-> byte code offset #187
    //   Java source line #145	-> byte code offset #194
    //   Java source line #146	-> byte code offset #201
    //   Java source line #147	-> byte code offset #209
    //   Java source line #150	-> byte code offset #216
    //   Java source line #151	-> byte code offset #223
    //   Java source line #152	-> byte code offset #238
    //   Java source line #153	-> byte code offset #246
    //   Java source line #157	-> byte code offset #254
    //   Java source line #158	-> byte code offset #261
    //   Java source line #160	-> byte code offset #264
    //   Java source line #161	-> byte code offset #270
    //   Java source line #158	-> byte code offset #276
    //   Java source line #164	-> byte code offset #281
    //   Java source line #165	-> byte code offset #289
    //   Java source line #166	-> byte code offset #307
    //   Java source line #168	-> byte code offset #318
    //   Java source line #171	-> byte code offset #323
    //   Java source line #173	-> byte code offset #338
    //   Java source line #174	-> byte code offset #344
    //   Java source line #175	-> byte code offset #353
    //   Java source line #176	-> byte code offset #371
    //   Java source line #177	-> byte code offset #381
    //   Java source line #179	-> byte code offset #388
    //   Java source line #180	-> byte code offset #397
    //   Java source line #182	-> byte code offset #405
    //   Java source line #183	-> byte code offset #416
    //   Java source line #184	-> byte code offset #427
    //   Java source line #185	-> byte code offset #438
    //   Java source line #188	-> byte code offset #446
    //   Java source line #189	-> byte code offset #455
    //   Java source line #190	-> byte code offset #463
    //   Java source line #191	-> byte code offset #471
    //   Java source line #192	-> byte code offset #480
    //   Java source line #193	-> byte code offset #489
    //   Java source line #195	-> byte code offset #501
    //   Java source line #196	-> byte code offset #509
    //   Java source line #198	-> byte code offset #518
    //   Java source line #199	-> byte code offset #526
    //   Java source line #200	-> byte code offset #535
    //   Java source line #202	-> byte code offset #547
    //   Java source line #204	-> byte code offset #551
    //   Java source line #207	-> byte code offset #557
    //   Java source line #202	-> byte code offset #563
    //   Java source line #204	-> byte code offset #566
    //   Java source line #205	-> byte code offset #574
    //   Java source line #207	-> byte code offset #577
    //   Java source line #208	-> byte code offset #585
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	588	0	ctx	ChannelHandlerContext
    //   0	588	1	message	MqttConnectMessage
    //   1	340	2	payloadBufferSize	int
    //   6	368	3	mqttFixedHeader	MqttFixedHeader
    //   11	508	4	variableHeader	MqttConnectVariableHeader
    //   17	281	5	payload	MqttConnectPayload
    //   33	257	6	mqttVersion	MqttVersion
    //   72	377	7	clientIdentifier	String
    //   119	332	8	clientIdentifierBytes	int
    //   133	341	9	willTopic	String
    //   140	336	10	willTopicBytes	int
    //   147	8	11	willMessage	byte[]
    //   162	333	12	willMessageBytes	byte[]
    //   192	320	13	userName	String
    //   199	315	14	userNameBytes	int
    //   221	8	15	password	byte[]
    //   236	305	16	passwordBytes	byte[]
    //   259	141	17	protocolNameBytes	byte[]
    //   279	301	18	propertiesBuf	ByteBuf
    //   305	4	19	willPropertiesBuf	ByteBuf
    //   321	248	19	willPropertiesBuf	ByteBuf
    //   336	3	20	variableHeaderBufferSize	int
    //   342	42	21	variablePartSize	int
    //   351	9	22	fixedHeaderBufferSize	int
    //   369	179	23	buf	ByteBuf
    //   549	15	24	localByteBuf1	ByteBuf
    //   566	9	25	localObject1	Object
    //   577	9	26	localObject2	Object
    // Exception table:
    //   from	to	target	type
    //   323	551	566	finally
    //   566	568	566	finally
    //   281	557	577	finally
    //   566	579	577	finally
  }
  
  private static int getConnVariableHeaderFlag(MqttConnectVariableHeader variableHeader)
  {
    int flagByte = 0;
    if (variableHeader.hasUserName()) {
      flagByte |= 0x80;
    }
    if (variableHeader.hasPassword()) {
      flagByte |= 0x40;
    }
    if (variableHeader.isWillRetain()) {
      flagByte |= 0x20;
    }
    flagByte |= (variableHeader.willQos() & 0x3) << 3;
    if (variableHeader.isWillFlag()) {
      flagByte |= 0x4;
    }
    if (variableHeader.isCleanSession()) {
      flagByte |= 0x2;
    }
    return flagByte;
  }
  

  private static ByteBuf encodeConnAckMessage(ChannelHandlerContext ctx, MqttConnAckMessage message)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    ByteBuf propertiesBuf = encodePropertiesIfNeeded(mqttVersion, ctx
      .alloc(), message
      .variableHeader().properties());
    try
    {
      ByteBuf buf = ctx.alloc().buffer(4 + propertiesBuf.readableBytes());
      buf.writeByte(getFixedHeaderByte1(message.fixedHeader()));
      writeVariableLengthInt(buf, 2 + propertiesBuf.readableBytes());
      buf.writeByte(message.variableHeader().isSessionPresent() ? 1 : 0);
      buf.writeByte(message.variableHeader().connectReturnCode().byteValue());
      buf.writeBytes(propertiesBuf);
      return buf;
    } finally {
      propertiesBuf.release();
    }
  }
  

  private static ByteBuf encodeSubscribeMessage(ChannelHandlerContext ctx, MqttSubscribeMessage message)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    ByteBuf propertiesBuf = encodePropertiesIfNeeded(mqttVersion, ctx
      .alloc(), message
      .idAndPropertiesVariableHeader().properties());
    try
    {
      int variableHeaderBufferSize = 2 + propertiesBuf.readableBytes();
      int payloadBufferSize = 0;
      
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      MqttMessageIdVariableHeader variableHeader = message.variableHeader();
      MqttSubscribePayload payload = message.payload();
      
      for (MqttTopicSubscription topic : payload.topicSubscriptions()) {
        String topicName = topic.topicName();
        int topicNameBytes = ByteBufUtil.utf8Bytes(topicName);
        payloadBufferSize += 2 + topicNameBytes;
        payloadBufferSize++;
      }
      
      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      
      ByteBuf buf = ctx.alloc().buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      writeVariableLengthInt(buf, variablePartSize);
      

      int messageId = variableHeader.messageId();
      buf.writeShort(messageId);
      buf.writeBytes(propertiesBuf);
      

      for (Object localObject1 = payload.topicSubscriptions().iterator(); ((Iterator)localObject1).hasNext();) { MqttTopicSubscription topic = (MqttTopicSubscription)((Iterator)localObject1).next();
        writeUnsafeUTF8String(buf, topic.topicName());
        if ((mqttVersion == MqttVersion.MQTT_3_1_1) || (mqttVersion == MqttVersion.MQTT_3_1)) {
          buf.writeByte(topic.qualityOfService().value());
        } else {
          MqttSubscriptionOption option = topic.option();
          
          int optionEncoded = option.retainHandling().value() << 4;
          if (option.isRetainAsPublished()) {
            optionEncoded |= 0x8;
          }
          if (option.isNoLocal()) {
            optionEncoded |= 0x4;
          }
          optionEncoded |= option.qos().value();
          
          buf.writeByte(optionEncoded);
        }
      }
      
      return buf;
    } finally {
      propertiesBuf.release();
    }
  }
  

  private static ByteBuf encodeUnsubscribeMessage(ChannelHandlerContext ctx, MqttUnsubscribeMessage message)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    ByteBuf propertiesBuf = encodePropertiesIfNeeded(mqttVersion, ctx
      .alloc(), message
      .idAndPropertiesVariableHeader().properties());
    try
    {
      int variableHeaderBufferSize = 2 + propertiesBuf.readableBytes();
      int payloadBufferSize = 0;
      
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      MqttMessageIdVariableHeader variableHeader = message.variableHeader();
      MqttUnsubscribePayload payload = message.payload();
      
      for (String topicName : payload.topics()) {
        int topicNameBytes = ByteBufUtil.utf8Bytes(topicName);
        payloadBufferSize += 2 + topicNameBytes;
      }
      
      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      
      ByteBuf buf = ctx.alloc().buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      writeVariableLengthInt(buf, variablePartSize);
      

      int messageId = variableHeader.messageId();
      buf.writeShort(messageId);
      buf.writeBytes(propertiesBuf);
      

      for (Object localObject1 = payload.topics().iterator(); ((Iterator)localObject1).hasNext();) { String topicName = (String)((Iterator)localObject1).next();
        writeUnsafeUTF8String(buf, topicName);
      }
      
      return buf;
    } finally {
      propertiesBuf.release();
    }
  }
  

  private static ByteBuf encodeSubAckMessage(ChannelHandlerContext ctx, MqttSubAckMessage message)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    ByteBuf propertiesBuf = encodePropertiesIfNeeded(mqttVersion, ctx
      .alloc(), message
      .idAndPropertiesVariableHeader().properties());
    try {
      int variableHeaderBufferSize = 2 + propertiesBuf.readableBytes();
      int payloadBufferSize = message.payload().grantedQoSLevels().size();
      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      ByteBuf buf = ctx.alloc().buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(message.fixedHeader()));
      writeVariableLengthInt(buf, variablePartSize);
      buf.writeShort(message.variableHeader().messageId());
      buf.writeBytes(propertiesBuf);
      for (Object localObject1 = message.payload().reasonCodes().iterator(); ((Iterator)localObject1).hasNext();) { int code = ((Integer)((Iterator)localObject1).next()).intValue();
        buf.writeByte(code);
      }
      
      return buf;
    } finally {
      propertiesBuf.release();
    }
  }
  

  private static ByteBuf encodeUnsubAckMessage(ChannelHandlerContext ctx, MqttUnsubAckMessage message)
  {
    if ((message.variableHeader() instanceof MqttMessageIdAndPropertiesVariableHeader)) {
      MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
      ByteBuf propertiesBuf = encodePropertiesIfNeeded(mqttVersion, ctx
        .alloc(), message
        .idAndPropertiesVariableHeader().properties());
      try {
        int variableHeaderBufferSize = 2 + propertiesBuf.readableBytes();
        MqttUnsubAckPayload payload = message.payload();
        int payloadBufferSize = payload == null ? 0 : payload.unsubscribeReasonCodes().size();
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
        ByteBuf buf = ctx.alloc().buffer(fixedHeaderBufferSize + variablePartSize);
        buf.writeByte(getFixedHeaderByte1(message.fixedHeader()));
        writeVariableLengthInt(buf, variablePartSize);
        buf.writeShort(message.variableHeader().messageId());
        buf.writeBytes(propertiesBuf);
        Object localObject1;
        if (payload != null) {
          for (localObject1 = payload.unsubscribeReasonCodes().iterator(); ((Iterator)localObject1).hasNext();) { Short reasonCode = (Short)((Iterator)localObject1).next();
            buf.writeByte(reasonCode.shortValue());
          }
        }
        
        return buf;
      } finally {
        propertiesBuf.release();
      }
    }
    return encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(ctx.alloc(), message);
  }
  


  private static ByteBuf encodePublishMessage(ChannelHandlerContext ctx, MqttPublishMessage message)
  {
    MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
    MqttFixedHeader mqttFixedHeader = message.fixedHeader();
    MqttPublishVariableHeader variableHeader = message.variableHeader();
    ByteBuf payload = message.payload().duplicate();
    
    String topicName = variableHeader.topicName();
    int topicNameBytes = ByteBufUtil.utf8Bytes(topicName);
    
    ByteBuf propertiesBuf = encodePropertiesIfNeeded(mqttVersion, ctx
      .alloc(), message
      .variableHeader().properties());
    
    try
    {
      int variableHeaderBufferSize = 2 + topicNameBytes + (mqttFixedHeader.qosLevel().value() > 0 ? 2 : 0) + propertiesBuf.readableBytes();
      int payloadBufferSize = payload.readableBytes();
      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      
      ByteBuf buf = ctx.alloc().buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      writeVariableLengthInt(buf, variablePartSize);
      writeExactUTF8String(buf, topicName, topicNameBytes);
      if (mqttFixedHeader.qosLevel().value() > 0) {
        buf.writeShort(variableHeader.packetId());
      }
      buf.writeBytes(propertiesBuf);
      buf.writeBytes(payload);
      
      return buf;
    } finally {
      propertiesBuf.release();
    }
  }
  
  private static ByteBuf encodePubReplyMessage(ChannelHandlerContext ctx, MqttMessage message)
  {
    if ((message.variableHeader() instanceof MqttPubReplyMessageVariableHeader)) {
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      
      MqttPubReplyMessageVariableHeader variableHeader = (MqttPubReplyMessageVariableHeader)message.variableHeader();
      int msgId = variableHeader.messageId();
      



      MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
      int variableHeaderBufferSize; ByteBuf propertiesBuf; boolean includeReasonCode; int variableHeaderBufferSize; if ((mqttVersion == MqttVersion.MQTT_5) && (
        (variableHeader.reasonCode() != 0) || 
        (!variableHeader.properties().isEmpty()))) {
        ByteBuf propertiesBuf = encodeProperties(ctx.alloc(), variableHeader.properties());
        boolean includeReasonCode = true;
        variableHeaderBufferSize = 3 + propertiesBuf.readableBytes();
      } else {
        propertiesBuf = Unpooled.EMPTY_BUFFER;
        includeReasonCode = false;
        variableHeaderBufferSize = 2;
      }
      try
      {
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variableHeaderBufferSize);
        ByteBuf buf = ctx.alloc().buffer(fixedHeaderBufferSize + variableHeaderBufferSize);
        buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variableHeaderBufferSize);
        buf.writeShort(msgId);
        if (includeReasonCode) {
          buf.writeByte(variableHeader.reasonCode());
        }
        buf.writeBytes(propertiesBuf);
        
        return buf;
      } finally {
        propertiesBuf.release();
      }
    }
    return encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(ctx.alloc(), message);
  }
  


  private static ByteBuf encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(ByteBufAllocator byteBufAllocator, MqttMessage message)
  {
    MqttFixedHeader mqttFixedHeader = message.fixedHeader();
    MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader)message.variableHeader();
    int msgId = variableHeader.messageId();
    
    int variableHeaderBufferSize = 2;
    int fixedHeaderBufferSize = 1 + getVariableLengthInt(variableHeaderBufferSize);
    ByteBuf buf = byteBufAllocator.buffer(fixedHeaderBufferSize + variableHeaderBufferSize);
    buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
    writeVariableLengthInt(buf, variableHeaderBufferSize);
    buf.writeShort(msgId);
    
    return buf;
  }
  

  private static ByteBuf encodeReasonCodePlusPropertiesMessage(ChannelHandlerContext ctx, MqttMessage message)
  {
    if ((message.variableHeader() instanceof MqttReasonCodeAndPropertiesVariableHeader)) {
      MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      
      MqttReasonCodeAndPropertiesVariableHeader variableHeader = (MqttReasonCodeAndPropertiesVariableHeader)message.variableHeader();
      int variableHeaderBufferSize;
      ByteBuf propertiesBuf;
      boolean includeReasonCode;
      int variableHeaderBufferSize;
      if ((mqttVersion == MqttVersion.MQTT_5) && (
        (variableHeader.reasonCode() != 0) || 
        (!variableHeader.properties().isEmpty()))) {
        ByteBuf propertiesBuf = encodeProperties(ctx.alloc(), variableHeader.properties());
        boolean includeReasonCode = true;
        variableHeaderBufferSize = 1 + propertiesBuf.readableBytes();
      } else {
        propertiesBuf = Unpooled.EMPTY_BUFFER;
        includeReasonCode = false;
        variableHeaderBufferSize = 0;
      }
      try
      {
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variableHeaderBufferSize);
        ByteBuf buf = ctx.alloc().buffer(fixedHeaderBufferSize + variableHeaderBufferSize);
        buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variableHeaderBufferSize);
        if (includeReasonCode) {
          buf.writeByte(variableHeader.reasonCode());
        }
        buf.writeBytes(propertiesBuf);
        
        return buf;
      } finally {
        propertiesBuf.release();
      }
    }
    return encodeMessageWithOnlySingleByteFixedHeader(ctx.alloc(), message);
  }
  


  private static ByteBuf encodeMessageWithOnlySingleByteFixedHeader(ByteBufAllocator byteBufAllocator, MqttMessage message)
  {
    MqttFixedHeader mqttFixedHeader = message.fixedHeader();
    ByteBuf buf = byteBufAllocator.buffer(2);
    buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
    buf.writeByte(0);
    
    return buf;
  }
  

  private static ByteBuf encodePropertiesIfNeeded(MqttVersion mqttVersion, ByteBufAllocator byteBufAllocator, MqttProperties mqttProperties)
  {
    if (mqttVersion == MqttVersion.MQTT_5) {
      return encodeProperties(byteBufAllocator, mqttProperties);
    }
    return Unpooled.EMPTY_BUFFER;
  }
  
  /* Error */
  private static ByteBuf encodeProperties(ByteBufAllocator byteBufAllocator, MqttProperties mqttProperties)
  {
    // Byte code:
    //   0: aload_0
    //   1: invokeinterface 157 1 0
    //   6: astore_2
    //   7: aload_0
    //   8: invokeinterface 157 1 0
    //   13: astore_3
    //   14: aload_1
    //   15: invokevirtual 158	io/netty/handler/codec/mqtt/MqttProperties:listAll	()Ljava/util/Collection;
    //   18: invokeinterface 159 1 0
    //   23: astore 4
    //   25: aload 4
    //   27: invokeinterface 98 1 0
    //   32: ifeq +474 -> 506
    //   35: aload 4
    //   37: invokeinterface 99 1 0
    //   42: checkcast 160	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty
    //   45: astore 5
    //   47: aload 5
    //   49: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   52: invokestatic 162	io/netty/handler/codec/mqtt/MqttProperties$MqttPropertyType:valueOf	(I)Lio/netty/handler/codec/mqtt/MqttProperties$MqttPropertyType;
    //   55: astore 6
    //   57: getstatic 163	io/netty/handler/codec/mqtt/MqttEncoder$1:$SwitchMap$io$netty$handler$codec$mqtt$MqttProperties$MqttPropertyType	[I
    //   60: aload 6
    //   62: invokevirtual 164	io/netty/handler/codec/mqtt/MqttProperties$MqttPropertyType:ordinal	()I
    //   65: iaload
    //   66: tableswitch	default:+409->475, 1:+122->188, 2:+122->188, 3:+122->188, 4:+122->188, 5:+122->188, 6:+122->188, 7:+122->188, 8:+122->188, 9:+157->223, 10:+157->223, 11:+157->223, 12:+157->223, 13:+192->258, 14:+192->258, 15:+192->258, 16:+192->258, 17:+227->293, 18:+261->327, 19:+261->327, 20:+261->327, 21:+261->327, 22:+261->327, 23:+261->327, 24:+261->327, 25:+288->354, 26:+365->431, 27:+365->431
    //   188: aload_3
    //   189: aload 5
    //   191: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   194: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   197: aload 5
    //   199: checkcast 165	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty
    //   202: getfield 166	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty:value	Ljava/lang/Object;
    //   205: checkcast 127	java/lang/Integer
    //   208: invokevirtual 167	java/lang/Integer:byteValue	()B
    //   211: istore 7
    //   213: aload_3
    //   214: iload 7
    //   216: invokevirtual 71	io/netty/buffer/ByteBuf:writeByte	(I)Lio/netty/buffer/ByteBuf;
    //   219: pop
    //   220: goto +283 -> 503
    //   223: aload_3
    //   224: aload 5
    //   226: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   229: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   232: aload 5
    //   234: checkcast 165	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty
    //   237: getfield 166	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty:value	Ljava/lang/Object;
    //   240: checkcast 127	java/lang/Integer
    //   243: invokevirtual 168	java/lang/Integer:shortValue	()S
    //   246: istore 8
    //   248: aload_3
    //   249: iload 8
    //   251: invokevirtual 73	io/netty/buffer/ByteBuf:writeShort	(I)Lio/netty/buffer/ByteBuf;
    //   254: pop
    //   255: goto +248 -> 503
    //   258: aload_3
    //   259: aload 5
    //   261: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   264: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   267: aload 5
    //   269: checkcast 165	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty
    //   272: getfield 166	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty:value	Ljava/lang/Object;
    //   275: checkcast 127	java/lang/Integer
    //   278: invokevirtual 128	java/lang/Integer:intValue	()I
    //   281: istore 9
    //   283: aload_3
    //   284: iload 9
    //   286: invokevirtual 169	io/netty/buffer/ByteBuf:writeInt	(I)Lio/netty/buffer/ByteBuf;
    //   289: pop
    //   290: goto +213 -> 503
    //   293: aload_3
    //   294: aload 5
    //   296: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   299: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   302: aload 5
    //   304: checkcast 165	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty
    //   307: getfield 166	io/netty/handler/codec/mqtt/MqttProperties$IntegerProperty:value	Ljava/lang/Object;
    //   310: checkcast 127	java/lang/Integer
    //   313: invokevirtual 128	java/lang/Integer:intValue	()I
    //   316: istore 10
    //   318: aload_3
    //   319: iload 10
    //   321: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   324: goto +179 -> 503
    //   327: aload_3
    //   328: aload 5
    //   330: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   333: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   336: aload_3
    //   337: aload 5
    //   339: checkcast 170	io/netty/handler/codec/mqtt/MqttProperties$StringProperty
    //   342: getfield 171	io/netty/handler/codec/mqtt/MqttProperties$StringProperty:value	Ljava/lang/Object;
    //   345: checkcast 119	java/lang/String
    //   348: invokestatic 172	io/netty/handler/codec/mqtt/MqttEncoder:writeEagerUTF8String	(Lio/netty/buffer/ByteBuf;Ljava/lang/String;)V
    //   351: goto +152 -> 503
    //   354: aload 5
    //   356: checkcast 173	io/netty/handler/codec/mqtt/MqttProperties$UserProperties
    //   359: getfield 174	io/netty/handler/codec/mqtt/MqttProperties$UserProperties:value	Ljava/lang/Object;
    //   362: checkcast 175	java/util/List
    //   365: astore 11
    //   367: aload 11
    //   369: invokeinterface 97 1 0
    //   374: astore 12
    //   376: aload 12
    //   378: invokeinterface 98 1 0
    //   383: ifeq +45 -> 428
    //   386: aload 12
    //   388: invokeinterface 99 1 0
    //   393: checkcast 176	io/netty/handler/codec/mqtt/MqttProperties$StringPair
    //   396: astore 13
    //   398: aload_3
    //   399: aload 5
    //   401: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   404: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   407: aload_3
    //   408: aload 13
    //   410: getfield 177	io/netty/handler/codec/mqtt/MqttProperties$StringPair:key	Ljava/lang/String;
    //   413: invokestatic 172	io/netty/handler/codec/mqtt/MqttEncoder:writeEagerUTF8String	(Lio/netty/buffer/ByteBuf;Ljava/lang/String;)V
    //   416: aload_3
    //   417: aload 13
    //   419: getfield 178	io/netty/handler/codec/mqtt/MqttProperties$StringPair:value	Ljava/lang/String;
    //   422: invokestatic 172	io/netty/handler/codec/mqtt/MqttEncoder:writeEagerUTF8String	(Lio/netty/buffer/ByteBuf;Ljava/lang/String;)V
    //   425: goto -49 -> 376
    //   428: goto +75 -> 503
    //   431: aload_3
    //   432: aload 5
    //   434: getfield 161	io/netty/handler/codec/mqtt/MqttProperties$MqttProperty:propertyId	I
    //   437: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   440: aload 5
    //   442: checkcast 179	io/netty/handler/codec/mqtt/MqttProperties$BinaryProperty
    //   445: getfield 180	io/netty/handler/codec/mqtt/MqttProperties$BinaryProperty:value	Ljava/lang/Object;
    //   448: checkcast 181	[B
    //   451: astore 12
    //   453: aload_3
    //   454: aload 12
    //   456: arraylength
    //   457: invokevirtual 73	io/netty/buffer/ByteBuf:writeShort	(I)Lio/netty/buffer/ByteBuf;
    //   460: pop
    //   461: aload_3
    //   462: aload 12
    //   464: iconst_0
    //   465: aload 12
    //   467: arraylength
    //   468: invokevirtual 79	io/netty/buffer/ByteBuf:writeBytes	([BII)Lio/netty/buffer/ByteBuf;
    //   471: pop
    //   472: goto +31 -> 503
    //   475: new 46	io/netty/handler/codec/EncoderException
    //   478: dup
    //   479: new 29	java/lang/StringBuilder
    //   482: dup
    //   483: invokespecial 30	java/lang/StringBuilder:<init>	()V
    //   486: ldc -74
    //   488: invokevirtual 32	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   491: aload 6
    //   493: invokevirtual 183	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   496: invokevirtual 35	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   499: invokespecial 48	io/netty/handler/codec/EncoderException:<init>	(Ljava/lang/String;)V
    //   502: athrow
    //   503: goto -478 -> 25
    //   506: aload_2
    //   507: aload_3
    //   508: invokevirtual 66	io/netty/buffer/ByteBuf:readableBytes	()I
    //   511: invokestatic 72	io/netty/handler/codec/mqtt/MqttEncoder:writeVariableLengthInt	(Lio/netty/buffer/ByteBuf;I)V
    //   514: aload_2
    //   515: aload_3
    //   516: invokevirtual 77	io/netty/buffer/ByteBuf:writeBytes	(Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;
    //   519: pop
    //   520: aload_2
    //   521: astore 4
    //   523: aload_3
    //   524: invokevirtual 80	io/netty/buffer/ByteBuf:release	()Z
    //   527: pop
    //   528: aload 4
    //   530: areturn
    //   531: astore 14
    //   533: aload_3
    //   534: invokevirtual 80	io/netty/buffer/ByteBuf:release	()Z
    //   537: pop
    //   538: aload 14
    //   540: athrow
    //   541: astore_3
    //   542: aload_2
    //   543: invokevirtual 80	io/netty/buffer/ByteBuf:release	()Z
    //   546: pop
    //   547: aload_3
    //   548: athrow
    // Line number table:
    //   Java source line #585	-> byte code offset #0
    //   Java source line #588	-> byte code offset #7
    //   Java source line #590	-> byte code offset #14
    //   Java source line #591	-> byte code offset #47
    //   Java source line #592	-> byte code offset #52
    //   Java source line #593	-> byte code offset #57
    //   Java source line #602	-> byte code offset #188
    //   Java source line #603	-> byte code offset #197
    //   Java source line #604	-> byte code offset #213
    //   Java source line #605	-> byte code offset #220
    //   Java source line #610	-> byte code offset #223
    //   Java source line #611	-> byte code offset #232
    //   Java source line #612	-> byte code offset #243
    //   Java source line #613	-> byte code offset #248
    //   Java source line #614	-> byte code offset #255
    //   Java source line #619	-> byte code offset #258
    //   Java source line #620	-> byte code offset #267
    //   Java source line #621	-> byte code offset #283
    //   Java source line #622	-> byte code offset #290
    //   Java source line #624	-> byte code offset #293
    //   Java source line #625	-> byte code offset #302
    //   Java source line #626	-> byte code offset #318
    //   Java source line #627	-> byte code offset #324
    //   Java source line #635	-> byte code offset #327
    //   Java source line #636	-> byte code offset #336
    //   Java source line #637	-> byte code offset #351
    //   Java source line #639	-> byte code offset #354
    //   Java source line #641	-> byte code offset #367
    //   Java source line #642	-> byte code offset #398
    //   Java source line #643	-> byte code offset #407
    //   Java source line #644	-> byte code offset #416
    //   Java source line #645	-> byte code offset #425
    //   Java source line #646	-> byte code offset #428
    //   Java source line #649	-> byte code offset #431
    //   Java source line #650	-> byte code offset #440
    //   Java source line #651	-> byte code offset #453
    //   Java source line #652	-> byte code offset #461
    //   Java source line #653	-> byte code offset #472
    //   Java source line #656	-> byte code offset #475
    //   Java source line #658	-> byte code offset #503
    //   Java source line #659	-> byte code offset #506
    //   Java source line #660	-> byte code offset #514
    //   Java source line #662	-> byte code offset #520
    //   Java source line #664	-> byte code offset #523
    //   Java source line #662	-> byte code offset #528
    //   Java source line #664	-> byte code offset #531
    //   Java source line #665	-> byte code offset #538
    //   Java source line #666	-> byte code offset #541
    //   Java source line #667	-> byte code offset #542
    //   Java source line #668	-> byte code offset #547
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	549	0	byteBufAllocator	ByteBufAllocator
    //   0	549	1	mqttProperties	MqttProperties
    //   6	537	2	propertiesHeaderBuf	ByteBuf
    //   13	521	3	propertiesBuf	ByteBuf
    //   541	7	3	e	RuntimeException
    //   23	506	4	localObject1	Object
    //   45	396	5	property	MqttProperties.MqttProperty
    //   55	437	6	propertyType	MqttProperties.MqttPropertyType
    //   211	4	7	bytePropValue	byte
    //   246	4	8	twoBytesInPropValue	short
    //   281	4	9	fourBytesIntPropValue	int
    //   316	4	10	vbi	int
    //   365	3	11	pairs	List<MqttProperties.StringPair>
    //   374	13	12	localIterator	Iterator
    //   451	15	12	binaryPropValue	byte[]
    //   396	22	13	pair	MqttProperties.StringPair
    //   531	8	14	localObject2	Object
    // Exception table:
    //   from	to	target	type
    //   14	523	531	finally
    //   531	533	531	finally
    //   7	528	541	java/lang/RuntimeException
    //   531	541	541	java/lang/RuntimeException
  }
  
  private static int getFixedHeaderByte1(MqttFixedHeader header)
  {
    int ret = 0;
    ret |= header.messageType().value() << 4;
    if (header.isDup()) {
      ret |= 0x8;
    }
    ret |= header.qosLevel().value() << 1;
    if (header.isRetain()) {
      ret |= 0x1;
    }
    return ret;
  }
  
  private static void writeVariableLengthInt(ByteBuf buf, int num) {
    do {
      int digit = num % 128;
      num /= 128;
      if (num > 0) {
        digit |= 0x80;
      }
      buf.writeByte(digit);
    } while (num > 0);
  }
  
  private static int nullableUtf8Bytes(String s) {
    return s == null ? 0 : ByteBufUtil.utf8Bytes(s);
  }
  
  private static int nullableMaxUtf8Bytes(String s) {
    return s == null ? 0 : ByteBufUtil.utf8MaxBytes(s);
  }
  
  private static void writeExactUTF8String(ByteBuf buf, String s, int utf8Length) {
    buf.ensureWritable(utf8Length + 2);
    buf.writeShort(utf8Length);
    if (utf8Length > 0) {
      int writtenUtf8Length = ByteBufUtil.reserveAndWriteUtf8(buf, s, utf8Length);
      assert (writtenUtf8Length == utf8Length);
    }
  }
  
  private static void writeEagerUTF8String(ByteBuf buf, String s) {
    int maxUtf8Length = nullableMaxUtf8Bytes(s);
    buf.ensureWritable(maxUtf8Length + 2);
    int writerIndex = buf.writerIndex();
    int startUtf8String = writerIndex + 2;
    buf.writerIndex(startUtf8String);
    int utf8Length = s != null ? ByteBufUtil.reserveAndWriteUtf8(buf, s, maxUtf8Length) : 0;
    buf.setShort(writerIndex, utf8Length);
  }
  
  private static void writeUnsafeUTF8String(ByteBuf buf, String s) {
    int writerIndex = buf.writerIndex();
    int startUtf8String = writerIndex + 2;
    
    buf.writerIndex(startUtf8String);
    int utf8Length = s != null ? ByteBufUtil.reserveAndWriteUtf8(buf, s, 0) : 0;
    buf.setShort(writerIndex, utf8Length);
  }
  
  private static int getVariableLengthInt(int num) {
    int count = 0;
    do {
      num /= 128;
      count++;
    } while (num > 0);
    return count;
  }
}
