package me.pugabyte.bearnation.server.features.holidays.bearfair20.quests;

import me.pugabyte.bearnation.BNCore;
import me.pugabyte.bearnation.api.utils.MaterialTag;
import me.pugabyte.bearnation.features.holidays.bearfair20.BearFair20;
import me.pugabyte.bearnation.server.models.bearfair.BearFairService;
import me.pugabyte.bearnation.server.models.bearfair.BearFairUser;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

import static me.pugabyte.bearnation.features.holidays.bearfair20.BearFair20.PREFIX;
import static me.pugabyte.bearnation.features.holidays.bearfair20.BearFair20.givePoints;
import static me.pugabyte.bearnation.features.holidays.bearfair20.BearFair20.isAtBearFair;
import static me.pugabyte.bearnation.features.holidays.bearfair20.BearFair20.send;

public class EasterEggs implements Listener {
	BearFairService service = new BearFairService();
	private static final String easterEgg = "ba3b7698-589c-3326-90ff-4862853a5c24";
	private static final int total = 15;
	private static String foundOne = PREFIX + "You found a secret treasure chest! There are still more to find throughout the islands.";
	private static String duplicate = PREFIX + "You already found this one.";
	private static String foundAll = PREFIX + "The final treasure chest has been found! You are a champion of treasure hunting. Congratulations!";


	public EasterEggs() {
		BNCore.registerListener(this);
	}

	@EventHandler
	public void onRightClickSkull(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (block == null) return;
		if (!MaterialTag.SKULLS.isTagged(block.getType())) return;
		if (!EquipmentSlot.HAND.equals(event.getHand())) return;

		Player player = event.getPlayer();
		if (!isAtBearFair(player)) return;

		Skull skull = (Skull) block.getState();
		if (skull.getOwningPlayer() == null) return;
		if (!easterEgg.equals(skull.getOwningPlayer().getUniqueId().toString())) return;

		Location playerLoc = player.getLocation();
		Location blockLoc = block.getLocation();
		BearFairUser bfUser = service.get(player);
		List<Location> foundLocs = bfUser.getEasterEggsLocs();
		if (foundLocs.contains(blockLoc)) {
			send(duplicate, player);
			player.playSound(playerLoc, Sound.ENTITY_VILLAGER_NO, 2F, 1F);
			return;
		}

		foundLocs.add(blockLoc);

		if (foundLocs.size() == total) {
			send(foundAll, player);
			player.playSound(playerLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2F, 1F);

			if (givePoints) {
				BearFair20.send(PREFIX + "&e+150 &3Points!", player);
				bfUser.givePoints(150, true);
			}

		} else {
			send(foundOne, player);
			player.playSound(playerLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2F, 2F);
			player.playSound(playerLoc, Sound.BLOCK_BEACON_POWER_SELECT, 2F, 2F);

			if (givePoints) {
				bfUser.givePoints(10, true);
			}
		}
		service.save(bfUser);
	}

}
