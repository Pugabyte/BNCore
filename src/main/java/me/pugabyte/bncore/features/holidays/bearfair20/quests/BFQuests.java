package me.pugabyte.bncore.features.holidays.bearfair20.quests;

import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.holidays.bearfair20.BearFair20;
import me.pugabyte.bncore.features.holidays.bearfair20.quests.fishing.Fishing;
import me.pugabyte.bncore.features.holidays.bearfair20.quests.npcs.Merchants;
import me.pugabyte.bncore.features.holidays.bearfair20.quests.npcs.Talkers;
import me.pugabyte.bncore.utils.CitizensUtils;
import me.pugabyte.bncore.utils.Utils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static me.pugabyte.bncore.features.holidays.bearfair20.BearFair20.BFProtectedRg;
import static me.pugabyte.bncore.features.holidays.bearfair20.BearFair20.WGUtils;
import static me.pugabyte.bncore.utils.StringUtils.colorize;

public class BFQuests implements Listener {
	public static String itemLore = "BearFair20 Item";
	// Data
	private static List<Location> collectorLocs = new ArrayList<>();
	private static List<Location> prevCollectorLocs = new ArrayList<>();
	// Error Messages
	private static String prefix = "&8&l[&eBFQuests&8&l] ";
	public static String cantBreakError = prefix + "&c&lHey! &7That's not a block you can break";
	public static String notFullyGrownError = prefix + "&c&lHey! &7That's not fully grown";
	public static String bottomBlockError = prefix + "&c&lHey! &7You can't break the bottom block";
	public static String decorOnlyError = prefix + "&c&lHey! &7This block is just decoration";
	public static String craftItemError = prefix + "&c&lHey! &7You can only craft that item with BearFair20 items!";
	public static String fishingError = prefix + "&c&lHey! &7You may only fish here using a BearFair20 Fishing Rod";

	public BFQuests() {
		BNCore.registerListener(this);
		new RegenCrops();
		setupCollector();
		new Beehive();
		new Fishing();
		new EasterEggs();
		new SellCrates();
		Recipes.loadRecipes();
	}

	private void setupCollector() {
		World world = BearFair20.world;
		Location observatory = new Location(world, -1097, 157, -1550);
		Location town = new Location(world, -1095, 139, -1666);
		Location forest = new Location(world, -1031, 140, -1556);
		Location flag = new Location(world, -984, 144, -1615);
		Location campsite = new Location(world, -1020, 153, -1760);
		Location ruins = new Location(world, -919, 137, -1711);
		Location carnival = new Location(world, -888, 136, -1659);
		collectorLocs = Arrays.asList(observatory, town, forest, flag, campsite, ruins, carnival);
	}

	public static void moveCollector() {
		if (prevCollectorLocs.size() == collectorLocs.size())
			prevCollectorLocs.clear();

		Location newLoc = Utils.getRandomElement(collectorLocs);
		if (newLoc == null) return;

		for (int i = 0; i < 10; i++) {
			if (!prevCollectorLocs.contains(newLoc)) {
				prevCollectorLocs.add(newLoc);
				break;
			}
			newLoc = Utils.getRandomElement(collectorLocs);
		}

		if (newLoc == null) return;
		newLoc = Utils.getCenteredLocation(newLoc);
		Location finalNewLoc = newLoc;

		NPC npc = CitizensUtils.getNPC(2750);
		Location oldLoc = npc.getEntity().getLocation();
		World world = BearFair20.world;

		world.playSound(oldLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1F, 1F);
		world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, oldLoc, 500, 0.5, 1, 0.5, 0);
		world.spawnParticle(Particle.FLASH, oldLoc, 10, 0, 0, 0);
		npc.despawn();

		world.playSound(finalNewLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1F, 1F);
		world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, finalNewLoc, 500, 0.5, 1, 0.5, 0);
		world.spawnParticle(Particle.FLASH, finalNewLoc, 10, 0, 0, 0);
		npc.spawn(finalNewLoc);
	}

	// Add Lore to items
	@EventHandler
	public void onBlockDropItemEvent(BlockDropItemEvent event) {
		Location loc = event.getBlock().getLocation();
		if (!event.getBlock().getLocation().getWorld().equals(BearFair20.world)) return;
		if (!WGUtils.getRegionsAt(loc).contains(BFProtectedRg)) return;
		event.getItems().forEach(item -> {
			Material type = item.getItemStack().getType();
			if (type.equals(Material.WHEAT_SEEDS) || type.equals(Material.BEETROOT_SEEDS)) {
				item.remove();
				return;
			}

			item.getItemStack().setLore(Collections.singletonList(itemLore));
		});
	}

	@EventHandler
	public void onEntityDropItem(EntityDropItemEvent event) {
		Location loc = event.getEntity().getLocation();
		if (!event.getEntity().getLocation().getWorld().equals(BearFair20.world)) return;
		if (!WGUtils.getRegionsAt(loc).contains(BFProtectedRg)) return;
		event.getItemDrop().getItemStack().setLore(Collections.singletonList(itemLore));
	}

	@EventHandler
	public void onCraftItem(CraftItemEvent event) {
		if (!(event.getView().getPlayer() instanceof Player)) return;
		Player player = (Player) event.getView().getPlayer();
		Location loc = player.getLocation();
		if (!WGUtils.getRegionsAt(loc).contains(BFProtectedRg)) return;

		ItemStack result = event.getInventory().getResult();
		ItemStack[] ingredients = event.getInventory().getMatrix();

		boolean questCrafting = true;
		for (ItemStack ingredient : ingredients) {
			if (Utils.isNullOrAir(ingredient))
				continue;
			ItemMeta meta = ingredient.getItemMeta();
			if (!meta.hasLore() || meta.getLore() == null) {
				questCrafting = false;
				continue;
			}

			if (!meta.getLore().contains(itemLore))
				questCrafting = false;
		}

		if (!questCrafting) {
			if (result != null && result.getItemMeta().getLore() != null && result.getItemMeta().getLore().contains(itemLore)) {
				event.getInventory().setResult(new ItemStack(Material.AIR));
				player.sendMessage(colorize(craftItemError));
			}
		}
	}

	@EventHandler
	public void onPrepareCraftItem(PrepareItemCraftEvent event) {
		if (!(event.getView().getPlayer() instanceof Player)) return;
		Player player = (Player) event.getView().getPlayer();
		Location loc = player.getLocation();
		if (!WGUtils.getRegionsAt(loc).contains(BFProtectedRg)) return;

		ItemStack[] ingredients = event.getInventory().getMatrix();
		ItemStack result = event.getInventory().getResult();

		// Each item must be a BF20 item, for it to result in a BF20 item
		boolean questCrafting = true;
		for (ItemStack ingredient : ingredients) {
			if (Utils.isNullOrAir(ingredient))
				continue;
			ItemMeta meta = ingredient.getItemMeta();
			if (!meta.hasLore() || meta.getLore() == null) {
				questCrafting = false;
				continue;
			}

			if (!meta.getLore().contains(itemLore))
				questCrafting = false;
		}

		if (!questCrafting)
			return;

		if (result == null) {
			result = Recipes.getRecipe(Arrays.asList(ingredients));
			if (result == null)
				return;
		}

		ItemMeta resultMeta = result.getItemMeta();
		resultMeta.setLore(Collections.singletonList(itemLore));
		result.setItemMeta(resultMeta);
		event.getInventory().setResult(result);
	}

	// NPC Stuff
	@EventHandler
	public void onRightClickNPC(NPCRightClickEvent event) {
		Player player = event.getClicker();
		Location loc = player.getLocation();
		if (player.getWorld().equals(BearFair20.world) && WGUtils.getRegionsAt(loc).contains(BFProtectedRg)) {
			int id = event.getNPC().getId();
			Talkers.startScript(player, id);
			Merchants.openMerchant(player, id);
		}
	}

	// Preventers
	@EventHandler
	public void onInteractWithVillager(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		if (!(entity instanceof Villager)) return;
		if (CitizensAPI.getNPCRegistry().isNPC(entity)) return;

		Player player = event.getPlayer();
		Location loc = player.getLocation();

		if (player.getWorld().equals(BearFair20.world) && WGUtils.getRegionsAt(loc).contains(BFProtectedRg)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onMcMMOXpGainEvent(McMMOPlayerXpGainEvent event) {
		Location loc = event.getPlayer().getLocation();
		if (!WGUtils.getRegionsAt(loc).contains(BFProtectedRg)) return;
		event.setRawXpGained(0F);
		event.setCancelled(true);
	}
	//

	//TODO:
	// - on milk cow --> give BF20 milk bucket, ignore if bucket is bf20 or not
	// - on shear bee nest, if 2 blocks below nest is a campfire, give BF20 honeycomb
	// - Prevent enchanting BF20 Items
}
