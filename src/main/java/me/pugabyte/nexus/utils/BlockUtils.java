package me.pugabyte.nexus.utils;

import me.lexikiq.HasPlayer;
import me.lexikiq.OptionalPlayer;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.utils.LocationUtils.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

public class BlockUtils {

	public static Queue<Location> createDistanceSortedQueue(Location origin) {
		return new PriorityQueue<>((loc1, loc2) -> (int) (loc1.distanceSquared(origin) - loc2.distanceSquared(origin)));
	}

	public static void updateBlockProperty(Block block, String key, String newValue) {
		block.setBlockData(getBlockDataWithNewValue(block, key, newValue));
	}

	public static String getBlockProperty(Block block, String key) {
		return getBlockProperties(block).getOrDefault(key, null);
	}

	public static boolean containsBlockProperty(Block block, String key) {
		return getBlockProperty(block, key) != null;
	}

	public static HashMap<String, String> getBlockProperties(Block block) {
		return getBlockProperties(block.getState().getBlockData().getAsString());
	}

	public static HashMap<String, String> getBlockProperties(BlockData blockData) {
		return getBlockProperties(blockData.getAsString());
	}

	public static HashMap<String, String> getBlockProperties(String blockDataString) {
		HashMap<String, String> blockDataVariables = new HashMap<>();
		String[] variables = blockDataString.replace("]", "").split("\\[");
		String[] variableList = variables.length > 1 ? variables[1].split(",") : null;
		if (variableList != null)
			for (String s : variableList) blockDataVariables.put(s.split("=")[0], s.split("=")[1]);
		return blockDataVariables;
	}

	public static BlockData getBlockDataWithNewValue(Block block, String key, String newValue) {
		HashMap<String, String> variables = getBlockProperties(block);
		if (variables.containsKey(key.toLowerCase())) variables.put(key, newValue.toLowerCase());
		return getBlockDataFromList(block, variables);
	}

	public static BlockData getBlockDataFromList(Block block, HashMap<String, String> variables) {
		return getBlockDataFromList(block.getType(), variables);
	}

	public static BlockData getBlockDataFromList(Material material, HashMap<String, String> variables) {
		if (material == null) return null;
		if (variables != null && !variables.isEmpty()) {
			return Bukkit.createBlockData(generateBlockDataString(material, variables));
		} else {
			return Bukkit.createBlockData(material);
		}
	}

	public static String generateBlockDataString(Material material, HashMap<String, String> values) {
		if (values != null && !values.isEmpty()) {
			StringBuilder vsb = new StringBuilder();
			Iterator<String> i = values.keySet().iterator();
			while (i.hasNext()) {
				String v = i.next();
				vsb.append(String.format("%s=%s", v, i.hasNext() ? values.get(v) + "," : values.get(v)));
			}
			return String.format("minecraft:%s[%s]", material.toString().toLowerCase(), vsb.toString());
		} else {
			return String.format("minecraft:%s", material.toString()).toLowerCase();
		}
	}

	public static List<Block> getAdjacentBlocks(Block block) {
		Block north = block.getRelative(BlockFace.NORTH);
		Block east = block.getRelative(BlockFace.EAST);
		Block south = block.getRelative(BlockFace.SOUTH);
		Block west = block.getRelative(BlockFace.WEST);
		Block up = block.getRelative(BlockFace.UP);
		Block down = block.getRelative(BlockFace.DOWN);
		List<Block> relatives = Arrays.asList(north, east, south, west, up, down);
		List<Block> adjacent = new ArrayList<>();
		for (Block relative : relatives) {
			if (!isNullOrAir(relative))
				adjacent.add(relative);
		}
		return adjacent;
	}

	public static List<Block> getBlocksInRadius(Location start, int radius) {
		return getBlocksInRadius(start.getBlock(), radius, radius, radius);
	}

	public static List<Block> getBlocksInRadius(Location start, int xRadius, int yRadius, int zRadius) {
		return getBlocksInRadius(start.getBlock(), xRadius, yRadius, zRadius);
	}

	public static List<Block> getBlocksInRadius(Block start, int radius) {
		return getBlocksInRadius(start, radius, radius, radius);
	}

	public static List<Block> getBlocksInRadius(Block start, int xRadius, int yRadius, int zRadius) {
		List<Block> blocks = new ArrayList<>();
		for (int x = -xRadius; x <= xRadius; x++)
			for (int z = -zRadius; z <= zRadius; z++)
				for (int y = -yRadius; y <= yRadius; y++)
					blocks.add(start.getRelative(x, y, z));
		return blocks;
	}

	/*
	Doesnt work:

	fences
	gates
	walls
	sea pickles
	pot
	hanging on all vines
	in a cobweb
	on a ladder (and on top)
	end rods
	trapdoors (uses the block underneath)
	carpet (uses the block underneath)
	scaffolding
	lily pads
	composter (inside)
	skulls (on floor & wall)
	in water & lava
	snow layers (all levels, layers=1 & 2, uses the block underneath)
	 */

	public static Block getBlockStandingOn(HasPlayer hasPlayer) {
		Player player = hasPlayer.getPlayer();
		Location below = player.getLocation().add(0, -.25, 0);
		Block block = below.getBlock();
		if (block.getType().isSolid())
			return block;

		List<BlockFace> priority = new HashMap<BlockFace, Double>() {{
			put(BlockFace.NORTH, below.getZ() - Math.floor(below.getZ()));
			put(BlockFace.EAST, Math.abs(below.getX() - Math.ceil(below.getX())));
			put(BlockFace.SOUTH, Math.abs(below.getZ() - Math.ceil(below.getZ())));
			put(BlockFace.WEST, below.getX() - Math.floor(below.getX()));
		}}.entrySet().stream()
				.filter(direction -> direction.getValue() < .3)
				.sorted(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.limit(2)
				.collect(Collectors.toList());

		if (priority.size() == 2)
			priority.add(getBlockFaceBetween(priority.get(0), priority.get(1)));

		for (BlockFace blockFace : priority) {
			Block relative = block.getRelative(blockFace);
			if (relative.getType().isSolid())
				return relative;
		}

		return null;
	}

	public static BlockFace getDirection(Block from, Block to) {
		return getDirection(from.getLocation(), to.getLocation());
	}

	public static BlockFace getDirection(Location from, Location to) {
		Axis axis = Axis.getAxis(from, to);
		if (axis == null)
			throw new InvalidInputException("Locations not aligned on an axis, cannot determine direction");

		switch (axis) {
			case X:
				if ((from.getZ() - to.getZ()) > 0)
					return BlockFace.NORTH;
				else
					return BlockFace.SOUTH;
			case Y:
				if (from.getY() - to.getY() > 0)
					return BlockFace.DOWN;
				else
					return BlockFace.UP;
			case Z:
				if (from.getX() - to.getX() > 0)
					return BlockFace.WEST;
				else
					return BlockFace.EAST;
		}

		throw new InvalidInputException("Cannot determine direction");
	}

	public static BlockFace getBlockFaceBetween(BlockFace face1, BlockFace face2) {
		int x = face1.getModX() + face2.getModX();
		int y = face1.getModY() + face2.getModY();
		int z = face1.getModZ() + face2.getModZ();
		for (BlockFace face : BlockFace.values())
			if (face.getModX() == x && face.getModY() == y && face.getModZ() == z)
				return face;

		return null;
	}

	public static boolean isNullOrAir(Block block) {
		return block == null || block.getType().equals(Material.AIR);
	}

	public static void glow(Block block, int ticks, OptionalPlayer viewer) {
	}

	public static void glow(Block block, int ticks, List<? extends OptionalPlayer> viewers) {
		List<Player> _viewers = PlayerUtils.getNonNullPlayers(viewers);

		Material material = block.getType();
		if (ItemUtils.isNullOrAir(material))
			material = Material.WHITE_CONCRETE;

		Location location = block.getLocation();
		World blockWorld = block.getWorld();
		FallingBlock fallingBlock = blockWorld.spawnFallingBlock(LocationUtils.getCenteredLocation(location), material.createBlockData());
		fallingBlock.setDropItem(false);
		fallingBlock.setGravity(false);
		fallingBlock.setInvulnerable(true);
		fallingBlock.setVelocity(new Vector(0, 0, 0));
	}
}
