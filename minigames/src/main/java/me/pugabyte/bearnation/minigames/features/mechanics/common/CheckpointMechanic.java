package me.pugabyte.bearnation.minigames.features.mechanics.common;

import com.mewin.worldguardregionapi.events.RegionEnteredEvent;
import me.pugabyte.bearnation.api.utils.StringUtils.TimespanFormatter;
import me.pugabyte.bearnation.minigames.Minigames;
import me.pugabyte.bearnation.minigames.features.managers.PlayerManager;
import me.pugabyte.bearnation.minigames.features.models.Arena;
import me.pugabyte.bearnation.minigames.features.models.Minigamer;
import me.pugabyte.bearnation.minigames.features.models.arenas.CheckpointArena;
import me.pugabyte.bearnation.minigames.features.models.events.matches.MatchQuitEvent;
import me.pugabyte.bearnation.minigames.features.models.events.matches.MatchTimerTickEvent;
import me.pugabyte.bearnation.minigames.features.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.bearnation.minigames.features.models.matchdata.CheckpointMatchData;
import me.pugabyte.bearnation.minigames.features.models.mechanics.singleplayer.SingleplayerMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;

public abstract class CheckpointMechanic extends SingleplayerMechanic {

	public CheckpointMatchData getMatchData(Minigamer minigamer) {
		return minigamer.getMatch().getMatchData();
	}

	@Override
	public void onDeath(MinigamerDeathEvent event) {
		super.onDeath(event);

		getMatchData(event.getMinigamer()).toCheckpoint(event.getMinigamer());
	}

	@Override
	public void onQuit(MatchQuitEvent event) {
		super.onQuit(event);

		getMatchData(event.getMinigamer()).clearData(event.getMinigamer());
	}

	@EventHandler
	public void onEnterWinRegion(RegionEnteredEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		Arena arena = minigamer.getMatch().getArena();
		if (arena.ownsRegion(event.getRegion().getId(), "win")) {
			Minigames.broadcast("&e" + minigamer.getColoredName() + " &3completed &e" + arena.getDisplayName() + " &3in &e" + TimespanFormatter.of(minigamer.getScore()).format());
			minigamer.quit();
		}
	}

	@EventHandler
	public void onEnterCheckpointRegion(RegionEnteredEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		CheckpointArena arena = minigamer.getMatch().getArena();

		if (arena.ownsRegion(event.getRegion().getId(), "checkpoint")) {
			int checkpointId = Arena.getRegionTypeId(event.getRegion());
			getMatchData(minigamer).setCheckpoint(minigamer, checkpointId);
		}
	}

	@EventHandler
	public void onTick(MatchTimerTickEvent event) {
		if (!event.getMatch().isMechanic(this)) return;

		event.getMatch().getMinigamers().forEach(Minigamer::scored);
	}

	@EventHandler
	public void onCheckpointReset(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;

		if (player.getInventory().getItemInMainHand() == null) return;
		if (player.getInventory().getItemInMainHand().getType() != Material.POISONOUS_POTATO) return;
		if (!Arrays.asList(Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR).contains(event.getAction())) return;

		getMatchData(minigamer).toCheckpoint(minigamer);
	}

}
