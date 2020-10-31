package me.pugabyte.bncore.features.commands;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.models.statusbar.StatusBar;
import me.pugabyte.bncore.models.statusbar.StatusBarService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

@NoArgsConstructor
public class StatusBarCommand extends CustomCommand implements Listener {
	private final StatusBarService service = new StatusBarService();
	private StatusBar statusBar;

	public StatusBarCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayer())
			statusBar = service.get(player());
	}

	@Path("[on|off]")
	void run(Boolean enable) {
		if (enable == null)
			enable = !statusBar.isEnabled();

		statusBar.setEnabled(enable);
		service.save(statusBar);

		if (statusBar.isEnabled())
			statusBar.start();
		else
			statusBar.stop();

		send(PREFIX + (enable ? "&aEnabled" : "&cDisabled"));
	}

	@Override
	public void _shutdown() {
		Map<UUID, StatusBar> cache = new StatusBarService().getCache();
		cache.values().forEach(StatusBar::stop);
	}

	static {
		StatusBarService service = new StatusBarService();
		Bukkit.getOnlinePlayers().forEach(player -> {
			StatusBar compass = service.get(player);
			compass.start();
		});
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		StatusBar compass = service.get(event.getPlayer());
		compass.start();
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		StatusBar compass = service.get(event.getPlayer());
		compass.stop();
	}

}