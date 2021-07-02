package me.pugabyte.nexus.features.minigames.mechanics;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.arenas.GoldRushArena;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchBeginEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldEditUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GoldRush extends TeamlessMechanic {

	@Override
	public @NotNull String getName() {
		return "Gold Rush";
	}

	@Override
	public @NotNull String getDescription() {
		return "Mine all the blocks to the finish";
	}

	@Override
	public @NotNull GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.GOLD_INGOT);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		Match match = event.getMatch();
		GoldRushArena goldRushArena = match.getArena();
		createMineStacks(goldRushArena.getMineStackHeight(), match.getAliveTeams().get(0).getSpawnpoints());
		for (Location loc : match.getAliveTeams().get(0).getSpawnpoints())
			loc.clone().subtract(0, 1, 0).getBlock().setType(Material.GLASS);
	}

	@Override
	public void onBegin(@NotNull MatchBeginEvent event) {
		super.onBegin(event);
		Match match = event.getMatch();
		match.broadcast("Mine!");
		for (Location location : match.getAliveTeams().get(0).getSpawnpoints())
			location.clone().subtract(0, 1, 0).getBlock().breakNaturally();
		for (Minigamer minigamer : match.getMinigamers())
			minigamer.getPlayer().playSound(minigamer.getPlayer().getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1);
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		super.onEnd(event);
		if (event.getMatch().isStarted()) {
			GoldRushArena goldRushArena = event.getMatch().getArena();
			for (Location location : event.getMatch().getAliveTeams().get(0).getSpawnpoints())
				removeMineStacks(goldRushArena.getMineStackHeight(), location);
		}
	}

	public void createMineStacks(int mineStackHeight, List<Location> locations) {
		Map<BlockType, Double> pattern = new HashMap<>() {{
			put(BlockTypes.COBBLESTONE, 10.0);
			put(BlockTypes.GOLD_ORE, 40.0);
			put(BlockTypes.DIRT, 20.0);
			put(BlockTypes.IRON_ORE, 20.0);
			put(BlockTypes.OAK_LOG, 10.0);
		}};

		WorldEditUtils WEUtils = new WorldEditUtils(locations.get(0));

		BlockVector3 p1 = WEUtils.toBlockVector3(locations.get(0).clone().subtract(0, 2, 0));
		BlockVector3 p2 = WEUtils.toBlockVector3(locations.get(0).clone().subtract(0, mineStackHeight, 0));
		Region region = new CuboidRegion(p1, p2);
		WEUtils.replace(region, Collections.singleton(BlockTypes.AIR), pattern);

		Clipboard schematic = WEUtils.copy(locations.get(0).clone().subtract(0, 2, 0), locations.get(0).clone().subtract(0, mineStackHeight, 0));
		for (Location location : locations)
			WEUtils.paster().clipboard(schematic).at(WEUtils.toBlockVector3(location.clone().subtract(0, mineStackHeight, 0))).paste();
	}

	public void removeMineStacks(int mineStackHeight, Location loc) {
		WorldEditUtils WEUtils = new WorldEditUtils(loc);
		BlockVector3 p1 = WEUtils.toBlockVector3(loc.clone().subtract(0, 2, 0));
		BlockVector3 p2 = WEUtils.toBlockVector3(loc.clone().subtract(0, mineStackHeight, 0));
		Region region = new CuboidRegion(p1, p2);
		WEUtils.set(region, BlockTypes.AIR);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;
		event.setDropItems(false);
		if (event.getBlock().getType().equals(Material.IRON_ORE)) {
			trap(event.getBlock());
			PlayerUtils.send(event.getPlayer(), Minigames.PREFIX + "You mined some fools gold! Next time, click it with the TNT to remove it!");
		}
	}

	@EventHandler
	public void onClick(PlayerInteractEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;
		if (event.getClickedBlock() == null) return;
		if (!event.getClickedBlock().getType().equals(Material.IRON_ORE)) return;
		if (!event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.TNT)) return;
		event.getClickedBlock().setType(Material.AIR);
	}

	@EventHandler
	public void onPlaceBlock(BlockPlaceEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (minigamer.isPlaying(this)) event.setCancelled(true);
	}

	@EventHandler
	public void onRegionEnter(PlayerEnteredRegionEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;
		if (minigamer.getMatch().getArena().ownsRegion(event.getRegion().getId(), "win")) {
			minigamer.scored();
			minigamer.getMatch().end();
		}
	}

	public void trap(Block block) {
		Tasks.wait(1, () -> block.getRelative(BlockFace.UP).getLocation().clone().subtract(0, 1, 0).getBlock().setType(Material.COBWEB));
		Tasks.wait(2 * 20, () -> block.setType(Material.AIR));
	}

}
