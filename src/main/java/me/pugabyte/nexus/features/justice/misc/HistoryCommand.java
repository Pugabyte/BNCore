package me.pugabyte.nexus.features.justice.misc;

import lombok.NonNull;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleteIgnore;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.punishments.Punishment;
import me.pugabyte.nexus.models.punishments.Punishments;
import me.pugabyte.nexus.models.punishments.PunishmentsService;
import me.pugabyte.nexus.utils.JsonBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;

@Permission("group.moderator")
public class HistoryCommand extends _JusticeCommand {
	private final PunishmentsService service = new PunishmentsService();

	public HistoryCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<player> [page]")
	void run(Punishments player, @Arg("1") int page) {
		if (player.getPunishments().isEmpty())
			error("No history found");

		send("");
		send(PREFIX + "History of &e" + player.getNickname());

		int perPage = 3;

		BiFunction<Punishment, String, JsonBuilder> formatter = (punishment, index) -> {
			JsonBuilder json = punishment.getType().getHistoryDisplay(punishment);
			int indexInt = Integer.parseInt(index);
			if (indexInt % perPage != 0 && indexInt != player.getPunishments().size())
				json.newline();
			return json;
		};

		List<Punishment> sorted = player.getPunishments().stream()
				.sorted(Comparator.comparing(Punishment::getTimestamp).reversed())
				.collect(toList());

		paginate(sorted, formatter, "/history " + player.getName(), page, perPage);
	}

	@Confirm
	@TabCompleteIgnore
	@Path("delete <player> <id>")
	void delete(Punishments player, @Arg(context = 1) Punishment punishment) {
		player.remove(punishment);
		service.save(player);
		send(PREFIX + "Punishment deleted");
	}

	@ConverterFor(Punishment.class)
	Punishment convertToPunishment(String value, Punishments context) {
		return context.getPunishment(UUID.fromString(value));
	}

}
