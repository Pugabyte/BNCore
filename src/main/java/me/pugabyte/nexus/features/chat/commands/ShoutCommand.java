package me.pugabyte.nexus.features.chat.commands;

import me.pugabyte.nexus.features.chat.ChatManager;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.chat.ChatService;
import me.pugabyte.nexus.models.chat.Chatter;

public class ShoutCommand extends CustomCommand {
	private Chatter chatter;

	public ShoutCommand(CommandEvent event) {
		super(event);
		chatter = new ChatService().get(player());
	}

	@Path
	void run() {
		chatter.say(ChatManager.getMainChannel(), argsString());
	}
}