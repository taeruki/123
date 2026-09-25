package com.baton.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	@Shadow
	@Final
	private static Component PLAYER_REPORTING;

	@Shadow
	@Final
	private static Tooltip CUSTOM_OPTIONS_TOOLTIP;

	private PauseScreenMixin(Component title) {
		super(title);
	}

	@Redirect(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/PauseScreen;addFeedbackButtons(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;)V"))
	private void baton$skipFeedback(Screen screen, GridLayout.RowHelper rows) {
	}

	@Redirect(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
	private LayoutElement baton$skipReporting(GridLayout.RowHelper rows, LayoutElement element) {
		return element instanceof AbstractWidget widget && widget.getMessage() == PLAYER_REPORTING ? element : rows.addChild(element);
	}

	@Inject(method = "addFeedbackSubscreenAndCustomDialogButtons", at = @At("HEAD"), cancellable = true)
	private void baton$dialogOnly(Minecraft minecraft, Holder<Dialog> dialog, GridLayout.RowHelper rows, CallbackInfo ci) {
		rows.addChild(Button.builder(dialog.value().common().computeExternalTitle(), button -> minecraft.player.connection.showDialog(dialog, this))
			.width(98)
			.tooltip(CUSTOM_OPTIONS_TOOLTIP)
			.build());
		ci.cancel();
	}
}
