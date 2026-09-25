package com.baton.client.screen;

import com.baton.client.render.Ui;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class MainMenuScreen extends BatonScreen {
	private static final Identifier LOGO = Ui.id("textures/gui/logo.png");
	private static final int LOGO_TEXTURE = 288;
	private static final int LOGO_SIZE = 80;
	private static final int MENU_WIDTH = 116;
	private static final int ROW = 20;
	private static final int INSET = 2;
	private static final int GAP = 10;
	private static final int EXIT_WIDTH = 84;
	private static final int EXIT_HEIGHT = 20;
	private static final int KNOB = 16;
	private static final int PANEL = 0x08FFFFFF;
	private static final int EDGE = 0x14FFFFFF;
	private static final int HIGHLIGHT = 0x10FFFFFF;
	private static final int ACCENT = 0xFF9ED0FF;
	private static final int TEXT = 0xFF8C98AB;
	private static final int TEXT_ACTIVE = 0xFFF2F6FC;
	private static final int MUTED = 0xFF6B788C;
	private static final int KNOB_COLOR = 0xFFE6F2FF;
	private static final int KNOB_ICON = 0xFF0B1220;

	private final Entry[] entries = {
		entry("Одиночная", () -> minecraft.setScreen(new SelectWorldScreen(this))),
		entry("Серверы", () -> minecraft.setScreen(new JoinMultiplayerScreen(this))),
		entry("Профили", () -> minecraft.setScreen(new ProfilesScreen(this))),
		entry("Параметры", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)))
	};
	private final Component exitLabel = Ui.text("Выход");
	private final Component exitIcon = Ui.text("→");
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
		int menuHeight = entries.length * ROW + INSET * 2;
		logoY = (height - LOGO_SIZE - GAP - menuHeight) / 2 - 8;
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

		int menuHeight = entries.length * ROW + INSET * 2;
		Ui.rect(graphics, menuX, menuY, MENU_WIDTH, menuHeight, 8.0F, Ui.fade(PANEL, appear));
		Ui.outline(graphics, menuX, menuY, MENU_WIDTH, menuHeight, 8.0F, 0.5F, Ui.fade(EDGE, appear));
		float highlight = appear * highlightAlpha;
		Ui.rect(graphics, menuX + INSET, highlightY, MENU_WIDTH - INSET * 2, ROW, 6.0F, Ui.fade(HIGHLIGHT, highlight));
		Ui.rect(graphics, width / 2.0F - 4.0F, highlightY + ROW - 3.0F, 8.0F, 1.0F, 0.5F, Ui.fade(ACCENT, highlight));
		for (int i = 0; i < entries.length; i++) {
			float focus = highlightAlpha * Mth.clamp(1.0F - Math.abs(highlightY - rowY(i)) / ROW, 0.0F, 1.0F);
			Entry entry = entries[i];
			graphics.drawString(font, entry.label, (width - entry.width) / 2, rowY(i) + 6, Ui.fade(ARGB.srgbLerp(focus, TEXT, TEXT_ACTIVE), appear), false);
		}

		Ui.rect(graphics, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, EXIT_HEIGHT / 2.0F, Ui.fade(PANEL, appear));
		Ui.outline(graphics, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, EXIT_HEIGHT / 2.0F, 0.5F, Ui.fade(EDGE, appear));
		int labelX = exitX + KNOB + 4 + (EXIT_WIDTH - KNOB - 4 - font.width(exitLabel)) / 2;
		graphics.drawString(font, exitLabel, labelX, exitY + 6, Ui.fade(MUTED, appear * (1.0F - knob)), false);
		float knobX = knobX();
		Ui.rect(graphics, knobX, exitY + 2, KNOB, KNOB, KNOB / 2.0F, Ui.fade(KNOB_COLOR, appear));
		graphics.pose().pushMatrix();
		graphics.pose().translate(knobX + (KNOB - font.width(exitIcon)) / 2.0F, exitY + 6);
		graphics.drawString(font, exitIcon, 0, 0, Ui.fade(KNOB_ICON, appear), false);
		graphics.pose().popMatrix();
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

	private Entry entry(String label, Runnable action) {
		Component text = Ui.text(label);
		return new Entry(text, font.width(text), action);
	}

	private record Entry(Component label, int width, Runnable action) {
	}
}
