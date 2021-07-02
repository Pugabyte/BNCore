package me.pugabyte.nexus.features.listeners;

import me.lexikiq.HasPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public interface TemporaryListener extends Listener, HasPlayer {

	Player getPlayer();

	default void unregister() {}

}
