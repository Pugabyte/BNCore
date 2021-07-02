package me.pugabyte.nexus.features.shops;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.shop.Shop;
import me.pugabyte.nexus.models.shop.ShopService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

import static me.pugabyte.nexus.utils.StringUtils.pretty;

public class ShopUtils {

	public static void giveItems(OfflinePlayer player, ItemStack item) {
		giveItems(player, Collections.singletonList(item));
	}

	public static void giveItems(OfflinePlayer player, List<ItemStack> items) {
		Shop shop = new ShopService().get(player);
		if (player.isOnline()) {
			List<ItemStack> excess = PlayerUtils.giveItemsAndGetExcess(player.getPlayer(), items);
			shop.addHolding(excess);
			if (!excess.isEmpty())
				if (new CooldownService().check(player, "shop-excess-items", Time.SECOND.x(2)))
					PlayerUtils.send(player, new JsonBuilder(Shops.PREFIX + "Excess items added to item collection menu, click to view").command("/shops collect"));
		} else
			shop.addHolding(items);
	}

	public static String prettyMoney(Number number) {
		return prettyMoney(number, true);
	}

	public static String prettyMoney(Number number, boolean free) {
		if (free && number.doubleValue() == 0)
			return "free";
		return "$" + pretty(number);
	}

}
