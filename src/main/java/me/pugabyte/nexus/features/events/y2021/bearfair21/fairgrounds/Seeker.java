package me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.BF21PointSource;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class Seeker implements Listener {
	private static final World world = BearFair21.getWorld();
	private static final Map<Location, BlockFace> locationsMap = new HashMap<>() {{
		put(new Location(world, 69, 138, -48), BlockFace.NORTH);
		put(new Location(world, 56, 146, -48), BlockFace.WEST);
		put(new Location(world, 35, 139, -48), BlockFace.EAST);
		put(new Location(world, 29, 139, -34), BlockFace.NORTH);
		put(new Location(world, 34, 145, -34), BlockFace.NORTH);
		put(new Location(world, 30, 139, -24), BlockFace.SOUTH);
		put(new Location(world, 41, 139, -12), BlockFace.EAST);
		put(new Location(world, 42, 139, -10), BlockFace.SOUTH);
		put(new Location(world, 44, 139, -5), BlockFace.EAST);
		put(new Location(world, 56, 138, -12), BlockFace.SOUTH);
		put(new Location(world, 65, 138, -9), BlockFace.SOUTH);
		put(new Location(world, 70, 139, -15), BlockFace.EAST);
		put(new Location(world, 74, 139, -20), BlockFace.NORTH);
		put(new Location(world, 77, 141, -32), BlockFace.WEST);
		put(new Location(world, 69, 140, -30), BlockFace.WEST);
		put(new Location(world, 68, 141, -21), BlockFace.NORTH);
	}};

	private static final Map<UUID, Location> playersMap = new HashMap<>();

	public Seeker() {
		Nexus.registerListener(this);
		updateTask();
	}

	public static void addPlayer(Player player) {
		playersMap.put(player.getUniqueId(), RandomUtils.randomElement(locationsMap.keySet()));
	}

	public static void removePlayer(Player player) {
		playersMap.remove(player.getUniqueId());
	}

	public static boolean isPlaying(Player player) {
		return playersMap.containsKey(player.getUniqueId());
	}

	private void updateTask() {
		Tasks.repeat(0, Time.SECOND.x(2), () -> {
			if (playersMap.size() == 0)
				return;

			for (UUID uuid : new HashSet<>(playersMap.keySet())) {
				OfflinePlayer offlinePlayer = PlayerUtils.getPlayer(uuid);
				if (!offlinePlayer.isOnline() || offlinePlayer.getPlayer() == null)
					continue;

				Player player = offlinePlayer.getPlayer();
				if (BearFair21.isNotAtBearFair(player))
					continue;

				Location location = playersMap.get(uuid);
				BlockFace blockFace = locationsMap.get(location);

				BlockData blockData = Material.CRIMSON_BUTTON.createBlockData();
				Directional directional = (Directional) blockData;
				directional.setFacing(blockFace);
				blockData = directional;

				player.sendBlockChange(location, blockData);
			}
		});
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;

		Player player = event.getPlayer();
		if (!isPlaying(player)) return;

		Block block = event.getClickedBlock();
		if (block == null)
			return;

		Location blockLocation = block.getLocation();
		if (!LocationUtils.blockLocationsEqual(blockLocation, playersMap.get(player.getUniqueId())))
			return;

		event.setCancelled(true);

		removePlayer(player);
		player.sendBlockChange(blockLocation, blockLocation.getBlock().getBlockData());

		BearFair21.giveDailyTokens(player, BF21PointSource.SEEKER, 25);
	}
}
