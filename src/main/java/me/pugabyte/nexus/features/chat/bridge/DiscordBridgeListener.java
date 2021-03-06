package me.pugabyte.nexus.features.chat.bridge;

import com.vdurmont.emoji.EmojiParser;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.chat.Chat.Broadcast;
import me.pugabyte.nexus.features.chat.ChatManager;
import me.pugabyte.nexus.features.chat.events.DiscordChatEvent;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.discord.DiscordId.User;
import me.pugabyte.nexus.models.chat.PublicChannel;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.discord.DiscordUserService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.Tasks;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

@NoArgsConstructor
public class DiscordBridgeListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		Tasks.sync(() -> {
			Optional<PublicChannel> channel = ChatManager.getChannelByDiscordId(event.getChannel().getId());
			if (!channel.isPresent()) return;

			if (event.getAuthor().isBot())
				if (!event.getAuthor().getId().equals(User.UBER.getId()))
					return;

			String content = event.getMessage().getContentDisplay().trim();

			try {
				content = EmojiParser.parseToAliases(content);
			} catch (Throwable ignore) {}

			DiscordChatEvent discordChatEvent = new DiscordChatEvent(event.getMember(), channel.get(), content, content, channel.get().getPermission());
			if (!discordChatEvent.callEvent()) {
				Tasks.async(() -> event.getMessage().delete().queue());
				return;
			}

			content = discordChatEvent.getMessage();

			DiscordUser user = new DiscordUserService().getFromUserId(event.getAuthor().getId());
			JsonBuilder builder = new JsonBuilder(channel.get().getDiscordColor() + "[D] ");

			if (user != null)
				builder.next(Nerd.of(user.getUuid()).getChatFormat());
			else
				builder.next("&f" + Discord.getName(event.getMember(), event.getAuthor()));

			builder.next(" " + channel.get().getDiscordColor() + "&l>&f");

			if (content.length() > 0)
				builder.next(" " + colorize(content.replaceAll("&", "&&f")));

			for (Message.Attachment attachment : event.getMessage().getAttachments())
				builder.group()
						.next(" &f&l[View Attachment]")
						.url(attachment.getUrl());

			Identity identity = user == null ? Identity.nil() : user.identity();
			Broadcast.ingame().channel(channel.get()).sender(identity).message(builder).messageType(MessageType.CHAT).send();
		});
	}

}