package me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.sabotage;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the Minigamer is sent how much time is left in the minigame to their action bar
 */
@Getter
@Setter
public class MinigamerDisplayTimerEvent extends MinigamerEvent implements Cancellable {
	/**
	 * Message that will be displayed on the action bar
	 */
	private @NotNull Component contents;
	private final int seconds;

	public MinigamerDisplayTimerEvent(@NotNull Minigamer minigamer, @NotNull Component contents, int seconds) {
		super(minigamer);
		this.contents = contents;
		this.seconds = seconds;
	}

	/**
	 * Sets the message that will be displayed on the action bar
	 * @param contents message to display
	 */
	public void setContents(@NotNull ComponentLike contents) {
		this.contents = Preconditions.checkNotNull(contents, "contents cannot be null").asComponent();
	}

	// boilerplate

	protected boolean cancelled = false;

	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
