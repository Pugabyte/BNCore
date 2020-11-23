package me.pugabyte.nexus.features.warps.commands;

import lombok.NoArgsConstructor;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.warps.Warp;
import me.pugabyte.nexus.models.warps.WarpService;
import me.pugabyte.nexus.models.warps.WarpType;
import me.pugabyte.nexus.utils.JsonBuilder;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor
public abstract class _WarpCommand extends CustomCommand {
	WarpService service = new WarpService();

	public _WarpCommand(CommandEvent event) {
		super(event);
	}

	public abstract WarpType getWarpType();

	@Path("(list|warps) [filter]")
	public void list(@Arg(tabCompleter = Warp.class) String filter) {
		List<String> warps = tabCompleteWarp(filter);
		JsonBuilder builder = new JsonBuilder();
		for (String warp : warps) {
			if (!builder.isInitialized())
				builder.initialize();
			else
				builder.next("&e, ").group();

			builder.next("&3" + warp)
					.command(getAliasUsed() + " tp " + warp)
					.group();
		}
		line();
		send(PREFIX + "&3List of warps &e(Click to teleport)");
		send(builder);
	}

	@Path("set <name>")
	@Permission(value = "group.staff", absolute = true)
	public void set(@Arg(tabCompleter = Warp.class) String name) {
		Warp warp = service.get(name, getWarpType());
		if (warp != null)
			error("That warp is already set.");

		service.save(new Warp(name, player().getLocation(), getWarpType().name()));
		send(PREFIX + "&e" + name + " &3set to your current location");
	}

	@Path("reset <name>")
	@Permission(value = "group.staff", absolute = true)
	public void reset(@Arg(tabCompleter = Warp.class) String name) {
		service.save(new Warp(name, player().getLocation(), getWarpType().name()));
		send(PREFIX + "&e" + name + " &3set to your current location");
	}

	@Path("(rm|remove|delete|del) <name>")
	@Permission(value = "group.staff", absolute = true)
	public void delete(Warp warp) {
		service.delete(warp);
		send(PREFIX + "Successfully deleted warp &e" + warp.getName());
	}

	@Path("(teleport|tp|warp) <name>")
	public void teleport(Warp warp) {
		warp.teleport(player());
		send(PREFIX + "&3Warping to &e" + warp.getName());
	}

	@Path("<name>")
	public void tp(Warp warp) {
		teleport(warp);
	}

	@Path("tp nearest")
	public void teleportNearest() {
		getNearestWarp(player().getLocation()).ifPresent(this::teleport);
	}

	@Path("nearest")
	public void nearest() {
		Optional<Warp> warp = getNearestWarp(player().getLocation());
		if (!warp.isPresent())
			error("No nearest warp found");
		send(PREFIX + "Nearest warp is &e" + warp.get().getName() + " &3(&e" + (int) warp.get().getLocation().distance(player().getLocation()) + " &3blocks away)");
	}

	public Optional<Warp> getNearestWarp(Location location) {
		Warp nearest = null;
		double distance = Double.MAX_VALUE;
		for (Warp warp : new WarpService().getWarpsByType(getWarpType())) {
			if (location.getWorld() != warp.getLocation().getWorld()) continue;
			double _distance = location.distance(warp.getLocation());
			if (_distance < distance) {
				distance = _distance;
				nearest = warp;
			}
		}
		return Optional.ofNullable(nearest);
	}

	@ConverterFor(Warp.class)
	Warp convertToWarp(String value) {
		if ("skyblock".equalsIgnoreCase(value))
			error("&cSkyblock is currently disabled while we update it");

		Warp warp = service.get(value, getWarpType());
		if (warp == null) error("That warp is not set");
		return warp;
	}

	@TabCompleterFor(Warp.class)
	List<String> tabCompleteWarp(String filter) {
		return service.getWarpsByType(getWarpType()).stream()
				.filter(warp -> filter == null || warp.getName().toLowerCase().startsWith(filter.toLowerCase()))
				.map(Warp::getName)
				.collect(Collectors.toList());
	}

}