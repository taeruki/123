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
	private static final float SMALL = 7.5F;
	private static final int ACCENT_HOVER = 0xFFD8DCE4;

	protected final Screen parent;
	private final int cardWidth;
	private final List<List<Action>> rows = new ArrayList<>();
	private int cardX;
	private int cardY;
	private int headerY;
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
		if (rows.isEmpty()) {
			row();
		}
		rows.getLast().add(new Action(label, kind, enabled, run));
	}

	protected final void row() {
		rows.add(new ArrayList<>());
	}

	@Override
	protected final void init() {
		rows.clear();
		setup();
		int cardHeight = bodyHeight() + PAD * 2;
		int actionsHeight = rows.size() * (CONTROL + GAP);
		cardX = (width - cardWidth) / 2;
		headerY = (height - HEADER - cardHeight - actionsHeight) / 2;
		cardY = headerY + HEADER;
		int y = cardY + cardHeight + GAP;
		for (List<Action> row : rows) {
			int actionWidth = (cardWidth - GAP * (row.size() - 1)) / row.size();
			for (int i = 0; i < row.size(); i++) {
				int x = cardX + i * (actionWidth + GAP);
				row.get(i).place(x, y, i == row.size() - 1 ? cardX + cardWidth - x : actionWidth);
			}
			y += CONTROL + GAP;
		}
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float appear) {
		if (armed != null && Util.getMillis() - armedAt > CONFIRM_MILLIS) {
			armed = null;
		}
		UiFont.drawCentered(graphics, heading(), width / 2.0F, headerY + 6, Ui.HEADING_SIZE, Ui.fade(Ui.HEADING, appear));
		UiFont.drawCentered(graphics, subtitle(), width / 2.0F, headerY + 20, SMALL, Ui.fade(Ui.TEXT, appear));
		Ui.panel(graphics, cardX, cardY, cardWidth, bodyHeight() + PAD * 2, 10.0F, appear);
		renderBody(graphics, cardX + PAD, cardY + PAD, cardWidth - PAD * 2, mouseX, mouseY, appear);
		for (List<Action> row : rows) {
			for (Action action : row) {
				renderAction(graphics, action, mouseX, mouseY, appear);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		for (List<Action> row : rows) {
			for (Action action : row) {
				if (action.contains(event.x(), event.y()) && action.enabled.getAsBoolean()) {
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

	private void renderAction(GuiGraphics graphics, Action action, int mouseX, int mouseY, float appear) {
		boolean enabled = action.enabled.getAsBoolean();
		boolean hovered = enabled && action.contains(mouseX, mouseY);
		if (hovered) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
		int x = action.x;
		int y = action.y;
		int width = action.width;
		int text = switch (action.kind) {
			case NORMAL -> {
				Ui.control(graphics, x, y, width, CONTROL, hovered, enabled, appear);
				yield enabled ? Ui.TEXT_ACTIVE : Ui.MUTED;
			}
			case PRIMARY -> {
				if (enabled) {
					Ui.rect(graphics, x, y, width, CONTROL, 7.0F, Ui.fade(hovered ? ACCENT_HOVER : Ui.ACCENT, appear));
					yield Ui.ON_ACCENT;
				}
				Ui.control(graphics, x, y, width, CONTROL, false, false, appear);
				yield Ui.MUTED;
			}
			case DANGER -> {
				Ui.rect(graphics, x, y, width, CONTROL, 7.0F, Ui.fade(hovered || action == armed ? Ui.DANGER_HOVER : Ui.DANGER_FILL, enabled ? appear : appear * 0.5F));
				yield enabled ? Ui.DANGER : Ui.MUTED;
			}
		};
		UiFont.drawCentered(graphics, action == armed ? "Точно?" : action.label, x + width / 2.0F, y + CONTROL / 2.0F, SMALL, Ui.fade(text, appear));
	}

	protected enum Kind {
		NORMAL,
		PRIMARY,
		DANGER
	}

	private static final class Action {
		private final String label;
		private final Kind kind;
		private final BooleanSupplier enabled;
		private final Runnable run;
		private int x;
		private int y;
		private int width;

		private Action(String label, Kind kind, BooleanSupplier enabled, Runnable run) {
			this.label = label;
			this.kind = kind;
			this.enabled = enabled;
			this.run = run;
		}

		private void place(int x, int y, int width) {
			this.x = x;
			this.y = y;
			this.width = width;
		}

		private boolean contains(double mouseX, double mouseY) {
			return inside(mouseX, mouseY, x, y, width, CONTROL);
		}
	}
}
