package com.baton.client.screen;

import com.baton.client.proxy.Proxies;
import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.baton.client.screen.component.ScrollList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class ServersScreen extends CardScreen {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int WIDTH = 220;
	private static final int ROW = 26;
	private static final int VISIBLE_ROWS = 7;
	private static final int ICON = 18;
	private static final int ICON_TEXTURE = 64;
	private static final int STATUS_WIDTH = 44;
	private static final int PROXY_WIDTH = 58;
	private static final int PROXY_HEIGHT = 18;
	private static final int PROXY_MARGIN = 10;
	private static final float NAME = 8.5F;
	private static final float DETAIL = 6.5F;
	private static final int PING_GOOD = 0xFF7BD88F;
	private static final int PING_FAIR = 0xFFE8C46A;
	private static final int PING_BAD = 0xFFFF8A94;

	private final ServerList servers;
	private final ServerStatusPinger pinger = new ServerStatusPinger();
	private final Map<ServerData, Icon> icons = new IdentityHashMap<>();
	private final ScrollList<ServerData> list = new ScrollList<>(ROW, VISIBLE_ROWS, "Серверов пока нет", data -> data.ip + '\n' + data.name, this::renderRow);

	public ServersScreen(Screen parent) {
		super(parent, WIDTH);
		servers = new ServerList(minecraft);
		servers.load();
	}

	@Override
	protected String heading() {
		return "Серверы";
	}

	@Override
	protected String subtitle() {
		String count = plural(servers.size(), "сервер", "сервера", "серверов");
		return Proxies.settings().enabled() ? count + "  ·  через прокси" : count;
	}

	@Override
	protected int bodyHeight() {
		return list.height();
	}

	@Override
	protected void setup() {
		action("Играть", Kind.PRIMARY, () -> list.selected() != null, () -> join(list.selected()));
		row();
		action("Добавить", Kind.NORMAL, () -> true, () -> minecraft.setScreen(new ServerFormScreen(this, "", "", (name, ip) -> {
			servers.add(new ServerData(name, ip, ServerData.Type.OTHER), false);
			servers.save();
		})));
		action("Изменить", Kind.NORMAL, () -> list.selected() != null, () -> {
			ServerData data = list.selected();
			minecraft.setScreen(new ServerFormScreen(this, data.name, data.ip, (name, ip) -> {
				data.name = name;
				data.ip = ip;
				data.setState(ServerData.State.INITIAL);
				servers.save();
			}));
		});
		action("Удалить", Kind.DANGER, () -> list.selected() != null, () -> {
			servers.remove(list.selected());
			servers.save();
			refresh();
		});
		refresh();
	}

	@Override
	protected void renderBody(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float appear) {
		list.layout(x, y, width);
		list.render(graphics, mouseX, mouseY, delta, appear);
		if (list.hovered(mouseX, mouseY) != null) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	@Override
	protected boolean clickBody(double mouseX, double mouseY, boolean doubleClick) {
		ServerData data = list.hovered(mouseX, mouseY);
		if (data == null) {
			return false;
		}
		if (doubleClick) {
			join(data);
		} else {
			list.select(data);
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return list.scroll(mouseX, mouseY, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation() && list.selected() != null) {
			join(list.selected());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void tick() {
		pinger.tick();
	}

	@Override
	public void removed() {
		pinger.removeAll();
		icons.values().forEach(icon -> icon.texture.close());
		icons.clear();
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float appear) {
		super.renderContent(graphics, mouseX, mouseY, appear);
		int x = width - PROXY_MARGIN - PROXY_WIDTH;
		boolean hovered = overProxy(mouseX, mouseY);
		if (hovered) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
		Ui.control(graphics, x, PROXY_MARGIN, PROXY_WIDTH, PROXY_HEIGHT, hovered, true, appear);
		boolean enabled = Proxies.settings().enabled();
		Ui.rect(graphics, x + 9.0F, PROXY_MARGIN + PROXY_HEIGHT / 2.0F - 2.0F, 4.0F, 4.0F, 2.0F, Ui.fade(enabled ? PING_GOOD : Ui.MUTED, appear));
		UiFont.drawCentered(graphics, "Proxy", x + PROXY_WIDTH / 2.0F + 5.0F, PROXY_MARGIN + PROXY_HEIGHT / 2.0F, 7.5F, Ui.fade(hovered || enabled ? Ui.TEXT_ACTIVE : Ui.TEXT, appear));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && overProxy(event.x(), event.y())) {
			click();
			minecraft.setScreen(new ProxyScreen(this));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void refresh() {
		List<ServerData> entries = new ArrayList<>(servers.size());
		for (int i = 0; i < servers.size(); i++) {
			ServerData data = servers.get(i);
			data.setState(ServerData.State.INITIAL);
			entries.add(data);
		}
		list.set(entries);
	}

	private void join(@Nullable ServerData data) {
		if (data != null) {
			ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(data.ip), data, false, null);
		}
	}

	private boolean overProxy(double mouseX, double mouseY) {
		return inside(mouseX, mouseY, width - PROXY_MARGIN - PROXY_WIDTH, PROXY_MARGIN, PROXY_WIDTH, PROXY_HEIGHT);
	}

	private void ping(ServerData data) {
		data.setState(ServerData.State.PINGING);
		ServerSelectionList.THREAD_POOL.submit(() -> {
			try {
				pinger.pingServer(data, () -> minecraft.execute(servers::save), () -> data.setState(
					data.protocol == SharedConstants.getCurrentVersion().protocolVersion() ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE
				), EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
			} catch (UnknownHostException e) {
				data.setState(ServerData.State.UNREACHABLE);
			} catch (Exception e) {
				LOGGER.debug("Failed to ping {}", data.ip, e);
				data.setState(ServerData.State.UNREACHABLE);
			}
		});
	}

	private void renderRow(GuiGraphics graphics, ServerData data, int x, int width, int height, boolean hovered, boolean selected, float alpha) {
		if (data.state() == ServerData.State.INITIAL) {
			ping(data);
		}
		Icon icon = icons.computeIfAbsent(data, key -> new Icon(FaviconTexture.forServer(minecraft.getTextureManager(), key.ip)));
		icon.update(data);
		int iconY = (height - ICON) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon.texture.textureLocation(), x + 4, iconY, 0, 0, ICON, ICON, ICON_TEXTURE, ICON_TEXTURE, ICON_TEXTURE, ICON_TEXTURE, ARGB.white(alpha));

		float textX = x + ICON + 11;
		float textWidth = width - ICON - 15 - STATUS_WIDTH;
		String motd = data.motd == null ? "" : ChatFormatting.stripFormatting(data.motd.getString()).lines().findFirst().orElse("");
		String detail = UiFont.printable(motd);
		UiFont.draw(graphics, UiFont.ellipsize(data.name, NAME, textWidth), textX, 9.0F, NAME, Ui.fade(selected || hovered ? Ui.TEXT_ACTIVE : Ui.TEXT, alpha));
		UiFont.draw(graphics, UiFont.ellipsize(detail.isEmpty() ? data.ip : detail, DETAIL, textWidth), textX, 18.5F, DETAIL, Ui.fade(Ui.MUTED, alpha));

		float right = x + width - 6;
		switch (data.state()) {
			case SUCCESSFUL -> {
				String players = data.players == null ? "" : data.players.online() + "/" + data.players.max();
				String ping = data.ping + " мс";
				int color = data.ping < 150 ? PING_GOOD : data.ping < 300 ? PING_FAIR : PING_BAD;
				UiFont.draw(graphics, players, right - UiFont.width(players, DETAIL + 0.5F), 9.0F, DETAIL + 0.5F, Ui.fade(Ui.TEXT, alpha));
				float pingX = right - UiFont.width(ping, DETAIL);
				UiFont.draw(graphics, ping, pingX, 18.5F, DETAIL, Ui.fade(Ui.MUTED, alpha));
				Ui.rect(graphics, pingX - 5.0F, 17.0F, 3.0F, 3.0F, 1.5F, Ui.fade(color, alpha));
			}
			case INCOMPATIBLE -> drawStatus(graphics, data.version.getString(), right, alpha);
			case UNREACHABLE -> drawStatus(graphics, "нет связи", right, alpha);
			default -> UiFont.draw(graphics, "···", right - UiFont.width("···", NAME), 9.0F, NAME, Ui.fade(Ui.MUTED, alpha));
		}
	}

	private static void drawStatus(GuiGraphics graphics, String text, float right, float alpha) {
		String fitted = UiFont.ellipsize(text, DETAIL, STATUS_WIDTH);
		UiFont.draw(graphics, fitted, right - UiFont.width(fitted, DETAIL), 9.0F, DETAIL, Ui.fade(Ui.DANGER, alpha));
	}

	private static final class Icon {
		private final FaviconTexture texture;
		private byte @Nullable [] bytes;

		private Icon(FaviconTexture texture) {
			this.texture = texture;
		}

		private void update(ServerData data) {
			byte[] current = data.getIconBytes();
			if (Arrays.equals(current, bytes)) {
				return;
			}
			bytes = current;
			if (current == null) {
				texture.clear();
				return;
			}
			try {
				texture.upload(NativeImage.read(current));
			} catch (IOException e) {
				LOGGER.warn("Invalid icon for server {}", data.ip, e);
				texture.clear();
			}
		}
	}
}
