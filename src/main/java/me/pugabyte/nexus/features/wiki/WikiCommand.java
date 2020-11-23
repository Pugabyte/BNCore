package me.pugabyte.nexus.features.wiki;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;

public class WikiCommand extends CustomCommand {

	WikiCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void help() {
		send("&eVisit our wiki at &3https://wiki.bnn.gg");
		send("&eOr use &c/wiki search <query> &eto search the wiki from ingame.");
	}

	@Path("search <query...>")
	void search(String search) {
		Wiki.search(sender(), search.split(" "), "Wiki");
	}
}