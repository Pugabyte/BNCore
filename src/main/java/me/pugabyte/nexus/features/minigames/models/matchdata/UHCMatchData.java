package me.pugabyte.nexus.features.minigames.models.matchdata;

import eden.utils.TimeUtils.Timespan;
import lombok.Data;
import me.pugabyte.nexus.features.minigames.mechanics.UHC;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.MatchData;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.MatchDataFor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@MatchDataFor(UHC.class)
public class UHCMatchData extends MatchData {
	private LocalDateTime startTime = LocalDateTime.now(); // placeholder, is updated on true game start
	private final Map<UUID, Integer> timeAlive = new HashMap<>();

	public UHCMatchData(Match match) {
		super(match);
	}

	public void died(Minigamer minigamer) {
		timeAlive.put(minigamer.getUniqueId(), Timespan.of(startTime, LocalDateTime.now()).getOriginal());
	}

}
