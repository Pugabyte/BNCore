package me.pugabyte.bncore.features.discord.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.pugabyte.bncore.features.discord.DiscordId.Channel;
import me.pugabyte.bncore.features.discord.DiscordId.Role;

import static me.pugabyte.bncore.utils.Utils.runConsoleCommand;

public class ClearChatCommand extends Command {

	public ClearChatCommand() {
		this.name = "clearchat";
		this.aliases = new String[]{"cc"};
		this.requiredRole = Role.STAFF.name();
		this.guildOnly = true;
	}

	protected void execute(CommandEvent event) {
		if (!event.getChannel().getId().equals(Channel.STAFF_BRIDGE.getId()))
			return;

		runConsoleCommand("clearchat");
	}


}
