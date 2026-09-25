package com.baton.client.mixin;

import com.baton.client.render.Ui;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Checkbox.class)
public abstract class CheckboxMixin extends AbstractButton {
	@Shadow
	private boolean selected;

	private CheckboxMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Redirect(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
	private void baton$box(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
		Ui.control(graphics, x, y, width, height, isHoveredOrFocused(), active, alpha);
		if (selected) {
			Ui.rect(graphics, x + 3.5F, y + 3.5F, width - 7.0F, height - 7.0F, 2.0F, Ui.fade(Ui.ACCENT, alpha));
		}
	}
}
