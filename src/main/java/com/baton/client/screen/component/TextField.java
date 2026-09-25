package com.baton.client.screen.component;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;

public final class TextField {
	private static final float SIZE = 8.5F;
	private static final int EDGE_FOCUSED = 0x26FFFFFF;

	private final String hint;
	private final int maxLength;
	private final IntPredicate allowed;
	private String value = "";
	private boolean focused;
	private boolean secret;

	public TextField(String hint, int maxLength, IntPredicate allowed) {
		this.hint = hint;
		this.maxLength = maxLength;
		this.allowed = allowed;
	}

	public TextField secret() {
		secret = true;
		return this;
	}

	public String value() {
		return value;
	}

	public void value(String text) {
		StringBuilder builder = new StringBuilder(maxLength);
		text.codePoints().filter(allowed).limit(maxLength).forEach(builder::appendCodePoint);
		value = builder.toString();
	}

	public boolean focused() {
		return focused;
	}

	public void focused(boolean focused) {
		this.focused = focused;
	}

	public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, float alpha) {
		if (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
			graphics.requestCursor(CursorTypes.IBEAM);
		}
		Ui.rect(graphics, x, y, width, height, 7.0F, Ui.fade(Ui.FIELD, alpha));
		Ui.outline(graphics, x, y, width, height, 7.0F, 0.5F, Ui.fade(focused ? EDGE_FOCUSED : Ui.EDGE, alpha));
		graphics.enableScissor(x + 1, y, x + width - 1, y + height);
		String shown = secret ? "•".repeat(value.length()) : value;
		float textWidth = UiFont.width(shown, SIZE);
		float textX = x + 7 - Math.max(0.0F, textWidth - width + 16);
		float centerY = y + height / 2.0F;
		UiFont.draw(graphics, value.isEmpty() ? hint : shown, textX, centerY, SIZE, Ui.fade(value.isEmpty() ? Ui.MUTED : Ui.TEXT_ACTIVE, alpha));
		if (focused && Util.getMillis() / 500 % 2 == 0) {
			Ui.rect(graphics, textX + textWidth + 0.5F, centerY - 4.5F, 0.5F, 9.0F, 0.0F, Ui.fade(Ui.TEXT_ACTIVE, alpha));
		}
		graphics.disableScissor();
	}

	public static void focusNext(TextField... fields) {
		int current = -1;
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].focused) {
				current = i;
			}
			fields[i].focused = false;
		}
		fields[(current + 1) % fields.length].focused = true;
	}

	public boolean charTyped(CharacterEvent event) {
		if (!focused || value.length() >= maxLength || !allowed.test(event.codepoint())) {
			return false;
		}
		value += event.codepointAsString();
		return true;
	}

	public boolean keyPressed(KeyEvent event) {
		if (!focused) {
			return false;
		}
		if (event.key() == InputConstants.KEY_BACKSPACE && !value.isEmpty()) {
			value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
			return true;
		}
		if (event.isPaste()) {
			value(value + Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}
		return false;
	}
}
