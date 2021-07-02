package me.pugabyte.nexus.features.minigames.mechanics.common;

import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams.TeamMechanic;
import org.jetbrains.annotations.NotNull;

public abstract class DeathmatchMechanic extends TeamMechanic {
	@Override
	public boolean usesAlternativeRegen() {
		return true;
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		if (event.getAttacker() != null) {
			event.getAttacker().scored();
			event.getAttacker().getMatch().scored(event.getAttacker().getTeam());
		}
		super.onDeath(event);
	}
}
