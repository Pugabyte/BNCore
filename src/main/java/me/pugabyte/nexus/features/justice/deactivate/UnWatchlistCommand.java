package me.pugabyte.nexus.features.justice.deactivate;

import lombok.NonNull;
import me.pugabyte.nexus.features.justice.misc._PunishmentCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.punishments.PunishmentType;
import me.pugabyte.nexus.models.punishments.Punishments;

import java.util.List;

@Permission("group.moderator")
public class UnWatchlistCommand extends _PunishmentCommand {

	public UnWatchlistCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<player>")
	void run(@Arg(type = Punishments.class) List<Punishments> players) {
		deactivate(players);
	}

	@Override
	protected PunishmentType getType() {
		return PunishmentType.WATCHLIST;
	}

}
