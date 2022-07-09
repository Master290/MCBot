package io.netty.util;

import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;




























@Deprecated
public final class DomainNameMappingBuilder<V>
{
  private final V defaultValue;
  private final Map<String, V> map;
  
  public DomainNameMappingBuilder(V defaultValue)
  {
    this(4, defaultValue);
  }
  






  public DomainNameMappingBuilder(int initialCapacity, V defaultValue)
  {
    this.defaultValue = ObjectUtil.checkNotNull(defaultValue, "defaultValue");
    map = new LinkedHashMap(initialCapacity);
  }
  











  public DomainNameMappingBuilder<V> add(String hostname, V output)
  {
    map.put(ObjectUtil.checkNotNull(hostname, "hostname"), ObjectUtil.checkNotNull(output, "output"));
    return this;
  }
  





  public DomainNameMapping<V> build()
  {
    return new ImmutableDomainNameMapping(defaultValue, map, null);
  }
  

  private static final class ImmutableDomainNameMapping<V>
    extends DomainNameMapping<V>
  {
    private static final String REPR_HEADER = "ImmutableDomainNameMapping(default: ";
    
    private static final String REPR_MAP_OPENING = ", map: {";
    
    private static final String REPR_MAP_CLOSING = "})";
    
    private static final int REPR_CONST_PART_LENGTH = "ImmutableDomainNameMapping(default: "
      .length() + ", map: {".length() + "})".length();
    
    private final String[] domainNamePatterns;
    private final V[] values;
    private final Map<String, V> map;
    
    private ImmutableDomainNameMapping(V defaultValue, Map<String, V> map)
    {
      super(defaultValue);
      
      Set<Map.Entry<String, V>> mappings = map.entrySet();
      int numberOfMappings = mappings.size();
      domainNamePatterns = new String[numberOfMappings];
      values = ((Object[])new Object[numberOfMappings]);
      
      Map<String, V> mapCopy = new LinkedHashMap(map.size());
      int index = 0;
      for (Map.Entry<String, V> mapping : mappings) {
        String hostname = normalizeHostname((String)mapping.getKey());
        V value = mapping.getValue();
        domainNamePatterns[index] = hostname;
        values[index] = value;
        mapCopy.put(hostname, value);
        index++;
      }
      
      this.map = Collections.unmodifiableMap(mapCopy);
    }
    
    @Deprecated
    public DomainNameMapping<V> add(String hostname, V output)
    {
      throw new UnsupportedOperationException("Immutable DomainNameMapping does not support modification after initial creation");
    }
    

    public V map(String hostname)
    {
      if (hostname != null) {
        hostname = normalizeHostname(hostname);
        
        int length = domainNamePatterns.length;
        for (int index = 0; index < length; index++) {
          if (matches(domainNamePatterns[index], hostname)) {
            return values[index];
          }
        }
      }
      
      return defaultValue;
    }
    
    public Map<String, V> asMap()
    {
      return map;
    }
    
    public String toString()
    {
      String defaultValueStr = defaultValue.toString();
      
      int numberOfMappings = domainNamePatterns.length;
      if (numberOfMappings == 0) {
        return "ImmutableDomainNameMapping(default: " + defaultValueStr + ", map: {" + "})";
      }
      
      String pattern0 = domainNamePatterns[0];
      String value0 = values[0].toString();
      int oneMappingLength = pattern0.length() + value0.length() + 3;
      int estimatedBufferSize = estimateBufferSize(defaultValueStr.length(), numberOfMappings, oneMappingLength);
      

      StringBuilder sb = new StringBuilder(estimatedBufferSize).append("ImmutableDomainNameMapping(default: ").append(defaultValueStr).append(", map: {");
      
      appendMapping(sb, pattern0, value0);
      for (int index = 1; index < numberOfMappings; index++) {
        sb.append(", ");
        appendMapping(sb, index);
      }
      
      return "})";
    }
    











    private static int estimateBufferSize(int defaultValueLength, int numberOfMappings, int estimatedMappingLength)
    {
      return REPR_CONST_PART_LENGTH + defaultValueLength + (int)(estimatedMappingLength * numberOfMappings * 1.1D);
    }
    
    private StringBuilder appendMapping(StringBuilder sb, int mappingIndex)
    {
      return appendMapping(sb, domainNamePatterns[mappingIndex], values[mappingIndex].toString());
    }
    
    private static StringBuilder appendMapping(StringBuilder sb, String domainNamePattern, String value) {
      return sb.append(domainNamePattern).append('=').append(value);
    }
  }
}
