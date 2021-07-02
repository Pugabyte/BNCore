package me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams;

import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchInitializeEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.VanillaMechanic;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.TimeUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class TeamVanillaMechanic extends TeamMechanic implements VanillaMechanic<Team> {
	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		VanillaMechanic.super.onStart(event);
	}

	@Override
	public void onInitialize(@NotNull MatchInitializeEvent event) {
		super.onInitialize(event);
		resetBorder();
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		super.onEnd(event);
		resetBorder();
	}

	@Override
	public void tellMapAndMechanic(@NotNull Minigamer minigamer) {
		minigamer.tell("You are playing &e" + minigamer.getMatch().getMechanic().getName());
		tellDescriptionAndModifier(minigamer);
	}

	@Override
	public @NotNull GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	@Override
	public boolean canOpenInventoryBlocks() {
		return true;
	}

	@Override
	public boolean canDropItem(@NotNull ItemStack item) {
		return true;
	}

	@Override
	public void spreadPlayers(@NotNull Match match) {
		match.getMinigamers().forEach(minigamer -> {
			minigamer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, TimeUtils.Time.SECOND.x(20), 10, false, false));
			minigamer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, TimeUtils.Time.SECOND.x(5), 10, false, false));
			minigamer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, TimeUtils.Time.SECOND.x(5), 255, false, false));
			minigamer.getPlayer().setVelocity(new Vector(0, 0, 0));
		});
		match.getAliveTeams().forEach(team -> Tasks.async(() -> randomTeleport(match, team)));
	}

	@Override
	public @NotNull CompletableFuture<Void> onRandomTeleport(@NotNull Match match, @NotNull Team team, @NotNull Location location) {
		CompletableFuture<?>[] results = team.getMinigamers(match).stream().map(minigamer -> minigamer.teleport(location)).toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(results);
	}
}
