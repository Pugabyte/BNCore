package me.pugabyte.nexus.features.commands;

import lombok.NonNull;
import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Aliases("nearby")
@Description("View nearby players and their distance from you. Players in this list can see your local chat.")
public class NearCommand extends CustomCommand {

	public NearCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("[player]")
	void run(@Arg(value = "self", permission = "group.staff") Player player) {
		if (PlayerManager.get(player).isPlaying())
			error("This command cannot be used during Minigames");

		UUID uuid = player.getUniqueId();
		List<Player> nearby = PlayerUtils.getOnlinePlayers(player.getWorld()).stream()
				.filter(_player -> !uuid.equals(_player.getUniqueId())
						 && getDistance(player, _player) <= Chat.getLocalRadius()
						 && (!isPlayer() || PlayerUtils.canSee(player(), _player)))
				.collect(Collectors.toList());

		boolean showDistance = player.hasPermission("near.distance");

		String message = "&ePlayers nearby" + (isSelf(player) ? "" : " " + player.getName()) + "";
		if (nearby.size() == 0)
			send(message + ": &fNone");
		else
			send(message + " (&3" + nearby.size() + "&e): &f" + nearby.stream()
					.map(_player -> {
						if (showDistance)
							return Nickname.of(_player) + " (&3" + getDistance(player, _player) + "m&f)";
						else
							return Nickname.of(_player);
					})
					.collect(Collectors.joining(", ")));
	}

	private long getDistance(@Arg("self") Player player, Player _player) {
		return Math.round(player.getLocation().distance(_player.getLocation()));
	}

}
