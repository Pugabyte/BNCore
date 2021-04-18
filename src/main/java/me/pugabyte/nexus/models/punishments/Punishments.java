package me.pugabyte.nexus.models.punishments;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.pugabyte.nexus.features.afk.AFK;
import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocationConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.punishments.Punishments.Punishment.PunishmentBuilder;
import me.pugabyte.nexus.models.punishments.Punishments.Punishment.PunishmentType;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.TimeUtils.Timespan;
import me.pugabyte.nexus.utils.TimeUtils.Timespan.FormatType;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;

@Data
@Builder
@Entity("punishments")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, LocationConverter.class})
public class Punishments extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<Punishment> punishments = new ArrayList<>();
	private List<String> ipHistory = new ArrayList<>();

	public static transient final String PREFIX = StringUtils.getPrefix("Punishments");
	public static transient final String DISCORD_PREFIX = StringUtils.getDiscordPrefix("Punishments");

	public static Punishments of(String name) {
		return of(PlayerUtils.getPlayer(name));
	}

	public static Punishments of(UUID uuid) {
		return of(PlayerUtils.getPlayer(uuid));
	}

	public static Punishments of(PlayerOwnedObject player) {
		return of(player.getUuid());
	}

	public static Punishments of(OfflinePlayer player) {
		return new PunishmentsService().get(player);
	}

	// TODO Other player IP Ban check - service query IP history
	public Optional<Punishment> getAnyActiveBan() {
		return getLastActive(PunishmentType.BAN, PunishmentType.IP_BAN);
	}

	public Optional<Punishment> getActiveBan() {
		return getLastActive(PunishmentType.BAN);
	}

	public Optional<Punishment> getActiveIPBan() {
		return getLastActive(PunishmentType.IP_BAN);
	}

	public Optional<Punishment> getActiveMute() {
		return getLastActive(PunishmentType.MUTE);
	}

	public Optional<Punishment> getActiveFreeze() {
		return getLastActive(PunishmentType.FREEZE);
	}

	public Optional<Punishment> getLastWarn() {
		return getLastActive(PunishmentType.WARN);
	}

	public List<Punishment> getActive(PunishmentType... types) {
		return punishments.stream()
				.filter(punishment -> punishment.isActive() && Arrays.asList(types).contains(punishment.getType()))
				.collect(toList());
	}

	public Optional<Punishment> getLastActive(PunishmentType... types) {
		return getActive(types).stream().max(Comparator.comparing(Punishment::getTimestamp));
	}

	public List<Punishment> getNewWarnings() {
		return getActive(PunishmentType.WARN).stream()
				.filter(punishment -> !punishment.hasBeenReceived())
				.collect(toList());
	}

	public void add(PunishmentBuilder builder) {
		Punishment punishment = builder.uuid(uuid).build();

		if (punishment.getType().isOnlyOneActive())
			deactivatePrevious(punishment);

		punishments.add(punishment);
		punishment.getType().action(punishment);
		punishment.announceStart();

		save();
	}

	private void deactivatePrevious(Punishment punishment) {
		for (Punishment old : getActive(punishment.getType())) {
			old.setReplacedBy(punishment.getPunisher());
			old.setActive(false);
			String typeName = old.getType().name().toLowerCase().replace("_", "-");
			Nerd.of(punishment.getPunisher()).send(PREFIX + "Replacing previous " + typeName + " for &e"
					+ Nickname.of(punishment.getUuid()) + (isNullOrEmpty(old.getReason()) ? "" : "&3: &7" + old.getReason()) + " &3(" + old.getTimeSince() + ")");
		}
	}

	public void remove(Punishment punishment) {
		punishments.remove(punishment);
		punishment.announceEnd();
		punishment.getType().onExpire(punishment);

		save();
	}

	private static void broadcast(String message) {
		Chat.broadcastIngame(PREFIX + message);
		Chat.broadcastDiscord(DISCORD_PREFIX + message);
	}

	private void save() {
		new PunishmentsService().save(this);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Converters({UUIDConverter.class, LocationConverter.class})
	public static class Punishment extends PlayerOwnedObject {
		private UUID id;
		private UUID uuid;
		private UUID punisher;

		private PunishmentType type;
		private String reason;
		private boolean active;

		private LocalDateTime timestamp;
		private int seconds;
		private LocalDateTime expiration;
		private LocalDateTime received;

		private UUID remover;
		private LocalDateTime removed;

		private UUID replacedBy;
		// TODO: For ip bans?
		//  private Set<UUID> related = new HashSet<>();

		@Builder
		public Punishment(@NotNull UUID uuid, @NotNull UUID punisher, @NotNull PunishmentType type, String input) {
			this.id = UUID.randomUUID();
			this.uuid = uuid;
			this.type = type;
			this.punisher = punisher;
			this.timestamp = LocalDateTime.now();
			this.active = true;

			if (type.hasTimespan()) {
				Timespan timespan = Timespan.find(input);
				this.reason = timespan.getRest();
				this.seconds = timespan.getOriginal();
				if (isOnline())
					received();
			} else
				this.reason = input;
		}

		public static PunishmentBuilder ofType(PunishmentType type) {
			return builder().type(type);
		}

		public boolean isActive() {
			LocalDateTime now = LocalDateTime.now();
			if (!active)
				return false;

			if (type.hasTimespan()) {
				if (timestamp != null && timestamp.isAfter(now))
					return false;
				if (expiration != null && expiration.isBefore(now))
					return false;
			}

			return true;
		}

		private boolean hasBeenReceived() {
			return received != null;
		}

		public void received() {
			if (hasBeenReceived())
				return;

			if (!type.isReceivedIfAfk())
				if (isOnline() && AFK.get(getPlayer()).isAfk())
					return;

			actuallyReceived();
		}

		public void actuallyReceived() {
			received = LocalDateTime.now();
			if (type.hasTimespan() && seconds > 0)
				expiration = Timespan.of(seconds).fromNow();
		}

		public void deactivate(UUID remover) {
			this.active = false;
			this.remover = remover;
			announceEnd();
			getType().onExpire(this);
		}

		private void announceStart() {
			String message = "&e" + Nickname.of(punisher) + " &c" + type.getPastTense() + " &e" + getNickname();
			if (seconds > 0)
				message += " &cfor &e" + Timespan.of(seconds).format(FormatType.LONG);

			if (!isNullOrEmpty(reason))
				message += " &cfor &7" + reason;

			broadcast(message);
		}

		private void announceEnd() {
			if (remover != null)
				broadcast("&e" + Nickname.of(remover) + " &3un" + type.getPastTense() + " &e" + getNickname());
		}

		public Component getDisconnectMessage() {
			return Component.text(getType().getDisconnectMessage(this));
		}

		public String getTimeLeft() {
			if (expiration == null)
				if (seconds > 0)
					return Timespan.of(seconds).format() + " left";
				else
					return "forever";
			else
				return Timespan.of(expiration).format() + " left";
		}

		public String getTimeSince() {
			return Timespan.of(timestamp).format() + " ago";
		}

		@Getter
		@AllArgsConstructor
		public enum PunishmentType {
			BAN("banned", true, true, true) {
				@Override
				public void action(Punishment punishment) {
					kick(punishment);
				}

				@Override
				public String getDisconnectMessage(Punishment punishment) {
					return punishment.getReason();
				}
			},
			IP_BAN("ip-banned", true, true, true) { // TODO onlyOneActive ?
				@Override
				public void action(Punishment punishment) {
					kick(punishment);
					// TODO look for alts, kick
				}

				@Override
				public String getDisconnectMessage(Punishment punishment) {
					return punishment.getReason();
				}
			},
			KICK("kicked", false, false, true) {
				@Override
				public void action(Punishment punishment){
					kick(punishment);
				}

				@Override
				public String getDisconnectMessage(Punishment punishment) {
					return punishment.getReason();
				}
			},
			MUTE("muted", true, true, false) {
				@Override
				public void action(Punishment punishment) {
					punishment.send("You have been muted"); // TODO
				}

				@Override
				public void onExpire(Punishment punishment) {
					punishment.send("Your mute has expired");
				}
			},
			WARN("warned", false, false, false) {
				@Override
				public void action(Punishment punishment) {
					punishment.send("You have been warned"); // TODO
				}
			},
			FREEZE("froze", false, true, true) {
				@Override
				public void action(Punishment punishment) {
					punishment.send("&cYou have been frozen! This likely means you are breaking a rule; please pay attention to staff in chat");
				}

				@Override
				public void onExpire(Punishment punishment) {
					punishment.send("&cYou have been unfrozen");
				}
			};

			private final String pastTense;
			@Accessors(fluent = true)
			private final boolean hasTimespan;
			private final boolean onlyOneActive;
			private final boolean receivedIfAfk;

			public abstract void action(Punishment punishment);

			public void onExpire(Punishment punishment) {}

			public String getDisconnectMessage(Punishment punishment) {
				throw new UnsupportedOperationException("Punishment type " + camelCase(this) + " does not have a disconnect message");
			}

			void kick(Punishment punishment) {
				if (punishment.isOnline()) {
					punishment.getPlayer().kick(punishment.getDisconnectMessage());
					punishment.received();
				}
			}
		}

	}

}
