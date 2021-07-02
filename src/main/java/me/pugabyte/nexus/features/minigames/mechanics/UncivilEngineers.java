package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.Regenerating;
import me.pugabyte.nexus.features.minigames.models.arenas.UncivilEngineersArena;
import me.pugabyte.nexus.features.minigames.models.arenas.UncivilEngineersArena.MobPoint;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.CheckpointData;
import me.pugabyte.nexus.features.minigames.models.matchdata.UncivilEngineersMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

@Regenerating(value = "regen")
public class UncivilEngineers extends TeamlessMechanic {
	private static final int SLICES = 10;
	private static final int SEPARATOR_Z = -25;

	public static Location getStart() {
		return new Location(Minigames.getWorld(), 2554, 56, -2677, 90, 0);
	}

	@Override
	public @NotNull String getName() {
		return "Uncivil Engineers";
	}

	@Override
	public @NotNull String getDescription() {
		return "Race to the finish";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.WOODEN_PICKAXE);
	}

	@Override
	public @NotNull GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	@Override
	public boolean shouldClearInventory() {
		return false;
	}

	@Override
	public boolean canDropItem(@NotNull ItemStack item) {
		return true;
	}

	@Override
	public boolean canOpenInventoryBlocks() {
		return true;
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		separatePlayers(event.getMatch());
		spawnEntities(event.getMatch());
	}

	public void separatePlayers(Match match) {
		final UncivilEngineersMatchData matchData = match.getMatchData();
		final Iterator<Minigamer> minigamers = match.getMinigamers().iterator();
		for (int i = 1; i <= SLICES; i++)
			if (minigamers.hasNext())
				matchData.assignSlice(minigamers.next(), i);
	}

	public void spawnEntities(Match match) {
		UncivilEngineersArena arena = match.getArena();
		for (MobPoint point : arena.getMobPoints())
			for (int i = 1; i <= SLICES; i++) {
				Location location = offset(point.getLocation(), i);
//				PaperLib.getChunkAtAsync(spawnLoc).thenRun(() ->
//					match.spawn(spawnLoc, point.getType().getEntityClass()));
				match.spawn(location, point.getType().getEntityClass());
			}
	}

	public static Location offset(Location location, Minigamer minigamer) {
		return offset(location, getSlice(minigamer));
	}

	public static Location offset(Location location, int slice) {
		return location.clone().add(0, 0, SEPARATOR_Z * (slice - 1));
	}

	public static int getSlice(Minigamer minigamer) {
		return minigamer.getMatch().<UncivilEngineersMatchData>getMatchData().getSlice(minigamer);
	}

	@EventHandler
	public void onFinish(PlayerEnteredRegionEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		if (!minigamer.getMatch().getArena().ownsRegion(event.getRegion().getId(), "finish")) return;
		minigamer.scored();
		minigamer.getMatch().end();
		// TODO wait for 1 min ?
	}

	@EventHandler
	public void onEnterCheckpointRegion(PlayerEnteredRegionEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		UncivilEngineersArena arena = minigamer.getMatch().getArena();
		CheckpointData matchData = minigamer.getMatch().getMatchData();

		if (arena.ownsRegion(event.getRegion().getId(), "checkpoint")) {
			int checkpointId = Arena.getRegionNumber(event.getRegion());
			matchData.setCheckpoint(minigamer, checkpointId);
		}
	}

	public void toCheckpoint(Minigamer minigamer) {
		UncivilEngineersArena arena = minigamer.getMatch().getArena();
		CheckpointData matchData = minigamer.getMatch().getMatchData();
		minigamer.clearState();

		Location location;
		if (!matchData.getCheckpoints().containsKey(minigamer.getPlayer().getUniqueId()))
			location = getStart().toCenterLocation();
		else
			location = arena.getCheckpoint(matchData.getCheckpointId(minigamer));

		minigamer.teleport(offset(location, minigamer));
	}

	@Override
	public void onDeath(@NotNull Minigamer victim) {
		toCheckpoint(victim);
	}

	@EventHandler
	public void onCustomDeath(PlayerDeathEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getEntity());
		if (!minigamer.isPlaying(this)) return;

		event.setKeepInventory(true);
		event.getDrops().clear();
		event.setDeathMessage(null);
	}

}
