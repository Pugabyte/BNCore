package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootCategory;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.pugabyte.nexus.utils.ItemUtils.getTool;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;

public class Fishing implements Listener {

	public Fishing() {
		Nexus.registerListener(this);
	}

	private static ItemStack getLoot(Player player) {
		FishingLoot fishingLoot = getFishingLoot(player);
		if (fishingLoot == null)
			return null;

		return fishingLoot.getItem();
	}

	public static FishingLoot getFishingLoot(Player player) {
		FishingLootCategory category = getLootCategory(player);
		Map<FishingLoot, Double> lootMap = new HashMap<>();
		for (FishingLoot loot : FishingLoot.of(category)) {
			if (category.equals(FishingLootCategory.UNIQUE)) {
				if (loot.applies(player))
					lootMap.put(loot, loot.getWeight());
			} else
				lootMap.put(loot, loot.getWeight());
		}

		if (lootMap.isEmpty())
			return null;

		return RandomUtils.getWeightedRandom(lootMap);
	}

	private static FishingLootCategory getLootCategory(Player player) {
		BearFair21UserService userService = new BearFair21UserService();
		BearFair21User user = userService.get(player);
		Map<FishingLootCategory, Double> categoryMap = new HashMap<>();
		for (FishingLootCategory category : FishingLootCategory.values()) {
			double weight = category.getWeight();
			if (category.equals(FishingLootCategory.JUNK))
				weight = user.getJunkWeight().getWeight();
			categoryMap.put(category, weight);
		}

		return RandomUtils.getWeightedRandom(categoryMap);
	}

	@EventHandler
	public void onFishCatch(PlayerFishEvent event) {
		Player player = event.getPlayer();
		if (BearFair21.isNotAtBearFair(player)) return;

		ItemStack rod = getTool(player);
		if (rod == null) return;

		if (!event.getState().equals(PlayerFishEvent.State.CAUGHT_FISH)) return;

		Entity caught = event.getCaught();
		if (!(caught instanceof Item item)) return;

		ItemStack tool = ItemUtils.getTool(player, Material.FISHING_ROD);
		if (isNullOrAir(tool))
			return;

		ItemMeta meta = tool.getItemMeta();

		int loops = 0;
		if (meta != null && meta.hasEnchants()) {
			if (meta.getEnchants().keySet().stream().anyMatch(enchantment -> enchantment.equals(Enchantment.LUCK))) {
				loops = RandomUtils.randomInt(0, meta.getEnchants().get(Enchantment.LUCK));
			}
		}

		ItemStack loot = getFishingItem(player);
		ItemStack itemStack = item.getItemStack();
		itemStack.setType(loot.getType());
		itemStack.setItemMeta(loot.getItemMeta());

		for (int i = 0; i < loops; i++) {
			Tasks.wait(1, () -> {
				Item _item = player.getWorld().dropItem(item.getLocation(), getFishingItem(player));
				_item.setVelocity(item.getVelocity());
			});
		}
	}

	private ItemStack getFishingItem(Player player) {
		ItemStack loot = getLoot(player);
		if (loot == null)
			loot = FishingLoot.CARP.getItem();

		return loot;
	}

	@EventHandler
	public void onOpenTreasureChest(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;
		Player player = event.getPlayer();

		ItemStack item = getTool(player);
		if (ItemUtils.isNullOrAir(item)) return;
		if (!ItemUtils.isFuzzyMatch(item, FishingLoot.TREASURE_CHEST.getItem())) return;

		item.setAmount(item.getAmount() - 1);
		Quests.giveItem(player, getTreasureChestLoot());
	}

	private static ItemStack getTreasureChestLoot() {
		List<ItemStack> treasure = Arrays.asList(
			FishingLoot.TREASURE_CHEST.getItem(),
			FishingLoot.UNBREAKING.getItem(),
			FishingLoot.EFFICIENCY.getItem(),
			FishingLoot.LURE.getItem(),
			FishingLoot.FORTUNE.getItem(),
			new ItemBuilder(Material.GOLD_NUGGET).amount(RandomUtils.randomInt(3, 7)).build(),
			new ItemBuilder(Material.GOLD_INGOT).amount(RandomUtils.randomInt(2, 5)).build(),
			new ItemBuilder(Material.IRON_INGOT).amount(RandomUtils.randomInt(2, 5)).build(),
			new ItemBuilder(Material.DIAMOND).amount(RandomUtils.randomInt(1, 3)).build()
		);

		return RandomUtils.randomElement(treasure);
	}
}
