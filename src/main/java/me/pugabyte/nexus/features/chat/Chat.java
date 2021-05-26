package me.pugabyte.nexus.features.chat;

import lombok.Getter;
import me.lexikiq.HasUniqueId;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.alerts.AlertsListener;
import me.pugabyte.nexus.features.chat.bridge.IngameBridgeListener;
import me.pugabyte.nexus.features.chat.translator.Translator;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.features.discord.DiscordId.TextChannel;
import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.models.chat.Channel;
import me.pugabyte.nexus.models.chat.ChatService;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.PublicChannel;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.utils.AdventureUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.TimeUtils.Timer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.ComponentLike;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.UUID;

public class Chat extends Feature {

	// TODO:
	//   Discord queue
	//   /bridge command

	public static final String PREFIX = StringUtils.getPrefix("Chat");

	@Override
	public void onStart() {
		new Timer("    addChannels", this::addChannels);
		new Timer("    ChatListener", () -> Nexus.registerListener(new ChatListener()));
		new Timer("    IngameBridgeListener", () -> Nexus.registerListener(new IngameBridgeListener()));
		new Timer("    AlertsListener", () -> Nexus.registerListener(new AlertsListener()));
		new Timer("    Translator", () -> Nexus.registerListener(new Translator()));
		new Timer("    updateChannels", this::updateChannels);
	}

	@Override
	public void onStop() {
		new HashMap<>(new ChatService().getCache()).forEach((uuid, chatter) -> new ChatService().saveSync(chatter));
	}

	private void updateChannels() {
		Bukkit.getOnlinePlayers().stream()
				.map(player -> (Chatter) new ChatService().get(player))
				.forEach(Chatter::updateChannels);
	}

	private void addChannels() {
		for (StaticChannel channel : StaticChannel.values())
			ChatManager.addChannel(channel.getChannel());

		ChatManager.setMainChannel(StaticChannel.GLOBAL.getChannel());
	}

	public enum StaticChannel {
		GLOBAL(PublicChannel.builder()
				.name("Global")
				.nickname("G")
				.discordTextChannel(TextChannel.BRIDGE)
				.discordColor(ChatColor.DARK_PURPLE)
				.color(ChatColor.DARK_GREEN)
				.local(false)
				.crossWorld(true)
				.build()),
		LOCAL(PublicChannel.builder()
				.name("Local")
				.nickname("L")
				.color(ChatColor.YELLOW)
				.local(true)
				.crossWorld(false)
				.build()),
		STAFF(PublicChannel.builder()
				.name("Staff")
				.nickname("S")
				.rank(Rank.BUILDER)
				.discordTextChannel(TextChannel.STAFF_BRIDGE)
				.color(ChatColor.BLACK)
				.censor(false)
				.local(false)
				.crossWorld(true)
				.build()),
		OPERATOR(PublicChannel.builder()
				.name("Operator")
				.nickname("O")
				.rank(Rank.OPERATOR)
				.discordTextChannel(TextChannel.STAFF_OPERATORS)
				.color(ChatColor.DARK_AQUA)
				.censor(false)
				.local(false)
				.crossWorld(true)
				.build()),
		ADMIN(PublicChannel.builder()
				.name("Admin")
				.nickname("A")
				.rank(Rank.ADMIN)
				.discordTextChannel(TextChannel.STAFF_ADMINS)
				.color(ChatColor.BLUE)
				.censor(false)
				.local(false)
				.crossWorld(true)
				.build()),
		MINIGAMES(PublicChannel.builder()
				.name("Minigames")
				.nickname("M")
				.color(ChatColor.DARK_AQUA)
				.local(false)
				.crossWorld(true)
				.build()),
		CREATIVE(PublicChannel.builder()
				.name("Creative")
				.nickname("C")
				.color(ChatColor.AQUA)
				.local(false)
				.crossWorld(false)
				.build()),
		SKYBLOCK(PublicChannel.builder()
				.name("Skyblock")
				.nickname("B")
				.color(ChatColor.GOLD)
				.local(false)
				.crossWorld(false)
				.build());

		@Getter
		private final PublicChannel channel;

		StaticChannel(PublicChannel channel) {
			this.channel = channel;
		}
	}

	public static int getLocalRadius() {
		return Nexus.getInstance().getConfig().getInt("localRadius");
	}

	public static void setActiveChannel(HasUniqueId player, Channel channel) {
		new ChatService().get(player).setActiveChannel(channel);
	}

	public static void setActiveChannel(HasUniqueId player, StaticChannel channel) {
		setActiveChannel(player, channel.getChannel());
	}

	// Broadcasts

	public static void broadcast(String message) {
		broadcast(message, ChatManager.getMainChannel());
	}

	public static void broadcast(String message, MuteMenuItem muteMenuItem) {
		broadcast(message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcast(String message, StaticChannel channel) {
		broadcast(message, ChatManager.getChannel(channel.name()));
	}

	public static void broadcast(String message, StaticChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(message, ChatManager.getChannel(channel.name()), muteMenuItem);
	}

	public static void broadcast(String message, String channel) {
		broadcast(message, ChatManager.getChannel(channel));
	}

	public static void broadcast(String message, PublicChannel channel) {
		broadcast(message, channel, null);
	}

	public static void broadcast(String message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		channel.broadcast(message, muteMenuItem);
	}

	public static void broadcast(ComponentLike message) {
		broadcast(message, ChatManager.getMainChannel());
	}

	public static void broadcast(ComponentLike message, MessageType type) {
		broadcast(message, ChatManager.getMainChannel(), type);
	}

	public static void broadcast(ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcast(message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcast(Identity sender, ComponentLike message) {
		broadcast(sender, message, ChatManager.getMainChannel());
	}

	public static void broadcast(Identified sender, ComponentLike message) {
		broadcast(sender, message, ChatManager.getMainChannel());
	}

	public static void broadcast(UUID sender, ComponentLike message) {
		broadcast(sender, message, ChatManager.getMainChannel());
	}

	public static void broadcast(Identity sender, ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcast(Identity sender, ComponentLike message, MessageType type) {
		broadcast(sender, message, ChatManager.getMainChannel(), type);
	}

	public static void broadcast(Identified sender, ComponentLike message, MessageType type) {
		broadcast(sender, message, ChatManager.getMainChannel(), type);
	}

	public static void broadcast(UUID sender, ComponentLike message, MessageType type) {
		broadcast(sender, message, ChatManager.getMainChannel(), type);
	}

	public static void broadcast(Identity sender, ComponentLike message, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getMainChannel(), type, muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getMainChannel(), type, muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getMainChannel(), type, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, StaticChannel channel) {
		broadcast(message, ChatManager.getChannel(channel.name()));
	}

	public static void broadcast(ComponentLike message, StaticChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(message, ChatManager.getChannel(channel.name()), muteMenuItem);
	}

	public static void broadcast(ComponentLike message, StaticChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(Identity.nil(), message, channel, type, muteMenuItem);
	}

	public static void broadcast(Identity sender, ComponentLike message, StaticChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, StaticChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, StaticChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, StaticChannel channel, MessageType type) {
		broadcast(Identity.nil(), message, channel, type);
	}

	public static void broadcast(Identity sender, ComponentLike message, StaticChannel channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM, null);
	}

	public static void broadcast(Identified sender, ComponentLike message, StaticChannel channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM, null);
	}

	public static void broadcast(UUID sender, ComponentLike message, StaticChannel channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM, null);
	}

	public static void broadcast(Identity sender, ComponentLike message, StaticChannel channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(Identified sender, ComponentLike message, StaticChannel channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(UUID sender, ComponentLike message, StaticChannel channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(Identity sender, ComponentLike message, StaticChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getChannel(channel.name()), type, muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, StaticChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender.identity(), message, channel, type, muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, StaticChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(AdventureUtils.identityOf(sender), message, channel, type, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, String channel) {
		broadcast(message, channel, MessageType.SYSTEM);
	}

	public static void broadcast(Identity sender, ComponentLike message, String channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcast(Identified sender, ComponentLike message, String channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcast(UUID sender, ComponentLike message, String channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcast(ComponentLike message, String channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(Identity.nil(), message, channel, type, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcast(Identity.nil(), message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(Identity sender, ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, String channel, MessageType type) {
		broadcast(Identity.nil(), message, channel, type);
	}

	public static void broadcast(Identity sender, ComponentLike message, String channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(Identified sender, ComponentLike message, String channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(UUID sender, ComponentLike message, String channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(Identity sender, ComponentLike message, String channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, ChatManager.getChannel(channel), type, muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, String channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender.identity(), message, channel, type, muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, String channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(AdventureUtils.identityOf(sender), message, channel, type, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, PublicChannel channel) {
		broadcast(message, channel, MessageType.SYSTEM, null);
	}

	public static void broadcast(Identity sender, ComponentLike message, PublicChannel channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcast(Identified sender, ComponentLike message, PublicChannel channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcast(UUID sender, ComponentLike message, PublicChannel channel) {
		broadcast(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcast(ComponentLike message, PublicChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(Identity.nil(), message, channel, type, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(Identity.nil(), message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(Identity sender, ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcast(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcast(ComponentLike message, PublicChannel channel, MessageType type) {
		broadcast(Identity.nil(), message, channel, type);
	}

	public static void broadcast(Identity sender, ComponentLike message, PublicChannel channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(Identified sender, ComponentLike message, PublicChannel channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(UUID sender, ComponentLike message, PublicChannel channel, MessageType type) {
		broadcast(sender, message, channel, type, null);
	}

	public static void broadcast(Identity sender, ComponentLike message, PublicChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		channel.broadcast(sender, message, type, muteMenuItem);
	}

	public static void broadcast(Identified sender, ComponentLike message, PublicChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(sender.identity(), message, channel, type, muteMenuItem);
	}

	public static void broadcast(UUID sender, ComponentLike message, PublicChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcast(AdventureUtils.identityOf(sender), message, channel, type, muteMenuItem);
	}

	public static void broadcastIngame(String message) {
		broadcastIngame(message, ChatManager.getMainChannel());
	}

	public static void broadcastIngame(String message, MuteMenuItem muteMenuItem) {
		broadcastIngame(message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcastIngame(String message, StaticChannel channel) {
		broadcastIngame(message, ChatManager.getChannel(channel.name()));
	}

	public static void broadcastIngame(String message, StaticChannel channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(message, ChatManager.getChannel(channel.name()), muteMenuItem);
	}

	public static void broadcastIngame(String message, String channel) {
		broadcastIngame(message, ChatManager.getChannel(channel));
	}

	public static void broadcastIngame(String message, PublicChannel channel) {
		broadcastIngame(message, channel, null);
	}

	public static void broadcastIngame(String message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		channel.broadcastIngame(message, muteMenuItem);
	}

	public static void broadcastIngame(ComponentLike message) {
		broadcastIngame(message, ChatManager.getMainChannel());
	}

	public static void broadcastIngame(Identity sender, ComponentLike message) {
		broadcastIngame(sender, message, ChatManager.getMainChannel());
	}

	public static void broadcastIngame(Identified sender, ComponentLike message) {
		broadcastIngame(sender, message, ChatManager.getMainChannel());
	}

	public static void broadcastIngame(UUID sender, ComponentLike message) {
		broadcastIngame(sender, message, ChatManager.getMainChannel());
	}

	public static void broadcastIngame(ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcastIngame(message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, ChatManager.getMainChannel(), muteMenuItem);
	}

	public static void broadcastIngame(ComponentLike message, MessageType type) {
		broadcastIngame(message, ChatManager.getMainChannel(), type);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, MessageType type) {
		broadcastIngame(sender, message, ChatManager.getMainChannel(), type);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, MessageType type) {
		broadcastIngame(sender, message, ChatManager.getMainChannel(), type);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, MessageType type, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, ChatManager.getMainChannel(), type, muteMenuItem);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, MessageType type) {
		broadcastIngame(sender, message, ChatManager.getMainChannel(), type);
	}

	public static void broadcastIngame(ComponentLike message, StaticChannel channel) {
		broadcastIngame(message, ChatManager.getChannel(channel.name()));
	}

	public static void broadcastIngame(ComponentLike message, StaticChannel channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(message, ChatManager.getChannel(channel.name()), muteMenuItem);
	}

	public static void broadcastIngame(ComponentLike message, String channel) {
		broadcastIngame(message, ChatManager.getChannel(channel));
	}

	public static void broadcastIngame(ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(Identity.nil(), message, channel, muteMenuItem);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, String channel) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, String channel) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, String channel) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, String channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcastIngame(ComponentLike message, String channel, MessageType type) {
		broadcastIngame(Identity.nil(), message, channel, type);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, String channel, MessageType type) {
		broadcastIngame(sender, message, channel, type, null);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, String channel, MessageType type) {
		broadcastIngame(sender, message, channel, type, null);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, String channel, MessageType type) {
		broadcastIngame(sender, message, channel, type, null);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, String channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, ChatManager.getChannel(channel), type, muteMenuItem);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, String channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender.identity(), message, channel, type, muteMenuItem);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, String channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcastIngame(AdventureUtils.identityOf(sender), message, channel, type, muteMenuItem);
	}

	public static void broadcastIngame(ComponentLike message, PublicChannel channel) {
		broadcastIngame(message, channel, MessageType.SYSTEM);
	}

	public static void broadcastIngame(ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(Identity.nil(), message, channel, muteMenuItem);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, PublicChannel channel) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, PublicChannel channel) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, PublicChannel channel) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, PublicChannel channel, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender, message, channel, MessageType.SYSTEM, muteMenuItem);
	}

	public static void broadcastIngame(ComponentLike message, PublicChannel channel, MessageType type) {
		broadcastIngame(Identity.nil(), message, channel, type);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, PublicChannel channel, MessageType type) {
		broadcastIngame(sender, message, channel, type, null);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, PublicChannel channel, MessageType type) {
		broadcastIngame(sender, message, channel, type, null);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, PublicChannel channel, MessageType type) {
		broadcastIngame(sender, message, channel, type, null);
	}

	public static void broadcastIngame(Identity sender, ComponentLike message, PublicChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		channel.broadcastIngame(sender, message, type, muteMenuItem);
	}

	public static void broadcastIngame(Identified sender, ComponentLike message, PublicChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcastIngame(sender.identity(), message, channel, type, muteMenuItem);
	}

	public static void broadcastIngame(UUID sender, ComponentLike message, PublicChannel channel, MessageType type, MuteMenuItem muteMenuItem) {
		broadcastIngame(AdventureUtils.identityOf(sender), message, channel, type, muteMenuItem);
	}

	public static void broadcastDiscord(String message) {
		broadcastDiscord(message, ChatManager.getMainChannel());
	}

	public static void broadcastDiscord(String message, StaticChannel channel) {
		broadcastDiscord(message, ChatManager.getChannel(channel.name()));
	}

	public static void broadcastDiscord(String message, String channel) {
		broadcastDiscord(message, ChatManager.getChannel(channel));
	}

	public static void broadcastDiscord(String message, PublicChannel channel) {
		channel.broadcastDiscord(message);
	}

	public static void broadcastDiscord(ComponentLike message) {
		broadcastDiscord(message, ChatManager.getMainChannel());
	}

	public static void broadcastDiscord(ComponentLike message, StaticChannel channel) {
		broadcastDiscord(message, ChatManager.getChannel(channel.name()));
	}

	public static void broadcastDiscord(ComponentLike message, String channel) {
		broadcastDiscord(message, ChatManager.getChannel(channel));
	}

	public static void broadcastDiscord(ComponentLike message, PublicChannel channel) {
		channel.broadcastDiscord(message);
	}

}
