package me.pugabyte.nexus.features.mobheads;

import eden.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.utils.ColorType;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Cat.Type;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Llama.Color;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.MushroomCow.Variant;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Panda.Gene;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.ItemStack;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;

@Getter
public enum MobHeadType {
	AXOLOTL,
	BAT(EntityType.BAT),
	BEE(EntityType.BEE),
	BLAZE(EntityType.BLAZE),
	CAT(EntityType.CAT, CatType.class, entity -> CatType.of((Cat) entity)),
	CAVE_SPIDER(EntityType.CAVE_SPIDER),
	CHICKEN(EntityType.CHICKEN),
	COD(EntityType.COD),
	COW(EntityType.COW),
	CREEPER(EntityType.CREEPER, CreeperType.class, entity -> CreeperType.of((Creeper) entity)),
	DOLPHIN(EntityType.DOLPHIN),
	DONKEY(EntityType.DONKEY),
	DROWNED(EntityType.DROWNED),
	ELDER_GUARDIAN(EntityType.ELDER_GUARDIAN),
	ENDER_DRAGON(EntityType.ENDER_DRAGON),
	ENDERMAN(EntityType.ENDERMAN),
	ENDERMITE(EntityType.ENDERMITE),
	EVOKER(EntityType.EVOKER),
	FOX(EntityType.FOX, FoxType.class, entity -> FoxType.of((Fox) entity)),
	GHAST(EntityType.GHAST),
	GLOW_SQUID,
	GOAT,
	GUARDIAN(EntityType.GUARDIAN),
	HOGLIN(EntityType.HOGLIN),
	HORSE(EntityType.HORSE, HorseColor.class, entity -> HorseColor.of((Horse) entity)),
	HUSK(EntityType.HUSK),
	ILLUSIONER(EntityType.ILLUSIONER),
	IRON_GOLEM(EntityType.IRON_GOLEM),
	LLAMA(EntityType.LLAMA, LlamaColor.class, entity -> LlamaColor.of((Llama) entity)),
	MAGMA_CUBE(EntityType.MAGMA_CUBE),
	MUSHROOM_COW(EntityType.MUSHROOM_COW, MooshroomType.class, entity -> MooshroomType.of((MushroomCow) entity)),
	MULE(EntityType.MULE),
	OCELOT(EntityType.OCELOT),
	PANDA(EntityType.PANDA, PandaGene.class, entity -> PandaGene.of((Panda) entity)),
	PARROT(EntityType.PARROT, ParrotVariant.class, entity -> ParrotVariant.of((Parrot) entity)),
	PHANTOM(EntityType.PHANTOM),
	PIG(EntityType.PIG),
	PIGLIN(EntityType.PIGLIN),
	PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE),
	PILLAGER(EntityType.PILLAGER),
	PLAYER(EntityType.PLAYER),
	POLAR_BEAR(EntityType.POLAR_BEAR),
	PUFFERFISH(EntityType.PUFFERFISH),
	RABBIT(EntityType.RABBIT, RabbitType.class, entity -> RabbitType.of((Rabbit) entity)),
	RAVAGER(EntityType.RAVAGER),
	SALMON(EntityType.SALMON),
	SHEEP(EntityType.SHEEP, SheepColor.class, entity -> SheepColor.of((Sheep) entity)),
	SHULKER(EntityType.SHULKER),
	SILVERFISH(EntityType.SILVERFISH),
	SKELETON(EntityType.SKELETON),
	SKELETON_HORSE(EntityType.SKELETON_HORSE),
	SLIME(EntityType.SLIME),
	SNOWMAN(EntityType.SNOWMAN, SnowmanType.class, entity -> SnowmanType.of((Snowman) entity)),
	SPIDER(EntityType.SPIDER),
	SQUID(EntityType.SQUID),
	STRAY(EntityType.STRAY),
	STRIDER(EntityType.STRIDER),
	TRADER_LLAMA(EntityType.TRADER_LLAMA, TraderLlamaColor.class, entity -> TraderLlamaColor.of((TraderLlama) entity)),
	TROPICAL_FISH(EntityType.TROPICAL_FISH, TropicalFishType.class, entity -> TropicalFishType.RANDOM),
	TURTLE(EntityType.TURTLE),
	VEX(EntityType.VEX),
	VILLAGER(EntityType.VILLAGER, VillagerProfession.class, entity -> VillagerProfession.of((Villager) entity)),
	VINDICATOR(EntityType.VINDICATOR),
	WANDERING_TRADER(EntityType.WANDERING_TRADER),
	WITCH(EntityType.WITCH),
	WITHER(EntityType.WITHER),
	WITHER_SKELETON(EntityType.WITHER_SKELETON),
	WOLF(EntityType.WOLF),
	ZOGLIN(EntityType.ZOGLIN),
	ZOMBIE(EntityType.ZOMBIE),
	ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE),
	ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER, ZombieVillagerProfession.class, entity -> ZombieVillagerProfession.of((ZombieVillager) entity)),
	ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN),
	;

	private final EntityType type;
	private final Class<? extends MobHeadVariant> variant;
	private final Function<Entity, MobHeadVariant> converter;

	@Setter
	private ItemStack generic;
	@Setter
	private double chance;

	public ItemStack getGeneric() {
		if (generic == null)
			return null;
		return generic.clone();
	}

	@Getter
	private static final Set<ItemStack> allSkulls = new HashSet<>();

	MobHeadType() {
		this(null);
	}

	MobHeadType(EntityType type) {
		this(type, null, null);
	}

	MobHeadType(EntityType type, Class<? extends MobHeadVariant> variant, Function<Entity, MobHeadVariant> getter) {
		this.type = type;
		this.variant = variant;
		this.converter = getter;
	}

	public static MobHeadType of(EntityType from) {
		return Arrays.stream(values()).filter(entry -> from.equals(entry.getType())).findFirst().orElse(null);
	}

	public static ItemStack getSkull(Entity entity) {
		MobHeadType mobHeadType = MobHeadType.of(entity.getType());
		if (mobHeadType == null)
			return null;

		if (mobHeadType.getConverter() == null)
			return mobHeadType.getGeneric();

		return mobHeadType.getConverter().apply(entity).getSkull();
	}

	public ItemStack getSkull(MobHeadVariant variant) {
		return variant == null ? generic : variant.getSkull();
	}

	public interface MobHeadVariant {

		EntityType getEntityType();

		ItemStack getItemStack();

		default ItemStack getSkull() {
			ItemStack skull = getItemStack();
			if (isNullOrAir(skull))
				return MobHeadType.of(getEntityType()).getGeneric();
			return skull;
		}

		void setItemStack(ItemStack itemStack);

		default String getDisplayName() {
			return "&e" + camelCase((Enum<?>) this) + " " + camelCase(getEntityType()) + " Head";
		}

	}

	@Getter
	@RequiredArgsConstructor
	private enum SheepColor implements MobHeadVariant {
		NONE(null),
		RED(ColorType.RED),
		ORANGE(ColorType.ORANGE),
		YELLOW(ColorType.YELLOW),
		LIGHT_GREEN(ColorType.LIGHT_GREEN),
		GREEN(ColorType.GREEN),
		CYAN(ColorType.CYAN),
		LIGHT_BLUE(ColorType.LIGHT_BLUE),
		BLUE(ColorType.BLUE),
		PURPLE(ColorType.PURPLE),
		MAGENTA(ColorType.MAGENTA),
		PINK(ColorType.PINK),
		BROWN(ColorType.BROWN),
		BLACK(ColorType.BLACK),
		GRAY(ColorType.GRAY),
		LIGHT_GRAY(ColorType.LIGHT_GRAY),
		WHITE(ColorType.WHITE),
		;

		private final ColorType type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.SHEEP;
		}

		public static SheepColor of(Sheep sheep) {
			return Arrays.stream(values()).filter(entry -> ColorType.of(sheep.getColor()) == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum HorseColor implements MobHeadVariant {
		NONE(null),
		WHITE(Horse.Color.WHITE),
		CREAMY(Horse.Color.CREAMY),
		CHESTNUT(Horse.Color.CHESTNUT),
		BROWN(Horse.Color.BROWN),
		BLACK(Horse.Color.BLACK),
		GRAY(Horse.Color.GRAY),
		DARK_BROWN(Horse.Color.DARK_BROWN),
		;

		private final Horse.Color type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.HORSE;
		}

		public static HorseColor of(Horse horse) {
			return Arrays.stream(values()).filter(entry -> horse.getColor() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum CatType implements MobHeadVariant {
		NONE(null),
		BLACK(Type.BLACK),
		WHITE(Type.WHITE),
		ALL_BLACK(Type.ALL_BLACK),
		RED(Type.RED),
		BRITISH_SHORTHAIR(Type.BRITISH_SHORTHAIR),
		CALICO(Type.CALICO),
		JELLIE(Type.JELLIE),
		PERSIAN(Type.PERSIAN),
		RAGDOLL(Type.RAGDOLL),
		SIAMESE(Type.SIAMESE),
		TABBY(Type.TABBY),
		;

		@Override
		public EntityType getEntityType() {
			return EntityType.CAT;
		}

		private final Cat.Type type;
		@Setter
		private ItemStack itemStack;

		public static CatType of(Cat cat) {
			return Arrays.stream(values()).filter(entry -> cat.getCatType() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum RabbitType implements MobHeadVariant {
		NONE(null),
		BLACK(Rabbit.Type.BLACK),
		WHITE(Rabbit.Type.WHITE),
		BROWN(Rabbit.Type.BROWN),
		BLACK_AND_WHITE(Rabbit.Type.BLACK_AND_WHITE),
		GOLD(Rabbit.Type.GOLD),
		SALT_AND_PEPPER(Rabbit.Type.SALT_AND_PEPPER),
		;

		private final Rabbit.Type type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.RABBIT;
		}

		public static RabbitType of(Rabbit rabbit) {
			return Arrays.stream(values()).filter(entry -> rabbit.getRabbitType() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum TropicalFishType implements MobHeadVariant {
		RANDOM,
		;

		@Setter
		private List<ItemStack> itemStacks = new ArrayList<>();

		public ItemStack getItemStack() {
			return RandomUtils.randomElement(itemStacks);
		}

		@Override
		public EntityType getEntityType() {
			return EntityType.TROPICAL_FISH;
		}

		public void setItemStack(ItemStack itemStack) {
			itemStacks.add(itemStack);
		}

		@Override
		public String getDisplayName() {
			return "&e" + camelCase(getEntityType()) + " Head";
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum ParrotVariant implements MobHeadVariant {
		NONE(null),
		BLUE(Parrot.Variant.BLUE),
		RED(Parrot.Variant.RED),
		CYAN(Parrot.Variant.CYAN),
		GRAY(Parrot.Variant.GRAY),
		GREEN(Parrot.Variant.GREEN),
		;

		private final Parrot.Variant type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.PARROT;
		}

		public static ParrotVariant of(Parrot parrot) {
			return Arrays.stream(values()).filter(entry -> parrot.getVariant() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum FoxType implements MobHeadVariant {
		NONE(null),
		RED(Fox.Type.RED),
		SNOW(Fox.Type.SNOW),
		;

		private final Fox.Type type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.FOX;
		}

		public static FoxType of(Fox fox) {
			return Arrays.stream(values()).filter(entry -> fox.getFoxType() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum CreeperType implements MobHeadVariant {
		NONE,
		POWERED,
		;

		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.CREEPER;
		}

		public static CreeperType of(Creeper creeper) {
			return creeper.isPowered() ? POWERED : NONE;
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum PandaGene implements MobHeadVariant {
		NONE(null),
		BROWN(Gene.BROWN),
		AGGRESSIVE(Gene.AGGRESSIVE),
		LAZY(Gene.LAZY),
		NORMAL(Gene.NORMAL),
		PLAYFUL(Gene.PLAYFUL),
		WEAK(Gene.WEAK),
		WORRIED(Gene.WORRIED),
		;

		private final Panda.Gene type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.PANDA;
		}

		public static PandaGene of(Panda panda) {
			return Arrays.stream(values()).filter(entry -> panda.getMainGene() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum SnowmanType implements MobHeadVariant {
		NONE,
		DERP,
		;

		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.SNOWMAN;
		}

		public static SnowmanType of(Snowman snowman) {
			return snowman.isDerp() ? DERP : NONE;
		}

	}

	@Getter
	@RequiredArgsConstructor
	private enum VillagerProfession implements MobHeadVariant {
		NONE(null),
		ARMORER(Profession.ARMORER),
		BUTCHER(Profession.BUTCHER),
		CARTOGRAPHER(Profession.CARTOGRAPHER),
		CLERIC(Profession.CLERIC),
		FARMER(Profession.FARMER),
		FISHERMAN(Profession.FISHERMAN),
		FLETCHER(Profession.FLETCHER),
		LEATHERWORKER(Profession.LEATHERWORKER),
		LIBRARIAN(Profession.LIBRARIAN),
		MASON(Profession.MASON),
		NITWIT(Profession.NITWIT),
		SHEPHERD(Profession.SHEPHERD),
		TOOLSMITH(Profession.TOOLSMITH),
		WEAPONSMITH(Profession.WEAPONSMITH),
		;

		private final Villager.Profession type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.VILLAGER;
		}

		private static VillagerProfession of(Villager villager) {
			return Arrays.stream(values()).filter(entry -> villager.getProfession() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum ZombieVillagerProfession implements MobHeadVariant {
		NONE(null),
		ARMORER(Profession.ARMORER),
		BUTCHER(Profession.BUTCHER),
		CARTOGRAPHER(Profession.CARTOGRAPHER),
		CLERIC(Profession.CLERIC),
		FARMER(Profession.FARMER),
		FISHERMAN(Profession.FISHERMAN),
		FLETCHER(Profession.FLETCHER),
		LEATHERWORKER(Profession.LEATHERWORKER),
		LIBRARIAN(Profession.LIBRARIAN),
		MASON(Profession.MASON),
		NITWIT(Profession.NITWIT),
		SHEPHERD(Profession.SHEPHERD),
		TOOLSMITH(Profession.TOOLSMITH),
		WEAPONSMITH(Profession.WEAPONSMITH),
		;

		private final Villager.Profession type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.ZOMBIE_VILLAGER;
		}

		public static ZombieVillagerProfession of(ZombieVillager zombieVillager) {
			return Arrays.stream(values()).filter(entry -> zombieVillager.getVillagerProfession() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum LlamaColor implements MobHeadVariant {
		NONE(null),
		GRAY(Color.GRAY),
		WHITE(Color.WHITE),
		BROWN(Color.BROWN),
		CREAMY(Color.CREAMY),
		;

		private final Llama.Color type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.LLAMA;
		}

		private static LlamaColor of(Llama llama) {
			return Arrays.stream(values()).filter(entry -> llama.getColor() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum TraderLlamaColor implements MobHeadVariant {
		NONE(null),
		GRAY(Color.GRAY),
		WHITE(Color.WHITE),
		BROWN(Color.BROWN),
		CREAMY(Color.CREAMY),
		;

		private final Llama.Color type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.TRADER_LLAMA;
		}

		public static TraderLlamaColor of(TraderLlama traderLlama) {
			return Arrays.stream(values()).filter(entry -> traderLlama.getColor() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	@Getter
	@RequiredArgsConstructor
	private enum MooshroomType implements MobHeadVariant {
		NONE(null),
		RED(Variant.RED),
		BROWN(Variant.BROWN),
		;

		private final MushroomCow.Variant type;
		@Setter
		private ItemStack itemStack;

		@Override
		public EntityType getEntityType() {
			return EntityType.MUSHROOM_COW;
		}

		public static MooshroomType of(MushroomCow mushroomCow) {
			return Arrays.stream(values()).filter(entry -> mushroomCow.getVariant() == entry.getType()).findFirst().orElse(NONE);
		}
	}

	static {
		World world = Bukkit.getWorld("survival");
		WorldGuardUtils WGUtils = new WorldGuardUtils(world);
		WorldEditUtils WEUtils = new WorldEditUtils(world);

		for (Block block : WEUtils.getBlocks(WGUtils.getRegion("mobheads"))) {
			try {
				if (!MaterialTag.SIGNS.isTagged(block.getType()))
					continue;

				Sign sign = (Sign) block.getState();
				Directional directional = (Directional) sign.getBlockData();
				ItemStack skull = block.getRelative(directional.getFacing().getOppositeFace()).getRelative(BlockFace.UP)
						.getDrops().stream().findFirst().orElse(null);
				if (skull == null)
					continue;

				EntityType type;
				String entity = (sign.getLine(0) + sign.getLine(1)).trim();
				try {
					type = EntityType.valueOf(entity);
				} catch (Exception ignored) {
					Nexus.log("Cannot parse entity type: " + entity);
					continue;
				}

				double chance = Double.parseDouble(sign.getLine(3));

				skull = new ItemBuilder(skull).name("&e" + camelCase(type) + " Head").build();
				MobHeadType.of(type).setGeneric(skull);
				MobHeadType.of(type).setChance(chance);
				allSkulls.add(skull);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		Reflections reflections = new Reflections(MobHeadType.class.getPackage().getName());
		for (Class<? extends MobHeadVariant> variant : reflections.getSubTypesOf(MobHeadVariant.class)) {
			for (Block block : WEUtils.getBlocks(WGUtils.getRegion("mobheads_variant_" + variant.getSimpleName().toLowerCase()))) {
				try {
					if (!MaterialTag.SIGNS.isTagged(block.getType()))
						continue;

					Sign sign = (Sign) block.getState();
					Directional directional = (Directional) sign.getBlockData();
					ItemStack skull = block.getRelative(directional.getFacing().getOppositeFace())
							.getRelative(BlockFace.UP)
							.getDrops().stream()
							.findFirst().orElse(null);

					if (skull == null)
						continue;

					MobHeadVariant mobHeadVariant;
					String variantType = (sign.getLine(0) + sign.getLine(1)).trim();
					try {
						mobHeadVariant = EnumUtils.valueOf(variant, variantType);
					} catch (Exception ignored) {
						Nexus.log("Cannot parse entity variant: " + variant.getSimpleName() + "." + variantType);
						continue;
					}

					skull = new ItemBuilder(skull).name(mobHeadVariant.getDisplayName()).build();
					mobHeadVariant.setItemStack(skull);
					allSkulls.add(skull);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
