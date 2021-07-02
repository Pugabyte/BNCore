package me.pugabyte.nexus.features.minigames.mechanics;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.annotations.AntiCamp;
import me.pugabyte.nexus.features.minigames.models.arenas.AnvilDropArena;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDamageEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// TODO:
//  - add maps

@AntiCamp
public class AnvilDrop extends TeamlessMechanic {
	List<String> deathMessages = Arrays.asList(
			"was squished",
			"became strawberry jam"
	);

	@Override
	public @NotNull String getName() {
		return "Anvil Drop";
	}

	@Override
	public @NotNull String getDescription() {
		return "Survive the onslaught of falling anvils";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.ANVIL);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		dropAnvils(event.getMatch());
	}

	@Override
	public void onDamage(@NotNull MinigamerDamageEvent event) {
		if (event.getOriginalEvent() instanceof EntityDamageEvent)
			((EntityDamageEvent) event.getOriginalEvent()).setDamage(event.getMinigamer().getPlayer().getHealth() + 1);
		super.onDamage(event);
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		if (event.getOriginalEvent() instanceof EntityDamageEvent entityDamageEvent) {
			if (entityDamageEvent.getCause().equals(EntityDamageEvent.DamageCause.FALLING_BLOCK)) {
				String minigamer = event.getMinigamer().getColoredName();
				String deathMessage = RandomUtils.randomElement(deathMessages);
				event.setDeathMessage(minigamer + " &3" + deathMessage);

				Entity eventEntity = entityDamageEvent.getEntity();
				eventEntity.getWorld().playSound(eventEntity.getLocation(), Sound.ENTITY_PLAYER_DEATH, 10F, 1F);
				eventEntity.getWorld().playSound(eventEntity.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 10F, 1F);
			}
		}
		super.onDeath(event);
	}

	public void dropAnvils(Match match) {
		AnvilDropArena arena = match.getArena();
		List<Location> dropLocs = getLocations(match.getWEUtils().getBlocks(arena.getRegion("dropzone")));
		match.getTasks().repeat(Time.SECOND.x(3), 5, () -> RandomUtils.randomElement(dropLocs).getBlock().setType(Material.ANVIL));
	}

	@EventHandler
	public void onAnvilLand(EntityChangeBlockEvent event) {
		if (!event.getTo().equals(Material.ANVIL)) return;

		Entity entity = event.getEntity();

		Set<ProtectedRegion> regions = new WorldGuardUtils(entity).getRegionsAt(entity.getLocation());
		regions.forEach(region -> {
			if (region.getId().contains("anvildrop")) {
				event.setCancelled(true);
				entity.remove();
				entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10F, 1F);
			}
		});
	}

	public List<Location> getLocations(List<Block> blocks) {
		List<Location> locations = new ArrayList<>();
		for (Block block : blocks)
			locations.add(block.getLocation());
		return locations;
	}


}
