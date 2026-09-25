package com.baton.client.screen.component;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public final class ScrollList<T> {
	private static final float APPEAR_SECONDS = 0.24F;
	private static final float APPEAR_OFFSET = 10.0F;
	private static final float SCROLL_SPEED = 12.0F;
	private static final float MOVE_SPEED = 16.0F;
	private static final int SCROLLBAR = 0x1AFFFFFF;

	private final int rowHeight;
	private final int visibleRows;
	private final String empty;
	private final Function<T, String> key;
	private final RowRenderer<T> renderer;
	private final List<Row<T>> rows = new ArrayList<>();
	private boolean filled;
	private int x;
	private int y;
	private int width;
	private float scroll;
	private float scrollTarget;
	@Nullable
	private String selected;

	public ScrollList(int rowHeight, int visibleRows, String empty, Function<T, String> key, RowRenderer<T> renderer) {
		this.rowHeight = rowHeight;
		this.visibleRows = visibleRows;
		this.empty = empty;
		this.key = key;
		this.renderer = renderer;
	}

	public void set(List<T> items) {
		Map<String, Row<T>> previous = new HashMap<>();
		rows.forEach(row -> previous.put(row.key, row));
		rows.clear();
		long born = filled ? Util.getMillis() : 0L;
		for (int i = 0; i < items.size(); i++) {
			T item = items.get(i);
			String id = key.apply(item);
			Row<T> row = previous.get(id);
			rows.add(row != null ? row.with(item) : new Row<>(id, item, i * rowHeight, born));
		}
		filled = true;
		scrollTarget = Mth.clamp(scrollTarget, 0.0F, maxScroll());
		scroll = Math.min(scroll, maxScroll());
	}

	public void layout(int x, int y, int width) {
		this.x = x;
		this.y = y;
		this.width = width;
	}

	public int height() {
		return rowHeight * visibleRows;
	}

	public void revealEnd() {
		scrollTarget = maxScroll();
	}

	public void select(@Nullable T item) {
		selected = item != null ? key.apply(item) : null;
	}

	@Nullable
	public T selected() {
		for (Row<T> row : rows) {
			if (row.key.equals(selected)) {
				return row.item;
			}
		}
		return null;
	}

	@Nullable
	public T hovered(double mouseX, double mouseY) {
		if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height()) {
			return null;
		}
		int index = (int) ((mouseY - y + scroll) / rowHeight);
		return index < rows.size() ? rows.get(index).item : null;
	}

	public boolean scroll(double mouseX, double mouseY, double amount) {
		if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height()) {
			return false;
		}
		scrollTarget = Mth.clamp(scrollTarget - (float) amount * rowHeight * 1.5F, 0.0F, maxScroll());
		return true;
	}

	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta, float alpha) {
		scroll = Ui.approach(scroll, scrollTarget, SCROLL_SPEED, delta);
		if (rows.isEmpty()) {
			UiFont.drawCentered(graphics, empty, x + width / 2.0F, y + height() / 2.0F, 7.5F, Ui.fade(Ui.MUTED, alpha));
			return;
		}
		T hovered = hovered(mouseX, mouseY);
		long now = Util.getMillis();
		graphics.enableScissor(x, y, x + width, y + height());
		for (int i = 0; i < rows.size(); i++) {
			Row<T> row = rows.get(i);
			row.y = Ui.approach(row.y, i * rowHeight, MOVE_SPEED, delta);
			float top = row.y - scroll;
			if (top + rowHeight <= 0 || top >= height()) {
				continue;
			}
			float progress = Mth.clamp((now - row.born) / 1000.0F / APPEAR_SECONDS, 0.0F, 1.0F);
			float appear = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
			float rowAlpha = alpha * appear;
			boolean isSelected = row.key.equals(selected);
			boolean isHovered = row.item == hovered;
			graphics.pose().pushMatrix();
			graphics.pose().translate((1.0F - appear) * -APPEAR_OFFSET, y + top);
			if (isSelected || isHovered) {
				Ui.rect(graphics, x, 0, width, rowHeight, 7.0F, Ui.fade(isSelected ? Ui.SELECTED : Ui.HOVER, rowAlpha));
			}
			renderer.render(graphics, row.item, x, width, rowHeight, isHovered, isSelected, rowAlpha);
			graphics.pose().popMatrix();
		}
		graphics.disableScissor();
		float maxScroll = maxScroll();
		if (maxScroll > 0.0F) {
			float thumb = height() * height() / (float) (rows.size() * rowHeight);
			Ui.rect(graphics, x + width + 1.5F, y + scroll / maxScroll * (height() - thumb), 1.0F, thumb, 0.5F, Ui.fade(SCROLLBAR, alpha));
		}
	}

	private float maxScroll() {
		return Math.max(0, rows.size() * rowHeight - height());
	}

	@FunctionalInterface
	public interface RowRenderer<T> {
		void render(GuiGraphics graphics, T item, int x, int width, int height, boolean hovered, boolean selected, float alpha);
	}

	private static final class Row<T> {
		private final String key;
		private final long born;
		private T item;
		private float y;

		private Row(String key, T item, float y, long born) {
			this.key = key;
			this.item = item;
			this.y = y;
			this.born = born;
		}

		private Row<T> with(T item) {
			this.item = item;
			return this;
		}
	}
}
