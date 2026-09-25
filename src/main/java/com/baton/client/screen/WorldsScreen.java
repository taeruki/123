package com.baton.client.screen;

import com.baton.client.render.Ui;
import com.baton.client.render.UiFont;
import com.baton.client.screen.component.ScrollList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class WorldsScreen extends CardScreen {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
	private static final int WIDTH = 220;
	private static final int ROW = 26;
	private static final int VISIBLE_ROWS = 7;
	private static final int ICON = 18;
	private static final int ICON_TEXTURE = 64;
	private static final float NAME = 8.5F;
	private static final float DETAIL = 6.5F;

	private final Map<String, FaviconTexture> icons = new HashMap<>();
	private final ScrollList<LevelSummary> list = new ScrollList<>(ROW, VISIBLE_ROWS, "Миров пока нет", LevelSummary::getLevelId, this::renderRow);
	private int count;
	private boolean loading;

	public WorldsScreen(Screen parent) {
		super(parent, WIDTH);
	}

	@Override
	protected String heading() {
		return "Одиночная";
	}

	@Override
	protected String subtitle() {
		return loading ? "Загрузка…" : plural(count, "мир", "мира", "миров");
	}

	@Override
	protected int bodyHeight() {
		return list.height();
	}

	@Override
	protected void setup() {
		action("Играть", Kind.PRIMARY, () -> playable(list.selected()), () -> join(list.selected()));
		row();
		action("Создать", Kind.NORMAL, () -> true, () -> CreateWorldScreen.openFresh(minecraft, () -> minecraft.setScreen(this)));
		action("Удалить", Kind.DANGER, () -> list.selected() != null && list.selected().canDelete(), () -> delete(list.selected()));
		load();
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
		LevelSummary summary = list.hovered(mouseX, mouseY);
		if (summary == null) {
			return false;
		}
		if (doubleClick) {
			join(summary);
		} else {
			list.select(summary);
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return list.scroll(mouseX, mouseY, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation() && playable(list.selected())) {
			join(list.selected());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		icons.values().forEach(FaviconTexture::close);
		icons.clear();
	}

	private void load() {
		LevelStorageSource source = minecraft.getLevelSource();
		try {
			loading = true;
			source.loadLevelSummaries(source.findLevelCandidates()).thenAcceptAsync(this::show, minecraft);
		} catch (LevelStorageException e) {
			LOGGER.error("Couldn't load level list", e);
			show(List.of());
		}
	}

	private void show(List<LevelSummary> summaries) {
		List<LevelSummary> sorted = summaries.stream().sorted().toList();
		loading = false;
		count = sorted.size();
		list.set(sorted);
	}

	private void join(@Nullable LevelSummary summary) {
		if (playable(summary)) {
			minecraft.createWorldOpenFlows().openWorld(summary.getLevelId(), () -> minecraft.setScreen(this));
		}
	}

	private void delete(@Nullable LevelSummary summary) {
		if (summary == null) {
			return;
		}
		try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(summary.getLevelId())) {
			access.deleteLevel();
		} catch (IOException e) {
			SystemToast.onWorldDeleteFailure(minecraft, summary.getLevelId());
			LOGGER.error("Failed to delete world {}", summary.getLevelId(), e);
		}
		load();
	}

	private static boolean playable(@Nullable LevelSummary summary) {
		return summary != null && summary.primaryActionActive();
	}

	private FaviconTexture icon(LevelSummary summary) {
		return icons.computeIfAbsent(summary.getLevelId(), id -> {
			FaviconTexture texture = FaviconTexture.forWorld(minecraft.getTextureManager(), id);
			Path file = summary.getIcon();
			if (file != null && Files.isRegularFile(file)) {
				try (InputStream stream = Files.newInputStream(file)) {
					texture.upload(NativeImage.read(stream));
				} catch (IOException e) {
					LOGGER.warn("Invalid icon for world {}", id, e);
				}
			}
			return texture;
		});
	}

	private void renderRow(GuiGraphics graphics, LevelSummary summary, int x, int width, int height, boolean hovered, boolean selected, float alpha) {
		int iconY = (height - ICON) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon(summary).textureLocation(), x + 4, iconY, 0, 0, ICON, ICON, ICON_TEXTURE, ICON_TEXTURE, ICON_TEXTURE, ICON_TEXTURE, ARGB.white(alpha));
		float textX = x + ICON + 11;
		float textWidth = width - ICON - 17;
		String mode = summary.isHardcore() ? "Хардкор" : summary.getGameMode().getLongDisplayName().getString();
		String details = mode + "  ·  " + DATE.format(Instant.ofEpochMilli(summary.getLastPlayed()));
		UiFont.draw(graphics, UiFont.ellipsize(summary.getLevelName(), NAME, textWidth), textX, 9.0F, NAME, Ui.fade(selected || hovered ? Ui.TEXT_ACTIVE : Ui.TEXT, alpha));
		UiFont.draw(graphics, UiFont.ellipsize(details, DETAIL, textWidth), textX, 18.5F, DETAIL, Ui.fade(summary.isCompatible() ? Ui.MUTED : Ui.DANGER, alpha));
	}
}
