package com.baton.client.mixin;

import com.baton.client.render.UiFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.GuiGraphics$RenderingTextCollector")
public abstract class RenderingTextCollectorMixin {
	@Inject(method = "acceptScrolling", at = @At("HEAD"), cancellable = true)
	private void baton$uiFont(Component component, int center, int left, int right, int top, int bottom, ActiveTextCollector.Parameters parameters, CallbackInfo ci) {
		UiFont.label(Minecraft.getInstance().gameRenderer.guiRenderState, parameters.pose(), parameters.scissor(), component, left, right, (top + bottom) / 2.0F, parameters.opacity());
		ci.cancel();
	}
}
