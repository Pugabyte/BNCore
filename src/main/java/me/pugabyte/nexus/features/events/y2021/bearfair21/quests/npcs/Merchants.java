package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs;

import eden.utils.Utils;
import lombok.Getter;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.farming.FarmingLoot;
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

	public static void openMerchant(Player player, int id) {
		BFMerchant bfMerchant = BFMerchant.getFromId(id);
		if (bfMerchant == null)
			return;

		List<TradeBuilder> trades = bfMerchant.getTrades(player);
		if (Utils.isNullOrEmpty(trades))
			return;

		new MerchantBuilder(StringUtils.camelCase(bfMerchant.getName()))
				.trades(trades)
				.open(player);
	}

	public enum BFMerchant {
		ARTIST("Sage", 2657) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return new ArrayList<>() {{
					for (Material material : MaterialTag.DYES.getValues()) {
						add(new TradeBuilder()
								.result(goldNugget.clone().amount(1))
								.ingredient(new ItemBuilder(material).amount(8)));
					}
				}};
			}
		},
		BAKER("Rye", 2659) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
							.result(goldNugget.clone().amount(2))
							.ingredient(new ItemBuilder(Material.BREAD).amount(64)));
				}};
			}
		},
		BARTENDER("Cosmo", 2655) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.POTION).potionType(PotionType.POISON, true, false))
							.ingredient(goldNugget.clone().amount(3)));
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.POTION).potionType(PotionType.WEAKNESS))
							.ingredient(goldNugget.clone().amount(3)));
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.POTION).potionType(PotionType.SLOWNESS))
							.ingredient(goldNugget.clone().amount(3)));
				}};
			}
		},
		BLACKSMITH("Alvor", 2656) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.GUNPOWDER).amount(1))
							.ingredient(goldNugget.clone().amount(1)));
				}};
			}
		},
		BOTANIST("Fern", 2661) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return FarmingLoot.getTrades();
			}
		},
		BREWER("Charlie", 2662) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return null;
			}
		},
		COLLECTOR("Pluto", 2750) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
							.result(Quests.getBackPack(player))
							.ingredient(goldNugget.clone().amount(1)));
					add(new TradeBuilder()
							.result(new ItemStack(Material.ELYTRA))
							.ingredient(goldNugget.clone().amount(1)));
				}};
			}
		},
		FISHERMAN("Gage", 2653) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return null;
			}
		},
		INVENTOR("Joshua", 2660) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return null;
			}
		},
		PASTRY_CHEF("Maple", 2654) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.EGG))
							.ingredient(goldNugget.clone().amount(5)));
					add(new TradeBuilder()
							.result(goldNugget.clone().amount(2))
							.ingredient(new ItemBuilder(Material.EGG)));
				}};
			}
		},
		SORCERER("Lucian", 2658) {
			@Override
			public List<TradeBuilder> getTrades(Player player) {
				return new ArrayList<>() {{
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.UNBREAKING, 3))
							.ingredient(goldNugget.clone().amount(1)));
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.EFFICIENCY, 5))
							.ingredient(goldNugget.clone().amount(1)));
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.FORTUNE, 3))
							.ingredient(goldNugget.clone().amount(1)));
					add(new TradeBuilder()
							.result(new ItemBuilder(Material.ENCHANTED_BOOK).enchant(Enchant.LURE, 3))
							.ingredient(goldNugget.clone().amount(1)));
				}};
			}
		};

		@Getter
		private final String name;
		@Getter
		private final int npcId;


		BFMerchant(String name, int npcId) {
			this.name = name;
			this.npcId = npcId;
		}

		public static BFMerchant getFromId(int id) {
			for (BFMerchant merchant : values()) {
				if (merchant.getNpcId() == id)
					return merchant;
			}

			return null;
		}

		public abstract List<TradeBuilder> getTrades(Player player);
	}
}
