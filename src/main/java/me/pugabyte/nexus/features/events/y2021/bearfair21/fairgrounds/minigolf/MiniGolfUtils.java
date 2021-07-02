package me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf.models.MiniGolfColor;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf.models.MiniGolfHole;
import me.pugabyte.nexus.models.bearfair21.MiniGolf21User;
import me.pugabyte.nexus.models.trophy.Trophy;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MiniGolfUtils {
	public static boolean isInMiniGolf(Location location) {
		return BearFair21.getWGUtils().getRegionsLikeAt(MiniGolf.getGameRegion() + ".*", location).size() > 0;
	}

	public static String getStrokeString(MiniGolf21User user) {
		String strokes = "Stroke " + user.getCurrentStrokes();
		if (user.getMiniGolfColor().equals(MiniGolfColor.RAINBOW))
			return StringUtils.Rainbow.apply(strokes);
		else
			return user.getChatColor() + strokes;
	}

	public static boolean isInBounds(MiniGolf21User user, Location location) {
		Material material = location.subtract(0, 0.1, 0).getBlock().getType();
		if (!MiniGolf.getInBounds().contains(material))
			return false;

		return user.getCurrentHole().equals(getHole(location));
	}

	public static void checkHoleInOnes(MiniGolf21User user) {
		Set<MiniGolfHole> userHoles = user.getHoleInOne();
		for (MiniGolfHole hole : MiniGolfHole.getHoles()) {
			if (!userHoles.contains(hole))
				return;
		}

		send(user, "You've unlocked the " + StringUtils.Rainbow.apply("Rainbow Ball") + "&3!");
		user.setRainbow(true);
		MiniGolf.getService().save(user);
	}

	public static void checkCompleted(MiniGolf21User user) {
		Set<MiniGolfHole> userCompleted = user.getCompleted();
		if (userCompleted.size() == MiniGolfHole.getHoles().size())
			Quests.giveTrophy(user, Trophy.BEAR_FAIR_2021_MINIGOLF);
	}


	public static MiniGolfHole getHole(Location location) {
		Set<ProtectedRegion> regions = BearFair21.getWGUtils().getRegionsLikeAt(MiniGolf.getRegionHole() + ".*", location);
		ProtectedRegion region = regions.stream().findFirst().orElse(null);
		if (region != null) {
			for (MiniGolfHole minigolfHole : MiniGolfHole.values()) {
				String[] regionSplit = region.getId().replace(MiniGolf.getRegionHole(), "").split("_");
				if (regionSplit[0].equalsIgnoreCase(String.valueOf(minigolfHole.getHole())))
					return minigolfHole;
			}
		}

		return null;
	}

	public static boolean isBottomSlab(Block block) {
		return Tag.SLABS.isTagged(block.getType()) && ((Slab) block.getBlockData()).getType() == Slab.Type.BOTTOM;
	}

	public static void giveBall(MiniGolf21User user) {
		if (user.getOnlinePlayer().isOnline())
			PlayerUtils.giveItem(user.getOnlinePlayer(), MiniGolf.getGolfBall().clone().customModelData(user.getMiniGolfColor().getCustomModelData()).build());
	}

	public static void respawnBall(Snowball ball) {
		MiniGolf21User user = getUser(ball);
		if (user == null)
			return;

		user.debug("found user from ball, respawning ball...");

		ball.setVelocity(new Vector(0, 0, 0));
		ball.setGravity(false);
		ball.teleport(user.getBallLocation().add(0, MiniGolf.getFloorOffset(), 0));
		ball.setFireTicks(0);
		ball.setTicksLived(1);

		sendActionBar(user, "&cOut of bounds!");
		new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BASS).receiver(user.getOnlinePlayer()).pitchStep(0).play();
	}

	public static MiniGolf21User getUser(Snowball ball) {
		for (MiniGolf21User user : new HashSet<>(MiniGolf.getService().getUsers())) {
			if (user.getSnowball() == null)
				continue;

			if (!user.isOnline()) {
				user.debug("user is not online, removing golfball");
				user.removeBall();
				continue;
			}

			if (user.getSnowball().equals(ball))
				return user;
		}
		return null;
	}

	public static MiniGolf21User getUser(UUID uuid) {
		return MiniGolf.getService().get(uuid);
	}

	public static void sendActionBar(MiniGolf21User user, String message) {
		if (!user.isOnline())
			return;

		ActionBarUtils.sendActionBar(user.getOnlinePlayer(), message, Time.SECOND.x(3));
	}

	public static void error(MiniGolf21User user, String message) {
		send(user, "&c" + message);
	}

	public static void send(MiniGolf21User user, String message) {
		user.getOnlinePlayer().sendMessage(MiniGolf.getPREFIX() + StringUtils.colorize(message));
	}

	static String getScore(MiniGolf21User user) {
		int strokes = user.getCurrentStrokes();
		if (strokes == 1)
			return "Hole In One";

		int diff = strokes - user.getCurrentHole().getPar();
		switch (diff) {
			case -4:
				return "Condor";
			case -3:
				return "Albatross";
			case -2:
				return "Eagle";
			case -1:
				return "Birdie";
			case 0:
				return "Par";
			case 1:
				return "Bogey";
			case 2:
				return "Double Bogey";
			case 3:
				return "Triple Bogey";
			default:
				if (diff < 4)
					return "" + diff;
				else
					return "+" + diff;

		}
	}
}
