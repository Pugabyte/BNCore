package me.pugabyte.nexus.features.minigames.models.events.matches.minigamers;

import lombok.Getter;
import lombok.Setter;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import org.bukkit.event.Event;

public class MinigamerDeathEvent extends MinigamerEvent {
	@Getter
	private Minigamer attacker;
	@Getter
	private Event originalEvent;
	@Getter
	@Setter
	private String deathMessage = "";

	public MinigamerDeathEvent(Minigamer victim) {
		super(victim);
	}

	public MinigamerDeathEvent(Minigamer victim, Minigamer attacker) {
		super(victim);
		this.attacker = attacker;
	}

	public MinigamerDeathEvent(Minigamer victim, Event originalEvent) {
		super(victim);
		this.originalEvent = originalEvent;
	}

	public MinigamerDeathEvent(Minigamer victim, Minigamer attacker, Event originalEvent) {
		super(victim);
		this.attacker = attacker;
		this.originalEvent = originalEvent;
	}

	public void broadcastDeathMessage() {
		if (deathMessage == null) return;
		if (deathMessage.length() == 0)
			if (attacker == null)
				getMatch().broadcast(minigamer.getColoredName() + " &3died");
			else
				getMatch().broadcast(minigamer.getColoredName() + " &3was killed by " + attacker.getColoredName());
		else
			getMatch().broadcast(deathMessage);
	}

}