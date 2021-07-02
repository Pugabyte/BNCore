package me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds;

import com.sk89q.worldedit.regions.CuboidRegion;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.BF21PointSource;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerLeftRegionEvent;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
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

import static me.pugabyte.nexus.features.commands.staff.WorldGuardEditCommand.canWorldGuardEdit;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.getWEUtils;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.getWGUtils;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.isInRegion;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.send;

public class Frogger implements Listener {

	private static final String gameRg = BearFair21.getRegion() + "_frogger";
	private static final String winRg = gameRg + "_win";
	private static final String damageRg = gameRg + "_damage";
	private static final String killRg = gameRg + "_kill";
	private static final String logsRg = gameRg + "_logs";
	private static final String carsRg = gameRg + "_cars";
	private static final String roadRg = gameRg + "_road";
	private static final String checkpointRg = gameRg + "_checkpoint";
	//
	private static final Location respawnLoc = new Location(BearFair21.getWorld(), 133.5, 138.0, -55.5, -180, 0);
	private static final Location checkpointLoc = new Location(BearFair21.getWorld(), 133.5, 138.0, -67.5, -180, 0);
	private static final Set<Player> checkpointList = new HashSet<>();
	private static boolean enabled = false;
	private static int animationTaskId;
	//
	private static final Map<Location, Material> logSpawnMap = new HashMap<>();
	private static final List<Integer> logTasks = new ArrayList<>();
	private static final Material logMaterial = Material.SPRUCE_WOOD;
	private static final Material riverMaterial = Material.WATER;
	//
	private static final Map<Location, Material> carSpawnMap = new HashMap<>();
	private static final List<Integer> carTasks = new ArrayList<>();
	private static final Set<Material> carMaterials = MaterialTag.CONCRETES.exclude(Material.BLACK_CONCRETE, Material.LIGHT_GRAY_CONCRETE).getValues();

	public Frogger() {
		Nexus.registerListener(this);
		loadLogSpawns();
		loadCarSpawns();
	}

	private void loadLogSpawns() {
		loadSpawns(logsRg, logSpawnMap);
	}

	private void loadCarSpawns() {
		loadSpawns(carsRg, carSpawnMap);
	}

	private void loadSpawns(String logsRg, Map<Location, Material> logSpawnMap) {
		List<Block> blocks = getWEUtils().getBlocks((CuboidRegion) getWGUtils().getRegion(logsRg));
		for (Block block : blocks)
			if (block.getType().equals(Material.DIAMOND_BLOCK) || block.getType().equals(Material.EMERALD_BLOCK))
				logSpawnMap.put(block.getLocation(), block.getType());
	}

	public void startAnimations() {
		// Log Animations
		AtomicInteger taskId = new AtomicInteger();
		taskId.set(Tasks.wait(0, () -> {
			clearLogs();
			int lastLogLen = 3;
			for (Location spawnLoc : logSpawnMap.keySet()) {
				BlockFace blockFace = (spawnLoc.getBlock().getType().equals(Material.DIAMOND_BLOCK)) ? BlockFace.WEST : BlockFace.EAST;
				for (int i = 0; i < 4; i++) {

					int ran = RandomUtils.randomInt(2, 3);
					int wait = ((lastLogLen * 10) + 30) + (((RandomUtils.randomInt(1, 3)) * 10) * i);
					lastLogLen = ran;

					Tasks.wait((long) wait * i, () -> {
						if (animationTaskId == taskId.get())
							logTask(ran, spawnLoc, blockFace);
					});
				}
			}

			// Car Animations
			clearCars();
			for (Location spawnLoc : carSpawnMap.keySet()) {
				Location loc = spawnLoc.getBlock().getRelative(0, 2, 0).getLocation();
				BlockFace blockFace = (spawnLoc.getBlock().getType().equals(Material.DIAMOND_BLOCK)) ? BlockFace.WEST : BlockFace.EAST;

				Tasks.wait(0, () -> {
					if (animationTaskId == taskId.get())
						carTask(loc, blockFace);
				});
				Tasks.wait(28, () -> {
					if (animationTaskId == taskId.get())
						carTask(loc, blockFace);
				});
			}
		}));

		animationTaskId = taskId.get();
	}

	public void stopAnimations() {
		for (Integer logTask : logTasks)
			Tasks.cancel(logTask);
		logTasks.clear();

		for (Integer carTask : carTasks)
			Tasks.cancel(carTask);
		carTasks.clear();
	}

	private void logTask(int maxLength, Location location, BlockFace blockFace) {
		final Location start = location.clone().getBlock().getRelative(blockFace).getLocation();
		AtomicReference<Location> current = new AtomicReference<>(start.clone());
		AtomicInteger distance = new AtomicInteger(0);
		AtomicInteger currentLength = new AtomicInteger(0);

		int taskId = Tasks.repeat(0, 10, () -> {
			if (!enabled) {
				stopAnimations();
				return;
			}

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
		int maxLen = 3;
		final Location startLoc = location.clone();
		AtomicReference<Location> currentLoc = new AtomicReference<>(startLoc.clone());
		AtomicReference<Material> carMaterial = new AtomicReference<>(RandomUtils.randomElement(carMaterials));
		AtomicInteger distance = new AtomicInteger(0);
		AtomicInteger currentLen = new AtomicInteger(0);

		int taskId = Tasks.repeat(0, 2, () -> {
			if (!enabled) {
				stopAnimations();
				return;
			}

			// If the next block is black stained glass
			Block next = currentLoc.get().clone().getBlock().getRelative(blockFace);
			if (next.getType().equals(Material.BLACK_STAINED_GLASS)) {
				Block behind = currentLoc.get().clone().getBlock().getRelative(blockFace.getOppositeFace(), currentLen.get());
				removeCarSlice(behind);
				currentLen.decrementAndGet();

				// if currentLen < 0, LOOP
				if (currentLen.get() < 0) {
					currentLoc.set(location.clone());
					distance.set(0);
					carMaterial.set(RandomUtils.randomElement(carMaterials));
				}
			}
			// If block at next location is not bedrock, set it to car
			else {
				currentLoc.set(startLoc.clone().getBlock().getRelative(blockFace, distance.get()).getLocation());
				buildCar(currentLoc.get().clone(), blockFace, carMaterial.get(), currentLen.get());
				distance.incrementAndGet();
				currentLen.incrementAndGet();

				// if currentLen >= maxLen, set the block maxLen blocks behind currentLoc to AIR
				if (currentLen.get() > maxLen) {
					Block behind = currentLoc.get().clone().getBlock().getRelative(blockFace.getOppositeFace(), currentLen.get());
					if (!behind.getType().equals(Material.BLACK_STAINED_GLASS))
						removeCarSlice(behind);
					currentLen.decrementAndGet();
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

	private void removeCarSlice(Block start) {
		start.setType(Material.AIR);
		start.getRelative(BlockFace.UP).setType(Material.AIR);
		start.getRelative(BlockFace.NORTH).setType(Material.AIR);
		start.getRelative(BlockFace.SOUTH).setType(Material.AIR);
		start.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).setType(Material.AIR);
		start.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).setType(Material.AIR);
	}

	private void clearLogs() {
		List<Block> blocks = getWEUtils().getBlocks((CuboidRegion) getWGUtils().getRegion(logsRg));
		for (Block block : blocks) {
			if (block.getType().equals(logMaterial))
				block.setType(riverMaterial);
		}
	}

	private void clearCars() {
		List<Block> blocks = getWEUtils().getBlocks((CuboidRegion) getWGUtils().getRegion(roadRg));
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
			if (enabled)
				return;
			enabled = true;
			startAnimations();

		} else if (regionId.equalsIgnoreCase(checkpointRg)) {
			if (player.getPing() >= 200) {
				checkpointList.add(player);
			}

		} else if (regionId.equalsIgnoreCase(damageRg)) {
			String cheatingMsg = BearFair21.isCheatingMsg(player);
			if (cheatingMsg != null && !cheatingMsg.contains("wgedit")) {
				player.teleport(respawnLoc);
				send("Don't cheat, turn " + cheatingMsg + " off!", player);
				Quests.sound_villagerNo(player);
			}

		} else if (regionId.equalsIgnoreCase(killRg)) {
			if (canWorldGuardEdit(player)) return;
			if (checkpointList.contains(player))
				player.teleport(checkpointLoc);
			else
				player.teleport(respawnLoc);
			new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BIT).receiver(player).volume(10).play();

		} else if (regionId.equalsIgnoreCase(winRg)) {
			if (canWorldGuardEdit(player)) return;

			checkpointList.remove(player);
			player.teleport(respawnLoc);
			new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BIT).receiver(player).volume(10).pitch(2.0).play();

			BearFair21.giveDailyTokens(player, BF21PointSource.FROGGER, 5);
		}
	}

	@EventHandler
	public void onRegionExit(PlayerLeftRegionEvent event) {
		String regionId = event.getRegion().getId();
		if (regionId.equalsIgnoreCase(gameRg)) {
			int size = getWGUtils().getPlayersInRegion(gameRg).size();
			if (size == 0) {
				enabled = false;
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
		new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BIT).receiver(player).volume(10).play();
	}
}
