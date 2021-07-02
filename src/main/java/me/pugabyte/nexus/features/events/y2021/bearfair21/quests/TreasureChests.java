package me.pugabyte.nexus.features.events.y2021.bearfair21.quests;

import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;
import java.util.function.Supplier;

public class TreasureChests implements Listener {
	private static final BearFair21UserService userService = new BearFair21UserService();
	private static final Supplier<ItemBuilder> treasureChest = () -> ItemBuilder.fromHeadId("13379");

	// @formatter:off
	@Getter
	private static final Set<Location> locations = Set.of(
		// Main Heads
		loc(3, 138, -17), 		// main warp, right side, freebie
		loc(137, 133, 15), 		// in purple tent, minigolf staircase
		loc(32, 138, -121),		// in red tent, under connect4 stands
		loc(30, 143, -38),		// in the cyan tent, back of maze
		loc(-66, 150, -88),		// ceiling of wood mill
		loc(-78, 195, -53),		// in the hot air balloon, nearest the observatory
		loc(13, 136, 33),		// stellar tides painting
		loc(-15, 153, -203),	// inside villager den
		loc(-19, 105, -53),		// in beehive
		loc(-147, 145, 0),		// in Honeywood, near house under construction
		loc(76, 106, -29),		// in lava cavern
		loc(-54, 110, -121),	// in lush cavern
		// Island Heads
		loc(-163, 110, -188),	// mgn - in volcano
		loc(-140, 157, -183),	// mgn - top of glass dome
		loc(-67, 129, -344),	// pugmas - grinch cave
		loc(-82, 167, -373),	// pugmas - top most house
		loc(58, 143, -305),		// halloween - between wall and house
		loc(96, 110, -309),		// halloween - underworld, near ruben
		loc(188, 152, -144),	// sdu - by burning trees
		loc(164, 141, -242)		// sdu - under staircase on backside of island
	);
	// @formatter:on

	public TreasureChests() {
		Nexus.registerListener(this);
	}

	@EventHandler
	public void onClickHead(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (BearFair21.isNotAtBearFair(event)) return;
		if (BlockUtils.isNullOrAir(block)) return;
		if (!MaterialTag.PLAYER_SKULLS.isTagged(block.getType())) return;

		Location location = block.getLocation();
		if (!locations.contains(location)) return;
		if (!ItemUtils.isSameHead(ItemUtils.getItem(block), treasureChest.get().build())) return;

		BearFair21User user = userService.get(event.getPlayer());
		if (user.isFoundAllTreasureChests()) return;

		int userSize = user.getTreasureChests().size();
		int size = locations.size();
		if (userSize == size) {
			Quests.sound_villagerNo(user.getPlayer());
			user.sendMessage("&3You already found &eall &3Treasure Chests!");
			return;
		}

		if (user.getTreasureChests().contains(location)) {
			Quests.sound_villagerNo(user.getPlayer());
			user.sendMessage("&cYou've already found this Treasure Chest! (" + userSize + "/" + size + ")");
			return;
		}

		user.getTreasureChests().add(location);
		++userSize;
		if (userSize == size) {
			user.setFoundAllTreasureChests(true);

			user.sendMessage("&3You found &eall &3Treasure Chests!");
			Quests.giveKey(user, 2);
			BearFair21.giveTokens(user, 300);
		} else {
			Quests.sound_obtainItem(user.getPlayer());
			user.sendMessage("&3You've found a Treasure Chest! (&e" + userSize + "/" + size + "&3)");
		}

		userService.save(user);
	}

	public static Location loc(int x, int y, int z) {
		return new Location(BearFair21.getWorld(), x, y, z);
	}
}
