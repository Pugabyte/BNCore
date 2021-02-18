package me.pugabyte.nexus.features.wiki;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;

@Aliases("minecraftwiki")
public class MCWikiCommand extends CustomCommand {

	MCWikiCommand(CommandEvent event) {
		super(event);
	}

	@Path
	@Override
	public void help() {
		send("&eVisit the minecraft wiki at &3https://minecraft.gamepedia.com/");
		send("&eOr use &c/mcwiki search <query> &eto search the wiki from in-game.");
	}

	@Path("search <query...>")
	void search(String search) {
		Wiki.search(sender(), search.split(" "), "MCWiki");
	}
}
