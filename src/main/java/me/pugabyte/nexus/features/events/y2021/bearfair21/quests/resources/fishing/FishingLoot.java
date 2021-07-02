package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.Merchants;
import me.pugabyte.nexus.utils.Enchant;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MerchantBuilder.TradeBuilder;
import me.pugabyte.nexus.utils.RandomUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootCategory.FISH;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootCategory.JUNK;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootCategory.TREASURE;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootCategory.UNIQUE;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootTime.BOTH;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootTime.DAY;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootTime.NIGHT;


@Getter
@AllArgsConstructor
public enum FishingLoot {

	// Fish
	CARP(FISH, 2, Material.COD, 40.0, "Carp", 1),
	SALMON(FISH, 2, Material.COD, 30.0, "Salmon", 2),
	TROPICAL_FISH(FISH, 2, Material.COD, 20.0, "Tropical Fish", 3),
	PUFFERFISH(FISH, 2, Material.COD, 10.0, "Pufferfish", 4),
	BULLHEAD(FISH, 2, Material.COD, 10.0, "Bullhead", 5),
	STURGEON(FISH, 2, Material.COD, 10.0, "Sturgeon", 6),
	WOODSKIP(FISH, 2, Material.COD, 10.0, "Woodskip", 7),
	VOID_SALMON(FISH, 2, Material.COD, 10.0, "Void Salmon", 8),
	RED_SNAPPER(FISH, 2, Material.COD, 10.0, "Red Snapper", 9),
	RED_MULLET(FISH, 2, Material.COD, 10.0, "Red Mullet", 10),
	// Junk
	OLD_BOOTS(JUNK, 1, Material.LEATHER_BOOTS, 10.0, "Old Boots", 1),
	RUSTY_SPOON(JUNK, 0, Material.IRON_SHOVEL, 10.0, "Rusty Spoon", 1),
	BROKEN_CD(JUNK, 0, Material.MUSIC_DISC_11, 10.0, "Broken CD", 1),
	LOST_BOOK(JUNK, 1, Material.BOOK, 10.0, "Lost Book", 1),
	SOGGY_NEWSPAPER(JUNK, 0, Material.PAPER, 10.0, "Soggy Newspaper", 1),
	DRIFTWOOD(JUNK, 0, Material.STICK, 10.0, "Driftwood", 47),
	SEAWEED(JUNK, 0, Material.KELP, 10.0, "Seaweed", 1),
	// Treasure
	GOLD_NUGGET(TREASURE, 1, Material.GOLD_NUGGET, 15.0),
	UNBREAKING(TREASURE, 5, Material.ENCHANTED_BOOK, 10.0),
	EFFICIENCY(TREASURE, 5, Material.ENCHANTED_BOOK, 10.0),
	FORTUNE(TREASURE, 6, Material.ENCHANTED_BOOK, 8.0),
	LURE(TREASURE, 6, Material.ENCHANTED_BOOK, 8.0),
	DIAMOND(TREASURE, 7, Material.DIAMOND, 6.0),
	NAUTILUS_SHELL(TREASURE, 7, Material.NAUTILUS_SHELL, 6.0),
	TREASURE_CHEST(TREASURE, 8, Material.CHEST, 5.0, "Treasure Chest", 1),
	// Unique
	MIDNIGHT_CARP(UNIQUE, 8, Material.COD, 50.0, "Midnight Carp", 11, "main", NIGHT),
	SUNFISH(UNIQUE, 8, Material.COD, 50.0, "Sunfish", 12, "main", DAY),
	STONEFISH(UNIQUE, 10, Material.COD, 100.0, "Stonefish", 13, "main", 120),
	TIGER_TROUT(UNIQUE, 8, Material.COD, 50.0, "Tiger Trout", 14, "minigamenight"),
	SEA_CUCUMBER(UNIQUE, 8, Material.SEA_PICKLE, 50.0, "Sea Cucumber", 1, "minigamenight"),
	GLACIERFISH(UNIQUE, 10, Material.COD, 100.0, "Glacierfish", 15, "pugmas"),
	CRIMSONFISH(UNIQUE, 10, Material.COD, 100.0, "Crimsonfish", 16, "halloween"),
	BLOBFISH(UNIQUE, 10, Material.COD, 100.0, "Blobfish", 17, "summerdownunder"),
	;

	FishingLootCategory category;
	int gold;
	Material material;
	double weight;
	String customName;
	int customModelData;
	String region;
	FishingLootTime time;
	Integer maxY;

	FishingLoot(FishingLootCategory category, int gold, Material material, double weight) {
		this(category, gold, material, weight, null, 0);
	}

	FishingLoot(FishingLootCategory category, int gold, Material material, double weight, String customName, int customModelData) {
		this(category, gold, material, weight, customName, customModelData, null, BOTH);
	}

	FishingLoot(FishingLootCategory category, int gold, Material material, double weight, String customName, int customModelData, String region) {
		this(category, gold, material, weight, customName, customModelData, region, BOTH);
	}

	FishingLoot(FishingLootCategory category, int gold, Material material, double weight, String customName, int customModelData, String region, FishingLootTime time) {
		this(category, gold, material, weight, customName, customModelData, region, time, null);
	}

	FishingLoot(FishingLootCategory category, int gold, Material material, double weight, String customName, int customModelData, String region, Integer maxY) {
		this(category, gold, material, weight, customName, customModelData, region, BOTH, maxY);
	}

	public static List<FishingLoot> of(FishingLootCategory category) {
		List<FishingLoot> result = new ArrayList<>();
		for (FishingLoot loot : values()) {
			if (loot.getCategory().equals(category))
				result.add(loot);
		}
		return result;
	}

	public static boolean isTrash(ItemStack itemStack) {
		for (FishingLoot loot : of(JUNK)) {
			if (ItemUtils.isFuzzyMatch(loot.getItem(), itemStack))
				return true;
		}

		return false;
	}

	public double getChance() {
		double sum = 0.0;
		FishingLootCategory category = this.getCategory();
		for (FishingLoot loot : values())
			if (category.equals(loot.getCategory()))
				sum += loot.getWeight();

		return (weight / sum) * 100;
	}

	public boolean applies(Player player) {
		return this.timeApplies(player) && this.regionApplies(player) && this.yValueApplies(player);
	}

	private boolean timeApplies(Player player) {
		World world = player.getWorld();
		FishingLootTime time = this.getTime();
		if (time == BOTH)
			return true;

		boolean isDay = world.isDayTime();
		if (isDay && time == DAY)
			return true;
		else
			return !isDay && time == NIGHT;
	}

	private boolean regionApplies(Player player) {
		Location location = player.getLocation();
		if (this.getRegion() == null)
			return true;

		return BearFair21.getWGUtils().isInRegion(location, BearFair21.getRegion() + "_" + this.getRegion());
	}

	private boolean yValueApplies(Player player) {
		Location location = player.getLocation();
		if (this.getMaxY() == null)
			return true;

		return location.getBlockY() <= this.getMaxY();
	}

	public ItemStack getItem() {
		return getItemBuilder().build();
	}

	public ItemBuilder getItemBuilder() {
		Material material = this.getMaterial();
		ItemBuilder result = new ItemBuilder(material);

		if (this.getCustomName() != null)
			result.name(this.getCustomName());
		if (this.getCustomModelData() != 0)
			result.customModelData(this.getCustomModelData());
		if (material.equals(Material.ENCHANTED_BOOK)) {
			if (this.equals(UNBREAKING))
				result.enchant(Enchant.UNBREAKING, 1);
			if (this.equals(EFFICIENCY)) {
				if (RandomUtils.chanceOf(20))
					result.enchant(Enchant.EFFICIENCY, 3);
				else
					result.enchant(Enchant.EFFICIENCY, 2);
			}
			if (this.equals(FORTUNE))
				result.enchant(Enchant.FORTUNE, 1);
			if (this.equals(LURE))
				result.enchant(Enchant.LURE, 1);
		}

		return result.lore(getLore());
	}

	public String getLore() {
		return switch (category) {
			case FISH -> "&7Fish";
			case UNIQUE -> "&7Unique";
			case TREASURE -> null;
			case JUNK -> "&7Trash";
		};
	}

	public static List<TradeBuilder> getTrades() {
		return new ArrayList<>() {{
			for (FishingLoot loot : values()) {
				if (loot.equals(EFFICIENCY)) {
					add(new TradeBuilder()
						.result(Merchants.goldNugget.clone().amount(loot.getGold()).build())
						.ingredient(loot.getItemBuilder().enchant(Enchant.EFFICIENCY, 2))
					);
					add(new TradeBuilder()
						.result(Merchants.goldNugget.clone().amount(loot.getGold()).build())
						.ingredient(loot.getItemBuilder().enchant(Enchant.EFFICIENCY, 3))
					);
				} else {
					add(new TradeBuilder()
						.result(Merchants.goldNugget.clone().amount(loot.getGold()).build())
						.ingredient(loot.getItem())
					);
				}
			}
		}};
	}

	@Getter
	@AllArgsConstructor
	public enum FishingLootCategory {
		FISH(50.0),
		JUNK(JunkWeight.MAX.getWeight()),
		UNIQUE(17.0),
		TREASURE(8.0);

		double weight;

		public double getChance() {
			double sum = 0.0;
			for (FishingLootCategory category : values())
				sum += category.getWeight();

			return (weight / sum) * 100;
		}
	}

	public enum FishingLootTime {
		DAY,
		NIGHT,
		BOTH
	}

	@Getter
	@AllArgsConstructor
	public enum JunkWeight {
		MAX(25.0, 0),
		EIGHTY_PERCENT(20.0, 25),    // +25
		SIXTY_PERCENT(15.0, 100),    // +75
		MIN(10.0, 225);            // +125

		double weight;
		int amount;

		public JunkWeight update(int amount) {
			if (amount >= MIN.getAmount())
				return MIN;
			else if (amount >= SIXTY_PERCENT.getAmount())
				return SIXTY_PERCENT;
			else if (amount >= EIGHTY_PERCENT.getAmount())
				return SIXTY_PERCENT;
			else if (amount >= MAX.getAmount())
				return MAX;

			return this;
		}
	}

}
