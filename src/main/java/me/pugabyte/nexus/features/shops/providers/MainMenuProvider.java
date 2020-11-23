package me.pugabyte.nexus.features.shops.providers;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import me.pugabyte.nexus.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MainMenuProvider extends _ShopProvider {

	public MainMenuProvider(_ShopProvider previousMenu) {
		this.previousMenu = previousMenu;
	}

	@Override
	public void open(Player viewer, int page) {
		open(viewer, page, this, "&0Shops");
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		super.init(player, contents);

		contents.set(1, 2, ClickableItem.from(nameItem(Material.CHEST, "&6&lBrowse Market"), e -> new BrowseMarketProvider(this).open(player)));
		contents.set(1, 4, ClickableItem.from(nameItem(Material.CHEST, "&6&lBrowse Shops"), e -> new BrowseShopsProvider(this).open(player)));
		contents.set(1, 6, ClickableItem.from(nameItem(Material.CHEST, "&6&lBrowse Items"), e -> new BrowseItemsProvider(this).open(player)));

		contents.set(3, 3, ClickableItem.from(nameItem(Material.COMPASS, "&6&lSearch Items"), e -> new SearchItemsProvider(this).open(player)));
		ItemStack head = new ItemBuilder(Material.PLAYER_HEAD).skullOwner(player).name("&6&lYour Shop").build();
		contents.set(3, 5, ClickableItem.from(head, e -> new YourShopProvider(this).open(player)));
	}

}