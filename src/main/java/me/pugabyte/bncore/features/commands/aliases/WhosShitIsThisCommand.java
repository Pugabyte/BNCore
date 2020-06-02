package me.pugabyte.bncore.features.commands.aliases;

import me.pugabyte.bncore.framework.annotations.Disabled;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;

@Disabled // Core doesnt log drops :(
public class WhosShitIsThisCommand extends CustomCommand {

	public WhosShitIsThisCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		runCommand("pr l a:drop r:10 t:10000d");
	}

}
