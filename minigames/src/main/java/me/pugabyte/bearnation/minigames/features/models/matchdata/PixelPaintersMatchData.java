package me.pugabyte.bearnation.minigames.features.models.matchdata;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Data;
import lombok.experimental.Accessors;
import me.pugabyte.bearnation.minigames.features.mechanics.PixelPainters;
import me.pugabyte.bearnation.minigames.features.models.Match;
import me.pugabyte.bearnation.minigames.features.models.MatchData;
import me.pugabyte.bearnation.minigames.features.models.Minigamer;
import me.pugabyte.bearnation.minigames.features.models.annotations.MatchDataFor;

import java.util.ArrayList;
import java.util.List;

@Data
@MatchDataFor(PixelPainters.class)
public class PixelPaintersMatchData extends MatchData {
	private List<Minigamer> checked = new ArrayList<>();
	private List<Integer> designsPlayed = new ArrayList<>();
	@Accessors(fluent = true)
	private boolean canCheck;
	private boolean roundOver;
	private int currentRound;
	private int designCount;
	private CuboidRegion designRegion;
	private long roundStart;
	private int totalFinished;
	private int roundCountdownId;
	private int timeLeft;
	private boolean animateLobby;
	private int lobbyDesign;
	private int animateLobbyId;

	public PixelPaintersMatchData(Match match) {
		super(match);
	}
}
