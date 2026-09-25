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
	float padding,
	float z,
	int color,
	int a,
	int b,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
	static ShapeRenderState of(RenderPipeline pipeline, Matrix3x2fc pose, float x, float y, float width, float height, float padding, float z, int color, int a, int b) {
		ScreenRectangle area = new ScreenRectangle(
			Mth.floor(x - padding),
			Mth.floor(y - padding),
			Mth.ceil(width + padding * 2) + 1,
			Mth.ceil(height + padding * 2) + 1
		).transformMaxBounds(pose);
		return new ShapeRenderState(pipeline, pose, x, y, width, height, padding, z, color, a, b, Ui.scissor(), Ui.bounds(area));
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		float halfWidth = width * 0.5F + padding;
		float halfHeight = height * 0.5F + padding;
		float centerX = x + width * 0.5F;
		float centerY = y + height * 0.5F;
		vertex(consumer, centerX, centerY, -halfWidth, -halfHeight);
		vertex(consumer, centerX, centerY, -halfWidth, halfHeight);
		vertex(consumer, centerX, centerY, halfWidth, halfHeight);
		vertex(consumer, centerX, centerY, halfWidth, -halfHeight);
	}

	private void vertex(VertexConsumer consumer, float centerX, float centerY, float localX, float localY) {
		float px = centerX + localX;
		float py = centerY + localY;
		consumer.addVertex(pose.m00() * px + pose.m10() * py + pose.m20(), pose.m01() * px + pose.m11() * py + pose.m21(), z)
			.setColor(color)
			.setUv(localX, localY)
			.setUv2(a, b);
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}
}
