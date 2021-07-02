package me.pugabyte.nexus.features.votes;

import eden.utils.TimeUtils.Timespan;
import eden.utils.Utils;
import lombok.NonNull;
import me.pugabyte.nexus.features.votes.vps.VPS;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.setting.Setting;
import me.pugabyte.nexus.models.setting.SettingService;
import me.pugabyte.nexus.models.vote.Vote;
import me.pugabyte.nexus.models.vote.VoteService;
import me.pugabyte.nexus.models.vote.VoteSite;
import me.pugabyte.nexus.models.vote.Voter;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.TimeUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.votes.Votes.GOAL;
import static me.pugabyte.nexus.utils.StringUtils.ProgressBarStyle.NONE;
import static me.pugabyte.nexus.utils.StringUtils.progressBar;

@Aliases("votes")
public class VoteCommand extends CustomCommand {
	private final String PLUS = "&e[+] &3";
	private Voter voter;

	public VoteCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayer())
			voter = new VoteService().get(player());
	}

	@Path
	@Async
	void run() {
		line(3);
		send(json("&3Support the server by voting daily! Each vote gives you &e1 Vote point &3to spend in the " +
				"&eVote Point Store, &3and the top voters of each month receive a special reward!"));
		line();
		JsonBuilder builder = json("&3 Links");
		for (VoteSite site : VoteSite.values())
			builder.next(" &3|| &e").next("&e" + site.name()).url(site.getUrl(Nerd.of(player()).getName())).group();
		send(builder);
		int sum = new VoteService().getTopVoters(LocalDateTime.now().getMonth()).stream()
				.mapToInt(topVoter -> Long.valueOf(topVoter.getCount()).intValue()).sum();
		line();
		send(json("&3Server goal: " + progressBar(sum, GOAL, NONE, 75) + " &e" + sum + "&3/&e" + GOAL)
				.hover("&eReach the goal together for a monthly reward!"));
		line();
		send(PLUS + "You have &e" + voter.getPoints() + " &3vote points");
		line();
		send(json(PLUS + "Visit the &eVote Points Store").command("/vps"));
		send(json(PLUS + "View top voters, prizes and more on our &ewebsite").url("https://projecteden.gg/vote"));
	}

	@Permission("group.admin")
	@Path("extra")
	void extra() {
		send("Extra config: " + Votes.getExtras());
	}

	@Path("time [player]")
	void time(@Arg(value = "self", permission = "group.staff") OfflinePlayer player) {
		voter = new VoteService().get(player);
		line();
		for (VoteSite site : VoteSite.values()) {
			Optional<Vote> first = voter.getActiveVotes().stream().filter(_vote -> _vote.getSite() == site).findFirst();
			if (first.isPresent()) {
				LocalDateTime expirationTime = first.get().getTimestamp().plusHours(site.getExpirationHours());
				send("&e" + site.name() + " &7- &3You can vote in &e" + Timespan.of(expirationTime).format());
			} else {
				send(json("&e" + site.name() + " &7- &3Click here to vote").url(site.getUrl(Nerd.of(player()).getName())));
			}
		}
	}

	@Async
	@Permission("group.admin")
	@Path("getTopDays [page]")
	void getTopDays(@Arg("1") int page) {
		Map<LocalDate, Integer> days = new VoteService().getVotesByDay();

		send(PREFIX + "Most votes in a day");
		BiFunction<LocalDate, String, JsonBuilder> formatter = (date, index) -> {
			String color = date.equals(LocalDate.now()) ? "&6" : "&e";
			return json("&3" + index + " " + color + TimeUtils.shortishDateFormat(date) + " &7- " + days.get(date));
		};

		paginate(days.keySet(), formatter, "/votes getTopDays", page);
	}

	@Async
	@Permission("group.admin")
	@Path("allCounts")
	void allCounts() {
		Map<Integer, List<Player>> activeVotes = new HashMap<>();
		for (Player player : PlayerUtils.getOnlinePlayers()) {
			Voter voter = new VoteService().get(player);
			int count = voter.getActiveVotes().size();

			List<Player> players = activeVotes.getOrDefault(count, new ArrayList<>());
			players.add(player);
			activeVotes.put(count, players);
		}

		send(PREFIX + "Vote counts");
		Utils.sortByKeyReverse(activeVotes).forEach((count, players) ->
				send("&e" + count + " &7- " + players.stream().map(Nickname::of).collect(Collectors.joining(", "))));
	}

	@Path("reminders [enable] [player]")
	void reminders(Boolean enable, @Arg(value = "self", permission = "group.staff") OfflinePlayer player) {
		SettingService settingService = new SettingService();
		Setting reminders = settingService.get(player.getUniqueId().toString(), "vote-reminders");
		if (enable == null)
			if (reminders.getValue() != null)
				enable = !reminders.getBoolean();
			else
				enable = false;

		reminders.setBoolean(enable);
		settingService.save(reminders);

		send(PREFIX + "Discord voting reminders " + (enable ? "&aenabled" : "&cdisabled"));
	}

	@Path("points store [page]")
	void run(@Arg("1") int page) {
		VPS.open(player(), page);
	}

	@Path("points [player]")
	void points(@Arg("self") OfflinePlayer player) {
		if (!isSelf(player)) {
			voter = new Voter(player);
			send("&e" + nickname(player) + " &3has &e" + voter.getPoints() + plural(" &3vote point", voter.getPoints()));
		} else
			send("&3You have &e" + voter.getPoints() + plural(" &3vote point", voter.getPoints()));
	}

	@Permission("group.seniorstaff")
	@Path("points set <player> <number>")
	void setPoints(OfflinePlayer player, int number) {
		Voter voter = new Voter(player);
		voter.setPoints(number);
		send("&e" + nickname(player) + " &3now has &e" + voter.getPoints() + plural(" &3vote point", voter.getPoints()));
	}

	@Permission("group.seniorstaff")
	@Path("points give <player> <number>")
	void givePoints(OfflinePlayer player, int number) {
		Voter voter = new Voter(player);
		voter.givePoints(number);
		send("&e" + nickname(player) + " &3now has &e" + voter.getPoints() + plural(" &3vote point", voter.getPoints()));
	}

	@Permission("group.seniorstaff")
	@Path("points take <player> <number>")
	void takePoints(OfflinePlayer player, int number) {
		Voter voter = new Voter(player);
		voter.takePoints(number);
		send("&e" + nickname(player) + " &3now has &e" + voter.getPoints() + plural(" &3vote point", voter.getPoints()));
	}

	@Path("endOfMonth [month]")
	@Permission("group.admin")
	void endOfMonth(Month month) {
		console();
		EndOfMonth.run(month);
	}

	@Path("write")
	@Permission("group.admin")
	void write() {
		Votes.write();
		send(PREFIX + "Done");
	}

	@ConverterFor(Voter.class)
	Voter convertToVoter(String value) {
		return new VoteService().get(convertToOfflinePlayer(value));
	}

	@TabCompleterFor(Voter.class)
	List<String> tabCompleteVoter(String value) {
		return tabCompletePlayer(value);
	}

}
