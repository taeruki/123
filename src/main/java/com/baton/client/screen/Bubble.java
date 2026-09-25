package com.baton.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Util;

public final class Bubble {
	private static final float SECONDS = 0.26F;
	private static final float START_SCALE = 0.9F;
	private static final float OVERSHOOT = 1.70158F;

	private Bubble() {
	}

	public static float appear(long openedAt) {
		float inverse = 1.0F - progress(openedAt);
		return 1.0F - inverse * inverse * inverse;
	}

	public static void apply(GuiGraphics graphics, int width, int height, long openedAt) {
		float t = progress(openedAt) - 1.0F;
		float scale = START_SCALE + (1.0F - START_SCALE) * (1.0F + (OVERSHOOT + 1.0F) * t * t * t + OVERSHOOT * t * t);
		graphics.pose().translate(width / 2.0F, height / 2.0F);
		graphics.pose().scale(scale);
		graphics.pose().translate(-width / 2.0F, -height / 2.0F);
	}

	private static float progress(long openedAt) {
		return Math.min((Util.getMillis() - openedAt) / 1000.0F / SECONDS, 1.0F);
	}
}
