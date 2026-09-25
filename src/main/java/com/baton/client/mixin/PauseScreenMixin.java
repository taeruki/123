package com.baton.client.mixin;

import com.baton.client.screen.component.HeadingWidget;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	private static final int WIDE = 204;
	private static final int TOP_PADDING = 50;

	@Shadow
	@Final
	private static Component RETURN_TO_GAME;

	@Shadow
	@Final
	private static Component ADVANCEMENTS;

	@Shadow
	@Final
	private static Component STATS;

	@Shadow
	@Final
	private static Component OPTIONS;

	@Shadow
	@Final
	private static Component SHARE_TO_LAN;

	@Shadow
	@Final
	private static Tooltip CUSTOM_OPTIONS_TOOLTIP;

	@Shadow
	@Nullable
	private Button disconnectButton;

	private PauseScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	private Button openScreenButton(Component component, Supplier<Screen> supplier) {
		throw new AssertionError();
	}

	@Shadow
	private Optional<? extends Holder<Dialog>> getCustomAdditions() {
		throw new AssertionError();
	}

	@Redirect(method = "init", at = @At(value = "NEW", target = "(IIIILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)Lnet/minecraft/client/gui/components/StringWidget;"))
	private StringWidget baton$heading(int x, int y, int width, int height, Component title, Font font) {
		return new HeadingWidget(x, y, width, height, title, font);
	}

	@Inject(method = "createPauseMenu", at = @At("HEAD"), cancellable = true)
	private void baton$menu(CallbackInfo ci) {
		GridLayout grid = new GridLayout();
		grid.defaultCellSetting().padding(4, 4, 4, 0);
		GridLayout.RowHelper rows = grid.createRowHelper(2);
		rows.addChild(Button.builder(RETURN_TO_GAME, button -> {
			minecraft.setScreen(null);
			minecraft.mouseHandler.grabMouse();
		}).width(WIDE).build(), 2, grid.newCellSettings().paddingTop(TOP_PADDING));
		rows.addChild(openScreenButton(ADVANCEMENTS, () -> new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this)));
		rows.addChild(openScreenButton(STATS, () -> new StatsScreen(this, minecraft.player.getStats())));
		getCustomAdditions().ifPresent(dialog -> rows.addChild(
			Button.builder(dialog.value().common().computeExternalTitle(), button -> minecraft.player.connection.showDialog(dialog, this))
				.width(WIDE)
				.tooltip(CUSTOM_OPTIONS_TOOLTIP)
				.build(),
			2
		));
		if (minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished()) {
			rows.addChild(openScreenButton(OPTIONS, () -> new OptionsScreen(this, minecraft.options)));
			rows.addChild(openScreenButton(SHARE_TO_LAN, () -> new ShareToLanScreen(this)));
		} else {
			rows.addChild(Button.builder(OPTIONS, button -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))).width(WIDE).build(), 2);
		}
		disconnectButton = rows.addChild(Button.builder(CommonComponents.disconnectButtonLabel(minecraft.isLocalServer()), button -> {
			button.active = false;
			minecraft.getReportingContext().draftReportHandled(minecraft, this, () -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
		}).width(WIDE).build(), 2);
		grid.arrangeElements();
		FrameLayout.alignInRectangle(grid, 0, 0, width, height, 0.5F, 0.25F);
		grid.visitWidgets(this::addRenderableWidget);
		ci.cancel();
	}
}
