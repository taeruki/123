package com.baton.client.mixin;

import com.baton.client.render.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin extends AbstractWidget {
	private AbstractSelectionListMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
	private void baton$background(GuiGraphics graphics, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
	private void baton$separators(GuiGraphics graphics, CallbackInfo ci) {
		Ui.rect(graphics, getX(), getY() - 0.5F, getWidth(), 0.5F, 0.0F, Ui.EDGE);
		Ui.rect(graphics, getX(), getBottom(), getWidth(), 0.5F, 0.0F, Ui.EDGE);
		ci.cancel();
	}
}
