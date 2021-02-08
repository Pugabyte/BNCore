package me.pugabyte.nexus.models.announcement;

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
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocalDateTimeConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.models.discord.DiscordService;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.vote.VoteService;
import me.pugabyte.nexus.models.vote.VoteSite;
import me.pugabyte.nexus.models.vote.Voter;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Data
@Builder
@Entity("announcement_config")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class AnnouncementConfig extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<Announcement> announcements = new ArrayList<>();

	public Optional<Announcement> findRequestMatch(String id) {
		return announcements.stream()
				.filter(_request -> _request.getId().equalsIgnoreCase(id))
				.findFirst();
	}

	public Announcement getRandomAnnouncement() {
		return RandomUtils.randomElement(announcements);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@RequiredArgsConstructor
	@Converters(LocalDateTimeConverter.class)
	public static class Announcement {
		@NonNull
		private String id;
		@NonNull
		private String text;
		private boolean enabled = true;
		private Set<String> showPermissions = new HashSet<>();
		private Set<String> hidePermissions = new HashSet<>();
		private LocalDateTime startTime;
		private LocalDateTime endTime;
		private AnnouncementCondition condition;

		public void send(Player player) {
			PlayerUtils.send(player, "");
			PlayerUtils.send(player, "&8&l[&b⚡&8&l] &7" + text);
			PlayerUtils.send(player, "");
		}

		public enum AnnouncementCondition {
			VOTE(player -> {
				Voter voter = new VoteService().get(player);
				return voter.getActiveVotes().size() < VoteSite.values().length - 2;
			}),
			DISCORD_LINK(player -> {
				DiscordUser user = new DiscordService().get(player);
				return user.getUserId() == null;
			});

			@Getter
			private final Predicate<Player> condition;

			AnnouncementCondition(Predicate<Player> condition) {
				this.condition = condition;
			}

			public boolean test(Player player) {
				return condition.test(player);
			}
		}
	}

}
