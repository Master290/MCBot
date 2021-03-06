package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;



























public final class Snappy
{
  private static final int MAX_HT_SIZE = 16384;
  private static final int MIN_COMPRESSIBLE_BYTES = 15;
  private static final int PREAMBLE_NOT_FULL = -1;
  private static final int NOT_ENOUGH_INPUT = -1;
  private static final int LITERAL = 0;
  private static final int COPY_1_BYTE_OFFSET = 1;
  private static final int COPY_2_BYTE_OFFSET = 2;
  private static final int COPY_4_BYTE_OFFSET = 3;
  private State state = State.READY;
  private byte tag;
  private int written;
  public Snappy() {}
  
  private static enum State { READY, 
    READING_PREAMBLE, 
    READING_TAG, 
    READING_LITERAL, 
    READING_COPY;
    
    private State() {} }
  
  public void reset() { state = State.READY;
    tag = 0;
    written = 0;
  }
  
  public void encode(ByteBuf in, ByteBuf out, int length)
  {
    for (int i = 0;; i++) {
      int b = length >>> i * 7;
      if ((b & 0xFFFFFF80) != 0) {
        out.writeByte(b & 0x7F | 0x80);
      } else {
        out.writeByte(b);
        break;
      }
    }
    
    int inIndex = in.readerIndex();
    int baseIndex = inIndex;
    
    short[] table = getHashTable(length);
    int shift = Integer.numberOfLeadingZeros(table.length) + 1;
    
    int nextEmit = inIndex;
    
    if (length - inIndex >= 15) {
      int nextHash = hash(in, ++inIndex, shift);
      for (;;) {
        int skip = 32;
        

        int nextIndex = inIndex;
        int candidate;
        do { inIndex = nextIndex;
          int hash = nextHash;
          int bytesBetweenHashLookups = skip++ >> 5;
          nextIndex = inIndex + bytesBetweenHashLookups;
          

          if (nextIndex > length - 4) {
            break;
          }
          
          nextHash = hash(in, nextIndex, shift);
          
          candidate = baseIndex + table[hash];
          
          table[hash] = ((short)(inIndex - baseIndex));
        }
        while (in.getInt(inIndex) != in.getInt(candidate));
        
        encodeLiteral(in, out, inIndex - nextEmit);
        int insertTail;
        do
        {
          int base = inIndex;
          int matched = 4 + findMatchingLength(in, candidate + 4, inIndex + 4, length);
          inIndex += matched;
          int offset = base - candidate;
          encodeCopy(out, offset, matched);
          in.readerIndex(in.readerIndex() + matched);
          insertTail = inIndex - 1;
          nextEmit = inIndex;
          if (inIndex >= length - 4) {
            break;
          }
          
          int prevHash = hash(in, insertTail, shift);
          table[prevHash] = ((short)(inIndex - baseIndex - 1));
          int currentHash = hash(in, insertTail + 1, shift);
          candidate = baseIndex + table[currentHash];
          table[currentHash] = ((short)(inIndex - baseIndex));
        }
        while (in.getInt(insertTail + 1) == in.getInt(candidate));
        
        nextHash = hash(in, insertTail + 2, shift);
        inIndex++;
      }
    }
    

    if (nextEmit < length) {
      encodeLiteral(in, out, length - nextEmit);
    }
  }
  









  private static int hash(ByteBuf in, int index, int shift)
  {
    return in.getInt(index) * 506832829 >>> shift;
  }
  





  private static short[] getHashTable(int inputSize)
  {
    int htSize = 256;
    while ((htSize < 16384) && (htSize < inputSize)) {
      htSize <<= 1;
    }
    return new short[htSize];
  }
  










  private static int findMatchingLength(ByteBuf in, int minIndex, int inIndex, int maxIndex)
  {
    int matched = 0;
    
    while ((inIndex <= maxIndex - 4) && 
      (in.getInt(inIndex) == in.getInt(minIndex + matched))) {
      inIndex += 4;
      matched += 4;
    }
    
    while ((inIndex < maxIndex) && (in.getByte(minIndex + matched) == in.getByte(inIndex))) {
      inIndex++;
      matched++;
    }
    
    return matched;
  }
  







  private static int bitsToEncode(int value)
  {
    int highestOneBit = Integer.highestOneBit(value);
    int bitLength = 0;
    while (highestOneBit >>= 1 != 0) {
      bitLength++;
    }
    
    return bitLength;
  }
  








  static void encodeLiteral(ByteBuf in, ByteBuf out, int length)
  {
    if (length < 61) {
      out.writeByte(length - 1 << 2);
    } else {
      int bitLength = bitsToEncode(length - 1);
      int bytesToEncode = 1 + bitLength / 8;
      out.writeByte(59 + bytesToEncode << 2);
      for (int i = 0; i < bytesToEncode; i++) {
        out.writeByte(length - 1 >> i * 8 & 0xFF);
      }
    }
    
    out.writeBytes(in, length);
  }
  
  private static void encodeCopyWithOffset(ByteBuf out, int offset, int length) {
    if ((length < 12) && (offset < 2048)) {
      out.writeByte(0x1 | length - 4 << 2 | offset >> 8 << 5);
      out.writeByte(offset & 0xFF);
    } else {
      out.writeByte(0x2 | length - 1 << 2);
      out.writeByte(offset & 0xFF);
      out.writeByte(offset >> 8 & 0xFF);
    }
  }
  






  private static void encodeCopy(ByteBuf out, int offset, int length)
  {
    while (length >= 68) {
      encodeCopyWithOffset(out, offset, 64);
      length -= 64;
    }
    
    if (length > 64) {
      encodeCopyWithOffset(out, offset, 60);
      length -= 60;
    }
    
    encodeCopyWithOffset(out, offset, length);
  }
  
  public void decode(ByteBuf in, ByteBuf out) {
    while (in.isReadable()) {
      switch (1.$SwitchMap$io$netty$handler$codec$compression$Snappy$State[state.ordinal()]) {
      case 1: 
        state = State.READING_PREAMBLE;
      
      case 2: 
        int uncompressedLength = readPreamble(in);
        if (uncompressedLength == -1)
        {
          return;
        }
        if (uncompressedLength == 0)
        {
          state = State.READY;
          return;
        }
        out.ensureWritable(uncompressedLength);
        state = State.READING_TAG;
      
      case 3: 
        if (!in.isReadable()) {
          return;
        }
        tag = in.readByte();
        switch (tag & 0x3) {
        case 0: 
          state = State.READING_LITERAL;
          break;
        case 1: 
        case 2: 
        case 3: 
          state = State.READING_COPY;
        }
        
        break;
      case 4: 
        int literalWritten = decodeLiteral(tag, in, out);
        if (literalWritten != -1) {
          state = State.READING_TAG;
          written += literalWritten;
        }
        else
        {
          return;
        }
        break;
      case 5: 
        switch (tag & 0x3) {
        case 1: 
          int decodeWritten = decodeCopyWith1ByteOffset(tag, in, out, written);
          if (decodeWritten != -1) {
            state = State.READING_TAG;
            written += decodeWritten;
          }
          else {
            return;
          }
          break;
        case 2: 
          int decodeWritten = decodeCopyWith2ByteOffset(tag, in, out, written);
          if (decodeWritten != -1) {
            state = State.READING_TAG;
            written += decodeWritten;
          }
          else {
            return;
          }
          break;
        case 3: 
          int decodeWritten = decodeCopyWith4ByteOffset(tag, in, out, written);
          if (decodeWritten != -1) {
            state = State.READING_TAG;
            written += decodeWritten;
          }
          else
          {
            return;
          }
          

          break;
        }
        
        
        break;
      }
      
    }
  }
  

  private static int readPreamble(ByteBuf in)
  {
    int length = 0;
    int byteIndex = 0;
    while (in.isReadable()) {
      int current = in.readUnsignedByte();
      length |= (current & 0x7F) << byteIndex++ * 7;
      if ((current & 0x80) == 0) {
        return length;
      }
      
      if (byteIndex >= 4) {
        throw new DecompressionException("Preamble is greater than 4 bytes");
      }
    }
    
    return 0;
  }
  










  static int decodeLiteral(byte tag, ByteBuf in, ByteBuf out)
  {
    in.markReaderIndex();
    int length;
    int length; int length; int length; int length; switch (tag >> 2 & 0x3F) {
    case 60: 
      if (!in.isReadable()) {
        return -1;
      }
      length = in.readUnsignedByte();
      break;
    case 61: 
      if (in.readableBytes() < 2) {
        return -1;
      }
      length = in.readUnsignedShortLE();
      break;
    case 62: 
      if (in.readableBytes() < 3) {
        return -1;
      }
      length = in.readUnsignedMediumLE();
      break;
    case 63: 
      if (in.readableBytes() < 4) {
        return -1;
      }
      length = in.readIntLE();
      break;
    default: 
      length = tag >> 2 & 0x3F;
    }
    length++;
    
    if (in.readableBytes() < length) {
      in.resetReaderIndex();
      return -1;
    }
    
    out.writeBytes(in, length);
    return length;
  }
  












  private static int decodeCopyWith1ByteOffset(byte tag, ByteBuf in, ByteBuf out, int writtenSoFar)
  {
    if (!in.isReadable()) {
      return -1;
    }
    
    int initialIndex = out.writerIndex();
    int length = 4 + ((tag & 0x1C) >> 2);
    int offset = (tag & 0xE0) << 8 >> 5 | in.readUnsignedByte();
    
    validateOffset(offset, writtenSoFar);
    
    out.markReaderIndex();
    if (offset < length) {
      for (int copies = length / offset; 
          copies > 0; copies--) {
        out.readerIndex(initialIndex - offset);
        out.readBytes(out, offset);
      }
      if (length % offset != 0) {
        out.readerIndex(initialIndex - offset);
        out.readBytes(out, length % offset);
      }
    } else {
      out.readerIndex(initialIndex - offset);
      out.readBytes(out, length);
    }
    out.resetReaderIndex();
    
    return length;
  }
  












  private static int decodeCopyWith2ByteOffset(byte tag, ByteBuf in, ByteBuf out, int writtenSoFar)
  {
    if (in.readableBytes() < 2) {
      return -1;
    }
    
    int initialIndex = out.writerIndex();
    int length = 1 + (tag >> 2 & 0x3F);
    int offset = in.readUnsignedShortLE();
    
    validateOffset(offset, writtenSoFar);
    
    out.markReaderIndex();
    if (offset < length) {
      for (int copies = length / offset; 
          copies > 0; copies--) {
        out.readerIndex(initialIndex - offset);
        out.readBytes(out, offset);
      }
      if (length % offset != 0) {
        out.readerIndex(initialIndex - offset);
        out.readBytes(out, length % offset);
      }
    } else {
      out.readerIndex(initialIndex - offset);
      out.readBytes(out, length);
    }
    out.resetReaderIndex();
    
    return length;
  }
  












  private static int decodeCopyWith4ByteOffset(byte tag, ByteBuf in, ByteBuf out, int writtenSoFar)
  {
    if (in.readableBytes() < 4) {
      return -1;
    }
    
    int initialIndex = out.writerIndex();
    int length = 1 + (tag >> 2 & 0x3F);
    int offset = in.readIntLE();
    
    validateOffset(offset, writtenSoFar);
    
    out.markReaderIndex();
    if (offset < length) {
      for (int copies = length / offset; 
          copies > 0; copies--) {
        out.readerIndex(initialIndex - offset);
        out.readBytes(out, offset);
      }
      if (length % offset != 0) {
        out.readerIndex(initialIndex - offset);
        out.readBytes(out, length % offset);
      }
    } else {
      out.readerIndex(initialIndex - offset);
      out.readBytes(out, length);
    }
    out.resetReaderIndex();
    
    return length;
  }
  








  private static void validateOffset(int offset, int chunkSizeSoFar)
  {
    if (offset == 0) {
      throw new DecompressionException("Offset is less than minimum permissible value");
    }
    
    if (offset < 0)
    {
      throw new DecompressionException("Offset is greater than maximum value supported by this implementation");
    }
    
    if (offset > chunkSizeSoFar) {
      throw new DecompressionException("Offset exceeds size of chunk");
    }
  }
  





  static int calculateChecksum(ByteBuf data)
  {
    return calculateChecksum(data, data.readerIndex(), data.readableBytes());
  }
  





  static int calculateChecksum(ByteBuf data, int offset, int length)
  {
    Crc32c crc32 = new Crc32c();
    try {
      crc32.update(data, offset, length);
      return maskChecksum(crc32.getValue());
    } finally {
      crc32.reset();
    }
  }
  








  static void validateChecksum(int expectedChecksum, ByteBuf data)
  {
    validateChecksum(expectedChecksum, data, data.readerIndex(), data.readableBytes());
  }
  








  static void validateChecksum(int expectedChecksum, ByteBuf data, int offset, int length)
  {
    int actualChecksum = calculateChecksum(data, offset, length);
    if (actualChecksum != expectedChecksum)
    {

      throw new DecompressionException("mismatching checksum: " + Integer.toHexString(actualChecksum) + " (expected: " + Integer.toHexString(expectedChecksum) + ')');
    }
  }
  










  static int maskChecksum(long checksum)
  {
    return (int)((checksum >> 15 | checksum << 17) + -1568478504L);
  }
}
