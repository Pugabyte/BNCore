package me.pugabyte.nexus.features.minigames.models.matchdata;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import eden.utils.TimeUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lexikiq.HasUniqueId;
import me.pugabyte.nexus.features.menus.sabotage.AbstractVoteScreen;
import me.pugabyte.nexus.features.menus.sabotage.ResultsScreen;
import me.pugabyte.nexus.features.menus.sabotage.VotingScreen;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.mechanics.Sabotage;
import me.pugabyte.nexus.features.minigames.models.Loadout;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.MatchData;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.MatchDataFor;
import me.pugabyte.nexus.features.minigames.models.arenas.SabotageArena;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerLoadoutEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.sabotage.MinigamerVoteEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.SabotageColor;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.SabotageScoreboard;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.SabotageTeam;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.Task;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.TaskPart;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.Tasks;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.taskpartdata.SabotageTaskPartData;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotOnlineException;
import me.pugabyte.nexus.models.chat.PublicChannel;
import me.pugabyte.nexus.utils.BossBarBuilder;
import me.pugabyte.nexus.utils.ColorType;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PacketUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundUtils;
import me.pugabyte.nexus.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@MatchDataFor(Sabotage.class)
public class SabotageMatchData extends MatchData {
	public SabotageMatchData(Match match) {
		super(match);
		match.setScoreboardTeams(new SabotageScoreboard(match));
	}

	private final Map<UUID, UUID> votes = new HashMap<>();
	private final BiMap<UUID, SabotageColor> playerColors = HashBiMap.create();
	private LocalDateTime meetingStarted, meetingEnded = LocalDateTime.of(1970, 1, 1, 0, 0);
	private int meetingTaskID = -1;
	private AbstractVoteScreen votingScreen;
	private LocalDateTime roundStarted;
	private final Set<UUID> buttonUsers = new HashSet<>();
	private final BossBar bossbar = new BossBarBuilder().color(ColorType.GREEN).title("&aTask Completion").overlay(BossBar.Overlay.NOTCHED_12).progress(0).build();
	private int endMeetingTask = -1;
	private final PublicChannel gameChannel = PublicChannel.builder()
			.name("Sabotage")
			.nickname("!")
			.persistent(false)
			.permission("")
			.color(ChatColor.RED)
			.build();
	private final PublicChannel spectatorChannel = PublicChannel.builder()
			.name("Sabotage Spectator")
			.nickname("X")
			.persistent(false)
			.messageColor(ChatColor.GRAY)
			.color(ChatColor.DARK_GRAY)
			.permission("")
			.build();
	private final Map<UUID, Integer> killCooldowns = new HashMap<>();
	private Task sabotage;
	private int sabotageTaskId = -1;
	private int customSabotageTaskId = -1; // custom task created by the sabotage
	private LocalDateTime sabotageStarted;
	private final Map<UUID, Set<Task>> tasks = new HashMap<>();
	private final Map<UUID, Body> bodies = new HashMap<>(); // this is a map of Armor Stand UUIDs to their report locations
	private final Set<Tasks> commonTasks = randomTasks(Tasks.TaskType.COMMON);
	private final Map<UUID, Location> venters = new HashMap<>();
	private final Map<UUID, Location> lightMap = new HashMap<>();
	private Set<ArmorStandTask> armorStandTasks = null;

	private Set<ArmorStandTask> armorStandTasksInit() {
		Set<ArmorStandTask> set = new HashSet<>();
		getArena().getWGUtils().getEntitiesInRegionByClass(getArena().getProtectedRegion(), ArmorStand.class).forEach(armorStand -> {
			if (armorStand.getEquipment() == null) return;
			ItemStack item = armorStand.getEquipment().getHelmet();
			if (ItemUtils.isNullOrAir(item)) return;
			TaskPart part = TaskPart.get(item);
			if (part == null) return;
			BlockFace facing = armorStand.getFacing();
			LocationUtils.CardinalDirection direction = LocationUtils.CardinalDirection.of(facing);
			Vector corner1 = armorStand.getEyeLocation()
					.add(0, 2, 0)
					.add(facing.getDirection())
					.add(direction.turnRight().toVector()).toVector();
			Vector corner2 = armorStand.getLocation()
					.subtract(0, 2, 0)
					.add(direction.turnLeft().toVector()).toVector();
			set.add(new ArmorStandTask(armorStand.getUniqueId(), part, BoundingBox.of(corner1, corner2)));
		});
		return set;
	}

	@Data
	public static class ArmorStandTask {
		private final UUID uuid;
		private final TaskPart part;
		private final BoundingBox boundingBox;

		public @NotNull ArmorStand getEntity() {
			return (ArmorStand) Bukkit.getEntity(uuid);
		}
	}

	public @Nullable Task getNearbyTask(Minigamer minigamer) {
		Vector pos = minigamer.getPlayer().getLocation().toVector();
		Map<TaskPart, Task> tasks = new HashMap<>();
		getTasks(minigamer).forEach(task -> {
			TaskPart part = task.nextPart();
			if (part != null)
				tasks.put(part, task);
		});
		for (ArmorStandTask wrapper : armorStandTasks) {
			if (!wrapper.boundingBox.contains(pos))
				continue;
			if (tasks.containsKey(wrapper.part))
				return tasks.get(wrapper.part);
		}
		return null;
	}

	private Set<Tasks> randomTasks(Tasks.TaskType type) {
		List<Tasks> legalTasks = Tasks.getByType(type).stream().filter(tasks -> getArena().getTasks().contains(tasks)).collect(Collectors.toList());
		Collections.shuffle(legalTasks);
		List<Tasks> finalTasks = new ArrayList<>();
		Set<String> groups = new HashSet<>();
		legalTasks.forEach(task -> {
			String group = task.getGroup();
			if (groups.contains(group)) return;
			groups.add(group);
			finalTasks.add(task);
		});
		int maxTasks = getArena().maxTasksOf(type);
		while (finalTasks.size() > maxTasks)
			finalTasks.remove(0);
		return new HashSet<>(finalTasks);
	}

	public SabotageArena getArena() {
		return (SabotageArena) super.getArena();
	}

	public void putKillCooldown(HasUniqueId player) {
		killCooldowns.put(player.getUniqueId(), TimeUtils.Time.SECOND.x(getArena().getKillCooldown()));
	}

	public int canButtonIn() {
		return (int) Math.max(Duration.between(LocalDateTime.now(), roundStarted.plusSeconds(getArena().getMeetingCooldown())).getSeconds(), 0);
	}

	public boolean canButton() {
		return canButtonIn() == 0;
	}

	/**
	 * Gets the state of a player's ability to use the emergency meeting button
	 */
	public enum ButtonState {
		/**
		 * The round has just started and the button is on cooldown
		 */
		COOLDOWN,
		/**
		 * The player has already used their button
		 */
		USED,
		/**
		 * A sabotage is currently active and thus the button cannot be called
		 */
		SABOTAGE,
		/**
		 * Player is able to use the button to call an emergency meeting
		 */
		USABLE;
	}

	public ButtonState canButton(HasUniqueId reporter) {
		if (!canButton())
			return ButtonState.COOLDOWN;
		if (sabotage != null)
			return ButtonState.SABOTAGE;
		if (buttonUsers.contains(reporter.getUniqueId()))
			return ButtonState.USED;
		return ButtonState.USABLE;
	}

	public ButtonState button(HasUniqueId reporter) {
		ButtonState state = canButton(reporter);
		if (state == ButtonState.USABLE)
			buttonUsers.add(reporter.getUniqueId());
		return state;
	}

	public @NotNull SabotageColor getColor(HasUniqueId player) {
		playerColors.computeIfAbsent(player.getUniqueId(), $ -> RandomUtils.randomElement(Arrays.stream(SabotageColor.values()).filter(color -> !playerColors.containsValue(color)).collect(Collectors.toList())));
		return playerColors.get(player.getUniqueId());
	}

	public @Nullable SabotageColor getColorNoCompute(HasUniqueId player) {
		return playerColors.get(player.getUniqueId());
	}

	public boolean hasVoted(HasUniqueId player) {
		return votes.containsKey(player.getUniqueId());
	}

	public @Nullable Minigamer getVote(HasUniqueId player) {
		if (!votes.containsKey(player.getUniqueId()))
			return null;
		return PlayerManager.get(votes.get(player.getUniqueId()));
	}

	public @NotNull Set<Minigamer> getVotesFor(HasUniqueId player) {
		UUID uuid = player == null ? null : player.getUniqueId();
		return votes.entrySet().stream().filter(entry -> Objects.equals(entry.getValue(), uuid)).map(entry -> PlayerManager.get(entry.getKey())).collect(Collectors.toSet());
	}

	public int maxVotes() {
		List<Minigamer> minigamers = match.getAliveMinigamers();
		minigamers.add(null);
		return Utils.getMax(minigamers, minigamer -> getVotesFor(minigamer).size()).getInteger();
	}

	public boolean waitingToVote() {
		return LocalDateTime.now().isBefore(meetingStarted.plusSeconds(Sabotage.VOTING_DELAY));
	}

	public int votingStartsIn() {
		return waitingToVote() ? (1 + (int) Duration.between(LocalDateTime.now(), meetingStarted.plusSeconds(Sabotage.VOTING_DELAY)).getSeconds()) : 0;
	}

	public boolean vote(HasUniqueId voter, HasUniqueId target) {
		MinigamerVoteEvent event = new MinigamerVoteEvent(PlayerManager.get(voter), PlayerManager.get(target), (VotingScreen) votingScreen);
		event.setCancelled(votes.containsKey(voter.getUniqueId()) || waitingToVote());
		if (event.callEvent()) {
			votes.put(voter.getUniqueId(), target == null ? null : target.getUniqueId());
			if (match.getAliveMinigamers().size() == votes.size())
				endMeeting();
		}
		return event.isCancelled();
	}

	public void clearVotes() {
		votes.clear();
	}

	public boolean isMeetingActive() {
		return meetingTaskID != -1;
	}

	@Getter
	@RequiredArgsConstructor
	@EqualsAndHashCode
	public static class Body {
		private final SabotageColor playerColor;
		private final BoundingBox reportBoundingBox;
	}

	public void spawnBody(Minigamer minigamer) {
		Location location = minigamer.getPlayer().getLocation();
		while (MaterialTag.ALL_AIR.isTagged(location.getBlock().getRelative(0, -1, 0)) && location.getY() > location.getWorld().getMinHeight())
			location.setY(location.getY() - 1);
		location.setY(Math.floor(location.getY()));
		location.add(0, -1.4, 0);
		ArmorStand armorStand = minigamer.getMatch().spawn(location, ArmorStand.class);
		armorStand.setInvulnerable(true);
		armorStand.setInvisible(true);
		armorStand.setGravity(false);
		armorStand.getEquipment().setHelmet(getColor(minigamer).getHead());
		bodies.put(armorStand.getUniqueId(), new Body(getColor(minigamer), BoundingBox.of(location, 5, 8, 5)));
	}

	private void clearBodies() {
		new HashMap<>(bodies).forEach((uuid, $) -> {
			Entity armorStand = Bukkit.getEntity(uuid);
			if (armorStand != null) //failsafe
				armorStand.remove();
			bodies.remove(uuid);
		});
	}

	public void exitVent(Minigamer minigamer) {
		Player player = minigamer.getPlayer();
		getVenters().remove(player.getUniqueId());
		match.<Sabotage>getMechanic().onLoadout(new MinigamerLoadoutEvent(minigamer, new Loadout()));
	}

	public double getProgress() {
		double progress = 0;
		double total = 0;
		for (Map.Entry<UUID, Set<Task>> entry : tasks.entrySet()) {
			UUID uuid = entry.getKey();
			Set<Task> taskSet = entry.getValue();
			Minigamer minigamer;
			try {
				minigamer = PlayerManager.get(uuid);
			} catch (PlayerNotOnlineException e) {continue;}
			if (SabotageTeam.of(minigamer) == SabotageTeam.IMPOSTOR) continue;
			for (Task task : taskSet) {
				if (task.getTask().getTaskType() == Tasks.TaskType.SABOTAGE) continue;
				progress += task.getCompleted();
				total += task.getTaskSize();
			}
		}
		return progress / total;
	}

	public void startMeeting(Minigamer origin) {
		startMeeting(origin, null);
	}

	public void startMeeting(Minigamer origin, SabotageColor bodyReported) {
		if (sabotage != null && sabotageTaskId == -1)
			endSabotage(); // sabotage is non-persistent (i.e. reactor) so it should end when a meeting starts
		meetingStarted = LocalDateTime.now();
		votingScreen = new VotingScreen(origin, bodyReported);
		clearBodies();
		bossbar.progress((float) getProgress());
		meetingTaskID = match.getTasks().repeat(0, 2, () -> match.getMinigamers().forEach(minigamer -> {
			InventoryView openInv = minigamer.getPlayer().getOpenInventory();
			if (LocationUtils.blockLocationsEqual(minigamer.getPlayer().getLocation(), getArena().getRespawnLocation())) {
				if (openInv.getType() == InventoryType.CRAFTING) return;
				if (openInv.getTitle().equals(votingScreen.getInventory().getTitle())) return;
			} else
				minigamer.teleport(getArena().getRespawnLocation());
			openInv.close();
			votingScreen.open(minigamer);
		}));

		match.getMinigamers().forEach(minigamer -> {
			if (venters.containsKey(minigamer.getUniqueId()))
				exitVent(minigamer);
			PlayerUtils.hidePlayers(minigamer, match.getMinigamers());
			minigamer.teleport(getArena().getRespawnLocation());
			minigamer.getPlayer().getInventory().clear();
			PlayerUtils.giveItem(minigamer, Sabotage.VOTING_ITEM.get());
			match.getTasks().wait(1, () -> {
				votingScreen.open(minigamer);
				SoundUtils.Jingle.SABOTAGE_MEETING.play(minigamer);
			});
			if (SabotageTeam.of(minigamer) == SabotageTeam.IMPOSTOR)
				putKillCooldown(minigamer);
			minigamer.getPlayer().removePotionEffect(PotionEffectType.NIGHT_VISION);
		});
		endMeetingTask = match.getTasks().wait(TimeUtils.Time.SECOND.x(Sabotage.MEETING_LENGTH + Sabotage.VOTING_DELAY), this::endMeeting);
	}

	public void endMeeting() {
		if (votingScreen instanceof ResultsScreen)
			return;
		AbstractVoteScreen oldScreen = votingScreen;
		votingScreen = new ResultsScreen();
		meetingEnded = LocalDateTime.now();
		match.getTasks().cancel(meetingTaskID);
		match.getTasks().cancel(endMeetingTask);
		meetingTaskID = -1;
		endMeetingTask = -1;
		match.getMinigamers().forEach(minigamer -> {
			oldScreen.close(minigamer);
			votingScreen.open(minigamer);
			minigamer.getPlayer().getInventory().remove(Sabotage.VOTING_ITEM.get());
		});
		match.getTasks().wait(TimeUtils.Time.SECOND.x(Sabotage.POST_MEETING_DELAY), () -> {
			match.getMinigamers().forEach(minigamer -> votingScreen.close(minigamer));
			Minigamer ejected = null;
			int votes = getVotesFor(null).size();
			boolean tie = false;
			for (Minigamer minigamer : match.getAliveMinigamers()) {
				int mVotes = getVotesFor(minigamer).size();
				if (mVotes > votes) {
					ejected = minigamer;
					votes = mVotes;
					tie = false;
				} else if (mVotes == votes) {
					ejected = null;
					tie = true;
				}
			}

			String ejectedName;
			if (ejected != null) {
				ejectedName = ejected.getNickname();
			} else
				ejectedName = "Nobody";

			String display = ejectedName + " was ejected.";
			if (ejected == null)
				display += " (" + (tie ? "Tied" : "Skipped") + ")";

			// TODO: true animation
			match.showTitle(Title.title(Component.empty(), new JsonBuilder(display).build(), Title.Times.of(fade, Duration.ofSeconds(7), fade)));
			match.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, Sound.Source.PLAYER, 1.0F, 1.0F));
			clearVotes();
			votingScreen = null;
			roundStarted = LocalDateTime.now();
			match.getMinigamers().forEach(Minigamer::respawn);

			SabotageTeam team = SabotageTeam.of(ejected);
			if (team == SabotageTeam.JESTER) {
				ejected.scored();
				match.end();
			} else if (ejected != null)
				match.getMechanic().onDeath(new MinigamerDeathEvent(ejected));
		});
	}

	public void sabotage(Tasks sabotage) {
		this.sabotage = new Task(sabotage);
		sabotageStarted = LocalDateTime.now();
		SabotageTaskPartData data = sabotage.getParts()[0].createTaskPartData();
		int duration = data.getDuration();
		if (duration > 0)
			sabotageTaskId = match.getTasks().wait(TimeUtils.Time.SECOND.x(duration), () -> {
				SabotageTeam.IMPOSTOR.players(match).forEach(Minigamer::scored);
				match.end();
			});
		if (data.hasRunnable())
			customSabotageTaskId = match.getTasks().repeat(0, 1, () -> data.runnable(match));
		match.getMinigamers().forEach(this::initGlow);
	}

	public void endSabotage() {
		this.sabotage = null;
		if (sabotageTaskId != -1) {
			match.getTasks().cancel(sabotageTaskId);
			sabotageTaskId = -1;
		}
		if (customSabotageTaskId != -1) {
			match.getTasks().cancel(customSabotageTaskId);
			customSabotageTaskId = -1;
		}
		sabotageStarted = null;
		match.getMinigamers().forEach(this::initGlow);
	}

	public Set<Task> getTasks(Minigamer player) {
		tasks.computeIfAbsent(player.getUniqueId(), $ -> {
			Set<Tasks> ptasks = new HashSet<>(commonTasks);
			ptasks.addAll(randomTasks(Tasks.TaskType.LONG));
			ptasks.addAll(randomTasks(Tasks.TaskType.SHORT));
			return ptasks.stream().map(Task::new).collect(Collectors.toCollection(LinkedHashSet::new));
		});
		Set<Task> tasks = new LinkedHashSet<>(this.tasks.get(player.getUniqueId()));
		if (sabotage != null && player.isAlive())
			tasks.add(sabotage);
		return tasks;
	}

	public int lightLevel() {
		return (sabotage == null || sabotage.getTask() == Tasks.LIGHTS) ? 6 : 2;
	}

	public int getKillCooldown(HasUniqueId player) {
		return killCooldowns.getOrDefault(player.getUniqueId(), -1);
	}

	public int getKillCooldownAsSeconds(HasUniqueId player) {
		int ticks = getKillCooldown(player);
		if (ticks == -1) return -1;
		return (int) Math.ceil(ticks / 20d);
	}

	public void initGlow(Minigamer minigamer) {
		if (armorStandTasks == null)
			armorStandTasks = armorStandTasksInit();

		Player player = minigamer.getPlayer();
		List<ArmorStand> disable = new ArrayList<>();
		List<ArmorStand> enable = new ArrayList<>();

		Set<TaskPart> tasks;
		if (SabotageTeam.of(minigamer) == SabotageTeam.IMPOSTOR) {
			if (sabotage != null)
				tasks = Set.of(sabotage.nextPart());
			else
				tasks = Set.of();
		} else
			tasks = getTasks(minigamer).stream().map(Task::nextPart).filter(Objects::nonNull).collect(Collectors.toSet());

		armorStandTasks.forEach(armorStandTask -> {
			ArmorStand entity = armorStandTask.getEntity();
			if (tasks.contains(armorStandTask.part)) {
				enable.add(entity);
				ItemStack item = armorStandTask.part.getInteractionItem();
				if (item.getType() == Material.BARRIER)
					match.getTasks().wait(1, () -> PacketUtils.sendFakeItem(entity, minigamer, EXCLAMATION_ITEM.get(), EnumWrappers.ItemSlot.HEAD));
			} else {
				disable.add(entity);
				match.getTasks().wait(1, () -> PacketUtils.sendFakeItem(entity, minigamer, entity.getEquipment().getHelmet(), EnumWrappers.ItemSlot.HEAD));
			}
		});
	}

	private static final Duration fade = Duration.ofSeconds(1).dividedBy(2);
	private static final Supplier<ItemStack> EXCLAMATION_ITEM = () -> new ItemBuilder(Material.BARRIER).customModelData(2).build();

	public void setRoundStarted() {
		setRoundStarted(LocalDateTime.now());
	}
}
