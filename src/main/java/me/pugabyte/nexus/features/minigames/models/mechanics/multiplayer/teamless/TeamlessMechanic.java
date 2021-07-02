package me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.MultiplayerMechanic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class TeamlessMechanic extends MultiplayerMechanic {

	@Override
	public boolean isTeamGame() {
		return false;
	}

	@Override
	public void balance(@NotNull List<Minigamer> minigamers) {
		Arena arena = minigamers.get(0).getMatch().getArena();

		minigamers.forEach(minigamer -> minigamer.setTeam(arena.getTeams().get(0)));
	}

	@Override
	public void announceWinners(@NotNull Match match) {
		announceTeamlessWinners(match);
	}

	@Override
	public boolean shouldBeOver(@NotNull Match match) {
		if (match.getAliveMinigamers().size() <= 1) {
			Nexus.log("Match has only one player, ending");
			return true;
		}

		int winningScore = match.getArena().getCalculatedWinningScore(match);
		if (winningScore > 0)
			for (Minigamer minigamer : match.getAliveMinigamers())
				if (minigamer.getScore() >= winningScore) {
					Nexus.log("Teamless match has reached calculated winning score (" + winningScore + "), ending");
					return true;
				}

		return false;
	}

	@Override
	public void nextTurn(@NotNull Match match) {
		// TODO
	}

}
