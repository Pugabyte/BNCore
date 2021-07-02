package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs;

import eden.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.BF21PointSource;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MainIsland;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MinigameNightIsland;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.WoodCutting.BearFair21TreeType;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.farming.FarmingLoot;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.utils.Enchant;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.MerchantBuilder;
import me.pugabyte.nexus.utils.MerchantBuilder.TradeBuilder;
import me.pugabyte.nexus.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;


public class Merchants {

	public static ItemBuilder goldNugget = new ItemBuilder(Material.GOLD_NUGGET);
	public static ItemBuilder goldIngot = new ItemBuilder(Material.GOLD_INGOT);
	public static ItemBuilder goldBlock = new ItemBuilder(Material.GOLD_BLOCK);
	public static ItemBuilder traderCoupon = new ItemBuilder(Material.PAPER).name("&eEvent Token Coupon").lore("&3Amount: &e50");

	public static void openMerchant(Player player, int id) {
		BFMerchant bfMerchant = BFMerchant.getFromId(id);
		if (bfMerchant == null)
			return;

		List<TradeBuilder> trades = bfMerchant.getTrades(player);
		if (Utils.isNullOrEmpty(trades))
			return;

		new MerchantBuilder(StringUtils.camelCase(bfMerchant.getBearFair21NPC().name() + " " + bfMerchant.getBearFair21NPC().getNpcName()))
			.trades(trades)
			.open(player);
	}

	@AllArgsConstructor
	public enum BFMerchant {
		ARTIST(BearFair21NPC.ARTIST) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					for (Material material : MaterialTag.DYES.getValues()) {
						if (material.equals(Material.BLUE_DYE)) {
							add(new TradeBuilder()
								.result(goldNugget.clone().amount(1))
								.ingredient(new ItemBuilder(material).amount(32)));
						} else if (material.equals(Material.GREEN_DYE)) {
							add(new TradeBuilder()
								.result(goldNugget.clone().amount(1))
								.ingredient(new ItemBuilder(material).amount(4)));
						} else {
							add(new TradeBuilder()
								.result(goldNugget.clone().amount(1))
								.ingredient(new ItemBuilder(material).amount(8)));
						}
					}
				}};
			}
		},
		BAKER(BearFair21NPC.BAKER) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.BREAD).amount(16))
						.ingredient(goldNugget.clone().amount(2)));
				}};
			}
		},
		BARTENDER(BearFair21NPC.BARTENDER) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.POTION).potionType(PotionType.POISON, true, false))
						.ingredient(goldNugget.clone().amount(2)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.POTION).potionType(PotionType.WEAKNESS))
						.ingredient(goldNugget.clone().amount(2)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.POTION).potionType(PotionType.SLOWNESS))
						.ingredient(goldNugget.clone().amount(2)));
				}};
			}
		},
		BLACKSMITH(BearFair21NPC.BLACKSMITH) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					if (user.getQuestStage_Lumberjack() == QuestStage.STARTED) {
						add(new TradeBuilder()
							.result(MainIsland.getReplacementSaw().clone())
							.ingredient(new ItemBuilder(Material.IRON_BLOCK).amount(9))
							.ingredient(goldBlock.clone().amount(1)));
					}

					add(new TradeBuilder()
						.result(new ItemBuilder(Material.GUNPOWDER).amount(1))
						.ingredient(goldNugget.clone().amount(4)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(5))
						.ingredient(new ItemBuilder(Material.DIAMOND).amount(2)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(1))
						.ingredient(new ItemBuilder(Material.IRON_INGOT).amount(1)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(1))
						.ingredient(new ItemBuilder(Material.COAL).amount(16)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.STONE_PICKAXE).amount(1))
						.ingredient(goldIngot.clone().amount(1))
						.ingredient(new ItemBuilder(Material.WOODEN_PICKAXE).amount(1)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.STONE_AXE).amount(1))
						.ingredient(goldIngot.clone().amount(1))
						.ingredient(new ItemBuilder(Material.WOODEN_AXE).amount(1)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.NETHERITE_INGOT).amount(1))
						.ingredient(goldBlock.clone().amount(1)));
				}};
			}
		},
		BOTANIST(BearFair21NPC.BOTANIST) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return FarmingLoot.getTrades();
			}
		},
		COLLECTOR(BearFair21NPC.COLLECTOR) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					if (!Utils.isNullOrEmpty(Collector.getRandomTrades()))
						this.addAll(Collector.getRandomTrades());
				}};
			}
		},
		FISHERMAN(BearFair21NPC.FISHERMAN1) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.result(new ItemStack(Material.STRING))
						.ingredient(goldNugget.clone().amount(3)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(1))
						.ingredient(new ItemBuilder(Material.STRING).amount(4)));
				}};
			}
		},
		INVENTOR(BearFair21NPC.INVENTOR) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.maxUses(1)
						.result(Quests.getBackPack(user.getPlayer()))
						.ingredient(goldIngot.clone().amount(6)));
					add(new TradeBuilder()
						.result(new ItemStack(Material.ELYTRA))
						.ingredient(goldBlock.clone().amount(2)));
				}};
			}
		},
		LUMBERJACK(BearFair21NPC.LUMBERJACK) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.WOODEN_AXE).amount(1))
						.ingredient(goldNugget.clone().amount(2)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(1))
						.ingredient(BearFair21TreeType.OAK.getDrop().clone().amount(8)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(1))
						.ingredient(new ItemBuilder(Material.OAK_PLANKS).amount(32))); // oak amount * 4
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(1))
						.ingredient(new ItemBuilder(Material.STICK).amount(64))); // plank amount * 2
				}};
			}
		},
		PASTRY_CHEF(BearFair21NPC.PASTRY_CHEF) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.EGG))
						.ingredient(goldIngot.clone().amount(1)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(5))
						.ingredient(new ItemBuilder(Material.EGG)));

					if (user.getQuestStage_Main() == QuestStage.STEP_FOUR) {
						add(new TradeBuilder()
							.result(MainIsland.getCakeItem().clone())
							.ingredient(new ItemBuilder(Material.CAKE).build())
							.ingredient(new ItemBuilder(Material.COCOA_BEANS).amount(8).build()));
					} else {
						add(new TradeBuilder()
							.result(goldIngot.clone().amount(3))
							.ingredient(new ItemBuilder(Material.CAKE)));
						add(new TradeBuilder()
							.result(goldNugget.clone().amount(2))
							.ingredient(new ItemBuilder(Material.COCOA_BEANS).amount(8)));
					}

					add(new TradeBuilder()
						.result(goldNugget.clone().amount(3))
						.ingredient(new ItemBuilder(Material.COOKIE).amount(64)));
					add(new TradeBuilder()
						.result(goldNugget.clone().amount(3))
						.ingredient(new ItemBuilder(Material.MILK_BUCKET)));
				}};
			}
		},
		SORCERER(BearFair21NPC.SORCERER) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.UNBREAKING, 3))
						.ingredient(goldBlock.clone().amount(3)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.EFFICIENCY, 5))
						.ingredient(goldBlock.clone().amount(2)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.FORTUNE, 3))
						.ingredient(goldBlock.clone().amount(3)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.LURE, 3))
						.ingredient(goldBlock.clone().amount(2)));
					add(new TradeBuilder()
						.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.LUCK_OF_THE_SEA, 3))
						.ingredient(goldBlock.clone().amount(3)));
				}};
			}
		},
		TRADER(BearFair21NPC.TRADER) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					if (BearFair21.getDailyTokensLeft(user.getPlayer(), BF21PointSource.TRADER, 50) <= 0) {
						add(new TradeBuilder()
							.maxUses(1)
							.result(traderCoupon.clone().amount(1))
							.ingredient(goldBlock.clone().amount(1)));
					}
				}};
			}
		},
		JAMES(BearFair21NPC.JAMES) {
			@Override
			public List<TradeBuilder> getTrades(BearFair21User user) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
						.maxUses(1)
						.result(MinigameNightIsland.getCarKey().build())
						.ingredient(goldBlock.clone().amount(1)));
				}};
			}
		},
		;

		@Getter
		private final BearFair21NPC bearFair21NPC;

		public static BFMerchant getFromId(int id) {
			for (BFMerchant merchant : values()) {
				if (merchant.getBearFair21NPC().getId() == id)
					return merchant;
			}

			return null;
		}

		public abstract List<TradeBuilder> getTrades(BearFair21User user);

		public List<TradeBuilder> getTrades(Player player) {
			BearFair21UserService userService = new BearFair21UserService();
			return getTrades(userService.get(player));
		}
	}
}
