package com.baton.client.screen;

import com.baton.client.profile.Profiles;
import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.baton.client.screen.component.ScrollList;
import com.baton.client.screen.component.TextField;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ARGB;

public final class ProfilesScreen extends CardScreen {
	private static final int WIDTH = 136;
	private static final int ROW = 20;
	private static final int VISIBLE_ROWS = 9;
	private static final int HEAD = 12;
	private static final int REMOVE = 16;
	private static final float NAME = 8.5F;

	private final TextField field = new TextField("Никнейм", Profiles.MAX_LENGTH, Profiles::allowed);
	private final ScrollList<String> list = new ScrollList<>(ROW, VISIBLE_ROWS, "", name -> name, this::renderRow);
	private int fieldY;
	private int confirmX;
	private int removeX;
	private int mouseX;

	public ProfilesScreen(Screen parent) {
		super(parent, WIDTH);
		field.focused(true);
	}

	@Override
	protected String heading() {
		return "Профили";
	}

	@Override
	protected String subtitle() {
		return Profiles.current() + "  |  " + plural(Profiles.names().size(), "профиль", "профиля", "профилей");
	}

	@Override
	protected int bodyHeight() {
		return list.height() + PAD + CONTROL;
	}

	@Override
	protected void setup() {
		action("Случайный", Kind.NORMAL, () -> true, () -> {
			Profiles.select(Profiles.random());
			refresh(true);
		});
		action("Удалить все", Kind.DANGER, () -> Profiles.names().size() > 1, () -> {
			Profiles.clear();
			refresh(false);
		});
		refresh(false);
	}

	@Override
	protected void renderBody(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float appear) {
		this.mouseX = mouseX;
		list.layout(x, y, width);
		list.render(graphics, mouseX, mouseY, delta, appear);
		fieldY = y + list.height() + PAD;
		confirmX = x + width - CONTROL;
		removeX = x + width - REMOVE;
		field.render(graphics, x, fieldY, width - CONTROL - 3, CONTROL, appear);
		boolean valid = Profiles.valid(field.value());
		if (valid && inside(mouseX, mouseY, confirmX, fieldY, CONTROL, CONTROL) || list.hovered(mouseX, mouseY) != null) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
		Ui.rect(graphics, confirmX, fieldY, CONTROL, CONTROL, 7.0F, Ui.fade(valid ? Ui.ACCENT : Ui.HOVER, appear));
		UiFont.drawCentered(graphics, "✓", confirmX + CONTROL / 2.0F, fieldY + CONTROL / 2.0F, NAME, Ui.fade(valid ? Ui.ON_ACCENT : Ui.MUTED, appear));
	}

	@Override
	protected boolean clickBody(double mouseX, double mouseY, boolean doubleClick) {
		String name = list.hovered(mouseX, mouseY);
		if (name != null) {
			click();
			if (mouseX >= removeX && !name.equals(Profiles.current())) {
				Profiles.remove(name);
			} else {
				Profiles.select(name);
			}
			refresh(false);
			return true;
		}
		if (inside(mouseX, mouseY, confirmX, fieldY, CONTROL, CONTROL)) {
			confirm();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return list.scroll(mouseX, mouseY, scrollY);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return field.charTyped(event) || super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation()) {
			confirm();
			return true;
		}
		return field.keyPressed(event) || super.keyPressed(event);
	}

	private void confirm() {
		if (Profiles.valid(field.value())) {
			click();
			Profiles.select(field.value());
			field.value("");
			refresh(true);
		}
	}

	private void refresh(boolean reveal) {
		list.set(Profiles.names());
		list.select(Profiles.current());
		if (reveal) {
			list.revealEnd();
		}
	}

	private void renderRow(GuiGraphics graphics, String name, int x, int width, int height, boolean hovered, boolean selected, float alpha) {
		PlayerFaceRenderer.draw(graphics, DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)), x + 4, (height - HEAD) / 2, HEAD, ARGB.white(alpha));
		UiFont.draw(graphics, name, x + HEAD + 10, height / 2.0F, NAME, Ui.fade(selected || hovered ? Ui.TEXT_ACTIVE : Ui.TEXT, alpha));
		float iconX = x + width - REMOVE / 2.0F;
		if (selected) {
			Ui.rect(graphics, iconX - 1.5F, height / 2.0F - 1.5F, 3.0F, 3.0F, 1.5F, Ui.fade(Ui.TEXT_ACTIVE, alpha));
		} else if (hovered) {
			UiFont.drawCentered(graphics, "×", iconX, height / 2.0F, 9.0F, Ui.fade(mouseX >= removeX ? Ui.DANGER : Ui.MUTED, alpha));
		}
	}
}
