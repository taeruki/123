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
	int a,
	int b,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
	private static final float EDGE_PADDING = 1.0F;

	static ShapeRenderState of(RenderPipeline pipeline, Matrix3x2fc pose, float x, float y, float width, float height, int color, int a, int b) {
		ScreenRectangle area = new ScreenRectangle(
			Mth.floor(x - EDGE_PADDING),
			Mth.floor(y - EDGE_PADDING),
			Mth.ceil(width + EDGE_PADDING * 2) + 1,
			Mth.ceil(height + EDGE_PADDING * 2) + 1
		).transformMaxBounds(pose);
		return new ShapeRenderState(pipeline, pose, x, y, width, height, color, a, b, Ui.scissor(), Ui.bounds(area));
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		float halfWidth = width * 0.5F + EDGE_PADDING;
		float halfHeight = height * 0.5F + EDGE_PADDING;
		float centerX = x + width * 0.5F;
		float centerY = y + height * 0.5F;
		vertex(consumer, centerX - halfWidth, centerY - halfHeight, -halfWidth, -halfHeight);
		vertex(consumer, centerX - halfWidth, centerY + halfHeight, -halfWidth, halfHeight);
		vertex(consumer, centerX + halfWidth, centerY + halfHeight, halfWidth, halfHeight);
		vertex(consumer, centerX + halfWidth, centerY - halfHeight, halfWidth, -halfHeight);
	}

	private void vertex(VertexConsumer consumer, float px, float py, float localX, float localY) {
		consumer.addVertexWith2DPose(pose, px, py).setColor(color).setUv(localX, localY).setUv2(a, b);
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}
}
