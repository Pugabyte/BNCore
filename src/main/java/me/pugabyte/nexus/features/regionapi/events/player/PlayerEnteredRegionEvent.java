package me.pugabyte.nexus.features.regionapi.events.player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.AccessLevel;
import lombok.Getter;
import me.lexikiq.HasPlayer;
import me.pugabyte.nexus.features.regionapi.MovementType;
import me.pugabyte.nexus.features.regionapi.events.common.EnteredRegionEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Event that is triggered after a player entered a WorldGuard region
 */
@Getter(AccessLevel.PUBLIC)
public class PlayerEnteredRegionEvent extends EnteredRegionEvent implements HasPlayer {
	protected final Player player;

	/**
	 * creates a new PlayerEnteredRegionEvent
	 *
	 * @param region       the region the player entered
	 * @param player       the player who triggered this event
	 * @param movementType the type of movement how the player entered the region
	 * @param newLocation  the location the player moved to
	 * @param parentEvent  the event that triggered this event
	 */
	public PlayerEnteredRegionEvent(ProtectedRegion region, Player player, MovementType movementType, Location newLocation, Event parentEvent) {
		super(region, player, movementType, newLocation, parentEvent);
		this.player = player;
	}

}
