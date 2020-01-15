package me.pugabyte.bncore.utils;

import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Data;
import lombok.NonNull;
import me.pugabyte.bncore.framework.exceptions.postconfigured.InvalidInputException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class WorldGuardUtils {
	@NonNull
	private World world;
	RegionManager manager;

	public WorldGuardUtils(@NonNull World world) {
		this.world = world;
		this.manager = WGBukkit.getRegionManager(world);
	}

	public ProtectedRegion getProtectedRegion(String name) {
		ProtectedRegion region = manager.getRegion(name);
		if (region == null)
			throw new InvalidInputException("Region not found");
		return region;
	}

	public Vector toVector(Location location) {
		return new Vector(location.getX(), location.getY(), location.getZ());
	}

	public Region getRegion(String name) {
		return convert(getProtectedRegion(name));
	}

	public Region getRegion(Location min, Location max) {
		return new CuboidRegion(toVector(min), toVector(max));
	}

	public Region getPlayerSelection(Player player) {
		return FawePlayer.wrap(player).getSelection();
	}

	public Set<ProtectedRegion> getRegionsAt(Location location) {
		return manager.getApplicableRegions(location).getRegions();
	}

	public Set<ProtectedRegion> getRegionsLike(String name) {
		Map<String, ProtectedRegion> regions = manager.getRegions();
		return regions.keySet().stream().filter(id -> id.matches(name.toLowerCase())).map(regions::get).collect(Collectors.toSet());
	}

	public ProtectedRegion getRegionLike(String name) {
		Set<ProtectedRegion> matches = getRegionsLike(name);
		if (matches.size() == 0)
			throw new InvalidInputException("No regions found");
		return matches.iterator().next();
	}

	public ProtectedRegion convert(Region region) {
		return new ProtectedCuboidRegion("temp", new BlockVector(region.getMaximumPoint()), new BlockVector(region.getMinimumPoint()));
	}

	public Region convert(ProtectedRegion region) {
		return new CuboidRegion(region.getMaximumPoint(), region.getMinimumPoint());
	}

	public Block getRandomBlock(String region) {
		return getRandomBlock(getProtectedRegion(region));
	}

	public Block getRandomBlock(ProtectedRegion region) {
		int xMin = region.getMinimumPoint().getBlockX();
		int yMin = region.getMinimumPoint().getBlockY();
		int zMin = region.getMinimumPoint().getBlockZ();

		int xDiff = region.getMaximumPoint().getBlockX() - xMin;
		int yDiff = region.getMaximumPoint().getBlockY() - yMin;
		int zDiff = region.getMaximumPoint().getBlockZ() - zMin;

		int x = xMin + Utils.randomInt(0, xDiff);
		int y = yMin + Utils.randomInt(0, yDiff);
		int z = zMin + Utils.randomInt(0, zDiff);

		return world.getBlockAt(x, y, z);
	}


}
