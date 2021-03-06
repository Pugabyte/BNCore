package me.pugabyte.nexus.features.commands;

import com.google.common.base.Strings;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.menus.BookBuilder.WrittenBookMenu;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.HideFromHelp;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleteIgnore;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.hours.Hours;
import me.pugabyte.nexus.models.hours.HoursService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.punishments.Punishments;
import me.pugabyte.nexus.models.referral.Referral;
import me.pugabyte.nexus.models.referral.Referral.Origin;
import me.pugabyte.nexus.models.referral.ReferralService;
import me.pugabyte.nexus.models.rule.HasReadRules;
import me.pugabyte.nexus.models.rule.HasReadRulesService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.Utils.sortByValueReverse;

@NoArgsConstructor
@Description("Tell us how you found the server")
public class ReferralCommand extends CustomCommand implements Listener {
	private final ReferralService service = new ReferralService();
	private Referral referral;

	public ReferralCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayer())
			referral = service.get(player());
	}

	@Path
	void run() {
		JsonBuilder json = json();
		for (Origin origin : Origin.values())
			json.next("&3" + origin.getDisplay())
					.hover("&e" + origin.getLink())
					.command("/referral choose " + origin.name().toLowerCase())
					.group()
					.newline();

		new WrittenBookMenu().addPage(json).open(player());
	}

	@HideFromHelp
	@TabCompleteIgnore
	@Path("choose [origin]")
	void choose(Origin origin) {
		referral.setOrigin(origin);
		service.save(referral);
		if (origin == Origin.OTHER) {

// In case this ever works in the future
//			new EditableBookMenu()
//					.addPage(json("Tell use more:").newline())
//					.onSign(meta -> {
//						referral.setExtra(meta.getPage(0));
//						service.save(referral);
//					})
//					.open(player());

			Nexus.getSignMenuFactory().blank()
					.prefix(PREFIX)
					.response(lines -> {
						List<String> list = new ArrayList<>(Arrays.asList(lines));
						list.removeIf(Strings::isNullOrEmpty);
						referral.setExtra(String.join(" ", list));
						service.save(referral);
						send(PREFIX + "Thank you for your feedback!");
					})
					.open(player());
		} else
			send(PREFIX + "Thank you for your feedback!");
	}

	@Path("debug [player]")
	@Permission("group.admin")
	void debug(@Arg("self") Referral referral) {
		send(toPrettyString(referral));
	}

	@Async
	@Path("extraInputs")
	void others() {
		List<Referral> referrals = service.getAll().stream()
				.filter(_referral -> !isNullOrEmpty(_referral.getExtra()))
				.collect(Collectors.toList());

		if (referrals.isEmpty())
			error("No referrals with extra content found");

		send(PREFIX + "Extra input: ");
		for (Referral _referral : referrals)
			send(" &e" + _referral.getName() + " &7" + _referral.getExtra());
	}

	@Async
	@Path("stats")
	void stats() {
		List<Referral> referrals = service.getAll();
		if (referrals.isEmpty())
			error("No referral stats available");

		Map<Origin, Integer> manuals = new HashMap<>();
		Map<String, Integer> ips = new HashMap<>();
		for (Referral referral : referrals) {
			if (referral.getOrigin() != null)
				manuals.put(referral.getOrigin(), manuals.getOrDefault(referral.getOrigin(), 0) + 1);
			if (referral.getIp() != null) {
				final String site = getSite(referral.getIp());
				ips.put(site, ips.getOrDefault(site, 0) + 1);
			}
		}

		line();
		send(PREFIX + "Stats:");
		send(" &3Manual input:");
		sortByValueReverse(manuals).forEach((origin, count) -> send("&7  " + count + " - &e" + origin.getDisplay()));
		line();
		send(" &3IPs:");
		sortByValueReverse(ips).forEach((ip, count) -> send("&7  " + count + " - &e" + ip));
	}

	@Data
	@RequiredArgsConstructor
	private static class TurnoverData {
		private final String ip;
		private final List<UUID> players = new ArrayList<>();
		private final Map<UUID, Integer> secondsPlayed = new HashMap<>();
		private final List<UUID> punished = new ArrayList<>();
		private final List<UUID> member = new ArrayList<>();
		private final List<UUID> trusted = new ArrayList<>();
		private final List<UUID> readRules = new ArrayList<>();
		private final HoursService hoursService = new HoursService();

		void add(OfflinePlayer player) {
			final UUID uuid = player.getUniqueId();
			players.add(uuid);
			secondsPlayed.put(uuid, hoursService.get(player).getTotal());

			if (!Punishments.of(uuid).getPunishments().isEmpty())
				punished.add(uuid);

			Rank rank = Rank.of(uuid);
			if (rank.gte(Rank.TRUSTED))
				trusted.add(uuid);
			else if (rank == Rank.MEMBER)
				member.add(uuid);

			final HasReadRules hasReadRules = new HasReadRulesService().get(uuid);
			if (hasReadRules.getReadSections().size() >= 2)
				readRules.add(uuid);
		}

		public int count() {
			return secondsPlayed.size();
		}

		// average
		public double mean() {
			return secondsPlayed.values().stream().mapToInt(Integer::intValue).sum() / secondsPlayed.size();
		}

		public double median() {
			final Integer[] array = secondsPlayed.values().toArray(Integer[]::new);
			int length = array.length;

			Arrays.sort(array);

			if (length % 2 != 0)
				return (double) array[length / 2];

			return (double) (array[(length - 1) / 2] + array[length / 2]) / 2d;
		}
	}

	@Async
	@Path("turnover")
	void turnover() {
		List<Referral> referrals = service.getAll();
		if (referrals.isEmpty())
			error("No referral stats available");

		Map<String, TurnoverData> turnoverData = new HashMap<>();
		for (Referral referral : referrals) {
			String ip = referral.getIp();
			if (ip == null)
				continue;

			final String site = getSite(ip);

			turnoverData.computeIfAbsent(site, $ -> new TurnoverData(site)).add(referral.getOfflinePlayer());
		}

		final List<TurnoverData> sorted = turnoverData.values().stream().sorted(Comparator.comparing(TurnoverData::mean)).toList();

		line();
		send(PREFIX + "Turnover:");

		sorted.stream().map(data -> {
			final int count = data.count();
			Function<Integer, String> percentage = amount ->
					StringUtils.getDf().format((amount / (double) count) * 100L) + "%";

			return json("&e" + data.getIp())
					.newline().next("&7  Count: &f" + count)
					.newline().next("&7  Mean playtime: &f" + Timespan.of((int) data.mean()).format())
					.newline().next("&7  Median playtime: &f" + Timespan.of((int) data.median()).format())
					.newline().next("&7  % Punished: &f" + percentage.apply(data.getPunished().size()))
					.newline().next("&7  % Trusted: &f" + percentage.apply(data.getTrusted().size()))
					.newline().next("&7  % Member: &f" + percentage.apply(data.getMember().size()))
					.newline().next("&7  % Read rules: &f" + percentage.apply(data.getReadRules().size()));
		}).forEach(this::send);
	}

	@Path("who has rank <rank> from <site> [page]")
	void whoHasRank(Rank rank, @Arg(tabCompleter = ReferralSite.class) String subdomain, @Arg("1") int page) {
		List<Hours> players = getPlayersFrom(subdomain).stream()
				.map(uuid -> new HoursService().get(uuid))
				.filter(uuid -> Rank.of(uuid).gte(rank))
				.sorted(Comparator.comparing(Hours::getTotal).reversed())
				.toList();

		BiFunction<Hours, String, JsonBuilder> formatter = (hours, index) -> json("&3" + index + " &e" + Nerd.of(hours).getColoredName());
		paginate(players, formatter, "/referral who has rank " + rank.name().toLowerCase() + " from " + subdomain, page);
	}

	@Path("who has playtime <playtime> from <site> [page]")
	void whoHasPlaytime(String playtime, @Arg(tabCompleter = ReferralSite.class) String subdomain, @Arg("1") int page) {
		final int seconds = Timespan.of(playtime).getOriginal();

		List<Hours> players = getPlayersFrom(subdomain).stream()
				.map(uuid -> new HoursService().get(uuid))
				.filter(hours -> hours.getTotal() >= seconds)
				.sorted(Comparator.comparing(Hours::getTotal).reversed())
				.toList();

		BiFunction<Hours, String, JsonBuilder> formatter = (hours, index) -> json("&3" + index + " &e" + Nerd.of(hours).getColoredName() +
				" &7- " + Timespan.of(hours.getTotal()).format());
		paginate(players, formatter, "/referral who has playtime " + playtime + " from " + subdomain, page);
	}

	@NotNull
	private List<UUID> getPlayersFrom(String subdomain) {
		List<Referral> referrals = service.getAll();
		if (referrals.isEmpty())
			error("No referral stats available");

		List<UUID> players = new ArrayList<>();
		for (Referral referral : referrals) {
			String ip = referral.getIp();
			if (ip == null)
				continue;

			final String site = getSite(ip);
			if (subdomain.equalsIgnoreCase(site))
				players.add(referral.getUuid());
		}
		return players;
	}

	@Getter
	private enum ReferralSite {
		DIRECT("server", "bnn.gg", "projecteden.gg", "51.", "192."),
		BIZ("bi", "bl", "bz", "iz", "play.biz", "baz"),
		MCSL("mscl", "mscsl", "mcssl", "mccl"),
		MCMP("mmcmp"),
		TOPG("gopg"),
		MCS("mmcs"),
		PMC("pcm"),
		DB("dn"),
		;

		private final List<String> subdomains;

		ReferralSite(String... subdomains) {
			this.subdomains = List.of(subdomains);
		}
	}

	@NotNull
	private String getSite(String ip) {
		ip = ip.toLowerCase();

		for (ReferralSite site : ReferralSite.values()) {
			for (String start : site.getSubdomains())
				if (ip.startsWith(start))
					return site.name().toLowerCase();
		}

		return ip.split("\\.", 2)[0].toLowerCase();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Tasks.waitAsync(Time.MINUTE, () -> {
			Referral referral = new ReferralService().get(event.getPlayer());
			if (referral.getOrigin() == null) {
				Nerd nerd = Nerd.of(event.getPlayer());
				if (nerd.getFirstJoin().isAfter(LocalDateTime.now().minusHours(2))) {
					Tasks.sync(() -> {
						if (!event.getPlayer().isOnline())
							return;

						if (new CooldownService().check(event.getPlayer(), "referralAsk", Time.MINUTE.x(5))) {
							send(event.getPlayer(), json().newline()
									.next("&e&lHey there! &3Could you quickly tell us where you found this server? &eClick here!")
									.command("/referral")
									.newline());
						}
					});
				}
			}
		});
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent event) {
		ReferralService service = new ReferralService();
		Referral referral = service.get(event.getPlayer());

		String hostname = event.getHostname();
		if (hostname.contains(":"))
			hostname = hostname.split(":")[0];
		if (hostname.endsWith("."))
			hostname = hostname.substring(0, hostname.length() - 1);
		if (hostname.equalsIgnoreCase("server.projecteden.gg"))
			hostname = "projecteden.gg";

		referral.setIp(hostname);
		service.save(referral);
	}

}
