package me.pugabyte.nexus.features.resourcepack;

import eden.utils.TimeUtils.Time;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.lexikiq.HasPlayer;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.commands.staff.admin.BashCommand;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.resourcepack.LocalResourcePackUser;
import me.pugabyte.nexus.models.resourcepack.LocalResourcePackUserService;
import me.pugabyte.nexus.utils.HttpUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.resourcepack.ResourcePack.URL;
import static me.pugabyte.nexus.features.resourcepack.ResourcePack.closeZip;
import static me.pugabyte.nexus.features.resourcepack.ResourcePack.file;
import static me.pugabyte.nexus.features.resourcepack.ResourcePack.fileName;
import static me.pugabyte.nexus.features.resourcepack.ResourcePack.hash;
import static me.pugabyte.nexus.features.resourcepack.ResourcePack.openZip;

@Aliases("rp")
@NoArgsConstructor
public class ResourcePackCommand extends CustomCommand implements Listener {
	private static final LocalResourcePackUserService service = new LocalResourcePackUserService();

	public ResourcePackCommand(@NonNull CommandEvent event) {
		super(event);
	}

	private void resourcePack(Player player) {
		player.setResourcePack(URL, hash);
	}

	@Path
	void resourcePack() {
		if (hash == null)
			error("Resource pack hash is null");

		if (Status.DECLINED == player().getResourcePackStatus())
			error("You declined the original prompt for the resource pack. In order to use the resource pack, you must edit the server in your server list and change the \"Server Resource Packs\" option to either \"enabled\" or \"prompt\"");

		resourcePack(player());
	}

	@Path("local [enabled]")
	@Description("Tell the server you have the resource pack if you have installed it locally")
	void local(Boolean enabled) {
		if (enabled == null) {
			send(PREFIX + "If you have the resource pack installed locally, use &c/rp local true");
			return;
		}

		if (enabled && Status.DECLINED != player().getResourcePackStatus())
			error("You must decline the resource pack in order to run this command");

		LocalResourcePackUser user = service.get(player());
		user.setEnabled(enabled);
		service.save(user);
		if (enabled)
			send(PREFIX + "The server will now trust that you have the resource pack installed");
		else {
			send(PREFIX + "The server will now automatically detect if you accept the resource pack download");
			if (Status.DECLINED != player().getResourcePackStatus())
				send(PREFIX + "Make sure to enable the resource pack in the server's settings in the multiplayer screen");
		}
	}

	public static @NotNull String statusOf(HasPlayer player) {
		LocalResourcePackUser user = service.get(player.getPlayer());
		if (user.isEnabled())
			return "Manual";
		else
			return StringUtils.camelCase(player.getPlayer().getResourcePackStatus());
	}

	@Permission("group.staff")
	@Path("getStatus [player]")
	void getStatus(@Arg("self") Player player) {
		send(PREFIX + "Resource pack status for " + Nickname.of(player) + ": &e" + statusOf(player));
	}

	@Permission("group.staff")
	@Path("getStatuses")
	void getStatuses() {
		send(PREFIX + "Statuses: ");
		new HashMap<String, List<String>>() {{
			for (Player player : PlayerUtils.getOnlinePlayers()) {
				String status = statusOf(player);
				List<String> names = getOrDefault(status, new ArrayList<>());
				names.add(Nickname.of(player));
				put(status, names);
			}
		}}.forEach((status, names) -> send("&e" + status + "&3: " + String.join(", ", names)));
	}

	@Async
	@Path("update")
	@Permission("group.admin")
	void update() {
		send(BashCommand.tryExecute("sudo /home/minecraft/git/Saturn/deploy.sh"));

		String newHash = Utils.createSha1(URL);

		if (hash != null && hash.equals(newHash))
			error("No resource pack update found");

		hash = newHash;

		if (hash == null)
			error("Resource pack hash is null");

		menuReload();

//		TODO: Figure out a solution that actually works, this just disables the active resource pack for all players who click it
//		for (Player player : PlayerUtils.getOnlinePlayers())
//			if (Arrays.asList(Status.ACCEPTED, Status.SUCCESSFULLY_LOADED).contains(player.getResourcePackStatus()))
//				send(player, json(PREFIX + "There's an update to the resource pack available, click to update.").command("/rp"));
	}

	@Async
	@Path("menu reload")
	@Permission("group.admin")
	void menuReload() {
		closeZip();
		file = HttpUtils.saveFile(URL, fileName);
		openZip();
		CustomModelMenu.load();
		send(PREFIX + "Menu updated");
	}

	@Path("menu [folder]")
	@Permission("group.staff")
	void menu(CustomModelFolder folder) {
		if (rank() == Rank.MODERATOR && worldGroup() != WorldGroup.STAFF)
			permissionError();

		new CustomModelMenu(folder).open(player());
	}

	@ConverterFor(CustomModelFolder.class)
	CustomModelFolder convertToCustomModelFolder(String value) {
		return ResourcePack.getRootFolder().getFolder("/" + (value == null ? "" : value));
	}

	@TabCompleterFor(CustomModelFolder.class)
	List<String> tabCompleteCustomModelFolder(String filter) {
		return ResourcePack.getFolders().stream()
				.map(CustomModelFolder::getDisplayPath)
				.filter(path -> path.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Tasks.wait(Time.SECOND.x(2), () -> {
			resourcePack(player);

			// Try Again if failed
			Tasks.wait(Time.SECOND.x(5), () -> {
				if (Status.FAILED_DOWNLOAD == player.getResourcePackStatus())
					resourcePack(player);
			});
		});
	}

	@EventHandler
	public void onResourcePackEvent(PlayerResourcePackStatusEvent event) {
		Nexus.debug("Resource Pack Status Update: " + event.getPlayer().getName() + " = " + event.getStatus());
	}
}
