package me.pugabyte.nexus.features.events.y2021.bearfair21.islands;

import me.pugabyte.nexus.features.events.models.BearFairIsland;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import org.bukkit.event.Listener;

public interface BearFair21Island extends BearFairIsland, Listener {
	@Override
	default String getEventRegion() {
		return BearFair21.getRegion();
	}

}
