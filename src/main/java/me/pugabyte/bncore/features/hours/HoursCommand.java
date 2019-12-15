package me.pugabyte.bncore.features.hours;

import me.pugabyte.bncore.Utils;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Aliases;
import me.pugabyte.bncore.framework.commands.models.annotations.Arg;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.bncore.models.hours.Hours;
import me.pugabyte.bncore.models.hours.HoursService;
import org.bukkit.OfflinePlayer;

import java.util.List;

@Aliases("jhours")
public class HoursCommand extends CustomCommand {
	private HoursService service = new HoursService();

	public HoursCommand(CommandEvent event) {
		super(event);
	}

	@Path("{offlineplayer}")
	void player(@Arg("self") OfflinePlayer player) {
		Hours hours = (Hours) service.get(player);
		reply("&3Total: &e" + Utils.timespanFormat(hours.getTotal(), "None"));
		reply("&3Daily: &e" + Utils.timespanFormat(hours.getDaily(), "None"));
		reply("&3Weekly: &e" + Utils.timespanFormat(hours.getWeekly(), "None"));
		reply("&3Monthly: &e" + Utils.timespanFormat(hours.getMonthly(), "None"));
	}

	@Path("top")
	void top() {
		Utils.async(() -> {
			String type = null;
			Integer page = null;
			try {
				page = intArg(2);
			} catch (InvalidInputException ex) {
				type = arg(2);
				page = intArg(3);
			}
			if (type == null) type = "total";
			if (page == null) page = 1;

			try {
				final HoursService.HoursType hoursType = service.getType(type);

				List<Hours> results = service.getPage(hoursType, page);
				if (results.size() == 0) {
					reply(PREFIX + "&cNo results on page " + page);
					return;
				}

				reply("");
				reply(PREFIX + "Total: " + Utils.timespanFormat(service.total(hoursType)) + (page > 1 ? "&e  |  &3Page " + page : ""));
				int i = (page - 1) * 10 + 1;
				for (Hours hours : results)
					reply("&3" + i++ + " &e" + hours.getPlayer().getName() + " &7- " + Utils.timespanFormat(hours.get(hoursType)));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}
}
