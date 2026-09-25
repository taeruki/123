package com.baton.client.mixin;

import java.util.function.Supplier;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	private static final int HEADER_HEIGHT = 40;
	private static final Component HEADING = Component.literal("Параметры");

	@Shadow
	@Final
	private HeaderAndFooterLayout layout;

	@Shadow
	@Final
	private Options options;

	private OptionsScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	protected abstract void repositionElements();

	@Shadow
	private Button openScreenButton(Component component, Supplier<Screen> supplier) {
		throw new AssertionError();
	}

	@Shadow
	private void applyPacks(PackRepository repository) {
		throw new AssertionError();
	}

	@Shadow
	private LayoutElement createOnlineButton() {
		throw new AssertionError();
	}

	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	private void baton$init(CallbackInfo ci) {
		layout.setHeaderHeight(HEADER_HEIGHT);
		layout.addTitleHeader(HEADING, font);
		GridLayout grid = new GridLayout();
		grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		GridLayout.RowHelper rows = grid.createRowHelper(2);
		rows.addChild(options.fov().createButton(options));
		rows.addChild(openScreenButton(Component.translatable("options.video"), () -> new VideoSettingsScreen(this, minecraft, options)));
		rows.addChild(openScreenButton(Component.translatable("options.sounds"), () -> new SoundOptionsScreen(this, options)));
		rows.addChild(openScreenButton(Component.translatable("options.controls"), () -> new ControlsScreen(this, options)));
		rows.addChild(openScreenButton(Component.translatable("options.chat"), () -> new ChatOptionsScreen(this, options)));
		rows.addChild(openScreenButton(Component.translatable("options.skinCustomisation"), () -> new SkinCustomizationScreen(this, options)));
		rows.addChild(openScreenButton(Component.translatable("options.language"), () -> new LanguageSelectScreen(this, options, minecraft.getLanguageManager())));
		rows.addChild(openScreenButton(Component.translatable("options.resourcepack"), () -> new PackSelectionScreen(
			minecraft.getResourcePackRepository(), this::applyPacks, minecraft.getResourcePackDirectory(), Component.translatable("resourcePack.title")
		)));
		if (minecraft.level != null && minecraft.hasSingleplayerServer()) {
			rows.addChild(createOnlineButton(), 2);
		}
		layout.addToContents(grid);
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
		ci.cancel();
	}
}
