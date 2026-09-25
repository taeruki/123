package com.baton.client.screen.component;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.mojang.blaze3d.platform.InputConstants;
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

	public TextField(String hint, int maxLength, IntPredicate allowed) {
		this.hint = hint;
		this.maxLength = maxLength;
		this.allowed = allowed;
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

	public void render(GuiGraphics graphics, int x, int y, int width, int height, float alpha) {
		Ui.rect(graphics, x, y, width, height, 7.0F, Ui.fade(Ui.FIELD, alpha));
		Ui.outline(graphics, x, y, width, height, 7.0F, 0.5F, Ui.fade(focused ? EDGE_FOCUSED : Ui.EDGE, alpha));
		graphics.enableScissor(x + 1, y, x + width - 1, y + height);
		float textWidth = UiFont.width(value, SIZE);
		float textX = x + 7 - Math.max(0.0F, textWidth - width + 16);
		float centerY = y + height / 2.0F;
		UiFont.draw(graphics, value.isEmpty() ? hint : value, textX, centerY, SIZE, Ui.fade(value.isEmpty() ? Ui.MUTED : Ui.TEXT_ACTIVE, alpha));
		if (focused && Util.getMillis() / 500 % 2 == 0) {
			Ui.rect(graphics, textX + textWidth + 0.5F, centerY - 4.5F, 0.5F, 9.0F, 0.0F, Ui.fade(Ui.TEXT_ACTIVE, alpha));
		}
		graphics.disableScissor();
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
