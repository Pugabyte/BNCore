package me.pugabyte.nexus.features.commands.staff.admin;

import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.features.chat.Chat.StaticChannel;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Nerd.StaffMember;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Set;

@Permission("group.admin")
public class OpCommand extends CustomCommand {

	public OpCommand(CommandEvent event) {
		super(event);
	}

	@Path("<player>")
	public void op(StaffMember staffMember) {
		OfflinePlayer player = staffMember.getOfflinePlayer();
		Nerd nerd = Nerd.of(player);

		String oper = name();
		String opee = nerd.getName();

		if (!nerd.getRank().isStaff())
			error(opee + " is not staff");

		if (player.isOp())
			error(opee + " is already op");

		player.setOp(true);
		if (player.equals(player()))
			Chat.broadcastIngame(PREFIX + oper + " opped themselves", StaticChannel.ADMIN);
		else
			Chat.broadcastIngame(PREFIX + oper + " opped " + opee, StaticChannel.ADMIN);

		send(player, PREFIX + "You are now op");

	}

	@Path("list")
	public void list() {
		Set<OfflinePlayer> ops = Bukkit.getOperators();
		if (ops.isEmpty())
			error("There are no server operators");

		send(PREFIX + "Ops:");
		for (OfflinePlayer operator : ops)
			send(" &7- &3" + operator.getName());
	}
}
