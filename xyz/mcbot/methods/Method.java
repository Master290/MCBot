package xyz.mcbot.methods;

import io.netty.channel.Channel;
import java.util.function.BiConsumer;
import xyz.mcbot.ProxyLoader.Proxy;

public abstract interface Method
  extends BiConsumer<Channel, ProxyLoader.Proxy>
{}
