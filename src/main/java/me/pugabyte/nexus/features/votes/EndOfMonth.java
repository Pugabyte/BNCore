package me.pugabyte.nexus.features.votes;

import lombok.Data;
import lombok.NonNull;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Koda;
import me.pugabyte.nexus.features.crates.models.CrateType;
import me.pugabyte.nexus.features.discord.DiscordId.User;
import me.pugabyte.nexus.framework.exceptions.NexusException;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.banker.Transaction.TransactionCause;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.models.vote.TopVoter;
import me.pugabyte.nexus.models.vote.VoteService;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.OfflinePlayer;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EndOfMonth {

	static void run() {
		run(null);
	}

	static void run(Month month) {
		if (month == null)
			month = LocalDateTime.now().getMonth().minus(1);

		final Month finalMonth = month;
		Tasks.async(() -> {
			try {
				TopVoterData data = new TopVoterData(finalMonth);
				Nexus.log(data.toString());

				Koda.announce(data.getDiscordMessage());
				writeHtml(data);

				if (data.getMysteryChestWinner() != null)
					CrateType.MYSTERY.give(PlayerUtils.getPlayer(data.getMysteryChestWinner().getUuid()));

				Tasks.sync(() -> {
					BankerService bankerService = new BankerService();
					data.getEco30kWinners().forEach(topVoter -> bankerService.deposit(PlayerUtils.getPlayer(topVoter.getUuid()), 30000, ShopGroup.SURVIVAL, TransactionCause.VOTE_REWARD));
					data.getEco20kWinners().forEach(topVoter -> bankerService.deposit(PlayerUtils.getPlayer(topVoter.getUuid()), 20000, ShopGroup.SURVIVAL, TransactionCause.VOTE_REWARD));
					data.getEco15kWinners().forEach(topVoter -> bankerService.deposit(PlayerUtils.getPlayer(topVoter.getUuid()), 15000, ShopGroup.SURVIVAL, TransactionCause.VOTE_REWARD));
				});
			} catch (NexusException ex) {
				Nexus.warn("[Votes] [End Of Month] " + ex.getMessage());
			}
		});
	}

	@Data
	public static class TopVoterData {
		@NonNull
		private Month month;
		@NonNull
		private List<TopVoter> topVoters;
		private int total;
		private List<Integer> scores;
		private List<TopVoter> first;
		private List<TopVoter> second;
		private List<TopVoter> third;
		private List<TopVoter> npcOrHoloWinners;
		private List<TopVoter> eco30kWinners;
		private List<TopVoter> eco20kWinners;
		private List<TopVoter> eco15kWinners;
		private TopVoter mysteryChestWinner;

		public TopVoterData(Month month) {
			this.month = month;
			compute(new VoteService().getTopVoters(month));
		}

		private void compute(@NonNull List<TopVoter> topVoters) {
			this.topVoters = topVoters;

			total = topVoters.stream().map(TopVoter::getCount).mapToInt(Long::intValue).sum();
			scores = topVoters.stream().map(TopVoter::getCount).map(Long::intValue).distinct().collect(Collectors.toList());
			if (scores.size() < 3)
				throw new NexusException("Not enough top scores, something must be wrong. (Scores: " + scores + ")");

			first = getVotersAt(scores.get(0));
			second = getVotersAt(scores.get(1));
			third = getVotersAt(scores.get(2));

			npcOrHoloWinners = getVotersWith(125);
			eco30kWinners = getVotersWith(100);
			eco20kWinners = getVotersBetween(75, 99);
			eco15kWinners = getVotersBetween(50, 74);

			mysteryChestWinner = RandomUtils.randomElement(getVotersWith(100).stream()
					.filter(topVoter -> !first.contains(topVoter) && !second.contains(topVoter) && !third.contains(topVoter))
					.collect(Collectors.toList()));
		}

		public List<TopVoter> getVotersWith(int count) {
			return topVoters.stream().filter(topVoter -> topVoter.getCount() >= count).collect(Collectors.toList());
		}

		public List<TopVoter> getVotersBetween(int min, int max) {
			return topVoters.stream().filter(topVoter -> topVoter.getCount() >= min && topVoter.getCount() <= max).collect(Collectors.toList());
		}

		private List<TopVoter> getVotersAt(int count) {
			return topVoters.stream().filter(topVoter -> topVoter.getCount() == count).collect(Collectors.toList());
		}

		public String getAsString(TopVoter topVoter) {
			return getAsString(Collections.singletonList(topVoter));
		}

		public String getAsString(List<TopVoter> topVoters) {
			String names = null;
			if (topVoters.size() > 0)
				names = topVoters.stream()
						.filter(Objects::nonNull)
						.map(topVoter -> {
							OfflinePlayer player = PlayerUtils.getPlayer(topVoter.getUuid());
							if (player != null)
								return Name.of(player);
							return "Unknown";
						})
						.collect(Collectors.joining(", "));
			if (names == null || names.length() == 0)
				return "None :(";
			return names;
		}

		public String getDiscordMessage() {
			String msg = "";
			msg += "***Time to congratulate the Top Voters of " + StringUtils.camelCase(month.name()) + "!***";
			msg += System.lineSeparator();
			msg += System.lineSeparator();
			msg += ":first_place:   **First place** ($10/$60,000/3 MC): " + getAsString(first) + " (" + first.get(0).getCount() + ")";
			msg += System.lineSeparator();
			msg += ":second_place:   **Second place** ($5/$45,000/2 MC): " + getAsString(second) + " (" + second.get(0).getCount() + ")";
			msg += System.lineSeparator();
			msg += ":third_place:   **Third place** ($35,000/1 MC): " + getAsString(third) + " (" + third.get(0).getCount() + ")";
			msg += System.lineSeparator();
			msg += System.lineSeparator();
			msg += "(Note: Rewards are (Store Credit/In-Game Money/# of Mystery Chests) - you can choose only one)";
			msg += System.lineSeparator();
			msg += System.lineSeparator();
			msg += ":gift:   **Lucky mystery chest winner:** " + getAsString(mysteryChestWinner) + (mysteryChestWinner == null ? "" : " (" + mysteryChestWinner.getCount() + ")");
			msg += System.lineSeparator();
			msg += System.lineSeparator();
			msg += ":walking: :speech_balloon:   **NPC or Hologram award:** " + getAsString(npcOrHoloWinners);
			msg += System.lineSeparator();
			msg += System.lineSeparator();
			msg += "Message <@" + User.PUGABYTE.getId() + "> to get your reward if you have won something above! (The below economy rewards are automatically applied)";
			msg += System.lineSeparator();
			msg += System.lineSeparator();
			msg += ":gem:   $30,000 bonus: " + getAsString(eco30kWinners);
			msg += System.lineSeparator();
			msg += ":moneybag:   $20,000 bonus: " + getAsString(eco20kWinners);
			msg += System.lineSeparator();
			msg += ":dollar:   $15,000 bonus: " + getAsString(eco15kWinners);
			msg += System.lineSeparator();
			msg += System.lineSeparator();

			if (total > Votes.GOAL)
				msg += "**You've reached the server wide voting goal, congratulations!** Stay tuned for further information from a staff member.";
			else
				msg += "**Unfortunately, the server wide goal was not reached. Get voting this month!**";

			msg += System.lineSeparator();
			msg += System.lineSeparator();
			msg += "**<https://projecteden.gg/vote>**";
			return msg;
		}
	}

	private static void writeHtml(TopVoterData data) {
		Path table = Paths.get("plugins/website/lastmonth_votes_monthly.jhtml");

		try (BufferedWriter writer = Files.newBufferedWriter(table, StandardCharsets.UTF_8)) {
			int index = 0;
			for (TopVoter topVoter : data.getTopVoters()) {
				OfflinePlayer player = PlayerUtils.getPlayer(topVoter.getUuid());
				++index;

				writer.write("  <tr>" + System.lineSeparator());
				writer.write("    <th>" + index + "</th>" + System.lineSeparator());
				writer.write("    <th>" + Name.of(player) + "</th>" + System.lineSeparator());
				writer.write("    <th>" + topVoter.getCount() + "</th>" + System.lineSeparator());
				writer.write("  <tr>" + System.lineSeparator());
			}

			Path total = Paths.get("plugins/website/lastmonth_votes_monthly.jhtml");
			Files.write(total, String.valueOf(data.getTotal()).getBytes());
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
