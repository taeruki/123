package com.baton.client.screen;

import com.baton.client.render.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

public abstract class BatonScreen extends Screen {
	private static final float INTRO_SECONDS = 0.45F;
	private static final float INTRO_OFFSET = 8.0F;

	private final long openedAt = Util.getMillis();
	private long lastFrame = openedAt;
	protected float delta;

	protected BatonScreen() {
		super(Component.empty());
	}

	protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float appear);

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Ui.snow(graphics, width, height);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		delta = Math.min((now - lastFrame) / 1000.0F, 0.1F);
		lastFrame = now;
		float progress = 1.0F - Math.min((now - openedAt) / 1000.0F / INTRO_SECONDS, 1.0F);
		float appear = 1.0F - progress * progress * progress;
		graphics.pose().pushMatrix();
		graphics.pose().translate(0.0F, (1.0F - appear) * INTRO_OFFSET);
		renderContent(graphics, mouseX, mouseY, appear);
		graphics.pose().popMatrix();
	}

	protected static void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	protected static boolean inside(double x, double y, float left, float top, float width, float height) {
		return x >= left && y >= top && x < left + width && y < top + height;
	}
}
