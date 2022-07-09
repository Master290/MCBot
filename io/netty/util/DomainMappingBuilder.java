package io.netty.util;















@Deprecated
public final class DomainMappingBuilder<V>
{
  private final DomainNameMappingBuilder<V> builder;
  













  public DomainMappingBuilder(V defaultValue)
  {
    builder = new DomainNameMappingBuilder(defaultValue);
  }
  






  public DomainMappingBuilder(int initialCapacity, V defaultValue)
  {
    builder = new DomainNameMappingBuilder(initialCapacity, defaultValue);
  }
  











  public DomainMappingBuilder<V> add(String hostname, V output)
  {
    builder.add(hostname, output);
    return this;
  }
  





  public DomainNameMapping<V> build()
  {
    return builder.build();
  }
}
