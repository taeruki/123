package com.baton.client.mixin;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabButton.class)
public abstract class TabButtonMixin extends AbstractWidget.WithInactiveMessage {
	private TabButtonMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Shadow
	public abstract boolean isSelected();

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void baton$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		boolean selected = isSelected();
		if (selected || isHoveredOrFocused()) {
			Ui.rect(graphics, getX() + 2, getY() + 2, getWidth() - 4, getHeight() - 4, 7.0F, selected ? Ui.SELECTED : Ui.HOVER);
		}
		if (selected) {
			Ui.rect(graphics, getX() + getWidth() / 2.0F - 6.0F, getBottom() - 4.0F, 12.0F, 1.0F, 0.5F, Ui.ACCENT);
		}
		UiFont.label(graphics.guiRenderState, graphics.pose(), graphics.scissorStack.peek(), getMessage(), getX() + 1, getRight() - 1, getY() + getHeight() / 2.0F, active ? 1.0F : 0.5F);
		handleCursor(graphics);
		ci.cancel();
	}
}
