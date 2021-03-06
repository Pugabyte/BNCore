package me.pugabyte.nexus.features.customenchants.enchants;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.customenchants.CustomEnchant;
import me.pugabyte.nexus.utils.Enchant;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MagnetEnchant extends CustomEnchant {

	public MagnetEnchant(@NotNull NamespacedKey key) {
		super(key);
	}

	private static final int RADIUS_MULTIPLIER = 5;

	static {
		Tasks.repeat(Time.TICK, Time.TICK, () -> {
			for (Player player : PlayerUtils.getOnlinePlayers()) {
				if (WorldGroup.of(player) != WorldGroup.SURVIVAL)
					continue;

				int maxLevel = getMaxLevel(player);
				if (maxLevel == 0)
					continue;

				int radius = maxLevel * RADIUS_MULTIPLIER;

				for (Entity entity : getDroppedItems(player, radius)) {
					final Vector vector = getVector(player, entity);
					if (vector == null)
						continue;

					entity.setVelocity(vector);
				}
			}
		});
	}

	@Nullable
	private static Vector getVector(Player player, Entity entity) {
		final double distance = player.getLocation().distance(entity.getLocation());
		if (distance < 1)
			return null;

		final Vector subtract = player.getLocation().toVector().subtract(entity.getLocation().toVector());
		final Vector normalized = subtract.normalize();
		final double multiplier = .6 - distance / 10d;
		return normalized.multiply(Math.max(.1, multiplier));
	}

	private static List<Entity> getDroppedItems(Player player, int radius) {
		return new ArrayList<>() {{
			for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
				if (entity.getType() != EntityType.DROPPED_ITEM)
					continue;

				// TODO Metadata check

				add(entity);
			}
		}};
	}

	private static int getMaxLevel(Player player) {
		int maxLevel = 0;
		for (ItemStack item : getItems(player.getInventory()))
			maxLevel = Math.max(maxLevel, item.getItemMeta().getEnchantLevel(Enchant.MAGNET));

		return Math.min(Enchant.MAGNET.getMaxLevel(), maxLevel);
	}

	@NotNull
	private static List<ItemStack> getItems(PlayerInventory inventory) {
		List<ItemStack> items = new ArrayList<>() {{
			addAll(Arrays.asList(inventory.getArmorContents()));
			add(inventory.getItemInMainHand());
			add(inventory.getItemInOffHand());
		}};

		items.removeIf(ItemUtils::isNullOrAir);
		return items;
	}

}
