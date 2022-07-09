package io.netty.handler.ssl;

import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.List;



























public final class ApplicationProtocolConfig
{
  public static final ApplicationProtocolConfig DISABLED = new ApplicationProtocolConfig();
  

  private final List<String> supportedProtocols;
  

  private final Protocol protocol;
  

  private final SelectorFailureBehavior selectorBehavior;
  
  private final SelectedListenerFailureBehavior selectedBehavior;
  

  public ApplicationProtocolConfig(Protocol protocol, SelectorFailureBehavior selectorBehavior, SelectedListenerFailureBehavior selectedBehavior, Iterable<String> supportedProtocols)
  {
    this(protocol, selectorBehavior, selectedBehavior, ApplicationProtocolUtil.toList(supportedProtocols));
  }
  







  public ApplicationProtocolConfig(Protocol protocol, SelectorFailureBehavior selectorBehavior, SelectedListenerFailureBehavior selectedBehavior, String... supportedProtocols)
  {
    this(protocol, selectorBehavior, selectedBehavior, ApplicationProtocolUtil.toList(supportedProtocols));
  }
  








  private ApplicationProtocolConfig(Protocol protocol, SelectorFailureBehavior selectorBehavior, SelectedListenerFailureBehavior selectedBehavior, List<String> supportedProtocols)
  {
    this.supportedProtocols = Collections.unmodifiableList((List)ObjectUtil.checkNotNull(supportedProtocols, "supportedProtocols"));
    this.protocol = ((Protocol)ObjectUtil.checkNotNull(protocol, "protocol"));
    this.selectorBehavior = ((SelectorFailureBehavior)ObjectUtil.checkNotNull(selectorBehavior, "selectorBehavior"));
    this.selectedBehavior = ((SelectedListenerFailureBehavior)ObjectUtil.checkNotNull(selectedBehavior, "selectedBehavior"));
    
    if (protocol == Protocol.NONE) {
      throw new IllegalArgumentException("protocol (" + Protocol.NONE + ") must not be " + Protocol.NONE + '.');
    }
    ObjectUtil.checkNonEmpty(supportedProtocols, "supportedProtocols");
  }
  


  private ApplicationProtocolConfig()
  {
    supportedProtocols = Collections.emptyList();
    protocol = Protocol.NONE;
    selectorBehavior = SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL;
    selectedBehavior = SelectedListenerFailureBehavior.ACCEPT;
  }
  


  public static enum Protocol
  {
    NONE,  NPN,  ALPN,  NPN_AND_ALPN;
    




    private Protocol() {}
  }
  



  public static enum SelectorFailureBehavior
  {
    FATAL_ALERT, 
    




    NO_ADVERTISE, 
    







    CHOOSE_MY_LAST_PROTOCOL;
    



    private SelectorFailureBehavior() {}
  }
  


  public static enum SelectedListenerFailureBehavior
  {
    ACCEPT, 
    



    FATAL_ALERT, 
    





    CHOOSE_MY_LAST_PROTOCOL;
    
    private SelectedListenerFailureBehavior() {}
  }
  
  public List<String> supportedProtocols()
  {
    return supportedProtocols;
  }
  


  public Protocol protocol()
  {
    return protocol;
  }
  


  public SelectorFailureBehavior selectorFailureBehavior()
  {
    return selectorBehavior;
  }
  


  public SelectedListenerFailureBehavior selectedListenerFailureBehavior()
  {
    return selectedBehavior;
  }
}
