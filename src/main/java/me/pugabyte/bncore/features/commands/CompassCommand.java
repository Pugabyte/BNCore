package me.pugabyte.bncore.features.commands;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.models.compass.Compass;
import me.pugabyte.bncore.models.compass.CompassService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

@NoArgsConstructor
public class CompassCommand extends CustomCommand implements Listener {
	private final CompassService service = new CompassService();
	private Compass compass;

	public CompassCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayer())
			compass = service.get(player());
	}

	@Path("[on|off]")
	void run(Boolean enable) {
		if (enable == null)
			enable = !compass.isEnabled();

		compass.setEnabled(enable);
		service.save(compass);

		if (compass.isEnabled())
			compass.start();
		else
			compass.stop();

		send(PREFIX + (enable ? "&aEnabled" : "&cDisabled"));
	}

	@Override
	public void _shutdown() {
		Map<UUID, Compass> cache = new CompassService().getCache();
		cache.values().forEach(Compass::stop);
	}

	static {
		CompassService service = new CompassService();
		Bukkit.getOnlinePlayers().forEach(player -> {
			Compass compass = service.get(player);
			compass.start();
		});
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Compass compass = service.get(event.getPlayer());
		compass.start();
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Compass compass = service.get(event.getPlayer());
		compass.stop();
	}

}
