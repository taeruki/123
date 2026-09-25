package com.baton.client.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.tutorial.TutorialSteps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {
	@Shadow
	public String languageCode;

	@Shadow
	public boolean onboardAccessibility;

	@Shadow
	public TutorialSteps tutorialStep;

	@Shadow
	public boolean skipMultiplayerWarning;

	@Shadow
	public boolean joinedFirstServer;

	@Shadow
	@Final
	private OptionInstance<Integer> guiScale;

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
	private void baton$applyDefaults(CallbackInfo ci) {
		languageCode = "ru_ru";
		onboardAccessibility = false;
		tutorialStep = TutorialSteps.NONE;
		skipMultiplayerWarning = true;
		joinedFirstServer = true;
		guiScale.set(2);
	}
}
