package com.baton.client.render.shader;

import com.baton.client.BatonClient;
import com.baton.client.render.ShapeBatch;
import java.io.IOException;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class BatonShaders {
	@Nullable
	private static ShaderInstance shape;

	private BatonShaders() {
	}

	public static void register(CoreShaderRegistrationCallback.RegistrationContext context) throws IOException {
		context.register(ResourceLocation.fromNamespaceAndPath(BatonClient.MOD_ID, "shape"), ShapeBatch.FORMAT, program -> shape = program);
	}

	@Nullable
	public static ShaderInstance shape() {
		return shape;
	}
}
