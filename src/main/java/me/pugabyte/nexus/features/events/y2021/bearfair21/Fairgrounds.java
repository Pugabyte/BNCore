package me.pugabyte.nexus.features.events.y2021.bearfair21;

import eden.utils.StringUtils;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Archery;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Frogger;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Interactables;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Rides;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Seeker;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf.MiniGolf;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.reflection.ReflectionGame;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerLeftRegionEvent;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.TimeUtils.Timer;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

import static me.pugabyte.nexus.features.events.y2020.bearfair20.quests.BFQuests.itemLore;

public class Fairgrounds implements Listener {

	public Fairgrounds() {
		Nexus.registerListener(this);
		new Timer("      Interactables", Interactables::new);
		new Timer("      Rides", Rides::new);
		new Timer("      Minigolf", MiniGolf::new);
		new Timer("      Archery", Archery::new);
		new Timer("      Frogger", Frogger::new);
		new Timer("      Seeker", Seeker::new);
		new Timer("      Reflection", ReflectionGame::new);
	}

	public enum BearFair21Kit {
		ARCHERY(
				new ItemBuilder(Material.BOW)
						.enchant(Enchantment.ARROW_INFINITE)
						.unbreakable()
						.build(),
				new ItemBuilder(Material.ARROW)
						.build()
		),
		MINECART(
				new ItemBuilder(Material.MINECART)
						.lore(itemLore)
						.build()
		),
		;

		List<ItemStack> items;

		BearFair21Kit(ItemStack... items) {
			this.items = Arrays.asList(items);
		}

		public ItemStack getItem() {
			return getItems().get(0);
		}

		public List<ItemStack> getItems() {
			return items;
		}

		public void giveItems(Player player) {
			if (!PlayerUtils.hasRoomFor(player, this.getItems())) {
				BearFair21.send("&cCouldn't give " + StringUtils.camelCase(this) + " kit", player);
				return;
			}

			PlayerUtils.giveItems(player, this.getItems());
		}

		public void removeItems(Player player) {
			PlayerUtils.removeItems(player, this.getItems());
		}
	}

	@EventHandler
	public void onRegionEnter(PlayerEnteredRegionEvent event) {
		String id = event.getRegion().getId();
		if (id.equalsIgnoreCase("bearfair21_main_coaster_kit")) {
			if (PlayerUtils.playerHas(event.getPlayer(), BearFair21Kit.MINECART.getItem()))
				return;

			PlayerUtils.giveItem(event.getPlayer(), BearFair21Kit.MINECART.getItem());
		}
	}

	@EventHandler
	public void onRegionExit(PlayerLeftRegionEvent event) {
		String id = event.getRegion().getId();
		if (id.equalsIgnoreCase("bearfair21_main_coaster_kit")) {
			PlayerUtils.removeItem(event.getPlayer(), BearFair21Kit.MINECART.getItem());
		}
	}

	@EventHandler
	public void onPlaceMinecart(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;

		ItemStack item = event.getItem();
		if (ItemUtils.isNullOrAir(item) || !item.getType().equals(Material.MINECART))
			return;

		Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block) || !MaterialTag.RAILS.isTagged(block.getType()))
			return;

		Location blockLoc = block.getLocation();
		if (new WorldGuardUtils(blockLoc).getRegionsLikeAt("bearfair21_main_coaster", blockLoc).size() == 0)
			return;

		event.setCancelled(true);

		Entity minecart = blockLoc.getWorld().spawnEntity(blockLoc, EntityType.MINECART);
		minecart.addPassenger(event.getPlayer());

		Vector unitVector = BlockFace.EAST.getDirection();
		minecart.setVelocity((minecart.getVelocity().add(unitVector.multiply(0.25))).setY(0));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onExitMinecart(VehicleExitEvent event) {
		if (event.isCancelled())
			return;

		if (!(event.getVehicle() instanceof Minecart minecart))
			return;

		if (!(event.getExited() instanceof Player player))
			return;

		if (BearFair21.isNotAtBearFair(player))
			return;

		Block block = minecart.getLocation().getBlock();
		Block under = block.getRelative(BlockFace.DOWN);
		if (MaterialTag.RAILS.isTagged(block.getType()) || MaterialTag.RAILS.isTagged(under.getType())) {
			minecart.remove();
			PlayerUtils.giveItem(player, new ItemStack(Material.MINECART));
		}
	}
}
