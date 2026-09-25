package com.baton.client.mixin;

import com.baton.client.render.Ui;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin extends AbstractWidget {
	private static final int FILL = 0x14FFFFFF;
	private static final float LINE_INSET = 6.0F;

	@Shadow
	protected double value;

	@Shadow
	private boolean dragging;

	private AbstractSliderButtonMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void baton$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Ui.control(graphics, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), active, alpha);
		float progress = (float) value;
		Ui.rect(graphics, getX() + 2.0F, getY() + 2.0F, (getWidth() - 4.0F) * progress, getHeight() - 4.0F, 5.0F, Ui.fade(FILL, alpha));
		Ui.rect(graphics, getX() + LINE_INSET, getY() + getHeight() - 3.5F, (getWidth() - LINE_INSET * 2.0F) * progress, 1.0F, 0.5F, Ui.fade(Ui.ACCENT, alpha));
		renderScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE), getMessage(), 2);
		if (isHovered()) {
			graphics.requestCursor(dragging ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND);
		}
		ci.cancel();
	}
}
