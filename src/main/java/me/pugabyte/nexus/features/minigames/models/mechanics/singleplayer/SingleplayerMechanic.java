package me.pugabyte.nexus.features.minigames.models.mechanics.singleplayer;

import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SingleplayerMechanic extends Mechanic {

	@Override
	public void balance(@NotNull List<Minigamer> minigamers) {
		Arena arena = minigamers.get(0).getMatch().getArena();
		minigamers.forEach(minigamer -> minigamer.setTeam(arena.getTeams().get(0)));
	}

	@Override
	public void processJoin(@NotNull Minigamer minigamer) {
		balance(minigamer);
		minigamer.getMatch().teleportIn(minigamer);
		if (!minigamer.getMatch().isStarted())
			minigamer.getMatch().start();
	}

	@Override
	public boolean shouldBeOver(@NotNull Match match) {
		// TODO: Any logic here, or just let MatchManager clean it up?
		return false;
	}

	@Override
	public void announceWinners(@NotNull Match match) {
		// TODO
	}

}
