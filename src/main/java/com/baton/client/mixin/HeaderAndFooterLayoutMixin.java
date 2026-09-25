package com.baton.client.mixin;

import com.baton.client.screen.component.HeadingWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeaderAndFooterLayout.class)
public abstract class HeaderAndFooterLayoutMixin {
	@Shadow
	@Final
	private FrameLayout headerFrame;

	@Inject(method = "addTitleHeader", at = @At("HEAD"), cancellable = true)
	private void baton$heading(Component title, Font font, CallbackInfo ci) {
		headerFrame.addChild(new HeadingWidget(title, font));
		ci.cancel();
	}
}
