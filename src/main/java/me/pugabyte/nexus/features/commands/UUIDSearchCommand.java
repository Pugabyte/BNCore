package me.pugabyte.nexus.features.commands;

import eden.annotations.Disabled;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.NerdService;

import java.util.ArrayList;
import java.util.List;

@Disabled
public class UUIDSearchCommand extends CustomCommand {
	private final NerdService service = new NerdService();

	public UUIDSearchCommand(CommandEvent event) {
		super(event);
	}

	@Path("<uuid> [amount]")
	void search(String search, @Arg("25") int limit) {
		if (search.length() < 4)
			error("Please be more specific!");

//		List<Nerd> nerds = service.find(search, "uuid").stream().limit(limit).collect(Collectors.toList());
		List<Nerd> nerds = new ArrayList<>();
		if (nerds.size() == 0)
			error("No matches found for &e" + search);

		send("&3Matches for '&e" + search + "&3' (&e" + nerds.size() + "&3):");
		for (Nerd nerd : nerds)
			send(json("&e" + nerd.getUuid() + " (" + nerd.getName() + ")").insert(nerd.getUuid().toString()));
		send("&3Shift+Click on a name to insert it into your chat");
	}

}
