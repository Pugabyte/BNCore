package me.pugabyte.nexus.utils;

import lombok.Getter;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static me.pugabyte.nexus.utils.Utils.collect;
import static org.bukkit.Material.*;

@SuppressWarnings("unused")
public class MaterialTag implements Tag<Material> {
	public static final MaterialTag ALL_AIR = new MaterialTag("AIR", MatchMode.SUFFIX);
	public static final MaterialTag WOOL = new MaterialTag("_WOOL", MatchMode.SUFFIX);
	public static final MaterialTag DYES = new MaterialTag("_DYE", MatchMode.SUFFIX);
	public static final MaterialTag BEDS = new MaterialTag("_BED", MatchMode.SUFFIX);
	public static final MaterialTag ALL_BANNERS = new MaterialTag(Tag.BANNERS);
	public static final MaterialTag BANNERS = new MaterialTag("_BANNER", MatchMode.SUFFIX);
	public static final MaterialTag WALL_BANNERS = new MaterialTag("_WALL_BANNER", MatchMode.SUFFIX);
	public static final MaterialTag STAINED_GLASS = new MaterialTag("_STAINED_GLASS", MatchMode.SUFFIX);
	public static final MaterialTag STAINED_GLASS_PANES = new MaterialTag("_STAINED_GLASS_PANE", MatchMode.SUFFIX);
	public static final MaterialTag ALL_STAINED_GLASS = new MaterialTag("STAINED_GLASS", MatchMode.CONTAINS);
	public static final MaterialTag GLAZED_TERRACOTTAS = new MaterialTag("_GLAZED_TERRACOTTA", MatchMode.SUFFIX);
	public static final MaterialTag COLORED_TERRACOTTAS = new MaterialTag("_TERRACOTTA", MatchMode.SUFFIX).exclude(GLAZED_TERRACOTTAS);
	public static final MaterialTag ALL_TERRACOTTAS = new MaterialTag("TERRACOTTA", MatchMode.CONTAINS);
	public static final MaterialTag CONCRETES = new MaterialTag("_CONCRETE", MatchMode.SUFFIX);
	public static final MaterialTag CONCRETE_POWDERS = new MaterialTag("_CONCRETE_POWDER", MatchMode.SUFFIX);
	public static final MaterialTag ALL_CONCRETES = new MaterialTag("CONCRETE", MatchMode.CONTAINS);
	public static final MaterialTag SANDSTONES = new MaterialTag("SANDSTONE", MatchMode.CONTAINS);
	public static final MaterialTag PRISMARINE = new MaterialTag("PRISMARINE", MatchMode.CONTAINS).exclude(PRISMARINE_CRYSTALS, PRISMARINE_SHARD);
	public static final MaterialTag PURPURS = new MaterialTag("PURPUR", MatchMode.CONTAINS);
	public static final MaterialTag END_STONES = new MaterialTag("END_STONE", MatchMode.CONTAINS);
	public static final MaterialTag SHULKER_BOXES = new MaterialTag("_SHULKER_BOX", MatchMode.SUFFIX).append(SHULKER_BOX);
	public static final MaterialTag BOOKS = new MaterialTag("BOOK", MatchMode.CONTAINS);
	public static final MaterialTag ALL_QUARTZ = new MaterialTag("QUARTZ", MatchMode.CONTAINS).exclude(QUARTZ, NETHER_QUARTZ_ORE);
	public static final MaterialTag ALL_GLASS = new MaterialTag("GLASS", MatchMode.CONTAINS);


	public static final MaterialTag COLORABLE = new MaterialTag(WOOL, DYES, CARPETS, BEDS, ALL_BANNERS,
			ALL_STAINED_GLASS, ALL_TERRACOTTAS, ALL_CONCRETES, SHULKER_BOXES);

	public static final MaterialTag FOODS = new MaterialTag(Material::isEdible);

	public static final MaterialTag SWORDS = new MaterialTag("_SWORD", MatchMode.SUFFIX);
	public static final MaterialTag PICKAXES = new MaterialTag("_PICKAXE", MatchMode.SUFFIX);
	public static final MaterialTag AXES = new MaterialTag("_AXE", MatchMode.SUFFIX);
	public static final MaterialTag SHOVELS = new MaterialTag("_SHOVEL", MatchMode.SUFFIX);
	public static final MaterialTag HOES = new MaterialTag("_HOE", MatchMode.SUFFIX);

	public static final MaterialTag TOOLS = new MaterialTag(PICKAXES, AXES, SHOVELS, HOES)
			.append(FISHING_ROD, LEAD, SHEARS, FLINT_AND_STEEL);

	public static final MaterialTag WEAPONS = new MaterialTag("_SWORD", MatchMode.SUFFIX)
			.append(BOW, CROSSBOW, TRIDENT).append("ARROW", MatchMode.SUFFIX);

	public static final MaterialTag ARMOR = new MaterialTag("_HELMET", MatchMode.SUFFIX)
			.append("_CHESTPLATE", MatchMode.SUFFIX).append("_LEGGINGS", MatchMode.SUFFIX).append("_BOOTS", MatchMode.SUFFIX);

	public static final MaterialTag TOOLS_WEAPONS_ARMOR = new MaterialTag(TOOLS, WEAPONS, ARMOR);

	public static final MaterialTag ARMOR_DIAMOND = new MaterialTag("DIAMOND_", MatchMode.PREFIX, MaterialTag.ARMOR);
	public static final MaterialTag ARMOR_IRON = new MaterialTag("IRON_", MatchMode.PREFIX, MaterialTag.ARMOR);
	public static final MaterialTag ARMOR_GOLD = new MaterialTag("GOLDEN_", MatchMode.PREFIX, MaterialTag.ARMOR);
	public static final MaterialTag ARMOR_CHAINMAIL = new MaterialTag("CHAINMAIL_", MatchMode.PREFIX, MaterialTag.ARMOR);
	public static final MaterialTag ARMOR_LEATHER = new MaterialTag("LEATHER_", MatchMode.PREFIX, MaterialTag.ARMOR);

	public static final MaterialTag TOOLS_NETHERITE = new MaterialTag("NETHERITE_", MatchMode.PREFIX, MaterialTag.TOOLS);
	public static final MaterialTag TOOLS_DIAMOND = new MaterialTag("DIAMOND_", MatchMode.PREFIX, MaterialTag.TOOLS);
	public static final MaterialTag TOOLS_IRON = new MaterialTag("IRON_", MatchMode.PREFIX, MaterialTag.TOOLS);
	public static final MaterialTag TOOLS_GOLD = new MaterialTag("GOLDEN_", MatchMode.PREFIX, MaterialTag.TOOLS);
	public static final MaterialTag TOOLS_CHAINMAIL = new MaterialTag("CHAINMAIL_", MatchMode.PREFIX, MaterialTag.TOOLS);
	public static final MaterialTag TOOLS_LEATHER = new MaterialTag("LEATHER_", MatchMode.PREFIX, MaterialTag.TOOLS);

	public static final MaterialTag CHESTS = new MaterialTag(CHEST, TRAPPED_CHEST, BARREL);
	public static final MaterialTag FURNACES = new MaterialTag(FURNACE, BLAST_FURNACE, SMOKER);
	public static final MaterialTag INVENTORY_BLOCKS = new MaterialTag(HOPPER, DISPENSER, DROPPER)
			.append(CHESTS)
			.append(FURNACES)
			.append(SHULKER_BOXES)
			.append(LECTERN);
	public static final MaterialTag MENU_BLOCKS = new MaterialTag(Material.CRAFTING_TABLE, Material.GRINDSTONE,
			Material.ENCHANTING_TABLE, Material.STONECUTTER, Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.BELL,
			Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.BEACON);

	public static final MaterialTag COMMAND_BLOCKS = new MaterialTag("COMMAND_BLOCK", MatchMode.CONTAINS);
	public static final MaterialTag MINECARTS = new MaterialTag("_MINECART", MatchMode.SUFFIX);

	public static final MaterialTag UNOBTAINABLE = new MaterialTag(WATER, LAVA, AIR,
			STRUCTURE_BLOCK, STRUCTURE_VOID, JIGSAW, BARRIER, BEDROCK,
			COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK, COMMAND_BLOCK_MINECART,
			END_PORTAL, END_PORTAL_FRAME, NETHER_PORTAL, KNOWLEDGE_BOOK,
			DEBUG_STICK, SPAWNER, CHORUS_PLANT);

	public static final MaterialTag POTIONS = new MaterialTag(POTION, SPLASH_POTION, LINGERING_POTION);

	public static final MaterialTag POTION_MATERIALS = new MaterialTag(POTIONS)
			.append(GLASS_BOTTLE, FERMENTED_SPIDER_EYE, BLAZE_POWDER, MAGMA_CREAM, GLISTERING_MELON_SLICE, GOLDEN_CARROT,
					RABBIT_FOOT, DRAGON_BREATH, PHANTOM_MEMBRANE, GHAST_TEAR, BREWING_STAND, CAULDRON, CARROT, SLIME_BALL,
					QUARTZ, RED_MUSHROOM, APPLE, ROTTEN_FLESH, BROWN_MUSHROOM, INK_SAC, FERN, POISONOUS_POTATO, GOLDEN_APPLE);

	public static final MaterialTag REQUIRES_META = new MaterialTag(POTIONS).append(TIPPED_ARROW, WRITTEN_BOOK, ENCHANTED_BOOK);

	public static final MaterialTag CORAL_BLOCKS = new MaterialTag("CORAL_BLOCK", MatchMode.CONTAINS);
	public static final MaterialTag CORAL_WALL_FANS = new MaterialTag("_WALL_FAN", MatchMode.SUFFIX);
	public static final MaterialTag ALL_CORALS = new MaterialTag("CORAL", MatchMode.CONTAINS);

	public static final MaterialTag SEEDS = new MaterialTag(COCOA_BEANS, WHEAT_SEEDS, POTATO, CARROT, BEETROOT_SEEDS, PUMPKIN_SEEDS, MELON_SEEDS);
	public static final MaterialTag PLANTS = new MaterialTag(GRASS, FERN, TALL_GRASS, LARGE_FERN, DEAD_BUSH, SWEET_BERRY_BUSH,
			BROWN_MUSHROOM, RED_MUSHROOM, LILY_PAD, BAMBOO_SAPLING, BAMBOO, SEAGRASS, TALL_SEAGRASS, KELP, KELP_PLANT, SUGAR_CANE,
			CACTUS, SEA_PICKLE, CHORUS_PLANT, CHORUS_FLOWER, WEEPING_VINES, TWISTING_VINES, NETHER_SPROUTS, WARPED_ROOTS, CRIMSON_ROOTS,
			WARPED_FUNGUS, CRIMSON_FUNGUS, VINE)
			.append(ALL_CORALS, FLOWERS);

	public static final MaterialTag TULIPS = new MaterialTag("_TULIP", MatchMode.SUFFIX);

	public static final MaterialTag ALL_FLOWERS = new MaterialTag(SMALL_FLOWERS, TALL_FLOWERS);

	public static final MaterialTag ALL_BEEHIVES = new MaterialTag(BEEHIVE, BEE_NEST);

	public static final MaterialTag ALL_STONE = new MaterialTag(STONE, STONE_STAIRS, STONE_SLAB, Material.STONE_BRICKS, SMOOTH_STONE, SMOOTH_STONE_SLAB)
		.append(new MaterialTag("STONE_BRICK", MatchMode.PREFIX))
		.append(new MaterialTag("_STONE_BRICKS", MatchMode.SUFFIX)).exclude(END_STONE_BRICKS)
		.append(new MaterialTag("COBBLESTONE", MatchMode.CONTAINS))
		.append(new MaterialTag("GRANITE", MatchMode.CONTAINS))
		.append(new MaterialTag("DIORITE", MatchMode.CONTAINS))
		.append(new MaterialTag("ANDESITE", MatchMode.CONTAINS));

	public static final MaterialTag MINERAL_ORES = new MaterialTag("_ORE", MatchMode.CONTAINS);
	public static final MaterialTag MINERAL_NUGGETS = new MaterialTag(GOLD_NUGGET, IRON_NUGGET, NETHERITE_SCRAP, ANCIENT_DEBRIS);
	public static final MaterialTag MINERAL_INGOTS = new MaterialTag(COAL, CHARCOAL, LAPIS_LAZULI, REDSTONE, QUARTZ,
			GOLD_INGOT, IRON_INGOT, DIAMOND, EMERALD, NETHERITE_INGOT);
	public static final MaterialTag MINERAL_BLOCKS = new MaterialTag(COAL_BLOCK, LAPIS_BLOCK, REDSTONE_BLOCK, GOLD_BLOCK,
			IRON_BLOCK, DIAMOND_BLOCK, EMERALD_BLOCK, NETHERITE_BLOCK)
			.append(ALL_QUARTZ);
	public static final MaterialTag ALL_MINERALS = new MaterialTag(MINERAL_ORES, MINERAL_NUGGETS, MINERAL_INGOTS, MINERAL_BLOCKS);

	public static final MaterialTag ALL_DIRT = new MaterialTag(Tag.DIRT).append(DIRT_PATH, FARMLAND);
	public static final MaterialTag ALL_SAND = new MaterialTag(Material.SAND, RED_SAND);

	public static final MaterialTag VILLAGER_WORKBLOCKS = new MaterialTag(BLAST_FURNACE, SMOKER,
			CARTOGRAPHY_TABLE, BREWING_STAND, COMPOSTER, BARREL, FLETCHING_TABLE,
			CAULDRON, LECTERN, STONECUTTER, LOOM, SMITHING_TABLE, GRINDSTONE);

	public static final MaterialTag TREE_LOGS = new MaterialTag(OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, WARPED_STEM, CRIMSON_STEM);
	public static final MaterialTag STRIPPED_LOGS = new MaterialTag(STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG, STRIPPED_JUNGLE_LOG, STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG, STRIPPED_WARPED_STEM, STRIPPED_CRIMSON_STEM);
	public static final MaterialTag LOGS = new MaterialTag(TREE_LOGS, STRIPPED_LOGS);

	public static final MaterialTag TREE_WOOD = new MaterialTag(OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD, WARPED_HYPHAE, CRIMSON_HYPHAE);
	public static final MaterialTag STRIPPED_WOOD = new MaterialTag(STRIPPED_OAK_WOOD, STRIPPED_SPRUCE_WOOD, STRIPPED_BIRCH_WOOD, STRIPPED_JUNGLE_WOOD, STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD, STRIPPED_WARPED_HYPHAE, STRIPPED_CRIMSON_HYPHAE);
	public static final MaterialTag WOOD = new MaterialTag(TREE_WOOD, STRIPPED_WOOD);

	public static final MaterialTag WOOD_STAIRS = new MaterialTag(OAK_STAIRS, SPRUCE_STAIRS, BIRCH_STAIRS, JUNGLE_STAIRS, ACACIA_STAIRS, DARK_OAK_STAIRS, WARPED_STAIRS, CRIMSON_STAIRS);
	public static final MaterialTag WOOD_SLABS = new MaterialTag(OAK_SLAB, SPRUCE_SLAB, BIRCH_SLAB, JUNGLE_SLAB, ACACIA_SLAB, DARK_OAK_SLAB, PETRIFIED_OAK_SLAB, WARPED_SLAB, CRIMSON_SLAB);

	public static final MaterialTag WOOD_BUTTONS = new MaterialTag(OAK_BUTTON, SPRUCE_BUTTON, BIRCH_BUTTON, JUNGLE_BUTTON, ACACIA_BUTTON, DARK_OAK_BUTTON, WARPED_BUTTON, CRIMSON_BUTTON);
	public static final MaterialTag WOOD_DOORS = new MaterialTag(OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR, WARPED_DOOR, CRIMSON_DOOR);
	public static final MaterialTag WOOD_TRAPDOORS = new MaterialTag(OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR, ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, WARPED_TRAPDOOR, CRIMSON_TRAPDOOR);
	public static final MaterialTag WOOD_FENCES = new MaterialTag(OAK_FENCE, SPRUCE_FENCE, BIRCH_FENCE, JUNGLE_FENCE, ACACIA_FENCE, DARK_OAK_FENCE, WARPED_FENCE, CRIMSON_FENCE);
	public static final MaterialTag WOOD_FENCE_GATES = new MaterialTag(OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE, JUNGLE_FENCE_GATE, ACACIA_FENCE_GATE, DARK_OAK_FENCE_GATE, WARPED_FENCE_GATE, CRIMSON_FENCE_GATE);
	public static final MaterialTag WOOD_PRESSURE_PLATES = new MaterialTag(OAK_PRESSURE_PLATE, SPRUCE_PRESSURE_PLATE, BIRCH_PRESSURE_PLATE, JUNGLE_PRESSURE_PLATE, ACACIA_PRESSURE_PLATE, DARK_OAK_PRESSURE_PLATE, WARPED_PRESSURE_PLATE, CRIMSON_PRESSURE_PLATE);
	public static final MaterialTag WOOD_SIGNS = new MaterialTag(OAK_SIGN, SPRUCE_SIGN, BIRCH_SIGN, JUNGLE_SIGN, ACACIA_SIGN, DARK_OAK_SIGN, WARPED_SIGN, CRIMSON_SIGN);

	public static final MaterialTag ALL_WOOD = new MaterialTag(LOGS, WOOD, PLANKS, WOOD_STAIRS, WOOD_SLABS, WOOD_BUTTONS,
			WOOD_DOORS, WOOD_TRAPDOORS, WOOD_FENCES, WOOD_FENCE_GATES, WOOD_PRESSURE_PLATES, WOOD_SIGNS);

	public static final MaterialTag ALL_SIGNS = new MaterialTag(SIGNS, WALL_SIGNS);

	public static final MaterialTag ALL_NETHER = new MaterialTag(GLOWSTONE, OBSIDIAN, CRYING_OBSIDIAN, GRAVEL, MAGMA_BLOCK, MAGMA_CREAM,
			WITHER_SKELETON_SKULL, BLAZE_ROD, BLAZE_POWDER, WEEPING_VINES, TWISTING_VINES, MUSIC_DISC_PIGSTEP)
			.append("NETHER", MatchMode.CONTAINS)
			.append("NYLIUM", MatchMode.CONTAINS)
			.append("WARPED", MatchMode.CONTAINS)
			.append("CRIMSON", MatchMode.CONTAINS)
			.append("BLACKSTONE", MatchMode.CONTAINS)
			.append("SOUL", MatchMode.CONTAINS)
			.append(GOLDEN_SWORD, GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS)
			.append(NETHER_QUARTZ_ORE, QUARTZ, NETHER_GOLD_ORE, GOLD_NUGGET, GOLD_INGOT, GOLD_BLOCK).append(ALL_QUARTZ);

	public static final MaterialTag ALL_END = new MaterialTag("END", MatchMode.CONTAINS)
			.append("PURPUR", MatchMode.CONTAINS)
			.append("CHORUS", MatchMode.CONTAINS)
			.append(SHULKER_BOXES)
			.append(SHULKER_SHELL, ELYTRA, DRAGON_EGG, DRAGON_HEAD, OBSIDIAN, MAGENTA_STAINED_GLASS);

	public static final MaterialTag RAW_FISH = new MaterialTag(SALMON, COD, TROPICAL_FISH, PUFFERFISH);
	public static final MaterialTag COOKED_FISH = new MaterialTag(COOKED_SALMON, COOKED_COD);

	public static final MaterialTag ALL_FISH = new MaterialTag(RAW_FISH, COOKED_FISH);

	public static final MaterialTag FISH_BUCKETS = new MaterialTag(TROPICAL_FISH_BUCKET, PUFFERFISH_BUCKET);

	public static final MaterialTag ALL_OCEAN = new MaterialTag(SOUL_SAND, MAGMA_BLOCK, TURTLE_EGG, CONDUIT, SCUTE,
			PUFFERFISH_BUCKET, SALMON_BUCKET, COD_BUCKET, TROPICAL_FISH_BUCKET, NAUTILUS_SHELL, COD, SALMON, PUFFERFISH,
			TROPICAL_FISH, COOKED_COD, COOKED_SALMON, LILY_PAD, TURTLE_HELMET, FISHING_ROD, INK_SAC, GRAVEL, Material.SAND)
			.append("PRISMARINE", MatchMode.CONTAINS)
			.append("KELP", MatchMode.CONTAINS)
			.append("SEA", MatchMode.CONTAINS)
			.append("BOAT", MatchMode.CONTAINS)
			.append(ALL_CORALS, ALL_FISH);

	public static final MaterialTag MUSIC = new MaterialTag("DISC", MatchMode.CONTAINS).append(NOTE_BLOCK, JUKEBOX, BELL);

	public static final MaterialTag BLOCKS = new MaterialTag(Material::isSolid)
			.exclude(CACTUS, BAMBOO, DRAGON_EGG, TURTLE_EGG, CONDUIT, CAKE)
			.exclude(SIGNS, BANNERS, ALL_CORALS)
			.append(CORAL_BLOCKS);

	public static final MaterialTag SKULLS = new MaterialTag("_SKULL", MatchMode.SUFFIX).append("_HEAD", MatchMode.SUFFIX).exclude(PISTON_HEAD);
	public static final MaterialTag PLAYER_SKULLS = new MaterialTag(PLAYER_HEAD, PLAYER_WALL_HEAD);
	public static final MaterialTag BOATS = new MaterialTag(Tag.ITEMS_BOATS);
	public static final MaterialTag SAPLINGS = new MaterialTag(Tag.SAPLINGS);
	public static final MaterialTag SPAWN_EGGS = new MaterialTag("_SPAWN_EGG", MatchMode.SUFFIX);
	public static final MaterialTag PORTALS = new MaterialTag(END_PORTAL, NETHER_PORTAL);
	public static final MaterialTag LIQUIDS = new MaterialTag(WATER, LAVA);
	public static final MaterialTag CONTAINERS = new MaterialTag(ENDER_CHEST, Material.ANVIL, BREWING_STAND).append(INVENTORY_BLOCKS);
	public static final MaterialTag PRESSURE_PLATES = new MaterialTag("_PRESSURE_PLATE", MatchMode.SUFFIX);
	public static final MaterialTag TORCHES = new MaterialTag("TORCH", MatchMode.CONTAINS);
	public static final MaterialTag CAMPFIRES = new MaterialTag("CAMPFIRE", MatchMode.CONTAINS);
	public static final MaterialTag FLORA = new MaterialTag(SAPLINGS, FLOWERS, PLANTS, SEEDS, LEAVES);

	public static final MaterialTag NEEDS_SUPPORT = new MaterialTag(Material.GRAVEL, VINE, LILY_PAD, TURTLE_EGG,
			REPEATER, COMPARATOR, ITEM_FRAME, BELL, Material.SNOW, SCAFFOLDING, TRIPWIRE_HOOK, LADDER, LEVER, SOUL_LANTERN)
			.append(SAPLINGS, DOORS, SIGNS, RAILS, BANNERS, CONCRETE_POWDERS, SAND, CORALS, CARPETS,
					PRESSURE_PLATES, BUTTONS, FLOWER_POTS, ANVIL, PLANTS, TORCHES);

	public static final MaterialTag WEARABLE = new MaterialTag(ARMOR, SKULLS).append(CARVED_PUMPKIN).exclude("_WALL_", MatchMode.CONTAINS);

	public static final MaterialTag INTERACTABLES = new MaterialTag(BEDS, SHULKER_BOXES, CONTAINERS, WOOD_FENCE_GATES,
			DOORS, TRAPDOORS, BUTTONS, MENU_BLOCKS).append(
			Material.REPEATER, Material.COMPARATOR, Material.NOTE_BLOCK, Material.JUKEBOX, Material.DAYLIGHT_DETECTOR, Material.LEVER);

	@SneakyThrows
	public static Map<String, Tag<Material>> getApplicable(Material material) {
		return collect(tags.entrySet().stream().filter(entry -> entry.getValue().isTagged(material)));
	}

	@Getter
	private static final Map<String, Tag<Material>> tags = new HashMap<>() {{
		List<Field> fields = new ArrayList<>() {{
			addAll(Arrays.asList(MaterialTag.class.getFields()));
			addAll(Arrays.asList(Tag.class.getFields()));
		}};

		for (Field field : fields) {
			try {
				field.setAccessible(true);
				if (field.getType() == Tag.class || field.getType() == MaterialTag.class) {
					Tag<Material> materialTag = (Tag<Material>) field.get(null);

					try {
						Method isTaggedMethod = materialTag.getClass().getMethod("isTagged", Material.class);
						put(field.getName(), materialTag);
					} catch (NoSuchMethodException ignore) {
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}};

	static {
		for (Field field : MaterialTag.class.getFields()) {
			try {
				field.setAccessible(true);
				if (field.getType() == MaterialTag.class) {
					MaterialTag materialTag = (MaterialTag) field.get(null);

					try {
						Method isTaggedMethod = materialTag.getClass().getMethod("isTagged", Material.class);
						materialTag.key = new NamespacedKey(Nexus.getInstance(), field.getName());
					} catch (NoSuchMethodException ignore) {}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private final EnumSet<Material> materials;
	private NamespacedKey key = null;

	public MaterialTag() {
		this.materials = EnumSet.noneOf(Material.class);
	}

	public MaterialTag(NamespacedKey key) {
		this.materials = EnumSet.noneOf(Material.class);
		this.key = key;
	}

	public MaterialTag(EnumSet<Material> materials) {
		this.materials = materials.clone();
	}

	@SafeVarargs
	public MaterialTag(Tag<Material>... materialTags) {
		this.materials = EnumSet.noneOf(Material.class);
		append(materialTags);
	}

	public MaterialTag(Material... materials) {
		this.materials = EnumSet.noneOf(Material.class);
		append(materials);
	}

	public MaterialTag(String segment, MatchMode mode) {
		this.materials = EnumSet.noneOf(Material.class);
		append(segment, mode);
	}

	public MaterialTag(String segment, MatchMode mode, MaterialTag materials) {
		this.materials = EnumSet.noneOf(Material.class);
		append(segment, mode, materials.getValues().toArray(Material[]::new));
	}

	public MaterialTag(Predicate<Material> predicate) {
		this.materials = EnumSet.noneOf(Material.class);
		append(predicate);
	}

	@Override
	public NamespacedKey getKey() {
		return key;
	}

	public MaterialTag append(Material... materials) {
		this.materials.addAll(Arrays.asList(materials));
		return this;
	}

	@SafeVarargs
	public final MaterialTag append(Tag<Material>... materialTags) {
		for (Tag<Material> materialTag : materialTags)
			this.materials.addAll(materialTag.getValues());

		return this;
	}

	public MaterialTag append(Predicate<Material> predicate) {
		for (Material m : Material.values())
			if (predicate.test(m))
				this.materials.add(m);
		return this;
	}

	public MaterialTag append(String segment, MatchMode mode) {
		append(segment, mode, Material.values());
		return this;
	}

	public MaterialTag append(String segment, MatchMode mode, Material[] materials) {
		segment = segment.toUpperCase();

		switch (mode) {
			case PREFIX:
				for (Material m : materials)
					if (m.name().startsWith(segment))
						this.materials.add(m);
				break;

			case SUFFIX:
				for (Material m : materials)
					if (m.name().endsWith(segment))
						this.materials.add(m);
				break;

			case CONTAINS:
				for (Material m : materials)
					if (m.name().contains(segment))
						this.materials.add(m);
				break;
		}

		return this;
	}

	public MaterialTag exclude(Material... materials) {
		for (Material m : materials)
			this.materials.remove(m);

		return this;
	}

	@SafeVarargs
	public final MaterialTag exclude(Tag<Material>... materialTags) {
		for (Tag<Material> materialTag : materialTags)
			this.materials.removeAll(materialTag.getValues());

		return this;
	}

	public MaterialTag exclude(Predicate<Material> predicate) {
		materials.removeIf(predicate);
		return this;
	}

	public MaterialTag exclude(String segment, MatchMode mode) {
		exclude(segment, mode, Material.values());
		return this;
	}

	public MaterialTag exclude(String segment, MatchMode mode, Material[] materials) {
		segment = segment.toUpperCase();

		switch (mode) {
			case PREFIX:
				for (Material m : materials)
					if (m.name().startsWith(segment))
						this.materials.remove(m);
				break;

			case SUFFIX:
				for (Material m : materials)
					if (m.name().endsWith(segment))
						this.materials.remove(m);
				break;

			case CONTAINS:
				for (Material m : materials)
					if (m.name().contains(segment))
						this.materials.remove(m);
				break;
		}

		return this;
	}

	@Override
	public Set<Material> getValues() {
		return materials;
	}

	public Material[] toArray() {
		return new ArrayList<>(materials).toArray(Material[]::new);
	}

	@Override
	public boolean isTagged(@NotNull Material material) {
		return materials.contains(material);
	}

	public boolean isTagged(@NotNull ItemStack item) {
		return isTagged(item.getType());
	}

	public boolean isTagged(@NotNull Block block) {
		return isTagged(block.getType());
	}

	@Override
	public String toString() {
		return materials.toString();
	}

	public enum MatchMode {
		PREFIX,
		SUFFIX,
		CONTAINS
	}

}
