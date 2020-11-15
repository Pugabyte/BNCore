package me.pugabyte.bncore.features.holidays.pugmas20.commands;

import me.pugabyte.bncore.features.holidays.pugmas20.AdventChests;
import me.pugabyte.bncore.features.holidays.pugmas20.Pugmas20;
import me.pugabyte.bncore.features.holidays.pugmas20.Train;
import me.pugabyte.bncore.features.holidays.pugmas20.menu.AdventMenu;
import me.pugabyte.bncore.features.holidays.pugmas20.models.PugmasTreeType;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Aliases;
import me.pugabyte.bncore.framework.commands.models.annotations.Arg;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.annotations.Permission;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.models.pugmas20.Pugmas20Service;
import me.pugabyte.bncore.models.pugmas20.Pugmas20User;
import me.pugabyte.bncore.utils.StringUtils;
import me.pugabyte.bncore.utils.WorldEditUtils;
import org.bukkit.OfflinePlayer;

@Aliases("pugmas")
public class Pugmas20Command extends CustomCommand {
	private final Pugmas20Service service = new Pugmas20Service();
	private WorldEditUtils worldEditUtils;


	public Pugmas20Command(CommandEvent event) {
		super(event);
		if (isPlayer())
			worldEditUtils = new WorldEditUtils(player());
	}

	@Path()
	void pugmas() {
		String timeLeft = StringUtils.timespanDiff(Pugmas20.openingDay);
		send("Soon™ (" + timeLeft + ")");
	}

	@Permission("group.staff") // TODO PUGMAS - Remove
	@Path("advent [day]")
	void advent(@Arg("-1") int day) {
		AdventMenu.openAdvent(player(), day);
	}

	@Permission("group.admin")
	@Path("advent give <day>")
	void adventGiveHead(int day) {
		AdventChests.giveAdventHead(player(), day);
	}

	@Permission("group.admin")
	@Path("advent open <day>")
	void adventOpenDay(int day) {
		AdventChests.openAdventLootInv(player(), day);
	}

	@Permission("group.admin")
	@Path("advent addDay <player> <day>")
	void adventAddDay(OfflinePlayer player, int day) {
		Pugmas20User user = service.get(player);
		user.getFoundDays().add(day);
		service.save(user);

		send("Added day " + day + " to " + player.getName());
	}

	@Permission("group.admin")
	@Path("database delete <player>")
	void databaseDelete(OfflinePlayer player) {
		Pugmas20User user = service.get(player);
		service.delete(user);
		send("Deleted data for " + player.getName());
	}

	@Path("testtrain")
	@Permission("group.admin")
	void testTrain() {
		if (!Train.animate())
			error("Train is animating!");
	}

	@Permission("group.admin")
	@Path("tree build <treeType> <id>")
	void treeSchematic(PugmasTreeType treeType, int id) {
		treeType.build(id);
	}

	@Permission("group.admin")
	@Path("tree feller <treeType> <id>")
	void treeFeller(PugmasTreeType treeType, int id) {
		treeType.feller(player(), id);
	}
}
