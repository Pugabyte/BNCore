package me.pugabyte.nexus.features.commands.staff.moderator.punishments;

import lombok.NonNull;
import me.pugabyte.nexus.framework.annotations.Environments;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.punishments.Punishments;
import me.pugabyte.nexus.models.punishments.Punishments.Punishment;
import me.pugabyte.nexus.models.punishments.Punishments.Punishment.PunishmentType;
import me.pugabyte.nexus.utils.Env;

import java.util.List;
import java.util.Optional;

@Environments(Env.DEV)
@Permission("group.moderator")
public class UnbanCommand extends _PunishmentCommand {

	public UnbanCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<player>")
	void run(@Arg(type = Punishments.class) List<Punishments> players) {
		for (Punishments player : players) {
			Optional<Punishment> activeBan = player.getActiveBan();
			activeBan.ifPresent(ban -> ban.deactivate(uuid()));
		}
	}

	@Override
	protected PunishmentType getType() {
		return PunishmentType.BAN;
	}

}