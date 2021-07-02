package me.pugabyte.nexus.features.minigames.mechanics.common;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchTimerTickEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.CaptureTheFlagMatchData;
import me.pugabyte.nexus.features.minigames.models.matchdata.OneFlagCaptureTheFlagMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams.TeamMechanic;
import me.pugabyte.nexus.features.minigames.models.perks.Perk;
import me.pugabyte.nexus.features.minigames.models.perks.common.PlayerParticlePerk;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.TitleUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public abstract class CaptureTheFlagMechanic extends TeamMechanic {

	@Override
	public boolean usesPerk(@NotNull Class<? extends Perk> perk, @NotNull Minigamer minigamer) {
		return !(PlayerParticlePerk.class.isAssignableFrom(perk)) || (
				minigamer.getMatch().getMatchData() instanceof CaptureTheFlagMatchData ?
						((CaptureTheFlagMatchData) minigamer.getMatch().getMatchData()).getFlagByCarrier(minigamer) != null
						: minigamer.equals(((OneFlagCaptureTheFlagMatchData) minigamer.getMatch().getMatchData()).getFlagCarrier())
				);
	}

	@Override
	public boolean usesAlternativeRegen() {
		return true;
	}

	protected abstract void onFlagInteract(Minigamer minigamer, Sign sign);

	protected abstract void doFlagParticles(Match match);

	protected abstract void onEnterKillRegion(Minigamer minigamer);

	protected boolean canPickupFlag(Minigamer minigamer, Sign sign) {
		if (minigamer.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) {
			if (sign.getLine(1).equalsIgnoreCase(ChatColor.GREEN + "Flag")) {
				minigamer.sendMessage(ChatColor.RED + "You must be unarmed to interact with flags!");
			}
			return false;
		}
		return true;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());

		if (!(
				minigamer.isPlaying(this) &&
						event.getAction() == Action.RIGHT_CLICK_BLOCK &&
						event.getClickedBlock() != null &&
						event.getHand() != null &&
						event.getHand().equals(EquipmentSlot.HAND) &&
						MaterialTag.SIGNS.isTagged(event.getClickedBlock().getType()) &&
						canPickupFlag(minigamer, (Sign) event.getClickedBlock().getState())
		)) return;

		Sign sign = (Sign) event.getClickedBlock().getState();

		if ((ChatColor.DARK_BLUE + "[Minigame]").equalsIgnoreCase(sign.getLine(0)))
			if ((ChatColor.GREEN + "Flag").equalsIgnoreCase(sign.getLine(1))) {
				onFlagInteract(minigamer, sign);
			}
	}

	@EventHandler
	public void onRegionEvent(PlayerEnteredRegionEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;
		if (!minigamer.getMatch().getArena().ownsRegion(event.getRegion(), "kill")) return;

		onEnterKillRegion(minigamer);
	}

	@EventHandler
	public void onMatchTimerTick(MatchTimerTickEvent event) {
		if (!event.getMatch().isMechanic(this)) return;
		if (event.getTime() % 2 != 0) return;

		doFlagParticles(event.getMatch());
	}

	protected void flagMessage(Match match, Team team, String message, boolean chat) {
		flagMessage(match, team, message, Sound.ENTITY_WITHER_SPAWN, 0.6f, chat);
	}

	protected void flagMessage(Match match, Team team, String message, Sound sound, float volume, boolean chat) {
		flagMessage(team.getMinigamers(match), message, sound, volume, chat);
	}

	protected void flagMessage(Match match, Team team, Minigamer except, String message, boolean chat) {
		flagMessage(match, team, except, message, Sound.ENTITY_WITHER_SPAWN, 0.6f, chat);
	}

	protected void flagMessage(Match match, Team team, Minigamer except, String message, Sound sound, float volume, boolean chat) {
		flagMessage(team.getMinigamers(match).stream().filter(minigamer -> !minigamer.equals(except)).collect(Collectors.toList()), message, sound, volume, chat);
	}

	protected void flagMessage(List<Minigamer> minigamers, String message, boolean chat) {
		flagMessage(minigamers, message, Sound.ENTITY_WITHER_SPAWN, 0.6f, chat);
	}

	protected void flagMessage(List<Minigamer> minigamers, Minigamer except, String message, boolean chat) {
		flagMessage(minigamers.stream().filter(minigamer -> !minigamer.equals(except)).collect(Collectors.toList()), message, chat);
	}

	protected void flagMessage(List<Minigamer> minigamers, String message, Sound sound, float volume, boolean chat) {
		if (chat && !minigamers.isEmpty())
			minigamers.get(0).getMatch().broadcast(message);
		minigamers.forEach(enemy -> {
			new SoundBuilder(sound).receiver(enemy.getPlayer()).category(SoundCategory.PLAYERS).volume(volume).pitch(1.2).play();
			new SoundBuilder(Sound.ENTITY_ENDER_DRAGON_FLAP).receiver(enemy.getPlayer()).category(SoundCategory.PLAYERS).volume(0.9).play();
			TitleUtils.sendSubtitle(enemy.getPlayer(), message, 7, Time.SECOND.x(3), 7);
		});
	}

}
