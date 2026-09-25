package com.baton.client.screen;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public abstract class CardScreen extends BatonScreen {
	protected static final int PAD = 5;
	protected static final int CONTROL = 20;
	private static final int HEADER = 32;
	private static final int GAP = 6;
	private static final long CONFIRM_MILLIS = 2500L;
	private static final float TITLE = 12.0F;
	private static final float SMALL = 7.5F;
	private static final int TITLE_COLOR = 0xFFECEEF3;
	private static final int ACCENT_HOVER = 0xFFD8DCE4;

	protected final Screen parent;
	private final int cardWidth;
	private final List<Action> actions = new ArrayList<>();
	private int cardX;
	private int cardY;
	private int headerY;
	private int actionsY;
	@Nullable
	private Action armed;
	private long armedAt;

	protected CardScreen(Screen parent, int cardWidth) {
		this.parent = parent;
		this.cardWidth = cardWidth;
	}

	protected abstract String heading();

	protected abstract String subtitle();

	protected abstract int bodyHeight();

	protected abstract void setup();

	protected abstract void renderBody(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float appear);

	protected abstract boolean clickBody(double mouseX, double mouseY, boolean doubleClick);

	protected final void action(String label, Kind kind, BooleanSupplier enabled, Runnable run) {
		actions.add(new Action(label, kind, enabled, run));
	}

	@Override
	protected final void init() {
		actions.clear();
		setup();
		int cardHeight = bodyHeight() + PAD * 2;
		cardX = (width - cardWidth) / 2;
		headerY = (height - HEADER - cardHeight - GAP - CONTROL) / 2;
		cardY = headerY + HEADER;
		actionsY = cardY + cardHeight + GAP;
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float appear) {
		if (armed != null && Util.getMillis() - armedAt > CONFIRM_MILLIS) {
			armed = null;
		}
		UiFont.drawCentered(graphics, heading(), width / 2.0F, headerY + 6, TITLE, Ui.fade(TITLE_COLOR, appear));
		UiFont.drawCentered(graphics, subtitle(), width / 2.0F, headerY + 20, SMALL, Ui.fade(Ui.TEXT, appear));
		Ui.panel(graphics, cardX, cardY, cardWidth, bodyHeight() + PAD * 2, 10.0F, appear);
		renderBody(graphics, cardX + PAD, cardY + PAD, cardWidth - PAD * 2, mouseX, mouseY, appear);

		int actionWidth = actionWidth();
		for (int i = 0; i < actions.size(); i++) {
			Action action = actions.get(i);
			int x = cardX + i * (actionWidth + GAP);
			boolean enabled = action.enabled.getAsBoolean();
			boolean hovered = enabled && inside(mouseX, mouseY, x, actionsY, actionWidth, CONTROL);
			if (hovered) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
			int text = switch (action.kind) {
				case NORMAL -> {
					Ui.control(graphics, x, actionsY, actionWidth, CONTROL, hovered, enabled, appear);
					yield enabled ? Ui.TEXT_ACTIVE : Ui.MUTED;
				}
				case PRIMARY -> {
					if (enabled) {
						Ui.rect(graphics, x, actionsY, actionWidth, CONTROL, 7.0F, Ui.fade(hovered ? ACCENT_HOVER : Ui.ACCENT, appear));
						yield Ui.ON_ACCENT;
					}
					Ui.control(graphics, x, actionsY, actionWidth, CONTROL, false, false, appear);
					yield Ui.MUTED;
				}
				case DANGER -> {
					Ui.rect(graphics, x, actionsY, actionWidth, CONTROL, 7.0F, Ui.fade(hovered || action == armed ? Ui.DANGER_HOVER : Ui.DANGER_FILL, enabled ? appear : appear * 0.5F));
					yield enabled ? Ui.DANGER : Ui.MUTED;
				}
			};
			String label = action == armed ? "Точно?" : action.label;
			UiFont.drawCentered(graphics, label, x + actionWidth / 2.0F, actionsY + CONTROL / 2.0F, SMALL, Ui.fade(text, appear));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		int actionWidth = actionWidth();
		for (int i = 0; i < actions.size(); i++) {
			Action action = actions.get(i);
			if (inside(event.x(), event.y(), cardX + i * (actionWidth + GAP), actionsY, actionWidth, CONTROL) && action.enabled.getAsBoolean()) {
				click();
				if (action.kind == Kind.DANGER && action != armed) {
					armed = action;
					armedAt = Util.getMillis();
				} else {
					armed = null;
					action.run.run();
				}
				return true;
			}
		}
		return clickBody(event.x(), event.y(), doubleClick) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	protected static String plural(int count, String one, String few, String many) {
		int last = count % 10;
		int lastTwo = count % 100;
		String word = last == 1 && lastTwo != 11 ? one : last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14) ? few : many;
		return count + " " + word;
	}

	private int actionWidth() {
		return actions.isEmpty() ? 0 : (cardWidth - GAP * (actions.size() - 1)) / actions.size();
	}

	protected enum Kind {
		NORMAL,
		PRIMARY,
		DANGER
	}

	private record Action(String label, Kind kind, BooleanSupplier enabled, Runnable run) {
	}
}
