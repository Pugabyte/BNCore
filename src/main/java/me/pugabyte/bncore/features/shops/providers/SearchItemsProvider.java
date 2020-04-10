package me.pugabyte.bncore.features.shops.providers;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.shops.ShopMenu;
import me.pugabyte.bncore.models.shop.Shop.Product;
import me.pugabyte.bncore.utils.MaterialTag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Collections;
import java.util.function.Function;

import static me.pugabyte.bncore.utils.StringUtils.colorize;

public class SearchItemsProvider extends _ShopProvider {

	public SearchItemsProvider(_ShopProvider previousMenu) {
		this.previousMenu = previousMenu;
	}

	@Override
	public void open(Player viewer, int page) {
		SmartInventory.builder()
				.provider(this)
				.title(colorize("&0Search Items"))
				.size(6, 9)
				.build()
				.open(viewer, page);
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		super.init(player, contents);

		contents.set(1, 1, ClickableItem.from(nameItem(Material.NAME_TAG, "&6Search by item name"),
				e -> BNCore.getSignMenuFactory().lines("", "^ ^ ^ ^ ^ ^", "Enter a", "search term").response((_player, response) -> {
					try {
						if (response[0].length() > 0)
							ShopMenu.BROWSE_ITEMS.open(player, this, Collections.singletonList((Function<Product, Boolean>) product ->
									product.getItem().getType().name().toLowerCase().contains(response[0].toLowerCase())));
						else
							open(player);
					} catch (Exception ex) {
						_player.sendMessage(ex.getMessage());
						open(player);
					}
				})
				.open(player)));

		contents.set(1, 3, ClickableItem.from(nameItem(Material.APPLE, "&6Search for food"),
				e -> ShopMenu.BROWSE_ITEMS.open(player, this, Collections.singletonList((Function<Product, Boolean>) product ->
						product.getItem().getType().isEdible()))));

		contents.set(1, 5, ClickableItem.from(nameItem(Material.ENCHANTED_BOOK, "&6Search for enchanted items"),
				e -> ShopMenu.BROWSE_ITEMS.open(player, this, Collections.singletonList((Function<Product, Boolean>) product -> {
					if (product.getItem().getType().equals(Material.ENCHANTED_BOOK)) {
						EnchantmentStorageMeta book = (EnchantmentStorageMeta) product.getItem().getItemMeta();
						return book != null && !book.getStoredEnchants().isEmpty();
					} else {
						return !product.getItem().getEnchantments().isEmpty();
					}
				}))));

		contents.set(1, 7, ClickableItem.from(nameItem(Material.DIAMOND_SWORD, "&6Search for tools,", "&6weapons and armour"),
				e -> ShopMenu.BROWSE_ITEMS.open(player, this, Collections.singletonList((Function<Product, Boolean>) product ->
						MaterialTag.TOOLS_WEAPONS_ARMOR.isTagged(product.getItem().getType())))));
	}


}
