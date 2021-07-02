package me.pugabyte.nexus.features.minigames.models;

import com.google.common.base.Strings;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.interfaces.Named;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.models.annotations.Regenerating;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import me.pugabyte.nexus.features.minigames.models.mechanics.MechanicType;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.pugabyte.nexus.utils.SerializationUtils.YML.deserializeMaterialSet;
import static me.pugabyte.nexus.utils.SerializationUtils.YML.serializeMaterialSet;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SerializableAs("Arena")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Arena implements ConfigurationSerializable, Named, ComponentLike {
	@EqualsAndHashCode.Include
	private int id = ArenaManager.getNextId();
	@NotNull
	@EqualsAndHashCode.Include
	private String name;
	@NotNull
	private String displayName;
	private boolean testMode = false;
	@NotNull
	private MechanicType mechanicType = MechanicType.FREE_FOR_ALL;
	@NotNull
	private List<Team> teams = new ArrayList<>() {{
		add(new Team());
	}};
	@NotNull
	private Lobby lobby = new Lobby();
	@Nullable
	private Location spectateLocation;
	@Nullable
	private Location respawnLocation;
	private int respawnSeconds = 5;
	private int seconds = 300;
	private int beginDelay = 0;
	private int turnTime = 0;
	private int maxTurns = 0;
	private int minPlayers = 2;
	private int maxPlayers = 30;
	private int winningScore;
	private int minWinningScore;
	private int maxWinningScore;
	private int lives = 0;
	@NotNull
	private Set<Material> blockList = new HashSet<>();
	@Accessors(fluent = true)
	private boolean isWhitelist = true;
	@Accessors(fluent = true)
	private boolean canJoinLate = false;

	public @NotNull <T extends Mechanic> T getMechanic() {
		return (T) getMechanicType().get();
	}

	public Arena(@NotNull String name) {
		this(new HashMap<>() {{
			put("name", name);
		}});
	}

	public Arena(@NotNull Map<String, Object> map) {
		this.id = (int) map.getOrDefault("id", id);
		this.name = (String) map.get("name");
		this.displayName = (String) map.getOrDefault("displayName", name);
		this.mechanicType = MechanicType.valueOf(((String) map.getOrDefault("mechanicType", mechanicType.name())).toUpperCase());
		this.teams = (List<Team>) map.getOrDefault("teams", teams);
		this.lobby = (Lobby) map.getOrDefault("lobby", lobby);
		this.spectateLocation = (Location) map.get("spectateLocation");
		this.respawnLocation = (Location) map.get("respawnLocation");
		this.respawnSeconds = (Integer) map.getOrDefault("respawnSeconds", respawnSeconds);
		this.seconds = (Integer) map.getOrDefault("seconds", seconds);
		this.beginDelay = (Integer) map.getOrDefault("beginDelay", beginDelay);
		this.turnTime = (Integer) map.getOrDefault("turnTime", turnTime);
		this.maxTurns = (Integer) map.getOrDefault("maxTurns", maxTurns);
		this.minPlayers = (Integer) map.getOrDefault("minPlayers", minPlayers);
		this.maxPlayers = (Integer) map.getOrDefault("maxPlayers", maxPlayers);
		this.winningScore = (Integer) map.getOrDefault("winningScore", winningScore);
		this.minWinningScore = (Integer) map.getOrDefault("minWinningScore", minWinningScore);
		this.maxWinningScore = (Integer) map.getOrDefault("maxWinningScore", maxWinningScore);
		this.lives = (Integer) map.getOrDefault("lives", lives);
		this.blockList = deserializeMaterialSet((List<String>) map.getOrDefault("blockList", new ArrayList<>()));
		this.isWhitelist = (Boolean) map.getOrDefault("isWhitelist", isWhitelist);
		this.canJoinLate = (Boolean) map.getOrDefault("canJoinLate", canJoinLate);
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		return new LinkedHashMap<>() {{
			put("id", getId());
			put("name", getName());
			put("displayName", getDisplayName());
			put("mechanicType", getMechanicType().name());
			put("teams", getTeams());
			put("lobby", getLobby());
			put("spectateLocation", getSpectateLocation());
			put("respawnLocation", getRespawnLocation());
			put("respawnSeconds", getRespawnSeconds());
			put("seconds", getSeconds());
			put("beginDelay", getBeginDelay());
			put("turnTime", getTurnTime());
			put("maxTurns", getMaxTurns());
			put("minPlayers", getMinPlayers());
			put("maxPlayers", getMaxPlayers());
			put("winningScore", getWinningScore());
			put("minWinningScore", getMinWinningScore());
			put("maxWinningScore", getMaxWinningScore());
			put("lives", getLives());
			put("blockList", serializeMaterialSet(getBlockList()));
			put("isWhitelist", isWhitelist());
			put("canJoinLate", canJoinLate());
		}};
	}

	public @NotNull TextComponent asComponent() {
		return Component.text(getDisplayName(), NamedTextColor.YELLOW)
				.hoverEvent(HoverEvent.showText(Component.text(getMechanic().getName(), NamedTextColor.DARK_AQUA)));
	}

	/**
	 * Gets the world of the arena
	 * @return arena's world
	 * @throws InvalidInputException arena location not found
	 */
	public @NotNull final World getWorld() throws InvalidInputException {
		Location location = getTeleportLocation();
		if (location == null)
			throw new InvalidInputException("No location found for arena, could not initialize match");
		return location.getWorld();
	}

	public @NotNull final WorldGuardUtils getWGUtils() {
		return new WorldGuardUtils(getWorld());
	}

	public @NotNull final WorldEditUtils getWEUtils() {
		return new WorldEditUtils(getWorld());
	}

	public void regenerate() {
		for (Class<? extends Mechanic> mechanic : getMechanic().getSuperclasses()) {
			Regenerating annotation = mechanic.getAnnotation(Regenerating.class);
			if (annotation != null)
				for (String type : annotation.value())
					regenerate(type);
		}
	}

	private void regenerate(@NotNull String type) {
		String regex = getRegionTypeRegex(type);

		getWGUtils().getRegionsLike(regex).forEach(region -> {
			String file = getSchematicName(region.getId().replaceFirst(getRegionBaseName().toLowerCase() + "_", ""));
			getWEUtils().paster().file(file.toLowerCase()).at(region.getMinimumPoint()).pasteAsync();
		});
	}

	public @NotNull String getSchematicName(@NotNull String name) {
		return (getSchematicBaseName() + name).toLowerCase();
	}

	@NotNull
	protected String getMechanicName() {
		return getMechanic().getClass().getSimpleName().toLowerCase();
	}

	public @NotNull String getSchematicBaseName() {
		return ("minigames/" + getMechanicName() + "/" + getName()).toLowerCase() + "_";
	}

	public @NotNull String getRegionBaseName() {
		return (getMechanicName() + "_" + getName()).toLowerCase();
	}

	private static final @NotNull String NUMBER_MODIFIER = "(_[0-9]+)?";

	public @NotNull String getRegionTypeRegex(String type) {
		if (Strings.isNullOrEmpty(type))
			return "^" + getRegionBaseName() + "$";
		return "^" + getRegionBaseName() + "_" + type.toLowerCase() + NUMBER_MODIFIER + "$";
	}

	public boolean ownsRegion(@NotNull ProtectedRegion region, @NotNull String type) {
		return ownsRegion(region.getId(), type);
	}

	public boolean ownsRegion(@NotNull String regionName, @NotNull String type) {
		return regionName.toLowerCase().matches(getRegionTypeRegex(type));
	}

	public Region getRegion() {
		return getWGUtils().getRegion(getRegionBaseName());
	}

	public Region getRegion(@NotNull String type) {
		return getWGUtils().getRegion(getRegionBaseName() + "_" + type);
	}

	public static int getRegionNumber(@NotNull ProtectedRegion region) {
		String[] split = region.getId().split("_");
		return Integer.parseInt(split[split.length - 1]);
	}

	public @NotNull Set<ProtectedRegion> getRegionsLike(@NotNull String regex) {
		return getWGUtils().getRegionsLike(getRegionBaseName() + "_" + regex + NUMBER_MODIFIER);
	}

	public @NotNull Set<ProtectedRegion> getRegionsLikeAt(@NotNull String regex, @NotNull Location location) {
		return getWGUtils().getRegionsLikeAt(getRegionBaseName() + "_" + regex + NUMBER_MODIFIER, location);
	}

	public @NotNull ProtectedRegion getProtectedRegion() {
		return getWGUtils().getProtectedRegion(getRegionBaseName());
	}

	public @NotNull ProtectedRegion getProtectedRegion(@NotNull String type) {
		return getWGUtils().getProtectedRegion(getRegionBaseName() + "_" + type);
	}

	public boolean isInRegion(@NotNull Block block, @NotNull String type) {
		return isInRegion(block.getLocation(), type);
	}

	public boolean isInRegion(@NotNull Location location, @NotNull String type) {
		return !getRegionsLikeAt(type, location).isEmpty();
	}

	public boolean canUseBlock(@NotNull Material type) {
		if (blockList.size() == 0)
			return true;

		if (isWhitelist)
			return blockList.contains(type);
		else
			return !blockList.contains(type);
	}

	public void write() {
		ArenaManager.write(this);
	}

	public void delete() {
		ArenaManager.delete(this);
	}

	public @Nullable Location getTeleportLocation() {
		if (respawnLocation != null)
			return respawnLocation;
		else if (spectateLocation != null)
			return spectateLocation;
		else if (lobby.getLocation() != null && !lobby.getLocation().equals(Minigames.getLobby()))
			return lobby.getLocation();
		else if (teams.size() > 0 && teams.get(0).getSpawnpoints() != null && teams.get(0).getSpawnpoints().size() > 0)
			return teams.get(0).getSpawnpoints().get(0);
		return null;
	}

	public void teleport(@NotNull Minigamer minigamer) {
		Location location = getTeleportLocation();
		if (location == null)
			minigamer.tell("No teleport location found");
		else
			minigamer.teleport(location);
	}

	public int getCalculatedWinningScore(@NotNull Match match) {
		if (minWinningScore == 0 || maxWinningScore == 0)
			return winningScore;

		float players = Math.min(Math.max(match.getAlivePlayers().size(), minPlayers), maxPlayers);

		return Math.min(winningScore, Math.round((players - minPlayers) / (maxPlayers - minPlayers) * (maxWinningScore - minWinningScore) + minWinningScore));
	}

}
