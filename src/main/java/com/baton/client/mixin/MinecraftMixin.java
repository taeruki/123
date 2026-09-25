package com.baton.client.mixin;

import com.baton.client.profile.Profiles;
import com.baton.client.screen.MainMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	@Nullable
	public ClientLevel level;

	@Shadow
	public abstract void setScreen(@Nullable Screen screen);

	@Inject(method = "<init>", at = @At("TAIL"))
	private void baton$loadProfiles(GameConfig config, CallbackInfo ci) {
		Profiles.load((Minecraft) (Object) this);
	}

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void baton$replaceTitleScreen(@Nullable Screen screen, CallbackInfo ci) {
		if (screen instanceof TitleScreen || screen == null && level == null) {
			setScreen(new MainMenuScreen());
			ci.cancel();
		}
	}
}
