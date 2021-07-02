package me.pugabyte.nexus.features.chat.commands;


import lombok.NonNull;
import me.pugabyte.nexus.features.chat.Censor;
import me.pugabyte.nexus.features.chat.events.ChatEvent;
import me.pugabyte.nexus.features.chat.events.PublicChatEvent;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.chat.ChatterService;
import me.pugabyte.nexus.models.chat.PublicChannel;

import java.util.HashSet;

@Permission("group.seniorstaff")
public class CensorCommand extends CustomCommand {

	public CensorCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("process <channel> <message...>")
	void process(PublicChannel channel, String message) {
		ChatEvent event = new PublicChatEvent(new ChatterService().get(player()), channel, message, message, new HashSet<>());
		Censor.process(event);
		send(PREFIX + "Processed message:" + (event.isCancelled() ? " &c(Cancelled)" : ""));
		send("&eOriginal: &f" + event.getOriginalMessage());
		send("&eResult: &f" + event.getMessage());
		send("&eChanged: " + (event.wasChanged() ? "&aYes" : "&cNo"));
	}

	@Path("reload")
	void reload() {
		Censor.reloadConfig();
		send(PREFIX + Censor.getCensorItems().size() + " censor items loaded from disk");
	}

	@Path("debug")
	void debug() {
		send(Censor.getCensorItems().toString());
	}

}
