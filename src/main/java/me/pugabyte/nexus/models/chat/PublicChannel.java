package me.pugabyte.nexus.models.chat;

import lombok.Builder;
import lombok.Data;
import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.features.discord.DiscordId.TextChannel;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.utils.PlayerUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class PublicChannel implements Channel {
	private String name;
	private String nickname;
	private ChatColor color;
	private ChatColor messageColor;
	private TextChannel discordTextChannel;
	private ChatColor discordColor;
	@Builder.Default
	private boolean censor = true;
	private boolean isPrivate;
	private boolean local;
	private boolean crossWorld;
	private String permission;
	private Rank rank;
	@Builder.Default
	private boolean persistent = true;

	public ChatColor getDiscordColor() {
		return discordColor == null ? color : discordColor;
	}

	public ChatColor getMessageColor() {
		return messageColor == null ? Channel.super.getMessageColor() : messageColor;
	}

	@Override
	public String getAssignMessage(Chatter chatter) {
		return "Now chatting in " + color + name;
	}

	public String getChatterFormat(Chatter chatter) {
		return color + "[" + nickname.toUpperCase() + "] " + Nerd.of(chatter.getOfflinePlayer()).getChatFormat() + " " + color + ChatColor.BOLD + "> " + getMessageColor();
	}

	public Set<Chatter> getRecipients(Chatter chatter) {
		List<Player> recipients = new ArrayList<>();
		if (local)
			recipients.addAll(PlayerUtils.getOnlinePlayers(chatter.getOnlinePlayer().getWorld()).stream()
					.filter(player -> player.getLocation().distance(chatter.getOnlinePlayer().getLocation()) <= Chat.getLocalRadius())
					.collect(Collectors.toList()));
		else if (crossWorld)
			recipients.addAll(PlayerUtils.getOnlinePlayers());
		else
			recipients.addAll(PlayerUtils.getOnlinePlayers(chatter.getOnlinePlayer().getWorld()));

		return recipients.stream()
				.map(player -> new ChatterService().get(player))
				.filter(_chatter -> _chatter.canJoin(this))
				.filter(_chatter -> _chatter.hasJoined(this))
				.collect(Collectors.toSet());
	}

	public String getPermission() {
		if (permission == null)
			return "chat.use." + name.toLowerCase();
		return permission;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PublicChannel that = (PublicChannel) o;
		return Objects.equals(name, that.name);
	}

}
