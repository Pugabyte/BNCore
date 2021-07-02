package me.pugabyte.nexus.features.commands.staff;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.ClientOption.ChatVisibility;
import eden.utils.TimeUtils.Timespan;
import eden.utils.TimeUtils.Timespan.TimespanBuilder;
import lombok.NonNull;
import me.pugabyte.nexus.features.resourcepack.ResourcePackCommand;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.geoip.GeoIP;
import me.pugabyte.nexus.models.geoip.GeoIPService;
import me.pugabyte.nexus.models.godmode.Godmode;
import me.pugabyte.nexus.models.godmode.GodmodeService;
import me.pugabyte.nexus.models.hours.Hours;
import me.pugabyte.nexus.models.hours.HoursService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.punishments.Punishments;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.utils.JsonBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Set;

import static me.pugabyte.nexus.utils.StringUtils.getLocationString;
import static me.pugabyte.nexus.utils.TimeUtils.shortDateTimeFormat;

@Aliases({"whotf", "whothefuck"})
@Permission("group.staff")
public class WhoIsCommand extends CustomCommand {

	public WhoIsCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Async
	@Path("<player>")
	void run(Nerd nerd) {
		line();
		line();
		send("&3Who the fuck is &6&l" + nerd.getName() + "&3?");

		HoursService hoursService = new HoursService();
		GeoIPService geoIpService = new GeoIPService();

		Punishments punishments = Punishments.of(nerd);
		boolean history = punishments.hasHistory();
		JsonBuilder alts = punishments.getAltsMessage();

		String nickname = Nickname.of(nerd);
		Hours hours = hoursService.get(nerd);
		String rank = nerd.getRank().getColoredName();
		String firstJoin = shortDateTimeFormat(nerd.getFirstJoin());
		String lastJoinQuitLabel = null;
		String lastJoinQuitDate = null;
		String lastJoinQuitDiff = null;
		OfflinePlayer offlinePlayer = nerd.getOfflinePlayer();
		Player player = offlinePlayer.getPlayer();

		if (offlinePlayer.isOnline()) {
			if (nerd.getLastQuit() != null) {
				lastJoinQuitLabel = "Last Quit";
				lastJoinQuitDate = shortDateTimeFormat(nerd.getLastQuit());
				lastJoinQuitDiff = Timespan.of(nerd.getLastQuit()).format();
			}
		} else {
			lastJoinQuitLabel = "Last Join";
			lastJoinQuitDate = shortDateTimeFormat(nerd.getLastQuit());
			lastJoinQuitDiff = Timespan.of(nerd.getLastJoin()).format();
		}
		Set<String> pastNames = nerd.getPastNames();
		Godmode godmode = new GodmodeService().get(nerd);

		JsonBuilder json = json();

		if (nerd.hasNickname())
			json.newline().next("&3Nickname: &e" + nickname).group();

		json.newline().next("&3Rank: &e" + rank).group();
		json.newline().next("&3First Join: &e" + firstJoin).group();

		if (lastJoinQuitDate != null)
			json.newline().next("&3" + lastJoinQuitLabel + ": &e" + lastJoinQuitDiff + " ago").hover("&e" + lastJoinQuitDate).group();

		if (hours.getTotal() > 0)
			json.newline().next("&3Hours: &e" + TimespanBuilder.of(hours.getTotal()).noneDisplay(true).format()).group();

		if (history)
			json.newline().next("&3History: &e" + punishments.getPunishments().size()).command("/history " + nerd.getName()).hover("&eClick to view history").group();

		if (alts != null)
			json.newline().next("&3Alts: &e").next(alts).group();

		if (!pastNames.isEmpty())
			json.newline().next("&3Past Names: &e" + String.join("&3, &e", pastNames)).group();

		try {
			GeoIP geoIp = geoIpService.get(nerd);
			if (!isNullOrEmpty(geoIp.getIp()))
				json.newline().next("&3GeoIP: &e" + geoIp.getFriendlyLocationString()).hover("&e" + geoIp.getIp()).suggest(geoIp.getIp()).group();
		} catch (InvalidInputException ex) {
			json.newline().next("&3GeoIP: &c" + ex.getMessage()).group();
		}

		try {
			json.newline().next("&3Location: &e" + getLocationString(nerd.getLocation())).hover("&eClick to TP").command("/tp " + offlinePlayer.getName()).group();
		} catch (InvalidInputException ex) {
			json.newline().next("&3Location: &c" + ex.getMessage()).group();
		}

		json.newline().next("&3Balances:");
		for (ShopGroup shopGroup : ShopGroup.values())
			if (new BankerService().getBalance(offlinePlayer, shopGroup) != 500)
				json.newline().next("  &3" + camelCase(shopGroup) + ": &e" + new BankerService().getBalanceFormatted(offlinePlayer, shopGroup)).group();

		if (player != null) {
			json.newline().next("&3Client Brand Name: &e" + player.getClientBrandName()).group();

			json.newline().next("&3Gamemode: &e" + camelCase(player.getGameMode())).group();

			json.newline().next("&3God mode: &e" + godmode.isEnabledRaw()).group();

			json.newline().next("&3Fly mode: &e" + player.getAllowFlight() + " &3(" + (player.isFlying() ? "flying" : "not flying") + ")").group();

			json.newline().next("&3RP status: &e" + ResourcePackCommand.statusOf(player)).group();

			final ChatVisibility chatVisibility = player.getClientOption(ClientOption.CHAT_VISIBILITY);
			if (chatVisibility != ChatVisibility.FULL)
				json.newline().next("&3Chat Visibility: &e" + camelCase(chatVisibility));
		}

		json.newline().next("&3OP: &e" + offlinePlayer.isOp()).group();

		send(json);
	}

}
