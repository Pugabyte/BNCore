package me.pugabyte.nexus.features.events.y2020.pugmas20;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2020.pugmas20.menu.AdventMenu;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.AdventChest;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.AdventChest.District;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.Quests;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerLeavingRegionEvent;
import me.pugabyte.nexus.models.pugmas20.Pugmas20User;
import me.pugabyte.nexus.models.pugmas20.Pugmas20UserService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.Utils.ActionGroup;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.PREFIX;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isBeforePugmas;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isPastPugmas;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isSecondChance;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.location;

public class AdventChests implements Listener {
	public static Map<Integer, Location> adventLootMap = new HashMap<>();
	public static List<AdventChest> adventChestList = new ArrayList<>();
	public static ItemBuilder adventLootHead;
	//
	public static final Block lootOrigin = location(867, 45, 579).getBlock();
	private static final String InvTitle = "Advent Chest #";
	private static UUID adventLootHeadOwner = null;
	private static final String adventLootHeadTitle = "Pugmas Advent Skull";
	private static final String adventLootHeadLore = "Day #";
	//
	private static final String wrongDay = PREFIX + "You cannot open this chest, look for chest #<day>";
	private static final String openPrevious = PREFIX + "Need to find the rest to open this one";
	private static final String alreadyFound = PREFIX + "Already opened this chest!";

	public AdventChests() {
		Nexus.registerListener(this);

		AdventMenu.origin.getRelative(0, 0, 1).getDrops().stream()
				.findFirst().ifPresent(skull -> adventLootHead = new ItemBuilder(skull));
		adventLootHeadOwner = ItemUtils.getSkullOwner(adventLootHead.build());

		loadLootLocations();
		loadlocationations();
	}

	private void loadLootLocations() {
		int index = 1;
		for (int z = 0; z <= 6; z++) {         // 0-3 col (Every other)
			for (int x = 0; x <= 12; x++) {    // 0-6 row (Every other)
				Block block = lootOrigin.getRelative(x, 0, z);
				if (ItemUtils.isNullOrAir(block.getType()) || !block.getType().equals(Material.CHEST))
					continue;

				adventLootMap.put(index++, block.getLocation());
			}
		}
	}

	private void loadlocationations() {
		// @formatter:off
		adventChestList.add(new AdventChest(1,  location(913, 50, 411)));	// Plaza
		adventChestList.add(new AdventChest(2,  location(900, 45, 580)));	// Harbor
		adventChestList.add(new AdventChest(3,  location(997, 50, 497)));	// Frozen
		adventChestList.add(new AdventChest(4,  location(966, 51, 400)));	// Plaza
		adventChestList.add(new AdventChest(5,  location(987, 74, 346)));	// Gardens
		adventChestList.add(new AdventChest(6,  location(860, 50, 441)));	// Plaza
		adventChestList.add(new AdventChest(7,  location(1001, 58, 534)));	// Frozen
		adventChestList.add(new AdventChest(8,  location(847, 54, 583)));	// Harbor
		adventChestList.add(new AdventChest(9,  location(986, 61, 415)));	// Plaza
		adventChestList.add(new AdventChest(10, location(840, 60, 606)));	// Harbor
		adventChestList.add(new AdventChest(11, location(1051, 49, 569)));	// Frozen
		adventChestList.add(new AdventChest(12, location(839, 52, 542)));	// Harbor
		adventChestList.add(new AdventChest(13, location(898, 58, 352)));	// Plaza
		adventChestList.add(new AdventChest(14, location(1062, 60, 424)));	// Gardens
		adventChestList.add(new AdventChest(15, location(964, 52, 441)));	// Plaza
		adventChestList.add(new AdventChest(16, location(1009, 47, 567)));	// Frozen
		adventChestList.add(new AdventChest(17, location(828, 51, 515)));	// Harbor
		adventChestList.add(new AdventChest(18, location(862, 58, 457)));	// Plaza
		adventChestList.add(new AdventChest(19, location(812, 52, 538)));	// Harbor
		adventChestList.add(new AdventChest(20, location(849, 55, 384)));	// Plaza
		adventChestList.add(new AdventChest(21, location(809, 68, 565)));	// Harbor
		adventChestList.add(new AdventChest(22, location(1094, 45, 386)));	// Gardens
		adventChestList.add(new AdventChest(23, location(866, 57, 395)));	// Plaza
		adventChestList.add(new AdventChest(24, location(1056, 43, 531)));	// Frozen
		adventChestList.add(new AdventChest(25, location(886, 95, 313)));	// Unknown
		// @formatter:on
	}

	public static AdventChest getAdventChest(Location location) {
		return adventChestList.stream().filter(adventChest -> adventChest.getLocation().equals(location)).findFirst().orElse(null);
	}

	public static AdventChest getAdventChest(int day) {
		return adventChestList.stream().filter(adventChest -> adventChest.getDay() == day).findFirst().orElse(null);
	}

	// Advent Methods & Listeners

	@EventHandler
	public void onAdventChestOpen(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
		if (BlockUtils.isNullOrAir(event.getClickedBlock())) return;

		Player player = event.getPlayer();
		if (!Pugmas20.isAtPugmas(player)) return;

		Block block = event.getClickedBlock();
		if (!block.getType().equals(Material.CHEST)) return;

		AdventChest adventChest = getAdventChest(block.getLocation());
		if (adventChest == null) return;

		event.setCancelled(true);
		if (!Quests.hasRoomFor(player, 1)) {
			PlayerUtils.send(player, Quests.fullInvError_open);
			return;
		}

		Pugmas20UserService service = new Pugmas20UserService();
		Pugmas20User user = service.get(player);

		int chestDay = adventChest.getDay();
		LocalDate now = LocalDate.now();
		int today = LocalDate.now().getDayOfMonth();

		if (isBeforePugmas(now)) return;
		if (isPastPugmas(now)) return;

		boolean waypoint = !user.getLocatedDays().contains(chestDay);
		user.getLocatedDays().add(chestDay);
		service.save(user);

		boolean openChest = false;
		String reason = "";
		if (user.getFoundDays().contains(chestDay))
			reason = alreadyFound;
		else {
			if (isSecondChance(now))
				if (chestDay != 25 || user.getFoundDays().size() == 24)
					openChest = true;
				else
					reason = openPrevious;
			else if (chestDay == today)
				openChest = true;
			else {
				reason = wrongDay + " (" + AdventChests.getAdventChest(today).getDistrict().getName() + " District)";
			}
		}

		if (!openChest) {
			reason = reason.replaceAll("<day>", String.valueOf(today));
			user.sendMessage(reason);

			if (waypoint)
				user.sendMessage(PREFIX + "Chest &e#" + chestDay + " &3saved as a waypoint");
			return;
		}

		user.getFoundDays().add(chestDay);
		service.save(user);

		giveAdventHead(player, chestDay);
	}

	public static void giveAdventHead(Player player, int day) {
		ItemStack skull = adventLootHead.clone().name(adventLootHeadTitle).lore(adventLootHeadLore + day).build();

		Inventory inventory = Bukkit.createInventory(null, 3 * 9, InvTitle + day);
		inventory.setItem(13, skull);
		player.openInventory(inventory);
		new SoundBuilder(Sound.BLOCK_CHEST_OPEN).receiver(player).play();
	}

	@EventHandler
	public void onOpenAdventLootHead(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getHand() != EquipmentSlot.HAND) return;
		if (!ActionGroup.RIGHT_CLICK.applies(event)) return;

		ItemStack skull = ItemUtils.getTool(player);
		if (ItemUtils.isNullOrAir(skull) || !skull.getType().equals(Material.PLAYER_HEAD)) return;
		if (Utils.isNullOrEmpty(skull.getLore())) return;

		UUID skullOwner = ItemUtils.getSkullOwner(skull);
		if (skullOwner == null) return;
		if (!skullOwner.equals(adventLootHeadOwner)) return;

		String lore = skull.getLore().get(0);
		if(!lore.contains(adventLootHeadLore)) return;

		event.setCancelled(true);
		int day = Integer.parseInt(lore.replaceAll(adventLootHeadLore, ""));

		if (openAdventLootInv(player, day))
			skull.setAmount(skull.getAmount() - 1);
	}

	public static boolean openAdventLootInv(Player player, int day) {
		Inventory inventory = Bukkit.createInventory(null, 3 * 9, InvTitle + day);

		Location loc = AdventChests.adventLootMap.get(day);
		if (loc == null)
			return false;

		Chest chest = (Chest) loc.getBlock().getState();
		ItemStack[] contents = chest.getBlockInventory().getContents();
		if (!Quests.hasRoomFor(player, contents)) {
			PlayerUtils.send(player, Quests.fullInvError_open);
			Quests.sound_villagerNo(player);
			return false;
		}

		inventory.setContents(contents);

		player.openInventory(inventory);
		new SoundBuilder(Sound.BLOCK_CHEST_OPEN).receiver(player).play();

		return true;
	}

	@EventHandler
	public void onAdventLootInvClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		if (event.getInventory().getHolder() != null) return;
		if (!Utils.containsInvViewTitle(event.getView(), InvTitle)) return;

		List<ItemStack> leftover = new ArrayList<>(Arrays.asList(event.getInventory().getContents())).stream()
				.filter(itemStack -> !ItemUtils.isNullOrAir(itemStack)).collect(Collectors.toList());

		if (leftover.size() == 0)
			return;

		PlayerUtils.send(player, Quests.leftoverItems);
		PlayerUtils.giveItems(player, leftover);
	}

	@EventHandler
	public void onDistrictEnter(PlayerEnteredRegionEvent event) {
		Player player = event.getPlayer();
		if (!Pugmas20.isAtPugmas(player)) return;

		Location loc = player.getLocation();
		if (!event.getRegion().getId().matches(District.getRegion() + ".*")) return;

		District district = District.of(loc);
		if (district != District.UNKNOWN)
			ActionBarUtils.sendActionBar(player, "&a&lEntering " + district.getName() + " District");
	}

	@EventHandler
	public void onDistrictExit(PlayerLeavingRegionEvent event) {
		Player player = event.getPlayer();
		if (!Pugmas20.isAtPugmas(player)) return;

		Location loc = player.getLocation();
		if (!event.getRegion().getId().matches(District.getRegion() + ".*")) return;

		District district = District.of(loc);
		if (district != District.UNKNOWN)
			ActionBarUtils.sendActionBar(player, "&c&lExiting " + district.getName() + " District");
	}

}
