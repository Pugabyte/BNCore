package me.pugabyte.nexus.features.events.y2021.pride21;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.pride21.Pride21User;
import me.pugabyte.nexus.models.pride21.Pride21UserService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.WorldGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eden.utils.StringUtils.plural;

public class BuyFlagsMenu extends MenuUtils implements InventoryProvider {
	private static final Pride21UserService service = new Pride21UserService();
	private static final EventUserService eventService = new EventUserService();
	private static final int COST = 10;

	@Override
	public void open(Player viewer, int page) {
		SmartInventory.builder()
				.provider(this)
				.title("Pride Shop")
				.size(getRows(Flags.values().length*2, 0), 9)
				.build()
				.open(viewer, page);
	}

	@Override
	public void init(Player player, InventoryContents inventoryContents) {
		int freebies = service.get(player).rewardsLeft();
		Arrays.stream(Flags.values()).forEach(flag -> {
			ItemStack flagItem = flag.getFlag();
			ItemStack buntingItem = flag.getBunting();
			String lore;
			if (freebies > 0)
				lore = "&3You have &e"+freebies+plural(" free item", freebies)+"&3 remaining";
			else
				lore = "&3Cost: &e" + COST + " event tokens";
			List<Component> itemLore = Collections.singletonList(new JsonBuilder(lore).decorate(false, TextDecoration.ITALIC).build());
			flagItem.lore(itemLore);
			buntingItem.lore(itemLore);
			inventoryContents.add(ClickableItem.from(flagItem, $ -> purchase(flag, player, false)));
			inventoryContents.add(ClickableItem.from(buntingItem, $ -> purchase(flag, player, true)));
		});
		inventoryContents.set(5, 8, ClickableItem.empty(new ItemBuilder(Material.PAPER).name("&3Your balance: &e" + eventService.get(player).getTokens() + " tokens").build()));
	}

	private void purchase(Flags flag, Player player, boolean bunting) {
		ItemStack item = bunting ? flag.getBunting() : flag.getFlag();
		Pride21User user = service.get(player);
		EventUser eventUser = eventService.get(player);
		if (user.claimReward()) {/* this method removes tokens and saves for us so we don't need to do anything here */}
		else if (eventUser.getTokens() >= COST) {
			eventUser.takeTokens(COST);
			eventService.save(eventUser);
		} else {
			player.sendMessage(JsonBuilder.fromPrefix(Pride21.PREFIX).next("&cYou don't have enough event tokens to purchase this item"));
			new SoundBuilder(Sound.ENTITY_VILLAGER_NO).receiver(player).category(SoundCategory.VOICE).volume(0.8f).play();
			return;
		}
		PlayerUtils.giveItemAndMailExcess(player, item, "Pride 2021 Purchase", WorldGroup.SURVIVAL);
		open(player);
	}
}
