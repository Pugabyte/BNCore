package me.pugabyte.nexus.features.chat;

import eden.utils.TimeUtils.Time;
import lombok.Getter;
import lombok.Setter;
import me.pugabyte.nexus.features.chat.events.PrivateChatEvent;
import me.pugabyte.nexus.features.chat.events.PublicChatEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotOnlineException;
import me.pugabyte.nexus.models.chat.Channel;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.PrivateChannel;
import me.pugabyte.nexus.models.chat.PublicChannel;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import net.kyori.adventure.audience.MessageType;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.PlayerUtils.canSee;
import static me.pugabyte.nexus.utils.StringUtils.decolorize;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

public class ChatManager {
	@Getter
	private static List<PublicChannel> channels = new ArrayList<>();

	@Getter
	@Setter
	private static PublicChannel mainChannel;

	public static PublicChannel getChannel(String id) {
		if (id == null) return null;
		Optional<PublicChannel> channel = channels.stream().filter(_channel -> _channel.getNickname().equalsIgnoreCase(id)).findFirst();
		if (!channel.isPresent())
			channel = channels.stream().filter(_channel -> _channel.getName().equalsIgnoreCase(id)).findFirst();
		if (!channel.isPresent())
			channel = channels.stream().filter(_channel -> _channel.getName().toLowerCase().startsWith(id.toLowerCase())).findFirst();

		return channel.orElseThrow(() -> new InvalidInputException("Channel '" + id + "' not found"));
	}

	public static Optional<PublicChannel> getChannelByDiscordId(String id) {
		return channels.stream().filter(_channel -> _channel.getDiscordTextChannel() != null && _channel.getDiscordTextChannel().getId().equalsIgnoreCase(id)).findFirst();
	}

	public static void addChannel(PublicChannel channel) {
		channels.add(channel);
	}

	public static void process(Chatter chatter, Channel channel, String message) {
		if (chatter == null || message == null)
			return;

		message = message.trim();

		if (!Rank.of(chatter.getOnlinePlayer()).isAdmin())
			message = stripColor(message);

		if (message.length() == 0)
			return;

		if (channel == null) {
			chatter.sendMessage(Chat.PREFIX + "&cYou are not speaking in a channel. &3Use &c/ch g &3to return to Global chat.");
			return;
		}

		try {
			Set<Chatter> recipients = channel.getRecipients(chatter);
			if (channel instanceof PublicChannel publicChannel) {
				if (!chatter.canJoin(publicChannel))
					throw new InvalidInputException("You do not have permission to speak in that channel");

				if ("G".equals(publicChannel.getNickname())) {
					if ("Tuniab".equals(chatter.getName())) {
						String id = "chat-" + publicChannel.getName().toLowerCase();
						CooldownService service = new CooldownService();
						if (!service.check(chatter.getUuid(), id, Time.SECOND.x(5)))
							throw new InvalidInputException("You are talking too fast! (&e" + service.getDiff(chatter.getUuid(), id) + " left&c)");
					}
				}

				PublicChatEvent event = new PublicChatEvent(chatter, publicChannel, message, message, recipients);
				if (event.callEvent())
					process(event);
			} else if (channel instanceof PrivateChannel) {
				PrivateChatEvent event = new PrivateChatEvent(chatter, (PrivateChannel) channel, message, message, recipients);
				if (event.callEvent())
					process(event);
			}
		} catch (InvalidInputException ex) {
			PlayerUtils.send(chatter.getOnlinePlayer(), Chat.PREFIX + "&c" + ex.getMessage());
		}
	}

	public static void process(PublicChatEvent event) {
		if (!event.wasSeen())
			Tasks.wait(1, () -> event.getChatter().sendMessage(Chat.PREFIX + "No one can hear you! Type &c/ch g &3to talk globally"));

		String chatterFormat = event.getChannel().getChatterFormat(event.getChatter());
		JsonBuilder json = new JsonBuilder(chatterFormat);

		Nerd nerd = Nerd.of(event.getChatter());

		List<String> hoverLines = new ArrayList<>();
		if (nerd.hasNickname())
			hoverLines.add("&3Real name: &e" + nerd.getName());
		if (!nerd.getPronouns().isEmpty())
			hoverLines.add("&3Pronouns: " + nerd.getPronouns().stream().map(pronoun -> "&e" + pronoun + "&3").collect(Collectors.joining(", ")));

		if (!hoverLines.isEmpty()) {
			String hover = String.join("\n", hoverLines);
			json.hover(hover);
		}

		json.group().color(event.getChannel().getMessageColor()).next(event.getMessage());

		JsonBuilder staff = new JsonBuilder(json);

		if (event.isFiltered())
			staff.next(" &c&l*")
					.hover("&cChat message was filtered")
					.hover("&cClick to see original message")
					.command("/echo &3Original message: " + decolorize(chatterFormat + event.getOriginalMessage()));

		event.getRecipients().forEach(recipient -> {
			if (Rank.of(recipient.getOnlinePlayer()).isStaff())
				recipient.sendMessage(event, staff, MessageType.CHAT);
			else
				recipient.sendMessage(event, json, MessageType.CHAT);
		});

		Bukkit.getConsoleSender().sendMessage(stripColor(json.toString()));
	}

	public static void process(PrivateChatEvent event) {
		JsonBuilder to = new JsonBuilder("&3&l[&bPM&3&l] &eTo &3" + event.getRecipientNames() + " &b&l> ")
				.next(event.getChannel().getMessageColor() + event.getMessage());
		JsonBuilder from = new JsonBuilder("&3&l[&bPM&3&l] &eFrom &3" + Nickname.of(event.getChatter()) + " &b&l> ")
				.next(event.getChannel().getMessageColor() + event.getMessage());

		int seen = 0;
		for (Chatter recipient : event.getRecipients()) {
			recipient.setLastPrivateMessage(event.getChannel());

			if (!recipient.equals(event.getChatter())) {
				boolean canSee = canSee(event.getChatter().getOfflinePlayer(), recipient.getOfflinePlayer());
				JsonBuilder notOnline = new JsonBuilder(Chat.PREFIX).next(new PlayerNotOnlineException(recipient.getOfflinePlayer()).getJson());

				if (!recipient.getOfflinePlayer().isOnline())
					event.getChatter().sendMessage(notOnline);
				else {
					recipient.sendMessage(event, from, MessageType.CHAT);
					if (canSee)
						++seen;
					else
						event.getChatter().sendMessage(notOnline);
				}
			}
		}

		if (seen > 0)
			event.getChatter().sendMessage(event, to, MessageType.CHAT);

		Bukkit.getConsoleSender().sendMessage(Nickname.of(event.getChatter()) + " -> " + event.getRecipientNames() + ": " + event.getMessage());
	}

}
