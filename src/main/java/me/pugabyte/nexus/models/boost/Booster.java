package me.pugabyte.nexus.models.boost;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.features.discord.Bot;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.discord.DiscordId;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils.Dev;
import me.pugabyte.nexus.utils.StringUtils;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static eden.utils.StringUtils.camelCase;

@Data
@Builder
@Entity("booster")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class Booster implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<Boost> boosts = new ArrayList<>();

	@Data
	@NoArgsConstructor
	@RequiredArgsConstructor
	public static class Boost implements PlayerOwnedObject {
		private int id;
		@NonNull
		private UUID uuid;
		private Boostable type;
		private double multiplier;
		private int duration;
		private LocalDateTime received;
		private LocalDateTime activated;
		private boolean cancelled;

		public Boost(@NonNull UUID uuid, Boostable type, double multiplier, Time duration) {
			this(uuid, type, multiplier, duration.get() / 20);
		}

		public Boost(@NonNull UUID uuid, Boostable type, double multiplier, int duration) {
			this.uuid = uuid;
			this.id = getBooster().getBoosts().size();
			this.type = type;
			this.multiplier = multiplier;
			this.duration = duration;
			this.received = LocalDateTime.now();
		}

		public Booster getBooster() {
			return new BoosterService().get(uuid);
		}

		private BoostConfig config() {
			return new BoostConfigService().get();
		}

		@Override
		public @NotNull String getNickname() {
			if (Dev.KODA.is(this))
				return "Server";
			return PlayerOwnedObject.super.getNickname();
		}

		public String getRefId() {
			return uuid + "#" + id;
		}

		public String getNicknameId() {
			return getNickname() + "#" + id;
		}

		public ItemBuilder getDisplayItem() {
			return type.getDisplayItem().name("&e" + camelCase(type) + " &7- &6" + getMultiplierFormatted());
		}

		@NotNull
		public String getMultiplierFormatted() {
			return StringUtils.stripTrailingZeros(StringUtils.getDf().format(multiplier)) + "x";
		}

		public void activate() {
			if (!canActivate())
				throw new InvalidInputException("This boost cannot be activated");

			if (config().hasBoost(type))
				throw new InvalidInputException("There is already an active " + camelCase(type) + " boost");

			config().addBoost(this);
			activated = LocalDateTime.now();
			DiscordHandler.deleteHistoryAndSendMessage();
			broadcast(getNickname() + " has &aactivated &3a &e" + getMultiplierFormatted() + " " + camelCase(type) + " boost&3!");
			save();
		}

		public void expire() {
			config().removeBoost(this);
			broadcast(getNickname() + "'s &e" + getMultiplierFormatted() + " " + camelCase(type) + " boost &3has &cexpired");
			DiscordHandler.editMessage();
			// TODO Auto start next in queue?
			save();
		}

		public void cancel() {
			config().removeBoost(this);
			cancelled = true;
			broadcast(getNickname() + "'s &e" + getMultiplierFormatted() + " " + camelCase(type) + " boost &3has been &ccancelled");
			DiscordHandler.editMessage();
			save();
		}

		private void broadcast(String message) {
			Chat.broadcastIngame(StringUtils.getPrefix("Boosts") + message, MuteMenuItem.BOOSTS);
			Chat.broadcastDiscord(StringUtils.getDiscordPrefix("Boosts") + message);
		}

		public boolean isActive() {
			if (activated == null)
				return false;
			if (isExpired())
				return false;
			if (isCancelled())
				return false;

			Boost activeBoost = config().getBoost(type);
			if (!activeBoost.equals(this))
				throw new InvalidInputException("Active boost (" + getNicknameId() + ") is not active server boost (" + activeBoost.getNicknameId() + ")");

			return true;
		}

		public boolean isExpired() {
			if (activated == null)
				return false;

			if (isCancelled())
				return true;

			return getExpiration().isBefore(LocalDateTime.now());
		}

		public boolean canActivate() {
			return !isActive() && !isCancelled() && !isExpired();
		}

		@NotNull
		public LocalDateTime getExpiration() {
			return activated.plusSeconds(duration);
		}

		public String getTimeLeft() {
			return Timespan.of(getExpiration()).format() + " left";
		}

		public int getDurationLeft() {
			if (isActive())
				return (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), getExpiration());
			else
				return duration;
		}

		private void save() {
			new BoosterService().save(getBooster());
		}

	}

	@Override
	public @NotNull String getNickname() {
		if (Dev.KODA.is(this))
			return "Server";
		return PlayerOwnedObject.super.getNickname();
	}

	public Boost add(Boost boost) {
		boosts.add(boost);
		return boost;
	}

	public Boost add(Boostable type, double multiplier, Time duration) {
		Boost boost = new Boost(uuid, type, multiplier, duration);
		add(boost);
		return boost;
	}

	public Boost add(Boostable type, double multiplier, int duration) {
		Boost boost = new Boost(uuid, type, multiplier, duration);
		add(boost);
		return boost;
	}

	public Boost get(int id) {
		// Shortcut
		Boost index = boosts.get(id);
		if (index.getId() == id)
			return index;

		for (Boost boost : boosts)
			if (boost.getId() == id)
				return boost;

		throw new InvalidInputException("Boost " + getNickname() + "#" + id + " not found");
	}

	public List<Boost> get(Boostable type) {
		return boosts.stream().filter(boost -> boost.getType() == type).toList();
	}

	public int count(Boostable type) {
		return get(type).size();
	}

	public List<Boost> getNonExpiredBoosts() {
		return getNonExpiredBoosts(boosts);
	}

	public List<Boost> getNonExpiredBoosts(Boostable type) {
		return getNonExpiredBoosts(get(type));
	}

	private List<Boost> getNonExpiredBoosts(List<Boost> boosts) {
		return boosts.stream().filter(boost -> !boost.isExpired()).toList();
	}

	static class DiscordHandler {

		static void deleteHistoryAndSendMessage() {
			if (!Discord.isConnected())
				return;

			deleteHistory(DiscordHandler::sendMessage);
		}

		static void editMessage() {
			if (!Discord.isConnected())
				return;

			getHistory().thenAcceptAsync(history -> {
				if (history.size() == 0) {
					sendMessage();
					return;
				}

				Iterator<Message> iterator = history.iterator();
				Message message = iterator.next();

				while (iterator.hasNext())
					iterator.next().delete().queue();

				message.editMessage(getMessage()).queue();
			});
		}

		@NotNull
		private static Message getMessage() {
			BoostConfig config = BoostConfig.get();

			MessageBuilder builder = new MessageBuilder()
					.append("**Active Boosts**")
					.append(System.lineSeparator())
					.append(System.lineSeparator());

			Set<Boostable> boosts = config.getBoosts().keySet();
			if (boosts.isEmpty())
				builder.append("None");
			else
				for (Boostable type : boosts) {
					Boost boost = config.getBoost(type);
					builder.append(String.format("**%s** %s - %s", boost.getMultiplierFormatted(), camelCase(type), boost.getNickname()))
							.append(System.lineSeparator());
				}

			return builder.build();
		}

		private static void sendMessage() {
			getChannel().sendMessage(getMessage()).queue();
		}

		private static void deleteHistory(Runnable then) {
			getHistory().thenAcceptAsync(history -> {
				Iterator<Message> iterator = history.iterator();
				while (iterator.hasNext()) {
					iterator.next().delete().queue();
					if (!iterator.hasNext())
						then.run();
				}
			});
		}

		@NotNull
		private static CompletableFuture<List<Message>> getHistory() {
			return getChannel().getIterableHistory().takeAsync(100);
		}

		private static TextChannel getChannel() {
			return DiscordId.TextChannel.BOOSTS.get(Bot.RELAY);
		}
	}

}
