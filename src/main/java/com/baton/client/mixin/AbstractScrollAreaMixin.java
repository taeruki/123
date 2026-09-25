package com.baton.client.mixin;

import com.baton.client.render.Ui;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaMixin {
	private static final String BLIT_SPRITE = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V";
	private static final int THUMB = 0x26FFFFFF;

	@Redirect(method = "renderScrollbar", at = @At(value = "INVOKE", target = BLIT_SPRITE, ordinal = 0))
	private void baton$track(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
	}

	@Redirect(method = "renderScrollbar", at = @At(value = "INVOKE", target = BLIT_SPRITE, ordinal = 1))
	private void baton$thumb(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
		Ui.rect(graphics, x + 2.0F, y, 2.0F, height, 1.0F, THUMB);
	}
}
