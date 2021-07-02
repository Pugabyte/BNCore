package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class FreeForAll extends TeamlessMechanic {

	@Override
	public @NotNull String getName() {
		return "Free For All";
	}

	@Override
	public @NotNull String getDescription() {
		return "Kill all your enemy opponents";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.IRON_SWORD);
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		if (event.getAttacker() != null)
			event.getAttacker().scored();
		super.onDeath(event);
	}

	@Override
	public boolean usesAlternativeRegen() {
		return true;
	}
}
