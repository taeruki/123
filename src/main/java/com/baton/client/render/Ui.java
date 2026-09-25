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
	private static final RenderPipeline SHAPE = shapePipeline("shape");
	private static final RenderPipeline SNOW = shapePipeline("snow");
	private static final long SNOW_PERIOD_MILLIS = 32_768_000L;
	@Nullable
	private static ScreenRectangle scissor;

	private Ui() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void rect(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
		shape(graphics, SHAPE, x, y, width, height, color, Math.round(radius * 8), 0);
	}

	public static void outline(GuiGraphics graphics, float x, float y, float width, float height, float radius, float stroke, int color) {
		shape(graphics, SHAPE, x, y, width, height, color, Math.round(radius * 8), Math.round(stroke * 8));
	}

	public static void snow(GuiGraphics graphics, int width, int height, int background) {
		long millis = Util.getMillis() % SNOW_PERIOD_MILLIS;
		shape(graphics, SNOW, 0, 0, width, height, background, (int) (millis / 1000), (int) (millis % 1000));
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

	private static void shape(GuiGraphics graphics, RenderPipeline pipeline, float x, float y, float width, float height, int color, int a, int b) {
		if (ARGB.alpha(color) != 0) {
			submit(graphics, ShapeRenderState.of(pipeline, new Matrix3x2f(graphics.pose()), x, y, width, height, color, a, b));
		}
	}

	private static RenderPipeline shapePipeline(String fragment) {
		return pipeline(fragment)
			.withVertexShader(id("core/shape"))
			.withFragmentShader(id("core/" + fragment))
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
			.build();
	}
}
