package me.pugabyte.nexus.models.chat;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Chat.StaticChannel;
import me.pugabyte.nexus.features.chat.ChatManager;
import me.pugabyte.nexus.features.chat.translator.Language;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ChannelConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.PrivateChannelConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.PublicChannelConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundUtils.Jingle;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static me.pugabyte.nexus.features.chat.Chat.PREFIX;
import static me.pugabyte.nexus.utils.StringUtils.trimFirst;

@Data
@Entity(value = "chatter", noClassnameStored = true)
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, ChannelConverter.class, PrivateChannelConverter.class, PublicChannelConverter.class})
public class Chatter implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private Channel activeChannel;
	private Set<PublicChannel> joinedChannels = new HashSet<>();
	private Set<PublicChannel> leftChannels = new HashSet<>();
	private PrivateChannel lastPrivateMessage;
	private Language language;

	public void playSound() {
		if (isOnline())
			Jingle.PING.play(getOfflinePlayer().getPlayer());
	}

	public void say(String message) {
		if (message.startsWith("/"))
			PlayerUtils.runCommand(getOnlinePlayer(), trimFirst(message));
		else
			say(getActiveChannel(), message);
	}

	public void say(Channel channel, String message) {
		ChatManager.process(this, channel, message);
	}

	public void setActiveChannel(Channel channel) {
		if (channel == null)
			Nerd.of(getOfflinePlayer()).sendMessage(PREFIX + "You are no longer speaking in a channel");
		else {
			if (channel instanceof PublicChannel publicChannel)
				joinSilent(publicChannel);

			Nerd.of(getOfflinePlayer()).sendMessage(PREFIX + channel.getAssignMessage(this));
		}
		this.activeChannel = channel;
		save();
	}

	public boolean canJoin(PublicChannel channel) {
		if (channel.getPermission().isEmpty() && channel.getRank() == null)
			return true;

		boolean hasPerm;
		if (isOnline())
			hasPerm = getOnlinePlayer().hasPermission(channel.getPermission());
		else
			hasPerm = Nexus.getPerms().playerHas(null, getOfflinePlayer(), channel.getPermission());

		if (!hasPerm)
			return false;

		if (channel.getRank() != null)
			return Nerd.of(getUuid()).getRank().gte(channel.getRank());

		return true;
	}

	public boolean hasJoined(PublicChannel channel) {
		if (!canJoin(channel))
			return false;
		fixChannelSets();
		if (leftChannels.contains(channel))
			return false;
		return joinedChannels.contains(channel);
	}

	public boolean hasLeft(PublicChannel channel) {
		fixChannelSets();
		return leftChannels.contains(channel);
	}

	private void fixChannelSets() {
		if (joinedChannels == null)
			joinedChannels = new HashSet<>();
		else
			joinedChannels.removeIf(Objects::isNull);

		if (leftChannels == null)
			leftChannels = new HashSet<>();
		else
			joinedChannels.removeIf(Objects::isNull);
	}

	public void join(PublicChannel channel) {
		joinSilent(channel);
		sendMessage(PREFIX + "Joined " + channel.getColor() + channel.getName() + " &3channel");
	}

	public void joinSilent(PublicChannel channel) {
		if (!canJoin(channel))
			throw new InvalidInputException("You do not have permission to join the " + channel.getName() + " channel");
		fixChannelSets();
		leftChannels.remove(channel);
		joinedChannels.add(channel);
		save();
	}

	public void leave(PublicChannel channel) {
		fixChannelSets();
		joinedChannels.remove(channel);
		leftChannels.add(channel);
		sendMessage(PREFIX + "Left " + channel.getColor() + channel.getName() + " &3channel");

		if (channel.equals(activeChannel))
			findNewActiveChannel();
		save();
	}

	private void findNewActiveChannel() {
		if (!joinedChannels.isEmpty()) {
			for (StaticChannel staticChannel : StaticChannel.values()) {
				final PublicChannel channel = staticChannel.getChannel();

				if (!joinedChannels.contains(channel))
					continue;

				if (!canJoin(channel))
					continue;

				setActiveChannel(channel);
				return;
			}
		}

		setActiveChannel(null);
	}

	public void updateChannels() {
		if (isOnline())
			ChatManager.getChannels().forEach(channel -> {
				if (canJoin(channel)) {
					if (!hasJoined(channel) && !hasLeft(channel))
						joinSilent(channel);
				} else if (hasJoined(channel)) {
					leave(channel);
				}
			});
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Chatter chatter = (Chatter) o;
		return Objects.equals(getOfflinePlayer().getUniqueId(), chatter.getOfflinePlayer().getUniqueId());
	}

	private void save() {
		new ChatterService().queueSave(5, this);
	}

}
