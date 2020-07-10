package me.pugabyte.bearnation.minigames.features.models.events.matches.minigamers;

import lombok.Getter;
import me.pugabyte.bearnation.minigames.features.models.Minigamer;
import org.bukkit.event.Event;

public class MinigamerDamageEvent extends MinigamerEvent {
	@Getter
	private Minigamer attacker;
	@Getter
	private Event originalEvent;

	public MinigamerDamageEvent(Minigamer victim) {
		super(victim);
	}

	public MinigamerDamageEvent(Minigamer victim, Event originalEvent) {
		super(victim);
		this.originalEvent = originalEvent;
	}

	public MinigamerDamageEvent(Minigamer victim, Minigamer attacker, Event originalEvent) {
		super(victim);
		this.attacker = attacker;
		this.originalEvent = originalEvent;
	}

}
