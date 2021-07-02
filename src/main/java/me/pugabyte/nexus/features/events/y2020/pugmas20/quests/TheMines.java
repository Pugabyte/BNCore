package me.pugabyte.nexus.features.events.y2020.pugmas20.quests;

import eden.utils.TimeUtils.Time;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.Merchants.MerchantNPC;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.pugmas20.Pugmas20User;
import me.pugabyte.nexus.models.pugmas20.Pugmas20UserService;
import me.pugabyte.nexus.models.task.Task;
import me.pugabyte.nexus.models.task.TaskService;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.LocationUtils.CardinalDirection;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.MerchantBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SerializationUtils.JSON;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils.ActionGroup;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.pugabyte.nexus.features.commands.staff.WorldGuardEditCommand.canWorldGuardEdit;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.addTokenMax;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isAtPugmas;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.questItem;
import static me.pugabyte.nexus.utils.ItemUtils.isFuzzyMatch;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.utils.StringUtils.colorize;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@NoArgsConstructor
public class TheMines implements Listener {

	static {
		addTokenMax("themines_" + OreType.COAL.name(), 3);
		addTokenMax("themines_" + OreType.IRON.name(), 3);
		addTokenMax("themines_" + OreType.LUMINITE.name(), 3);
		addTokenMax("themines_" + OreType.MITHRIL.name(), 3);
		addTokenMax("themines_" + OreType.ADAMANTITE.name(), 3);
		addTokenMax("themines_" + OreType.NECRITE.name(), 3);
		addTokenMax("themines_" + OreType.LIGHT_ANIMICA.name(), 3);
	}

	@EventHandler
	public void onClickSellCrate(PlayerInteractEvent event) {
		if (!EquipmentSlot.HAND.equals(event.getHand())) return;

		Player player = event.getPlayer();
		if (!Pugmas20.isAtPugmas(player)) return;

		Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;

		Material type = block.getType();
		String crateType = null;
		if (MaterialTag.SIGNS.isTagged(type)) {
			crateType = getCrateType(block);
		} else {
			for (CardinalDirection direction : CardinalDirection.values()) {
				Block relative = block.getRelative(direction.toBlockFace());
				if (MaterialTag.SIGNS.isTagged(relative.getType())) {
					crateType = getCrateType(relative);
					if (crateType != null)
						break;
				}
			}
		}

		if (crateType == null) return;

		Pugmas20UserService service = new Pugmas20UserService();
		Pugmas20User user = service.get(player);
		if (QuestStage.COMPLETE.equals(user.getMinesStage())) return;

		event.setCancelled(true);

		Inventory inv = Bukkit.createInventory(null, 27, colorize("&eSell Crate - " + crateType));
		player.openInventory(inv);
	}

	private String getCrateType(Block block) {
		Sign sign = (Sign) block.getState();
		String line1 = sign.getLine(0);
		String line2 = sign.getLine(1);
		if (stripColor(line1).equals("[Sell Crate]") && stripColor(line2).contains("Ingots"))
			return line2;
		return null;
	}

	@EventHandler
	public void onSellCrateClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		String title = stripColor(event.getView().getTitle());
		if (!title.contains(stripColor("Sell Crate - Ingots"))) return;

		List<MerchantBuilder.TradeBuilder> tradeBuilders = MerchantNPC.THEMINES_SELLCRATE.getTrades(null);

		if (tradeBuilders == null || tradeBuilders.size() == 0) {
			PlayerUtils.giveItems((Player) event.getPlayer(), Arrays.asList(event.getInventory().getContents()));
			return;
		}

		int profit = 0;
		OreType key;
		for (ItemStack item : event.getInventory().getContents()) {
			if (ItemUtils.isNullOrAir(item)) {
				continue;
			}

			boolean foundTrade = false;
			boolean leftovers = false;
			for (MerchantBuilder.TradeBuilder tradeBuilder : tradeBuilders) {
				ItemStack result = tradeBuilder.getResult();
				List<ItemStack> ingredients = tradeBuilder.getIngredients();
				if (ingredients.size() != 1) continue;
				ItemStack ingredient = ingredients.get(0);
				if (ItemUtils.isNullOrAir(ingredient)) continue;
				if (ItemUtils.isNullOrAir(result)) continue;

				key = OreType.ofIngot(ingredient.getType());
				if (key == null) continue;

				Material type = item.getType();
				if (type.equals(ingredient.getType())) {
					if (item.getAmount() >= ingredient.getAmount()) {
						double loops = Math.ceil((item.getAmount() + 0D) / ingredient.getAmount());
						for (double i = 0; i < loops; i++) {
							int itemAmount = item.getAmount();
							int ingredientAmount = ingredient.getAmount();
							if (itemAmount < ingredientAmount) {
								leftovers = true;
								break;
							}

							item.setAmount(ingredientAmount);
							if (item.equals(ingredient)) {
								foundTrade = true;

								int testAmt = profit + result.getAmount();
								int excess = Pugmas20.checkDailyTokens(player, "themines_" + key.name(), testAmt);
								if (excess <= 0) {
									itemAmount -= ingredientAmount;
									profit += result.getAmount();
								}

								item.setAmount(itemAmount);
							}
						}
					}

					Pugmas20.giveDailyTokens(player, "themines_" + key.name(), profit);
					profit = 0;
				}
			}

			if (!foundTrade || leftovers || item.getAmount() > 0)
				PlayerUtils.giveItem(player, item);
		}

		EventUserService service = new EventUserService();
		EventUser user = service.get(player);
		user.sendMessage(Pugmas20.PREFIX + "New event token balance: " + user.getTokens());
	}

	public static String taskId = "pugmas-ore-regen";

	@Getter
	private static final ItemStack minersPickaxe = questItem(Material.IRON_PICKAXE).name("Miner's Pickaxe").enchant(Enchantment.DIG_SPEED, 4).build();
	@Getter
	private static final ItemStack minersSieve = questItem(Material.HOPPER).name("Miner's Sieve").build();
	@Getter
	private static final ItemStack flint = questItem(Material.FLINT).build();

	private static final int orePerCoal = 2;

	@EventHandler
	public void onOreBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (!isAtPugmas(player, "cave"))
			return;

		if (canWorldGuardEdit(player))
			return;

		event.setCancelled(true);

		Block block = event.getBlock();
		Material material = block.getType();
		OreType oreType = OreType.ofOre(material);
		if (oreType == null)
			return;

		if (!isFuzzyMatch(minersPickaxe, player.getInventory().getItemInMainHand()))
			return;

		player.getInventory().getItemInMainHand().addEnchantment(Enchantment.DIG_SPEED, 4);

		new SoundBuilder(Sound.BLOCK_STONE_BREAK).location(player.getLocation()).category(SoundCategory.BLOCKS).play();
		ItemStack itemStack = oreType == OreType.COAL ? oreType.getIngot(RandomUtils.randomElement(1, 1, 2, 2, 2, 3)) : oreType.getOre();
		PlayerUtils.giveItem(player, itemStack);

		scheduleRegen(block);
		block.setType(Material.STONE);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!isAtPugmas(player, "cave"))
			return;

		if (!minersSieve.equals(player.getInventory().getItemInMainHand()))
			return;

		if (!ActionGroup.CLICK_BLOCK.applies(event) || event.getClickedBlock() == null)
			return;

		event.setCancelled(true);

		if (Action.LEFT_CLICK_BLOCK != event.getAction())
			return;

		Block block = event.getClickedBlock();
		if (block.getType() != Material.GRAVEL)
			return;

		new SoundBuilder(Sound.ENTITY_HORSE_SADDLE).receiver(player).volume(.5F).pitch(.5F).play();
		new SoundBuilder(Sound.UI_STONECUTTER_TAKE_RESULT).receiver(player).volume(.5F).pitch(.5F).play();
		Tasks.wait(5, () -> {
			new SoundBuilder(Sound.ENTITY_HORSE_SADDLE).receiver(player).volume(.5F).pitch(.5F).play();
			new SoundBuilder(Sound.UI_STONECUTTER_TAKE_RESULT).receiver(player).volume(.5F).pitch(.5F).play();
		});

		if (RandomUtils.chanceOf(20))
			PlayerUtils.giveItem(event.getPlayer(), flint);

		scheduleRegen(block);
		block.setType(Material.LIGHT_GRAY_CONCRETE_POWDER);
	}

	public void scheduleRegen(Block block) {
		new TaskService().save(new Task(taskId, new HashMap<>() {{
			put("location", JSON.serializeLocation(block.getLocation()));
			put("material", block.getType());
		}}, LocalDateTime.now().plusSeconds(RandomUtils.randomInt(3 * 60, 5 * 60))));
	}

	@EventHandler
	public void onSmelt(FurnaceSmeltEvent event) {
		if (!isAtPugmas(event.getBlock().getLocation()))
			return;

		OreType oreType = OreType.ofOre(event.getSource().getType());
		if (oreType == null)
			return;

		if (isFuzzyMatch(event.getSource(), oreType.getOre()))
			event.setResult(oreType.getIngot());
	}

	@EventHandler
	public void onBurn(FurnaceBurnEvent event) {
		if (!isAtPugmas(event.getBlock().getLocation()))
			return;

		if (!(event.getBlock().getState() instanceof BlastFurnace state))
			return;

		if (state.getCookSpeedMultiplier() != 5) {
			state.setCookSpeedMultiplier(5);
			state.update();
		}

		if (isNullOrAir(event.getFuel()))
			return;

		if (!isFuzzyMatch(event.getFuel(), OreType.COAL.getIngot())) {
			state.setCookTimeTotal(0);
			state.update();
			event.setCancelled(true);
			return;
		}

		ItemStack smelting = state.getInventory().getSmelting();
		if (isNullOrAir(smelting)) {
			event.setCancelled(true);
			return;
		}

		OreType oreType = OreType.ofOre(smelting.getType());
		if (oreType == null || !isFuzzyMatch(oreType.getOre(), smelting)) {
			event.setCancelled(true);
			return;
		}

		ItemStack fuel = state.getInventory().getFuel();

		if (!isNullOrAir(fuel)) {
			fuel.setAmount(fuel.getAmount() - 1);
			state.getInventory().setFuel(fuel);
		}

		event.setBurnTime((int) (event.getBurnTime() / ((8 / orePerCoal) * state.getCookSpeedMultiplier())));
	}

	static {
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

	public enum OreType {
		LIGHT_ANIMICA(Material.DIAMOND_ORE, Material.DIAMOND),
		NECRITE(Material.EMERALD_ORE, Material.EMERALD),
		ADAMANTITE(Material.REDSTONE_ORE, Material.REDSTONE),
		MITHRIL(Material.LAPIS_ORE, Material.LAPIS_LAZULI),
		IRON(Material.IRON_ORE, Material.IRON_INGOT),
		LUMINITE(Material.GOLD_ORE, Material.GOLD_INGOT),
		COAL(Material.COAL_ORE, Material.CHARCOAL);

		@Getter
		private final ItemStack ore;
		@Getter
		private final ItemStack ingot;

		OreType(Material ore, Material ingot) {
			this.ore = questItem(ore).name(camelCase(name() + " Ore")).build();
			this.ingot = questItem(ingot).name(camelCase(name())).build();
		}

		public static OreType ofOre(Material ore) {
			for (OreType oreType : OreType.values())
				if (oreType.getOre().getType() == ore)
					return oreType;
			return null;
		}

		public static OreType ofIngot(Material ingot) {
			for (OreType oreType : OreType.values())
				if (oreType.getIngot().getType() == ingot)
					return oreType;
			return null;
		}

		public ItemStack getOre(int amount) {
			return new ItemBuilder(ore).lore(ingot.getLore()).amount(amount).build();
		}

		public ItemStack getIngot(int amount) {
			return new ItemBuilder(ingot).lore(ingot.getLore()).amount(amount).build();
		}
	}

}
