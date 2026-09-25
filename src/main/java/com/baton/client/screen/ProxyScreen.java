package com.baton.client.screen;

import com.baton.client.proxy.Proxies;
import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.baton.client.screen.component.TextField;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.function.IntPredicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public final class ProxyScreen extends CardScreen {
	private static final int WIDTH = 176;
	private static final float SMALL = 7.5F;
	private static final IntPredicate PRINTABLE = codePoint -> codePoint > ' ' && codePoint < 127;
	private static final Proxies.Type[] TYPES = Proxies.Type.values();

	private final TextField address = new TextField("IP:порт", 128, PRINTABLE);
	private final TextField username = new TextField("Логин", 64, PRINTABLE);
	private final TextField password = new TextField("Пароль", 64, PRINTABLE).secret();
	private final TextField[] fields = {address, username, password};
	private Proxies.Type type;
	private int bodyX;
	private int bodyY;
	private int bodyWidth;

	public ProxyScreen(Screen parent) {
		super(parent, WIDTH);
		Proxies.Settings settings = Proxies.settings();
		type = settings.type();
		address.value(settings.address());
		username.value(settings.username());
		password.value(settings.password());
		address.focused(true);
	}

	@Override
	protected String heading() {
		return "Прокси";
	}

	@Override
	protected String subtitle() {
		Proxies.Settings settings = Proxies.settings();
		return settings.enabled() ? settings.type().label() + "  ·  " + settings.address() : "Выключен";
	}

	@Override
	protected int bodyHeight() {
		return CONTROL * 3 + PAD * 2;
	}

	@Override
	protected void setup() {
		action("Выключить", Kind.NORMAL, () -> Proxies.settings().enabled(), () -> Proxies.set(settings(false)));
		action("Подключить", Kind.PRIMARY, () -> settings(true).target().isPresent(), () -> {
			Proxies.set(settings(true));
			onClose();
		});
	}

	@Override
	protected void renderBody(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float appear) {
		bodyX = x;
		bodyY = y;
		bodyWidth = width;
		int segment = segmentWidth();
		Ui.rect(graphics, x, y, width, CONTROL, 7.0F, Ui.fade(Ui.FIELD, appear));
		for (int i = 0; i < TYPES.length; i++) {
			int segmentX = x + i * segment;
			boolean selected = TYPES[i] == type;
			boolean hovered = inside(mouseX, mouseY, segmentX, y, segment, CONTROL);
			if (hovered) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
			if (selected || hovered) {
				Ui.rect(graphics, segmentX + 2, y + 2, segment - 4, CONTROL - 4, 5.0F, Ui.fade(selected ? Ui.SELECTED : Ui.HOVER, appear));
			}
			UiFont.drawCentered(graphics, TYPES[i].label(), segmentX + segment / 2.0F, y + CONTROL / 2.0F, SMALL, Ui.fade(selected ? Ui.TEXT_ACTIVE : Ui.TEXT, appear));
		}
		address.render(graphics, x, fieldY(1), width, CONTROL, mouseX, mouseY, appear);
		int half = (width - PAD) / 2;
		username.render(graphics, x, fieldY(2), half, CONTROL, mouseX, mouseY, appear);
		password.render(graphics, x + width - half, fieldY(2), half, CONTROL, mouseX, mouseY, appear);
	}

	@Override
	protected boolean clickBody(double mouseX, double mouseY, boolean doubleClick) {
		int segment = segmentWidth();
		for (int i = 0; i < TYPES.length; i++) {
			if (inside(mouseX, mouseY, bodyX + i * segment, bodyY, segment, CONTROL)) {
				click();
				type = TYPES[i];
				return true;
			}
		}
		int half = (bodyWidth - PAD) / 2;
		boolean overAddress = inside(mouseX, mouseY, bodyX, fieldY(1), bodyWidth, CONTROL);
		boolean overUsername = inside(mouseX, mouseY, bodyX, fieldY(2), half, CONTROL);
		boolean overPassword = inside(mouseX, mouseY, bodyX + bodyWidth - half, fieldY(2), half, CONTROL);
		if (!overAddress && !overUsername && !overPassword) {
			return false;
		}
		address.focused(overAddress);
		username.focused(overUsername);
		password.focused(overPassword);
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		for (TextField field : fields) {
			if (field.charTyped(event)) {
				return true;
			}
		}
		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isCycleFocus()) {
			TextField.focusNext(fields);
			return true;
		}
		if (event.isConfirmation() && settings(true).target().isPresent()) {
			Proxies.set(settings(true));
			onClose();
			return true;
		}
		for (TextField field : fields) {
			if (field.keyPressed(event)) {
				return true;
			}
		}
		return super.keyPressed(event);
	}

	private Proxies.Settings settings(boolean enabled) {
		return new Proxies.Settings(enabled, type, address.value().trim(), username.value(), password.value());
	}

	private int segmentWidth() {
		return bodyWidth / TYPES.length;
	}

	private int fieldY(int row) {
		return bodyY + row * (CONTROL + PAD);
	}
}
