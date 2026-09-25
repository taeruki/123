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
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public final class Ui {
	public static final String MOD_ID = "baton";
	private static final RenderPipeline SHAPE = shapePipeline("shape").withShaderDefine("PADDED").build();
	private static final RenderPipeline SNOW = shapePipeline("snow").build();
	private static final RenderPipeline GLASS = shapePipeline("glass").build();
	private static final long TIME_PERIOD_MILLIS = 3_600_000L;
	private static final float EDGE = 1.0F;
	private static final int GLASS_TINT = 0xFF9AA1B0;
	private static final int GLASS_SHADOW = 0x66000000;
	@Nullable
	private static ScreenRectangle scissor;

	private Ui() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void rect(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
		shape(graphics, SHAPE, x, y, width, height, EDGE, EDGE, color, radius, 0.0F);
	}

	public static void outline(GuiGraphics graphics, float x, float y, float width, float height, float radius, float stroke, int color) {
		shape(graphics, SHAPE, x, y, width, height, EDGE, EDGE, color, radius, stroke);
	}

	public static void shadow(GuiGraphics graphics, float x, float y, float width, float height, float radius, float softness, int color) {
		float padding = EDGE + softness;
		shape(graphics, SHAPE, x, y, width, height, padding, padding, color, radius, -softness);
	}

	public static void glass(GuiGraphics graphics, float x, float y, float width, float height, float radius, float alpha) {
		glass(graphics, x, y, width, height, radius, GLASS_TINT, 0.07F, alpha);
	}

	public static void glass(GuiGraphics graphics, float x, float y, float width, float height, float radius, int tint, float strength, float alpha) {
		shadow(graphics, x, y + 3.0F, width, height, radius, 9.0F, fade(GLASS_SHADOW, alpha));
		shape(graphics, GLASS, x, y, width, height, EDGE, time(), fade(tint, alpha), radius, strength * 125.0F);
	}

	public static void snow(GuiGraphics graphics, int width, int height) {
		shape(graphics, SNOW, 0, 0, width, height, EDGE, time(), 0xFFFFFFFF, 0.0F, 0.0F);
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

	private static float time() {
		return (Util.getMillis() % TIME_PERIOD_MILLIS) / 1000.0F;
	}

	private static void shape(GuiGraphics graphics, RenderPipeline pipeline, float x, float y, float width, float height, float padding, float z, int color, float a, float b) {
		if (ARGB.alpha(color) != 0) {
			submit(graphics, ShapeRenderState.of(pipeline, new Matrix3x2f(graphics.pose()), x, y, width, height, padding, z, color, Math.round(a * 8), Math.round(b * 8)));
		}
	}

	private static RenderPipeline.Builder shapePipeline(String fragment) {
		return pipeline(fragment)
			.withVertexShader(id("core/shape"))
			.withFragmentShader(id("core/" + fragment))
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS);
	}
}
