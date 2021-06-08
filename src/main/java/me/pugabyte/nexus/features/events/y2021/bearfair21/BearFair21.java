package me.pugabyte.nexus.features.events.y2021.bearfair21;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.godmode.GodmodeService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.TimeUtils.Timer;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static me.pugabyte.nexus.features.commands.staff.WorldGuardEditCommand.canWorldGuardEdit;
import static me.pugabyte.nexus.utils.PlayerUtils.isVanished;


public class BearFair21 {
	@Getter
	private static final String PREFIX = "&8&l[&eBearFair&8&l] &3";
	@Getter
	private static final String region = "bearfair21";
	@Getter
	private static final boolean allowWarp = false;
	//
	// TODO BF21: When BF is over, disable these, and disable block break/place on regions
	public static boolean enableQuests = true;
	public static boolean giveDailyPoints = false;


	public BearFair21() {
		new Timer("    Restrictions", BearFair21Restrictions::new);
		new Timer("    Fairgrounds", Fairgrounds::new);
		if (enableQuests)
			new Timer("    Quests", Quests::new);

		Arrays.stream(BF21PointSource.values()).forEach(source -> addTokenMax(source, 25));
	}

	public static World getWorld() {
		return Bukkit.getWorld("bearfair21");
	}

	public static WorldGuardUtils getWGUtils() {
		return new WorldGuardUtils(getWorld());
	}

	public static WorldEditUtils getWEUtils() {
		return new WorldEditUtils(getWorld());
	}

	public static ProtectedRegion getProtectedRegion() {
		return getWGUtils().getProtectedRegion(region);
	}

	public static boolean isAtBearFair(Block block) {
		return isAtBearFair(block.getLocation());
	}

	public static boolean isAtBearFair(Entity entity) {
		return isAtBearFair(entity.getLocation());
	}

	public static boolean isAtBearFair(Player player) {
		return isAtBearFair(player.getLocation());
	}

	public static boolean isAtBearFair(Location location) {
		return location.getWorld().equals(getWorld());
	}

	public static boolean isInRegion(Block block, String region) {
		return isInRegion(block.getLocation(), region);
	}

	public static boolean isInRegion(Player player, String region) {
		return isInRegion(player.getLocation(), region);
	}

	public static boolean isInRegion(Location location, String region) {
		return isAtBearFair(location) && getWGUtils().isInRegion(location, region);
	}

	public static boolean isInRegionRegex(Location location, String regex) {
		return isAtBearFair(location) && getWGUtils().getRegionsLikeAt(regex, location).size() > 0;
	}

	public static void send(String message, Player to) {
		PlayerUtils.send(to, message);
	}

	public static String isCheatingMsg(Player player) {
		if (canWorldGuardEdit(player)) return "wgedit";
		if (!player.getGameMode().equals(GameMode.SURVIVAL)) return "creative";
		if (player.isFlying()) return "fly";
		if (isVanished(player)) return "vanish";
		if (new GodmodeService().get(player).isEnabled()) return "godmode";

		return null;
	}

	public static Set<Player> getPlayers() {
		Set<Player> result = new HashSet<>();
		for (Player player : PlayerUtils.getRealPlayers()) {
			if (isAtBearFair(player))
				result.add(player);
		}
		return result;
	}

	// point stuff

	private static final Map<String, Integer> tokenMaxes = new HashMap<>();

	public static void addTokenMax(BF21PointSource source, int amount) {
		tokenMaxes.put("bearfair21_" + source.name().toLowerCase(), amount);
	}

	public static int checkDailyTokens(OfflinePlayer player, BF21PointSource source, int amount) {
		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		return user.checkDaily("bearfair21_" + source.name().toLowerCase(), amount, tokenMaxes);
	}

	public static void giveDailyPoints(Player player, BF21PointSource source, int amount) {
		// TODO BF21: Remove me
		if (true) {
			player.sendMessage("Give +" + amount + " points");
			return;
		}
		//

		if (!giveDailyPoints)
			return;

		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		user.giveTokens("bearfair21_" + source.name().toLowerCase(), amount, tokenMaxes);
		service.save(user);

		ActionBarUtils.sendActionBar(player, "+" + amount + " Event Points");
	}

	public static void givePoints(Player player, int amount) {
		// TODO BF21: Remove me
		if (true) {
			player.sendMessage("Give +" + amount + " points");
			return;
		}
		//

		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		user.giveTokens(amount);
		service.save(user);

		ActionBarUtils.sendActionBar(player, "+" + amount + " Event Points");
	}

	public enum BF21PointSource {
		ARCHERY,
		MINIGOLF,
		FROGGER,
		SEEKER,
		REFLECTION
	}
}
