package io.netty.handler.codec.compression;








final class Bzip2HuffmanStageDecoder
{
  private final Bzip2BitReader reader;
  






  byte[] selectors;
  






  private final int[] minimumLengths;
  






  private final int[][] codeBases;
  





  private final int[][] codeLimits;
  





  private final int[][] codeSymbols;
  





  private int currentTable;
  





  private int groupIndex = -1;
  



  private int groupPosition = -1;
  



  final int totalTables;
  



  final int alphabetSize;
  



  final Bzip2MoveToFrontTable tableMTF = new Bzip2MoveToFrontTable();
  

  int currentSelector;
  

  final byte[][] tableCodeLengths;
  

  int currentGroup;
  

  int currentLength = -1;
  int currentAlpha;
  boolean modifyLength;
  
  Bzip2HuffmanStageDecoder(Bzip2BitReader reader, int totalTables, int alphabetSize) {
    this.reader = reader;
    this.totalTables = totalTables;
    this.alphabetSize = alphabetSize;
    
    minimumLengths = new int[totalTables];
    codeBases = new int[totalTables][25];
    codeLimits = new int[totalTables][24];
    codeSymbols = new int[totalTables]['Ă'];
    tableCodeLengths = new byte[totalTables]['Ă'];
  }
  


  void createHuffmanDecodingTables()
  {
    int alphabetSize = this.alphabetSize;
    
    for (int table = 0; table < tableCodeLengths.length; table++) {
      int[] tableBases = codeBases[table];
      int[] tableLimits = codeLimits[table];
      int[] tableSymbols = codeSymbols[table];
      byte[] codeLengths = tableCodeLengths[table];
      
      int minimumLength = 23;
      int maximumLength = 0;
      

      for (int i = 0; i < alphabetSize; i++) {
        byte currLength = codeLengths[i];
        maximumLength = Math.max(currLength, maximumLength);
        minimumLength = Math.min(currLength, minimumLength);
      }
      minimumLengths[table] = minimumLength;
      

      for (int i = 0; i < alphabetSize; i++) {
        tableBases[(codeLengths[i] + 1)] += 1;
      }
      int i = 1; for (int b = tableBases[0]; i < 25; i++) {
        b += tableBases[i];
        tableBases[i] = b;
      }
      


      int i = minimumLength; for (int code = 0; i <= maximumLength; i++) {
        int base = code;
        code += tableBases[(i + 1)] - tableBases[i];
        tableBases[i] = (base - tableBases[i]);
        tableLimits[i] = (code - 1);
        code <<= 1;
      }
      

      int bitLength = minimumLength; for (int codeIndex = 0; bitLength <= maximumLength; bitLength++) {
        for (int symbol = 0; symbol < alphabetSize; symbol++) {
          if (codeLengths[symbol] == bitLength) {
            tableSymbols[(codeIndex++)] = symbol;
          }
        }
      }
    }
    
    currentTable = selectors[0];
  }
  




  int nextSymbol()
  {
    if (++groupPosition % 50 == 0) {
      groupIndex += 1;
      if (groupIndex == selectors.length) {
        throw new DecompressionException("error decoding block");
      }
      this.currentTable = (selectors[groupIndex] & 0xFF);
    }
    
    Bzip2BitReader reader = this.reader;
    int currentTable = this.currentTable;
    int[] tableLimits = codeLimits[currentTable];
    int[] tableBases = codeBases[currentTable];
    int[] tableSymbols = codeSymbols[currentTable];
    int codeLength = minimumLengths[currentTable];
    


    int codeBits = reader.readBits(codeLength);
    for (; codeLength <= 23; codeLength++) {
      if (codeBits <= tableLimits[codeLength])
      {
        return tableSymbols[(codeBits - tableBases[codeLength])];
      }
      codeBits = codeBits << 1 | reader.readBits(1);
    }
    
    throw new DecompressionException("a valid code was not recognised");
  }
}
