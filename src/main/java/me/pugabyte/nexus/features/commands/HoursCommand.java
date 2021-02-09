package me.pugabyte.nexus.features.commands;

import lombok.Data;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.afk.AFK;
import me.pugabyte.nexus.features.chat.Koda;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.models.hours.Hours;
import me.pugabyte.nexus.models.hours.HoursService;
import me.pugabyte.nexus.models.hours.HoursService.PageResult;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundUtils.Jingle;
import me.pugabyte.nexus.utils.StringUtils.TimespanFormatter;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import me.pugabyte.nexus.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Aliases({"playtime", "days", "minutes", "seconds"})
public class HoursCommand extends CustomCommand {
	private final HoursService service = new HoursService();

	public HoursCommand(CommandEvent event) {
		super(event);
	}

	private static final int DAY = Time.DAY.get() / 20;

	@Async
	@Path("[player]")
	void player(@Arg("self") Hours hours) {
		OfflinePlayer player = hours.getOfflinePlayer();
		boolean isSelf = isSelf(player);

		send("");
		send(PREFIX + (isSelf ? "Your" : "&e" + player.getName() + "&3's") + " playtime");
		send("&3Total: &e" + TimespanFormatter.of(hours.getTotal()).noneDisplay(true).format());
		send("&7- &3Today: &e" + TimespanFormatter.of(hours.getDaily()).noneDisplay(true).format());
		send("&7- &3This month: &e" + TimespanFormatter.of(hours.getMonthly()).noneDisplay(true).format());

		if (Rank.getHighestRank(player) == Rank.GUEST) {

			String who = (isSelf ? "You need" : player.getName() + " needs") + " ";
			String left = TimespanFormatter.of(DAY - hours.getTotal()).format();

			line();
			send("&3" + who + "&e" + left + " more in-game play time &3to achieve &fMember&3.");
		}
	}

	@Permission("group.seniorstaff")
	@Path("debug [player]")
	void debug(@Arg("self") OfflinePlayer player) {
		send(service.get(player).toString());
	}

	@Async
	@Path("top [page]")
	void top(@Arg("1") int page) {
//		String type = isIntArg(2) ? "total" : arg(2);
//		int page = isIntArg(2) ? intArg(2) : isIntArg(3) ? intArg(3) : 1;

//		final HoursType hoursType = service.getType(type);

//		List<Hours> results = service.getPage(hoursType, page);

		List<PageResult> results = service.getPage(page);
		if (results.size() == 0)
			error("&cNo results on page " + page);

		send("");
//		send(PREFIX + "Total: " + StringUtils.timespanFormat(service.total(hoursType)) + (page > 1 ? "&e  |  &3Page " + page : ""));
		send(PREFIX + (page > 1 ? "&3Page " + page : ""));
		int i = (page - 1) * 10 + 1;
		for (PageResult result : results)
			send("&3" + i++ + " &e" + result.getOfflinePlayer().getName() + " &7- " + TimespanFormatter.of(result.getTotal()).format());
	}

	@Data
	public static class HoursTopArguments {
		private int year = -1;
		private int month = -1;
		private int day = -1;
		private int page = 1;

		public HoursTopArguments(String input) {
			LocalDate now = LocalDate.now();

			switch (input) {
				case "day":
				case "daily":
					day = now.getDayOfMonth();
					month = now.getMonthValue();
					year = now.getYear();
					break;
				case "month":
				case "monthly":
					month = now.getMonthValue();
					year = now.getYear();
					break;
				case "year":
				case "yearly":
					year = now.getYear();
					break;
				default:
					String[] args = input.split(" ");
					String[] split = args[0].split("-");
					if (split[0].length() > 0 && Utils.isInt(split[0])) {
						int yearInput = Integer.parseInt(split[0]);
						if (yearInput >= 2015)
							if (yearInput <= 2019)
								throw new InvalidInputException("Years 2015-2019 are not supported");
							else
								if (yearInput > now.getYear())
									throw new InvalidInputException("Year &e" + yearInput + " &cis in the future");
								else
									year = yearInput;
						else {
							page = yearInput;
							break;
						}

						if (split.length >= 2) {
							if (split[1].length() > 0 && Utils.isInt(split[1])) {
								int monthInput = Integer.parseInt(split[1]);
								if (monthInput >= 1 && monthInput <= 12)
									if (YearMonth.of(year, monthInput).isAfter(YearMonth.now()))
										throw new InvalidInputException("Month &e" + yearInput + "-" + monthInput + " &cis in the future");
									else
										month = monthInput;
								else
									throw new InvalidInputException("Invalid month &e" + monthInput);
							} else
								throw new InvalidInputException("Invalid month &e" + split[1]);

							if (split.length >= 3) {
								if (split[2].length() > 0 && Utils.isInt(split[2])) {
									int dayInput = Integer.parseInt(split[2]);
									if (YearMonth.of(year, month).isValidDay(dayInput))
										if (LocalDate.of(year, month, dayInput).isAfter(now))
											throw new InvalidInputException("Day &e" + year + "-" + month + "-" + dayInput + " &cis in the future");
										else
											day = dayInput;
									else
										throw new InvalidInputException("Invalid day of month &e" + dayInput);
								} else
									throw new InvalidInputException("Invalid day &e" + split[2]);
							}
						}
					} else
						throw new InvalidInputException("Invalid year &e" + split[0]);

					if (args.length >= 2 && Utils.isInt(args[1]))
						page = Integer.parseInt(args[1]);

					if (year == 2020) {
						if (month == -1)
							throw new InvalidInputException("Year 2020 is not supported");
						else if (month <= 5)
							throw new InvalidInputException("Months Jan-May of 2020 are not supported");
					}

					if (page < 1)
						throw new InvalidInputException("Page cannot be less than 1");
			}
		}
	}

	@Async
	@Permission("group.admin")
	@Path("top2 [args...]")
	void top2(@Arg("1") HoursTopArguments args) {
		send(args.toString());
	}

	@ConverterFor(HoursTopArguments.class)
	HoursTopArguments convertToHoursTopArgument(String value) {
		return new HoursTopArguments(value);
	}

	@TabCompleterFor(HoursTopArguments.class)
	List<String> tabCompleteHoursTopArgument(String filter) {
		return new ArrayList<>();
	}

	private static final int INTERVAL = 5;

	static {
		Tasks.repeatAsync(10, Time.SECOND.x(INTERVAL), () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				try {
					if (AFK.get(player).isAfk()) continue;

					HoursService service = new HoursService();
					Hours hours = service.get(player);
					hours.increment(INTERVAL);
					service.update(hours);

					if (Rank.getHighestRank(player) == Rank.GUEST) {
						if (hours.getTotal() > DAY) {
							Tasks.sync(() -> {
								PlayerUtils.runCommandAsConsole("lp user " + player.getName() + " parent set " + Rank.MEMBER.name());
								Koda.say("Congrats on Member rank, " + player.getName() + "!");
								Jingle.RANKUP.play(player);
								PlayerUtils.send(player, "");
								PlayerUtils.send(player, "");
								PlayerUtils.send(player, "&e&lCongratulations! &3You have been promoted to &fMember&3 for " +
										"playing for &e24 hours &3in-game. You are now eligible for &c/trusted&3.");
								PlayerUtils.send(player, "");
								PlayerUtils.send(player, "&6&lThank you for flying Bear Nation!");
							});
						}
					}
				} catch (Exception ex) {
					Nexus.warn("Error in Hours scheduler: " + ex.getMessage());
				}
			}
		});
	}
}


