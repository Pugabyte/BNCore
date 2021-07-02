package me.pugabyte.nexus.features.minigames.mechanics;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.SneakyThrows;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.arenas.GrabAJumbuckArena;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.GrabAJumbuckMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.utils.ColorType;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GrabAJumbuck extends TeamlessMechanic {

	@Override
	public @NotNull String getName() {
		return "Grab-A-Jumbuck";
	}

	@Override
	public @NotNull String getDescription() {
		return "Grab as many sheep as possible! Colored sheep are worth more points!";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.WHITE_WOOL);
	}

	@Override
	public boolean usesAlternativeRegen() {
		return true;
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		spawnSheep(event.getMatch(), 20);
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		Match match = event.getMatch();
		match.getMinigamers().forEach((player) -> removeAllPassengers(player.getPlayer(), match));
		GrabAJumbuckMatchData matchData = match.getMatchData();
		try {
			matchData.getSheeps().forEach(Entity::remove);
			matchData.getItems().forEach(Entity::remove);
		} catch (Exception ignore) {}
		match.getMinigamers().forEach(Minigamer::toGamelobby);
		super.onEnd(event);
	}

	@Override
	public void onQuit(@NotNull MinigamerQuitEvent event) {
		super.onQuit(event);
		removeAllPassengers(event.getMinigamer().getPlayer(), event.getMatch());
	}

	public void spawnSheep(Match match, int sheepAmount) {
		GrabAJumbuckMatchData matchData = match.getMatchData();
		while (sheepAmount != 0) {
			Sheep sheep = match.getWorld().spawn(getRandomSheepSpawnLocation(match), Sheep.class);
			sheep.setInvulnerable(true);
			DyeColor color = ColorType.values()[RandomUtils.randomInt(1, ColorType.values().length - 1)].getDyeColor();
			if (RandomUtils.randomInt(0, 100) > 80) sheep.setColor(color);
			matchData.getSheeps().add(sheep);
			sheepAmount--;
		}
	}

	public Location getRandomSheepSpawnLocation(Match match) {
		Block block = match.getWGUtils().getRandomBlock(match.getArena().getProtectedRegion("sheep"));
		if (block == null)
			return getRandomSheepSpawnLocation(match);
		block = Minigames.getWorld().getHighestBlockAt((int) block.getLocation().getX(), (int) block.getLocation().getZ()).getLocation().clone().subtract(0, 1, 0).getBlock();
		GrabAJumbuckArena arena = ArenaManager.convert(match.getArena(), GrabAJumbuckArena.class);
		if (!arena.getSheepSpawnBlocks().contains(block.getType()))
			return getRandomSheepSpawnLocation(match);
		return block.getLocation().clone().add(0, 2, 0);
	}

	@SneakyThrows
	public void addSheep(Minigamer minigamer, Sheep sheep) {
		if (getTopPassenger(minigamer) == minigamer.getPlayer()) {
			GrabAJumbuckMatchData matchData = minigamer.getMatch().getMatchData();
			Item item = spawnItem(minigamer.getPlayer().getLocation());
			minigamer.getPlayer().addPassenger(item);
			item.addPassenger(sheep);
			matchData.getItems().add(item);
		} else {
			getTopPassenger(minigamer).addPassenger(sheep);
		}
		minigamer.getPlayer().setLevel(getSheep(minigamer).size());
	}

	public Item spawnItem(Location loc) {
		Item item = Minigames.getWorld().dropItem(loc, new ItemStack(Material.STONE_BUTTON));
		item.setItemStack(new ItemStack(Material.STONE_BUTTON));
		item.setInvulnerable(true);
		item.setPickupDelay(99999999);
		return item;
	}

	public Entity getTopPassenger(Minigamer minigamer) {
		if (minigamer.getPlayer().getPassengers().size() == 0) return minigamer.getPlayer();
		if (minigamer.getPlayer().getPassengers().get(0).getPassengers().size() == 0)
			return minigamer.getPlayer().getPassengers().get(0);
		return getSheep(minigamer).get(getSheep(minigamer).size() - 1);
	}

	public List<Sheep> getSheep(Minigamer minigamer) {
		List<Sheep> sheep = new ArrayList<>();
		Entity entity = minigamer.getPlayer();
		while (entity.getPassengers().size() > 0) {
			if (entity.getPassengers().get(0).getType() == EntityType.SHEEP) {
				sheep.add((Sheep) entity.getPassengers().get(0));
			}
			entity = entity.getPassengers().get(0);
		}
		return sheep;
	}

	public void removeAllPassengers(Entity entity, Match match) {
		GrabAJumbuckMatchData matchData = match.getMatchData();
		if (entity.getPassengers().size() > 0) {
			if (entity instanceof Player) ((Player) entity).setLevel(0);
			Entity newEntity = entity.getPassengers().get(0);
			entity.removePassenger(newEntity);
			if (entity.getType() == EntityType.DROPPED_ITEM) {
				matchData.getItems().remove(entity);
				entity.remove();
			}
			removeAllPassengers(newEntity, match);
		}
	}

	@EventHandler
	public void onSheepClick(PlayerInteractEntityEvent event) {
		if (!event.getHand().equals(EquipmentSlot.HAND)) return;
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;
		GrabAJumbuckMatchData matchData = minigamer.getMatch().getMatchData();
		if (!matchData.getSheeps().contains(event.getRightClicked())) return;
		if (getSheep(minigamer).size() == 3) {
			PlayerUtils.send(minigamer.getPlayer(), Minigames.PREFIX + "You can only carry three sheep at a time!");
			return;
		}
		Sheep sheep = (Sheep) event.getRightClicked();
		if (sheep.isInsideVehicle()) return;
		addSheep(minigamer, sheep);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		Set<ProtectedRegion> regions = new WorldGuardUtils(event.getDamager()).getRegionsAt(event.getDamager().getLocation());
		for (ProtectedRegion region : regions) {
			Arena arena = ArenaManager.getFromRegion(region.getId());
			if (arena != null && arena.ownsRegion(region.getId(), "capture")) {
				event.setCancelled(true);
				return;
			}
		}

		if (!(event.getEntity() instanceof Player player)) return;
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;
		removeAllPassengers(player, minigamer.getMatch());
	}

	@EventHandler
	public void onRegionEnter(PlayerEnteredRegionEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;
		Arena arena = minigamer.getMatch().getArena();
		if (!arena.ownsRegion(event.getRegion().getId(), "capture")) return;
		if (getTopPassenger(minigamer) == minigamer.getPlayer()) return;

		final int sheepAmount = getSheep(minigamer).size();
		minigamer.getMatch().getTasks().wait(8 * 20, () -> {
			if (minigamer.getMatch().isEnded()) return;
			spawnSheep(minigamer.getMatch(), sheepAmount);
		});

		int score = 0;
		for (Sheep sheep : getSheep(minigamer)) {
			score += sheep.getColor() == DyeColor.WHITE ? 1 : 3;
			sheep.remove();
		}
		removeAllPassengers(minigamer.getPlayer(), minigamer.getMatch());
		minigamer.scored(score);
		minigamer.tell("You scored " + score + " point" + ((score == 1) ? "" : "s"));

	}

}
