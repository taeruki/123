package com.baton.client.proxy;

import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;

public final class Proxies {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new Gson();
	private static Path file = Path.of("");
	private static Settings settings = Settings.DISABLED;

	private Proxies() {
	}

	public static void load(Path directory) {
		file = directory.resolve("proxy.json");
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				Settings loaded = GSON.fromJson(reader, Settings.class);
				settings = loaded != null && loaded.type != null ? loaded : Settings.DISABLED;
			} catch (IOException | JsonParseException e) {
				LOGGER.warn("Failed to read proxy settings from {}", file, e);
			}
		}
	}

	public static Settings settings() {
		return settings;
	}

	public static void set(Settings value) {
		settings = value;
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(value, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to save proxy settings to {}", file, e);
		}
	}

	public static ChannelHandler wrap(ChannelHandler initializer) {
		Settings current = settings;
		Optional<HostAndPort> target = current.enabled ? current.target() : Optional.empty();
		if (target.isEmpty()) {
			return initializer;
		}
		ProxyHandler proxy = current.handler(new InetSocketAddress(target.get().getHost(), target.get().getPort()));
		return new ChannelInitializer<>() {
			@Override
			protected void initChannel(Channel channel) {
				channel.pipeline().addLast(proxy, initializer);
			}
		};
	}

	public enum Type {
		SOCKS5("SOCKS5", 1080),
		SOCKS4("SOCKS4", 1080),
		HTTP("HTTP", 8080);

		private final String label;
		private final int defaultPort;

		Type(String label, int defaultPort) {
			this.label = label;
			this.defaultPort = defaultPort;
		}

		public String label() {
			return label;
		}
	}

	public record Settings(boolean enabled, Type type, String address, String username, String password) {
		public static final Settings DISABLED = new Settings(false, Type.SOCKS5, "", "", "");

		public Optional<HostAndPort> target() {
			try {
				HostAndPort parsed = HostAndPort.fromString(address.trim()).withDefaultPort(type.defaultPort);
				return parsed.getHost().isEmpty() ? Optional.empty() : Optional.of(parsed);
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}

		private ProxyHandler handler(InetSocketAddress proxy) {
			boolean auth = !username.isEmpty();
			return switch (type) {
				case SOCKS5 -> auth ? new Socks5ProxyHandler(proxy, username, password) : new Socks5ProxyHandler(proxy);
				case SOCKS4 -> auth ? new Socks4ProxyHandler(proxy, username) : new Socks4ProxyHandler(proxy);
				case HTTP -> auth ? new HttpProxyHandler(proxy, username, password) : new HttpProxyHandler(proxy);
			};
		}
	}
}
