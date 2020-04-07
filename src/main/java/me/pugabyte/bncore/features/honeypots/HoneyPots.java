package me.pugabyte.bncore.features.honeypots;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.chat.Chat;
import me.pugabyte.bncore.models.setting.Setting;
import me.pugabyte.bncore.models.setting.SettingService;
import me.pugabyte.bncore.utils.StringUtils;
import me.pugabyte.bncore.utils.Utils;
import me.pugabyte.bncore.utils.WorldEditUtils;
import me.pugabyte.bncore.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Set;

public class HoneyPots implements Listener {
	public static final String PREFIX = StringUtils.getPrefix("HoneyPots");
	SettingService service = new SettingService();

	public HoneyPots() {
		BNCore.registerListener(this);
	}

	public static String getHP(ProtectedRegion region) {
		return region.getId().replace("hp_", "");
	}

	public static void fixHP(ProtectedRegion region, World world) {
		WorldEditUtils WEUtils = new WorldEditUtils(world);
		String fileName = region.getId().replace("_", "/");
		WEUtils.paste(fileName, WEUtils.toVector(getSchemRegen(region, world).getMinimumPoint()));
	}

	public static ProtectedRegion getSchemRegen(ProtectedRegion region, World world) {
		String name = region.getId().replace("hp_", "hpregen_");
		return new WorldGuardUtils(world).getProtectedRegion(name);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		//if (event.getPlayer().hasPermission("honeypot.bypass")) return;
		incrementPlayer(event.getPlayer(), event.getBlock().getLocation());
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		//if (event.getPlayer().hasPermission("honeypot.bypass")) return;
		incrementPlayer(event.getPlayer(), event.getBlock().getLocation());
	}

	@EventHandler
	public void onEntityKill(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player)) return;
		if (!(event.getEntity() instanceof Animals)) return;
		Player player = (Player) event.getDamager();
		//if (player.hasPermission("honeypot.bypass")) return;
		incrementPlayer(player, event.getEntity().getLocation());
	}

	public void incrementPlayer(Player player, Location location) {
		WorldGuardUtils WGUtils = new WorldGuardUtils(location.getWorld());
		Set<ProtectedRegion> regions = WGUtils.getRegionsAt(location);
		for (ProtectedRegion region : regions) {
			if (!region.getId().contains("hp_")) continue;
			Setting setting = service.get(player, "hpTriggered");
			int triggered;
			try {
				triggered = Integer.parseInt(setting.getValue()) + 1;
			} catch (NumberFormatException ex) {
				triggered = 1;
				Chat.broadcast(PREFIX + "&e" + player.getName() + " &3has triggered a Honey Pot &e(HP: " + getHP(region) + ")", "Staff");
			}
			if (triggered > 9) {
				Utils.runConsoleCommand("sudo " + player.getName() + " ticket [HoneyPot] Grief trap triggered! " +
						"Please make sure the area has been fully repaired, and take the blocks from their inventory. " +
						"(HP: " + getHP(region) + ")");
				fixHP(region, player.getWorld());
				triggered = 0;
				Utils.runConsoleCommand("ban " + player.getName() + " 10h You have been automatically banend " +
						"by a grief trap. Griefing is not allowed! (HP: " + getHP(region) + ")");
			}
			setting.setValue(triggered + "");
			service.save(setting);
		}
	}

}
