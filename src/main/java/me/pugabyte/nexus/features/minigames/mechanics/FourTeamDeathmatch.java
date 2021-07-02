package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.mechanics.common.DeathmatchMechanic;
import me.pugabyte.nexus.features.minigames.models.annotations.TeamGlowing;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@TeamGlowing
public final class FourTeamDeathmatch extends DeathmatchMechanic {

	@Override
	public @NotNull String getName() {
		return "Four Team Deathmatch";
	}

	@Override
	public @NotNull String getDescription() {
		return "Kill the other teams";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.DIAMOND_SWORD);
	}

}
