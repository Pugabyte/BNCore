package me.pugabyte.bncore.models.shop;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.bncore.framework.persistence.serializer.mongodb.ItemStackConverter;
import me.pugabyte.bncore.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.bncore.models.PlayerOwnedObject;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.pugabyte.bncore.features.shops.ShopUtils.giveItem;
import static me.pugabyte.bncore.features.shops.ShopUtils.pretty;
import static me.pugabyte.bncore.utils.StringUtils.colorize;

@Data
@Builder
@Entity("shop")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, ItemStackConverter.class})
// Dumb structure due to morphia refusing to deserialize interfaces properly
public class Shop extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private String description;
	@Embedded
	private List<Product> products = new ArrayList<>();
	@Embedded
	private List<ItemStack> holding = new ArrayList<>();

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Product {
		private UUID uuid;
		private ItemStack item;
		private double stock;
		private ExchangeType exchangeType;
		private Object price;

		public Shop getShop() {
			return new ShopService().get(uuid);
		}

		@SneakyThrows
		public void process(Player customer) {
			getExchange().process(this, customer);
		}

		@NotNull
		@SneakyThrows
		public Exchange getExchange() {
			return (Exchange) exchangeType.getClazz().getDeclaredConstructors()[0].newInstance(price);
		}

	}

	public enum ExchangeType {
		ITEM_FOR_ITEM(ItemForItemExchange.class),
		ITEM_FOR_MONEY(ItemForMoneyExchange.class),
		MONEY_FOR_ITEM(MoneyForItemExchange.class);

		@Getter
		private Class<? extends Exchange> clazz;

		ExchangeType(Class<? extends Exchange> clazz) {
			this.clazz = clazz;
		}
	}

	public interface Exchange {

		void process(Product product, Player customer);

		List<String> getLore(Product product);

	}

	@Data
	@Builder
	@AllArgsConstructor
	@Embedded(concreteClass = ItemForItemExchange.class)
	// Customer buying an item from the shop owner for money
	public static class ItemForMoneyExchange implements Exchange {
		@NonNull
		private Double price;

		@Override
		public void process(Product product, Player customer) {
			BNCore.log("Processing ItemForMoneyExchange");
			if (product.getStock() == 0)
				throw new InvalidInputException("This item is out of stock");
			if (!BNCore.getEcon().has(customer, price))
				throw new InvalidInputException("You do not have enough money to purchase this item");

			BNCore.getEcon().withdrawPlayer(customer, price);
			BNCore.getEcon().depositPlayer(product.getShop().getOfflinePlayer(), price);
			giveItem(customer, product.getItem());
			product.setStock(product.getStock() - product.getItem().getAmount());
			new ShopService().save(product.getShop());
			customer.sendMessage(colorize("You purchased " + product.getItem().getType() + " x" + product.getItem().getAmount() + " for $" + price));
		}

		@Override
		public List<String> getLore(Product product) {
			int stock = (int) product.getStock();
			return Arrays.asList(
					"&7Buy &e" + product.getItem().getAmount() + " &7for &a" + pretty(price),
					"&7Stock: " + (stock > 0 ? "&e" : "&c") + stock,
					"&7Seller: &e" + product.getShop().getOfflinePlayer().getName()
			);
		}
	}

	@Data
	@Builder
	@AllArgsConstructor
	@Embedded(concreteClass = ItemForItemExchange.class)
	// Customer buying an item from the shop owner for other items
	public static class ItemForItemExchange implements Exchange {
		@NonNull
		private ItemStack price;

		@Override
		public void process(Product product, Player customer) {
			BNCore.log("Processing ItemForItemExchange");
			if (product.getStock() == 0)
				throw new InvalidInputException("This item is out of stock");
			if (product.getStock() < product.getItem().getAmount())
				throw new InvalidInputException("There is not enough stock to fulfill your purchase");
			if (!customer.getInventory().containsAtLeast(price, price.getAmount()))
				throw new InvalidInputException("You do not have enough " + pretty(price) + " to purchase this item");

			customer.getInventory().removeItem(price);
			giveItem(product.getShop().getOfflinePlayer(), price);
			giveItem(customer, product.getItem());
			product.setStock(product.getStock() - product.getItem().getAmount());
			new ShopService().save(product.getShop());
			customer.sendMessage(colorize("You purchased " + pretty(product.getItem()) + " for " + pretty(price)));
		}

		@Override
		public List<String> getLore(Product product) {
			int stock = (int) product.getStock();
			return Arrays.asList(
					"&7Buy &e" + product.getItem().getAmount() + " &7for &a" + pretty(price),
					"&7Stock: " + (stock > 0 ? "&e" : "&c") + stock,
					"&7Seller: &e" + product.getShop().getOfflinePlayer().getName()
			);
		}
	}

	@Data
	@Builder
	@AllArgsConstructor
	@Embedded(concreteClass = MoneyForItemExchange.class)
	// Customer selling an item to the shop owner for money
	public static class MoneyForItemExchange implements Exchange {
		@NonNull
		private Double price;

		@Override
		public void process(Product product, Player customer) {
			BNCore.log("Processing ItemForMoneyExchange");
			OfflinePlayer shopOwner = product.getShop().getOfflinePlayer();
			if (product.getStock() == 0)
				throw new InvalidInputException("This item is out of stock");
			if (product.getStock() > 0 && product.getStock() < price)
				throw new InvalidInputException("There is not enough stock to fulfill your purchase");
			if (!BNCore.getEcon().has(shopOwner, price))
				throw new InvalidInputException(shopOwner.getName() + " does not have enough money to purchase this item from you");
			if (!customer.getInventory().containsAtLeast(product.getItem(), product.getItem().getAmount()))
				throw new InvalidInputException("You do not have enough " + pretty(product.getItem()) + " to sell");

			BNCore.getEcon().withdrawPlayer(shopOwner, price);
			BNCore.getEcon().depositPlayer(customer, price);
			giveItem(shopOwner, product.getItem());
			customer.getInventory().removeItem(product.getItem());
			product.setStock(product.getStock() - price);
			new ShopService().save(product.getShop());
			customer.sendMessage(colorize("You sold " + product.getItem().getType() + " x" + product.getItem().getAmount() + " for $" + price));
		}

		@Override
		public List<String> getLore(Product product) {
			int stock = (int) product.getStock();
			return Arrays.asList(
					"&7Sell &e" + product.getItem().getAmount() + " &7for &a" + pretty(price),
					"&7Stock: &e" + pretty(product.getStock()),
					"&7Seller: &e" + product.getShop().getOfflinePlayer().getName()
			);
		}
	}

}
