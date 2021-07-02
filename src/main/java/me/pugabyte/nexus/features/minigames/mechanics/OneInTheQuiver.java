package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class OneInTheQuiver extends TeamlessMechanic {

	@Override
	public @NotNull String getName() {
		return "One in the Quiver";
	}

	@Override
	public @NotNull String getDescription() {
		return "Kill enemy players with your one-hit-kill bow";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.ARROW);
	}

	@Override
	public boolean usesAlternativeRegen() {
		return true;
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		if (event.getAttacker() != null) {
			event.getAttacker().getPlayer().getInventory().addItem(new ItemStack(Material.ARROW, 1));
			event.getAttacker().scored();
		}
		super.onDeath(event);
	}

	@EventHandler
	public void onArrowHit(ProjectileHitEvent event) {
		if (event.getHitEntity() == null) return;
		if (!(event.getEntity() instanceof Arrow)) return;
		if (!(event.getHitEntity() instanceof Player)) return;
		if (!(event.getEntity().getShooter() instanceof Player)) return;
		Minigamer victim = PlayerManager.get(event.getHitEntity());
		Minigamer attacker = PlayerManager.get((Player) event.getEntity().getShooter());
		if (victim.isPlaying(this) && attacker.isPlaying(this)) {
			victim.getPlayer().damage(20, attacker.getPlayer());
			event.getEntity().remove();
		}
	}

}
