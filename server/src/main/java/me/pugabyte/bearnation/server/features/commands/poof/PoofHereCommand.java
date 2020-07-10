package me.pugabyte.bearnation.server.features.commands.poof;

import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.server.models.poof.Poof;
import me.pugabyte.bearnation.server.models.poof.PoofService;
import org.bukkit.entity.Player;

@Aliases("tpahere")
public class PoofHereCommand extends CustomCommand {
	PoofService service = new PoofService();

	public PoofHereCommand(CommandEvent event) {
		super(event);
	}

	@Path("<player>")
	void player(Player target) {
		if (target == player().getPlayer())
			error("You cannot poof to yourself");

		Poof request = new Poof(player(), target, Poof.PoofType.POOF_HERE);
		service.save(request);
		send(json("&ePoof-here &3request sent to " + target.getName() + ". ").next("&eClick to cancel").command("poof cancel"));
		send(target, "  &e" + player().getName() + " &3is asking you to poof &eto them&3.");
		send(target, json("&3  Click one  ||  &a&lAccept")
				.command("/poof accept")
				.hover("&eClick &3to accept")
				.group()
				.next("  &3||  &3")
				.group()
				.next("&c&lDeny")
				.command("/poof deny")
				.hover("&eClick &3to deny.")
				.group()
				.next("&3  ||"));

	}

	@Path("accept")
	void accept() {
		new PoofCommand().accept(player());
	}

	@Path("deny")
	void deny() {
		new PoofCommand().deny(player());
	}

	@Path("cancel")
	void cancel() {
		new PoofCommand().cancel(player());
	}

}
