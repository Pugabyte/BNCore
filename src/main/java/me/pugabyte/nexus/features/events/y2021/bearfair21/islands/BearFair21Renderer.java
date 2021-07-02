package me.pugabyte.nexus.features.events.y2021.bearfair21.islands;

import lombok.Getter;
import lombok.Setter;
import me.pugabyte.nexus.utils.MapPointerDirection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursor.Type;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.pugabyte.nexus.utils.MapPointerDirection.SOUTH;
import static me.pugabyte.nexus.utils.Utils.isWithinBounds;

public class BearFair21Renderer extends MapRenderer {
	@Getter
	private static final Map<UUID, BearFair21Renderer> renderers = new HashMap<>();

	public static BearFair21Renderer getRenderer(UUID uuid) {
		return renderers.computeIfAbsent(uuid, $ -> new BearFair21Renderer(true));
	}

	public static void shutdown() {
		BearFair21Renderer.getRenderers().values().forEach(BearFair21Renderer::deactivate);
	}

	private static BufferedImage image;

	static {
		try {
			image = ImageIO.read(new URL("https://i.imgur.com/QvTesq2.png"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void init() {}

	@Getter private final MapCursor cursor = new MapCursor((byte) 0, (byte) 0, SOUTH.val(), Type.WHITE_POINTER, true, "You");
	@Getter @Setter private boolean initialized = false;
	@Getter @Setter private boolean active = true;
	@Getter @Setter private boolean updating = true;

	public BearFair21Renderer(boolean contextual) {
		super(contextual);
	}

	@Override
	public void render(@NotNull MapView view, @NotNull MapCanvas canvas, @NotNull Player player) {
		final MapCursorCollection cursors = canvas.getCursors();
		cursor.setVisible(active);

		if (!active)
			return;

		if (!initialized) {
			view.setScale(Scale.CLOSEST);
			view.setUnlimitedTracking(false);
			canvas.drawImage(0, 0, image);
			cursors.addCursor(cursor);
			initialized = true;
		}

		if (updating) {
			final Location location = player.getLocation();
			final Location center = getIslandCenter(location);
			int xOffset = getXOffset(location, center);
			int yOffset = getYOffset(location, center);

			boolean withinBounds = isWithinBounds(xOffset, Byte.class) && isWithinBounds(yOffset, Byte.class);

			if (withinBounds) {
				cursor.setVisible(true);
				cursor.setX((byte) xOffset);
				cursor.setY((byte) yOffset);
				cursor.setDirection(MapPointerDirection.of(player).val());
			} else {
				cursor.setVisible(false);
			}
		}
	}

	// TODO: Fix offsets
	//  Distance from location to island 0,0 to map 0,0 to world 0,0

	private static final int mapXOffset = 0;
	private static final int mapYOffset = 140;

	private int getXOffset(Location location, Location center) {
		double xDiff = location.getX() - center.getX();
		return (int) Math.round(xDiff * .6) + (int) center.getX() + mapXOffset;
	}

	private int getYOffset(Location location, Location center) {
		double yDiff = location.getZ() - center.getZ();
		return (int) Math.round(yDiff * .6) + (int) center.getZ() + mapYOffset;
	}

	private Location getIslandCenter(Location location) {
//		IslandType island = IslandType.of(location);
//		if (island == null)
//			island = IslandType.MAIN;
//		if (island.getCenter().getX() == 0 && island.getCenter().getZ() == 0)
//			island = IslandType.MAIN;

		return IslandType.MAIN.getCenter();
	}

	public void deactivate() {
		active = false;
		cursor.setVisible(false);
	}

}
