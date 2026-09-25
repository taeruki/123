package com.baton.client.mixin;

import com.baton.client.render.UiFont;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractStringWidget.class)
public abstract class AbstractStringWidgetMixin extends AbstractWidget {
	private AbstractStringWidgetMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void baton$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if ((Object) this instanceof StringWidget) {
			float center = getX() + getWidth() / 2.0F;
			float half = Math.max(getWidth(), UiFont.width(getMessage().getString(), 9.0F)) / 2.0F + 2.0F;
			UiFont.label(graphics.guiRenderState, graphics.pose(), graphics.scissorStack.peek(), getMessage(), center - half, center + half, getY() + getHeight() / 2.0F, alpha);
			ci.cancel();
		}
	}
}
