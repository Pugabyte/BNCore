package me.pugabyte.nexus.features.store.perks;

import eden.utils.TimeUtils.Time;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.rainbowbeacon.RainbowBeacon;
import me.pugabyte.nexus.models.rainbowbeacon.RainbowBeaconService;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static me.pugabyte.nexus.features.store.perks.RainbowBeaconCommand.PERMISSION;

@NoArgsConstructor
@Permission(PERMISSION)
public class RainbowBeaconCommand extends CustomCommand implements Listener {
	public static final String PERMISSION = "rainbow.beacon";
	private final RainbowBeaconService service = new RainbowBeaconService();
	private RainbowBeacon rainbowBeacon;


	public RainbowBeaconCommand(CommandEvent event) {
		super(event);
		rainbowBeacon = service.get(player());
	}

	@Path("start [player]")
	void activate(@Arg(value = "self", permission = "group.seniorstaff") RainbowBeacon rainbowBeacon) {
		if (rainbowBeacon.getTaskId() != null)
			error(formatWho(rainbowBeacon, WhoType.POSSESSIVE_UPPER) + " rainbow beacon is already activated");

		if (rainbowBeacon.getLocation() == null)
			if (location().clone().subtract(0, 1, 0).getBlock().getType().equals(Material.BEACON)) {
				rainbowBeacon.setLocation(location().getBlock().getLocation());
				service.save(rainbowBeacon);
			} else
				error("You must be standing on a beacon");

		startTask(rainbowBeacon);
		send(PREFIX + "Activated " + formatWho(rainbowBeacon, WhoType.POSSESSIVE_LOWER) + " rainbow beacon");
	}

	@Path("stop [player]")
	void stop(@Arg(value = "self", permission = "group.seniorstaff") RainbowBeacon rainbowBeacon) {
		if (rainbowBeacon.getTaskId() == null)
			error(formatWho(rainbowBeacon, WhoType.ACTIONARY_UPPER) + " not have a running rainbow beacon");
		Tasks.cancel(rainbowBeacon.getTaskId());
		rainbowBeacon.getLocation().getBlock().setType(Material.AIR);
		send(PREFIX + "Successfully deactivated " +  formatWho(rainbowBeacon, WhoType.POSSESSIVE_LOWER) + " rainbow beacon");
	}

	@Path("delete [player]")
	void delete(@Arg(value = "self", permission = "group.seniorstaff") RainbowBeacon rainbowBeacon) {
		if (rainbowBeacon.getLocation() == null)
			error(formatWho(rainbowBeacon, WhoType.ACTIONARY_UPPER) + " not have a rainbow beacon set");

		if (rainbowBeacon.getTaskId() != null)
			Tasks.cancel(rainbowBeacon.getTaskId());

		rainbowBeacon.getLocation().getBlock().setType(Material.AIR);
		service.delete(rainbowBeacon);
		send(PREFIX + "Successfully deleted " + formatWho(rainbowBeacon, WhoType.POSSESSIVE_LOWER) + " rainbow beacon");
	}

	@Path("tp [player]")
	void tp(@Arg(value = "self", permission = "group.seniorstaff") RainbowBeacon rainbowBeacon) {
		if (rainbowBeacon.getLocation() == null)
			error(formatWho(rainbowBeacon, WhoType.ACTIONARY_UPPER) + " not have an active rainbow beacon");

		player().teleport(rainbowBeacon.getLocation(), TeleportCause.COMMAND);
	}

	@Path("list")
	@Permission(value = "group.seniorstaff", absolute = true)
	void list() {
		if (service.getCache().values().size() == 0)
			error("No active rainbow beacons");

		send(PREFIX + "Active beacons:");
		for (RainbowBeacon rainbowBeacon : service.getCache().values())
			send("&e" + rainbowBeacon.getOfflinePlayer().getName() + " &7- " + (rainbowBeacon.getTaskId() == null ? "Inactive" : "&aActive"));
	}

	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		for (RainbowBeacon rainbowBeacon : service.getCache().values())
			if (event.getBlock().getLocation().equals(rainbowBeacon.getLocation())) {
				event.setCancelled(true);
				break;
			}
	}

	static {
		Tasks.wait(1, () -> {
			RainbowBeaconService service = new RainbowBeaconService();
			List<RainbowBeacon> beacons = service.getAll();
			for (RainbowBeacon rainbowBeacon : beacons) {
				Nexus.log("[RainbowBeacon] Starting task for " + rainbowBeacon.getName());
				startTask(rainbowBeacon);
				service.cache(rainbowBeacon);
			}
		});
	}

	@Override
	public void _shutdown() {
		final RainbowBeaconService service = new RainbowBeaconService();
		for (RainbowBeacon rainbowBeacon : service.getCache().values())
			if (rainbowBeacon.getLocation() != null)
				rainbowBeacon.getLocation().getBlock().setType(Material.AIR);
	}

	private static final List<Material> colors = new ArrayList<>() {{
		add(Material.RED_STAINED_GLASS_PANE);
		add(Material.ORANGE_STAINED_GLASS_PANE);
		add(Material.YELLOW_STAINED_GLASS_PANE);
		add(Material.LIME_STAINED_GLASS_PANE);
		add(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
		add(Material.BLUE_STAINED_GLASS_PANE);
		add(Material.PURPLE_STAINED_GLASS_PANE);
		add(Material.MAGENTA_STAINED_GLASS_PANE);
	}};

	public static void startTask(RainbowBeacon rainbowBeacon) {
		Location location = rainbowBeacon.getLocation();
		AtomicInteger i = new AtomicInteger(0);
		rainbowBeacon.setTaskId(Tasks.repeat(0, Time.SECOND, () -> {
			if (!location.isChunkLoaded())
				return;

			if (location.getBlock().getRelative(BlockFace.DOWN).getType() != Material.BEACON) {
				Tasks.cancel(rainbowBeacon.getTaskId());
				rainbowBeacon.setTaskId(null);
				return;
			}

			location.getBlock().setType(colors.get(i.getAndIncrement()));
			if (i.get() == 8)
				i.set(0);
		}));
	}

}
