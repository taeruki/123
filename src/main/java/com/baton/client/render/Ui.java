package com.baton.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

public final class Ui {
	public static final String MOD_ID = "baton";
	public static final float HEADING_SIZE = 12.0F;
	public static final int HEADING = 0xFFECEEF3;
	public static final int BACKGROUND = 0xFF0B0C0F;
	public static final int PANEL = 0xFF121318;
	public static final int EDGE = 0x12FFFFFF;
	public static final int HOVER = 0x08FFFFFF;
	public static final int SELECTED = 0x10FFFFFF;
	public static final int FIELD = 0x40000000;
	public static final int TEXT = 0xFF979DAA;
	public static final int TEXT_ACTIVE = 0xFFF4F5F8;
	public static final int MUTED = 0xFF5F6573;
	public static final int ACCENT = 0xFFE9ECF2;
	public static final int ON_ACCENT = 0xFF0C0E13;
	public static final int DANGER = 0xFFFF8A94;
	public static final int DANGER_FILL = 0x14FF5A64;
	public static final int DANGER_HOVER = 0x24FF5A64;
	private static final int CONTROL = 0xFF17181D;
	private static final int CONTROL_HOVER = 0xFF1E2026;
	private static final int CONTROL_DISABLED = 0xFF131418;
	private static final RenderPipeline SHAPE = pipeline("shape")
		.withVertexShader(id("core/shape"))
		.withFragmentShader(id("core/shape"))
		.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
		.build();

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

	public static void panel(GuiGraphics graphics, float x, float y, float width, float height, float radius, float alpha) {
		rect(graphics, x, y, width, height, radius, fade(PANEL, alpha));
		outline(graphics, x, y, width, height, radius, 0.5F, fade(EDGE, alpha));
	}

	public static void control(GuiGraphics graphics, float x, float y, float width, float height, boolean hovered, boolean active, float alpha) {
		float radius = Math.min(height / 2.0F, 7.0F);
		rect(graphics, x, y, width, height, radius, fade(!active ? CONTROL_DISABLED : hovered ? CONTROL_HOVER : CONTROL, alpha));
		outline(graphics, x, y, width, height, radius, 0.5F, fade(EDGE, alpha));
	}

	public static float approach(float value, float target, float speed, float delta) {
		return target + (value - target) * (float) Math.exp(-speed * delta);
	}

	public static int fade(int color, float alpha) {
		return ARGB.multiplyAlpha(color, alpha);
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
			graphics.guiRenderState.submitGuiElement(ShapeRenderState.of(
				SHAPE, new Matrix3x2f(graphics.pose()), graphics.scissorStack.peek(), x, y, width, height, color, Math.round(radius * 8), Math.round(stroke * 8)
			));
		}
	}
}
