package me.pugabyte.nexus.features.commands;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import org.bukkit.entity.Player;

public class QuickActionCommand extends CustomCommand {

	public QuickActionCommand(CommandEvent event) {
		super(event);
	}

	@Path("<player>")
	void quickAction(Player player) {
		line();
		String playerName = player.getName();
		send("&8&l[&eQuickAction&8&l] &6&l" + playerName);
		line();
		send(json()
				.next("  &3|&3|  ")
				.next("&ePoof Request").suggest("/poof " + playerName).group()
				.next("&3  ||  &3")
				.next("&ePoof Here Request").suggest("/poofhere " + playerName).group()
				.next("  &3||"));

		send(json()
				.next("  &3||  &3")
				.next(" &eMessage").suggest("/msg " + playerName + " ").group()
				.next("&3  ||  &3")
				.next("&eTrust").command("/trust " + playerName).group()
				.next("  &3||  &3")
				.next("&eShop").suggest("/shop " + playerName).group()
				.next("  &3||  &3")
				.next("&ePay").suggest("/pay " + playerName + " 10").group()
				.next("  &3||"));
		line();
	}
}