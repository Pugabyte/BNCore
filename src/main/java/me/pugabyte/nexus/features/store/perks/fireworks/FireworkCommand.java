package me.pugabyte.nexus.features.store.perks.fireworks;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.FireworkLauncher;

import static me.pugabyte.nexus.features.store.perks.fireworks.FireworkCommand.PERMISSION;

@Aliases("fw")
@Permission(PERMISSION)
@Cooldown(value = @Part(Time.SECOND), bypass = "group.staff")
public class FireworkCommand extends CustomCommand {
	public static final String PERMISSION = "firework.launch";

	public FireworkCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void firework() {
		FireworkLauncher.random(location()).launch();
	}
}
