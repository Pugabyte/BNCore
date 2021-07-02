package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs;

import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.MerchantBuilder.TradeBuilder;
import me.pugabyte.nexus.utils.RandomUtils;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Collector {
	@Getter
	public static List<TradeBuilder> randomTrades = new ArrayList<>();
	@Getter
	public static Location currentLoc = null;
	private static final NPC npc = BearFair21NPC.COLLECTOR.getNPC();
	//
	private static final List<TradeBuilder> possibleTrades = loadTrades();
	private static final List<Location> locations = loadLocations();
	private static final List<Location> prevLocations = new ArrayList<>();

	public static void startup() {
		if (npc != null)
			currentLoc = npc.getStoredLocation();
		newTrades();
	}

	private static List<Location> loadLocations() {
		World world = BearFair21.getWorld();
		Location observatory = new Location(world, -113, 155, 16);
		Location town1 = new Location(world, -105, 139, -104);
		Location town2 = new Location(world, -125, 149, -26);
		Location forest = new Location(world, -39, 140, 7);
		Location flag = new Location(world, 1, 143, -60);
		Location campsite = new Location(world, -17, 153, -200);
		Location balloon = new Location(world, 50, 153, -201);
		Location carnival1 = new Location(world, 110, 138, -58);
		Location carnival2 = new Location(world, 157, 137, -26);

		return Arrays.asList(observatory, town1, town2, forest, flag, campsite, balloon, carnival1, carnival2);
	}

	private static List<TradeBuilder> loadTrades() {
		return new ArrayList<>() {{
			// Fishing Loot
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.TIGER_TROUT.getGold() * 2))
				.ingredient(FishingLoot.TIGER_TROUT.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.SEA_CUCUMBER.getGold() * 2))
				.ingredient(FishingLoot.SEA_CUCUMBER.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.GLACIERFISH.getGold() * 2))
					.ingredient(FishingLoot.GLACIERFISH.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.CRIMSONFISH.getGold() * 2))
					.ingredient(FishingLoot.CRIMSONFISH.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.BLOBFISH.getGold() * 2))
					.ingredient(FishingLoot.BLOBFISH.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.STONEFISH.getGold() * 2))
					.ingredient(FishingLoot.STONEFISH.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.MIDNIGHT_CARP.getGold() * 2))
					.ingredient(FishingLoot.MIDNIGHT_CARP.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.SUNFISH.getGold() * 2))
					.ingredient(FishingLoot.SUNFISH.getItem()));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(FishingLoot.NAUTILUS_SHELL.getGold() * 2))
					.ingredient(FishingLoot.NAUTILUS_SHELL.getItem()));
			// Food
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(3))
					.ingredient(new ItemStack(Material.CAKE)));
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(3))
					.ingredient(new ItemStack(Material.PUMPKIN_PIE)));
			// Items
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(4))
					.ingredient(new ItemStack(Material.BONE_BLOCK)));
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(6))
					.ingredient(new ItemStack(Material.LEATHER_HORSE_ARMOR)));
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(5))
					.ingredient(new ItemStack(Material.ANVIL)));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(4))
					.ingredient(new ItemStack(Material.CAMPFIRE)));
			add(new TradeBuilder()
				.result(Merchants.goldNugget.clone().amount(4))
					.ingredient(new ItemStack(Material.CROSSBOW)));
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(5))
					.ingredient(new ItemStack(Material.LECTERN)));
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(4))
					.ingredient(new ItemStack(Material.BOOKSHELF)));
			add(new TradeBuilder()
				.result(Merchants.goldIngot.clone().amount(1))
					.ingredient(new ItemStack(Material.BOOK)));
		}};
	}

	public static void move() {
		if (npc == null) {
			Nexus.warn("Could not find Collector NPC");
			return;
		}

		prevLocations.clear();

		Location newLoc = RandomUtils.randomElement(locations);
		if (newLoc == null) {
			Nexus.warn("Could not find new location to move collector to");
			return;
		}

		for (int i = 0; i < 10; i++) {
			if (!prevLocations.contains(newLoc)) {
				prevLocations.add(newLoc);
				break;
			}
			newLoc = RandomUtils.randomElement(locations);
		}

		if (newLoc == null) {
			Nexus.warn("Could not find unused new location to move collector to");
			return;
		}

		currentLoc = LocationUtils.getCenteredLocation(newLoc);
		Location oldLoc = npc.getStoredLocation();

		Quests.poof(oldLoc);
		npc.teleport(currentLoc, TeleportCause.PLUGIN);
		Quests.poof(currentLoc);

		newTrades();
	}

	private static void newTrades() {
		List<TradeBuilder> choices = new ArrayList<>(possibleTrades);
		if (randomTrades == null)
			randomTrades = new ArrayList<>();

		// Remove current trades from choices
		if (!randomTrades.isEmpty()) {
			for (TradeBuilder trade : randomTrades)
				choices.remove(trade);
			randomTrades.clear();
		}

		// Pick new trades
		for (int i = 0; i < 4; i++) {
			TradeBuilder random = RandomUtils.randomElement(choices);
			choices.remove(random);
			randomTrades.add(random);
		}
	}

	public static void spawn() {
		if (npc == null) return;
		if (npc.isSpawned()) return;
		npc.spawn(npc.getStoredLocation());
	}
}
