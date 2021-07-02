package me.pugabyte.nexus.features.chat.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.ChatterService;
import me.pugabyte.nexus.models.chat.PublicChannel;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.discord.DiscordUserService;
import me.pugabyte.nexus.utils.PlayerUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class DiscordChatEvent extends ChatEvent {
	private Member member;
	private PublicChannel channel;
	private final String originalMessage;
	private String message;
	private String permission;
	private boolean filtered;
	private boolean bad;

	public DiscordChatEvent(Member member, PublicChannel channel, String originalMessage, String message, String permission) {
		this.member = member;
		this.channel = channel;
		this.originalMessage = originalMessage;
		this.message = message;
		this.permission = permission;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPermission() {
		return this.permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	@Override
	public Chatter getChatter() {
		if (member != null) {
			DiscordUser user = new DiscordUserService().getFromUserId(member.getUser().getId());
			if (user != null)
				return new ChatterService().get(PlayerUtils.getPlayer(user.getUuid()));
		}
		return null;
	}

	@Override
	public String getOrigin() {
		if (getChatter() != null)
			return getChatter().getName();
		return Discord.getName(member);
	}

	public TextChannel getDiscordTextChannel() {
		return channel.getDiscordTextChannel().get();
	}

	@Override
	public Set<Chatter> getRecipients() {
		return PlayerUtils.getOnlinePlayers().stream()
						.filter(player -> player.hasPermission(permission))
						.map(player -> new ChatterService().get(player))
						.collect(Collectors.toSet());
	}

}
