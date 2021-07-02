package me.pugabyte.nexus.features.chat;

import eden.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import me.lexikiq.HasUniqueId;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.alerts.AlertsListener;
import me.pugabyte.nexus.features.chat.bridge.IngameBridgeListener;
import me.pugabyte.nexus.features.chat.translator.Translator;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.discord.DiscordId.TextChannel;
import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.models.chat.Channel;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.ChatterService;
import me.pugabyte.nexus.models.chat.PublicChannel;
import me.pugabyte.nexus.models.mutemenu.MuteMenuUser;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.utils.AdventureUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.TimeUtils.Timer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.ComponentLike;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

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
		new HashMap<>(new ChatterService().getCache()).forEach((uuid, chatter) -> new ChatterService().saveSync(chatter));
	}

	private void updateChannels() {
		PlayerUtils.getOnlinePlayers().stream()
				.map(player -> new ChatterService().get(player))
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
		new ChatterService().get(player).setActiveChannel(channel);
	}

	public static void setActiveChannel(HasUniqueId player, StaticChannel channel) {
		setActiveChannel(player, channel.getChannel());
	}

	public static class Broadcast {
		private final PublicChannel channel;
		private final Identity sender;
		private final String prefix;
		private final ComponentLike message;
		private final MuteMenuItem muteMenuItem;
		private final MessageType messageType;
		private final List<Target> targets;

		@Builder(buildMethodName = "send", builderMethodName = "all")
		public Broadcast(PublicChannel channel, Identity sender, String prefix, ComponentLike message, MuteMenuItem muteMenuItem, MessageType messageType, List<Target> targets) {
			Validate.notNull(message);

			this.channel = channel == null ? ChatManager.getMainChannel() : channel;
			this.sender = sender == null ? Identity.nil() : sender;
			this.prefix = prefix;
			this.message = message;
			this.muteMenuItem = muteMenuItem;
			this.messageType = messageType == null ? MessageType.SYSTEM : messageType;
			this.targets = Utils.isNullOrEmpty(targets) ? List.of(Target.INGAME, Target.DISCORD) : targets;

			for (Target target : this.targets)
				target.execute(this);
		}

		public static BroadcastBuilder all() {
			return new BroadcastBuilder().targets(Target.INGAME, Target.DISCORD);
		}

		public static BroadcastBuilder ingame() {
			return new BroadcastBuilder().targets(Target.INGAME);
		}

		public static BroadcastBuilder discord() {
			return new BroadcastBuilder().targets(Target.DISCORD);
		}

		public static BroadcastBuilder staff() {
			return all().channel(StaticChannel.STAFF);
		}

		public static BroadcastBuilder staffIngame() {
			return ingame().channel(StaticChannel.STAFF);
		}

		public static BroadcastBuilder staffDiscord() {
			return discord().channel(StaticChannel.STAFF);
		}

		public static BroadcastBuilder log() {
			return staff().targets(Target.LOG);
		}

		public static BroadcastBuilder admin() {
			return all().channel(StaticChannel.ADMIN);
		}

		public static BroadcastBuilder adminIngame() {
			return ingame().channel(StaticChannel.ADMIN);
		}

		public static BroadcastBuilder adminDiscord() {
			return discord().channel(StaticChannel.ADMIN);
		}

		@AllArgsConstructor
		public enum Target {
			INGAME(StringUtils::getPrefix) {
				@Override
				void execute(Broadcast broadcast) {
					final ComponentLike component = getMessage(broadcast);
					Bukkit.getConsoleSender().sendMessage(AdventureUtils.stripColor(component));
					List<Player> players = PlayerUtils.getOnlinePlayers();

					if (broadcast.channel != null && broadcast.sender != Identity.nil()) {
						Set<Chatter> recipients = broadcast.channel.getRecipients(new ChatterService().get(broadcast.sender.uuid()));
						players = recipients.stream().map(Chatter::getOnlinePlayer).toList();
					}

					players.stream()
							.map(player -> new ChatterService().get(player))
							.filter(chatter -> chatter.hasJoined(broadcast.channel))
							.filter(chatter -> !MuteMenuUser.hasMuted(chatter.getOfflinePlayer(), broadcast.muteMenuItem))
							.forEach(chatter -> chatter.sendMessage(broadcast.sender, component, broadcast.messageType));
				}
			},
			DISCORD(StringUtils::getDiscordPrefix) {
				@Override
				void execute(Broadcast broadcast) {
					if (broadcast.channel.getDiscordTextChannel() != null)
						Discord.send(AdventureUtils.asPlainText(getMessage(broadcast)), broadcast.channel.getDiscordTextChannel());
				}
			},
			LOG(StringUtils::getDiscordPrefix) {
				@Override
				void execute(Broadcast broadcast) {
					Discord.send(getMessage(broadcast).toString(), TextChannel.STAFF_LOG);
				}
			};

			private final Function<String, String> prefixFormatter;

			abstract void execute(Broadcast broadcast);

			ComponentLike getMessage(Broadcast broadcast) {
				if (broadcast.prefix == null)
					return broadcast.message;
				else
					return new JsonBuilder(prefixFormatter.apply(broadcast.prefix)).next(broadcast.message);
			}

		}

		public static class BroadcastBuilder {

			public BroadcastBuilder channel(PublicChannel channel) {
				this.channel = channel;
				return this;
			}

			public BroadcastBuilder channel(StaticChannel channel) {
				return channel(channel.getChannel());
			}

			public BroadcastBuilder channel(String channel) {
				return channel(ChatManager.getChannel(channel));
			}

			public BroadcastBuilder sender(Identity sender) {
				this.sender = sender;
				return this;
			}

			public BroadcastBuilder sender(Identified sender) {
				return sender(AdventureUtils.identityOf(sender));
			}

			public BroadcastBuilder sender(UUID sender) {
				return sender(AdventureUtils.identityOf(sender));
			}

			public BroadcastBuilder prefix(String prefix) {
				this.prefix = prefix;
				return this;
			}

			public BroadcastBuilder message(ComponentLike message) {
				this.message = message;
				return this;
			}

			public BroadcastBuilder message(String message) {
				return message(AdventureUtils.fromLegacyText(colorize(message)));
			}

			public BroadcastBuilder targets(Target... targets) {
				if (this.targets == null)
					this.targets = new ArrayList<>();

				this.targets.addAll(List.of(targets));
				return this;
			}

		}
	}

}
