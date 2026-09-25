package com.baton.client.mixin;

import com.baton.client.profile.Profiles;
import com.baton.client.render.UiFont;
import com.baton.client.screen.MainMenuScreen;
import com.baton.client.screen.ServersScreen;
import com.baton.client.screen.WorldsScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	@Nullable
	public ClientLevel level;

	@Shadow
	public abstract void setScreen(@Nullable Screen screen);

	@Inject(method = "<init>", at = @At("TAIL"))
	private void baton$load(GameConfig config, CallbackInfo ci) {
		UiFont.load();
		Profiles.load((Minecraft) (Object) this);
	}

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void baton$replaceScreens(@Nullable Screen screen, CallbackInfo ci) {
		Screen replacement = screen instanceof TitleScreen || screen == null && level == null ? new MainMenuScreen()
			: screen instanceof JoinMultiplayerScreen ? new ServersScreen(new MainMenuScreen())
			: screen instanceof SelectWorldScreen ? new WorldsScreen(new MainMenuScreen())
			: null;
		if (replacement != null) {
			setScreen(replacement);
			ci.cancel();
		}
	}

	@Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
	private void baton$title(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue("Baton " + SharedConstants.getCurrentVersion().name());
	}
}
