package me.pugabyte.nexus.features.scoreboard;

import com.gmail.nossr50.events.scoreboard.McMMOScoreboardRevertEvent;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.nexus.features.menus.BookBuilder.WrittenBookMenu;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.HideFromHelp;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleteIgnore;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.scoreboard.ScoreboardService;
import me.pugabyte.nexus.models.scoreboard.ScoreboardUser;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.stream.Collectors;

@NoArgsConstructor
@Aliases({"status", "sidebar", "sb", "featherboard"})
public class ScoreboardCommand extends CustomCommand implements Listener {
	private final ScoreboardService service = new ScoreboardService();
	private ScoreboardUser user;

	static {
		PlayerUtils.getOnlinePlayers().forEach(player -> {
			ScoreboardUser user = new ScoreboardService().get(player);
			if (user.isActive())
				user.on();
		});
	}

	public ScoreboardCommand(@NonNull CommandEvent event) {
		super(event);
		user = service.get(player());
	}

	@Description("Turn the scoreboard on or off")
	@Path("[on/off]")
	void toggle(Boolean enable) {
		user.setActive((enable != null ? enable : !user.isActive()));
		if (!user.isActive())
			user.off();
		else {
			if (user.getLines().isEmpty())
				user.setLines(ScoreboardLine.getDefaultLines(player()));
			user.on();
		}
		service.save(user);
	}

	@Description("Reset settings to default")
	@Path("reset")
	void reset() {
		user.setActive(true);
		user.setLines(ScoreboardLine.getDefaultLines(player()));
		user.on();
		service.save(user);
	}

	@Description("Control which lines you want to see")
	@Path("edit")
	void book() {
		WrittenBookMenu builder = new WrittenBookMenu();

		int index = 0;
		JsonBuilder json = new JsonBuilder();
		for (ScoreboardLine line : ScoreboardLine.values()) {
			if (!line.isOptional()) continue;
			if (!line.hasPermission(player()) && !user.getLines().containsKey(line)) continue;
			json.next((user.getLines().containsKey(line) && user.getLines().get(line)) ? "&a✔" : "&c✗")
					.command("/scoreboard edit toggle " + line.name().toLowerCase())
					.hover("&eClick to toggle")
					.next(" ").group();
			json.next("&3" + camelCase(line))
					.hover(line.render(player()));
			json.newline().group();

			if (++index % 14 == 0) {
				builder.addPage(json);
				json = new JsonBuilder();
			}
		}

		builder.addPage(json).open(player());
	}

	@HideFromHelp
	@TabCompleteIgnore
	@Path("edit toggle <type> [enable]")
	void toggle(ScoreboardLine line, Boolean enable) {
		if (enable == null)
			enable = !user.getLines().containsKey(line) || !user.getLines().get(line);
		user.getLines().put(line, enable);
		if (!enable)
			user.remove(line);
		user.startTasks();
		service.save(user);
		book();
	}

	@Permission("group.staff")
	@Path("list")
	void list() {
		String collect = PlayerUtils.getOnlinePlayers().stream()
				.map(player -> new ScoreboardService().get(player))
				.filter(ScoreboardUser::isActive)
				.map(Name::of)
				.collect(Collectors.joining("&3, &e"));
		send(PREFIX + "Active scoreboards: ");
		send("&e" + collect);
	}

	@Permission("group.staff")
	@Path("view <player>")
	void view(OfflinePlayer player) {
		send(service.get(player).toString());
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		ScoreboardService service = new ScoreboardService();
		ScoreboardUser user = service.get(event.getPlayer());
		if (user.isActive())
			user.on();
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		ScoreboardService service = new ScoreboardService();
		ScoreboardUser user = service.get(event.getPlayer());
		user.pause();
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		ScoreboardService service = new ScoreboardService();
		ScoreboardUser user = service.get(event.getPlayer());
		if (user.isActive())
			user.on();
	}

	@EventHandler
	public void onMatchQuit(MinigamerQuitEvent event) {
		ScoreboardService service = new ScoreboardService();
		ScoreboardUser user = service.get(event.getMinigamer().getPlayer());
		if (user.isActive() && user.getOfflinePlayer().isOnline())
			user.on();
	}

	@EventHandler
	public void onMatchEnd(MatchEndEvent event) {
		ScoreboardService service = new ScoreboardService();
		event.getMatch().getMinigamers().forEach(minigamer -> {
			ScoreboardUser user = service.get(minigamer.getPlayer());
			if (user.isActive() && user.getOfflinePlayer().isOnline())
				user.on();
		});
	}

	@EventHandler
	public void onMatchJoin(MatchJoinEvent event) {
		ScoreboardService service = new ScoreboardService();
		ScoreboardUser user = service.get(event.getMinigamer().getPlayer());
		user.pause();
	}

	@EventHandler
	public void onMcMMOScoreboardEnd(McMMOScoreboardRevertEvent event) {
		ScoreboardService service = new ScoreboardService();
		ScoreboardUser user = service.get(event.getTargetPlayer());
		if (user.isActive() && user.getOfflinePlayer().isOnline())
			user.on();
	}

}
