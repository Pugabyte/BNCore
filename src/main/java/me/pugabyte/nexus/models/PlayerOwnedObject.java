package me.pugabyte.nexus.models;

import me.lexikiq.HasUniqueId;
import me.lexikiq.OptionalPlayerLike;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.afk.AFK;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotOnlineException;
import me.pugabyte.nexus.models.mail.Mailer.Mail;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.nickname.NicknameService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldGroup;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static me.pugabyte.nexus.utils.AdventureUtils.identityOf;

/**
 * A mongo database object owned by a player
 */
public interface PlayerOwnedObject extends eden.interfaces.PlayerOwnedObject, OptionalPlayerLike {

	/**
	 * Gets the unique ID of this object. Alias for {@link #getUuid()}, for compatibility with {@link HasUniqueId}.
	 * @return this object's unique ID
	 */
	@Override
	default @NotNull UUID getUniqueId() {return getUuid();}

	default @NotNull OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(getUuid());
	}

	/**
	 * Gets the online player for this object and returns null if they're not online
	 * @return online player or null
	 */
	default @Nullable Player getPlayer() {
		return getOfflinePlayer().getPlayer();
	}

	/**
	 * Gets the online player for this object and throws if they're not online
	 * @return online player
	 * @throws PlayerNotOnlineException player is not online
	 */
	default @NotNull Player getOnlinePlayer() throws PlayerNotOnlineException {
		Player player = getOfflinePlayer().getPlayer();
		if (player == null)
			throw new PlayerNotOnlineException(getOfflinePlayer());
		return player;
	}

	default boolean isOnline() {
		return getOfflinePlayer().isOnline();
	}

	default boolean isAfk() {
		return AFK.get(getOnlinePlayer()).isAfk();
	}

	default boolean isTimeAfk() {
		return AFK.get(getOnlinePlayer()).isTimeAfk();
	}

	default @NotNull Nerd getNerd() {
		return Nerd.of(this);
	}

	default @NotNull Nerd getOnlineNerd() {
		return Nerd.of(getOnlinePlayer());
	}

	default @NotNull WorldGroup getWorldGroup() {
		return getOnlineNerd().getWorldGroup();
	}

	@Override
	default @NotNull String getName() {
		String name = Name.of(this);
		if (name == null)
			name = Nerd.of(getUuid()).getName();
		return name;
	}

	@Override
	default @NotNull String getNickname() {
		return Nickname.of(this);
	}

	default Nickname getNicknameData() {
		return new NicknameService().get(this.getUuid());
	}

	default boolean hasNickname() {
		return !isNullOrEmpty(getNicknameData().getNicknameRaw());
	}

	default void debug(String message) {
		if (Nexus.isDebug())
			sendMessage(message);
	}

	default void debug(ComponentLike message) {
		if (Nexus.isDebug())
			sendMessage(message);
	}

	default void sendMessage(String message) {
		sendMessage(json(message));
	}

	default void sendOrMail(String message) {
		if (isOnline())
			sendMessage(json(message));
		else
			Mail.fromServer(getUuid(), WorldGroup.SURVIVAL, message).send();
	}

	default void sendMessage(UUID sender, ComponentLike component, MessageType type) {
		sendMessage(identityOf(sender), component, type);
	}

	default void sendMessage(UUID sender, ComponentLike component) {
		sendMessage(identityOf(sender), component);
	}

	default void sendMessage(int delay, String message) {
		Tasks.wait(delay, () -> sendMessage(message));
	}

	default void sendMessage(int delay, ComponentLike message) {
		Tasks.wait(delay, () -> sendMessage(message));
	}

	default JsonBuilder json() {
		return json("");
	}

	default JsonBuilder json(String message) {
		return new JsonBuilder(message);
	}

	@Override
	default @NonNull Identity identity() {
		return Identity.identity(getUuid());
	}

}
