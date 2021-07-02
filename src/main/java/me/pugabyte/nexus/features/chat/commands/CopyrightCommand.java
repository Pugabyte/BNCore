package me.pugabyte.nexus.features.chat.commands;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.ChatterService;

public class CopyrightCommand extends CustomCommand {
	private final Chatter chatter;

	public CopyrightCommand(CommandEvent event) {
		super(event);
		chatter = new ChatterService().get(player());
	}

	@Path
	void run() {
		chatter.say(argsString() + "©");
	}

}
