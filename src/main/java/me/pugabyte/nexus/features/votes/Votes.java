package me.pugabyte.nexus.features.votes;

import com.vexsoftware.votifier.model.VotifierEvent;
import eden.utils.TimeUtils.Time;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Chat.Broadcast;
import me.pugabyte.nexus.features.discord.Bot;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.votes.vps.VPS;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotFoundException;
import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.models.boost.BoostConfig;
import me.pugabyte.nexus.models.boost.Boostable;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.discord.DiscordUserService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.setting.Setting;
import me.pugabyte.nexus.models.setting.SettingService;
import me.pugabyte.nexus.models.vote.TopVoter;
import me.pugabyte.nexus.models.vote.Vote;
import me.pugabyte.nexus.models.vote.VoteService;
import me.pugabyte.nexus.models.vote.VoteSite;
import me.pugabyte.nexus.models.vote.Voter;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.TimeUtils.Timer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.pugabyte.nexus.utils.RandomUtils.randomInt;
import static me.pugabyte.nexus.utils.StringUtils.plural;
import static me.pugabyte.nexus.utils.Utils.epochSecond;

@NoArgsConstructor
public class Votes extends Feature implements Listener {
	static final int GOAL = 6000;

	@Override
	public void onStart() {
		scheduler();

		new Timer("    EndOfMonth", () -> Nexus.getCron().schedule("00 00 1 * *", EndOfMonth::run));
	}

	private void scheduler() {
		Tasks.repeatAsync(Time.SECOND.x(5), Time.SECOND.x(10), () -> {
			// Don't try to process votes if discord is offline, reminders will break
			if (Discord.getGuild() == null)
				return;

			VoteService service = new VoteService();
			service.getActiveVotes().forEach(vote -> {
				LocalDateTime expiration = vote.getTimestamp().plusHours(vote.getSite().getExpirationHours());
				if (!expiration.isBefore(LocalDateTime.now())) return;

				vote.setExpired(true);
				service.save(vote);
				write();

				sendVoteReminder(vote);
			});
		});
	}

	private static MessageEmbed createEmbed(String username) {
		EmbedBuilder builder = new EmbedBuilder().setTitle("https://projecteden.gg/vote").setDescription("");
		for (VoteSite value : VoteSite.values())
			builder.appendDescription(System.lineSeparator() + "**" + value.name().toUpperCase() + "**: [Click to vote!](" + value.getUrl(username) + ")");
		return builder.build();
	}

	private void sendVoteReminder(Vote vote) {
		DiscordUser discordUser = new DiscordUserService().get(UUID.fromString(vote.getUuid()));
		if (discordUser.getUserId() == null)
			return;

		Setting reminders = new SettingService().get(vote.getUuid(), "vote-reminders");
		if (reminders.getValue() != null && !reminders.getBoolean())
			return;

		if (!new CooldownService().check(UUID.fromString(vote.getUuid()), "vote-reminder", Time.MINUTE.x(10)))
			return;

		User user = Bot.KODA.jda().retrieveUserById(discordUser.getUserId()).complete();
		if (user != null && user.getMutualGuilds().size() > 0) {
			String username = Nerd.of(vote.getUuid()).getName();
			Nexus.log("[Votes] Sending vote reminder to " + username);
			MessageBuilder messageBuilder = new MessageBuilder().append("Boop! It's votin' time!").setEmbed(createEmbed(username));
			user.openPrivateChannel().complete().sendMessage(messageBuilder.build()).queue();
		}
	}

	@EventHandler
	public void onVote(VotifierEvent event) {
		String username = event.getVote().getUsername().replaceAll(" ", "");
		OfflinePlayer player = null;
		try { player = PlayerUtils.getPlayer(username); } catch (PlayerNotFoundException ignore) {}
		String name = player != null ? Nickname.of(player) : "Unknown";
		String uuid = player != null ? player.getUniqueId().toString() : "00000000-0000-0000-0000-000000000000";
		VoteSite site = VoteSite.getFromId(event.getVote().getServiceName());

		Nexus.log("[Votes] Vote received from " + event.getVote().getServiceName() + ": " + username + " (" + name + " | " + uuid + ")");

		// MCBIZ is sending votes with a timestamp of an hour ago ??
		LocalDateTime timestamp = epochSecond(event.getVote().getTimeStamp());
		if (site == VoteSite.MCBIZ) {
			long minutes = timestamp.until(LocalDateTime.now(), ChronoUnit.MINUTES);
			if (minutes > 55 && minutes < 65)
				timestamp = timestamp.plusHours(1);
		}

		if (site == null)
			return;

		Vote vote = new Vote(uuid, site, extraVotePoints(), timestamp);
		new VoteService().save(vote);

		int sum = new VoteService().getTopVoters(LocalDateTime.now().getMonth()).stream()
			.mapToInt(topVoter -> Long.valueOf(topVoter.getCount()).intValue()).sum();
		int left = 0;
		if (GOAL > sum)
			left = GOAL - sum;

		if (new CooldownService().check(UUID.fromString(uuid), "vote-announcement", Time.HOUR)) {
			String message = " &3for the server and received &b" + basePoints + plural(" &3vote point", basePoints) + " per site! ";
			if (left > 0)
				message += "&e" + left + " &3more votes needed to hit the goal";

			Broadcast.ingame().message("&a[✔] &3" + name + " &bvoted" + message).send();
			Broadcast.discord().message(":white_check_mark: **" + name + " voted**" + message).send();
		}

		if (vote.getExtra() > 0) {
			Broadcast.ingame().message("&3[✦] &e" + name + " &3received &e" + vote.getExtra() + " extra &3vote points!").send();
			Broadcast.discord().message(":star: **" + name + "** received **" + vote.getExtra() + "** extra vote points!").send();
		}

		if (player != null) {
			int points = vote.getExtra() + basePoints;
			new Voter(player).givePoints(points);
			PlayerUtils.send(player, VPS.PREFIX + "You have received " + points + plural(" point", points));
		}

		Tasks.async(Votes::write);
	}

	private static final int basePoints = 1;
	private static final Map<Integer, Integer> extras = new HashMap<>() {{
		put(1500, 50);
		put(500, 25);
		put(200, 15);
		put(100, 10);
		put(50, 5);
	}};

	@NotNull
	protected static Map<Integer, Integer> getExtras() {
		double multiplier = BoostConfig.multiplierOf(Boostable.VOTE_POINTS);

		return new HashMap<>() {{
			extras.forEach((chance, amount) -> put((int) (chance / multiplier), amount));
		}};
	}

	private int extraVotePoints() {
		for (Map.Entry<Integer, Integer> pair : getExtras().entrySet())
			if (randomInt(pair.getKey()) == 1)
				return pair.getValue();
		return 0;
	}

	protected static void write() {
		Tasks.async(() -> {
			List<TopVoter> topVoters = new VoteService().getTopVoters(LocalDateTime.now().getMonth());

			try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("plugins/website/votes_monthly_top.html"), StandardCharsets.UTF_8)) {
				int index = 0;
				for (TopVoter topVoter : topVoters) {
					if (++index <= 3) {
						String name = "Unknown";
						try { name = Nerd.of(topVoter.getUuid()).getName(); } catch (PlayerNotFoundException ignore) {}

						writer.write("<div class=\"col-sm-4\">" + System.lineSeparator());
						writer.write("  <h3 style=\"text-align: center;\">#" + index + "</h3>" + System.lineSeparator());
						writer.write("  <img class=\"center\" style=\"border-radius: 12px; width: 75%%\" src=\"https://crafatar.com/avatars/" + topVoter.getUuid() + "?overlay\">" + System.lineSeparator());
						writer.write("  <h3 style=\"text-align: center;\">" + name + "</h3>" + System.lineSeparator());
						writer.write("  <h4 style=\"text-align: center;\">" + topVoter.getCount() + "</p>" + System.lineSeparator());
						writer.write("</div>" + System.lineSeparator());
					} else
						break;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}

			try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("plugins/website/votes_monthly.html"), StandardCharsets.UTF_8)) {
				int index = 0;
				for (TopVoter topVoter : topVoters) {
					if (++index > 3) {
						if (index < 54) {
							String name = "Unknown";
							try {
								name = Nerd.of(topVoter.getUuid()).getName();
							} catch (PlayerNotFoundException ignore) {}

							writer.write("  <tr>" + System.lineSeparator());
							writer.write("    <th>" + index + "</th>" + System.lineSeparator());
							writer.write("    <th>" + name + "</th>" + System.lineSeparator());
							writer.write("    <th>" + topVoter.getCount() + "</th>" + System.lineSeparator());
							writer.write("  </tr>" + System.lineSeparator());
						} else
							break;
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}


			try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("plugins/website/votes.html"), StandardCharsets.UTF_8)) {
				List<TopVoter> allTimeTopVoters = new VoteService().getTopVoters();

				int index = 0;
				for (TopVoter topVoter : allTimeTopVoters) {
					if (++index <= 50) {
						String name = "Unknown";
						try {
							name = Nerd.of(topVoter.getUuid()).getName();
						} catch (PlayerNotFoundException ignore) {}

						writer.write("  <tr>" + System.lineSeparator());
						writer.write("	<th>" + index + "</th>" + System.lineSeparator());
						writer.write("	<th>" + name + "</th>" + System.lineSeparator());
						writer.write("	<th>" + topVoter.getCount() + "</th>" + System.lineSeparator());
						writer.write("  </tr>" + System.lineSeparator());
					} else
						break;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			int sum = topVoters.stream().mapToInt(topVoter -> Long.valueOf(topVoter.getCount()).intValue()).sum();
			try {
				Files.write(Paths.get("plugins/website/votes_monthly_total.html"), String.valueOf(sum).getBytes());
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			try {
				List<Vote> activeVotes = new VoteService().getActiveVotes();
				File file = Paths.get("plugins/website/votes_voted.yml").toFile();
				if (!file.exists()) file.createNewFile();
				YamlConfiguration config = new YamlConfiguration();
				activeVotes.forEach(vote -> {
					OfflinePlayer player = PlayerUtils.getPlayer(vote.getUuid());
					String name = Name.of(player);
					if (name == null) return;
					if (!config.isConfigurationSection(name))
						config.createSection(name);
					config.getConfigurationSection(name).set(vote.getSite().name().toLowerCase(), true);
				});
				config.save(file);
			} catch (PlayerNotFoundException ignore) {
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

}
