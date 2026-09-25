package com.baton.client.screen;

import com.baton.client.screen.component.TextField;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.StringUtil;

public final class ServerFormScreen extends CardScreen {
	private static final int WIDTH = 176;

	private final TextField name = new TextField("Название", 32, StringUtil::isAllowedChatCharacter);
	private final TextField address = new TextField("Адрес сервера", 128, codePoint -> codePoint > ' ' && codePoint < 127);
	private final boolean editing;
	private final BiConsumer<String, String> onSave;
	private int fieldX;
	private int fieldY;
	private int fieldWidth;

	public ServerFormScreen(Screen parent, String serverName, String serverAddress, BiConsumer<String, String> onSave) {
		super(parent, WIDTH);
		this.editing = !serverAddress.isEmpty();
		this.onSave = onSave;
		name.value(serverName);
		address.value(serverAddress);
		(editing ? name : address).focused(true);
	}

	@Override
	protected String heading() {
		return editing ? "Изменить сервер" : "Новый сервер";
	}

	@Override
	protected String subtitle() {
		return editing ? address.value() : "Название можно не указывать";
	}

	@Override
	protected int bodyHeight() {
		return CONTROL * 2 + PAD;
	}

	@Override
	protected void setup() {
		action("Отмена", Kind.NORMAL, () -> true, this::onClose);
		action("Сохранить", Kind.PRIMARY, () -> !address.value().isBlank(), this::save);
	}

	@Override
	protected void renderBody(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float appear) {
		fieldX = x;
		fieldY = y;
		fieldWidth = width;
		name.render(graphics, x, y, width, CONTROL, appear);
		address.render(graphics, x, y + CONTROL + PAD, width, CONTROL, appear);
	}

	@Override
	protected boolean clickBody(double mouseX, double mouseY, boolean doubleClick) {
		boolean overName = inside(mouseX, mouseY, fieldX, fieldY, fieldWidth, CONTROL);
		boolean overAddress = inside(mouseX, mouseY, fieldX, fieldY + CONTROL + PAD, fieldWidth, CONTROL);
		if (overName || overAddress) {
			name.focused(overName);
			address.focused(overAddress);
		}
		return overName || overAddress;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return name.charTyped(event) || address.charTyped(event) || super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isCycleFocus()) {
			name.focused(!name.focused());
			address.focused(!name.focused());
			return true;
		}
		if (event.isConfirmation()) {
			save();
			return true;
		}
		return name.keyPressed(event) || address.keyPressed(event) || super.keyPressed(event);
	}

	private void save() {
		String ip = address.value().trim();
		if (!ip.isEmpty()) {
			onSave.accept(name.value().isBlank() ? ip : name.value().trim(), ip);
			onClose();
		}
	}
}
