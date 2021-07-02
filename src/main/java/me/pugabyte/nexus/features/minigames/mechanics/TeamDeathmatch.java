package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.mechanics.common.DeathmatchMechanic;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class TeamDeathmatch extends DeathmatchMechanic {

	@Override
	public @NotNull String getName() {
		return "Team Deathmatch";
	}

	@Override
	public @NotNull String getDescription() {
		return "Kill the other team";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.LEATHER_HELMET);
	}

}
