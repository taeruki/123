package com.baton.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;
import org.joml.Matrix3x2f;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

public final class UiFont {
	private static final String FILE = "/assets/" + Ui.MOD_ID + "/font/onest.ttf";
	private static final float BAKE_EM = 40.0F;
	private static final int PADDING = 5;
	private static final byte ON_EDGE = (byte) 128;
	private static final float DISTANCE_SCALE = 128.0F / PADDING;
	private static final int ATLAS_WIDTH = 1024;
	private static final char[] RANGES = {' ', '~', '\u00A0', '\u00FF', '\u0400', '\u045F', '\u2013', '\u2026', '\u2190', '\u2193', '\u2713', '\u2713'};
	private static final RenderPipeline PIPELINE = Ui.pipeline("text")
		.withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
		.withFragmentShader(Ui.id("core/text"))
		.withSampler("Sampler0")
		.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
		.build();

	private static CompletableFuture<Atlas> atlas = CompletableFuture.failedFuture(new IllegalStateException("Font is not loaded"));
	@Nullable
	private static TextureSetup texture;

	private UiFont() {
	}

	public static void load() {
		atlas = CompletableFuture.supplyAsync(UiFont::bake, Util.backgroundExecutor());
	}

	public static float width(String text, float size) {
		Atlas font = atlas.join();
		float width = 0.0F;
		char previous = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			width += font.kerning(previous, c) + font.glyph(c).advance();
			previous = c;
		}
		return width * size;
	}

	public static void draw(GuiGraphics graphics, String text, float x, float centerY, float size, int color) {
		if (text.isEmpty() || ARGB.alpha(color) == 0) {
			return;
		}
		Atlas font = atlas.join();
		Matrix3x2f pose = new Matrix3x2f(graphics.pose());
		ScreenRectangle area = new ScreenRectangle(Mth.floor(x) - 1, Mth.floor(centerY - size), Mth.ceil(width(text, size)) + 2, Mth.ceil(size * 2)).transformMaxBounds(pose);
		float baseline = centerY + font.capHeight() * size * 0.5F;
		Ui.submit(graphics, new Text(texture(font), pose, font, text, x, baseline, size, color, Ui.scissor(), Ui.bounds(area)));
	}

	public static void drawCentered(GuiGraphics graphics, String text, float centerX, float centerY, float size, int color) {
		draw(graphics, text, centerX - width(text, size) * 0.5F, centerY, size, color);
	}

	private static TextureSetup texture(Atlas font) {
		if (texture == null) {
			GpuDevice device = RenderSystem.getDevice();
			GpuTexture gpuTexture = device.createTexture("Baton UI font", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RED8, ATLAS_WIDTH, font.height(), 1, 1);
			device.createCommandEncoder().writeToTexture(gpuTexture, font.pixels(), NativeImage.Format.LUMINANCE, 0, 0, 0, 0, ATLAS_WIDTH, font.height());
			MemoryUtil.memFree(font.pixels());
			texture = TextureSetup.singleTexture(device.createTextureView(gpuTexture), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
		}
		return texture;
	}

	private static Atlas bake() {
		ByteBuffer data = read();
		try {
			STBTTFontinfo info = STBTTFontinfo.create();
			if (!STBTruetype.stbtt_InitFont(info, data)) {
				throw new IllegalStateException("Invalid font " + FILE);
			}
			float scale = STBTruetype.stbtt_ScaleForMappingEmToPixels(info, BAKE_EM);
			float unit = scale / BAKE_EM;
			int[] width = new int[1];
			int[] height = new int[1];
			int[] offsetX = new int[1];
			int[] offsetY = new int[1];
			int[] advance = new int[1];
			int[] bearing = new int[1];
			List<Pending> pending = new ArrayList<>();
			int penX = 0;
			int penY = 0;
			int rowHeight = 0;
			for (int range = 0; range < RANGES.length; range += 2) {
				for (char c = RANGES[range]; c <= RANGES[range + 1]; c++) {
					int index = STBTruetype.stbtt_FindGlyphIndex(info, c);
					if (index == 0) {
						continue;
					}
					STBTruetype.stbtt_GetGlyphHMetrics(info, index, advance, bearing);
					ByteBuffer sdf = STBTruetype.stbtt_GetGlyphSDF(info, scale, index, PADDING, ON_EDGE, DISTANCE_SCALE, width, height, offsetX, offsetY);
					if (sdf != null && penX + width[0] > ATLAS_WIDTH) {
						penX = 0;
						penY += rowHeight + 1;
						rowHeight = 0;
					}
					pending.add(new Pending(c, index, advance[0] * unit, sdf, width[0], height[0], offsetX[0], offsetY[0], penX, penY));
					if (sdf != null) {
						penX += width[0] + 1;
						rowHeight = Math.max(rowHeight, height[0]);
					}
				}
			}

			int atlasHeight = penY + rowHeight;
			ByteBuffer pixels = MemoryUtil.memCalloc(ATLAS_WIDTH * atlasHeight);
			Char2ObjectOpenHashMap<Glyph> glyphs = new Char2ObjectOpenHashMap<>(pending.size());
			Int2FloatOpenHashMap kerning = new Int2FloatOpenHashMap();
			for (Pending glyph : pending) {
				glyphs.put(glyph.c(), glyph.bake(pixels, atlasHeight));
				for (Pending next : pending) {
					int kern = STBTruetype.stbtt_GetGlyphKernAdvance(info, glyph.index(), next.index());
					if (kern != 0) {
						kerning.put(glyph.c() << 16 | next.c(), kern * unit);
					}
				}
			}
			STBTruetype.stbtt_GetCodepointBox(info, 'H', width, height, offsetX, offsetY);
			return new Atlas(pixels, atlasHeight, glyphs, kerning, offsetY[0] * unit, Objects.requireNonNull(glyphs.get('?')));
		} finally {
			MemoryUtil.memFree(data);
		}
	}

	private static ByteBuffer read() {
		try (InputStream stream = Objects.requireNonNull(UiFont.class.getResourceAsStream(FILE), FILE)) {
			byte[] bytes = stream.readAllBytes();
			return MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private record Pending(char c, int index, float advance, @Nullable ByteBuffer sdf, int width, int height, int offsetX, int offsetY, int x, int y) {
		Glyph bake(ByteBuffer pixels, int atlasHeight) {
			if (sdf == null) {
				return new Glyph(advance, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
			}
			for (int row = 0; row < height; row++) {
				MemoryUtil.memCopy(MemoryUtil.memAddress(sdf) + (long) row * width, MemoryUtil.memAddress(pixels) + (long) (y + row) * ATLAS_WIDTH + x, width);
			}
			STBTruetype.stbtt_FreeSDF(sdf);
			return new Glyph(
				advance,
				offsetX / BAKE_EM,
				offsetY / BAKE_EM,
				(offsetX + width) / BAKE_EM,
				(offsetY + height) / BAKE_EM,
				(float) x / ATLAS_WIDTH,
				(float) y / atlasHeight,
				(float) (x + width) / ATLAS_WIDTH,
				(float) (y + height) / atlasHeight
			);
		}
	}

	private record Glyph(float advance, float left, float top, float right, float bottom, float u0, float v0, float u1, float v1) {
	}

	private record Atlas(ByteBuffer pixels, int height, Char2ObjectOpenHashMap<Glyph> glyphs, Int2FloatOpenHashMap kerning, float capHeight, Glyph fallback) {
		Glyph glyph(char c) {
			return glyphs.getOrDefault(c, fallback);
		}

		float kerning(char previous, char c) {
			return kerning.get(previous << 16 | c);
		}
	}

	private record Text(
		TextureSetup textureSetup,
		Matrix3x2fc pose,
		Atlas font,
		String text,
		float x,
		float baseline,
		float size,
		int color,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds
	) implements GuiElementRenderState {
		@Override
		public void buildVertices(VertexConsumer consumer) {
			float pen = x;
			char previous = 0;
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				Glyph glyph = font.glyph(c);
				pen += font.kerning(previous, c) * size;
				if (glyph.u0() != glyph.u1()) {
					float x0 = pen + glyph.left() * size;
					float x1 = pen + glyph.right() * size;
					float y0 = baseline + glyph.top() * size;
					float y1 = baseline + glyph.bottom() * size;
					consumer.addVertexWith2DPose(pose, x0, y0).setUv(glyph.u0(), glyph.v0()).setColor(color);
					consumer.addVertexWith2DPose(pose, x0, y1).setUv(glyph.u0(), glyph.v1()).setColor(color);
					consumer.addVertexWith2DPose(pose, x1, y1).setUv(glyph.u1(), glyph.v1()).setColor(color);
					consumer.addVertexWith2DPose(pose, x1, y0).setUv(glyph.u1(), glyph.v0()).setColor(color);
				}
				pen += glyph.advance() * size;
				previous = c;
			}
		}

		@Override
		public RenderPipeline pipeline() {
			return PIPELINE;
		}
	}
}
