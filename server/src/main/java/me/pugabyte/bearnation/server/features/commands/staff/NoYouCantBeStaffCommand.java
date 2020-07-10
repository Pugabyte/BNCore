package me.pugabyte.bearnation.server.features.commands.staff;

import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Cooldown;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Permission;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.api.utils.Time;

@Aliases("nycbs")
@Permission("group.staff")
public class NoYouCantBeStaffCommand extends CustomCommand {

	public NoYouCantBeStaffCommand(CommandEvent event) {
		super(event);
	}

	@Path
	@Cooldown(global = true, value = @Part(value = Time.SECOND, x = 30))
	void nycbs() {
		runCommand("ch qm g One of the most basic duties of staff members is to help players. How do you expect to do that if you know *absolutely nothing* about the server?");
	}

}
