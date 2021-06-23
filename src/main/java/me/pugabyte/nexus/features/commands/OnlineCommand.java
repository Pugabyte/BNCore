package me.pugabyte.nexus.features.commands;

import eden.utils.TimeUtils.Timespan;
import me.pugabyte.nexus.features.afk.AFK;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.afk.AFKPlayer;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.hours.Hours;
import me.pugabyte.nexus.models.hours.HoursService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Aliases({"list", "ls", "who", "players", "eonline", "elist", "ewho", "eplayers"})
@Description("List online players and view basic information about them")
public class OnlineCommand extends CustomCommand {

	public OnlineCommand(CommandEvent event) {
		super(event);
	}

	@Path
	@Override
	public void help() {
		List<Rank> ranks = Arrays.asList(Rank.values());
		Collections.reverse(ranks);

		long vanished = PlayerUtils.getOnlinePlayers().stream().filter(PlayerUtils::isVanished).count();
		long online = PlayerUtils.getOnlinePlayers().size() - vanished;
		boolean canSeeVanished = !isPlayer() || player().hasPermission("pv.see");
		String counts = online + ((canSeeVanished && vanished > 0) ? " &3+ &e" + vanished : "");

		line();
		send("&3There are &e" + counts + " &3out of maximum &e" + Bukkit.getMaxPlayers() + " &3players online");

		ranks.forEach(rank -> {
			List<Nerd> nerds = rank.getOnlineNerds().stream().filter(this::canSee).collect(Collectors.toList());
			if (nerds.size() == 0) return;

			JsonBuilder builder = new JsonBuilder(rank.getColoredName() + "s&f: ");

			nerds.forEach(nerd -> getNameWithModifiers(nerd, builder));

			send(builder);
		});

		line();
		send("&e&lClick &3on a player's name to open the &eQuickAction &3menu");
		line();
	}

	private boolean canSee(Nerd nerd) {
		return PlayerUtils.canSee(player(), nerd.getOnlinePlayer()) && player().canSee(nerd.getOnlinePlayer());
	}

	void getNameWithModifiers(Nerd nerd, JsonBuilder builder) {
		boolean vanished = PlayerUtils.isVanished(nerd.getOnlinePlayer());
		boolean afk = AFK.get(nerd.getOnlinePlayer()).isAfk();

		String modifiers = "";
		if (vanished)
			if (afk)
				modifiers = "&7[AFK] [V] ";
			else
				modifiers = "&7[V] ";
		else if (afk)
			modifiers = "&7[AFK] ";

		if (!builder.isInitialized())
			builder.initialize();
		else
			builder.next("&f, ").group();

		builder.next(modifiers + nerd.getColoredName())
				.command("/quickaction " + nerd.getName())
				.hover(getInfo(nerd, modifiers))
				.group();
	}

	String getInfo(Nerd nerd, String modifiers) {
		Player player = nerd.getOnlinePlayer();
		Hours hours = new HoursService().get(player.getUniqueId());

		int ping = player.getPing();
		String onlineFor = Timespan.of(nerd.getLastJoin()).format();
		WorldGroup world = WorldGroup.of(player);
		ShopGroup shopGroup = ShopGroup.of(player);
		if (shopGroup == null)
			shopGroup = ShopGroup.SURVIVAL;
		String balance = new BankerService().getBalanceFormatted(player, shopGroup);
		String totalHours = Timespan.of(hours.getTotal()).format();
		String afk = "";

		if (modifiers.contains("AFK")) {
			AFKPlayer afkPlayer = AFK.get(player);
			String timeAFK = Timespan.of(afkPlayer.getTime()).format();
			afk = "&3AFK for: &e" + timeAFK + "\n \n";
		}

		return afk +
				"&3Ping: &e" + ping + "\n" +
				"&3World: &e" + world + "\n" +
				"&3" + camelCase(shopGroup) + " balance: &e" + balance + "\n" +
				"&3Online for: &e" + onlineFor + "\n" +
				"&3Hours: &e" + totalHours;
	}
}
