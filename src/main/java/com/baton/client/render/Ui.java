package com.baton.client.render;

import com.baton.client.mixin.GuiGraphicsAccessor;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public final class Ui {
	public static final String MOD_ID = "baton";
	private static final RenderPipeline SHAPE = pipeline("shape")
		.withVertexShader(id("core/shape"))
		.withFragmentShader(id("core/shape"))
		.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
		.build();
	private static final int PANEL = 0xFF121318;
	private static final int PANEL_EDGE = 0x12FFFFFF;
	private static final int PANEL_SHADOW = 0x8C000000;
	@Nullable
	private static ScreenRectangle scissor;

	private Ui() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void rect(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
		shape(graphics, x, y, width, height, color, radius, 0.0F);
	}

	public static void outline(GuiGraphics graphics, float x, float y, float width, float height, float radius, float stroke, int color) {
		shape(graphics, x, y, width, height, color, radius, stroke);
	}

	public static void shadow(GuiGraphics graphics, float x, float y, float width, float height, float radius, float softness, int color) {
		shape(graphics, x, y, width, height, color, radius, -softness);
	}

	public static void panel(GuiGraphics graphics, float x, float y, float width, float height, float radius, float alpha) {
		shadow(graphics, x, y + 4.0F, width, height, radius, 14.0F, fade(PANEL_SHADOW, alpha));
		rect(graphics, x, y, width, height, radius, fade(PANEL, alpha));
		outline(graphics, x, y, width, height, radius, 0.5F, fade(PANEL_EDGE, alpha));
	}

	public static void clip(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
		graphics.enableScissor(x0, y0, x1, y1);
		scissor = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformAxisAligned(graphics.pose());
	}

	public static void unclip(GuiGraphics graphics) {
		graphics.disableScissor();
		scissor = null;
	}

	public static float approach(float value, float target, float speed, float delta) {
		return target + (value - target) * (float) Math.exp(-speed * delta);
	}

	public static int fade(int color, float alpha) {
		return ARGB.multiplyAlpha(color, alpha);
	}

	@Nullable
	static ScreenRectangle bounds(ScreenRectangle area) {
		return scissor != null ? scissor.intersection(area) : area;
	}

	@Nullable
	static ScreenRectangle scissor() {
		return scissor;
	}

	static void submit(GuiGraphics graphics, GuiElementRenderState state) {
		((GuiGraphicsAccessor) graphics).baton$renderState().submitGuiElement(state);
	}

	static RenderPipeline.Builder pipeline(String name) {
		return RenderPipeline.builder()
			.withLocation(id("pipeline/" + name))
			.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
			.withUniform("Projection", UniformType.UNIFORM_BUFFER)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
	}

	private static void shape(GuiGraphics graphics, float x, float y, float width, float height, int color, float radius, float stroke) {
		if (ARGB.alpha(color) != 0) {
			submit(graphics, ShapeRenderState.of(SHAPE, new Matrix3x2f(graphics.pose()), x, y, width, height, color, Math.round(radius * 8), Math.round(stroke * 8)));
		}
	}
}
