package com.baton.client.screen;

import com.baton.client.profile.Profiles;
import com.baton.client.render.Ui;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

public final class ProfilesScreen extends BatonScreen {
	private static final int CARD_WIDTH = 188;
	private static final int ROW = 20;
	private static final int VISIBLE_ROWS = 7;
	private static final int INSET = 4;
	private static final int CONTROL = 20;
	private static final int RANDOM_WIDTH = 62;
	private static final int HEADER = 32;
	private static final int CLEAR_WIDTH = 116;
	private static final int CLEAR_HEIGHT = 18;
	private static final int CLEAR_GAP = 8;
	private static final int REMOVE_WIDTH = 18;
	private static final float TITLE_SCALE = 1.5F;
	private static final int PANEL = 0x08FFFFFF;
	private static final int EDGE = 0x14FFFFFF;
	private static final int HOVER = 0x08FFFFFF;
	private static final int SELECTED = 0x10FFFFFF;
	private static final int FIELD = 0x33000000;
	private static final int BUTTON = 0x0CFFFFFF;
	private static final int BUTTON_HOVER = 0x16FFFFFF;
	private static final int ACCENT = 0xFF9ED0FF;
	private static final int TITLE = 0xFFDCEEFF;
	private static final int TEXT = 0xFF8C98AB;
	private static final int TEXT_ACTIVE = 0xFFF2F6FC;
	private static final int MUTED = 0xFF5E6A7D;
	private static final int CONFIRM = 0xFFE6F2FF;
	private static final int CONFIRM_ICON = 0xFF0B1220;
	private static final int DANGER = 0xFFFF8A94;
	private static final int DANGER_BUTTON = 0x10FF5A64;
	private static final int DANGER_BUTTON_HOVER = 0x1CFF5A64;

	private final Screen parent;
	private final List<Row> rows = new ArrayList<>();
	private final Component title = Ui.text("Профили");
	private final Component hint = Ui.text("Никнейм");
	private final Component currentTag = Ui.text("текущий");
	private final Component removeIcon = Ui.text("×");
	private final Component confirmIcon = Ui.text("✓");
	private final Component randomLabel = Ui.text("Случайный");
	private final Component clearLabel = Ui.text("Удалить все");
	private Component subtitle = Component.empty();
	private Component inputText = Component.empty();
	private String input = "";
	private int cardX;
	private int titleY;
	private int cardY;
	private int cardHeight;
	private int listY;
	private int listHeight;
	private int fieldY;
	private int clearY;
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
		boolean overConfirm = overConfirm(mouseX, mouseY);
		boolean overRandom = overRandom(mouseX, mouseY);
		boolean overClear = overClear(mouseX, mouseY);
		if (hoveredRow >= 0 || overConfirm && Profiles.valid(input) || overRandom || overClear) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2.0F, titleY);
		graphics.pose().scale(TITLE_SCALE);
		graphics.drawString(font, title, -font.width(title) / 2, 0, Ui.fade(TITLE, appear), false);
		graphics.pose().popMatrix();
		graphics.drawString(font, subtitle, (width - font.width(subtitle)) / 2, titleY + 17, Ui.fade(MUTED, appear), false);

		Ui.rect(graphics, cardX, cardY, CARD_WIDTH, cardHeight, 10.0F, Ui.fade(PANEL, appear));
		Ui.outline(graphics, cardX, cardY, CARD_WIDTH, cardHeight, 10.0F, 0.5F, Ui.fade(EDGE, appear));

		int rowX = cardX + INSET;
		int rowWidth = CARD_WIDTH - INSET * 2;
		Ui.clip(graphics, rowX, listY, rowX + rowWidth, listY + listHeight);
		for (int i = 0; i < rows.size(); i++) {
			int y = Math.round(listY + i * ROW - scroll);
			if (y + ROW <= listY || y >= listY + listHeight) {
				continue;
			}
			Row row = rows.get(i);
			boolean selected = row.name.equals(Profiles.current());
			if (selected || i == hoveredRow) {
				Ui.rect(graphics, rowX, y, rowWidth, ROW, 6.0F, Ui.fade(selected ? SELECTED : HOVER, appear));
			}
			PlayerFaceRenderer.draw(graphics, row.skin, rowX + 4, y + 4, 12, ARGB.white(appear));
			graphics.drawString(font, row.label, rowX + 22, y + 6, Ui.fade(selected ? TEXT_ACTIVE : TEXT, appear), false);
			if (selected) {
				graphics.drawString(font, currentTag, rowX + rowWidth - 7 - font.width(currentTag), y + 6, Ui.fade(ACCENT, appear), false);
			} else if (i == hoveredRow) {
				int color = overRemove(mouseX) ? DANGER : MUTED;
				graphics.drawString(font, removeIcon, rowX + rowWidth - (REMOVE_WIDTH + font.width(removeIcon)) / 2, y + 6, Ui.fade(color, appear), false);
			}
		}
		Ui.unclip(graphics);
		float maxScroll = maxScroll();
		if (maxScroll > 0.0F) {
			float barHeight = listHeight * listHeight / (float) (rows.size() * ROW);
			Ui.rect(graphics, cardX + CARD_WIDTH - 3.0F, listY + scroll / maxScroll * (listHeight - barHeight), 1.5F, barHeight, 0.75F, Ui.fade(EDGE, appear));
		}

		int fieldWidth = fieldWidth();
		Ui.rect(graphics, rowX, fieldY, fieldWidth, CONTROL, 6.0F, Ui.fade(FIELD, appear));
		Ui.outline(graphics, rowX, fieldY, fieldWidth, CONTROL, 6.0F, 0.5F, Ui.fade(EDGE, appear));
		Ui.clip(graphics, rowX + 1, fieldY, rowX + fieldWidth - 1, fieldY + CONTROL);
		int inputWidth = font.width(inputText);
		int textX = rowX + 7 - Math.max(0, inputWidth - fieldWidth + 16);
		if (input.isEmpty()) {
			graphics.drawString(font, hint, textX, fieldY + 6, Ui.fade(MUTED, appear), false);
		} else {
			graphics.drawString(font, inputText, textX, fieldY + 6, Ui.fade(TEXT_ACTIVE, appear), false);
		}
		if (Util.getMillis() / 500 % 2 == 0) {
			Ui.rect(graphics, textX + inputWidth + 1, fieldY + 5, 0.5F, 10.0F, 0.0F, Ui.fade(TEXT_ACTIVE, appear));
		}
		Ui.unclip(graphics);

		boolean valid = Profiles.valid(input);
		int confirmX = rowX + fieldWidth + INSET;
		Ui.rect(graphics, confirmX, fieldY, CONTROL, CONTROL, 6.0F, Ui.fade(valid ? CONFIRM : BUTTON, appear));
		graphics.drawString(font, confirmIcon, confirmX + (CONTROL - font.width(confirmIcon)) / 2, fieldY + 6, Ui.fade(valid ? CONFIRM_ICON : MUTED, appear), false);
		int randomX = confirmX + CONTROL + INSET;
		Ui.rect(graphics, randomX, fieldY, RANDOM_WIDTH, CONTROL, 6.0F, Ui.fade(overRandom ? BUTTON_HOVER : BUTTON, appear));
		graphics.drawString(font, randomLabel, randomX + (RANDOM_WIDTH - font.width(randomLabel)) / 2, fieldY + 6, Ui.fade(overRandom ? TEXT_ACTIVE : TEXT, appear), false);

		int clearX = (width - CLEAR_WIDTH) / 2;
		Ui.rect(graphics, clearX, clearY, CLEAR_WIDTH, CLEAR_HEIGHT, 6.0F, Ui.fade(overClear ? DANGER_BUTTON_HOVER : DANGER_BUTTON, appear));
		graphics.drawString(font, clearLabel, clearX + (CLEAR_WIDTH - font.width(clearLabel)) / 2, clearY + 5, Ui.fade(DANGER, appear), false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		double x = event.x();
		double y = event.y();
		int row = hoveredRow(x, y);
		if (row >= 0) {
			String name = rows.get(row).name;
			if (overRemove(x) && !name.equals(Profiles.current())) {
				Profiles.remove(name);
			} else {
				Profiles.select(name);
			}
		} else if (overConfirm(x, y)) {
			confirm();
			return true;
		} else if (overRandom(x, y)) {
			Profiles.select(Profiles.random());
			scrollTarget = Float.MAX_VALUE;
		} else if (overClear(x, y)) {
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
			setInput(input + event.codepointAsString());
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
			setInput(input.substring(0, input.length() - 1));
			return true;
		}
		if (event.isPaste()) {
			setInput(Profiles.sanitize(input + minecraft.keyboardHandler.getClipboard()));
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
			setInput("");
			scrollTarget = Float.MAX_VALUE;
			refresh();
		}
	}

	private void setInput(String value) {
		input = value;
		inputText = Ui.text(value);
	}

	private void refresh() {
		rows.clear();
		for (String name : Profiles.names()) {
			rows.add(new Row(name, Ui.text(name), DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name))));
		}
		int count = rows.size();
		subtitle = Ui.text(Profiles.current()).withColor(TEXT_ACTIVE).append(Ui.text("  ·  " + count + " " + plural(count)).withColor(MUTED));
		listHeight = Math.min(count, VISIBLE_ROWS) * ROW;
		cardHeight = INSET * 3 + listHeight + CONTROL;
		cardX = (width - CARD_WIDTH) / 2;
		titleY = (height - HEADER - cardHeight - CLEAR_GAP - CLEAR_HEIGHT) / 2;
		cardY = titleY + HEADER;
		listY = cardY + INSET;
		fieldY = listY + listHeight + INSET;
		clearY = cardY + cardHeight + CLEAR_GAP;
		scrollTarget = Mth.clamp(scrollTarget, 0.0F, maxScroll());
		scroll = Math.min(scroll, maxScroll());
	}

	private float maxScroll() {
		return Math.max(0, rows.size() * ROW - listHeight);
	}

	private int fieldWidth() {
		return CARD_WIDTH - INSET * 4 - CONTROL - RANDOM_WIDTH;
	}

	private int hoveredRow(double x, double y) {
		if (!inside(x, y, cardX + INSET, listY, CARD_WIDTH - INSET * 2, listHeight)) {
			return -1;
		}
		int index = (int) ((y - listY + scroll) / ROW);
		return index < rows.size() ? index : -1;
	}

	private boolean overRemove(double x) {
		return x >= cardX + CARD_WIDTH - INSET - REMOVE_WIDTH;
	}

	private boolean overConfirm(double x, double y) {
		return inside(x, y, cardX + INSET * 2 + fieldWidth(), fieldY, CONTROL, CONTROL);
	}

	private boolean overRandom(double x, double y) {
		return inside(x, y, cardX + INSET * 3 + fieldWidth() + CONTROL, fieldY, RANDOM_WIDTH, CONTROL);
	}

	private boolean overClear(double x, double y) {
		return inside(x, y, (width - CLEAR_WIDTH) / 2.0F, clearY, CLEAR_WIDTH, CLEAR_HEIGHT);
	}

	private static String plural(int count) {
		int last = count % 10;
		int lastTwo = count % 100;
		if (last == 1 && lastTwo != 11) {
			return "профиль";
		}
		return last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14) ? "профиля" : "профилей";
	}

	private record Row(String name, Component label, PlayerSkin skin) {
	}
}
