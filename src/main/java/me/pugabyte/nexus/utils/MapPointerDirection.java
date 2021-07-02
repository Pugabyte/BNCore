package me.pugabyte.nexus.utils;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
public
enum MapPointerDirection {
	SOUTH,
	SOUTH_SOUTH_WEST,
	SOUTH_WEST,
	WEST_SOUTH_WEST,
	WEST,
	WEST_NORTH_WEST,
	NORTH_WEST,
	NORTH_NORTH_WEST,
	NORTH,
	NORTH_NORTH_EAST,
	NORTH_EAST,
	EAST_NORTH_EAST,
	EAST,
	EAST_SOUTH_EAST,
	SOUTH_EAST,
	SOUTH_SOUTH_EAST,
	;

	public byte val() {
		return (byte) ordinal();
	}

	private static final double step = 360 / (double) values().length;
	private static final double halfStep = step / 2d;

	private double center() {
		return step * ordinal();
	}

	private double lower() {
		return center() - halfStep;
	}

	private double upper() {
		return center() + halfStep;
	}

	private boolean contains(float yaw) {
		return yaw >= lower() && yaw < upper();
	}

	public static MapPointerDirection of(Player player) {
		return of(player.getLocation());
	}

	public static MapPointerDirection of(Location location) {
		float yaw = normalizeYaw(location);

		MapPointerDirection result = SOUTH;
		for (MapPointerDirection direction : values())
			if (direction.contains(yaw))
				result = direction;

		return result;
	}

	private static float normalizeYaw(Location location) {
		float yaw = Location.normalizeYaw(location.getYaw());
		if (yaw < 0) yaw += 360;
		return yaw;
	}
}
