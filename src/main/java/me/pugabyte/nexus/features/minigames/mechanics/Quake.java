package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.models.annotations.Railgun;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.sabotage.MinigamerDisplayTimerEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.utils.JsonBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Railgun
public class Quake extends TeamlessMechanic {

	@Override
	public @NotNull String getName() {
		return "Quake";
	}

	@Override
	public @NotNull String getDescription() {
		return "Defeat your opponents with the powerful Railgun";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.IRON_HOE);
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		if (event.getAttacker() != null)
			event.getAttacker().scored();
		super.onDeath(event);
	}

	@Override
	public void onDisplayTimer(MinigamerDisplayTimerEvent event) {
		event.setContents(new JsonBuilder(event.getContents())
				.next(" | Use ")
				.next(Component.keybind("key.use"))
				.next(" to shoot"));
	}
}
