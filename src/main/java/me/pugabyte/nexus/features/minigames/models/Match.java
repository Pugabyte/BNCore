package me.pugabyte.nexus.features.minigames.models;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.google.common.base.Strings;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import eden.utils.TimeUtils.Timespan.FormatType;
import eden.utils.TimeUtils.Timespan.TimespanBuilder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import me.pugabyte.nexus.features.commands.staff.admin.RebootCommand;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.models.Match.MatchTasks.MatchTaskType;
import me.pugabyte.nexus.features.minigames.models.annotations.TeamGlowing;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchBroadcastEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchInitializeEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchTimerTickEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.sabotage.MinigamerDisplayTimerEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.teams.TeamScoredEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams.TeamMechanic;
import me.pugabyte.nexus.features.minigames.models.modifiers.MinigameModifier;
import me.pugabyte.nexus.features.minigames.models.scoreboards.MinigameScoreboard;
import me.pugabyte.nexus.features.minigames.modifiers.NoModifier;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.utils.BossBarBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundUtils.Jingle;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Tasks.Countdown.CountdownBuilder;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.inventivetalent.glow.GlowAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

@Data
public class Match implements ForwardingAudience {
	@NonNull
	private Arena arena;
	@ToString.Exclude
	private final List<Minigamer> minigamers = new ArrayList<>();
	@ToString.Exclude
	private final List<Minigamer> allMinigamers = new ArrayList<>();
	private boolean initialized = false;
	private boolean started = false;
	private boolean begun = false;
	private boolean ended = false;
	private final Map<Team, Integer> scores = new HashMap<>();
	private MatchTimer timer;
	private MinigameScoreboard scoreboard;
	private MinigameScoreboard.ITeams scoreboardTeams;
	private final ArrayList<Entity> entities = new ArrayList<>();
	private final ArrayList<Hologram> holograms = new ArrayList<>();
	private MatchData matchData;
	private MatchTasks tasks;
	private final Set<Location> usedSpawnpoints = new HashSet<>();
	@Nullable
	private BossBar modifierBar;
	private final Map<Team, Integer> glowUpdates = new HashMap<>();

	public @Nullable Minigamer getMinigamer(@NotNull Player player) {
		for (Minigamer minigamer : minigamers)
			if (minigamer.getPlayer().equals(player))
				return minigamer;

		return null;
	}

	public List<Player> getPlayers() {
		return minigamers.stream().map(Minigamer::getPlayer).collect(Collectors.toList());
	}

	public List<Player> getAllPlayers() {
		return allMinigamers.stream().map(Minigamer::getPlayer).collect(Collectors.toList());
	}

	/**
	 * Gets all teams with living (i.e. not spectating) players.
	 * @return list of teams
	 */
	public List<Team> getAliveTeams() {
		// collects to a set first to remove duplicates
		return getAliveMinigamers().stream().map(Minigamer::getTeam).filter(Objects::nonNull).distinct().collect(Collectors.toList());
	}

	public <T extends Arena> T getArena() {
		return (T) arena;
	}

	public <T extends MatchData> T getMatchData() {
		return (T) matchData;
	}

	public <T extends Mechanic> T getMechanic() {
		return arena.getMechanic();
	}

	public World getWorld() {
		return arena.getWorld();
	}

	public WorldGuardUtils getWGUtils() {
		return arena.getWGUtils();
	}

	public WorldEditUtils getWEUtils() {
		return arena.getWEUtils();
	}

	public boolean join(Minigamer minigamer) {
		if (arena.getName().equals("RavensNestEstate")) {
			minigamer.tell("This arena is temporarily disabled while we work out some bugs");
			return false;
		}

		if (!initialized && RebootCommand.isQueued()) {
			minigamer.tell("Server reboot is queued, cannot start a new match");
			return false;
		}

		initialize();

		if (started && !arena.canJoinLate()) {
			minigamer.tell("This match has already started");
			return false;
		}

		if (minigamers.size() >= arena.getMaxPlayers()) {
			minigamer.tell("This match is full");
			return false;
		}

		MatchJoinEvent event = new MatchJoinEvent(this, minigamer);
		if (!event.callEvent()) return false;

		minigamers.add(minigamer);

		try {
			arena.getMechanic().processJoin(minigamer);
			arena.getMechanic().onJoin(event);
		} catch (Exception ex) { ex.printStackTrace(); }

		if (scoreboard != null) scoreboard.handleJoin(minigamer);
		if (scoreboardTeams != null) scoreboardTeams.handleJoin(minigamer);

		return true;
	}

	void quit(Minigamer minigamer) {
		if (!minigamers.contains(minigamer)) return;

		MinigamerQuitEvent event = new MinigamerQuitEvent(minigamer);
		event.callEvent();
		try { arena.getMechanic().onQuit(event); } catch (Exception ex) { ex.printStackTrace(); }
		if (event.isCancelled()) return;

		minigamers.remove(minigamer);
		minigamer.clearState(true);
		minigamer.toGamelobby();
		minigamer.unhideAll();

		if (modifierBar != null) minigamer.getPlayer().hideBossBar(modifierBar);
		if (scoreboard != null) scoreboard.handleQuit(minigamer);
		if (scoreboardTeams != null) scoreboardTeams.handleQuit(minigamer);
		GlowAPI.setGlowing(getPlayers(), GlowAPI.Color.NONE, minigamer.getPlayer());

		if (minigamers.size() == 0)
			end();
	}

	public void start() {
		if (started)
			throw new InvalidInputException("Match already started");

		MatchStartEvent event = new MatchStartEvent(this);
		if (!event.callEvent()) return;

		started = true;

		try {
			allMinigamers.addAll(minigamers);
			clearEntities();
			balance();
			initializeScores();
			teleportIn();
			startModifierBar();
			startTimer(); // -> arena.getMechanic().startTimer();
			arena.getMechanic().onStart(event);
			if (scoreboard != null) scoreboard.update();
			if (scoreboardTeams != null) scoreboardTeams.update();
		} catch (Exception ex) {
			ex.printStackTrace();
			end();
		}
	}

	public void end() {
		if (ended) return;

		MatchEndEvent event = new MatchEndEvent(this);
		if (!event.callEvent()) return;

		ended = true;
		if (tasks != null)
			tasks.end();
		broadcast("Match has ended");
		logScores();
		broadcastNoPrefix("");
		clearHolograms();
		clearEntities();
		clearStates();
		stopModifierBar();
		toGamelobby();
		try {
			arena.getMechanic().onEnd(event);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		minigamers.clear();

		if (scoreboard != null) scoreboard.handleEnd();
		if (scoreboardTeams != null) scoreboardTeams.handleEnd();

		List<Player> players = getPlayers();
		GlowAPI.setGlowing(players, GlowAPI.Color.NONE, players);

		MatchManager.remove(this);
	}

	private void logScores() {
		StringBuilder scores = new StringBuilder();
		if (arena.getMechanic() instanceof TeamMechanic) {
			getArena().getTeams().stream().sorted(Comparator.comparing((Team team) -> team.getScore(this)).reversed()).forEach(team ->
					scores.append("= ").append(team.getName()).append(" - ").append(team.getScore(this)).append(System.lineSeparator()));
		}

		getAllMinigamers().stream().sorted(Comparator.comparing(Minigamer::getScore).reversed()).forEach(minigamer ->
				scores.append("- ").append(minigamer.getNickname()).append(" - ").append(minigamer.getScore()).append(System.lineSeparator()));

		if (scores.length() > 0) {
			String header = "Scores for " + getArena().getName() + " (" + arena.getMechanic().getName() + "):" + System.lineSeparator();
			Discord.staffLog("```" + header + scores + "```");
		}
	}

	private void initialize() {
		if (!initialized) {
			try {
				MatchInitializeEvent event = new MatchInitializeEvent(this);
				if (!event.callEvent()) return;

				initializeMatchData();
				tasks = new MatchTasks();
				scoreboard = MinigameScoreboard.Factory.create(this);
				scoreboardTeams = MinigameScoreboard.ITeams.Factory.create(this);
				arena.getMechanic().onInitialize(event);
				initialized = true;
			} catch (Exception ex) {
				ex.printStackTrace();
				end();
			}
		}
	}

	@SneakyThrows
	private void initializeMatchData() {
		Map<Mechanic, Constructor<?>> matchDataMap = Minigames.getMatchDataMap();
		if (matchDataMap.isEmpty())
			Minigames.registerMatchDatas();

		if (matchDataMap.containsKey(arena.getMechanic()))
			matchData = (MatchData) matchDataMap.get(arena.getMechanic()).newInstance(this);
		else
			matchData = new MatchData(this);
	}

	private void startModifierBar() {
		MinigameModifier modifier = Minigames.getModifier();
		if (modifier instanceof NoModifier) return;
		modifierBar = new BossBarBuilder().title(modifier.asComponent()).color(BossBar.Color.BLUE).build();
		getMinigamers().forEach(minigamer -> minigamer.getPlayer().showBossBar(modifierBar));
	}

	private void stopModifierBar() {
		if (modifierBar == null) return;
		getAllPlayers().forEach(player -> player.hideBossBar(modifierBar));
	}

	private void startTimer() {
		timer = new MatchTimer(this, arena.getSeconds());
	}

	private void stopTimer() {
		if (timer != null)
			timer.stop();
	}

	public void handleGlow(Team team) {
		if (false && team != null && getMechanic().getAnnotation(TeamGlowing.class) != null && !glowUpdates.containsKey(team)) {
			// TODO: send potion effect packets instead of using GlowAPI (it creates its own scoreboard for colors which messes with things
			AtomicInteger taskId = new AtomicInteger(-1);
			taskId.set(tasks.wait(1, () -> {
				List<Player> teamMembers = team.getMinigamers(this).stream().map(Minigamer::getPlayer).collect(Collectors.toList());
				List<Player> otherPlayers = new ArrayList<>(PlayerUtils.getOnlinePlayers());
				otherPlayers.removeAll(teamMembers);
				GlowAPI.setGlowing(teamMembers, team.getColorType().getGlowColor(), teamMembers);
				GlowAPI.setGlowing(otherPlayers, GlowAPI.Color.NONE, teamMembers);
				glowUpdates.remove(team, taskId.get());
			}));
			glowUpdates.put(team, taskId.get());
		}
	}

	private static List<EntityType> deletableTypes = Arrays.asList(EntityType.ARROW, EntityType.SPECTRAL_ARROW, EntityType.DROPPED_ITEM);

	private void clearEntities() {
		entities.forEach(Entity::remove);
		getWorld().getEntities().forEach(entity -> {
			if (getArena().getRegion().contains(getWEUtils().toBlockVector3(entity.getLocation())))
				if (deletableTypes.contains(entity.getType()))
					entity.remove();
		});
	}

	public void clearHolograms() {
		holograms.forEach(Hologram::delete);
		holograms.clear();
	}

	private void balance() {
		arena.getMechanic().balance(minigamers);
		if (arena.getMechanic() instanceof TeamMechanic)
			minigamers.forEach(minigamer -> minigamer.tell("You are on team " + minigamer.getTeam().getColoredName()));
	}

	private void initializeScores() {
		arena.getTeams().forEach(team -> scores.put(team, 0));
	}

	private void teleportIn() {
		Set<Location> usedSpawnpoints = new HashSet<>();
		arena.getTeams().forEach(team -> team.spawn(this));
	}

	public void teleportIn(Minigamer minigamer) {
		minigamer.getTeam().spawn(minigamer);
	}

	private void clearStates() {
		minigamers.forEach(Minigamer::clearState);
	}

	private void toGamelobby() {
		minigamers.forEach(Minigamer::toGamelobby);
	}

	public void scored(Team team) {
		scored(team, 1);
	}

	public void scored(Team team, int score) {
		setScore(team, scores.getOrDefault(team, 0) + score);
	}

	public void setScore(Team team, int score) {
		int diff = score - scores.getOrDefault(team, 0);

		TeamScoredEvent event = new TeamScoredEvent(this, team, diff);
		if (!event.callEvent()) return;

		scores.put(team, scores.getOrDefault(team, 0) + event.getAmount());
		scoreboard.update();
	}

	public void broadcast(String message) {
		if (Strings.isNullOrEmpty(message)) {
			broadcastNoPrefix("");
			return;
		}

		MatchBroadcastEvent event = new MatchBroadcastEvent(this, message);
		event.callEvent();
		if (!event.isCancelled())
			minigamers.forEach(minigamer -> minigamer.tell(colorize(event.getMessage())));
	}

	public void broadcastNoPrefix(String message) {
		MatchBroadcastEvent event = new MatchBroadcastEvent(this, message);
		if (event.callEvent())
			minigamers.forEach(minigamer -> minigamer.sendMessage(colorize(event.getMessage())));
	}

	public void broadcast(Team team, String message) {
		MatchBroadcastEvent event = new MatchBroadcastEvent(this, message, team);
		if (event.callEvent()) {
			minigamers.stream()
					.filter(minigamer -> minigamer.getTeam().equals(event.getTeam()))
					.collect(Collectors.toSet())
					.forEach(minigamer -> minigamer.tell(colorize(event.getMessage())));
		}
	}

	public void playSound(Jingle jingle) {
		getPlayers().forEach(jingle::play);
	}

	public List<Minigamer> getAliveMinigamers() {
		return minigamers.stream().filter(Minigamer::isAlive).collect(Collectors.toList());
	}

	public List<Player> getAlivePlayers() {
		return minigamers.stream().filter(Minigamer::isAlive).map(Minigamer::getPlayer).collect(Collectors.toList());
	}

	public List<Minigamer> getUnassignedPlayers() {
		return minigamers.stream()
				.filter(waiting -> waiting.getTeam() == null)
				.collect(Collectors.toList());
	}

	public boolean isMechanic(Mechanic mechanic) {
		return isMechanic(mechanic.getClass());
	}

	public boolean isMechanic(Class<? extends Mechanic> mechanic) {
		return mechanic.isAssignableFrom(getMechanic().getClass());
	}

	public <T extends Entity> T spawn(Location location, Class<T> type) {
		T entity = location.getWorld().spawn(location, type);
		entities.add(entity);
		return entity;
	}

	public int getWinningScore() {
		return arena.getCalculatedWinningScore(this);
	}

	@Override
	public @org.checkerframework.checker.nullness.qual.NonNull Iterable<? extends Audience> audiences() {
		return getMinigamers();
	}

	public static class MatchTimer {
		private final Match match;
		private static final List<Integer> broadcasts = Arrays.asList((60 * 10), (60 * 5), 60, 30, 15, 5, 4, 3, 2, 1);
		private int taskId;
		@Getter
		@Setter
		private int time;

		MatchTimer(Match match, int time) {
			this.match = match;
			this.time = time;
			start();
		}

		public void addTime(int time) {
			this.time += time;
		}

		void start() {
			if (time > 0) {
				match.getTasks().wait(1, () -> broadcastTimeLeft(time + 1));
				taskId = match.getTasks().repeat(0, Time.SECOND, () -> {
					if (--time > 0) {
						if (broadcasts.contains(time)) {
							broadcastTimeLeft();
							match.getPlayers().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, .75F, .6F));
						}
						match.getMinigamers().forEach(player -> {
							MinigamerDisplayTimerEvent event = new MinigamerDisplayTimerEvent(player, Component.text(Timespan.of(time).format()), time);
							if (event.callEvent())
								player.sendActionBar(event.getContents());
						});
						MatchTimerTickEvent event = new MatchTimerTickEvent(match, time);
						event.callEvent();
					} else {
						match.end();
						stop();
					}
				});
			} else {
				taskId = match.getTasks().repeat(0, Time.SECOND, () -> {
					MatchTimerTickEvent event = new MatchTimerTickEvent(match, ++time);
					event.callEvent();
				});
			}

			match.getTasks().register(MatchTaskType.MATCH, taskId);
		}

		public void broadcastTimeLeft() {
			broadcastTimeLeft(time);
		}

		public void broadcastTimeLeft(int time) {
			match.broadcast("&e" + TimespanBuilder.of(time).format(FormatType.LONG) + " &7left...");
		}

		void stop() {
			Tasks.cancel(taskId);
		}
	}

	public static class MatchTasks {
		private final Set<Integer> taskIds = new HashSet<>();
		@Getter
		private final Map<MatchTaskType, Integer> taskTypeMap = new ConcurrentHashMap<>();

		void end() {
			List<Integer> tasks = new ArrayList<>(taskIds);
			tasks.forEach(this::cancel);
		}

		public void cancel(int taskId) {
			Tasks.cancel(taskId);
			taskIds.remove(taskId);
		}

		/**
		 * Cancels a task. If <code>taskId</code> is not <code>-1</code>, the corresponding task will be cancelled, and
		 * the <code>taskType</code> will be removed from the internal map if its saved task id matches.
		 * <p>
		 * If <code>taskId</code> is -1, the saved task for <code>taskType</code> will be removed and cancelled
		 * (if present).
		 * @param taskType a match task type
		 * @param taskId a task id or -1
		 */
		public void cancel(@NotNull MatchTaskType taskType, int taskId) {
			if (taskId != -1) {
				Tasks.cancel(taskId);
				taskTypeMap.remove(taskType, taskId);
			} else if (taskTypeMap.containsKey(taskType))
				Tasks.cancel(taskTypeMap.remove(taskType));
		}

		public void cancel(MatchTaskType taskType) {
			cancel(taskType, -1);
		}

		public void register(int taskId) {
			taskIds.add(taskId);
		}

		public void register(MatchTaskType taskType, int taskId) {
			cancel(taskType);
			taskTypeMap.put(taskType, taskId);
		}

		public boolean registered(MatchTaskType taskType) {
			return taskTypeMap.containsKey(taskType);
		}

		public int wait(Time delay, Runnable runnable) {
			return wait(delay.get(), runnable);
		}

		public int wait(long delay, Runnable runnable) {
			int taskId = Tasks.wait(delay, runnable);
			taskIds.add(taskId);
			Tasks.wait(delay, () -> taskIds.remove(taskId));
			return taskId;
		}

		public int waitAsync(Time delay, Runnable runnable) {
			return waitAsync(delay.get(), runnable);
		}

		public int waitAsync(long delay, Runnable runnable) {
			int taskId = Tasks.waitAsync(delay, runnable);
			taskIds.add(taskId);
			Tasks.wait(delay, () -> taskIds.remove(taskId));
			return taskId;
		}

		public int repeat(Time startDelay, long interval, Runnable runnable) {
			return repeat(startDelay.get(), interval, runnable);
		}

		public int repeat(long startDelay, Time interval, Runnable runnable) {
			return repeat(startDelay, interval.get(), runnable);
		}

		public int repeat(Time startDelay, Time interval, Runnable runnable) {
			return repeat(startDelay.get(), interval.get(), runnable);
		}

		public int repeat(long startDelay, long interval, Runnable runnable) {
			int taskId = Tasks.repeat(startDelay, interval, runnable);
			taskIds.add(taskId);
			return taskId;
		}

		public int repeatAsync(Time startDelay, long interval, Runnable runnable) {
			return repeatAsync(startDelay.get(), interval, runnable);
		}

		public int repeatAsync(long startDelay, Time interval, Runnable runnable) {
			return repeatAsync(startDelay, interval.get(), runnable);
		}

		public int repeatAsync(Time startDelay, Time interval, Runnable runnable) {
			return repeatAsync(startDelay.get(), interval.get(), runnable);
		}

		public int repeatAsync(long startDelay, long interval, Runnable runnable) {
			int taskId = Tasks.repeatAsync(startDelay, interval, runnable);
			taskIds.add(taskId);
			return taskId;
		}

		public int sync(Runnable runnable) {
			int taskId = Tasks.sync(runnable);
			taskIds.add(taskId);
			Tasks.sync(() -> taskIds.remove(taskId));
			return taskId;
		}

		public int async(Runnable runnable) {
			int taskId = Tasks.async(runnable);
			taskIds.add(taskId);
			Tasks.async(() -> taskIds.remove(taskId));
			return taskId;
		}

		public int countdown(CountdownBuilder countdown) {
			int taskId = countdown.start().getTaskId();
			taskIds.add(taskId);
			return taskId;
		}

		public enum MatchTaskType {
			MATCH,
			LOBBY,
			BEGIN_DELAY,
			SCOREBOARD,
			TURN,
			TURN_TIMER,
			TICK
		}
	}

}
