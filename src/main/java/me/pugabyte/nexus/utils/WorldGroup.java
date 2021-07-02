package me.pugabyte.nexus.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public enum WorldGroup {
	SURVIVAL("world", "world_nether", "world_the_end",
			"survival", "survival_nether", "survival_the_end",
			"legacy", "legacy_nether", "legacy_the_end",
			"resource", "resource_nether", "resource_the_end",
			"staff_world", "staff_world_nether", "staff_world_the_end",
			"safepvp", "events"),
	CREATIVE("creative", "buildcontest"),
	MINIGAMES("gameworld", "blockball", "deathswap", "uhc", "bingo"),
	SKYBLOCK("bskyblock_world", "bskyblock_world_nether", "bskyblock_world_the_end"),
	ONEBLOCK("oneblock_world", "oneblock_world_nether"),
	ADVENTURE("stranded", "aeveon_project"),
	EVENTS("bearfair21"),
	STAFF("buildadmin", "jail", "pirate", "tiger"),
	UNKNOWN;

	private final List<String> worldNames;

	WorldGroup() {
		this(new String[0]);
	}

	WorldGroup(String... worldNames) {
		this.worldNames = Arrays.asList(worldNames);
	}

	@Override
	public String toString() {
		return StringUtils.camelCase(name());
	}

	public boolean contains(World world) {
		return contains(world.getName());
	}

	public boolean contains(String world) {
		return worldNames.contains(world);
	}

	public List<World> getWorlds() {
		return worldNames.stream().map(Bukkit::getWorld).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static WorldGroup of(Entity entity) {
		return of(entity.getWorld());
	}

	public static WorldGroup of(Location location) {
		return of(location.getWorld());
	}

	public static WorldGroup of(World world) {
		return of(world.getName());
	}

	public static WorldGroup of(String world) {
		for (WorldGroup group : values())
			if (group.getWorldNames() != null)
				if (group.contains(world))
					return group;

		if (world.toLowerCase().startsWith("build"))
			return CREATIVE;

		return UNKNOWN;
	}
}
