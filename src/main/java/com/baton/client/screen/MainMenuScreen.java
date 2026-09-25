package com.baton.client.screen;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class MainMenuScreen extends BatonScreen {
	private static final Identifier LOGO = Ui.id("textures/gui/logo.png");
	private static final int LOGO_TEXTURE = 288;
	private static final int LOGO_SIZE = 72;
	private static final int MENU_WIDTH = 88;
	private static final int ROW = 22;
	private static final int INSET = 3;
	private static final int GAP = 8;
	private static final int EXIT_WIDTH = 84;
	private static final int EXIT_HEIGHT = 20;
	private static final int KNOB = 16;
	private static final float LABEL = 7.5F;
	private static final float SMALL = 7.0F;
	private static final int PANEL = 0x08FFFFFF;
	private static final int EDGE = 0x14FFFFFF;
	private static final int HIGHLIGHT = 0x10FFFFFF;
	private static final int ACCENT = 0xFF9ED0FF;
	private static final int TEXT = 0xFF8C98AB;
	private static final int TEXT_ACTIVE = 0xFFF2F6FC;
	private static final int MUTED = 0xFF6B788C;
	private static final int TRAIL = 0xFF1A2537;
	private static final int KNOB_COLOR = 0xFFE6F2FF;
	private static final int KNOB_ICON = 0xFF0B1220;

	private final Entry[] entries = {
		new Entry("Одиночная", () -> minecraft.setScreen(new SelectWorldScreen(this))),
		new Entry("Серверы", () -> minecraft.setScreen(new JoinMultiplayerScreen(this))),
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

		Ui.rect(graphics, menuX, menuY, MENU_WIDTH, menuHeight(), 9.0F, Ui.fade(PANEL, appear));
		Ui.outline(graphics, menuX, menuY, MENU_WIDTH, menuHeight(), 9.0F, 0.5F, Ui.fade(EDGE, appear));
		float highlight = appear * highlightAlpha;
		Ui.rect(graphics, menuX + INSET, highlightY, MENU_WIDTH - INSET * 2, ROW, 7.0F, Ui.fade(HIGHLIGHT, highlight));
		Ui.rect(graphics, width / 2.0F - 4.0F, highlightY + ROW - 3.5F, 8.0F, 1.0F, 0.5F, Ui.fade(ACCENT, highlight));
		for (int i = 0; i < entries.length; i++) {
			float focus = highlightAlpha * Mth.clamp(1.0F - Math.abs(highlightY - rowY(i)) / ROW, 0.0F, 1.0F);
			UiFont.drawCentered(graphics, entries[i].label, width / 2.0F, rowY(i) + ROW / 2.0F, LABEL, Ui.fade(ARGB.srgbLerp(focus, TEXT, TEXT_ACTIVE), appear));
		}

		float radius = EXIT_HEIGHT / 2.0F;
		Ui.rect(graphics, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, radius, Ui.fade(PANEL, appear));
		Ui.outline(graphics, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, radius, 0.5F, Ui.fade(EDGE, appear));
		UiFont.drawCentered(graphics, "Выход", width / 2.0F, exitY + radius, SMALL, Ui.fade(MUTED, appear));
		float knobX = knobX();
		Ui.rect(graphics, exitX + 2, exitY + 2, knobX - exitX - 2 + KNOB, KNOB, KNOB / 2.0F, Ui.fade(TRAIL, appear));
		Ui.rect(graphics, knobX, exitY + 2, KNOB, KNOB, KNOB / 2.0F, Ui.fade(KNOB_COLOR, appear));
		UiFont.drawCentered(graphics, "→", knobX + KNOB / 2.0F, exitY + radius, SMALL, Ui.fade(KNOB_ICON, appear));
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
		if (knob >= 1.0F) {
			minecraft.stop();
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (!dragging) {
			return super.mouseReleased(event);
		}
		dragging = false;
		if (knob > 0.9F) {
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
		return inside(x, y, knobX(), exitY + 2, KNOB, KNOB);
	}

	private record Entry(String label, Runnable action) {
	}
}
