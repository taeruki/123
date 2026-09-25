package com.baton.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

public record ShapeRenderState(
	RenderPipeline pipeline,
	Matrix3x2fc pose,
	float x,
	float y,
	float width,
	float height,
	int color,
	int radius,
	int stroke,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
	private static final float PADDING = 1.0F;

	static ShapeRenderState of(RenderPipeline pipeline, Matrix3x2fc pose, @Nullable ScreenRectangle scissor, float x, float y, float width, float height, int color, int radius, int stroke) {
		ScreenRectangle area = new ScreenRectangle(
			Mth.floor(x - PADDING),
			Mth.floor(y - PADDING),
			Mth.ceil(width + PADDING * 2) + 1,
			Mth.ceil(height + PADDING * 2) + 1
		).transformMaxBounds(pose);
		return new ShapeRenderState(pipeline, pose, x, y, width, height, color, radius, stroke, scissor, scissor != null ? scissor.intersection(area) : area);
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		float halfWidth = width * 0.5F + PADDING;
		float halfHeight = height * 0.5F + PADDING;
		float centerX = x + width * 0.5F;
		float centerY = y + height * 0.5F;
		vertex(consumer, centerX, centerY, -halfWidth, -halfHeight);
		vertex(consumer, centerX, centerY, -halfWidth, halfHeight);
		vertex(consumer, centerX, centerY, halfWidth, halfHeight);
		vertex(consumer, centerX, centerY, halfWidth, -halfHeight);
	}

	private void vertex(VertexConsumer consumer, float centerX, float centerY, float localX, float localY) {
		consumer.addVertexWith2DPose(pose, centerX + localX, centerY + localY).setColor(color).setUv(localX, localY).setUv2(radius, stroke);
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}
}
