package me.pugabyte.nexus.features.minigames.commands;

import lombok.Getter;
import lombok.NonNull;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.mechanics.Battleship;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.arenas.BattleshipArena;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData.Grid;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData.Grid.Coordinate;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData.ShipType;
import me.pugabyte.nexus.features.minigames.models.mechanics.MechanicType;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.LocationUtils.CardinalDirection;
import me.pugabyte.nexus.utils.PlayerUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.menus.SignMenuFactory.ARROWS;
import static me.pugabyte.nexus.utils.LocationUtils.getCenteredLocation;

@Aliases("bs")
public class BattleshipCommand extends CustomCommand {
	@Getter
	private static boolean debug;

	private Minigamer minigamer;
	private Battleship mechanic;

	private Match match;
	private BattleshipArena arena;
	private BattleshipMatchData matchData;
	private Grid grid;
	private Team team;
	private Team otherTeam;

	public BattleshipCommand(@NonNull CommandEvent event) {
		super(event);
		minigamer = PlayerManager.get(player());
		if (minigamer.isIn(Battleship.class)) {
			mechanic = (Battleship) MechanicType.BATTLESHIP.get();
			match = minigamer.getMatch();
			arena = minigamer.getMatch().getArena();
			matchData = minigamer.getMatch().getMatchData();
			grid = matchData.getGrid(minigamer.getTeam());
			team = minigamer.getTeam();
			otherTeam = grid.getOtherTeam();
		} else if (isCommandEvent())
			error("You must be playing Battleship to use this command");
	}

	private static final String[] aimMenuLines = {"", ARROWS, "Enter a", "coordinate (A0)"};

	@Path("aim [coordinate]")
	void aim(@Arg(permission = "minigames.manage") Coordinate coordinate) {
		if (coordinate != null)
			coordinate.aim();
		else
			Nexus.getSignMenuFactory()
					.lines(aimMenuLines)
					.prefix(PREFIX)
					.response(lines -> grid.getCoordinate(lines[0]).aim())
					.open(player());
	}

	@Path("aim random")
	void fireRandom() {
		grid.getRandomCoordinate().aim();
	}

	@Path("belay")
	void belay() {
		grid.belay();
	}

	@Path("fire [coordinate]")
	void fire(@Arg(permission = "minigames.manage") Coordinate coordinate) {
		if (coordinate == null)
			coordinate = grid.getAiming();
		if (coordinate == null)
			error("You have not aimed your cannon yet");

		coordinate.fire();
		mechanic.nextTurn(match);
	}

	@Path("fire random")
	@Permission("minigames.manage")
	void toolsFireRandom() {
		grid.getRandomCoordinate().fire();
		mechanic.nextTurn(match);
	}

	@Path("testWithKoda [start]")
	@Permission("minigames.manage")
	void testWithKoda(@Arg("true") boolean start) {
		runCommand("mcmd mgm join alphavs ;; sudo koda mgm join alphavs ;; wait 20 ;; mgm start" + (start ? " ;; wait 20 ;; bs start" : ""));
	}

	@Path("start")
	@Permission("minigames.manage")
	void start() {
		mechanic.begin(match);
	}

	@Path("kit")
	@Permission("minigames.manage")
	void kit() {
		Arrays.asList(ShipType.values()).forEach(shipType -> PlayerUtils.giveItem(player(), shipType.getItem()));
	}

	@Path("hideShips")
	@Permission("minigames.manage")
	void hideShips() {
		mechanic.hideShips(match, otherTeam);
	}

	@Path("debug")
	@Permission("minigames.manage")
	void debug() {
		debug = !debug;
		send(PREFIX + "Debug " + (debug ? "&aenabled" : "&cdisabled"));
	}

	@Path("getChatGrid")
	@Permission("minigames.manage")
	void getChatGrid() {
		grid.getChatGrid().forEach(this::send);
	}

	@Permission("minigames.manage")
	@Path("pasteShip <shipType> <direction>")
	void pasteShip(ShipType shipType, CardinalDirection direction) {
		mechanic.pasteShip(shipType, location(), direction);
	}

	@Permission("minigames.manage")
	@Path("toKitLocation <coordinate>")
	void toKitLocation(Coordinate coordinate) {
		minigamer.teleport(getCenteredLocation(coordinate.getKitLocation()));
	}

	@Permission("minigames.manage")
	@Path("toPegLocation <coordinate>")
	void toPegLocation(Coordinate coordinate) {
		minigamer.teleport(getCenteredLocation(coordinate.getPegLocation()));
	}

	@ConverterFor(Coordinate.class)
	Coordinate convertToCoordinate(String value, Team context) {
		if (context == null)
			context = minigamer.getTeam();

		return matchData.getGrid(context).getCoordinate(value);
	}

	@TabCompleterFor(Coordinate.class)
	List<String> tabCompleteCoordinate(String filter) {
		return Battleship.COORDINATES.stream()
				.filter(name -> name != null && name.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

}
