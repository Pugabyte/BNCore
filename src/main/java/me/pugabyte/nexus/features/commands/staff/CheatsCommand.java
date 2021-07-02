package me.pugabyte.nexus.features.commands.staff;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.Redirects.Redirect;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.godmode.Godmode;
import me.pugabyte.nexus.models.godmode.GodmodeService;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.GameMode;
import org.bukkit.World.Environment;

@Permission("group.staff")
@Redirect(from = "/nocheats", to = "/cheats off")
@Redirect(from = "/allcheats", to = "/cheats on")
public class CheatsCommand extends CustomCommand {

	public CheatsCommand(CommandEvent event) {
		super(event);
	}

	@Path("<on|off>")
	void on(Boolean enabled) {
		GodmodeService godmodeService = new GodmodeService();
		Godmode godmode = godmodeService.get(player());
		if (enabled) {
			if (player().hasPermission("essentials.gamemode.creative"))
				player().setGameMode(GameMode.CREATIVE);
			if (!world().getEnvironment().equals(Environment.THE_END)) {
				player().setAllowFlight(true);
				player().setFlying(true);
			}
			godmode.setEnabled(true);
			godmodeService.save(godmode);
			runCommand("vanish on");

			send(PREFIX + "&aEnabled");
		} else {
			runCommand("vanish off");
			godmode.setEnabled(false);
			godmodeService.save(godmode);
			if (WorldGroup.of(player()) != WorldGroup.CREATIVE) {
				if (!world().getEnvironment().equals(Environment.THE_END)) {
					player().setAllowFlight(false);
					player().setFlying(false);
				}
				player().setGameMode(GameMode.SURVIVAL);
				player().setFallDistance(0);
			}
			runCommand("wgedit off");

			send(PREFIX + "&cDisabled");
		}
	}
}
