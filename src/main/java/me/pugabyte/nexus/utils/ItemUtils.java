package me.pugabyte.nexus.utils;

import me.lexikiq.HasPlayer;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;

public class ItemUtils {

	@Contract("_, null -> false; null, _ -> false")
	public static boolean isTypeAndNameEqual(ItemStack itemStack1, ItemStack itemStack2) {
		if (isNullOrAir(itemStack1) || isNullOrAir(itemStack2)) return false;
		if (itemStack1.getType() != itemStack2.getType()) return false;
		Function<ItemStack, String> name = item -> stripColor(item.getItemMeta().getDisplayName());
		return name.apply(itemStack1).equals(name.apply(itemStack2));
	}

	public static boolean isFuzzyMatch(ItemStack itemStack1, ItemStack itemStack2) {
		if (itemStack1.getType() != itemStack2.getType())
			return false;

		ItemMeta itemMeta1 = itemStack1.getItemMeta();
		ItemMeta itemMeta2 = itemStack2.getItemMeta();

		if (!itemMeta1.getDisplayName().equals(itemMeta2.getDisplayName()))
			return false;

		if (!Objects.equals(itemMeta1.getLore(), itemMeta2.getLore()))
			return false;

		if (itemMeta1.hasCustomModelData() && itemMeta2.hasCustomModelData())
			return itemMeta1.getCustomModelData() == itemMeta2.getCustomModelData();

		return true;
	}

	public static void combine(List<ItemStack> itemStacks, ItemStack... newItemStacks) {
		combine(itemStacks, Arrays.asList(newItemStacks));
	}

	public static void combine(List<ItemStack> itemStacks, List<ItemStack> newItemStacks) {
		for (ItemStack newItemStack : newItemStacks) {
			if (isNullOrAir(newItemStack))
				continue;

			Optional<ItemStack> matching = itemStacks.stream()
					.filter(existing -> existing.isSimilar(newItemStack) && existing.getAmount() < existing.getType().getMaxStackSize())
					.findFirst();

			if (matching.isPresent()) {
				ItemStack match = matching.get();
				itemStacks.remove(match);
				int amountICanAdd = Math.min(newItemStack.getAmount(), match.getType().getMaxStackSize() - match.getAmount());
				match.setAmount(match.getAmount() + amountICanAdd);
				itemStacks.add(new ItemStack(match));

				newItemStack.setAmount(newItemStack.getAmount() - amountICanAdd);
			}

			if (newItemStack.getAmount() > 0)
				itemStacks.add(new ItemStack(newItemStack));
		}
	}

	public static List<ItemStack> clone(Collection<ItemStack> list) {
		return new ArrayList<>() {{
			for (ItemStack item : list)
				add(item.clone());
		}};
	}

	public static List<ItemStack> getShulkerContents(ItemStack itemStack) {
		return getRawShulkerContents(itemStack).stream().filter(content -> !ItemUtils.isNullOrAir(content)).collect(Collectors.toList());
	}

	public static List<ItemStack> getRawShulkerContents(ItemStack itemStack) {
		List<ItemStack> contents = new ArrayList<>();

		if (ItemUtils.isNullOrAir(itemStack))
			return contents;

		if (!MaterialTag.SHULKER_BOXES.isTagged(itemStack.getType()))
			return contents;
		
		if (!(itemStack.getItemMeta() instanceof BlockStateMeta meta))
			return contents;

		if (!(meta.getBlockState() instanceof ShulkerBox shulkerBox))
			return contents;

		contents.addAll(Arrays.asList(shulkerBox.getInventory().getContents()));

		return contents;
	}

	public static ItemStack getTool(HasPlayer player, EquipmentSlot hand) {
		return player.getPlayer().getInventory().getItem(hand);
	}

	public static ItemStack getTool(HasPlayer player) {
		return getTool(player, (Material) null);
	}

	public static ItemStack getTool(HasPlayer player, Material material) {
		Player _player = player.getPlayer();
		ItemStack mainHand = _player.getInventory().getItemInMainHand();
		ItemStack offHand = _player.getInventory().getItemInOffHand();
		if (!isNullOrAir(mainHand) && (material == null || mainHand.getType() == material))
			return mainHand;
		else if (!isNullOrAir(offHand) && (material == null || offHand.getType() == material))
			return offHand;
		return null;
	}

	public static ItemStack getToolRequired(HasPlayer player) {
		ItemStack item = getTool(player);
		if (isNullOrAir(item))
			throw new InvalidInputException("You are not holding anything");
		return item;
	}

	public static EquipmentSlot getHandWithTool(HasPlayer player) {
		return getHandWithTool(player, null);
	}

	public static EquipmentSlot getHandWithTool(HasPlayer player, Material material) {
		Player _player = player.getPlayer();
		ItemStack mainHand = _player.getInventory().getItemInMainHand();
		ItemStack offHand = _player.getInventory().getItemInOffHand();
		if (!isNullOrAir(mainHand) && (material == null || mainHand.getType() == material))
			return EquipmentSlot.HAND;
		else if (!isNullOrAir(offHand) && (material == null || offHand.getType() == material))
			return EquipmentSlot.OFF_HAND;
		return null;
	}

	public static EquipmentSlot getHandWithToolRequired(HasPlayer player) {
		EquipmentSlot hand = getHandWithTool(player);
		if (hand == null)
			throw new InvalidInputException("You are not holding anything");
		return hand;
	}

	/**
	 * Tests if an item is not null or {@link MaterialTag#ALL_AIR air}
	 * @param itemStack item
	 * @return if item is not null or air
	 */
	// useful for streams
	@Contract("null -> false; !null -> _")
	public static boolean isNotNullOrAir(ItemStack itemStack) {
		return !isNullOrAir(itemStack);
	}

	/**
	 * Tests if an item is not null or {@link MaterialTag#ALL_AIR air}
	 * @param material item
	 * @return if item is not null or air
	 */
	// useful for streams
	@Contract("null -> false; !null -> _")
	public static boolean isNotNullOrAir(Material material) {
		return !isNullOrAir(material);
	}

	/**
	 * Tests if an item is null or {@link MaterialTag#ALL_AIR air}
	 * @param itemStack item
	 * @return if item is null or air
	 */
	@Contract("null -> true; !null -> _")
	public static boolean isNullOrAir(ItemStack itemStack) {
		return itemStack == null || MaterialTag.ALL_AIR.isTagged(itemStack);
	}

	/**
	 * Tests if an item is null or {@link MaterialTag#ALL_AIR air}
	 * @param material item
	 * @return if item is null or air
	 */
	@Contract("null -> true; !null -> _")
	public static boolean isNullOrAir(Material material) {
		return material == null || MaterialTag.ALL_AIR.isTagged(material);
	}

	public static boolean isInventoryEmpty(Inventory inventory) {
		for (ItemStack itemStack : inventory.getContents())
			if (!isNullOrAir(itemStack))
				return false;
		return true;
	}

	public static @Nullable UUID getSkullOwner(ItemStack skull) {
		if (!skull.getType().equals(Material.PLAYER_HEAD))
			return null;

		ItemMeta itemMeta = skull.getItemMeta();
		SkullMeta skullMeta = (SkullMeta) itemMeta;


		if (skullMeta.getPlayerProfile() == null)
			return null;

		if (skullMeta.getPlayerProfile().getId() == null)
			return null;

		return skullMeta.getPlayerProfile().getId();
	}

	public static ItemStack setDurability(ItemStack item, int percentage) {
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof Damageable damageable) {
			double maxDurability = item.getType().getMaxDurability();
			double damage = (percentage / 100.0) * maxDurability;
			damageable.setDamage((int) damage);

			item.setItemMeta((ItemMeta) damageable);
		}

		return item;
	}

	public static List<Enchantment> getApplicableEnchantments(ItemStack item) {
		List<Enchantment> applicable = new ArrayList<>();
		for (Enchantment enchantment : Enchantment.values()) {
			try {
				item = new ItemStack(item.getType());
				item.addEnchantment(enchantment, 1);
				applicable.add(enchantment); // if it gets here it hasnt errored, so its valid
			} catch (Exception ex) { /* Not applicable, do nothing */ }
		}
		return applicable;
	}

	public static String getName(ItemStack result) {
		if (result.getItemMeta().hasDisplayName())
			return result.getItemMeta().getDisplayName();

		return result.getType().name();
	}

	public static boolean isSimilar(ItemStack item1, ItemStack item2) {
		if (isNullOrAir(item1) || isNullOrAir(item2))
			return false;

		if (item1.getType() != item2.getType())
			return false;

		if (!MaterialTag.SHULKER_BOXES.isTagged(item1.getType()))
			return item1.isSimilar(item2);

		List<ItemStack> contents1 = getRawShulkerContents(item1);
		List<ItemStack> contents2 = getRawShulkerContents(item2);
		if (contents1.isEmpty() && contents2.isEmpty())
			return true;

		for (int i = 0; i < contents1.size(); i++) {
			if (contents1.get(i) == null && contents2.get(i) == null)
				continue;
			if (contents1.get(i) == null || !contents1.get(i).isSimilar(contents2.get(i)))
				return false;
		}

		return true;
	}

	public static ItemStack getItem(Block block) {
		return block.getDrops().iterator().next();
	}

	public static class ItemStackComparator implements Comparator<ItemStack> {
		@Override
		public int compare(ItemStack a, ItemStack b) {
			int result = Integer.compare(b.getMaxStackSize(), a.getMaxStackSize());
			if (result != 0) return result;

			result = b.getRarity().compareTo(a.getRarity());
			if (result != 0) return result;

			result = b.getType().compareTo(a.getType());
			if (result != 0) return result;

			result = Integer.compare(b.getAmount(), a.getAmount());
			return result;
		}
	}

	public static boolean isSameHead(ItemStack itemStack1, ItemStack itemStack2) {
		if (isNullOrAir(itemStack1) || isNullOrAir(itemStack2)) return false;
		if (itemStack1.getType() != Material.PLAYER_HEAD || itemStack2.getType() != Material.PLAYER_HEAD) return false;
		return Nexus.getHeadAPI().getItemID(itemStack1).equals(Nexus.getHeadAPI().getItemID(itemStack2));
	}

	/**
	 * Get the raw defense points of an armor piece. This (obviously, it's a {@link Material}) does not include enchantments or custom NBT.
	 * @param item armor item
	 * @return defense value in half-bars
	 */
	public static int getDefensePoints(Material item) {
		// there might be a bukkit method for this somewhere but i haven't found it and i don't wanna do NMS
		switch (item) {
			// i think this looks better as an old switch and will make better diffs
			case LEATHER_HELMET:
			case LEATHER_BOOTS:
			case CHAINMAIL_BOOTS:
			case GOLDEN_BOOTS:
				return 1;
			case LEATHER_LEGGINGS:
			case CHAINMAIL_HELMET:
			case IRON_HELMET:
			case IRON_BOOTS:
			case GOLDEN_HELMET:
			case TURTLE_HELMET:
				return 2;
			case LEATHER_CHESTPLATE:
			case DIAMOND_HELMET:
			case DIAMOND_BOOTS:
			case GOLDEN_LEGGINGS:
			case NETHERITE_HELMET:
			case NETHERITE_BOOTS:
				return 3;
			case CHAINMAIL_LEGGINGS:
				return 4;
			case CHAINMAIL_CHESTPLATE:
			case IRON_LEGGINGS:
			case GOLDEN_CHESTPLATE:
				return 5;
			case IRON_CHESTPLATE:
			case DIAMOND_LEGGINGS:
			case NETHERITE_LEGGINGS:
				return 6;
			case DIAMOND_CHESTPLATE:
			case NETHERITE_CHESTPLATE:
				return 8;
			default:
				return 0;
		}
	}

	/**
	 * Get the raw armor toughness points of an armor piece. This (obviously, it's a {@link Material}) does not include enchantments or custom NBT.
	 * @param item armor item
	 * @return armor toughness value
	 */
	public static int getArmorToughness(Material item) {
		// there might be a bukkit method for this somewhere but i haven't found it and i don't wanna do NMS
		switch (item) {
			// i think this looks better as an old switch and will make better diffs
			case DIAMOND_HELMET:
			case DIAMOND_CHESTPLATE:
			case DIAMOND_LEGGINGS:
			case DIAMOND_BOOTS:
				return 2;
			case NETHERITE_HELMET:
			case NETHERITE_CHESTPLATE:
			case NETHERITE_LEGGINGS:
			case NETHERITE_BOOTS:
				return 3;
			default:
				return 0;
		}
	}

	/**
	 * Get the raw knockback resistance of an armor piece. This (obviously, it's a {@link Material}) does not include enchantments or custom NBT.
	 * @param item armor item
	 * @return knockback resistance value
	 */
	public static int getKnockbackResistance(Material item) {
		// there might be a bukkit method for this somewhere but i haven't found it and i don't wanna do NMS
		switch (item) {
			// i think this looks better as an old switch and will make better diffs
			case NETHERITE_HELMET:
			case NETHERITE_CHESTPLATE:
			case NETHERITE_LEGGINGS:
			case NETHERITE_BOOTS:
				return 1;
			default:
				return 0;
		}
	}

	/**
	 * Gets the equipment slot for an armor item
	 * @param item armor item
	 * @return equipment slot, or null if non-armor-item
	 */
	@Nullable
	public static EquipmentSlot getArmorEquipmentSlot(Material item) {
		switch (item) {
			case CHAINMAIL_HELMET:
			case DIAMOND_HELMET:
			case GOLDEN_HELMET:
			case IRON_HELMET:
			case LEATHER_HELMET:
			case NETHERITE_HELMET:
			case TURTLE_HELMET:
			case ZOMBIE_HEAD:
			case SKELETON_SKULL:
			case WITHER_SKELETON_SKULL:
			case CREEPER_HEAD:
			case PLAYER_HEAD:
			case CARVED_PUMPKIN:
				return EquipmentSlot.HEAD;
			case DIAMOND_CHESTPLATE:
			case CHAINMAIL_CHESTPLATE:
			case GOLDEN_CHESTPLATE:
			case IRON_CHESTPLATE:
			case LEATHER_CHESTPLATE:
			case NETHERITE_CHESTPLATE:
				return EquipmentSlot.CHEST;
			case CHAINMAIL_LEGGINGS:
			case DIAMOND_LEGGINGS:
			case GOLDEN_LEGGINGS:
			case IRON_LEGGINGS:
			case LEATHER_LEGGINGS:
			case NETHERITE_LEGGINGS:
				return EquipmentSlot.LEGS;
			case CHAINMAIL_BOOTS:
			case DIAMOND_BOOTS:
			case GOLDEN_BOOTS:
			case IRON_BOOTS:
			case LEATHER_BOOTS:
			case NETHERITE_BOOTS:
				return EquipmentSlot.FEET;
			default:
				return null;
		}
	}
}
