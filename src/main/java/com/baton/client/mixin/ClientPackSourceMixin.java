package com.baton.client.mixin;

import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPackSource.class)
public abstract class ClientPackSourceMixin {
	@Inject(method = "createBuiltinPack", at = @At("HEAD"), cancellable = true)
	private void baton$skipBundledPacks(String id, Pack.ResourcesSupplier resources, Component title, CallbackInfoReturnable<Pack> cir) {
		cir.setReturnValue(null);
	}
}
