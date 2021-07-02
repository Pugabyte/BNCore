package me.pugabyte.nexus.features.events.y2021.bearfair21;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import eden.utils.Utils;
import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Rides;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.IslandType;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MainIsland.MainNPCs;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MinigameNightIsland;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.Merchants;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.models.bearfair21.BearFair21Config;
import me.pugabyte.nexus.models.bearfair21.BearFair21ConfigService;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.godmode.GodmodeService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.pugabyte.nexus.features.commands.staff.WorldGuardEditCommand.canWorldGuardEdit;
import static me.pugabyte.nexus.models.bearfair21.BearFair21Config.BearFair21ConfigOption.GIVE_REWARDS;
import static me.pugabyte.nexus.models.bearfair21.BearFair21Config.BearFair21ConfigOption.QUESTS;
import static me.pugabyte.nexus.models.bearfair21.BearFair21Config.BearFair21ConfigOption.WARP;
import static me.pugabyte.nexus.utils.PlayerUtils.isVanished;
import static me.pugabyte.nexus.utils.StringUtils.colorize;

public class BearFair21 implements Listener {
	private static final BearFair21ConfigService configService = new BearFair21ConfigService();
	@Getter
	private static final BearFair21Config config = configService.get0();
	private static final BearFair21UserService userService = new BearFair21UserService();

	/**
	 * TODO BF21: When BearFair21 is over disable: GIVE_REWARDS
	 */

	@Getter
	private static final String PREFIX = "&8&l[&eBearFair&8&l] &3";
	@Getter
	private static final String region = "bearfair21";
	@Getter
	private static final Location shipSpawnLoc = BearFair21.locationOf(5, 135, 32, 90, 0).toCenterLocation();


	public BearFair21() {
		Nexus.registerListener(this);

		new Timer("    Restrictions", BearFair21Restrictions::new);
		new Timer("    Fairgrounds", Fairgrounds::new);
		new Timer("    Islands", IslandType::values);
		new Timer("    Quests", Quests::new);

		addTokenMax(BF21PointSource.ARCHERY, 25);
		addTokenMax(BF21PointSource.MINIGOLF, 25);
		addTokenMax(BF21PointSource.FROGGER, 25);
		addTokenMax(BF21PointSource.SEEKER, 25);
		addTokenMax(BF21PointSource.REFLECTION, 25);
		addTokenMax(BF21PointSource.TRADER, 50);
	}

	public static World getWorld() {
		return Bukkit.getWorld("bearfair21");
	}

	public static Location locationOf(double x, double y, double z) {
		return locationOf(x, y, z, 0, 0);
	}

	public static Location locationOf(double x, double y, double z, float yaw, float pitch) {
		return new Location(getWorld(), x, y, z, yaw, pitch);
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

	public static boolean isNotAtBearFair(Block block) {
		return isNotAtBearFair(block.getLocation());
	}

	public static boolean isNotAtBearFair(Entity entity) {
		return isNotAtBearFair(entity.getLocation());
	}

	public static boolean isNotAtBearFair(Player player) {
		return isNotAtBearFair(player.getLocation());
	}

	public static boolean isNotAtBearFair(Location location) {
		return !location.getWorld().equals(getWorld());
	}

	public static boolean isNotAtBearFair(PlayerInteractEvent event) {
		return isNotAtBearFair(event.getHand(), event.getPlayer());
	}

	public static boolean isNotAtBearFair(PlayerInteractEntityEvent event) {
		return isNotAtBearFair(event.getHand(), event.getPlayer());
	}

	private static boolean isNotAtBearFair(EquipmentSlot slot, Player player) {
		if (!EquipmentSlot.HAND.equals(slot)) return true;

		return BearFair21.isNotAtBearFair(player);
	}

	public static boolean canDoBearFairQuest(Player player) {
		if (!BearFair21.getConfig().isEnabled(QUESTS)) return false;
		return !isNotAtBearFair(player);
	}

	public static boolean canDoBearFairQuest(PlayerInteractEvent event) {
		if (!canDoBearFairQuest(event.getPlayer())) return false;
		return !isNotAtBearFair(event);
	}

	public static boolean canDoBearFairQuest(PlayerInteractEntityEvent event) {
		if (!canDoBearFairQuest(event.getPlayer())) return false;
		return !isNotAtBearFair(event);
	}

	public static boolean isInRegion(Block block, String region) {
		return isInRegion(block.getLocation(), region);
	}

	public static boolean isInRegion(Player player, String region) {
		return isInRegion(player.getLocation(), region);
	}

	public static boolean isInRegion(Location location, String region) {
		return !isNotAtBearFair(location) && getWGUtils().isInRegion(location, region);
	}

	public static boolean isInRegionRegex(Location location, String regex) {
		return !isNotAtBearFair(location) && getWGUtils().getRegionsLikeAt(regex, location).size() > 0;
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

	static {
		Tasks.repeat(Time.SECOND, Time.SECOND, () -> {
			if (!config.isEnabled(WARP))
				return;

			for (Player player : BearFair21.getPlayers()) {
				if (PlayerUtils.isVanished(player) || player.getGameMode() != GameMode.SURVIVAL) continue;

				if (player.isFlying()) {
					player.setFallDistance(0);
					player.setAllowFlight(false);
					player.setFlying(false);
					player.sendMessage(colorize("&cNo cheating!"));
				}
			}
		});
	}

	public static Set<Player> getPlayers() {
		return new HashSet<>(PlayerUtils.getOnlinePlayers(getWorld()));
	}

	// point stuff

	private static final Map<String, Integer> tokenMaxes = new HashMap<>();

	public static void addTokenMax(BF21PointSource source, int amount) {
		tokenMaxes.put("bearfair21_" + source.name().toLowerCase(), amount);
	}

	public static int getDailyTokensLeft(OfflinePlayer player, BF21PointSource source, int amount) {
		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		return user.getDailyTokensLeft(source.getId(), amount, tokenMaxes);
	}

	public static void giveDailyTokens(Player player, BF21PointSource source, int amount) {
		if (!config.isEnabled(GIVE_REWARDS))
			return;

		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		final int dailyTokensLeft = Math.abs(getDailyTokensLeft(player, source, 0));

		if (dailyTokensLeft == 0) {
			ActionBarUtils.sendActionBar(player, "&cDaily token limit reached");
		} else {
			user.giveTokens(source.getId(), amount, tokenMaxes);
			service.save(user);

			ActionBarUtils.sendActionBar(player, "&a+" + amount + " Event Tokens");
		}
	}

	public static void giveTokens(BearFair21User user, int amount) {
		giveTokens(user.getPlayer(), amount);
	}

	public static void giveTokens(Player player, int amount) {
		if (!config.isEnabled(GIVE_REWARDS))
			return;

		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		user.giveTokens(amount);
		service.save(user);

		ActionBarUtils.sendActionBar(player, "&a+" + amount + " Event Tokens");
	}

	public static boolean canWarp() {
		return config.isEnabled(WARP);
	}

	public static void startup() {
		Quests.startup();
		Rides.startup();
	}

	public static void shutdown() {
		Quests.shutdown();
	}

	public enum BF21PointSource {
		TRADER,
		MINIGOLF,
		SEEKER,
		ARCHERY,
		FROGGER,
		REFLECTION,
		;

		private String getId() {
			return "bearfair21_" + name().toLowerCase();
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		if (isNotAtBearFair(event.getPlayer())) return;
		if (event.getPlayer().hasPermission("group.staff"))
			event.getPlayer().chat("/cheats off");
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!event.getInventory().getType().equals(InventoryType.MERCHANT)) return;
		if (!(event.getPlayer() instanceof Player player)) return;
		if (isNotAtBearFair(player)) return;

		BearFair21UserService userService = new BearFair21UserService();
		BearFair21User user = userService.get(player);
		// Trader
		{
			List<ItemStack> items = Quests.getItemsLikeFrom(user, Collections.singletonList(Merchants.traderCoupon.clone()));
			if (Utils.isNullOrEmpty(items))
				return;

			Quests.removeItemStacks(user, items);
			giveDailyTokens(player, BF21PointSource.TRADER, 50);
			Quests.sound_obtainItem(player);
		}

		// James
		{
			List<ItemStack> items = Quests.getItemsLikeFrom(user, Collections.singletonList(MinigameNightIsland.getCarKey()));
			if (Utils.isNullOrEmpty(items))
				return;

			user.setMgn_boughtCar(true);
			user.getNextStepNPCs().remove(MainNPCs.JAMES.getNpcId());
			userService.save(user);
		}
	}

	@EventHandler
	public void onRegionEnterYacht(PlayerEnteredRegionEvent event) {
		if (!config.isEnabled(WARP)) return;
		if (!event.getRegion().getId().equalsIgnoreCase("spawn_spaceyacht")) return;
		Player player = event.getPlayer();
		send("", player);
		send("&3Captain &8> &fAll aboard! Everyone to their sleeping quarters! We'll be leaving soon.", player);
		send("", player);
	}

	@EventHandler
	public void onRegionEnterQuarters(PlayerEnteredRegionEvent event) {
		if (!config.isEnabled(WARP)) return;
		if (!event.getRegion().getId().equalsIgnoreCase("spawn_bearfair")) return;

		Location spawnTransition = new Location(Bukkit.getWorld("survival"), 9.5, 100, -180.5);
		Player player = event.getPlayer();
		BearFair21User user = userService.get(player);

		Tasks.wait(Time.SECOND.x(2), () -> {
			player.addPotionEffects(Collections.singletonList
				(new PotionEffect(PotionEffectType.BLINDNESS, 80, 250, false, false, false)));
			player.teleport(spawnTransition);
			send("", player);
			send("&e&o*You immediately fall asleep in your bed*", player);
			send("", player);

			Tasks.wait(Time.SECOND.x(4), () -> {
				boolean firstVisit = user.isFirstVisit();
				user.setFirstVisit(false);
				userService.save(user);
				player.teleport(shipSpawnLoc);
				send("", player);
				send("&e&o*You awake to the sounds of birds chirping, you must have slept the whole trip*", player);
				send("", player);
				if (firstVisit) {
//					user.getOnlinePlayer().getInventory().setContents(new ItemStack[0]);

					Tasks.wait(Time.SECOND.x(3), () -> {
						send("&8&l[&c&l!!!&8&l] &3You can now warp here using: &e/bearfair21", player);
						Quests.sound_obtainItem(player);
					});
				}
			});
		});
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		final boolean fromBearFair = event.getFrom().getWorld().equals(getWorld());
		final boolean toBearFair = event.getTo().getWorld().equals(getWorld());

		if (fromBearFair || !toBearFair)
			return;

		BearFair21User user = userService.get(event.getPlayer());
		if (!user.isFirstVisit())
			return;

		event.setCancelled(true);
		user.sendMessage(PREFIX + "To unlock the warp, you must first travel to Bear Fair aboard the space yacht at spawn");
	}

}
