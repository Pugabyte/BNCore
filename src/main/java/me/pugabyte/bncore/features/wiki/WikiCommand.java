package me.pugabyte.bncore.features.wiki;

import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Arg;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;

public class WikiCommand extends CustomCommand {

	WikiCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void help() {
		reply("&eVisit our wiki at &3https://wiki.bnn.gg");
		reply("&eOr use &c/wiki search <query> &eto search the wiki from ingame.");
	}

	@Path("search <query...>")
	void search(@Arg String search) {
		Wiki.search(sender(), search.split(" "), "Wiki");
	}
}
