package me.pugabyte.nexus.features.justice.misc;

import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import eden.utils.TimeUtils.Timespan.FormatType;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.nexus.features.chat.Chat.Broadcast;
import me.pugabyte.nexus.features.justice.Justice;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.hours.Hours;
import me.pugabyte.nexus.models.hours.HoursService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Aliases("ld")
@NoArgsConstructor
@Permission("group.moderator")
public class LockdownCommand extends CustomCommand implements Listener {
	private static boolean lockdown = false;
	private static String reason = null;
	private static LocalDateTime end = null;
	private static final Set<UUID> bypass = new HashSet<>();

	public LockdownCommand(@NonNull CommandEvent event) {
		super(event);
		PREFIX = Justice.PREFIX;
		DISCORD_PREFIX = Justice.DISCORD_PREFIX;
	}

	static {
		Tasks.repeat(Time.SECOND, Time.SECOND, () -> {
			if (!lockdown || LockdownCommand.end == null)
				return;

			if (LockdownCommand.end.isBefore(LocalDateTime.now()))
				PlayerUtils.runCommandAsConsole("lockdown end");
		});
	}

	@Path("start <time/reason...>")
	void start(String input) {
		if (lockdown) {
			send(PREFIX + "Overriding previous lockdown: &c" + LockdownCommand.reason);
			reason = null;
			end = null;
		}

		lockdown = true;
		Timespan timespan = Timespan.find(input);
		LockdownCommand.reason = timespan.getRest();
		if (timespan.getOriginal() > 0)
			LockdownCommand.end = timespan.fromNow();

		String message = "&c" + name() + " initiated lockdown for &e" + (timespan.isNull() ? "" : timespan.format(FormatType.LONG) + "&c for &e") + timespan.getRest();
		broadcast(message);

		for (Player player : PlayerUtils.getOnlinePlayers())
			if (!canBypass(player.getUniqueId())) {
				player.kick(getLockdownReason());
				broadcast("Removed " + player.getName() + " from server");
			}
	}

	@Path("end")
	void end() {
		if (!lockdown)
			error("Lockdown not enabled");

		lockdown = false;
		reason = null;
		end = null;
		bypass.clear();

		if (isPlayer())
			broadcast(name() + " ended lockdown");
		else
			broadcast("Lockdown expired");
	}

	@Path("bypass add <player>")
	void bypassAdd(OfflinePlayer player) {
		bypass.add(player.getUniqueId());
		send(PREFIX + "Added " + player.getName() + " to bypass list");
	}

	@Path("bypass remove <player>")
	void bypassRemove(OfflinePlayer player) {
		bypass.remove(player.getUniqueId());
		send(PREFIX + "Removed " + player.getName() + " from bypass list");
	}

	@EventHandler
	public void onConnect(AsyncPlayerPreLoginEvent event) {
		if (lockdown && event.getLoginResult() == Result.ALLOWED)
			if (!canBypass(event.getUniqueId())) {
				event.disallow(Result.KICK_OTHER, getLockdownReason());
				broadcast("Prevented " + event.getName() + " from joining the server");
			}
	}

	private void broadcast(String message) {
		Broadcast.log().prefix("Justice").message(message).send();
	}

	public boolean canBypass(UUID player) {
		if (bypass.contains(player))
			return true;

		Hours hours = new HoursService().get(player);
		return hours.getTotal() > (30 * 60);
	}

	private Component getLockdownReason() {
		return new JsonBuilder()
				.next("&e&lProject Eden &3&lis in &4&llockdown &3&lmode")
				.newline()
				.newline()
				.next("&eReason: &c" + reason)
				.newline()
				.newline()
				.next("&3Please check back soon!")
				.build();
	}

}
