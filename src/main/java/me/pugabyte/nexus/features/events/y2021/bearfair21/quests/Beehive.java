package me.pugabyte.nexus.features.events.y2021.bearfair21.quests;

import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MainIsland.MainNPCs;
import me.pugabyte.nexus.features.regionapi.events.common.EnteringRegionEvent;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.isNotAtBearFair;

public class Beehive implements Listener {
	private final BearFair21UserService userService = new BearFair21UserService();
	private static final String hiveRg = "bearfair21_main_beehive";
	private static final String queenRg = hiveRg + "_queen";
	@Getter
	private static final String exitRg = hiveRg + "_exit";
	private static final String enterRg = hiveRg + "_enter";
	//
	private final String allowedMsg = "*The defending swarm seems calmed by the flower's pleasant aroma. The queen allows you to enter.*";
	private final String deniedMsg = "*The swarming hoards of bees seem disturbed by your presence. You hear the Queen Bee call out to you.*";
	//
	private final Location exitLoc = new Location(BearFair21.getWorld(), -97.5, 136, 14.5, 60, -3);
	private final Location enterLoc = new Location(BearFair21.getWorld(), -93.5, 135, 14.5, -110, 17);

	public Beehive() {
		Nexus.registerListener(this);
	}

	@EventHandler
	public void onRegionEnter(EnteringRegionEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;

		if (isNotAtBearFair(player))
			return;

		String id = event.getRegion().getId();
		if (id.equalsIgnoreCase(exitRg))
			player.teleport(exitLoc);
		else if (id.equalsIgnoreCase(enterRg))
			tryEnterHive(player);
	}

	private void tryEnterHive(Player player) {
		BearFair21User user = userService.get(player);
		if (!user.isHiveAccess()) {
			List<ItemBuilder> required = new ArrayList<>();
			MaterialTag.SMALL_FLOWERS.getValues().forEach(material -> required.add(new ItemBuilder(material).amount(1)));

			if (!Quests.hasAllItemsLikeFrom(user, required)) {
				user.sendMessage(deniedMsg);
				BearFair21Talker.runScript(user, MainNPCs.QUEEN_BEE);
				return;
			}

			Quests.removeItems(user, required);
			user.sendMessage(allowedMsg);

			user.setHiveAccess(true);
			userService.save(user);
		}

		player.teleport(enterLoc);
	}

	@EventHandler
	private void onClickQueenBee(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;

		Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;

		if (BearFair21.getWGUtils().isInRegion(block.getLocation(), queenRg)) {
			BearFair21User user = userService.get(event.getPlayer());
			BearFair21Talker.runScript(user, MainNPCs.QUEEN_BEE);
		}
	}
}
