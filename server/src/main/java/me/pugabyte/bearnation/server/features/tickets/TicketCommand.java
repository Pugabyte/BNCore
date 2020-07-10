package me.pugabyte.bearnation.server.features.tickets;

import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Cooldown;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.api.models.nerd.Nerd;
import me.pugabyte.bearnation.api.models.nerd.Rank;
import me.pugabyte.bearnation.api.utils.SoundUtils.Jingle;
import me.pugabyte.bearnation.api.utils.StringUtils;
import me.pugabyte.bearnation.api.utils.Time;
import me.pugabyte.bearnation.api.utils.Utils;
import me.pugabyte.bearnation.features.chat.Chat;
import me.pugabyte.bearnation.features.chat.Chat.StaticChannel;
import me.pugabyte.bearnation.features.discord.Discord;
import me.pugabyte.bearnation.server.models.ticket.Ticket;
import me.pugabyte.bearnation.server.models.ticket.TicketService;

import java.util.Arrays;
import java.util.List;

import static me.pugabyte.bearnation.api.utils.StringUtils.stripColor;

@Cooldown(@Part(Time.MINUTE))
@Aliases({"helpop", "report"})
public class TicketCommand extends CustomCommand {
	private TicketService service = new TicketService();

	public TicketCommand(CommandEvent event) {
		super(event);
		PREFIX = Tickets.PREFIX;
	}

	@Path
	void help() {
		send("&3To request &ehelp &3or report &egrief&3, stand at the &erelevant location &3and open a &c/ticket " +
				"&3with an &einformative description &3of the issue.");
		send("&3Please be &epatient&3, as staff can be very busy!");
	}

	@Path("<description...>")
	void ticket(String description) {
		if (Arrays.asList("help", "info", "pls", "plz", "please").contains(description))
			error("Please make a ticket with a more informative description of the problem");

		if (Utils.isInt(description))
			error("Prevented accidental ticket");

		if (StringUtils.right(description, 5).equalsIgnoreCase("close"))
			error("Prevented accidental ticket (close)");

		Ticket ticket = new Ticket(player(), stripColor(description));
		service.saveSync(ticket);

		send(PREFIX + "You have submitted a ticket. Staff have been alerted, please wait patiently for a response. &eThank you!");
		send(" &eYour ticket (&c#" + ticket.getId() + "&e): &3" + ticket.getDescription());

		List<Nerd> onlineMods = Rank.getOnlineMods();
		Discord.staffLog("**[Tickets]** " + player().getName() + " (" + ticket.getId() + "): " + ticket.getDescription());
		Discord.staffBridge("**[Tickets]** " + player().getName() + " (" + ticket.getId() + "): " + ticket.getDescription() + (onlineMods.size() == 0 ? " [ @here ]" : ""));

		onlineMods.forEach(mod -> Jingle.PING.play(mod.getPlayer()));
		Chat.broadcastIngame("", StaticChannel.STAFF);
		Chat.broadcastIngame(PREFIX + "&e" + player().getName() + " &3opened ticket &c#" + ticket.getId() + "&3: &e" + ticket.getDescription(), StaticChannel.STAFF);
		Chat.broadcastIngame(Tickets.getTicketButtons(ticket), StaticChannel.STAFF);
	}

}
