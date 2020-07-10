package me.pugabyte.bearnation.survival.features.shops;

import lombok.NonNull;
import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Arg;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.api.utils.ItemBuilder;
import me.pugabyte.bearnation.features.shops.providers.MainMenuProvider;
import me.pugabyte.bearnation.models.shop.Shop;
import me.pugabyte.bearnation.models.shop.Shop.ExchangeType;
import me.pugabyte.bearnation.models.shop.Shop.Product;
import me.pugabyte.bearnation.models.shop.Shop.ShopGroup;
import me.pugabyte.bearnation.models.shop.ShopService;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

@Aliases("shops")
public class ShopCommand extends CustomCommand {
	private ShopService service = new ShopService();

	public ShopCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		if (player().hasPermission("group.seniorstaff"))
			new MainMenuProvider(null).open(player());
		else
			send("&cComing soon!");
	}

	@Path("addItems1 [player]")
	void addItems1(@Arg("self") OfflinePlayer player) {
		Shop shop = service.get(player);
		shop.getProducts().clear();
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.COBBLESTONE).amount(32).build(), 512, ExchangeType.SELL, 8.5D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.DIAMOND_SWORD).build(), 16, ExchangeType.SELL, 50D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.IRON_ORE).amount(10).build(), 100, ExchangeType.TRADE, new ItemStack(Material.GOLD_ORE, 3)));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.DIAMOND).amount(10).build(), 500, ExchangeType.BUY, 100D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.PUMPKIN_PIE).amount(2).build(), 32, ExchangeType.SELL, 30D));
		service.save(shop);
	}

	@Path("addItems2 [player]")
	void addItems2(@Arg("self") OfflinePlayer player) {
		Shop shop = service.get(player);
		shop.getProducts().clear();
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.SPRUCE_LOG).amount(32).build(), 100, ExchangeType.SELL, 16D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.IRON_AXE).amount(1).build(), 16, ExchangeType.SELL, 30D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.GOLD_INGOT).amount(10).build(), 100, ExchangeType.TRADE, new ItemStack(Material.GOLDEN_APPLE)));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.DIAMOND).amount(10).build(), -1, ExchangeType.BUY, 90D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.CROSSBOW).enchant(Enchantment.QUICK_CHARGE, 3).enchant(Enchantment.PIERCING, 4).build(), 5, ExchangeType.SELL, 1000D));
		service.save(shop);
	}

	@Path("addItems3 [player]")
	void addItems3(@Arg("self") OfflinePlayer player) {
		Shop shop = service.get(player);
		shop.getProducts().clear();
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.SAND).amount(32).build(), 2000, ExchangeType.SELL, 8.5D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.BOW).enchant(Enchantment.ARROW_INFINITE).build(), 1, ExchangeType.SELL, 1000D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.COOKED_BEEF).amount(10).build(), 100, ExchangeType.TRADE, new ItemStack(Material.BEEF, 10)));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.PURPLE_WOOL).amount(10).build(), 10000, ExchangeType.BUY, 3D));
		shop.getProducts().add(new Product(shop.getUuid(), ShopGroup.SURVIVAL, new ItemBuilder(Material.APPLE).amount(8).build(), 32, ExchangeType.SELL, 16D));
		service.save(shop);
	}

}
