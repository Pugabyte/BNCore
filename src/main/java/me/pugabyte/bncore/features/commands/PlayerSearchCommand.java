package me.pugabyte.bncore.features.commands;

import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Arg;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.models.nerds.Nerd;
import me.pugabyte.bncore.models.nerds.NerdService;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerSearchCommand extends CustomCommand {
	NerdService service = new NerdService();

	public PlayerSearchCommand(CommandEvent event) {
		super(event);
	}

	@Path("<name> [amount]")
	void search(String search, @Arg("25") int limit) {
		List<Nerd> nerds = service.find(search);
		if (nerds.size() == 0)
			error("No matches found for &e" + search);

		send("&3Matches for '&e" + search + "&3':");
		for (Nerd nerd : nerds.stream().limit(limit).collect(Collectors.toList()))
			send(json("&e" + nerd.getName()).insert(nerd.getName()));
		send("&3Shift+Click on a name to insert it into your chat");
	}

}
