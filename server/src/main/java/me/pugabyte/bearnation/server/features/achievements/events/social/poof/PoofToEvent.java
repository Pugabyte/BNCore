package me.pugabyte.bearnation.server.features.achievements.events.social.poof;

import org.bukkit.entity.Player;

public class PoofToEvent extends PoofEvent {

	public PoofToEvent(Player initiator, Player acceptor) {
		super(initiator, acceptor);
	}

}
