package com.baton.client;

import com.baton.client.render.shader.BatonShaders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;

public final class BatonClient implements ClientModInitializer {
	public static final String MOD_ID = "baton";

	@Override
	public void onInitializeClient() {
		CoreShaderRegistrationCallback.EVENT.register(BatonShaders::register);
	}
}
