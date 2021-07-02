package me.pugabyte.nexus.features.minigames.models;

import com.google.common.base.Strings;
import de.myzelyam.api.vanish.VanishAPI;
import eden.utils.TimeUtils.Time;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.lexikiq.PlayerLike;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.commands.SpeedCommand;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerScoredEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams.TeamMechanic;
import me.pugabyte.nexus.features.minigames.models.perks.Perk;
import me.pugabyte.nexus.framework.interfaces.Colored;
import me.pugabyte.nexus.framework.interfaces.IsColoredAndNicknamed;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.TitleUtils;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.WorldGroup;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.pugabyte.nexus.utils.LocationUtils.blockLocationsEqual;
import static me.pugabyte.nexus.utils.PlayerUtils.hidePlayer;
import static me.pugabyte.nexus.utils.PlayerUtils.showPlayer;
import static me.pugabyte.nexus.utils.StringUtils.colorize;

@Data
@EqualsAndHashCode(exclude = "match")
public final class Minigamer implements IsColoredAndNicknamed, PlayerLike, Colored {
	@NotNull
	private Player player;
	@ToString.Exclude
	private Match match;
	@Nullable
	private Team team;
	private int score = 0;
	/**
	 * Number that represents the player's participation in the game. Should be used to track events like kills,
	 * capturing flags, etc. Mostly used for team games where participation is more complex than just kill counts.
	 */
	private int contributionScore = 0;
	@Accessors(fluent = true)
	private boolean canTeleport;
	private boolean respawning = false;
	private boolean isAlive = true;
	private int lives;
	private int immobileTicks = 0;
	private int lastStruckTicks = 0;
	@MonotonicNonNull
	private Location lastLocation = null;
	// 1/2 = half a heart, /2s = half a heart every 2 sec, /4.5 = half a heart at max multiplier every 2s
	private static final double HEALTH_PER_TICK = (1d/2d)/ Time.SECOND.x(2);
	private static final int IMMOBILE_SECONDS = Time.SECOND.x(3);

	@Override
	public @NotNull OfflinePlayer getOfflinePlayer() {
		return player;
	}

	@Override
	public @NotNull UUID getUniqueId() {
		return player.getUniqueId();
	}

	/**
	 * Returns the Minigamer's Minecraft username.
	 * @deprecated You should probably be using {@link #getNickname()} instead.
	 */
	@Deprecated
	@NotNull
	public String getName() {
		return Name.of(player);
	}

	public @NotNull String getNickname() {
		return Nickname.of(player);
	}

	public @NotNull Color getColor() {
		if (team == null)
			return Color.WHITE;
		return team.getColor();
	}

	public void join(@NotNull String name) {
		join(ArenaManager.find(name));
	}

	public void join(@NotNull Arena arena) {
		if (!WorldGroup.MINIGAMES.equals(WorldGroup.of(player.getWorld()))) {
			toGamelobby();
			Tasks.wait(10, () -> join(arena));
			return;
		}

		if (match == null) {
			match = MatchManager.get(arena);
			if (!match.join(this))
				match = null;
		} else {
			tell("You are already in a match");
		}
	}

	public void quit() {
		if (match != null) {
			match.quit(this);
			match = null;
		} else {
			tell("You are not in a match");
		}
	}

	public boolean isPlaying() {
		return match != null;
	}

	public boolean isIn(@NotNull Match match) {
		return isIn(match.getMechanic()) && match.equals(this.match);
	}

	public boolean isInLobby(@NotNull Match match) {
		return isInLobby(match.getMechanic()) && match.equals(this.match);
	}

	public boolean isPlaying(@NotNull Match match) {
		return isPlaying(match.getMechanic()) && match.equals(this.match);
	}

	public boolean isIn(@NotNull Mechanic mechanic) {
		return isIn(mechanic.getClass());
	}

	public boolean isPlaying(@NotNull Mechanic mechanic) {
		return isPlaying(mechanic.getClass());
	}

	public boolean isInLobby(@NotNull Mechanic mechanic) {
		return isInLobby(mechanic.getClass());
	}

	public boolean isIn(@NotNull Class<? extends Mechanic> mechanic) {
		if (match != null)
			return mechanic.isInstance(match.getMechanic());
		return false;
	}

	public boolean isPlaying(@NotNull Class<? extends Mechanic> mechanic) {
		if (match != null)
			return mechanic.isInstance(match.getMechanic()) && match.isStarted() && isAlive();
		return false;
	}

	public boolean isInLobby(@NotNull Class<? extends Mechanic> mechanic) {
		if (match != null)
			return mechanic.isInstance(match.getMechanic()) && !match.isStarted();
		return false;
	}

	public boolean isInMatchRegion() {
		return isInMatchRegion(null);
	}

	public boolean isInMatchRegion(@Nullable String type) {
		return new WorldGuardUtils(getPlayer()).getRegionsAt(getPlayer().getLocation()).stream()
				.anyMatch(region -> {
					if (!Strings.isNullOrEmpty(type))
						return match.getArena().ownsRegion(region.getId(), type);
					else
						return region.getId().matches("^" + match.getArena().getRegionBaseName() + ".*");
				});
	}

	public boolean isInRegion(@NotNull String type) {
		return new WorldGuardUtils(getPlayer()).getRegionsAt(getPlayer().getLocation()).stream()
				.anyMatch(region -> match.getArena().ownsRegion(region.getId(), type));
	}

	/**
	 * Sends a message to this minigamer in their chat with no prefix ("[Minigames]")
	 * <p>
	 * This method will automatically {@link me.pugabyte.nexus.utils.StringUtils#colorize(String)} the input.
	 * @param noPrefix a message
	 */
	public void sendMessage(@NotNull String noPrefix) {
		tell(noPrefix, false);
	}

	/**
	 * Sends a message to this minigamer in their chat with a prefix ("[Minigames]")
	 * <p>
	 * This method will automatically {@link me.pugabyte.nexus.utils.StringUtils#colorize(String)} the input.
	 * @param withPrefix a message
	 */
	public void tell(@NotNull String withPrefix) {
		tell(withPrefix, true);
	}

	/**
	 * Sends a message to this minigamer in their chat. If <code>prefix</code> is true, the message will be
	 * prefixed with "[Minigames]".
	 * <p>
	 * This method will automatically {@link me.pugabyte.nexus.utils.StringUtils#colorize(String)} the input.
	 * @param message a message
	 * @param prefix whether or not to display the minigames prefix
	 */
	public void tell(@NotNull String message, boolean prefix) {
		player.sendMessage((prefix ? Minigames.PREFIX : "") + colorize(message));
	}

	public void toGamelobby() {
		boolean staff = Rank.of(player).isStaff();

		player.setGameMode(GameMode.SURVIVAL);
		player.setFallDistance(0);
		player.setAllowFlight(staff);
		player.setFlying(staff);

		teleport(Minigames.getLobby());
	}

	public void toSpectate() {
		teleport(match.getArena().getSpectateLocation());
		match.getMinigamers().forEach(minigamer -> {
			if (minigamer.isAlive)
				minigamer.getPlayer().hidePlayer(Nexus.getInstance(), player);
			else
				player.showPlayer(Nexus.getInstance(), minigamer.getPlayer());
		});
	}

	public CompletableFuture<Void> teleport(@NotNull Location location) {
		return teleport(location, false);
	}

	public CompletableFuture<Void> teleport(@NotNull Location location, boolean withSlowness) {
		Utils.notNull(location, "Tried to teleport " + getName() + " to a null location");

		final Location up = location.clone().add(0, .5, 0);
		final Vector still = new Vector(0, 0, 0);

		return location.getWorld().getChunkAtAsyncUrgently(up).thenRun(() -> {
			player.setVelocity(still);
			canTeleport = true;

			player.teleport(up, match == null ? TeleportCause.COMMAND : TeleportCause.PLUGIN);

			canTeleport = false;
			player.setVelocity(still);
			if (withSlowness) {
				match.getTasks().wait(1, () -> player.setVelocity(still));
				match.getTasks().wait(2, () -> player.setVelocity(still));
			}
		});
	}

	public void setTeam(@Nullable Team team) {
		assert match != null;
		match.handleGlow(this.team);
		this.team = team;
		match.handleGlow(team);

		// join new team channel
		if (match.getMechanic() instanceof TeamMechanic && team != null) {
			((TeamMechanic) match.getMechanic()).joinTeamChannel(this);
			if (team.getObjective() != null && !team.getObjective().isEmpty()) {
				sendMessage("&6Team Objective: &e" + team.getObjective());
				TitleUtils.sendTitle(player, "&6Team Objective", "&e" + team.getObjective(), 10, Time.SECOND.x(4), 20);
			}
		}

		if (match.getScoreboardTeams() != null)
			match.getTasks().wait(1, () -> match.getScoreboardTeams().update());
	}

	public void scored() {
		scored(1);
	}

	public void scored(int scored) {
		setScore(score + scored);
	}

	public void contributionScored(int amount) {
		setContributionScore(contributionScore + amount);
	}

	public void setScore(int score) {
		int diff = score - this.score;
		contributionScored(diff);

		MinigamerScoredEvent event = new MinigamerScoredEvent(this, diff);
		if (!event.callEvent()) return;

		this.score += event.getAmount();
		if (match.getScoreboard() != null)
			match.getScoreboard().update();
	}

	public void died() {
		--lives;
		lastStruckTicks = 0;
	}

	public void damaged() {
		lastStruckTicks = 0;
	}

	public void spawn() {
		if (!match.isEnded())
			team.spawn(this);
	}

	public void respawn() {
		if (match.getArena().getRespawnLocation() == null)
			spawn();
		else {
			respawning = true;
			teleport(match.getArena().getRespawnLocation(), true);
			clearState();
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, false));
			hideAll();
			Runnable respawn = () -> {
				if (!match.isEnded()) {
					unhideAll();
					spawn();
					respawning = false;
					// hides players who are still respawning (as unhideAll unhides them)
					match.getMinigamers().forEach(minigamer -> {
						if (!minigamer.equals(this) && (minigamer.isRespawning() || !minigamer.isAlive())) {
							hidePlayer(minigamer).from(this);
							if (minigamer.isAlive())
								hidePlayer(this).from(minigamer);
						}
					});
				}
			};
			int respawnIn = match.getArena().getRespawnSeconds() * 20;
			if (respawnIn == 0)
				respawn.run();
			else
				match.getTasks().wait(respawnIn, respawn);
		}
	}

	/**
	 * Calculates the current player's location without yaw or pitch
	 * @return player's {@link org.bukkit.Location} without yaw or pitch
	 */
	private @NotNull Location getRotationlessLocation() {
		Location location = player.getLocation();
		location.setYaw(0);
		location.setPitch(0);
		return location;
	}

	public void tick() {
		Location playerLocation = getRotationlessLocation();
		if (lastLocation == null || !blockLocationsEqual(playerLocation, lastLocation))
			immobileTicks = 0;
		else
			immobileTicks++;
		lastLocation = playerLocation;

		if (getMatch().getMechanic().usesAlternativeRegen())
			regen();
	}

	public void regen() {
		double multiplier = 1;
		double sneakMultiplier = 1; // calculated independently as it is variable

		if (player.isSneaking())
			sneakMultiplier = 1.5d;

		if (immobileTicks >= IMMOBILE_SECONDS) {
			multiplier *= immobileTicks/(double)IMMOBILE_SECONDS;
		}

		multiplier *= sneakMultiplier;

		if (lastStruckTicks <= Time.SECOND.x(2))
			multiplier *= 1d/3d;

		// this skips making the hearts do the little regeneration bobbing but idk how to fix that
		heal(HEALTH_PER_TICK * multiplier);

		lastStruckTicks++;
	}

	public void heal(double amount) {
		player.setHealth(Math.min(player.getMaxHealth(), player.getHealth()+amount));
	}

	// respawning
	//     you see alive players = false;
	//     you see dead players = false;
	//     alive players see you = false;
	// spectating
	//     you see alive players = true;
	//     you see dead players = true;
	//     alive players see you = false;

	private void hideAll() {
		if (respawning)
			PlayerUtils.getOnlinePlayers().forEach(_player -> {
				hidePlayer(_player).from(this);
				hidePlayer(this).from(_player);
			});
		else if (!isAlive)
			PlayerUtils.getOnlinePlayers().forEach(_player -> {
				showPlayer(_player).to(this);

				Minigamer minigamer = PlayerManager.get(_player);
				if (minigamer.isPlaying(match) && minigamer.isAlive())
					hidePlayer(_player).from(this);
			});
		 else
			unhideAll();
	}

	public void unhideAll() {
		PlayerUtils.getOnlinePlayers().forEach(_player -> {
			showPlayer(player).to(_player);
			showPlayer(_player).to(player);
		});
	}

	public void clearState() {
		clearState(false);
	}

	public void clearState(boolean forceClearInventory) {
		// TODO: Possibly edit ConditionalPerms to disallow voxel?
		player.setGameMode(match.getMechanic().getGameMode());
		clearGameModeState(forceClearInventory);

		unhideAll();
	}

	private void clearGameModeState(boolean forceClearInventory) {
		Mechanic mechanic = match.getMechanic();

		player.setFireTicks(0);
		player.resetMaxHealth();
		player.setHealth(20);
		player.setExp(0);
		player.setTotalExperience(0);
		player.setLevel(0);
		player.getInventory().setHeldItemSlot(0);
		player.setFoodLevel(20);
		player.setFallDistance(0);
		player.setAllowFlight(mechanic.allowFly());
		player.setFlying(mechanic.allowFly());
		if (VanishAPI.isInvisible(player))
			VanishAPI.showPlayer(player);
		SpeedCommand.resetSpeed(player);
		player.setOp(false);

		if (mechanic.shouldClearInventory() || forceClearInventory)
			player.getInventory().clear();

		for (PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());
	}

	public boolean usesPerk(@NotNull Class<? extends Perk> perk) {
		return match.getMechanic().usesPerk(perk, this);
	}

	public boolean usesPerk(@NotNull Perk perk) {
		return match.getMechanic().usesPerk(perk, this);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Minigamer minigamer = (Minigamer) o;
		return Objects.equals(player.getUniqueId(), minigamer.player.getUniqueId());
	}
}
