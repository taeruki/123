package com.baton.client.screen.component;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

public final class HeadingWidget extends StringWidget {
	public HeadingWidget(Component message, Font font) {
		this(0, 0, Math.round(UiFont.width(message.getString(), Ui.HEADING_SIZE)) + 4, 12, message, font);
	}

	public HeadingWidget(int x, int y, int width, int height, Component message, Font font) {
		super(x, y, width, height, message, font);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		UiFont.drawCentered(graphics, getMessage().getString(), getX() + getWidth() / 2.0F, getY() + getHeight() / 2.0F, Ui.HEADING_SIZE, Ui.fade(Ui.HEADING, alpha));
	}
}
