package me.pugabyte.nexus.features.chat.alerts;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.alerts.Alerts;
import me.pugabyte.nexus.models.alerts.AlertsService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class AlertsCommand extends CustomCommand {
	private final AlertsService service = new AlertsService();
	private Alerts alerts;

	public AlertsCommand(CommandEvent event) {
		super(event);
		if (sender() instanceof Player)
			alerts = service.get(player());
	}

	@Path
	void main() {
		new JsonBuilder()
				.next("&3Receive a &e'ping' noise &3whenever a word or phrase in your &c/alerts list &3is said in chat. ")
				.next("&3Make sure you have your 'Players' sound on!")
				.line()
				.next("&3You can edit your alerts with the following commands:")
				.newline()
				.next("&c /alerts list").suggest("/alerts list ")
				.newline()
				.next("&c /alerts add <word or phrase>").suggest("/alerts add ")
				.newline()
				.next("&c /alerts delete <word or phrase>").suggest("/alerts delete ")
				.newline()
				.next("&c /alerts clear").suggest("/alerts clear")
				.newline()
				.next("&c /alerts <mute|unmute|toggle>").suggest("/alerts toggle")
				.send(player());
	}

	@Path("(list|edit)")
	void list() {
		if (alerts.getHighlights().size() == 0)
			error("&3You don't have any alerts! Add some with &c/alerts add <word or phrase>");

		send(PREFIX + "&eYour alerts list:");
		for (Alerts.Highlight highlight : alerts.getHighlights()) {

			JsonBuilder builder = new JsonBuilder();

			if (highlight.isPartialMatching()) {
				builder.next("&a✔")
						.command("/alerts partialmatch false " + highlight.getHighlight())
						.hover("&cClick to turn off partial matching");
			} else {
				builder.next("&c✕ ")
						.command("/alerts partialmatch true " + highlight.getHighlight())
						.hover("&cClick to turn on partial matching");
			}

			builder.next(" &3" + highlight.getHighlight()).send(player());
		}
	}

	@Path("add <highlight...>")
	void add(String highlight) {
		if (highlight.equalsIgnoreCase(name()))
			error("Your name is automatically included in your alerts list");
		if (highlight.equalsIgnoreCase(nickname()))
			error("Your nickname is automatically included in your alerts list");

		if (!alerts.add(highlight))
			error("You already had &e" + highlight + " &3in your alerts list");

		service.save(alerts);
		send(PREFIX + "Added &e" + highlight + " &3to your alerts list");
	}

	@Path("delete <highlight...>")
	void delete(String highlight) {
		if (!alerts.delete(highlight))
			error("You did not have &e" + highlight + " &3in your alerts list");

		service.save(alerts);
		send(PREFIX + "Removed &e" + highlight + " &3from your alerts list");
	}

	@Path("(partialmatch|partialmatching) <truse|false> [highlight...]")
	void partialMatching(boolean partialMatching, String highlight) {
		Optional<Alerts.Highlight> match = alerts.get(highlight);
		if (!match.isPresent())
			error("I could not find that alert in your alerts list");

		alerts.delete(highlight);
		alerts.add(highlight, partialMatching);
		service.save(alerts);
		line();
		send(PREFIX + "Partial matching for alert " + ChatColor.YELLOW + highlight + ChatColor.DARK_AQUA + " "
				+ (partialMatching ? "enabled" : "disabled"));
		line();
		Tasks.wait(2, () -> PlayerUtils.runCommand(player(), "alerts edit"));
	}

	@Path("clear")
	void clear() {
		alerts.clear();
		service.save(alerts);
		send(PREFIX + "Cleared your alerts");
	}

	@Path("mute")
	void mute() {
		alerts.setMuted(true);
		send(PREFIX + "Alerts muted");
	}

	@Path("unmute")
	void unmute() {
		alerts.setMuted(false);
		send(PREFIX + "Alerts unmuted");
	}

	@Path("toggle")
	void toggle() {
		alerts.setMuted(!alerts.isMuted());
		send(PREFIX + "Alerts " + (alerts.isMuted() ? "" : "un") + "muted");
	}

	@Path("sound")
	void sound() {
		alerts.playSound();
		send(PREFIX + "Test sound sent");
	}

}
