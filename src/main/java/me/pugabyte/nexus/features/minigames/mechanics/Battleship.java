package me.pugabyte.nexus.features.minigames.mechanics;

import com.google.common.collect.Sets;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.commands.BattleshipCommand;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Match.MatchTasks.MatchTaskType;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.annotations.Regenerating;
import me.pugabyte.nexus.features.minigames.models.annotations.Scoreboard;
import me.pugabyte.nexus.features.minigames.models.arenas.BattleshipArena;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchBeginEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchInitializeEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData.Grid;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData.Ship;
import me.pugabyte.nexus.features.minigames.models.matchdata.BattleshipMatchData.ShipType;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams.TeamMechanic;
import me.pugabyte.nexus.features.minigames.models.scoreboards.MinigameScoreboard.Type;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.LocationUtils.CardinalDirection;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils.ActionGroup;
import me.pugabyte.nexus.utils.WorldEditUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static me.pugabyte.nexus.utils.ItemUtils.getTool;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.utils.StringUtils.getShortLocationString;
import static me.pugabyte.nexus.utils.Utils.attempt;

/*
	Regions:
		team_<team>
		(a0|b1)_(ships|pegs)_<team>
		ships
		config
		floor
		grid
 */

/*
TODO
	Ready up

 */

@Regenerating("board")
@Scoreboard(sidebarType = Type.TEAM)
public class Battleship extends TeamMechanic {
	private static final String PREFIX = StringUtils.getPrefix("Battleship");
	public static final String LETTERS = "ABCDEFGHIJ";
	public static final String SCHEMATIC_FOLDER = "minigames/battleship";
	public static final List<String> COORDINATES = new ArrayList<>() {{
		for (int number = 0; number < 10; number++)
			for (String letter : LETTERS.split(""))
				add(letter + number);
	}};
	protected static final List<Material> floorMaterials = Arrays.asList(Material.YELLOW_WOOL, Material.BLUE_CONCRETE, Material.BLACK_CONCRETE);

	private void debug(String message) {
		if (BattleshipCommand.isDebug())
			Nexus.log(StringUtils.stripColor(PREFIX + message));
	}

	@Override
	public @NotNull String getName() {
		return "Battleship";
	}

	@Override
	public @NotNull String getDescription() {
		return "Locate your opponent's ships before they find yours";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.OAK_BOAT);
	}

	@Override
	public @NotNull GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	@Override
	public boolean shuffleSpawnpoints() {
		return false;
	}

	@Override
	public boolean allowFly() {
		return true;
	}

	@Override
	public boolean usesTeamChannels() {
		return false;
	}

	@Override
	public @NotNull String getScoreboardTitle(@NotNull Match match) {
		return "&6&lBattleship";
	}

	@Override
	public @NotNull Map<String, Integer> getScoreboardLines(@NotNull Match match, @NotNull Team team) {
		BattleshipArena arena = match.getArena();
		BattleshipMatchData matchData = match.getMatchData();

		List<String> lines = new ArrayList<>();
		if (matchData.isPlacingKits()) {
			lines.add("&cPlace your kits!");
		} else {
			LocalDateTime to = matchData.getEnd() == null ? LocalDateTime.now() : matchData.getEnd();
			lines.add("&cTime: &e" + Timespan.of(matchData.getStart(), to).format());

			if (matchData.isEnding()) {
				lines.add("&cWinner: " + matchData.getWinnerTeam().getVanillaColoredName());
			} else {
				long turnDuration = matchData.getTurnStarted().until(LocalDateTime.now(), ChronoUnit.SECONDS);
				String timeLeft = Timespan.of(arena.getTurnTime() - turnDuration).format();
				if (team.equals(matchData.getTurnTeam()))
					lines.add("&cTurn over in: &e" + timeLeft);
				else
					lines.add("&cYour turn in: &e" + timeLeft);
			}
		}

		lines.add("&f");

		Team otherTeam = arena.getOtherTeam(team);

		lines.add("&6&l" + team.getName() + " Fleet &e(You)");
		lines.add("&0" + getProgressBar(matchData, team));
		lines.add("&6&l" + otherTeam.getName() + " Fleet");
		lines.add("&1" + getProgressBar(matchData, otherTeam));

		lines.add("&e");

		lines.add("&6&lHistory");
		for (int i = 1; i <= 4; i++) {
			String line = String.valueOf(i);
			if (matchData.getHistory().size() >= i)
				line += " " + matchData.getHistory().get(i - 1);
			lines.add(line);
		}

		return new HashMap<>() {{
			for (int i = lines.size(); i > 0; i--)
				put(lines.get(lines.size() - i), i);
		}};
	}

	public String getProgressBar(BattleshipMatchData matchData, Team team) {
		return StringUtils.progressBar(matchData.getGrid(team).getHealth(), ShipType.getCombinedHealth(), StringUtils.ProgressBarStyle.COUNT);
	}

	@Override
	public void onInitialize(@NotNull MatchInitializeEvent event) {
		Arena arena = event.getMatch().getArena();
		List<ItemStack> items = new ArrayList<>();
		for (ShipType shipType : ShipType.values())
			items.add(shipType.getItem());

		ItemStack[] inventory = items.toArray(new ItemStack[41]);

		for (Team team : arena.getTeams())
			team.getLoadout().setInventory(inventory);

		ArenaManager.write(arena);

		super.onInitialize(event);
	}

	@EventHandler
	public void onMatchStart(MatchStartEvent event) {
		Match match = event.getMatch();
		if (!match.isMechanic(this)) return;

		if (match.getPlayers().size() < 2) {
			match.broadcast("Cannot start match, not enough players");
			event.setCancelled(true);
			return;
		}

		for (Team team : match.getArena().getTeams())
			hideShips(match, team);

		match.getWEUtils().fixLight(match.getArena().getRegion("board"));
	}

	@Override
	public void onBegin(@NotNull MatchBeginEvent event) {
		super.onBegin(event);
		start(event.getMatch());
	}

	public void start(Match match) {
		match.getTasks().cancel(MatchTaskType.BEGIN_DELAY);
		BattleshipMatchData matchData = match.getMatchData();
		if (!matchData.isPlacingKits()) return;

		matchData.setStart(LocalDateTime.now());

		for (Team team : matchData.getShips().keySet()) {
			matchData.getShips().get(team).values().stream()
					.filter(ship -> ship.getOrigin() == null)
					.forEach(ship -> {
						if (!attempt(100, () -> KitPlacer.of(match, team, ship.getType()).run()))
							Nexus.warn("Could not place " + camelCase(ship.getType().name()) + " on team " + team.getName() + " in " + match.getArena().getName());
					});
		}

		matchData.setPlacingKits(false);

		matchData.getShips().forEach((team, ships) -> {
			for (ShipType shipType : ShipType.values())
				pasteShip(shipType, ships.get(shipType).getOrigin());
			Tasks.wait(10, () -> hideShips(match, team));
		});

		match.getMinigamers().forEach(minigamer -> {
			minigamer.getPlayer().getInventory().clear();
			minigamer.teleport(minigamer.getTeam().getSpawnpoints().get(1));
			minigamer.getPlayer().setGameMode(GameMode.ADVENTURE);
		});

		Tasks.wait(Time.SECOND, () -> match.getWEUtils().fixLight(match.getArena().getRegion("board")));

		Region floor = match.getArena().getRegion("floor");
		match.getArena().getWEUtils().replace(floor, Sets.newHashSet(BlockTypes.BLUE_CONCRETE, BlockTypes.YELLOW_WOOL), Collections.singleton(BlockTypes.WATER));
	}

	public void hideShips(Match match, Team team) {
		BattleshipMatchData matchData = match.getMatchData();
		Team otherTeam = matchData.getGrid(team).getOtherTeam();
		List<Minigamer> otherTeamMembers = otherTeam.getAliveMinigamers(match);

		Region region = match.getArena().getRegion("hideships_" + team.getName().toLowerCase());
		for (BlockVector3 vector : region) {
			Location location = match.getWEUtils().toLocation(vector);
			for (Minigamer minigamer : otherTeamMembers) {
				minigamer.getPlayer().sendBlockChange(location, Bukkit.createBlockData(Material.STONE));
				minigamer.getPlayer().sendBlockChange(location.add(0, 10, 0), Bukkit.createBlockData(Material.STONE));
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		event.setCancelled(true);

		ShipType shipType = ShipType.of(event.getBlockPlaced());
		if (shipType == null) return;

		placeKit(minigamer, shipType, event.getBlockAgainst());
	}

	public void placeKit(Minigamer minigamer, ShipType shipType, Block origin) {
		if (origin.getType() != Material.YELLOW_WOOL) return;

		BattleshipMatchData matchData = minigamer.getMatch().getMatchData();
		if (!matchData.isPlacingKits()) return;

		Location floor = origin.getLocation();
		if (!minigamer.getMatch().getArena().isInRegion(floor, "floor")) return;

		BlockFace direction = CardinalDirection.of(minigamer.getPlayer()).toBlockFace();
		if (KitPlacer.of(minigamer, shipType, floor.add(0, 3, 0), direction).run())
			minigamer.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
	}

	/*
		TODO Better support for ship config from far away
		Placing/breaking
			Allow clicking nearby block (blue concrete, barriers around the kit?)

		Rotating
			Player interact event is not thrown when right clicking air with no tool
			Option 1: Fill hotbar with some item
			Option 2:
				https://canary.discord.com/channels/289587909051416579/555462289851940864/781267632515055636
				[4:18 PM] ok: put invis armor stands between your eye perspective and the ships?
				[4:18 PM] ok: you could listen for entity right click
				[4:19 PM] ok: send an entity spawn armorstand packet invis etc, store the entity id in a hashmap somewhere, use plib to listen for interact packet with that entity id
				[4:20 PM] ok: or ig if you dont wanna use that use a normal invis non-persistent armorstand
	*/

	@Override
	public void onPlayerInteract(Minigamer minigamer, PlayerInteractEvent event) {
		super.onPlayerInteract(minigamer, event);

		if (event.getHand() != EquipmentSlot.HAND) return;

		Block block = minigamer.getPlayer().getTargetBlockExact(500);
		if (block == null) return;
		if (!canUseBlock(minigamer, block)) return;

		Match match = minigamer.getMatch();
		BattleshipMatchData matchData = match.getMatchData();
		Team team = minigamer.getTeam();

		if (matchData.isPlacingKits()) {
			ShipType shipType = ShipType.of(block);
			if (shipType == null) {
				if (event.getClickedBlock() != null) return;

				ItemStack tool = getTool(minigamer.getPlayer());
				if (tool == null) return;

				shipType = ShipType.of(tool.getType());
				if (shipType == null) return;
			}

			if (ActionGroup.RIGHT_CLICK.applies(event)) {
				if (block.getType() == Material.YELLOW_WOOL) {
					placeKit(minigamer, shipType, block);
				} else {
					BlockFace direction = getKitDirection(block.getLocation());

					if (direction != null)
						KitPlacer.of(minigamer, shipType, block.getLocation(), getNextDirection(direction)).run();
				}
			} else if (ActionGroup.LEFT_CLICK.applies(event)) {
				event.setCancelled(true);
				minigamer.sendMessage(PREFIX + "Removed " + shipType.getColoredName());
				deleteKit(block.getLocation());
				matchData.getGrid(team).vacate(shipType);
				giveKitItem(minigamer, shipType);
			}
		}
	}

	private void giveKitItem(Minigamer minigamer, ShipType shipType) {
		PlayerInventory inventory = minigamer.getPlayer().getInventory();
		if (inventory.getItemInMainHand().getType() == Material.AIR)
			inventory.setItemInMainHand(shipType.getItem());
		else
			PlayerUtils.giveItem(minigamer.getPlayer(), shipType.getItem());
	}

	private void deleteKit(Location location) {
		location.getBlock().setType(Material.AIR);
		for (Block block : BlockUtils.getBlocksInRadius(location, 1))
			if (Tag.WOOL.isTagged(block.getType())) {
				block.setType(Material.AIR);
				deleteKit(block.getLocation());
			}
	}

	private static class KitPlacer {
		private final Match match;
		private final BattleshipMatchData matchData;
		private final Battleship mechanic;
		private final Minigamer minigamer;
		private final Team team;
		private final Grid grid;
		private final ShipType shipType;
		private final Ship ship;
		private final Location location;
		private BlockFace direction;
		private int attempts;

		public KitPlacer(Match match, Minigamer minigamer, Team team, ShipType shipType, Location location, BlockFace direction) {
			this.match = match;
			this.matchData = match.getMatchData();
			this.mechanic = match.getMechanic();
			this.minigamer = minigamer;
			this.team = team;
			this.grid = matchData.getGrid(team);
			this.shipType = shipType;
			this.ship = matchData.getShip(team, shipType);
			this.location = location;
			this.direction = direction;
		}

		public static KitPlacer of(Match match, Team team, ShipType shipType) {
			BattleshipMatchData matchData = match.getMatchData();
			Grid grid = matchData.getGrid(team);
			Location location = grid.getRandomCoordinate().getKitLocation();
			return of(match, null, team, shipType, location, LocationUtils.CardinalDirection.random().toBlockFace());
		}

		public static KitPlacer of(Minigamer minigamer, ShipType shipType, Location location, BlockFace direction) {
			return of(minigamer.getMatch(), minigamer, minigamer.getTeam(), shipType, location, direction);
		}

		public static KitPlacer of(Match match, Minigamer minigamer, Team team, ShipType shipType, Location location, BlockFace direction) {
			return new KitPlacer(match, minigamer, team, shipType, location, direction);
		}

		private boolean run() {
			Consumer<String> send = message -> { if (minigamer != null) minigamer.sendMessage(PREFIX + message); };

			if (attempts >= 4) {
				if (location.getBlock().getType() == shipType.getItem().getType())
					send.accept("Your " + shipType.getColoredName() + " &3could not be rotated");
				else
					send.accept("Your " + shipType.getColoredName() + " &3doesnt fit there");
				return false;
			}

			++attempts;

			if (!kitFits(location, direction, shipType.getKitLength())) {
				this.direction = mechanic.getNextDirection(direction);
				return run();
			}

			if (location.getBlock().getType() == shipType.getItem().getType()) {
				mechanic.deleteKit(location);
				send.accept("Rotated " + shipType.getColoredName());
			} else {
				if (location.getBlock().getType() != Material.AIR) {
					send.accept("Your " + shipType.getColoredName() + " &3doesnt fit there");
					return false;
				}

				send.accept("Placed " + shipType.getColoredName());
			}

			grid.vacate(shipType);

			location.getBlock().setType(shipType.getItem().getType());
			grid.getCoordinate(location).occupy(ship);
			ship.setOrigin(location);

			Block index = location.getBlock();
			for (int i = 0; i < shipType.getKitLength(); i++) {
				index = index.getRelative(direction);
				index.setType(Material.WHITE_WOOL);
				grid.getCoordinate(index.getLocation()).occupy(ship);
			}

			mechanic.hideShips(match, grid.getOtherTeam());

			return true;
		}

		private boolean kitFits(Location location, BlockFace direction, int kitLength) {
			Block index = location.getBlock();
			for (int i = 0; i < kitLength; i++) {
				index = index.getRelative(direction);
				if (index.getType() != Material.AIR)
					return false;

				Material floorType = index.getLocation().add(0, -3, 0).getBlock().getType();
				if (!floorMaterials.contains(floorType))
					return false;
			}

			return true;
		}
	}

	private BlockFace getNextDirection(BlockFace direction) {
		if (direction == null)
			return BlockFace.NORTH;

		return LocationUtils.CardinalDirection.of(direction).turnRight().toBlockFace();
	}

	private void pasteShip(ShipType shipType, Location location) {
		BlockFace direction = getKitDirection(location).getOppositeFace();
		deleteKit(location);
		pasteShip(shipType, location, LocationUtils.CardinalDirection.of(direction));
	}

	public void pasteShip(ShipType shipType, Location location, CardinalDirection direction) {
		String schematic = shipType.getFileName();
		debug("Pasting schematic " + schematic + " at " + getShortLocationString(location) + " with rotation " + direction.getRotation());
		new WorldEditUtils(location).paster()
				.file(schematic)
				.at(location)
				.transform(direction.getRotationTransform())
				.paste();
	}

	public BlockFace getKitDirection(Location location) {
		List<Block> blocks = BlockUtils.getBlocksInRadius(location, 1);
		BlockFace direction = null;
		for (Block block : blocks)
			if (block.getType() == Material.WHITE_WOOL)
				if (LocationUtils.CardinalDirection.isCardinal(block.getFace(location.getBlock())))
					direction = block.getFace(location.getBlock()).getOppositeFace();

		debug("Kit direction: " + (direction == null ? "null" : direction.name().toLowerCase()));
		return direction;
	}

	@Override
	public void onTurnStart(@NotNull Match match, @NotNull Team team) {
		match.broadcast(team, "Your turn");

		super.onTurnStart(match, team);
	}

	@Override
	public void onTurnEnd(@NotNull Match match, @NotNull Team team) {
		BattleshipMatchData matchData = match.getMatchData();
		Grid grid = matchData.getGrid(team);
		if (grid.getAiming() != null) {
			match.broadcast(team, "Time is up, captain! Firing at current position");
			grid.getAiming().fire();
		} else if (!matchData.isFired()) {
			match.broadcast(team, "Time is up, captain! Firing at random position");
			grid.getRandomCoordinate().fire();
		}

		matchData.setFired(false);

		super.onTurnEnd(match, team);
	}

	@Override
	public void end(@NotNull Match match) {
		BattleshipMatchData matchData = match.getMatchData();
		matchData.end();
		match.broadcast(matchData.getWinnerTeam().getColoredName() + " &3won!");
		Tasks.wait(Time.SECOND.x(10), () -> super.end(match));
	}

}
