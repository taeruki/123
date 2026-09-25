package com.baton.client.mixin;

import java.util.stream.Stream;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin {
	@Inject(method = "getSelected", at = @At("RETURN"), cancellable = true)
	private void baton$hideVanilla(CallbackInfoReturnable<Stream<PackSelectionModel.Entry>> cir) {
		cir.setReturnValue(cir.getReturnValue().filter(entry -> !"vanilla".equals(entry.getId())));
	}
}
