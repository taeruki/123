package com.baton.client.profile;

import com.baton.client.mixin.MinecraftAccessor;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.core.UUIDUtil;
import org.slf4j.Logger;

public final class Profiles {
	public static final int MAX_LENGTH = 16;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Pattern VALID = Pattern.compile("\\w{3,16}");
	private static final String CONSONANTS = "bcdfghklmnprstvz";
	private static final String VOWELS = "aeiou";
	private static final List<String> NAMES = new ArrayList<>();
	private static final List<String> VIEW = Collections.unmodifiableList(NAMES);
	private static Path file = Path.of("");
	private static String current = "";

	private Profiles() {
	}

	public static void load(Minecraft minecraft) {
		file = minecraft.gameDirectory.toPath().resolve("baton").resolve("profiles.txt");
		current = minecraft.getUser().getName();
		try {
			if (Files.exists(file)) {
				Files.readAllLines(file).stream().filter(VALID.asMatchPredicate()).distinct().forEach(NAMES::add);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to read profiles from {}", file, e);
		}
		if (NAMES.isEmpty()) {
			NAMES.add(current);
		}
		select(NAMES.removeFirst());
	}

	public static List<String> names() {
		return VIEW;
	}

	public static String current() {
		return current;
	}

	public static void select(String name) {
		if (!NAMES.contains(name)) {
			NAMES.add(name);
		}
		current = name;
		Minecraft minecraft = Minecraft.getInstance();
		if (!name.equals(minecraft.getUser().getName())) {
			MinecraftAccessor accessor = (MinecraftAccessor) minecraft;
			accessor.baton$setUser(new User(name, UUIDUtil.createOfflinePlayerUUID(name), "", Optional.empty(), Optional.empty()));
			accessor.baton$setProfileFuture(CompletableFuture.completedFuture(null));
			accessor.baton$setProfileKeyPairManager(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
		}
		save();
	}

	public static void remove(String name) {
		if (!name.equals(current) && NAMES.remove(name)) {
			save();
		}
	}

	public static void clear() {
		NAMES.removeIf(name -> !name.equals(current));
		save();
	}

	public static boolean valid(String name) {
		return VALID.matcher(name).matches();
	}

	public static String sanitize(String text) {
		StringBuilder builder = new StringBuilder(MAX_LENGTH);
		text.codePoints().filter(Profiles::allowed).limit(MAX_LENGTH).forEach(builder::appendCodePoint);
		return builder.toString();
	}

	public static boolean allowed(int codePoint) {
		return codePoint == '_' || codePoint < 128 && Character.isLetterOrDigit(codePoint);
	}

	public static String random() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int length = 5 + random.nextInt(4);
		StringBuilder builder = new StringBuilder(length + 2);
		for (int i = 0; i < length; i++) {
			String letters = i % 2 == 0 ? CONSONANTS : VOWELS;
			builder.append(letters.charAt(random.nextInt(letters.length())));
		}
		builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
		if (random.nextBoolean()) {
			builder.append(random.nextInt(10, 100));
		}
		return builder.toString();
	}

	private static void save() {
		List<String> lines = new ArrayList<>(NAMES.size());
		lines.add(current);
		NAMES.stream().filter(name -> !name.equals(current)).forEach(lines::add);
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, lines);
		} catch (IOException e) {
			LOGGER.warn("Failed to save profiles to {}", file, e);
		}
	}
}
