package com.baton.client.mixin;

import com.baton.client.render.Ui;
import com.baton.client.screen.BatonScreen;
import com.baton.client.screen.Bubble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	private static final String RENDER = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";

	@Shadow
	@Final
	protected Minecraft minecraft;

	@Shadow
	public int width;

	@Shadow
	public int height;

	@Unique
	private long baton$openedAt;

	@Inject(method = "added", at = @At("TAIL"))
	private void baton$opened(CallbackInfo ci) {
		baton$openedAt = Util.getMillis();
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void baton$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (minecraft.level == null) {
			Ui.background(graphics, width, height, 1.0F);
			ci.cancel();
		}
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = RENDER))
	private void baton$bubbleIn(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		graphics.pose().pushMatrix();
		if (baton$animated()) {
			Bubble.apply(graphics, width, height, baton$openedAt);
		}
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.AFTER))
	private void baton$bubbleOut(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		graphics.pose().popMatrix();
		if (baton$animated() && minecraft.level == null) {
			Ui.background(graphics, width, height, 1.0F - Bubble.appear(baton$openedAt));
		}
	}

	@Unique
	private boolean baton$animated() {
		Object self = this;
		return !(self instanceof BatonScreen) && (minecraft.level == null || self instanceof PauseScreen || self instanceof OptionsScreen || self instanceof OptionsSubScreen);
	}
}
