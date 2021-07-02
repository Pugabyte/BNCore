package me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless;

import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchInitializeEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.VanillaMechanic;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.TimeUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class TeamlessVanillaMechanic extends TeamlessMechanic implements VanillaMechanic<Minigamer> {
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
		for (Minigamer minigamer : match.getMinigamers()) {
			Player player = minigamer.getPlayer();
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, TimeUtils.Time.SECOND.x(20), 10, false, false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, TimeUtils.Time.SECOND.x(5), 10, false, false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, TimeUtils.Time.SECOND.x(5), 255, false, false));
			player.setVelocity(new Vector(0, 0, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 254, false, false));
			Tasks.async(() -> randomTeleport(match, minigamer));
		}
	}

	@Override
	public @NotNull CompletableFuture<Void> onRandomTeleport(@NotNull Match match, @NotNull Minigamer minigamer, @NotNull Location location) {
		return minigamer.teleport(location).thenRun(() -> {
			Player player = minigamer.getPlayer();
			player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 400, 254, false, false));
		});
	}
}
