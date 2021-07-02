package me.pugabyte.nexus.features.listeners;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import me.pugabyte.nexus.features.chat.Censor;
import me.pugabyte.nexus.features.chat.Chat.Broadcast;
import me.pugabyte.nexus.features.chat.Koda;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.pugabyte.nexus.utils.BlockUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.PlayerUtils.getAdvancement;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;

public class Restrictions implements Listener {
	private static final String PREFIX = Koda.getLocalFormat();

	private static final List<WorldGroup> allowedWorldGroups = Arrays.asList(WorldGroup.SURVIVAL, WorldGroup.CREATIVE,
			WorldGroup.SKYBLOCK, WorldGroup.ONEBLOCK);
	private static final List<String> blockedWorlds = Arrays.asList("safepvp", "events");

	public static boolean isPerkAllowedAt(Location location) {
		WorldGroup worldGroup = WorldGroup.of(location);
		if (!allowedWorldGroups.contains(worldGroup))
			return false;

		if (blockedWorlds.contains(location.getWorld().getName()))
			return false;

		WorldGuardUtils worldGuardUtils = new WorldGuardUtils(location);
		if (!worldGuardUtils.getRegionsAt(location).isEmpty())
			return false;

		return true;
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		Player player = event.getPlayer();
		if (Rank.of(player).isStaff())
			return;

		String[] lines = event.getLines();
		boolean censored = false;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (Censor.isCensored(player, line)) {
				event.setLine(i, "");
				censored = true;
			}
		}

		if (!censored)
			return;

		PlayerUtils.send(player, "&cInappropriate sign content");
		String message = "&cSign content by " + Nickname.of(player) + " was censored: &e" + String.join(", ", lines);
		Broadcast.staff().prefix("Censor").message(message).send();
	}

	@EventHandler
	public void onAnvilRenameItem(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		Inventory inventory = event.getClickedInventory();
		if (inventory == null || inventory.getType() != InventoryType.ANVIL)
			return;

		if (event.getSlotType() != SlotType.RESULT)
			return;

		ItemStack item = event.getCurrentItem();

		if (ItemUtils.isNullOrAir(item))
			return;

		ItemMeta meta = item.getItemMeta();

		String input = meta.getDisplayName();
		if (!Censor.isCensored(player, input))
			return;

		meta.setDisplayName(null);
		item.setItemMeta(meta);

		PlayerUtils.send(player, "&cInappropriate item name");
		String message = "&cAnvil name by " + Nickname.of(player) + " was censored: &e" + input;
		Broadcast.staff().prefix("Censor").message(message).send();
	}

	@EventHandler
	public void onPortalEvent(PlayerPortalEvent event) {
		if (Arrays.asList(WorldGroup.ONEBLOCK, WorldGroup.CREATIVE).contains(WorldGroup.of(event.getPlayer())))
			event.setCancelled(true);
	}

	@EventHandler
	public void onEndPortalCreate(PortalCreateEvent event) {
		if (WorldGroup.of(event.getWorld()) != WorldGroup.SURVIVAL)
			event.setCancelled(true);
	}

	@EventHandler
	public void onWitherRoseEffect(EntityPotionEffectEvent event) {
		if (event.getCause() == Cause.WITHER_ROSE)
			if (event.getEntity() instanceof Player)
				event.setCancelled(true);
	}

	@EventHandler
	public void onOneBlockFallingCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (!Arrays.asList(WorldGroup.ONEBLOCK, WorldGroup.SKYBLOCK).contains(WorldGroup.of(player)))
			return;

		if (player.getFallDistance() > 5 && !player.isFlying()) {
			event.setCancelled(true);
			PlayerUtils.send(player, "&cYou cannot run commands while falling (try moving onto a solid block)");
		}
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		event.getPlayer().closeInventory();
		if (event.getFrom().getWorld().getEnvironment() == Environment.THE_END || event.getTo().getWorld().getEnvironment() != Environment.THE_END)
			return;

		Nerd nerd = Nerd.of(event.getPlayer());
		if (nerd.getRank().gte(Rank.TRUSTED))
			return;

		AdvancementProgress progress = event.getPlayer().getAdvancementProgress(getAdvancement("story/follow_ender_eye"));
		if (!progress.isDone()) {
			event.setCancelled(true);
			PlayerUtils.send(event.getPlayer(), "&cYou must enter an end portal before you can enter The End!");
		}
	}

	@EventHandler
	public void onInteractHoldingSpawnEgg(PlayerInteractEvent event) {
		if (ItemUtils.isNullOrAir(event.getItem())) return;
		if (!MaterialTag.SPAWN_EGGS.isTagged(event.getItem().getType())) return;
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
		if (isNullOrAir(event.getClickedBlock())) return;
		if (!event.getClickedBlock().getType().equals(Material.SPAWNER)) return;

		if (!Rank.of(event.getPlayer()).isSeniorStaff())
			event.setCancelled(true);
	}

	// TODO This should be handled by WE but its broken
	@EventHandler
	public void onWorldEditCommand(PlayerCommandPreprocessEvent event) {
		if (event.getPlayer().hasPermission("worldedit.bypass.material"))
			return;

		if (!Arrays.asList(Rank.GUEST, Rank.MEMBER).contains(Nerd.of(event.getPlayer()).getRank()))
			return;

		String command = event.getMessage().toLowerCase();

		if (!command.split(" ")[0].replace("worldedit:", "").startsWith("//"))
			return;

		if (command.split(" ", 2).length < 2)
			return;

		List<Material> used = new ArrayList<>();
		disallowedInWorldEdit.forEach(material -> {
			if (command.split(" ", 2)[1].contains(material.name().toLowerCase()))
				used.add(material);
		});

		if (!used.isEmpty()) {
			event.setCancelled(true);
			PlayerUtils.send(event.getPlayer(), "&cYou cannot use the following materials with WorldEdit:");
			used.forEach(material ->
					PlayerUtils.send(event.getPlayer(), "&7 - &c" + camelCase(material.name())));
		}
	}

	private static final List<Material> disallowedInWorldEdit = Arrays.asList(
			Material.ACACIA_BUTTON,
			Material.ACACIA_PRESSURE_PLATE,
			Material.ACACIA_SAPLING,
			Material.ACACIA_SIGN,
			Material.ACACIA_WALL_SIGN,
			Material.ACACIA_DOOR,
			Material.ACTIVATOR_RAIL,
			Material.ALLIUM,
			Material.ANVIL,
			Material.AZURE_BLUET,
			Material.BAMBOO,
			Material.BEETROOTS,
			Material.BIRCH_BUTTON,
			Material.BIRCH_DOOR,
			Material.BIRCH_PRESSURE_PLATE,
			Material.BIRCH_SAPLING,
			Material.BIRCH_SIGN,
			Material.BIRCH_WALL_SIGN,
			Material.BLACK_BANNER,
			Material.BLACK_CARPET,
			Material.BLACK_CONCRETE_POWDER,
			Material.BLACK_WALL_BANNER,
			Material.BLUE_BANNER,
			Material.BLUE_CARPET,
			Material.BLUE_CONCRETE_POWDER,
			Material.BLUE_ORCHID,
			Material.BLUE_WALL_BANNER,
			Material.BROWN_BANNER,
			Material.BROWN_CARPET,
			Material.BROWN_CONCRETE_POWDER,
			Material.BROWN_MUSHROOM,
			Material.BROWN_WALL_BANNER,
			Material.CACTUS,
			Material.CAKE,
			Material.CARROTS,
			Material.CHEST,
			Material.CHIPPED_ANVIL,
			Material.CHORUS_FLOWER,
			Material.CHORUS_PLANT,
			Material.COCOA,
			Material.COMPARATOR,
			Material.CORNFLOWER,
			Material.CRIMSON_FUNGUS,
			Material.CYAN_BANNER,
			Material.CYAN_CARPET,
			Material.CYAN_CONCRETE_POWDER,
			Material.CYAN_WALL_BANNER,
			Material.DAMAGED_ANVIL,
			Material.DANDELION,
			Material.DARK_OAK_BUTTON,
			Material.DARK_OAK_DOOR,
			Material.DARK_OAK_PRESSURE_PLATE,
			Material.DARK_OAK_SAPLING,
			Material.DARK_OAK_SIGN,
			Material.DARK_OAK_WALL_SIGN,
			Material.DEAD_BUSH,
			Material.DETECTOR_RAIL,
			Material.DRAGON_EGG,
			Material.END_PORTAL,
			Material.FERN,
			Material.FLOWER_POT,
//			Material.GRASS,
			Material.GRAVEL,
			Material.GRAY_BANNER,
			Material.GRAY_CARPET,
			Material.GRAY_CONCRETE_POWDER,
			Material.GRAY_WALL_BANNER,
			Material.GREEN_BANNER,
			Material.GREEN_CARPET,
			Material.GREEN_CONCRETE_POWDER,
			Material.GREEN_WALL_BANNER,
			Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
			Material.IRON_DOOR,
			Material.JUNGLE_BUTTON,
			Material.JUNGLE_DOOR,
			Material.JUNGLE_PRESSURE_PLATE,
			Material.JUNGLE_SAPLING,
			Material.JUNGLE_SIGN,
			Material.JUNGLE_WALL_SIGN,
			Material.KELP,
			Material.LADDER,
			Material.LARGE_FERN,
			Material.LEVER,
			Material.LIGHT_BLUE_BANNER,
			Material.LIGHT_BLUE_CARPET,
			Material.LIGHT_BLUE_CONCRETE_POWDER,
			Material.LIGHT_BLUE_WALL_BANNER,
			Material.LIGHT_GRAY_BANNER,
			Material.LIGHT_GRAY_CARPET,
			Material.LIGHT_GRAY_CONCRETE_POWDER,
			Material.LIGHT_GRAY_WALL_BANNER,
			Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
			Material.LILAC,
			Material.LILY_OF_THE_VALLEY,
			Material.LILY_PAD,
			Material.LIME_BANNER,
			Material.LIME_CARPET,
			Material.LIME_CONCRETE_POWDER,
			Material.LIME_WALL_BANNER,
			Material.MAGENTA_BANNER,
			Material.MAGENTA_CARPET,
			Material.MAGENTA_CONCRETE_POWDER,
			Material.MAGENTA_WALL_BANNER,
			Material.MELON_STEM,
			Material.NETHER_PORTAL,
			Material.NETHER_WART,
			Material.OAK_BUTTON,
			Material.OAK_DOOR,
			Material.OAK_PRESSURE_PLATE,
			Material.OAK_SAPLING,
			Material.OAK_SIGN,
			Material.OAK_WALL_SIGN,
			Material.ORANGE_BANNER,
			Material.ORANGE_CARPET,
			Material.ORANGE_CONCRETE_POWDER,
			Material.ORANGE_TULIP,
			Material.ORANGE_WALL_BANNER,
			Material.OXEYE_DAISY,
			Material.PEONY,
			Material.PINK_BANNER,
			Material.PINK_CARPET,
			Material.PINK_CONCRETE_POWDER,
			Material.PINK_TULIP,
			Material.PINK_WALL_BANNER,
			Material.POPPY,
			Material.POTATOES,
			Material.POWERED_RAIL,
			Material.PUMPKIN_STEM,
			Material.PURPLE_BANNER,
			Material.PURPLE_CARPET,
			Material.PURPLE_CONCRETE_POWDER,
			Material.PURPLE_WALL_BANNER,
			Material.RAIL,
			Material.RED_BANNER,
			Material.RED_CARPET,
			Material.RED_CONCRETE_POWDER,
			Material.RED_MUSHROOM,
//			Material.RED_SAND,
			Material.RED_TULIP,
			Material.RED_WALL_BANNER,
			Material.REDSTONE,
			Material.REDSTONE_WIRE,
			Material.REDSTONE_TORCH,
			Material.REPEATER,
			Material.ROSE_BUSH,
//			Material.SAND,
			Material.SEA_PICKLE,
			Material.SEAGRASS,
			Material.SNOW,
			Material.SPRUCE_BUTTON,
			Material.SPRUCE_DOOR,
			Material.SPRUCE_PRESSURE_PLATE,
			Material.SPRUCE_SAPLING,
			Material.SPRUCE_SIGN,
			Material.SPRUCE_WALL_SIGN,
			Material.STONE_BUTTON,
			Material.STONE_PRESSURE_PLATE,
			Material.SUGAR_CANE,
			Material.SUNFLOWER,
			Material.TALL_GRASS,
			Material.TORCH,
			Material.TRAPPED_CHEST,
			Material.VINE,
			Material.WARPED_FUNGUS,
			Material.WHEAT,
			Material.WHITE_BANNER,
			Material.WHITE_CARPET,
			Material.WHITE_CONCRETE_POWDER,
			Material.WHITE_TULIP,
			Material.WHITE_WALL_BANNER,
			Material.WITHER_ROSE,
			Material.YELLOW_BANNER,
			Material.YELLOW_CARPET,
			Material.YELLOW_CONCRETE_POWDER,
			Material.YELLOW_WALL_BANNER
	);

	@EventHandler(priority = EventPriority.LOWEST)
	public void onVanillaAchievement(PlayerAdvancementCriterionGrantEvent event) {
		if (!WorldGroup.SURVIVAL.contains(event.getPlayer().getWorld()) || event.getPlayer().getGameMode() != GameMode.SURVIVAL)
			event.setCancelled(true);
	}

}
