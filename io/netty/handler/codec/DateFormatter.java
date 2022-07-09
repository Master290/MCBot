package io.netty.handler.codec;

import io.netty.util.AsciiString;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectUtil;
import java.util.BitSet;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;






































public final class DateFormatter
{
  private static final BitSet DELIMITERS = new BitSet();
  
  static { DELIMITERS.set(9);
    for (char c = ' '; c <= '/'; c = (char)(c + '\001')) {
      DELIMITERS.set(c);
    }
    for (char c = ';'; c <= '@'; c = (char)(c + '\001')) {
      DELIMITERS.set(c);
    }
    for (char c = '['; c <= '`'; c = (char)(c + '\001')) {
      DELIMITERS.set(c);
    }
    for (char c = '{'; c <= '~'; c = (char)(c + '\001')) {
      DELIMITERS.set(c);
    }
  }
  
  private static final String[] DAY_OF_WEEK_TO_SHORT_NAME = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
  

  private static final String[] CALENDAR_MONTH_TO_SHORT_NAME = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
  

  private static final FastThreadLocal<DateFormatter> INSTANCES = new FastThreadLocal()
  {
    protected DateFormatter initialValue()
    {
      return new DateFormatter(null);
    }
  };
  




  public static Date parseHttpDate(CharSequence txt)
  {
    return parseHttpDate(txt, 0, txt.length());
  }
  






  public static Date parseHttpDate(CharSequence txt, int start, int end)
  {
    int length = end - start;
    if (length == 0)
      return null;
    if (length < 0)
      throw new IllegalArgumentException("Can't have end < start");
    if (length > 64) {
      throw new IllegalArgumentException("Can't parse more than 64 chars, looks like a user error or a malformed header");
    }
    
    return formatter().parse0((CharSequence)ObjectUtil.checkNotNull(txt, "txt"), start, end);
  }
  




  public static String format(Date date)
  {
    return formatter().format0((Date)ObjectUtil.checkNotNull(date, "date"));
  }
  





  public static StringBuilder append(Date date, StringBuilder sb)
  {
    return formatter().append0((Date)ObjectUtil.checkNotNull(date, "date"), (StringBuilder)ObjectUtil.checkNotNull(sb, "sb"));
  }
  
  private static DateFormatter formatter() {
    DateFormatter formatter = (DateFormatter)INSTANCES.get();
    formatter.reset();
    return formatter;
  }
  
  private static boolean isDelim(char c)
  {
    return DELIMITERS.get(c);
  }
  
  private static boolean isDigit(char c) {
    return (c >= '0') && (c <= '9');
  }
  
  private static int getNumericalValue(char c) {
    return c - '0';
  }
  
  private final GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
  private final StringBuilder sb = new StringBuilder(29);
  private boolean timeFound;
  private int hours;
  private int minutes;
  private int seconds;
  private boolean dayOfMonthFound;
  private int dayOfMonth;
  private boolean monthFound;
  private int month;
  private boolean yearFound;
  private int year;
  
  private DateFormatter() {
    reset();
  }
  
  public void reset() {
    timeFound = false;
    hours = -1;
    minutes = -1;
    seconds = -1;
    dayOfMonthFound = false;
    dayOfMonth = -1;
    monthFound = false;
    month = -1;
    yearFound = false;
    year = -1;
    cal.clear();
    sb.setLength(0);
  }
  
  private boolean tryParseTime(CharSequence txt, int tokenStart, int tokenEnd) {
    int len = tokenEnd - tokenStart;
    

    if ((len < 5) || (len > 8)) {
      return false;
    }
    
    int localHours = -1;
    int localMinutes = -1;
    int localSeconds = -1;
    int currentPartNumber = 0;
    int currentPartValue = 0;
    int numDigits = 0;
    
    for (int i = tokenStart; i < tokenEnd; i++) {
      char c = txt.charAt(i);
      if (isDigit(c)) {
        currentPartValue = currentPartValue * 10 + getNumericalValue(c);
        numDigits++; if (numDigits > 2) {
          return false;
        }
      } else if (c == ':') {
        if (numDigits == 0)
        {
          return false;
        }
        switch (currentPartNumber)
        {
        case 0: 
          localHours = currentPartValue;
          break;
        
        case 1: 
          localMinutes = currentPartValue;
          break;
        
        default: 
          return false;
        }
        currentPartValue = 0;
        currentPartNumber++;
        numDigits = 0;
      }
      else {
        return false;
      }
    }
    
    if (numDigits > 0)
    {
      localSeconds = currentPartValue;
    }
    
    if ((localHours >= 0) && (localMinutes >= 0) && (localSeconds >= 0)) {
      hours = localHours;
      minutes = localMinutes;
      seconds = localSeconds;
      return true;
    }
    
    return false;
  }
  
  private boolean tryParseDayOfMonth(CharSequence txt, int tokenStart, int tokenEnd) {
    int len = tokenEnd - tokenStart;
    
    if (len == 1) {
      char c0 = txt.charAt(tokenStart);
      if (isDigit(c0)) {
        dayOfMonth = getNumericalValue(c0);
        return true;
      }
    }
    else if (len == 2) {
      char c0 = txt.charAt(tokenStart);
      char c1 = txt.charAt(tokenStart + 1);
      if ((isDigit(c0)) && (isDigit(c1))) {
        dayOfMonth = (getNumericalValue(c0) * 10 + getNumericalValue(c1));
        return true;
      }
    }
    
    return false;
  }
  
  private boolean tryParseMonth(CharSequence txt, int tokenStart, int tokenEnd) {
    int len = tokenEnd - tokenStart;
    
    if (len != 3) {
      return false;
    }
    
    char monthChar1 = AsciiString.toLowerCase(txt.charAt(tokenStart));
    char monthChar2 = AsciiString.toLowerCase(txt.charAt(tokenStart + 1));
    char monthChar3 = AsciiString.toLowerCase(txt.charAt(tokenStart + 2));
    
    if ((monthChar1 == 'j') && (monthChar2 == 'a') && (monthChar3 == 'n')) {
      month = 0;
    } else if ((monthChar1 == 'f') && (monthChar2 == 'e') && (monthChar3 == 'b')) {
      month = 1;
    } else if ((monthChar1 == 'm') && (monthChar2 == 'a') && (monthChar3 == 'r')) {
      month = 2;
    } else if ((monthChar1 == 'a') && (monthChar2 == 'p') && (monthChar3 == 'r')) {
      month = 3;
    } else if ((monthChar1 == 'm') && (monthChar2 == 'a') && (monthChar3 == 'y')) {
      month = 4;
    } else if ((monthChar1 == 'j') && (monthChar2 == 'u') && (monthChar3 == 'n')) {
      month = 5;
    } else if ((monthChar1 == 'j') && (monthChar2 == 'u') && (monthChar3 == 'l')) {
      month = 6;
    } else if ((monthChar1 == 'a') && (monthChar2 == 'u') && (monthChar3 == 'g')) {
      month = 7;
    } else if ((monthChar1 == 's') && (monthChar2 == 'e') && (monthChar3 == 'p')) {
      month = 8;
    } else if ((monthChar1 == 'o') && (monthChar2 == 'c') && (monthChar3 == 't')) {
      month = 9;
    } else if ((monthChar1 == 'n') && (monthChar2 == 'o') && (monthChar3 == 'v')) {
      month = 10;
    } else if ((monthChar1 == 'd') && (monthChar2 == 'e') && (monthChar3 == 'c')) {
      month = 11;
    } else {
      return false;
    }
    
    return true;
  }
  
  private boolean tryParseYear(CharSequence txt, int tokenStart, int tokenEnd) {
    int len = tokenEnd - tokenStart;
    
    if (len == 2) {
      char c0 = txt.charAt(tokenStart);
      char c1 = txt.charAt(tokenStart + 1);
      if ((isDigit(c0)) && (isDigit(c1))) {
        year = (getNumericalValue(c0) * 10 + getNumericalValue(c1));
        return true;
      }
    }
    else if (len == 4) {
      char c0 = txt.charAt(tokenStart);
      char c1 = txt.charAt(tokenStart + 1);
      char c2 = txt.charAt(tokenStart + 2);
      char c3 = txt.charAt(tokenStart + 3);
      if ((isDigit(c0)) && (isDigit(c1)) && (isDigit(c2)) && (isDigit(c3)))
      {


        year = (getNumericalValue(c0) * 1000 + getNumericalValue(c1) * 100 + getNumericalValue(c2) * 10 + getNumericalValue(c3));
        return true;
      }
    }
    
    return false;
  }
  
  private boolean parseToken(CharSequence txt, int tokenStart, int tokenEnd)
  {
    if (!timeFound) {
      timeFound = tryParseTime(txt, tokenStart, tokenEnd);
      if (timeFound) {
        return (dayOfMonthFound) && (monthFound) && (yearFound);
      }
    }
    
    if (!dayOfMonthFound) {
      dayOfMonthFound = tryParseDayOfMonth(txt, tokenStart, tokenEnd);
      if (dayOfMonthFound) {
        return (timeFound) && (monthFound) && (yearFound);
      }
    }
    
    if (!monthFound) {
      monthFound = tryParseMonth(txt, tokenStart, tokenEnd);
      if (monthFound) {
        return (timeFound) && (dayOfMonthFound) && (yearFound);
      }
    }
    
    if (!yearFound) {
      yearFound = tryParseYear(txt, tokenStart, tokenEnd);
    }
    return (timeFound) && (dayOfMonthFound) && (monthFound) && (yearFound);
  }
  
  private Date parse0(CharSequence txt, int start, int end) {
    boolean allPartsFound = parse1(txt, start, end);
    return (allPartsFound) && (normalizeAndValidate()) ? computeDate() : null;
  }
  
  private boolean parse1(CharSequence txt, int start, int end)
  {
    int tokenStart = -1;
    
    for (int i = start; i < end; i++) {
      char c = txt.charAt(i);
      
      if (isDelim(c)) {
        if (tokenStart != -1)
        {
          if (parseToken(txt, tokenStart, i)) {
            return true;
          }
          tokenStart = -1;
        }
      } else if (tokenStart == -1)
      {
        tokenStart = i;
      }
    }
    

    return (tokenStart != -1) && (parseToken(txt, tokenStart, txt.length()));
  }
  
  private boolean normalizeAndValidate() {
    if ((dayOfMonth < 1) || (dayOfMonth > 31) || (hours > 23) || (minutes > 59) || (seconds > 59))
    {



      return false;
    }
    
    if ((year >= 70) && (year <= 99)) {
      year += 1900;
    } else if ((year >= 0) && (year < 70)) {
      year += 2000;
    } else if (year < 1601)
    {
      return false;
    }
    return true;
  }
  
  private Date computeDate() {
    cal.set(5, dayOfMonth);
    cal.set(2, month);
    cal.set(1, year);
    cal.set(11, hours);
    cal.set(12, minutes);
    cal.set(13, seconds);
    return cal.getTime();
  }
  
  private String format0(Date date) {
    append0(date, sb);
    return sb.toString();
  }
  
  private StringBuilder append0(Date date, StringBuilder sb) {
    cal.setTime(date);
    
    sb.append(DAY_OF_WEEK_TO_SHORT_NAME[(cal.get(7) - 1)]).append(", ");
    appendZeroLeftPadded(cal.get(5), sb).append(' ');
    sb.append(CALENDAR_MONTH_TO_SHORT_NAME[cal.get(2)]).append(' ');
    sb.append(cal.get(1)).append(' ');
    appendZeroLeftPadded(cal.get(11), sb).append(':');
    appendZeroLeftPadded(cal.get(12), sb).append(':');
    return appendZeroLeftPadded(cal.get(13), sb).append(" GMT");
  }
  
  private static StringBuilder appendZeroLeftPadded(int value, StringBuilder sb) {
    if (value < 10) {
      sb.append('0');
    }
    return sb.append(value);
  }
}
