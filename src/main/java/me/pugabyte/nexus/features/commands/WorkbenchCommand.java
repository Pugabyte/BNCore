package me.pugabyte.nexus.features.commands;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.WorldGroup;

import static me.pugabyte.nexus.features.commands.WorkbenchCommand.PERMISSION;

@Aliases("craft")
@Permission(PERMISSION)
public class WorkbenchCommand extends CustomCommand {
	public static final String PERMISSION = "workbench";

	public WorkbenchCommand(CommandEvent event) {
		super(event);
	}

	@Path
	public void workbench() {
		if (worldGroup() == WorldGroup.EVENTS)
			permissionError();

		player().openWorkbench(null, true);
	}
}
