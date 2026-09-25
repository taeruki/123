package com.baton.client.screen;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class MainMenuScreen extends BatonScreen {
	private static final Identifier LOGO = Ui.id("textures/gui/logo.png");
	private static final int LOGO_TEXTURE = 288;
	private static final int LOGO_SIZE = 72;
	private static final int MENU_WIDTH = 86;
	private static final int ROW = 30;
	private static final int INSET = 3;
	private static final int GAP = 8;
	private static final int EXIT_WIDTH = 68;
	private static final int EXIT_HEIGHT = 22;
	private static final int KNOB = 18;
	private static final float QUIT_THRESHOLD = 0.97F;
	private static final float LABEL = 8.5F;
	private static final int UNDERLINE = 0xB3FFFFFF;
	private static final int TRAIL = 0x14FFFFFF;
	private static final int KNOB_ARMED = 0xFFFF7A85;

	private final Entry[] entries = {
		new Entry("Одиночная", () -> minecraft.setScreen(new WorldsScreen(this))),
		new Entry("Серверы", () -> minecraft.setScreen(new ServersScreen(this))),
		new Entry("Профили", () -> minecraft.setScreen(new ProfilesScreen(this))),
		new Entry("Параметры", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)))
	};
	private int menuX;
	private int menuY;
	private int logoY;
	private int exitX;
	private int exitY;
	private float highlightY;
	private float highlightAlpha;
	private float knob;
	private boolean dragging;
	private double grab;

	@Override
	protected void init() {
		logoY = (height - LOGO_SIZE - GAP - menuHeight()) / 2 - 10;
		menuX = (width - MENU_WIDTH) / 2;
		menuY = logoY + LOGO_SIZE + GAP;
		exitX = (width - EXIT_WIDTH) / 2;
		exitY = height - EXIT_HEIGHT - 22;
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float appear) {
		int hovered = hovered(mouseX, mouseY);
		if (hovered >= 0) {
			float target = rowY(hovered);
			highlightY = highlightAlpha < 0.01F ? target : Ui.approach(highlightY, target, 22.0F, delta);
		}
		highlightAlpha = Ui.approach(highlightAlpha, hovered >= 0 ? 1.0F : 0.0F, 16.0F, delta);
		if (!dragging) {
			knob = Ui.approach(knob, 0.0F, 14.0F, delta);
		}
		if (hovered >= 0 || dragging || overKnob(mouseX, mouseY)) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		int tint = ARGB.colorFromFloat(appear, appear, appear, appear);
		graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, LOGO, (width - LOGO_SIZE) / 2, logoY, 0, 0, LOGO_SIZE, LOGO_SIZE, LOGO_TEXTURE, LOGO_TEXTURE, LOGO_TEXTURE, LOGO_TEXTURE, tint);

		Ui.panel(graphics, menuX, menuY, MENU_WIDTH, menuHeight(), 11.0F, appear);
		float highlight = appear * highlightAlpha;
		Ui.rect(graphics, menuX + INSET, highlightY, MENU_WIDTH - INSET * 2, ROW, 8.0F, Ui.fade(Ui.SELECTED, highlight));
		Ui.rect(graphics, width / 2.0F - 4.0F, highlightY + ROW - 3.0F, 8.0F, 1.0F, 0.5F, Ui.fade(UNDERLINE, highlight));
		for (int i = 0; i < entries.length; i++) {
			float focus = highlightAlpha * Mth.clamp(1.0F - Math.abs(highlightY - rowY(i)) / ROW, 0.0F, 1.0F);
			UiFont.drawCentered(graphics, entries[i].label, width / 2.0F, rowY(i) + ROW / 2.0F, LABEL, Ui.fade(ARGB.srgbLerp(focus, Ui.TEXT, Ui.TEXT_ACTIVE), appear));
		}

		float radius = EXIT_HEIGHT / 2.0F;
		float knobX = knobX();
		Ui.panel(graphics, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, radius, appear);
		Ui.rect(graphics, exitX + 2, exitY + 2, knobX - exitX - 2 + KNOB, KNOB, KNOB / 2.0F, Ui.fade(TRAIL, appear));
		float armed = Mth.clamp((knob - 0.75F) / (QUIT_THRESHOLD - 0.75F), 0.0F, 1.0F);
		Ui.rect(graphics, knobX, exitY + 2, KNOB, KNOB, KNOB / 2.0F, Ui.fade(ARGB.srgbLerp(armed, Ui.ACCENT, KNOB_ARMED), appear));
		UiFont.drawCentered(graphics, "→", knobX + KNOB / 2.0F, exitY + radius, 9.0F, Ui.fade(Ui.ON_ACCENT, appear));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			int hovered = hovered(event.x(), event.y());
			if (hovered >= 0) {
				click();
				entries[hovered].action.run();
				return true;
			}
			if (overKnob(event.x(), event.y())) {
				dragging = true;
				grab = event.x() - knobX();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (!dragging) {
			return super.mouseDragged(event, dragX, dragY);
		}
		knob = Mth.clamp((float) ((event.x() - grab - exitX - 2) / travel()), 0.0F, 1.0F);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (!dragging) {
			return super.mouseReleased(event);
		}
		dragging = false;
		if (knob >= QUIT_THRESHOLD) {
			minecraft.stop();
		}
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private int menuHeight() {
		return entries.length * ROW + INSET * 2;
	}

	private int hovered(double x, double y) {
		for (int i = 0; i < entries.length; i++) {
			if (inside(x, y, menuX + INSET, rowY(i), MENU_WIDTH - INSET * 2, ROW)) {
				return i;
			}
		}
		return -1;
	}

	private int rowY(int index) {
		return menuY + INSET + index * ROW;
	}

	private float travel() {
		return EXIT_WIDTH - KNOB - 4;
	}

	private float knobX() {
		return exitX + 2 + knob * travel();
	}

	private boolean overKnob(double x, double y) {
		return inside(x, y, knobX() - 2, exitY, KNOB + 4, EXIT_HEIGHT);
	}

	private record Entry(String label, Runnable action) {
	}
}
