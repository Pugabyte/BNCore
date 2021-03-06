package me.pugabyte.nexus.features.customenchants;

import io.papermc.paper.enchantments.EnchantmentRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static eden.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.utils.StringUtils.toRoman;

public abstract class CustomEnchant extends Enchantment {

	public CustomEnchant(@NotNull NamespacedKey key) {
		super(key);
	}

	@Override
	public @NotNull String getName() {
		return getKey().getKey();
	}

	@Override
	public @NotNull Component displayName(int level) {
		return Component.text(getDisplayName(level));
	}

	@NotNull
	public String getDisplayName(int level) {
		return camelCase(getName()) + (level > 1 ? " " + toRoman(level) : "");
	}

	public int getLevel(ItemStack item) {
		int level = 0;

		if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
			if (meta.hasStoredEnchant(this))
				level = meta.getStoredEnchantLevel(this);
		} else {
			if (item.getItemMeta().hasEnchant(this))
				level = item.getEnchantmentLevel(this);
		}

		return level;
	}

	@Override
	public int getStartLevel() {
		return 1;
	}

	@Override
	public int getMaxLevel() {
		return 255;
	}

	@Override
	public boolean conflictsWith(@NotNull Enchantment enchantment) {
		return false;
	}

	@Override
	public boolean canEnchantItem(@NotNull ItemStack itemStack) {
		return false;
	}

	@Override
	public @NotNull EnchantmentRarity getRarity() {
		return EnchantmentRarity.VERY_RARE;
	}

	@Override
	public @NotNull EnchantmentTarget getItemTarget() {
		return EnchantmentTarget.ALL;
	}

	@Override
	public float getDamageIncrease(int i, @NotNull EntityCategory entityCategory) {
		return 0;
	}

	@Override
	public @NotNull Set<EquipmentSlot> getActiveSlots() {
		return Set.of(EquipmentSlot.values());
	}

	@Override
	public boolean isTradeable() {
		return false;
	}

	@Override
	public boolean isDiscoverable() {
		return false;
	}

	@Override
	public boolean isCursed() {
		return false;
	}

	@Override
	public boolean isTreasure() {
		return false;
	}

}
