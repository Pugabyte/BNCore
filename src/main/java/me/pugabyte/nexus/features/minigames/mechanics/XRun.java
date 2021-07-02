package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.mechanics.common.CheckpointMechanic;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class XRun extends CheckpointMechanic {

	@Override
	public @NotNull String getName() {
		return "X-Run";
	}

	@Override
	public @NotNull String getDescription() {
		return "Race your way to the finish line";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.SUGAR);
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		event.setDeathMessage(null);
		super.onDeath(event);
	}

}
