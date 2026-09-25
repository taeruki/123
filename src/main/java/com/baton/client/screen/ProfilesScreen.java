package com.baton.client.screen;

import com.baton.client.profile.Profiles;
import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

public final class ProfilesScreen extends BatonScreen {
	private static final int WIDTH = 136;
	private static final int PAD = 5;
	private static final int TITLE_HEIGHT = 32;
	private static final int ROW = 20;
	private static final int HEAD = 12;
	private static final int VISIBLE_ROWS = 9;
	private static final int CONTROL = 20;
	private static final int REMOVE = 16;
	private static final int GAP = 6;
	private static final float TITLE = 12.0F;
	private static final float TEXT_SIZE = 8.5F;
	private static final float SMALL = 7.5F;
	private static final int HOVER = 0x08FFFFFF;
	private static final int SELECTED = 0x10FFFFFF;
	private static final int FIELD = 0x40000000;
	private static final int FIELD_EDGE = 0x0EFFFFFF;
	private static final int BUTTON = 0x0EFFFFFF;
	private static final int CONFIRM = 0xFFE9ECF2;
	private static final int CONFIRM_ICON = 0xFF0C0E13;
	private static final int TITLE_COLOR = 0xFFECEEF3;
	private static final int TEXT = 0xFF979DAA;
	private static final int TEXT_ACTIVE = 0xFFF4F5F8;
	private static final int MUTED = 0xFF5F6573;
	private static final int SCROLLBAR = 0x1AFFFFFF;
	private static final int DANGER = 0xFFFF8A94;
	private static final int DANGER_BUTTON = 0x12FF5A64;
	private static final int DANGER_HOVER = 0x22FF5A64;

	private final Screen parent;
	private final List<Row> rows = new ArrayList<>();
	private String input = "";
	private String count = "";
	private int x;
	private int titleY;
	private int cardY;
	private int cardHeight;
	private int listY;
	private int listHeight;
	private int fieldY;
	private int buttonsY;
	private float scroll;
	private float scrollTarget;

	public ProfilesScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	protected void init() {
		refresh();
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float appear) {
		scroll = Ui.approach(scroll, scrollTarget, 18.0F, delta);
		int hoveredRow = hoveredRow(mouseX, mouseY);
		boolean valid = Profiles.valid(input);
		boolean overConfirm = valid && overConfirm(mouseX, mouseY);
		boolean overRandom = overRandom(mouseX, mouseY);
		boolean overClear = overClear(mouseX, mouseY);
		if (hoveredRow >= 0 || overConfirm || overRandom || overClear) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		UiFont.drawCentered(graphics, "Профили", width / 2.0F, titleY + 6, TITLE, Ui.fade(TITLE_COLOR, appear));
		String name = Profiles.current();
		float nameWidth = UiFont.width(name, SMALL);
		float subtitleX = (width - nameWidth - UiFont.width(count, SMALL)) / 2.0F;
		UiFont.draw(graphics, name, subtitleX, titleY + 20, SMALL, Ui.fade(TEXT, appear));
		UiFont.draw(graphics, count, subtitleX + nameWidth, titleY + 20, SMALL, Ui.fade(MUTED, appear));

		Ui.panel(graphics, x, cardY, WIDTH, cardHeight, 10.0F, appear);
		int rowX = x + PAD;
		int rowWidth = WIDTH - PAD * 2;
		Ui.clip(graphics, rowX, listY, rowX + rowWidth, listY + listHeight);
		for (int i = 0; i < rows.size(); i++) {
			float rowY = listY + i * ROW - scroll;
			if (rowY + ROW <= listY || rowY >= listY + listHeight) {
				continue;
			}
			Row row = rows.get(i);
			boolean selected = row.name.equals(name);
			boolean hovered = i == hoveredRow;
			if (selected || hovered) {
				Ui.rect(graphics, rowX, rowY, rowWidth, ROW, 7.0F, Ui.fade(selected ? SELECTED : HOVER, appear));
			}
			PlayerFaceRenderer.draw(graphics, row.skin, rowX + 4, Math.round(rowY) + (ROW - HEAD) / 2, HEAD, ARGB.white(appear));
			UiFont.draw(graphics, row.name, rowX + HEAD + 10, rowY + ROW / 2.0F, TEXT_SIZE, Ui.fade(selected || hovered ? TEXT_ACTIVE : TEXT, appear));
			float iconX = rowX + rowWidth - REMOVE / 2.0F;
			if (selected) {
				Ui.rect(graphics, iconX - 1.5F, rowY + ROW / 2.0F - 1.5F, 3.0F, 3.0F, 1.5F, Ui.fade(TEXT_ACTIVE, appear));
			} else if (hovered) {
				UiFont.drawCentered(graphics, "×", iconX, rowY + ROW / 2.0F, 9.0F, Ui.fade(overRemove(mouseX) ? DANGER : MUTED, appear));
			}
		}
		Ui.unclip(graphics);
		float maxScroll = maxScroll();
		if (maxScroll > 0.0F) {
			float barHeight = listHeight * listHeight / (float) (rows.size() * ROW);
			Ui.rect(graphics, x + WIDTH - 3.0F, listY + scroll / maxScroll * (listHeight - barHeight), 1.0F, barHeight, 0.5F, Ui.fade(SCROLLBAR, appear));
		}

		int fieldWidth = rowWidth - CONTROL - 3;
		Ui.rect(graphics, rowX, fieldY, fieldWidth, CONTROL, 7.0F, Ui.fade(FIELD, appear));
		Ui.outline(graphics, rowX, fieldY, fieldWidth, CONTROL, 7.0F, 0.5F, Ui.fade(FIELD_EDGE, appear));
		Ui.clip(graphics, rowX + 1, fieldY, rowX + fieldWidth - 1, fieldY + CONTROL);
		float inputWidth = UiFont.width(input, TEXT_SIZE);
		float textX = rowX + 7 - Math.max(0.0F, inputWidth - fieldWidth + 16);
		float centerY = fieldY + CONTROL / 2.0F;
		UiFont.draw(graphics, input.isEmpty() ? "Никнейм" : input, textX, centerY, TEXT_SIZE, Ui.fade(input.isEmpty() ? MUTED : TEXT_ACTIVE, appear));
		if (Util.getMillis() / 500 % 2 == 0) {
			Ui.rect(graphics, textX + inputWidth + 0.5F, centerY - 4.5F, 0.5F, 9.0F, 0.0F, Ui.fade(TEXT_ACTIVE, appear));
		}
		Ui.unclip(graphics);
		int confirmX = confirmX();
		Ui.rect(graphics, confirmX, fieldY, CONTROL, CONTROL, 7.0F, Ui.fade(valid ? CONFIRM : BUTTON, appear));
		UiFont.drawCentered(graphics, "✓", confirmX + CONTROL / 2.0F, centerY, TEXT_SIZE, Ui.fade(valid ? CONFIRM_ICON : MUTED, appear));

		int buttonWidth = buttonWidth();
		float buttonCenter = buttonsY + CONTROL / 2.0F;
		Ui.panel(graphics, x, buttonsY, buttonWidth, CONTROL, 8.0F, appear);
		if (overRandom) {
			Ui.rect(graphics, x, buttonsY, buttonWidth, CONTROL, 8.0F, Ui.fade(HOVER, appear));
		}
		UiFont.drawCentered(graphics, "Случайный", x + buttonWidth / 2.0F, buttonCenter, SMALL, Ui.fade(overRandom ? TEXT_ACTIVE : TEXT, appear));
		int clearX = x + WIDTH - buttonWidth;
		Ui.rect(graphics, clearX, buttonsY, buttonWidth, CONTROL, 8.0F, Ui.fade(overClear ? DANGER_HOVER : DANGER_BUTTON, appear));
		UiFont.drawCentered(graphics, "Удалить все", clearX + buttonWidth / 2.0F, buttonCenter, SMALL, Ui.fade(DANGER, appear));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		double mouseX = event.x();
		double mouseY = event.y();
		int row = hoveredRow(mouseX, mouseY);
		if (row >= 0) {
			String name = rows.get(row).name;
			if (overRemove(mouseX) && !name.equals(Profiles.current())) {
				Profiles.remove(name);
			} else {
				Profiles.select(name);
			}
		} else if (overConfirm(mouseX, mouseY)) {
			confirm();
			return true;
		} else if (overRandom(mouseX, mouseY)) {
			Profiles.select(Profiles.random());
			scrollTarget = Float.MAX_VALUE;
		} else if (overClear(mouseX, mouseY)) {
			Profiles.clear();
		} else {
			return super.mouseClicked(event, doubleClick);
		}
		click();
		refresh();
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * ROW, 0.0F, maxScroll());
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (input.length() < Profiles.MAX_LENGTH && Profiles.allowed(event.codepoint())) {
			input += event.codepointAsString();
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation()) {
			confirm();
			return true;
		}
		if (event.key() == InputConstants.KEY_BACKSPACE && !input.isEmpty()) {
			input = input.substring(0, input.length() - 1);
			return true;
		}
		if (event.isPaste()) {
			input = Profiles.sanitize(input + minecraft.keyboardHandler.getClipboard());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private void confirm() {
		if (Profiles.valid(input)) {
			click();
			Profiles.select(input);
			input = "";
			scrollTarget = Float.MAX_VALUE;
			refresh();
		}
	}

	private void refresh() {
		rows.clear();
		for (String name : Profiles.names()) {
			rows.add(new Row(name, DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name))));
		}
		count = "  |  " + rows.size() + " " + plural(rows.size());
		listHeight = Math.min(rows.size(), VISIBLE_ROWS) * ROW;
		cardHeight = PAD * 3 + listHeight + CONTROL;
		x = (width - WIDTH) / 2;
		titleY = (height - TITLE_HEIGHT - cardHeight - GAP - CONTROL) / 2;
		cardY = titleY + TITLE_HEIGHT;
		listY = cardY + PAD;
		fieldY = listY + listHeight + PAD;
		buttonsY = cardY + cardHeight + GAP;
		scrollTarget = Mth.clamp(scrollTarget, 0.0F, maxScroll());
		scroll = Math.min(scroll, maxScroll());
	}

	private float maxScroll() {
		return Math.max(0, rows.size() * ROW - listHeight);
	}

	private int confirmX() {
		return x + WIDTH - PAD - CONTROL;
	}

	private int buttonWidth() {
		return (WIDTH - GAP) / 2;
	}

	private int hoveredRow(double mouseX, double mouseY) {
		if (!inside(mouseX, mouseY, x + PAD, listY, WIDTH - PAD * 2, listHeight)) {
			return -1;
		}
		int index = (int) ((mouseY - listY + scroll) / ROW);
		return index < rows.size() ? index : -1;
	}

	private boolean overRemove(double mouseX) {
		return mouseX >= x + WIDTH - PAD - REMOVE;
	}

	private boolean overConfirm(double mouseX, double mouseY) {
		return inside(mouseX, mouseY, confirmX(), fieldY, CONTROL, CONTROL);
	}

	private boolean overRandom(double mouseX, double mouseY) {
		return inside(mouseX, mouseY, x, buttonsY, buttonWidth(), CONTROL);
	}

	private boolean overClear(double mouseX, double mouseY) {
		return inside(mouseX, mouseY, x + WIDTH - buttonWidth(), buttonsY, buttonWidth(), CONTROL);
	}

	private static String plural(int count) {
		int last = count % 10;
		int lastTwo = count % 100;
		if (last == 1 && lastTwo != 11) {
			return "профиль";
		}
		return last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14) ? "профиля" : "профилей";
	}

	private record Row(String name, PlayerSkin skin) {
	}
}
