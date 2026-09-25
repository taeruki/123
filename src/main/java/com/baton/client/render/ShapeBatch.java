package com.baton.client.render;

import com.baton.client.render.shader.BatonShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class ShapeBatch implements AutoCloseable {
	private static final VertexFormatElement SHAPE = registerGenericElement(4);
	private static final VertexFormatElement SOFTNESS = registerGenericElement(1);

	public static final VertexFormat FORMAT = VertexFormat.builder()
		.add("Position", VertexFormatElement.POSITION)
		.add("Color", VertexFormatElement.COLOR)
		.add("UV0", VertexFormatElement.UV0)
		.add("Shape", SHAPE)
		.add("Softness", SOFTNESS)
		.build();

	private static final int STRIDE = FORMAT.getVertexSize();
	private static final int COLOR_OFFSET = FORMAT.getOffset(VertexFormatElement.COLOR);
	private static final int LOCAL_OFFSET = FORMAT.getOffset(VertexFormatElement.UV0);
	private static final int SHAPE_OFFSET = FORMAT.getOffset(SHAPE);
	private static final int SOFTNESS_OFFSET = FORMAT.getOffset(SOFTNESS);
	private static final float[] CORNER_X = {-1.0F, -1.0F, 1.0F, 1.0F};
	private static final float[] CORNER_Y = {-1.0F, 1.0F, 1.0F, -1.0F};
	private static final float EDGE_PADDING = 1.0F;

	private static final ShapeBatch INSTANCE = new ShapeBatch();

	private final ByteBufferBuilder buffer = new ByteBufferBuilder(STRIDE * 4 * 256);
	@Nullable
	private PoseStack poses;
	private int vertices;

	private ShapeBatch() {
	}

	public static ShapeBatch begin(GuiGraphics graphics) {
		RenderSystem.assertOnRenderThread();
		if (INSTANCE.poses != null) {
			throw new IllegalStateException("Shape batch is already active");
		}
		graphics.flush();
		INSTANCE.poses = graphics.pose();
		return INSTANCE;
	}

	public ShapeBatch rect(float x, float y, float width, float height, float radius, int color) {
		return shape(x, y, width, height, radius, 0.0F, 0.0F, color);
	}

	public ShapeBatch outline(float x, float y, float width, float height, float radius, float thickness, int color) {
		return shape(x, y, width, height, radius, thickness, 0.0F, color);
	}

	public ShapeBatch shadow(float x, float y, float width, float height, float radius, float softness, int color) {
		return shape(x, y, width, height, radius, 0.0F, softness, color);
	}

	public ShapeBatch shape(float x, float y, float width, float height, float radius, float stroke, float softness, int color) {
		PoseStack stack = poses;
		if (stack == null) {
			throw new IllegalStateException("Shape batch is not active");
		}
		if (width <= 0.0F || height <= 0.0F || color >>> 24 == 0) {
			return this;
		}

		float halfWidth = width * 0.5F;
		float halfHeight = height * 0.5F;
		float centerX = x + halfWidth;
		float centerY = y + halfHeight;
		float clampedRadius = Math.clamp(radius, 0.0F, Math.min(halfWidth, halfHeight));
		float blur = Math.max(softness, 0.0F);
		float extentX = halfWidth + blur * 0.5F + EDGE_PADDING;
		float extentY = halfHeight + blur * 0.5F + EDGE_PADDING;
		int abgr = FastColor.ABGR32.fromArgb32(color);
		Matrix4f pose = stack.last().pose();

		long pointer = buffer.reserve(STRIDE * 4);
		for (int corner = 0; corner < 4; corner++, pointer += STRIDE) {
			float localX = CORNER_X[corner] * extentX;
			float localY = CORNER_Y[corner] * extentY;
			float px = centerX + localX;
			float py = centerY + localY;
			MemoryUtil.memPutFloat(pointer, pose.m00() * px + pose.m10() * py + pose.m30());
			MemoryUtil.memPutFloat(pointer + 4L, pose.m01() * px + pose.m11() * py + pose.m31());
			MemoryUtil.memPutFloat(pointer + 8L, pose.m02() * px + pose.m12() * py + pose.m32());
			MemoryUtil.memPutInt(pointer + COLOR_OFFSET, abgr);
			MemoryUtil.memPutFloat(pointer + LOCAL_OFFSET, localX);
			MemoryUtil.memPutFloat(pointer + LOCAL_OFFSET + 4L, localY);
			MemoryUtil.memPutFloat(pointer + SHAPE_OFFSET, halfWidth);
			MemoryUtil.memPutFloat(pointer + SHAPE_OFFSET + 4L, halfHeight);
			MemoryUtil.memPutFloat(pointer + SHAPE_OFFSET + 8L, clampedRadius);
			MemoryUtil.memPutFloat(pointer + SHAPE_OFFSET + 12L, stroke);
			MemoryUtil.memPutFloat(pointer + SOFTNESS_OFFSET, blur);
		}
		vertices += 4;
		return this;
	}

	@Override
	public void close() {
		if (poses == null) {
			throw new IllegalStateException("Shape batch is not active");
		}
		poses = null;
		int count = vertices;
		vertices = 0;

		ByteBufferBuilder.Result result = buffer.build();
		if (result == null) {
			return;
		}
		if (BatonShaders.shape() == null) {
			result.close();
			return;
		}

		VertexFormat.Mode mode = VertexFormat.Mode.QUADS;
		RenderSystem.setShader(BatonShaders::shape);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		BufferUploader.drawWithShader(new MeshData(result, new MeshData.DrawState(FORMAT, count, mode.indexCount(count), mode, VertexFormat.IndexType.least(count))));
		RenderSystem.disableBlend();
	}

	private static VertexFormatElement registerGenericElement(int count) {
		for (int id = VertexFormatElement.MAX_COUNT - 1; id >= 0; id--) {
			if (VertexFormatElement.byId(id) == null) {
				return VertexFormatElement.register(id, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, count);
			}
		}
		throw new IllegalStateException("No free vertex format element slots");
	}
}
