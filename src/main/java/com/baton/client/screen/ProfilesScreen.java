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
	private static final int WIDTH = 128;
	private static final int PAD = 4;
	private static final int HEADER = 30;
	private static final int ROW = 16;
	private static final int VISIBLE_ROWS = 6;
	private static final int FIELD = 18;
	private static final int ICON = 12;
	private static final int REMOVE = 14;
	private static final float NAME = 7.0F;
	private static final float CAPTION = 5.5F;
	private static final int PANEL = 0x08FFFFFF;
	private static final int EDGE = 0x14FFFFFF;
	private static final int HOVER = 0x0AFFFFFF;
	private static final int FIELD_COLOR = 0x28000000;
	private static final int ICON_HOVER = 0x14FFFFFF;
	private static final int ACCENT = 0xFF9ED0FF;
	private static final int TEXT = 0xFF8C98AB;
	private static final int TEXT_ACTIVE = 0xFFF2F6FC;
	private static final int MUTED = 0xFF5E6A7D;
	private static final int DANGER = 0xFFFF8A94;

	private final Screen parent;
	private final List<Row> rows = new ArrayList<>();
	private Row current = row(Profiles.current());
	private String input = "";
	private int x;
	private int y;
	private int cardHeight;
	private int listY;
	private int listHeight;
	private int fieldY;
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
		boolean overRandom = overRandom(mouseX, mouseY);
		boolean overAdd = overAdd(mouseX, mouseY) && Profiles.valid(input);
		boolean overClear = overClear(mouseX, mouseY);
		if (hoveredRow >= 0 || overRandom || overAdd || overClear) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		UiFont.draw(graphics, "ПРОФИЛИ", x + 2, y - 7, CAPTION, Ui.fade(MUTED, appear));
		String count = Integer.toString(rows.size() + 1);
		UiFont.draw(graphics, count, x + WIDTH - 2 - UiFont.width(count, CAPTION), y - 7, CAPTION, Ui.fade(MUTED, appear));

		Ui.rect(graphics, x, y, WIDTH, cardHeight, 9.0F, Ui.fade(PANEL, appear));
		Ui.outline(graphics, x, y, WIDTH, cardHeight, 9.0F, 0.5F, Ui.fade(EDGE, appear));

		PlayerFaceRenderer.draw(graphics, current.skin, x + PAD + 4, y + 8, 14, ARGB.white(appear));
		UiFont.draw(graphics, current.name, x + PAD + 24, y + 12, 7.5F, Ui.fade(TEXT_ACTIVE, appear));
		Ui.rect(graphics, x + PAD + 24, y + 19.5F, 3.0F, 3.0F, 1.5F, Ui.fade(ACCENT, appear));
		UiFont.draw(graphics, "в игре", x + PAD + 30, y + 21, CAPTION, Ui.fade(MUTED, appear));
		Ui.rect(graphics, x + PAD, y + HEADER, WIDTH - PAD * 2, 0.5F, 0.0F, Ui.fade(EDGE, appear));

		int rowX = x + PAD;
		int rowWidth = WIDTH - PAD * 2;
		Ui.clip(graphics, rowX, listY, rowX + rowWidth, listY + listHeight);
		for (int i = 0; i < rows.size(); i++) {
			float rowY = listY + i * ROW - scroll;
			if (rowY + ROW <= listY || rowY >= listY + listHeight) {
				continue;
			}
			Row row = rows.get(i);
			boolean hovered = i == hoveredRow;
			if (hovered) {
				Ui.rect(graphics, rowX, rowY, rowWidth, ROW, 6.0F, Ui.fade(HOVER, appear));
				boolean overRemove = overRemove(mouseX);
				UiFont.drawCentered(graphics, "×", rowX + rowWidth - REMOVE / 2.0F, rowY + ROW / 2.0F, 8.0F, Ui.fade(overRemove ? DANGER : MUTED, appear));
			}
			PlayerFaceRenderer.draw(graphics, row.skin, rowX + 4, Math.round(rowY) + 4, 8, ARGB.white(appear));
			UiFont.draw(graphics, row.name, rowX + 17, rowY + ROW / 2.0F, NAME, Ui.fade(hovered ? TEXT_ACTIVE : TEXT, appear));
		}
		Ui.unclip(graphics);
		float maxScroll = maxScroll();
		if (maxScroll > 0.0F) {
			float barHeight = listHeight * listHeight / (float) (rows.size() * ROW);
			Ui.rect(graphics, x + WIDTH - 2.5F, listY + scroll / maxScroll * (listHeight - barHeight), 1.0F, barHeight, 0.5F, Ui.fade(EDGE, appear));
		}

		Ui.rect(graphics, rowX, fieldY, rowWidth, FIELD, 6.0F, Ui.fade(FIELD_COLOR, appear));
		int textRight = addX() - ICON - 4;
		Ui.clip(graphics, rowX + 1, fieldY, textRight, fieldY + FIELD);
		float inputWidth = UiFont.width(input, NAME);
		float textX = rowX + 6 - Math.max(0.0F, inputWidth - (textRight - rowX - 10));
		float centerY = fieldY + FIELD / 2.0F;
		if (input.isEmpty()) {
			UiFont.draw(graphics, "Новый ник", textX, centerY, NAME, Ui.fade(MUTED, appear));
		} else {
			UiFont.draw(graphics, input, textX, centerY, NAME, Ui.fade(TEXT_ACTIVE, appear));
		}
		if (Util.getMillis() / 500 % 2 == 0) {
			Ui.rect(graphics, textX + inputWidth + 0.5F, centerY - 4.0F, 0.5F, 8.0F, 0.0F, Ui.fade(TEXT_ACTIVE, appear));
		}
		Ui.unclip(graphics);

		int iconY = fieldY + (FIELD - ICON) / 2;
		int randomX = addX() - ICON - 2;
		if (overRandom) {
			Ui.rect(graphics, randomX, iconY, ICON, ICON, 4.0F, Ui.fade(ICON_HOVER, appear));
		}
		int dice = Ui.fade(overRandom ? TEXT_ACTIVE : TEXT, appear);
		Ui.outline(graphics, randomX + 2.5F, iconY + 2.5F, 7.0F, 7.0F, 2.0F, 0.75F, dice);
		Ui.rect(graphics, randomX + 4.25F, iconY + 4.25F, 1.5F, 1.5F, 0.75F, dice);
		Ui.rect(graphics, randomX + 6.25F, iconY + 6.25F, 1.5F, 1.5F, 0.75F, dice);
		boolean valid = Profiles.valid(input);
		Ui.rect(graphics, addX(), iconY, ICON, ICON, 4.0F, Ui.fade(valid ? ACCENT : ICON_HOVER, appear));
		int plus = Ui.fade(valid ? 0xFF0B1220 : MUTED, appear);
		Ui.rect(graphics, addX() + 3.0F, iconY + 5.5F, 6.0F, 1.0F, 0.5F, plus);
		Ui.rect(graphics, addX() + 5.5F, iconY + 3.0F, 1.0F, 6.0F, 0.5F, plus);

		if (!rows.isEmpty()) {
			UiFont.drawCentered(graphics, "Очистить список", width / 2.0F, clearY(), CAPTION + 0.5F, Ui.fade(overClear ? DANGER : MUTED, appear));
		}
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
			if (overRemove(mouseX)) {
				Profiles.remove(name);
			} else {
				Profiles.select(name);
			}
		} else if (overRandom(mouseX, mouseY)) {
			Profiles.select(Profiles.random());
		} else if (overAdd(mouseX, mouseY)) {
			confirm();
			return true;
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
			refresh();
		}
	}

	private void refresh() {
		current = row(Profiles.current());
		rows.clear();
		for (String name : Profiles.names()) {
			if (!name.equals(current.name)) {
				rows.add(row(name));
			}
		}
		listHeight = Math.min(rows.size(), VISIBLE_ROWS) * ROW;
		cardHeight = HEADER + PAD + listHeight + (rows.isEmpty() ? 0 : PAD) + FIELD + PAD;
		x = (width - WIDTH) / 2;
		y = (height - cardHeight) / 2;
		listY = y + HEADER + PAD;
		fieldY = listY + listHeight + (rows.isEmpty() ? 0 : PAD);
		scrollTarget = Mth.clamp(scrollTarget, 0.0F, maxScroll());
		scroll = Math.min(scroll, maxScroll());
	}

	private float maxScroll() {
		return Math.max(0, rows.size() * ROW - listHeight);
	}

	private int addX() {
		return x + WIDTH - PAD - ICON - 3;
	}

	private float clearY() {
		return y + cardHeight + 10.0F;
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

	private boolean overRandom(double mouseX, double mouseY) {
		return inside(mouseX, mouseY, addX() - ICON - 2, fieldY + (FIELD - ICON) / 2.0F, ICON, ICON);
	}

	private boolean overAdd(double mouseX, double mouseY) {
		return inside(mouseX, mouseY, addX(), fieldY + (FIELD - ICON) / 2.0F, ICON, ICON);
	}

	private boolean overClear(double mouseX, double mouseY) {
		return !rows.isEmpty() && inside(mouseX, mouseY, width / 2.0F - 36, clearY() - 5, 72, 10);
	}

	private static Row row(String name) {
		return new Row(name, DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)));
	}

	private record Row(String name, PlayerSkin skin) {
	}
}
