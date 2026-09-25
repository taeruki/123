package com.baton.client.mixin;

import com.baton.client.render.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Shadow
	@Final
	protected Minecraft minecraft;

	@Shadow
	public int width;

	@Shadow
	public int height;

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void baton$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (minecraft.level == null) {
			graphics.fill(0, 0, width, height, Ui.BACKGROUND);
			ci.cancel();
		}
	}
}
