package com.baton.client.mixin;

import com.baton.client.render.Ui;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
	@Shadow
	@Nullable
	private Supplier<Boolean> overrideRenderHighlightedSprite;

	private AbstractButtonMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderDefaultSprite", at = @At("HEAD"), cancellable = true)
	private void baton$sprite(GuiGraphics graphics, CallbackInfo ci) {
		boolean highlighted = overrideRenderHighlightedSprite != null ? overrideRenderHighlightedSprite.get() : isHoveredOrFocused();
		Ui.control(graphics, getX(), getY(), getWidth(), getHeight(), highlighted, active, alpha);
		ci.cancel();
	}
}
