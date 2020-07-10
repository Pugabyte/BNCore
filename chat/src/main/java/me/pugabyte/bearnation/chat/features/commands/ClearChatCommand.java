package me.pugabyte.bearnation.chat.features.commands;

import lombok.NonNull;
import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Permission;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.features.chat.Koda;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Aliases("cc")
@Permission("group.staff")
public class ClearChatCommand extends CustomCommand {

	public ClearChatCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		for (Player player : Bukkit.getOnlinePlayers())
			if (!player.hasPermission("group.staff"))
				line(player, 40);

		Koda.say("Chat has been cleared, sorry for any inconvenience.");
	}


}
