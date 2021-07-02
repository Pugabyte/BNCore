package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.mechanics.common.SpleefMechanic;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.utils.MaterialTag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class Spleef extends SpleefMechanic {

	@Override
	public @NotNull String getName() {
		return "Spleef";
	}

	@Override
	public @NotNull String getDescription() {
		return "Destroy the blocks underneath other players to make them fall to their death";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.DIAMOND_SHOVEL);
	}

	@Override
	public void onPlayerInteract(Minigamer minigamer, PlayerInteractEvent event) {
		super.onPlayerInteract(minigamer, event);
		if (event.isCancelled()) return;

		Location location = event.getClickedBlock().getLocation();

		if (breakBlock(minigamer.getMatch(), location))
			event.setCancelled(true);
	}

	@Override
	public void playBlockBreakSound(Location location) {
		Material material = location.getBlock().getType();
		Sound sound = Sound.BLOCK_STONE_BREAK;
		if (material.equals(Material.SNOW_BLOCK))
			sound = Sound.BLOCK_SNOW_BREAK;
		if (MaterialTag.WOOL.isTagged(material))
			sound = Sound.BLOCK_WOOL_BREAK;

		location.getWorld().playSound(location, sound, 1.0F, 0.75F);
	}

}
