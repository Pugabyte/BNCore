package me.pugabyte.nexus.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

@UtilityClass
public class MaterialUtils {
	public final Mineral DIAMOND = new Mineral(Material.DIAMOND);
	public final Mineral NETHERITE = Mineral.builder().mineral(Material.NETHERITE_INGOT).oreName(Material.ANCIENT_DEBRIS).build();
	public final Mineral IRON = new Mineral(Material.IRON_INGOT);
	public final Mineral GOLD = Mineral.builder().mineral(Material.GOLD_INGOT).extraBlock(Material.NETHER_GOLD_ORE).build();
	public final Mineral EMERALD = new Mineral(Material.EMERALD);
	public final Mineral QUARTZ = Mineral.builder().mineral(Material.QUARTZ).oreName(Material.NETHER_QUARTZ_ORE).build();
	public final Mineral COAL = new Mineral(Material.COAL);
	public final Mineral LAPIS = Mineral.builder().mineral(Material.LAPIS_LAZULI).prefix("LAPIS").build();
	public final Mineral REDSTONE = new Mineral(Material.REDSTONE);

	public @Nullable Material oreToIngot(@NotNull Material ore) {
		// TODO: 1.17 copper ingot
		return switch (ore) {
			case DIAMOND_ORE -> Material.DIAMOND;
			case IRON_ORE -> Material.IRON_INGOT;
			case GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
			case EMERALD_ORE -> Material.EMERALD;
			case COAL_ORE -> Material.COAL;
			case LAPIS_ORE -> Material.LAPIS_LAZULI;
			case REDSTONE_ORE -> Material.REDSTONE;
			case NETHER_QUARTZ_ORE -> Material.QUARTZ;
			default -> null;
		};
	}

	public @Nullable Material rawToCooked(@NotNull Material raw) {
		return switch (raw) {
			case PORKCHOP -> Material.COOKED_PORKCHOP;
			case BEEF -> Material.COOKED_BEEF;
			case COD -> Material.COOKED_COD;
			case CHICKEN -> Material.COOKED_CHICKEN;
			case MUTTON -> Material.COOKED_MUTTON;
			case RABBIT -> Material.COOKED_RABBIT;
			case SALMON -> Material.COOKED_SALMON;
			default -> null;
		};
	}

	@Getter
	@AllArgsConstructor
	@RequiredArgsConstructor
	@EqualsAndHashCode
	@Builder
	public static class Mineral {
		private final @NotNull Material mineral;
		private @Nullable String prefix;
		private @Nullable String oreName;
		@Singular
		private final @NotNull Set<Material> extraBlocks;

		public Mineral(@NotNull Material mineral) {
			this(mineral, Collections.emptySet());
		}

		public @NotNull String getPrefix() {
			if (prefix == null)
				prefix = mineral.name().replace("_INGOT", "");
			return prefix;
		}

		public @NotNull String getOreName() {
			if (oreName == null)
				oreName = prefix + "_ORE";
			return oreName;
		}

		public boolean blockIsMineral(@NotNull Material block) {
			String blockName = block.name();
			return getOreName().equals(blockName)
				|| (prefix + "_BLOCK").equals(blockName)
				|| extraBlocks.contains(block);
		}

		private static class MineralBuilder {
			public MineralBuilder oreName(String oreName) {
				this.oreName = oreName;
				return this;
			}

			public MineralBuilder oreName(Material oreBlock) {
				return oreName(oreBlock.name());
			}
		}
	}
}
