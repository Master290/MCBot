package io.netty.channel.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;













final class SelectedSelectionKeySetSelector
  extends Selector
{
  private final SelectedSelectionKeySet selectionKeys;
  private final Selector delegate;
  
  SelectedSelectionKeySetSelector(Selector delegate, SelectedSelectionKeySet selectionKeys)
  {
    this.delegate = delegate;
    this.selectionKeys = selectionKeys;
  }
  
  public boolean isOpen()
  {
    return delegate.isOpen();
  }
  
  public SelectorProvider provider()
  {
    return delegate.provider();
  }
  
  public Set<SelectionKey> keys()
  {
    return delegate.keys();
  }
  
  public Set<SelectionKey> selectedKeys()
  {
    return delegate.selectedKeys();
  }
  
  public int selectNow() throws IOException
  {
    selectionKeys.reset();
    return delegate.selectNow();
  }
  
  public int select(long timeout) throws IOException
  {
    selectionKeys.reset();
    return delegate.select(timeout);
  }
  
  public int select() throws IOException
  {
    selectionKeys.reset();
    return delegate.select();
  }
  
  public Selector wakeup()
  {
    return delegate.wakeup();
  }
  
  public void close() throws IOException
  {
    delegate.close();
  }
}
