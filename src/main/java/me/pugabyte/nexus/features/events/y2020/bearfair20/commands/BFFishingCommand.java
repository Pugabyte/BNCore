package me.pugabyte.nexus.features.events.y2020.bearfair20.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Timespan;
import me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20;
import me.pugabyte.nexus.features.events.y2020.bearfair20.models.WeightedLoot;
import me.pugabyte.nexus.features.events.y2020.bearfair20.quests.fishing.Fishing;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static me.pugabyte.nexus.features.events.y2020.bearfair20.quests.BFQuests.itemLore;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.quests.fishing.Fishing.weightedList;

@Permission("group.staff")
public class BFFishingCommand extends CustomCommand {
	static Map<UUID, LocalDateTime> timestamps = new HashMap<>();
	ItemStack fishingRod = new ItemBuilder(Material.FISHING_ROD).enchant(Enchantment.LURE, 2).lore(itemLore).build();

	public BFFishingCommand(CommandEvent event) {
		super(event);
	}

	private ItemStack unbreakable(ItemStack itemStack) {
		ItemMeta meta = itemStack.getItemMeta();
		meta.setUnbreakable(true);
		itemStack.setItemMeta(meta);
		return itemStack;
	}

	@Path("giveAll")
	@Permission(value = "group.admin", absolute = true)
	public void giveAll() {
		giveAllLoot(player());
	}

	@Path("start")
	public void startFishing() {
		UUID uuid = uuid();
		if (timestamps.containsKey(uuid))
			error("You're already fishing!");
		timestamps.put(uuid, LocalDateTime.now());

		PlayerUtils.giveItem(player(), unbreakable(fishingRod));
		send("Start fishing!");
	}

	@Path("stop")
	public void stopFishing() {
		UUID uuid = uuid();
		if (!timestamps.containsKey(uuid))
			error("You're aren't fishing!");

		LocalDateTime then = timestamps.get(uuid());
		String timeSpan = Timespan.of(then).format();
		timestamps.remove(uuid());

		String region = getIslandRegion(location());

		ItemStack[] contents = inventory().getContents();
		List<ItemStack> loot = getLoot(contents);
		int trash = countTrash(loot);
		int treasure = countTreasure(loot);
		int rare = countRare(loot);
		int island = countIsland(loot);
		int defaultFish = countDefault(loot);

		removeItems(player(), loot);

		send("");
		send("Stop fishing! Take a screenshot of below, and send it to Wakka.");
		send(" - - - - ");
		send("Trash: " + trash);
		send("Default: " + defaultFish);
		send("Rare: " + rare);
		send("Island: " + island);
		send("Treasure: " + treasure);
		send("");
		send("Time: " + timeSpan);
		send("Loc: " + region);
		send(" - - - - ");
		send("");

	}

	private List<ItemStack> getLoot(ItemStack[] contents) {
		Set<WeightedLoot> weightedList = Fishing.weightedList;
		Set<ItemStack> itemStacks = new HashSet<>();
		List<ItemStack> loot = new ArrayList<>();

		weightedList.forEach(weightedLoot -> itemStacks.add(weightedLoot.getItemStack()));
		for (ItemStack content : contents) {
			if (ItemUtils.isNullOrAir(content))
				continue;

			int amount = content.getAmount();
			if (amount > 1) {
				content.setAmount(1);
			}

			if (itemStacks.contains(content)) {
				content.setAmount(amount);
				loot.add(content);
			}
		}

		return loot;
	}

	private String getIslandRegion(Location location) {
		Set<ProtectedRegion> protectedRegions = BearFair20.getWGUtils().getRegionsAt(location);
		for (ProtectedRegion protectedRegion : protectedRegions) {
			if (protectedRegion.getId().contains(BearFair20.getRegion() + "_")) {
				String id = protectedRegion.getId();
				return id.substring(id.indexOf("_") + 1);
			}
		}
		return "unknown";
	}

	private void removeItems(Player player, List<ItemStack> loot) {
		// Remove loot
		for (ItemStack itemStack : loot)
			player.getInventory().remove(itemStack);

		// Remove fishing rod
		for (ItemStack content : player.getInventory().getContents()) {
			if (ItemUtils.isNullOrAir(content)) continue;
			if (!content.getType().equals(Material.FISHING_ROD)) continue;
			if (BearFair20.isBFItem(content))
				player.getInventory().remove(content);
		}
	}

	private int countDefault(List<ItemStack> contents) {
		Set<WeightedLoot> weightedList = Fishing.weightedList;
		Set<ItemStack> filterList = new HashSet<>();

		for (WeightedLoot weightedLoot : weightedList) {
			int weight = weightedLoot.getWeight();
			if (weight >= 14 && weight <= 25)
				filterList.add(weightedLoot.getItemStack());
		}

		int count = 0;
		for (ItemStack content : contents) {
			int amount = content.getAmount();
			content.setAmount(1);
			if (filterList.contains(content)) {
				count += amount;
			}
			content.setAmount(amount);
		}
		return count;
	}

	private int countTrash(List<ItemStack> contents) {
		Set<WeightedLoot> weightedList = Fishing.weightedList;
		Set<ItemStack> filterList = new HashSet<>();

		for (WeightedLoot weightedLoot : weightedList) {
			int weight = weightedLoot.getWeight();
			if (weight == 10)
				filterList.add(weightedLoot.getItemStack());
		}

		int count = 0;
		for (ItemStack content : contents) {
			int amount = content.getAmount();
			content.setAmount(1);
			if (filterList.contains(content)) {
				count += amount;
			}
			content.setAmount(amount);
		}
		return count;
	}

	private int countTreasure(List<ItemStack> contents) {
		Set<WeightedLoot> weightedList = Fishing.weightedList;
		Set<ItemStack> filterList = new HashSet<>();

		for (WeightedLoot weightedLoot : weightedList) {
			Material material = weightedLoot.getItemStack().getType();
			if (isTreasure(material))
				filterList.add(weightedLoot.getItemStack());
		}

		int count = 0;
		for (ItemStack content : contents) {
			int amount = content.getAmount();
			content.setAmount(1);
			if (filterList.contains(content)) {
				count += amount;
			}
			content.setAmount(amount);
		}
		return count;
	}

	private int countRare(List<ItemStack> contents) {
		Set<WeightedLoot> weightedList = Fishing.weightedList;
		Set<ItemStack> filterList = new HashSet<>();

		for (WeightedLoot weightedLoot : weightedList) {
			int weight = weightedLoot.getWeight();
			if (weight == 2 && !(isTreasure(weightedLoot.getItemStack().getType())))
				filterList.add(weightedLoot.getItemStack());
		}

		int count = 0;
		for (ItemStack content : contents) {
			int amount = content.getAmount();
			content.setAmount(1);
			if (filterList.contains(content)) {
				count += amount;
			}
			content.setAmount(amount);
		}
		return count;
	}

	private int countIsland(List<ItemStack> contents) {
		Set<WeightedLoot> weightedList = Fishing.weightedList;
		Set<ItemStack> filterList = new HashSet<>();

		for (WeightedLoot weightedLoot : weightedList) {
			int weight = weightedLoot.getWeight();
			if (weight == 1 && !(isTreasure(weightedLoot.getItemStack().getType())))
				filterList.add(weightedLoot.getItemStack());
		}

		int count = 0;
		for (ItemStack content : contents) {
			int amount = content.getAmount();
			content.setAmount(1);
			if (filterList.contains(content)) {
				count += amount;
			}
			content.setAmount(amount);
		}
		return count;
	}

	private boolean isTreasure(Material material) {
		return (MaterialTag.CORAL_PLANTS.isTagged(material)
				|| material.equals(Material.PHANTOM_MEMBRANE)
				|| material.equals(Material.HEART_OF_THE_SEA)
				|| material.equals(Material.NAUTILUS_SHELL));
	}


	private void giveAllLoot(Player player) {
		List<ItemStack> items = new ArrayList<>();
		weightedList.forEach(weightedLoot -> items.add(weightedLoot.getItemStack()));
		PlayerUtils.giveItems(player, items);
	}

}
