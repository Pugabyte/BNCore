package me.pugabyte.nexus.features.minigames.mechanics;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.MonsterMazeMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.features.minigames.utils.PowerUpUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MonsterMaze extends TeamlessMechanic {
	// Arena
	private Material floorMaterial = Material.STONE;
	private Material goalMaterial = Material.GOLD_BLOCK;
	private int MONSTERS = 10;
	private int POWERUPS = 3;

	// MatchData

	@Override
	public @NotNull String getName() {
		return "Monster Maze";
	}

	@Override
	public @NotNull String getDescription() {
		return "Get to the beacon without touching the monsters";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.SPAWNER);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);

		Match match = event.getMatch();
		MonsterMazeMatchData matchData = match.getMatchData();
		ProtectedRegion floor = match.getArena().getProtectedRegion("floor");

		List<Location> goals = new ArrayList<>();
		for (BlockVector3 vector : match.getArena().getRegion("floor")) {
			Location location = match.getWGUtils().toLocation(vector);
			if (location.getBlock().getType() == goalMaterial)
				goals.add(location.add(0, 1, 0));
		}

		List<Block> spawnpoints = match.getWGUtils().getRandomBlocks(floor, floorMaterial, MONSTERS);
		spawnpoints.stream().map(block -> block.getLocation().add(.5, 1, .5)).forEach(spawnpoint -> {
			Mob monster = match.spawn(spawnpoint, Zombie.class);
			monster.setAI(false);
			monster.setSilent(true);
			monster.setCollidable(false);
			monster.setInvulnerable(true);
			matchData.getMonsters().add(monster);
		});

		match.getMinigamers().forEach(this::preventJump);

		match.getTasks().wait(Time.SECOND.x(5), () -> {
			for (Mob monster : matchData.getMonsters()) {
				monster.setAI(true);
				updatePath(monster, goals);
			}

			match.getTasks().repeat(Time.SECOND.x(7), 30, () -> {
				for (Mob monster : matchData.getMonsters())
					if (!monster.getPathfinder().hasPath())
						updatePath(monster, goals);
			});

			match.getTasks().repeat(0, 2, () -> {
				for (Minigamer minigamer : match.getMinigamers())
					for (Mob monster : matchData.getMonsters()) {
						double distance = monster.getLocation().distance(minigamer.getPlayer().getLocation());
						if (distance < .7) {
							minigamer.getPlayer().damage(4);
							launch(minigamer, monster);
						}
					}
			});

			List<Block> powerupLocations = match.getWGUtils().getRandomBlocks(floor, floorMaterial, POWERUPS);
			match.broadcast("Power ups have spawned!");
			for (Block block : powerupLocations)
				new PowerUpUtils(match, Arrays.asList(JUMPS)).spawn(block.getLocation().add(0, 1, 0), true);
		});
	}

	private void launch(Minigamer minigamer, Mob monster) {
		Location playerCenterLocation = minigamer.getPlayer().getEyeLocation();
		Location playerToThrowLocation = monster.getEyeLocation();

		double x = playerCenterLocation.getX() - playerToThrowLocation.getX();
		double y = playerCenterLocation.getY() - playerToThrowLocation.getY();
		double z = playerCenterLocation.getZ() - playerToThrowLocation.getZ();

		Vector throwVector = new Vector(x, y, z).normalize().multiply(1.1D).setY(1.3D);

		minigamer.getPlayer().setVelocity(throwVector);
	}

	private void updatePath(Mob monster, List<Location> goals) {
		monster.getPathfinder().moveTo(getNewGoal(monster.getLocation(), goals));
	}

	public Location getNewGoal(Location start, List<Location> goals) {
		HashMap<Location, Double> distances = new HashMap<>();
		goals.forEach(goal -> distances.put(goal, start.distance(goal)));

		List<Location> sorted = distances.entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());

		return sorted.get(RandomUtils.randomInt(sorted.size() / 2, sorted.size() - 1));
	}

	private void allowJump(Minigamer minigamer) {
		minigamer.getPlayer().removePotionEffect(PotionEffectType.JUMP);
	}

	private void preventJump(Minigamer minigamer) {
		minigamer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 9999999, 250, false, false));
	}

	@EventHandler
	public void onPlayerJump(PlayerJumpEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		PlayerInventory inventory = event.getPlayer().getInventory();
		ItemStack item = inventory.getItem(8);
		if (item == null) {
			Nexus.warn("Player was allowed to jump without powerup");
			preventJump(minigamer);
			return;
		}

		if (item.getType() != Material.FEATHER) {
			Nexus.warn("Player was allowed to jump without powerup (Material is " + item.getType() + ")");
			preventJump(minigamer);
			return;
		}

		item.setAmount(item.getAmount() - 1);
		if (item.getAmount() == 0) {
			minigamer.tell("You have used all your jumps!");
			preventJump(minigamer);
			return;
		}

		inventory.setItem(8, item);
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		super.onEnd(event);
		MonsterMazeMatchData matchData = event.getMatch().getMatchData();
		matchData.getMonsters().forEach(Entity::remove);
	}

	PowerUpUtils.PowerUp JUMPS = new PowerUpUtils.PowerUp("3 Jumps", true, Material.FEATHER, minigamer -> {
		minigamer.getPlayer().getInventory().setItem(8, new ItemStack(Material.FEATHER, 3));
		allowJump(minigamer);
	});

	// healing
	// slowness snowballs
	// shoo zombies

}
