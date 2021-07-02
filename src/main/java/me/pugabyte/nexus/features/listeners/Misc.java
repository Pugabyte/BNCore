package me.pugabyte.nexus.features.listeners;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.ClientOption.ChatVisibility;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import eden.utils.TimeUtils.Time;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Koda;
import me.pugabyte.nexus.features.commands.SpeedCommand;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.warps.Warps;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.tip.Tip;
import me.pugabyte.nexus.models.tip.Tip.TipType;
import me.pugabyte.nexus.models.tip.TipService;
import me.pugabyte.nexus.models.warps.WarpService;
import me.pugabyte.nexus.models.warps.WarpType;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.pugabyte.nexus.utils.ItemUtils.getTool;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;

public class Misc implements Listener {

	static {
		for (World world : Bukkit.getWorlds()) {
			// Skip main world
			if (world.equals(Bukkit.getWorlds().get(0)))
				continue;

			world.setKeepSpawnInMemory(false);
		}
	}

	@EventHandler
	public void nbt_onDropItem(PlayerDropItemEvent event) {
		final ItemStack item = event.getItemDrop().getItemStack();
		if (isNullOrAir(item)) return;
		if (!new ItemBuilder(item).isDroppable())
			event.setCancelled(true);
	}

	@EventHandler
	public void nbt_onPlaceBlock(BlockPlaceEvent event) {
		final ItemStack item = event.getItemInHand();
		if (isNullOrAir(item)) return;
		if (!new ItemBuilder(item).isPlaceable())
			event.setCancelled(true);
	}

	@EventHandler
	public void onBeeCatch(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Bee bee))
			return;

		if (event.getHand() != EquipmentSlot.HAND)
			return;

		final Player player = event.getPlayer();
		ItemStack tool = getTool(player);
		if (isNullOrAir(tool))
			return;
		if (!MaterialTag.ALL_BEEHIVES.isTagged(tool.getType()))
			return;

		final BlockStateMeta meta = (BlockStateMeta) tool.getItemMeta();
		final Beehive beehive = (Beehive) meta.getBlockState();
		int max = beehive.getMaxEntities();
		int current = beehive.getEntityCount();

		if (current < max) {
			beehive.addEntity(bee);
			meta.setBlockState(beehive);

			if (tool.getAmount() == 1)
				tool.setItemMeta(meta);
			else {
				tool = ItemBuilder.oneOf(tool).build();
				player.getInventory().removeItem(tool);
				tool.setItemMeta(meta);
				PlayerUtils.giveItem(player, tool);
			}

			new SoundBuilder(Sound.BLOCK_BEEHIVE_ENTER)
				.location(player.getLocation())
				.category(SoundCategory.BLOCKS)
				.play();
		}
	}

	@EventHandler
	public void onCoralDeath(BlockFadeEvent event) {
		Block block = event.getBlock();
		if (MaterialTag.ALL_CORALS.isTagged(block.getType())) {
			WorldGroup worldGroup = WorldGroup.of(block.getWorld());
			if (WorldGroup.CREATIVE.equals(worldGroup) || WorldGroup.ADVENTURE.equals(worldGroup) || WorldGroup.MINIGAMES.equals(worldGroup))
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onHorseLikeDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof AbstractHorse)
			if (event.getCause().equals(EntityDamageEvent.DamageCause.SUFFOCATION))
				event.setCancelled(true);
	}

	@EventHandler
	public void onWanderingTraderSpawn(EntitySpawnEvent event) {
		List<EntityType> types = Arrays.asList(EntityType.WANDERING_TRADER, EntityType.TRADER_LLAMA);
		List<WorldGroup> worlds = Arrays.asList(WorldGroup.SURVIVAL, WorldGroup.SKYBLOCK);
		if (types.contains(event.getEntity().getType()))
			if (!worlds.contains(WorldGroup.of(event.getLocation().getWorld())))
				event.setCancelled(true);
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		if (!(event.getEntity() instanceof Player player)) return;

		if (event.getCause() == DamageCause.VOID)
			if (!(Arrays.asList(WorldGroup.SKYBLOCK, WorldGroup.ONEBLOCK).contains(WorldGroup.of(player))
					|| player.getWorld().getEnvironment() == Environment.THE_END)) {
				if (PlayerManager.get(player).getMatch() != null)
					Warps.spawn((Player) event.getEntity());
			}
	}

	@EventHandler
	public void onPlayerDamageByPlayer(PlayerDamageByPlayerEvent event) {
		if (event.getPlayer().getUniqueId().equals(event.getAttacker().getUniqueId()))
			event.getOriginalEvent().setCancelled(true);
	}

	@EventHandler
	public void onPlaceChest(BlockPlaceEvent event) {
		if (WorldGroup.of(event.getPlayer()) != WorldGroup.SURVIVAL)
			return;

		if (!event.getBlockPlaced().getType().equals(Material.CHEST))
			return;

		Tip tip = new TipService().get(event.getPlayer());
		if (tip.show(TipType.LWC_FURNACE))
			PlayerUtils.send(event.getPlayer(), Koda.getDmFormat() + "Your chest is protected with LWC! Use /lwcinfo to learn more. " +
					"Use &c/trust lock <player> &eto allow someone else to use it.");
	}

	@EventHandler
	public void onPlaceFurnace(BlockPlaceEvent event) {
		if (WorldGroup.of(event.getPlayer()) != WorldGroup.SURVIVAL)
			return;

		if (!event.getBlockPlaced().getType().equals(Material.FURNACE))
			return;

		Tip tip = new TipService().get(event.getPlayer());
		if (tip.show(TipType.LWC_FURNACE))
			PlayerUtils.send(event.getPlayer(), Koda.getDmFormat() + "Your furnace is protected with LWC! Use /lwcinfo to learn more. " +
					"Use &c/trust lock <player> &eto allow someone else to use it.");
	}

	private static final String CHAT_DISABLED_WARNING = "&4&lWARNING: &4You have chat disabled! If this is by mistake, please turn it on in your settings.";
	private static final int WARNING_LENGTH_TICKS = Time.MINUTE.x(1);

	@EventHandler
	public void onJoinWithChatDisabled(PlayerJoinEvent event) {
		Tasks.wait(Time.SECOND.x(3), () -> {
			Player player = event.getPlayer();
			ChatVisibility setting = player.getClientOption(ClientOption.CHAT_VISIBILITY);
			if (Arrays.asList(ChatVisibility.SYSTEM, ChatVisibility.HIDDEN).contains(setting)) {
				PlayerUtils.send(player, "");
				PlayerUtils.send(player, CHAT_DISABLED_WARNING);
				PlayerUtils.send(player, "");
				ActionBarUtils.sendActionBar(player, CHAT_DISABLED_WARNING, WARNING_LENGTH_TICKS);
			}
		});
	}

	@EventHandler
	public void onEnderDragonDeath(EntityDeathEvent event) {
		if (!event.getEntityType().equals(EntityType.ENDER_DRAGON))
			return;

		if (RandomUtils.chanceOf(33))
			event.getDrops().add(new ItemStack(Material.DRAGON_EGG));
	}

	@EventHandler
	public void onPlacePotionLauncherHopper(BlockPlaceEvent event) {
		if (!event.getBlockPlaced().getType().equals(Material.HOPPER))
			return;

		NBTItem itemNBT = new NBTItem(event.getItemInHand());
		if (!itemNBT.hasNBTData())
			return;

		if (itemNBT.asNBTString().contains("&8Potion Launcher"))
			event.setCancelled(true);
	}

	@EventHandler
	public void onBreakEmptyShulkerBox(BlockBreakEvent event) {
		if (!MaterialTag.SHULKER_BOXES.isTagged(event.getBlock().getType()))
			return;

		if (!event.getPlayer().getGameMode().equals(GameMode.CREATIVE))
			return;

		NBTTileEntity tileEntityNBT = new NBTTileEntity(event.getBlock().getState());
		if (!tileEntityNBT.asNBTString().contains("Items:[")) {
			event.setCancelled(true);
			event.getBlock().setType(Material.AIR);
		}
	}

	private static final List<UUID> toSpawn = new ArrayList<>();

	@EventHandler
	public void onConnect(AsyncPlayerPreLoginEvent event) {
		Nerd nerd = Nerd.of(event.getUniqueId());
		World world = nerd.getDimension();
		if (world == null) return;

		if (world.getName().startsWith("resource")) {
			nerd = Nerd.of(event.getUniqueId());
			if (nerd.getLastQuit().isBefore(YearMonth.now().atDay(1).atStartOfDay()))
				toSpawn.add(event.getUniqueId());
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (toSpawn.contains(event.getPlayer().getUniqueId())) {
			new WarpService().get("spawn", WarpType.NORMAL).teleport(event.getPlayer());
			Nexus.log("Teleporting resource world player " + Name.of(event.getPlayer()) + " to spawn");
			toSpawn.remove(event.getPlayer().getUniqueId());
		}

		Tasks.wait(5, () -> {
			if (toSpawn.contains(event.getPlayer().getUniqueId())) {
				new WarpService().get("spawn", WarpType.NORMAL).teleport(event.getPlayer());
				Nexus.log("Teleporting resource world player " + Name.of(event.getPlayer()) + " to spawn [2]");
				toSpawn.remove(event.getPlayer().getUniqueId());
			}

			WorldGroup worldGroup = WorldGroup.of(event.getPlayer());
			if (worldGroup == WorldGroup.MINIGAMES)
				joinMinigames(event.getPlayer());
			else if (worldGroup == WorldGroup.CREATIVE)
				joinCreative(event.getPlayer());
		});
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();

		WorldGroup worldGroup = WorldGroup.of(player);
		if (worldGroup == WorldGroup.MINIGAMES)
			Tasks.wait(5, () -> joinMinigames(player));
		else if (worldGroup == WorldGroup.CREATIVE)
			Tasks.wait(5, () -> joinCreative(player));

		Tasks.wait(10, player::resetPlayerTime);

		if (!Rank.of(player).isStaff()) {
			SpeedCommand.resetSpeed(player);
			player.setAllowFlight(false);
			player.setFlying(false);
		}

		if (WorldGroup.of(event.getFrom()) == WorldGroup.CREATIVE) {
			if (Nerd.of(player).isVanished())
				if (player.hasPermission("essentials.fly")) {
					player.setFallDistance(0);
					player.setAllowFlight(true);
					player.setFlying(true);
				}
		}

		if (event.getFrom().getName().equalsIgnoreCase("donortrial"))
			Tasks.wait(20, () -> {
				PlayerUtils.send(player, "Removing pets, disguises and ptime changes");
				PlayerUtils.runCommandAsConsole("undisguiseplayer " + player.getName());
				PlayerUtils.runCommandAsConsole("petadmin remove " + player.getName());
				PlayerUtils.runCommandAsConsole("mpet remove " + player.getName());
				PlayerUtils.runCommandAsOp(player, "particles stopall");
				PlayerUtils.runCommandAsOp(player, "powder cancel");
				PlayerUtils.runCommandAsConsole("speed walk 1 " + player.getName());
				player.resetPlayerTime();
			});

		if (player.getWorld().getName().equalsIgnoreCase("staff_world"))
			Tasks.wait(20, () -> PlayerUtils.runCommand(player, "cheats off"));
	}

	public void joinMinigames(Player player) {
		PlayerUtils.runCommand(player, "ch join m");
	}

	public void joinCreative(Player player) {
		PlayerUtils.runCommand(player, "ch join c");
	}

	public static class PlayerDamageByPlayerEvent extends PlayerEvent {
		@NonNull
		@Getter
		final Player attacker;
		@NonNull
		@Getter
		final EntityDamageByEntityEvent originalEvent;

		@SneakyThrows
		public PlayerDamageByPlayerEvent(@NotNull Player victim, @NotNull Player attacker, @NotNull EntityDamageByEntityEvent event) {
			super(victim);
			this.attacker = attacker;
			this.originalEvent = event;
		}

		private static final HandlerList handlers = new HandlerList();

		public static HandlerList getHandlerList() {
			return handlers;
		}

		@NotNull
		@Override
		public HandlerList getHandlers() {
			return handlers;
		}
	}

	public static class LivingEntityDamageByPlayerEvent extends EntityEvent {
		@NonNull
		@Getter
		final LivingEntity entity;
		@NonNull
		@Getter
		final Player attacker;
		@NonNull
		@Getter
		final EntityDamageByEntityEvent originalEvent;

		@SneakyThrows
		public LivingEntityDamageByPlayerEvent(@NotNull LivingEntity victim, @NotNull Player attacker, @NotNull EntityDamageByEntityEvent event) {
			super(victim);
			this.entity = victim;
			this.attacker = attacker;
			this.originalEvent = event;
		}

		private static final HandlerList handlers = new HandlerList();

		public static HandlerList getHandlerList() {
			return handlers;
		}

		@NotNull
		@Override
		public HandlerList getHandlers() {
			return handlers;
		}
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageByEntityEvent event) {
		Player attacker = null;
		if (event.getDamager() instanceof Player) {
			attacker = (Player) event.getDamager();
		} else if (event.getDamager() instanceof Projectile projectile) {
			if (projectile.getShooter() instanceof Player)
				attacker = (Player) projectile.getShooter();
		}

		if (attacker == null)
			return;

		if (event.getEntity() instanceof Player player)
			new PlayerDamageByPlayerEvent(player, attacker, event).callEvent();
		else if (event.getEntity() instanceof LivingEntity livingEntity)
			new LivingEntityDamageByPlayerEvent(livingEntity, attacker, event).callEvent();
	}

	// ImageOnMap rotating frames on placement; rotate back one before placement to offset
	@EventHandler
	public void onMapHang(PlayerInteractEntityEvent event) {
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		Entity entity = event.getRightClicked();
		if (!(entity instanceof ItemFrame itemFrame))
			return;

		ItemStack tool = getTool(event.getPlayer());
		if (tool == null)
			return;

		if (tool.getType() != Material.FILLED_MAP)
			return;

		int mapId = ((MapMeta) tool.getItemMeta()).getMapId();
		if (!Paths.get("plugins/ImageOnMap/images/map" + mapId + ".png").toFile().exists())
			return;

		if (!isNullOrAir(itemFrame.getItem()))
			return;

		itemFrame.setRotation(itemFrame.getRotation().rotateCounterClockwise());
	}

	@Getter
	public static class FixedCraftItemEvent extends CraftItemEvent {
		private final ItemStack resultItemStack;

		public FixedCraftItemEvent(@NotNull ItemStack resultItemStack, @NotNull Recipe recipe, @NotNull InventoryView what, @NotNull InventoryType.SlotType type, int slot, @NotNull ClickType click, @NotNull InventoryAction action) {
			super(recipe, what, type, slot, click, action);
			this.resultItemStack = resultItemStack;
		}

		public FixedCraftItemEvent(@NotNull ItemStack resultItemStack, @NotNull Recipe recipe, @NotNull InventoryView what, @NotNull InventoryType.SlotType type, int slot, @NotNull ClickType click, @NotNull InventoryAction action, int key) {
			super(recipe, what, type, slot, click, action, key);
			this.resultItemStack = resultItemStack;
		}
	}

	// Stolen from https://github.com/ezeiger92/QuestWorld2/blob/70f2be317daee06007f89843c79b3b059515d133/src/main/java/com/questworld/extension/builtin/CraftMission.java
	@EventHandler
	public void onCraft(CraftItemEvent event) {
		if (event instanceof FixedCraftItemEvent) return;

		ItemStack item = event.getRecipe().getResult().clone();
		ClickType click = event.getClick();

		int recipeAmount = item.getAmount();

		switch (click) {
			case NUMBER_KEY:
				if (event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) != null)
					recipeAmount = 0;
				break;

			case DROP:
			case CONTROL_DROP:
				ItemStack cursor = event.getCursor();
				if (!ItemUtils.isNullOrAir(cursor))
					recipeAmount = 0;
				break;

			case SHIFT_RIGHT:
			case SHIFT_LEFT:
				if (recipeAmount == 0)
					break;

				int maxCraftable = getMaxCraftAmount(event.getInventory());
				int capacity = fits(item, event.getView().getBottomInventory());

				if (capacity < maxCraftable)
					maxCraftable = ((capacity + recipeAmount - 1) / recipeAmount) * recipeAmount;

				recipeAmount = maxCraftable;
				break;
			default:
		}

		if (recipeAmount == 0)
			return;

		item.setAmount(recipeAmount);

		new FixedCraftItemEvent(item, event.getRecipe(), event.getView(), event.getSlotType(), event.getSlot(), event.getClick(), event.getAction(), event.getHotbarButton()).callEvent();
	}

	public static int getMaxCraftAmount(CraftingInventory inv) {
		if (inv.getResult() == null)
			return 0;

		int resultCount = inv.getResult().getAmount();
		int materialCount = Integer.MAX_VALUE;

		for (ItemStack item : inv.getMatrix())
			// this can in fact be null
			if (item != null && item.getAmount() < materialCount)
				materialCount = item.getAmount();

		return resultCount * materialCount;
	}

	public static int fits(ItemStack stack, Inventory inv) {
		ItemStack[] contents = inv.getContents();
		int result = 0;

		for (ItemStack item : contents)
			if (item == null)
				result += stack.getMaxStackSize();
			else if (item.isSimilar(stack))
				result += Math.max(stack.getMaxStackSize() - item.getAmount(), 0);

		return result;
	}

}
