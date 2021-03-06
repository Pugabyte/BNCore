package me.pugabyte.nexus.features.afk;

import eden.utils.TimeUtils.Timespan;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import org.bukkit.entity.Player;

@Aliases("timeafk")
public class AFKTimeCommand extends CustomCommand {

	public AFKTimeCommand(CommandEvent event) {
		super(event);
	}

	@Path("[player]")
	void timeAfk(@Arg("self") Player player) {
		String timespan = Timespan.of(AFK.get(player).getTime()).format();
		send(PREFIX + "&3" + nickname(player) + " has been AFK for &e" + timespan);
	}

}
