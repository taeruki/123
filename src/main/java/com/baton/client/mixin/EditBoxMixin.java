package com.baton.client.mixin;

import com.baton.client.render.Ui;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
	private static final int EDGE_FOCUSED = 0x26FFFFFF;

	private EditBoxMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void baton$field(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
		Ui.rect(graphics, x, y, width, height, 6.0F, Ui.FIELD);
		Ui.outline(graphics, x, y, width, height, 6.0F, 0.5F, isFocused() ? EDGE_FOCUSED : Ui.EDGE);
	}
}
