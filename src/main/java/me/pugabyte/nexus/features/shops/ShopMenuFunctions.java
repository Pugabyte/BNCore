package me.pugabyte.nexus.features.shops;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.pugabyte.nexus.features.shops.providers.SearchProductsProvider;
import me.pugabyte.nexus.models.shop.Shop.ExchangeType;
import me.pugabyte.nexus.models.shop.Shop.Product;
import me.pugabyte.nexus.utils.EnumUtils.IteratableEnum;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.function.Predicate;

public class ShopMenuFunctions {

	@Data
	@AllArgsConstructor
	public static class Filter {
		private FilterType type;
		private Predicate<Product> filter;
		private String message;
	}

	public interface FilterType extends IteratableEnum {

		String name();

		default Predicate<Product> getFilter() {
			return null;
		}

		default Filter get() {
			return new Filter(this, getFilter(), null);
		}

		default Filter of(String message) {
			return new Filter(this, getFilter(), message);
		}

		default Filter of(Predicate<Product> filter) {
			return new Filter(this, filter, null);
		}

		default Filter of(String message, Predicate<Product> filter) {
			return new Filter(this, filter, message);
		}
	}

	public enum FilterRequiredType implements FilterType {
		REQUIRED
	}

	public enum FilterSearchType implements FilterType {
		SEARCH;

		public Filter of(String message) {
			return SEARCH.of(message, product -> SearchProductsProvider.filter(product.getItem(), item -> {
				Material type = item.getType();

				if (type.name().toLowerCase().contains(message.toLowerCase()))
					return true;
				if (type.name().toLowerCase().replace("_", " ").contains(message.toLowerCase()))
					return true;

				for (Enchantment enchantment : item.getEnchantments().keySet())
					if (enchantment.getKey().getKey().toLowerCase().contains(message.toLowerCase()))
						return true;

				if (item.getItemMeta() instanceof EnchantmentStorageMeta) {
					EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
					for (Enchantment enchantment : meta.getStoredEnchants().keySet())
						if (enchantment.getKey().getKey().toLowerCase().contains(message.toLowerCase()))
							return true;
				}

				return false;
			}));
		}
	}

	public enum FilterExchangeType implements FilterType {
		BOTH,
		BUYING(product -> product.getExchangeType() == ExchangeType.TRADE || product.getExchangeType() == ExchangeType.SELL),
		SELLING(product -> product.getExchangeType() == ExchangeType.BUY);

		@Getter
		private Predicate<Product> filter;

		FilterExchangeType() {}

		FilterExchangeType(Predicate<Product> filter) {
			this.filter = filter;
		}
	}

	public enum FilterMarketItems implements FilterType {
		SHOWN,
		HIDDEN(product -> !product.isMarket());

		@Getter
		private Predicate<Product> filter;

		FilterMarketItems() {}

		FilterMarketItems(Predicate<Product> filter) {
			this.filter = filter;
		}
	}

	public enum FilterEmptyStock implements FilterType {
		SHOWN,
		HIDDEN(Product::canFulfillPurchase);

		@Getter
		private Predicate<Product> filter;

		FilterEmptyStock() {}

		FilterEmptyStock(Predicate<Product> filter) {
			this.filter = filter;
		}
	}

	public enum SortType {
		ALPHABETICAL,
		STOCK,
		PRICE
	}

}
