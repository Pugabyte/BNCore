package me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams;

import com.google.common.collect.ImmutableSet;
import eden.utils.TimeUtils.Time;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lexikiq.OptionalPlayer;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.discord.DiscordId;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Match.MatchTasks;
import me.pugabyte.nexus.features.minigames.models.Match.MatchTasks.MatchTaskType;
import me.pugabyte.nexus.features.minigames.models.MatchData;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.MultiplayerMechanic;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.discord.DiscordUserService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.RandomUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TeamMechanic extends MultiplayerMechanic {
	public static final @NotNull Set<String> TEAM_VOICE_CHANNELS = Set.of(
		DiscordId.VoiceChannel.RED.getId(),
		DiscordId.VoiceChannel.BLUE.getId(),
		DiscordId.VoiceChannel.GREEN.getId(),
		DiscordId.VoiceChannel.YELLOW.getId(),
		DiscordId.VoiceChannel.WHITE.getId()
	);
	public static final @NotNull Set<String> MINIGAME_VOICE_CHANNELS = ImmutableSet.copyOf(Arrays.stream(DiscordId.VoiceChannel.values()).map(DiscordId.VoiceChannel::getId).collect(Collectors.toSet()));
	private static final @NotNull Component RETURN_VC = new JsonBuilder().newline().next("&e&lClick here&f&3 to return to the Minigames voice channel.").command("voicechannel "+DiscordId.VoiceChannel.MINIGAMES.getId()).newline().build();

	// TODO: add spectators to all team channels (read-only)?
	// TODO: spectator chat? (git#26)

	@Override
	public boolean isTeamGame() {
		return true;
	}

	public boolean usesTeamChannels() {
		return true;
	}

	public static @Nullable Member getVoiceChannelMember(@NotNull OptionalPlayer hasPlayer) {
		Player player = hasPlayer.getPlayer();
		if (player == null) return null;

		Guild guild = Discord.getGuild();
		if (guild == null) return null;

		DiscordUser discordUser = new DiscordUserService().get(player);
		Member member = discordUser.getMember();
		if (member == null) {
			// user has no linked account, find a disc account with matching (nick)name
			Optional<Member> optionalMember = guild.getMembers().stream()
					.filter(fmember -> (fmember.getNickname() != null && fmember.getNickname().equalsIgnoreCase(Name.of(player)))
							|| fmember.getUser().getName().equalsIgnoreCase(Name.of(player)))
					.findAny();
			if (optionalMember.isPresent())
				member = optionalMember.get();
			else
				return null;
		}
		if (member.getVoiceState() == null || !member.getVoiceState().inVoiceChannel())
			return null;
		return member;
	}

	public final void joinTeamChannel(@Nullable Team team, @NotNull List<Minigamer> teamMembers) {
		if (!usesTeamChannels()) return;
		if (teamMembers.isEmpty()) return;
		if (team == null) {
			teamMembers.forEach(this::leaveTeamChannel);
			return;
		}

		ChatColor chatColor = team.getChatColor();
		DiscordId.VoiceChannel vcEnum;

		if (chatColor == ChatColor.RED || chatColor == ChatColor.DARK_RED)
			vcEnum = DiscordId.VoiceChannel.RED;
		else if (chatColor == ChatColor.BLUE || chatColor == ChatColor.AQUA || chatColor == ChatColor.DARK_AQUA || chatColor == ChatColor.DARK_BLUE)
			vcEnum = DiscordId.VoiceChannel.BLUE;
		else if (chatColor == ChatColor.WHITE || chatColor == ChatColor.GRAY)
			vcEnum = DiscordId.VoiceChannel.WHITE;
		else if (chatColor == ChatColor.GREEN || chatColor == ChatColor.DARK_GREEN)
			vcEnum = DiscordId.VoiceChannel.GREEN;
		else if (chatColor == ChatColor.YELLOW || chatColor == ChatColor.GOLD)
			vcEnum = DiscordId.VoiceChannel.YELLOW;
		else
			vcEnum = null;

		JsonBuilder voiceMessageBuilder = new JsonBuilder();
		if (vcEnum != null) {
			voiceMessageBuilder.newline().next("&e&lClick here&f&3 to join your team's voice channel").command("voicechannel " + vcEnum.getId());
			voiceMessageBuilder.initialize();
		} else return;

//		if (teamChannel == null && !voiceMessageBuilder.isInitialized()) return;

		Component message;
		if (voiceMessageBuilder.isInitialized())
			message = voiceMessageBuilder.build();
		else
			message = Component.text().asComponent(); // not rly necessary but it makes IDE stop yelling that it's not initialized

		teamMembers.forEach(minigamer -> {
			// add voice channel text if present and if user is in voice
			Member member = getVoiceChannelMember(minigamer);
			if (member != null) {
				// getVoiceChannelMember ensures these aren't null, but IDE is silly, so let's help it out
				assert member.getVoiceState() != null;
				assert member.getVoiceState().getChannel() != null;

				if (MINIGAME_VOICE_CHANNELS.contains(member.getVoiceState().getChannel().getId()))
					minigamer.getPlayer().sendMessage(message);
			}
		});
	}

	public final void joinTeamChannel(@NotNull Minigamer minigamer) {
		joinTeamChannel(minigamer.getTeam(), Collections.singletonList(minigamer));
	}

	public final void joinTeamChannel(@NotNull Team team, @NotNull Match match) {
		joinTeamChannel(team, team.getAliveMinigamers(match));
	}

	public final void leaveTeamVoiceChannel(@NotNull Minigamer minigamer) {
		if (!usesTeamChannels()) return;

		Member member = getVoiceChannelMember(minigamer);
		if (member == null) return;
		// getVoiceChannelMember ensures these aren't null, but IDE is silly, so let's help it out
		assert member.getVoiceState() != null;
		assert member.getVoiceState().getChannel() != null;

		if (TEAM_VOICE_CHANNELS.contains(member.getVoiceState().getChannel().getId()))
			minigamer.getPlayer().sendMessage(RETURN_VC);
	}

	public final void leaveTeamChannel(@NotNull Minigamer minigamer) {
		leaveTeamVoiceChannel(minigamer);
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		event.getMatch().getMinigamers().forEach(this::leaveTeamChannel);
		super.onEnd(event);
	}

	/**
	 * Whether or not this Minigame uses auto-balancing (tries to balance the teams upon player death).
	 * Recommended to disable for gamemodes that manually set teams like Infection.
	 * @return whether to enable auto balancing
	 */
	public boolean usesAutoBalancing() {
		return true;
	}

	@Override
	public void announceWinners(@NotNull Match match) {
		Arena arena = match.getArena();
		Map<Team, Integer> scores = match.getScores();

		int winningScore = getWinningScore(scores.values());
		List<Team> winners = getWinningTeams(winningScore, scores);

		String announcement = null;
		if (winningScore == 0)
			announcement = "No teams scored in ";
		else if (arena.getTeams().size() == winners.size())
			announcement = "All teams tied in ";

		JsonBuilder builder = new JsonBuilder();
		builder.next(announcement == null ? getWinnersComponent(winners) : Component.text(announcement));
		builder.next(arena);
		if (winningScore != 0)
			builder.next(" (" + winningScore + ")");

		Minigames.broadcast(builder);
	}

	protected @NotNull List<Team> getWinningTeams(int winningScore, @NotNull Map<Team, Integer> scores) {
		List<Team> winners = new ArrayList<>();

		for (Team team : scores.keySet())
			if (scores.getOrDefault(team, 0).equals(winningScore))
				winners.add(team);

		return winners;
	}

	private @NotNull String getScoreList(@NotNull Map<ChatColor, Integer> scores) {
		StringBuilder scoreList = new StringBuilder(" &3( ");
		int counter = 0;
		for (ChatColor color : scores.keySet()) {
			scoreList.append(color)
					.append(scores.get(color).toString())
					.append(ChatColor.DARK_AQUA)
					.append(++counter != scores.size() ? " | " : " )");
		}
		return scoreList.toString();
	}

	protected @Nullable Team getSmallestTeam(@NotNull List<Minigamer> minigamers, @NotNull List<Team> teams) {
		Map<Team, Integer> assignments = getCurrentAssignments(minigamers, teams);
		return getSmallestTeam(assignments);
	}

	protected @Nullable Team getSmallestTeam(@NotNull Map<Team, Integer> assignments) {
		Team smallest = null;
		int min = Integer.MAX_VALUE;
		for (Map.Entry<Team, Integer> entry : assignments.entrySet()) {
			if (entry.getValue() < min) {
				smallest = entry.getKey();
				min = entry.getValue();
			}
		}

		return smallest;
	}

	private @NotNull Map<Team, Integer> getCurrentAssignments(@NotNull List<Minigamer> minigamers, @NotNull List<Team> teams) {
		Map<Team, Integer> assignments = new HashMap<>();
		teams.forEach(team -> assignments.put(team, 0));
		minigamers.forEach(minigamer -> {
			if (minigamer.getTeam() != null)
				assignments.put(minigamer.getTeam(), assignments.get(minigamer.getTeam()) + 1);
		});
		return assignments;
	}

	@Override
	public boolean shouldBeOver(@NotNull Match match) {
		Set<Team> teams = new HashSet<>();
		match.getMinigamers().stream().filter(Minigamer::isAlive).forEach(minigamer -> teams.add(minigamer.getTeam()));
		if (teams.size() == 1) {
			Nexus.log("Match has only one team left, ending");
			return true;
		}

		int winningScore = match.getArena().getCalculatedWinningScore(match);
		if (winningScore > 0)
			for (Team team : teams)
				if (team.getScore(match) >= winningScore) {
					match.getMatchData().setWinnerTeam(team);
					Nexus.log("Team match has reached calculated winning score (" + winningScore + "), ending");
					return true;
				}

		return false;
	}

	public void onTurnStart(@NotNull Match match, @NotNull Team team) {
		match.getMatchData().setTurnStarted(LocalDateTime.now());
	}

	public void onTurnEnd(@NotNull Match match, @NotNull Team team) {

	}

	public void nextTurn(@NotNull Match match) {
		Arena arena = match.getArena();
		MatchData matchData = match.getMatchData();
		MatchTasks tasks = match.getTasks();

		if (match.isEnded() || matchData == null)
			return;

		if (matchData.getTurnTeam() != null) {
			onTurnEnd(match, matchData.getTurnTeam());
			matchData.setTurnTeam(null);
		}

		if (shouldBeOver(match)) {
			end(match);
			return;
		}

		if (matchData.getTurns() >= match.getArena().getMaxTurns()) {
			match.broadcast("Max turns reached, ending game");
			match.end();
			return;
		}

		if (matchData.getTurnTeamList().isEmpty()) {
			matchData.setTurnTeamList(new ArrayList<>(match.getAliveTeams()));
			if (shuffleTurnList())
				Collections.shuffle(matchData.getTurnTeamList());
		}

		tasks.cancel(MatchTaskType.TURN);

		Team team = matchData.getTurnTeamList().get(0);
		matchData.getTurnTeamList().remove(team);
		matchData.setTurnTeam(team);
		match.getScoreboard().update();

		onTurnStart(match, team);
		tasks.register(MatchTaskType.TURN, tasks.wait(arena.getTurnTime() * Time.SECOND.get(), () -> nextTurn(match)));
	}

	@Override
	public void onQuit(@NotNull MinigamerQuitEvent event) {
		Match match = event.getMatch();
		Team team = event.getMinigamer().getTeam();
		if (team != null && team.equals(match.getMatchData().getTurnTeam()))
			if (team.getAliveMinigamers(match).size() == 0)
				nextTurn(match);

		leaveTeamChannel(event.getMinigamer());

		super.onQuit(event);
	}

	@Override
	public int getMultiplier(@NotNull Match match, @NotNull Minigamer minigamer) {
		int winningScore = match.getWinningScore();
		Team team = minigamer.getTeam();
		// ignore losing teams
		if (team.getScore(match) < winningScore || winningScore <= 0)
			return 0;
		return super.getMultiplier(match, minigamer);
	}

	public final boolean basicBalanceCheck(@NotNull List<Minigamer> minigamers) {
		if (minigamers.isEmpty())
			return false;

		Match match = minigamers.get(0).getMatch();
		Arena arena = match.getArena();
		List<Team> teams = new ArrayList<>(arena.getTeams());

		int required = 0;
		for (Team team : teams) required += team.getMinPlayers();

		if (match.getMinigamers().size() < required) {
			error("Not enough players to meet team requirements!", match);
			return false;
		}

		return true;
	}

	protected @NotNull List<BalanceWrapper> getBalanceWrappers(@NotNull Minigamer minigamer) {
		return getBalanceWrappers(minigamer.getMatch());
	}

	protected @NotNull List<BalanceWrapper> getBalanceWrappers(@NotNull Match match) {
		// ALL PERCENTAGES HERE RANGE FROM 0 to 1 !!
		List<Team> teams = match.getArena().getTeams();
		List<BalanceWrapper> wrappers = new ArrayList<>();
		double percentageSum = 0; // sum of all balance percentages
		int noPercentage = 0; // count of teams w/o balance percentages
		for (Team team : teams) {
			BalanceWrapper wrapper = new BalanceWrapper(team, match);
			wrappers.add(wrapper);
			if (wrapper.getPercentage() != null)
				percentageSum += wrapper.getPercentage();
			else
				noPercentage++;
		}

		if (noPercentage > 0 && percentageSum < 1) {
			// evenly split the balance of teams that don't have a balance percentage (if there is any unassigned %)
			double percentage = (1d/noPercentage) * (1d-percentageSum);
			wrappers.stream().filter(wrapper -> wrapper.getPercentage() == null).forEach(wrapper -> wrapper.setPercentage(percentage));
		}

		// ensure percentages add up to 100
		double totalPercentage = wrappers.stream().mapToDouble(BalanceWrapper::getPercentage).sum();
		wrappers.forEach(wrapper -> wrapper.setPercentage(wrapper.getPercentage() / totalPercentage));

		return wrappers;
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		// auto-balancing
		super.onDeath(event);
		if (!usesAutoBalancing())
			return;
		Match match = event.getMatch();
		Minigamer minigamer = event.getMinigamer();
		if (!minigamer.isAlive() || match.isEnded())
			return;
		if (minigamer.getTeam().getMinigamers(match).size()-1 < minigamer.getTeam().getMinPlayers())
			return;

		List<BalanceWrapper> wrappers = getBalanceWrappers(match).stream()
				.filter(wrapper -> !wrapper.getTeam().equals(minigamer.getTeam()) && // only try to auto-balance to other teams
						wrapper.percentageDiscrepancy() > 0 &&
						wrapper.getNeededPlayers() != -1 &&
						wrapper.extraPlayerPercentDiscrepancy() >= 0).collect(Collectors.toList());
		if (wrappers.isEmpty())
			return;
		// sort teams by closest to being equal (inverse of natural sort)
		wrappers.sort(Collections.reverseOrder());
		// select randomly if multiple teams are equal
		List<BalanceWrapper> randomWrappers = new ArrayList<>();
		randomWrappers.add(wrappers.get(0));
		double val = wrappers.get(0).extraPlayerPercentDiscrepancy();
		int index = 1; // iterator var
		while (index < wrappers.size() && Math.abs(wrappers.get(index).extraPlayerPercentDiscrepancy() - val) < 0.0001d) {
			randomWrappers.add(wrappers.get(index));
			index++;
		}
		// assign team
		Team team = RandomUtils.randomElement(randomWrappers).getTeam();
		minigamer.setTeam(team);
		minigamer.tell("", false);
		minigamer.tell("&3You have been auto balanced to "+team.getColoredName());
	}

	@Override
	public void balance(@NotNull List<Minigamer> minigamers) {
		minigamers = new ArrayList<>(minigamers); // cries in pass by reference
		if (!basicBalanceCheck(minigamers))
			return;

		minigamers.forEach(minigamer -> minigamer.setTeam(null)); // clear teams
		Collections.shuffle(minigamers); // lets us assign teams to players in random order
		Match match = minigamers.get(0).getMatch();
		List<Team> teams = match.getArena().getTeams(); // old code made a new list so im doing it too

		// only one team, no need to bother with math
		if (teams.size() == 1) {
			minigamers.forEach(minigamer -> minigamer.setTeam(teams.get(0)));
			return;
		}

		// create wrapper objects
		List<BalanceWrapper> wrappers = getBalanceWrappers(match);

		// add players to teams that need them (i.e. have a minimum player count that is not satisfied)
		while (!minigamers.isEmpty()) {
			Optional<BalanceWrapper> needsPlayers = wrappers.stream().filter(wrapper -> wrapper.getNeededPlayers() > 0).findFirst();
			if (!needsPlayers.isPresent())
				break;
			Team team = needsPlayers.get().getTeam();
			minigamers.remove(0).setTeam(team);
		}

		// add rest of players according to percentages
		while (!minigamers.isEmpty()) {
			// this basically finds the team with the largest percent
			wrappers = wrappers.stream().filter(wrapper -> wrapper.getNeededPlayers() != -1).sorted().collect(Collectors.toList());
			if (wrappers.isEmpty())
				break;
			// get teams with matching percentage discrepancies (ie the teams are perfectly balanced) and randomly
			//  select one of them
			List<BalanceWrapper> equalWrappers = new ArrayList<>();
			equalWrappers.add(wrappers.get(0));
			int index = 1;
			double val = wrappers.get(0).percentageDiscrepancy();
			while (index < wrappers.size() && Math.abs(wrappers.get(index).percentageDiscrepancy() - val) < 0.0001d) {
				equalWrappers.add(wrappers.get(index));
				index++;
			}
			Team team = RandomUtils.randomElement(equalWrappers).getTeam();
			minigamers.remove(0).setTeam(team);
		}

		// leftover players means the teams all (somehow) reached their max player count
		minigamers.forEach(minigamer -> {
			minigamer.tell("Could not assign you to a team!");
			minigamer.quit();
		});
	}

	@Data
	@EqualsAndHashCode
	public static class BalanceWrapper implements Comparable<BalanceWrapper> {
		private final Team team;
		private final Match match;
		private Double percentage;
		private BalanceWrapper(Team team, Match match) {
			this.team = team;
			this.match = match;
			if (team.getBalancePercentage() == -1)
				percentage = null;
			else
				percentage = team.getBalancePercentage()/100d;
		}

		/**
		 * How many players this team needs to reach its minimum player quota.
		 * Returns -1 if the team has hit is maximum player count.
		 * @return integer number of players
		 */
		public int getNeededPlayers() {
			int teamPlayers = team.getMinigamers(match).size();
			if (team.getMaxPlayers() > -1 && teamPlayers >= team.getMaxPlayers())
				return -1;
			return Math.max(0, team.getMinPlayers()-teamPlayers);
		}

		public int getTeamPlayers() {
			return team.getMinigamers(match).size();
		}

		public int getTotalPlayers() {
			// should this ignore dead players (spectators)? i'm not sure... i can't think of a minigame that would be
			//  affected by that either way
			return (int) match.getMinigamers().stream().filter(minigamer -> minigamer.getTeam() != null).count();
		}

		/**
		 * Calculates the difference between the team's specified percentage and its current percentage (i.e. the
		 * current balance of the match). A negative score is unbalanced in favor of the team, a positive score is
		 * unbalanced in favor of other teams. Larger scores mean more unbalanced.
		 * @return a score ranging from -1 to 1
		 */
		public double percentageDiscrepancy() {
			return percentageDiscrepancy(getTeamPlayers(), getTotalPlayers());
		}

		/**
		 * Manually calculate the percentage discrepancy of a team
		 * @param teamPlayers players on this team
		 * @param totalPlayers all current players in the minigame
		 * @return a score ranging from -1 to 1
		 */
		public double percentageDiscrepancy(int teamPlayers, int totalPlayers) {
			double matchPercentage;
			if (totalPlayers == 0)
				matchPercentage = 0; // this is the first added player, assume all teams are on 0%
			else
				matchPercentage = (double)teamPlayers/totalPlayers; // get % of players on this team
			return percentage-matchPercentage;
		}

		public double extraPlayerPercentDiscrepancy() {
			return percentageDiscrepancy(getTeamPlayers()+1, getTotalPlayers());
		}

		/**
		 * Compares which of two teams has a larger player discrepancy. A negative value means this team has a larger
		 * discrepancy (to allow for naturally sort in descending order)
		 * @param otherWrapper the other team
		 * @return a score ranging from -1 to 1
		 */
		@Override
		public int compareTo(@NotNull BalanceWrapper otherWrapper) {
			return (int) ((otherWrapper.percentageDiscrepancy()-percentageDiscrepancy())*100);
		}
	}
}
