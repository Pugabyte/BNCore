package me.pugabyte.bearnation.chat.features.commands;

import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Arg;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;

@Aliases("cls")
public class ClearScreenCommand extends CustomCommand {

	public ClearScreenCommand(CommandEvent event) {
		super(event);
	}

	@Path("[lines]")
	void clearScreen(@Arg("20") Integer lines) {
		for (int i = 0; i < lines; i++) {
			send("");
		}
	}
}
