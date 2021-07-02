package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources;

import eden.utils.StringUtils;
import eden.utils.TimeUtils.Time;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.Errors;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.task.Task;
import me.pugabyte.nexus.models.task.TaskService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SerializationUtils.JSON;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.send;

public class Mining implements Listener {
	public static String taskId = "bearfair21-ore-regen";

	public Mining() {
		Nexus.registerListener(this);

		Tasks.repeatAsync(Time.SECOND, Time.SECOND, () -> {
			TaskService service = new TaskService();
			service.process(taskId).forEach(task -> {
				Map<String, Object> data = task.getJson();

				Location location = JSON.deserializeLocation((String) data.get("location"));
				Material material = Material.valueOf((String) data.get("material"));

				Tasks.sync(() -> location.getBlock().setType(material));

				service.complete(task);
			});
		});
	}

	public static boolean canBreak(Material type) {
		return OreType.getOres().contains(type);
	}

	public static boolean breakBlock(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Material type = block.getType();

		if (!OreType.getOres().contains(type))
			return false;

		OreType oreType = OreType.ofOre(type);
		if (oreType == null)
			return false;

		ItemStack tool = player.getInventory().getItemInMainHand();
		if (!oreType.canBeMinedBy(tool.getType())) {
			if (new CooldownService().check(player, "BF21_cantbreak_tool", Time.SECOND.x(15))) {
				send(Errors.cantBreak + " with this tool. Needs either: " + oreType.getCanBreak(), player);
				Quests.sound_villagerNo(player);
			}
			return true;
		}

		Quests.giveExp(player);
		new SoundBuilder(Sound.BLOCK_STONE_BREAK).location(player.getLocation()).category(SoundCategory.BLOCKS).play();
		PlayerUtils.giveItem(player, oreType.getIngotItemStack(tool));

		scheduleRegen(block);
		block.setType(Material.STONE);
		return true;
	}

	public static void scheduleRegen(Block block) {
		new TaskService().save(new Task(taskId, new HashMap<>() {{
			put("location", JSON.serializeLocation(block.getLocation()));
			put("material", block.getType());
		}}, LocalDateTime.now().plusSeconds(RandomUtils.randomInt(3 * 60, 5 * 60))));
	}

	@AllArgsConstructor
	public enum OreType {
		LAPIS(Material.LAPIS_ORE, Material.LAPIS_LAZULI, 1, 3, Material.STONE_PICKAXE),
		COAL(Material.COAL_ORE, Material.COAL, 1, 3, Material.STONE_PICKAXE),
		GOLD(Material.GOLD_ORE, Material.GOLD_NUGGET, 1, 2, Material.IRON_PICKAXE),
		IRON(Material.IRON_ORE, Material.IRON_NUGGET, 1, 3, Material.STONE_PICKAXE),
		DIAMOND(Material.DIAMOND_ORE, Material.DIAMOND, 1, 1, Material.IRON_PICKAXE);

		@Getter
		private final Material ore;
		@Getter
		private final Material ingot;
		private final int min;
		private final int max;
		@Getter
		private final Material pickaxe;

		private static final List<Material> pickaxeOrder = Arrays.asList(Material.WOODEN_PICKAXE, Material.GOLDEN_PICKAXE,
			Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE);

		public static List<Material> getOres() {
			return Arrays.stream(values()).map(OreType::getOre).toList();
		}

		public ItemStack getIngotItemStack(ItemStack tool) {
			ItemMeta meta = tool.getItemMeta();
			int level = 1;
			if (meta.hasEnchants()) {
				if (meta.getEnchants().keySet().stream().anyMatch(enchantment -> enchantment.equals(Enchantment.LOOT_BONUS_BLOCKS))) {
					level = meta.getEnchants().get(Enchantment.LOOT_BONUS_BLOCKS);
				}
			}

			int amount = RandomUtils.randomInt(min, max) * RandomUtils.randomInt(1, level);
			return new ItemBuilder(ingot).amount(amount).build();
		}

		public static OreType ofOre(Material ore) {
			for (OreType oreType : values())
				if (oreType.getOre() == ore)
					return oreType;
			return null;
		}

		public boolean canBeMinedBy(Material pickaxe) {
			return pickaxeOrder.indexOf(pickaxe) >= pickaxeOrder.indexOf(this.getPickaxe());
		}

		public String getCanBreak() {
			List<Material> result = new ArrayList<>();
			for (Material material : pickaxeOrder) {
				if (canBeMinedBy(material))
					result.add(material);
			}

			return result.stream().map(StringUtils::camelCase).collect(Collectors.joining(", "));
		}
	}
}
