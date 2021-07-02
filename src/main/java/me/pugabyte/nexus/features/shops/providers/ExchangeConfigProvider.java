package me.pugabyte.nexus.features.shops.providers;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.ItemClickData;
import fr.minuskube.inv.content.InventoryContents;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.shops.Shops;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.models.shop.Shop;
import me.pugabyte.nexus.models.shop.Shop.ExchangeType;
import me.pugabyte.nexus.models.shop.Shop.Product;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static me.pugabyte.nexus.features.menus.SignMenuFactory.ARROWS;
import static me.pugabyte.nexus.features.shops.ShopUtils.prettyMoney;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.utils.StringUtils.pretty;

public class ExchangeConfigProvider extends _ShopProvider {
	private Product product;
	private final AtomicReference<ItemStack> item = new AtomicReference<>();
	private final AtomicReference<ItemStack> priceItem = new AtomicReference<>();
	private double price = -1;
	private ExchangeType exchangeType = ExchangeType.SELL;
	private double stock;
	private boolean allowEditItem = true;

	private final ItemStack less8 = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).amount(8).name("&cDecrease amount by 8").build();
	private final ItemStack less1 = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("&cDecrease amount").build();
	private final ItemStack more1 = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name("&aIncrease amount").build();
	private final ItemStack more8 = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).amount(8).name("&aIncrease amount by 8").build();

	public ExchangeConfigProvider(_ShopProvider previousMenu) {
		this(previousMenu, null);
	}

	public ExchangeConfigProvider(_ShopProvider previousMenu, Product product) {
		this.previousMenu = previousMenu;
		this.product = product;
		if (product != null) {
			allowEditItem = false;
			exchangeType = product.getExchangeType();
			item.set(product.getItem());
			stock = product.getStock();
			if (product.getPrice() instanceof ItemStack)
				priceItem.set((ItemStack) product.getPrice());
			else
				price = (double) product.getPrice();
		}
	}

	@Override
	public void open(Player viewer, int page) {
		open(viewer, page, this, "&0Add Item");
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		super.init(player, contents);

		addItemSelector(player, contents, 1, item, allowEditItem);
		addExchangeControl(player, contents);
		if (exchangeType == ExchangeType.TRADE)
			addItemSelector(player, contents, 3, priceItem);
		else
			addMoneyEditor(player, contents);

		addConfirmButton(player, contents);
	}

	public void addConfirmButton(Player player, InventoryContents contents) {
		contents.set(5, 4, ClickableItem.empty(new ItemBuilder(Material.LIGHT_GRAY_CONCRETE_POWDER)
				.name("&7Configure your exchange before continuing").build()));

		if (item.get() != null) {
			ItemBuilder confirm = new ItemBuilder(Material.LIME_CONCRETE_POWDER);
			if (exchangeType == ExchangeType.TRADE) {
				if (priceItem.get() != null) {
					confirm.name("&3Trade &e" + pretty(item.get())).lore("&3for &e" + pretty(priceItem.get()));
					if (product == null)
						product = new Product(player.getUniqueId(), shopGroup, item.get(), stock, exchangeType, priceItem.get());
					else {
						product.setItem(item.get());
						product.setStock(stock);
						product.setExchangeType(exchangeType);
						product.setPrice(priceItem.get());
					}
				}
			} else
				if (price >= 0) {
					if (exchangeType == ExchangeType.BUY)
						confirm.name("&3Buy &e" + pretty(item.get()) + " &3from").lore("&3customers for &e" + prettyMoney(price));
					else if (exchangeType == ExchangeType.SELL)
						confirm.name("&3Sell &e" + pretty(item.get()) + " &3to").lore("&3customers for &e" + prettyMoney(price));
					if (product == null)
						product = new Product(player.getUniqueId(), shopGroup, item.get(), stock, exchangeType, price);
					else {
						product.setItem(item.get());
						product.setStock(stock);
						product.setExchangeType(exchangeType);
						product.setPrice(price);
					}
				}

			if (product != null)
				contents.set(5, 4, ClickableItem.from(confirm.build(), e -> {
					Shop shop = service.get(player);
					if (allowEditItem)
						shop.getProducts().add(product);
					service.save(shop);
					if (previousMenu instanceof EditProductProvider)
						previousMenu.open(player);
					else
						new EditProductProvider(previousMenu, product).open(player);
				}));
		}
	}

	private void addMoneyEditor(Player player, InventoryContents contents) {
		ItemBuilder item = new ItemBuilder(Material.GOLD_INGOT);

		if (price < 0)
			item.name("&eClick to specify a dollar amount");
		else
			item.name("&e" + camelCase(prettyMoney(price)));

		contents.set(3, 4, ClickableItem.from(item.build(), e -> Nexus.getSignMenuFactory()
				.lines("", "^ ^ ^ ^ ^ ^", "Enter a", "dollar amount")
				.prefix(Shops.PREFIX)
				.onError(() -> open(player))
				.response(lines -> {
					if (lines[0].length() > 0) {
						String input = lines[0].replaceAll("[^0-9.]+", "");
						if (!Utils.isDouble(input))
							throw new InvalidInputException("Could not parse &e" + lines[0] + " &cas a dollar amount");
						double price = new BigDecimal(input).setScale(2, RoundingMode.HALF_UP).doubleValue();
						if (price < 0)
							throw new InvalidInputException("Dollar amount must be $0 or greater");
						this.price = price;
					}
					open(player);
				}).open(player)));
		}

	private void addExchangeControl(Player player, InventoryContents contents) {
		ItemBuilder item = new ItemBuilder(Material.PAPER).name("&6Action: ")
				.lore("&7⬇ " + camelCase(exchangeType.previousWithLoop().name()))
				.lore("&e⬇ " + camelCase(exchangeType.name()))
				.lore("&7⬇ " + camelCase(exchangeType.nextWithLoop().name()));
		contents.set(2, 4, ClickableItem.from(item.build(), e -> {
			exchangeType = exchangeType.nextWithLoop();
			open(player);
		}));
	}

	public void addItemSelector(Player player, InventoryContents contents, int row, AtomicReference<ItemStack> itemStack) {
		addItemSelector(player, contents, row, itemStack, true);
	}

	public void addItemSelector(Player player, InventoryContents contents, int row, AtomicReference<ItemStack> itemStack, boolean allowEditItem) {
		ItemStack placeholder = new ItemBuilder(Material.BLACK_STAINED_GLASS).name("&ePlace your item here").lore("&7or click to search for an item").build();

		if (!allowEditItem)
			contents.set(row, 4, ClickableItem.empty(itemStack.get()));
		else {
			Consumer<ItemClickData> action = e -> {
				((InventoryClickEvent) e.getEvent()).setCancelled(true);
				if (!ItemUtils.isNullOrAir(player.getItemOnCursor())) {
					try {
						ItemStack item = player.getItemOnCursor();
						if (!new ItemBuilder(item).isTradeable())
							throw new InvalidInputException("You can not trade that item in shops");

						itemStack.set(item);
						PlayerUtils.giveItem(player, itemStack.get().clone());
						player.setItemOnCursor(null);
						open(player);
					} catch (Exception ex) {
						PlayerUtils.send(player, Shops.PREFIX + "&c" + ex.getMessage());
						open(player);
					}
				} else if (contents.get(row, 4).isPresent() && contents.get(row, 4).get().getItem().equals(placeholder)) {
					Nexus.getSignMenuFactory()
							.lines("", ARROWS, "Enter a", "search term")
							.prefix(Shops.PREFIX)
							.onError(() -> open(player))
							.response(lines -> {
								if (lines[0].length() > 0) {
									Function<Material, Boolean> filter = material -> material.name().toLowerCase().contains(lines[0].toLowerCase());
									new ItemSearchProvider(this, filter, onChoose -> {
										itemStack.set(new ItemStack(onChoose.getItem().getType()));
										open(player);
									}).open(player);
								} else
									open(player);
							}).open(player);
				} else {
					itemStack.set(null);
					open(player);
				}
			};

			if (itemStack.get() != null)
				contents.set(row, 4, ClickableItem.from(itemStack.get(), action));
			else
				contents.set(row, 4, ClickableItem.from(placeholder, action));
		}

		if (contents.get(row, 4).isPresent() && contents.get(row, 4).get().getItem() != null && contents.get(row, 4).get().getItem().equals(placeholder)) {
			contents.set(row, 2, ClickableItem.empty(less8));
			contents.set(row, 3, ClickableItem.empty(less1));
			contents.set(row, 5, ClickableItem.empty(more1));
			contents.set(row, 6, ClickableItem.empty(more8));
		} else {
			contents.set(row, 2, ClickableItem.from(less8, e2 -> contents.get(row, 4).ifPresent(i -> {
				ItemStack item = i.getItem();
				item.setAmount(Math.max(1, Math.min(item.getType().getMaxStackSize(), item.getAmount() == 64 ? 56 : item.getAmount() - 8)));
				open(player);
			})));
			contents.set(row, 3, ClickableItem.from(less1, e2 -> contents.get(row, 4).ifPresent(i -> {
				ItemStack item = i.getItem();
				item.setAmount(Math.max(1, Math.min(item.getType().getMaxStackSize(), item.getAmount() - 1)));
				open(player);
			})));
			contents.set(row, 5, ClickableItem.from(more1, e2 -> contents.get(row, 4).ifPresent(i -> {
				ItemStack item = i.getItem();
				item.setAmount(Math.min(64, Math.min(item.getType().getMaxStackSize(), item.getAmount() + 1)));
				open(player);
			})));
			contents.set(row, 6, ClickableItem.from(more8, e2 -> contents.get(row, 4).ifPresent(i -> {
				ItemStack item = i.getItem();
				item.setAmount(Math.min(64, Math.min(item.getType().getMaxStackSize(), item.getAmount() == 1 ? 8 : item.getAmount() + 8)));
				open(player);
			})));
		}
	}

}
