package me.pugabyte.nexus.features.events.y2020.bearfair20.fairgrounds;

import com.sk89q.worldedit.regions.CuboidRegion;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerLeftRegionEvent;
import me.pugabyte.nexus.models.bearfair20.BearFair20User;
import me.pugabyte.nexus.models.bearfair20.BearFair20User.BF20PointSource;
import me.pugabyte.nexus.models.bearfair20.BearFair20UserService;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldEditUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.getWGUtils;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.giveDailyPoints;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.isInRegion;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.send;

public class Frogger implements Listener {

	private static String gameRg = BearFair20.getRegion() + "_frogger";
	private static String winRg = gameRg + "_win";
	private static String damageRg = gameRg + "_damage";
	private static String killRg = gameRg + "_kill";
	private static String logsRg = gameRg + "_logs";
	private static String carsRg = gameRg + "_cars";
	private static String roadRg = gameRg + "_road";
	private static String checkpointRg = gameRg + "_checkpoint";
	//
	private static Location respawnLoc = new Location(BearFair20.getWorld(), -856.5, 138.0, -1617.5, -180, 0);
	private static Location checkpointLoc = new Location(BearFair20.getWorld(), -856.5, 138.0, -1630.5, -180, 0);
	private static Set<Player> checkpointList = new HashSet<>();
	private static boolean doAnimation = false;
	private static WorldEditUtils WEUtils = new WorldEditUtils(BearFair20.getWorld());
	private BF20PointSource SOURCE = BF20PointSource.FROGGER;
	//
	private static Map<Location, Material> logSpawnMap = new HashMap<>();
	private static List<Integer> logTasks = new ArrayList<>();
	private static Material logMaterial = Material.SPRUCE_WOOD;
	private static Material riverMaterial = Material.WATER;
	//
	private static Map<Location, Material> carSpawnMap = new HashMap<>();
	private static List<Integer> carTasks = new ArrayList<>();
	private static Set<Material> carMaterials = MaterialTag.CONCRETES.exclude(Material.BLACK_CONCRETE, Material.LIGHT_GRAY_CONCRETE).getValues();

	public Frogger() {
		Nexus.registerListener(this);
		loadLogSpawns();
		loadCarSpawns();
	}

	private void loadLogSpawns() {
		List<Block> blocks = WEUtils.getBlocks((CuboidRegion) getWGUtils().getRegion(logsRg));
		for (Block block : blocks) {
			if (block.getType().equals(Material.DIAMOND_BLOCK) || block.getType().equals(Material.EMERALD_BLOCK)) {
				logSpawnMap.put(block.getLocation(), block.getType());
			}
		}
	}

	private void loadCarSpawns() {
		List<Block> blocks = WEUtils.getBlocks((CuboidRegion) getWGUtils().getRegion(carsRg));
		for (Block block : blocks) {
			if (block.getType().equals(Material.DIAMOND_BLOCK) || block.getType().equals(Material.EMERALD_BLOCK)) {
				carSpawnMap.put(block.getLocation(), block.getType());
			}
		}
	}

	public void startAnimations() {
		// Log Animations
		clearLogs();
		Set<Location> spawnLogLocs = logSpawnMap.keySet();
		int lastLogLen = 3;
		for (Location spawnLoc : spawnLogLocs) {
			BlockFace blockFace = (spawnLoc.getBlock().getType().equals(Material.DIAMOND_BLOCK)) ? BlockFace.WEST : BlockFace.EAST;
			for (int i = 0; i < 4; i++) {

				int ran = RandomUtils.randomInt(2, 3);
				// 10 = task update interval
				int wait = ((lastLogLen * 10) + 30) + (((RandomUtils.randomInt(1, 3)) * 10) * i);
//				int wait = (((20 * lastLogLen) + (Utils.randomInt(2, 4) * 10) + 10) * i);
				lastLogLen = ran;

				Tasks.wait(wait * i, () -> logTask(ran, spawnLoc, blockFace));
			}
		}

		// Car Animations
		clearCars();
		Set<Location> spawnCarLocs = carSpawnMap.keySet();
		for (Location spawnLoc : spawnCarLocs) {
			Location loc = spawnLoc.getBlock().getRelative(0, 2, 0).getLocation();
			BlockFace blockFace = (spawnLoc.getBlock().getType().equals(Material.DIAMOND_BLOCK)) ? BlockFace.WEST : BlockFace.EAST;

			Tasks.wait(0, () -> carTask(loc, blockFace));
			Tasks.wait(28, () -> carTask(loc, blockFace));
		}
	}

	public void stopAnimations() {
		for (Integer logTask : logTasks) {
			Tasks.cancel(logTask);
		}
		for (Integer carTask : carTasks) {
			Tasks.cancel(carTask);
		}
	}

	private void logTask(int maxLength, Location location, BlockFace blockFace) {
		final Location start = location.clone().getBlock().getRelative(blockFace).getLocation();
		AtomicReference<Location> current = new AtomicReference<>(start.clone());
		AtomicInteger distance = new AtomicInteger(0);
		AtomicInteger currentLength = new AtomicInteger(0);

		int taskId = Tasks.repeat(0, 10, () -> {
			if (!doAnimation)
				stopAnimations();

			// If the next block is bedrock
			Block next = current.get().clone().getBlock().getRelative(blockFace);
			if (next.getType().equals(Material.BEDROCK)) {
				Block behind = current.get().clone().getBlock().getRelative(blockFace.getOppositeFace(), currentLength.get());
				behind.setType(riverMaterial);
				currentLength.decrementAndGet();

				// if currentLength < 0, LOOP
				if (currentLength.get() < 0) {
					current.set(location.clone());
					distance.set(0);
				}
			}
			// If block at next location is not bedrock, set it to log
			else {
				current.set(start.clone().getBlock().getRelative(blockFace, distance.get()).getLocation());
				current.get().getBlock().setType(logMaterial);
				distance.incrementAndGet();
				currentLength.incrementAndGet();

				// if currentLen >= maxLen, set the block maxLen blocks behind currentLoc to AIR
				if (currentLength.get() > maxLength) {
					Block block = current.get().clone().getBlock().getRelative(blockFace.getOppositeFace(), currentLength.get());
					if (!block.getType().equals(Material.DIAMOND_BLOCK) && !block.getType().equals(Material.EMERALD_BLOCK))
						block.setType(riverMaterial);
					currentLength.decrementAndGet();
				}
			}
		});

		logTasks.add(taskId);
	}

	private void carTask(Location location, BlockFace blockFace) {
		int maxLength = 3;
		final Location start = location.clone().getBlock().getRelative(blockFace).getLocation();
		AtomicReference<Material> carMaterial = new AtomicReference<>(RandomUtils.randomElement(carMaterials));
		AtomicReference<Location> current = new AtomicReference<>(start.clone());
		AtomicInteger distance = new AtomicInteger(0);
		AtomicInteger currentLength = new AtomicInteger(0);
		int taskId = Tasks.repeat(0, 2, () -> {
			if (!doAnimation)
				stopAnimations();

			// If the next block is black stained glass
			Block next = current.get().clone().getBlock().getRelative(blockFace);
			if (next.getType().equals(Material.BLACK_STAINED_GLASS)) {
				Block behind = current.get().clone().getBlock().getRelative(blockFace.getOppositeFace(), currentLength.get());
				removeCarSlice(behind.getLocation());
				currentLength.decrementAndGet();

				// if currentLength < 0, LOOP
				if (currentLength.get() < 0) {
					current.set(location.clone());
					distance.set(0);
					carMaterial.set(RandomUtils.randomElement(carMaterials));
				}
			}
			// If block at next location is not bedrock, set it to log
			else {
				current.set(start.clone().getBlock().getRelative(blockFace, distance.get()).getLocation());
				buildCar(current.get().clone(), blockFace, carMaterial.get(), currentLength.get());
				distance.incrementAndGet();
				currentLength.incrementAndGet();

				// if currentLen >= maxLen, set the block maxLen blocks behind currentLoc to AIR
				if (currentLength.get() > maxLength) {
					Block block = current.get().clone().getBlock().getRelative(blockFace.getOppositeFace(), currentLength.get());
					if (!block.getType().equals(Material.BLACK_STAINED_GLASS))
						removeCarSlice(block.getLocation());
					currentLength.decrementAndGet();
				}
			}
		});

		carTasks.add(taskId);

	}

	private void buildCar(Location loc, BlockFace blockFace, Material material, int currentLength) {
		blockFace = blockFace.getOppositeFace();
		Block front = loc.getBlock();
		if (currentLength >= 0 && !front.getType().equals(Material.BLACK_STAINED_GLASS)) {
			// Front
			front.setType(material);
			front.getRelative(BlockFace.NORTH).setType(Material.BLACK_CONCRETE);
			front.getRelative(BlockFace.SOUTH).setType(Material.BLACK_CONCRETE);
			front.getRelative(BlockFace.UP).setType(Material.WHITE_STAINED_GLASS_PANE);
			front.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).setType(Material.WHITE_STAINED_GLASS_PANE);
			front.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).setType(Material.WHITE_STAINED_GLASS_PANE);
		}

		// Body 1
		Block bodyOne = front.getRelative(blockFace);
		if (currentLength >= 1 && !bodyOne.getType().equals(Material.BLACK_STAINED_GLASS)) {
			bodyOne.setType(material);
			bodyOne.getRelative(BlockFace.NORTH).setType(material);
			bodyOne.getRelative(BlockFace.SOUTH).setType(material);
			bodyOne.getRelative(BlockFace.UP).setType(material);
			bodyOne.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).setType(material);
			bodyOne.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).setType(material);
		}

		// Body 2
		Block bodyTwo = bodyOne.getRelative(blockFace);
		if (currentLength >= 2 && !bodyTwo.getType().equals(Material.BLACK_STAINED_GLASS)) {
			bodyTwo.setType(material);
			bodyTwo.getRelative(BlockFace.NORTH).setType(material);
			bodyTwo.getRelative(BlockFace.SOUTH).setType(material);
			bodyTwo.getRelative(BlockFace.UP).setType(material);
			bodyTwo.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).setType(material);
			bodyTwo.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).setType(material);
		}

		// End
		Block end = bodyTwo.getRelative(blockFace);
		if (currentLength >= 3 && !end.getType().equals(Material.BLACK_STAINED_GLASS)) {
			end.setType(material);
			end.getRelative(BlockFace.NORTH).setType(Material.BLACK_CONCRETE);
			end.getRelative(BlockFace.SOUTH).setType(Material.BLACK_CONCRETE);
			end.getRelative(BlockFace.UP).setType(material);
			end.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).setType(Material.AIR);
			end.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).setType(Material.AIR);
		}
	}

	private void removeCarSlice(Location loc) {
		Block start = loc.getBlock();
		start.setType(Material.AIR);
		start.getRelative(BlockFace.UP).setType(Material.AIR);
		start.getRelative(BlockFace.NORTH).setType(Material.AIR);
		start.getRelative(BlockFace.SOUTH).setType(Material.AIR);
		start.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).setType(Material.AIR);
		start.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).setType(Material.AIR);
	}

	private void clearLogs() {
		List<Block> blocks = WEUtils.getBlocks((CuboidRegion) getWGUtils().getRegion(logsRg));
		for (Block block : blocks) {
			if (block.getType().equals(logMaterial))
				block.setType(riverMaterial);
		}
	}

	private void clearCars() {
		List<Block> blocks = WEUtils.getBlocks((CuboidRegion) getWGUtils().getRegion(roadRg));
		for (Block block : blocks) {
			if (!block.getType().equals(Material.AIR))
				block.setType(Material.AIR);
		}
	}

	@EventHandler
	public void onRegionEnter(PlayerEnteredRegionEvent event) {
		String regionId = event.getRegion().getId();
		Player player = event.getPlayer();
		if (regionId.equalsIgnoreCase(gameRg)) {
			if (doAnimation)
				return;
			doAnimation = true;
			startAnimations();

		} else if (regionId.equalsIgnoreCase(checkpointRg)) {
			if (player.getPing() >= 200) {
				checkpointList.add(player);
			}

		} else if (regionId.equalsIgnoreCase(damageRg)) {
			String cheatingMsg = BearFair20.isCheatingMsg(player);
			if (cheatingMsg != null && !cheatingMsg.contains("wgedit")) {
				player.teleport(respawnLoc);
				send("Don't cheat, turn " + cheatingMsg + " off!", player);
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 10F, 1F);
			}

		} else if (regionId.equalsIgnoreCase(killRg)) {
			if (player.hasPermission("worldguard.region.bypass.*")) return;
			if (checkpointList.contains(player))
				player.teleport(checkpointLoc);
			else
				player.teleport(respawnLoc);
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10F, 1F);

		} else if (regionId.equalsIgnoreCase(winRg)) {
			if (player.hasPermission("worldguard.region.bypass.*")) return;
			player.teleport(respawnLoc);
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10F, 2F);

			if (giveDailyPoints) {
				BearFair20User user = new BearFair20UserService().get(player);
				user.giveDailyPoints(SOURCE);
				new BearFair20UserService().save(user);
			}

			checkpointList.remove(player);
		}
	}

	@EventHandler
	public void onRegionExit(PlayerLeftRegionEvent event) {
		String regionId = event.getRegion().getId();
		if (regionId.equalsIgnoreCase(gameRg)) {
			int size = getWGUtils().getPlayersInRegion(gameRg).size();
			if (size == 0) {
				doAnimation = false;
				stopAnimations();
			}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;

		if (!isInRegion(player, damageRg)) return;
		if (event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) return;

		event.setDamage(0);
		event.setCancelled(true);
		if (checkpointList.contains(player))
			player.teleport(checkpointLoc);
		else
			player.teleport(respawnLoc);
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10F, 1F);
	}

}
