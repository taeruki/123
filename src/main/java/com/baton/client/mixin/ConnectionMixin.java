package com.baton.client.mixin;

import com.baton.client.proxy.Proxies;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
	@ModifyArg(
		method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;",
		at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/AbstractBootstrap;")
	)
	private static ChannelHandler baton$proxy(ChannelHandler initializer) {
		return Proxies.wrap(initializer);
	}
}
