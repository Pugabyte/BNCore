package me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds;

import com.sk89q.worldedit.regions.CuboidRegion;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.BF21PointSource;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Fairgrounds.BearFair21Kit;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerLeftRegionEvent;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.getWEUtils;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.getWGUtils;

public class Archery implements Listener {
	private static final String gameRegion = BearFair21.getRegion() + "_archery";
	private static final String kitRegion = gameRegion + "_kit";
	private static final String targetRegion = gameRegion + "_targets";
	private static boolean enabled = false;
	private static int activeTargets = 0;

	public Archery() {
		Nexus.registerListener(this);
		targetTask();
	}

	private void targetTask() {
		List<Location> spawnLocs = getTargetLocs();
		Tasks.repeat(0, 10, () -> {
			if (enabled) {
				if (activeTargets < 10) {
					Location loc = RandomUtils.randomElement(spawnLocs);
					if (canPlaceTarget(loc, true)) {
						placeTarget(loc);
						++activeTargets;
					}
				}
			}
		});
	}

	@EventHandler
	public void onRegionEnter(PlayerEnteredRegionEvent event) {
		String id = event.getRegion().getId();
		if (id.equalsIgnoreCase(gameRegion)) {
			if (enabled) return;
			enabled = true;
		} else if (id.equalsIgnoreCase(kitRegion)) {
			BearFair21Kit.ARCHERY.giveItems(event.getPlayer());
		}
	}

	@EventHandler
	public void onRegionExit(PlayerLeftRegionEvent event) {
		String id = event.getRegion().getId();
		if (id.equalsIgnoreCase(gameRegion)) {
			if (!enabled) return;
			if (getWGUtils().getPlayersInRegion(gameRegion).size() == 0) {
				enabled = false;
				clearTargets();
			}
		} else if (id.equalsIgnoreCase(kitRegion)) {
			BearFair21Kit.ARCHERY.removeItems(event.getPlayer());
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		if (!(projectile instanceof Arrow)) return;
		Block hitBlock = event.getHitBlock();
		if (hitBlock == null) return;
		if (!hitBlock.getType().equals(Material.TARGET)) return;
		if (!BearFair21.isInRegion(hitBlock, targetRegion)) return;
		if (!(projectile.getShooter() instanceof Player player)) return;

		projectile.remove();
		--activeTargets;
		removeTarget(hitBlock);
		new SoundBuilder(Sound.ENTITY_ARROW_HIT_PLAYER).receiver(player).volume(0.3).pitch(0.1).play();

		BearFair21.giveDailyTokens(player, BF21PointSource.ARCHERY, 1);
	}

	private List<Location> getTargetLocs() {
		List<Block> blocks = getWEUtils().getBlocks((CuboidRegion) getWGUtils().getRegion(targetRegion));
		List<Location> locs = new ArrayList<>();
		for (Block block : blocks) {
			Location loc = block.getLocation();
			if (canPlaceTarget(loc, false))
				locs.add(loc);
		}
		return locs;
	}

	private boolean canPlaceTarget(Location loc, boolean checkRadius) {
		if (loc == null)
			return false;
		if (checkRadius) {
			List<Block> nearbyBlocks = BlockUtils.getBlocksInRadius(loc, 1);
			for (Block block : nearbyBlocks) {
				if (!block.getType().equals(Material.AIR))
					return false;
			}
			return true;
		} else
			return loc.getBlock().getType().equals(Material.AIR);
	}

	private void placeTarget(Location location) {
		Block block = location.getBlock();
		Set<Material> concretes = new HashSet<>(MaterialTag.CONCRETES.getValues());
		Material concrete = RandomUtils.randomElement(concretes);

		block.setType(Material.TARGET);
		block.getRelative(BlockFace.UP).setType(concrete);
		block.getRelative(BlockFace.DOWN).setType(concrete);
		block.getRelative(BlockFace.WEST).setType(concrete);
		block.getRelative(BlockFace.EAST).setType(concrete);
	}

	private void removeTarget(Block block) {
		Collection<Entity> entities = block.getWorld().getNearbyEntities(block.getLocation(), 2, 2, 2);
		for (Entity entity : entities) {
			if (entity.getType().equals(EntityType.ARROW)) {
				Arrow arrow = (Arrow) entity;
				if (arrow.isInBlock())
					entity.remove();
			}
		}

		block.getRelative(BlockFace.SOUTH).setType(Material.AIR);
		block.setType(Material.AIR);
		block.getRelative(BlockFace.UP).setType(Material.AIR);
		block.getRelative(BlockFace.DOWN).setType(Material.AIR);
		block.getRelative(BlockFace.WEST).setType(Material.AIR);
		block.getRelative(BlockFace.EAST).setType(Material.AIR);
	}

	private void clearTargets() {
		activeTargets = 0;
		List<Block> blocks = getWEUtils().getBlocks((CuboidRegion) getWGUtils().getRegion(targetRegion));
		for (Block block : blocks) {
			if (block.getType().equals(Material.TARGET))
				removeTarget(block);
		}
	}
}
