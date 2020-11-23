package me.pugabyte.nexus.features.commands.ranks;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Rank;

public class TrustedCommand extends CustomCommand {

	public TrustedCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void trusted() {
		line(5);
		send("&3The " + Rank.TRUSTED.getColor() + "Trusted &3rank is awarded to those who are &eactive &3in the community, are &erespectful &3and &ehelpful &3to others, and follow the rules of the server.");
		line();
		RanksCommand.ranksReturn(player());
	}
}