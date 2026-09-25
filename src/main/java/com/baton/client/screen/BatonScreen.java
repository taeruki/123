package com.baton.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

public abstract class BatonScreen extends Screen {
	private static final int BACKGROUND = 0xFF0B0C0F;
	private static final float INTRO_SECONDS = 0.26F;
	private static final float INTRO_SCALE = 0.9F;
	private static final float OVERSHOOT = 1.70158F;

	private final long openedAt = Util.getMillis();
	private long lastFrame = openedAt;
	protected float delta;

	protected BatonScreen() {
		super(Component.empty());
	}

	protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float appear);

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, height, BACKGROUND);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		delta = Math.min((now - lastFrame) / 1000.0F, 0.1F);
		lastFrame = now;
		float progress = Math.min((now - openedAt) / 1000.0F / INTRO_SECONDS, 1.0F);
		float inverse = 1.0F - progress;
		float appear = 1.0F - inverse * inverse * inverse;
		float t = progress - 1.0F;
		float scale = INTRO_SCALE + (1.0F - INTRO_SCALE) * (1.0F + (OVERSHOOT + 1.0F) * t * t * t + OVERSHOOT * t * t);
		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2.0F, height / 2.0F);
		graphics.pose().scale(scale);
		graphics.pose().translate(-width / 2.0F, -height / 2.0F);
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
