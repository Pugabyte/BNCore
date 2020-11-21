package me.pugabyte.nexus.features.minigames.models;

import com.destroystokyo.paper.Title;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.ActionBarUtils.ActionBar;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;
import static me.pugabyte.nexus.utils.Utils.getNearestPlayer;

@Data
@Builder
@AllArgsConstructor
@SerializableAs("Team")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Team implements ConfigurationSerializable {
	@NonNull
	@EqualsAndHashCode.Include
	private String name = "Default";
	@NonNull
	@EqualsAndHashCode.Include
	private ChatColor color = ChatColor.WHITE;
	private String objective;
	private Loadout loadout = new Loadout();
	private List<Location> spawnpoints = new ArrayList<>();
	private int lives = 0;
	private int minPlayers = 1;
	private int maxPlayers = -1;
	private int balancePercentage = -1;

	public Team() {
		this(new HashMap<>());
	}

	public Team(String name) {
		this(new HashMap<String, Object>() {{ put("name", name); }});
	}

	public Team(Map<String, Object> map) {
		this.name = (String) map.getOrDefault("name", name);
		this.color = ChatColor.valueOf(((String) map.getOrDefault("color", color.name())).toUpperCase());
		this.objective = (String) map.get("objective");
		this.loadout = (Loadout) map.getOrDefault("loadout", loadout);
		this.spawnpoints = (List<Location>) map.getOrDefault("spawnpoints", spawnpoints);
		this.lives = (Integer) map.getOrDefault("lives", lives);
		this.minPlayers = (Integer) map.getOrDefault("minPlayers", minPlayers);
		this.maxPlayers = (Integer) map.getOrDefault("maxPlayers", maxPlayers);
		this.balancePercentage = (Integer) map.getOrDefault("balancePercentage", balancePercentage);
	}

	@Override
	public Map<String, Object> serialize() {
		return new LinkedHashMap<String, Object>() {{
			put("name", stripColor(getName()));
			put("color", getColor().name());
			put("objective", getObjective());
			put("loadout", getLoadout());
			put("spawnpoints", getSpawnpoints());
			put("lives", getLives());
			put("minPlayers", getMinPlayers());
			put("maxPlayers", getMaxPlayers());
			put("balancePercentage", getBalancePercentage());
		}};
	}

	public String getColoredName() {
		return color + name;
	}

	public void spawn(Match match) {
		spawn(getMinigamers(match));
	}

	public void spawn(Minigamer minigamer) {
		spawn(Collections.singletonList(minigamer));
	}

	public void spawn(List<Minigamer> minigamers) {
		List<Minigamer> members = getAliveMinigamers(minigamers);
		if (members.isEmpty()) return;

		members.forEach(minigamer -> {
			minigamer.getPlayer().setGameMode(minigamer.getMatch().getArena().getMechanic().getGameMode());
			minigamer.getPlayer().getInventory().setHeldItemSlot(0);
		});

		if (loadout != null)
			members.forEach(minigamer -> loadout.apply(minigamer));

		toSpawnpoints(members);
	}

	public void toSpawnpoints(Match match) {
		toSpawnpoints(getAliveMinigamers(match));
	}

	public void toSpawnpoints(List<Minigamer> members) {
		if (spawnpoints.size() == 1) {
			for (Minigamer minigamer : members)
				minigamer.teleport(spawnpoints.get(0));
			return;
		}

		int SAFETY = 0;
		while (members.size() > 0) {
			List<Location> locs = new ArrayList<>(spawnpoints);
			if (members.get(0).getMatch().getArena().getMechanic().shuffleSpawnpoints())
				Collections.shuffle(locs);

			List<Minigamer> toRemove = new ArrayList<>();
			for (Minigamer minigamer : members) {
				if (SAFETY < 50) {
					Player nearestPlayer = getNearestPlayer(minigamer.getPlayer());
					double distance = nearestPlayer.getLocation().distance(minigamer.getPlayer().getLocation());
					if (distance < 1)
						continue;
				}
				minigamer.teleport(locs.get(0));
				locs.remove(0);
				if (locs.size() == 0)
					locs.addAll(new ArrayList<>(spawnpoints));

				toRemove.add(minigamer);
			}
			members.removeAll(toRemove);
			++SAFETY;
		}
	}

	public List<Minigamer> getAliveMinigamers(Match match) {
		return getAliveMinigamers(getMinigamers(match));
	}

	public List<Minigamer> getAliveMinigamers(List<Minigamer> minigamers) {
		return ensureThisTeam(minigamers.stream()
				.filter(Minigamer::isAlive)
				.collect(Collectors.toList()));
	}

	public List<Minigamer> getMinigamers(Match match) {
		return ensureThisTeam(match.getMinigamers());
	}

	public List<Minigamer> ensureThisTeam(List<Minigamer> minigamers) {
		return minigamers.stream()
				.filter(minigamer -> this.equals(minigamer.getTeam()))
				.collect(Collectors.toList());
	}

	public int getScore(Match match) {
		return match.getScores().getOrDefault(this, 0);
	}

	public void broadcast(Match match, String text) {
		getAliveMinigamers(match).forEach(minigamer -> minigamer.tell(text));
	}

	public void broadcastNoPrefix(Match match, String text) {
		getAliveMinigamers(match).forEach(minigamer -> minigamer.send(text));
	}

	public void title(Match match, Title title) {
		getAliveMinigamers(match).forEach(minigamer -> minigamer.getPlayer().sendTitle(title));
	}

	public void actionBar(Match match, ActionBar actionBar) {
		getAliveMinigamers(match).forEach(minigamer -> ActionBarUtils.sendActionBar(minigamer.getPlayer(), actionBar));
	}

}
