package me.pugabyte.bearnation.server.features.commands.staff;

import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Permission;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;

@Aliases("craft")
@Permission("group.staff")
public class WorkbenchCommand extends CustomCommand {

	public WorkbenchCommand(CommandEvent event) {
		super(event);
	}

	@Path
	public void workbench() {
		player().openWorkbench(null, true);
	}
}
