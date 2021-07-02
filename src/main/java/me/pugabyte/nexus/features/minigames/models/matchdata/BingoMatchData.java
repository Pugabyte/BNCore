package me.pugabyte.nexus.features.minigames.models.matchdata;

import lombok.Data;
import me.pugabyte.nexus.features.minigames.mechanics.Bingo;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.MatchData;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.MatchDataFor;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.common.Challenge;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.common.IChallengeProgress;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.pugabyte.nexus.utils.LocationUtils.getCenteredLocation;

@Data
@MatchDataFor(Bingo.class)
public class BingoMatchData extends MatchData {
	private static final int size = 5;
	private Map<UUID, Location> spawnpoints = new HashMap<>();
	private Challenge[][] challenges = new Challenge[size][size];
	private Map<UUID, Map<Class<? extends IChallengeProgress>, IChallengeProgress>> progress = new HashMap<>();

	public BingoMatchData(Match match) {
		super(match);

		determineChallenges();
	}

	private void determineChallenges() {
		Iterator<Challenge> iterator = Challenge.shuffle().iterator();

		for (int i = 0; i < size; i++)
			for (int j = 0; j < size; j++)
				challenges[i][j] = iterator.next();
	}

	public CompletableFuture<Void> spawnpoint(Minigamer minigamer, Location location) {
		location = getCenteredLocation(location.clone().add(0, 2, 0));
		spawnpoints.put(minigamer.getUniqueId(), location);
		return minigamer.teleport(location, true);
	}

	public <T extends IChallengeProgress> T getProgress(Minigamer minigamer, Class<? extends T> clazz) {
		return (T) progress
				.computeIfAbsent(minigamer.getUniqueId(), $ -> new HashMap<>())
				.computeIfAbsent(clazz, $ -> {
					try {
						return clazz.getDeclaredConstructor().newInstance();
					} catch (Exception ex) {
						ex.printStackTrace();
						return null;
					}
				});
	}

}
