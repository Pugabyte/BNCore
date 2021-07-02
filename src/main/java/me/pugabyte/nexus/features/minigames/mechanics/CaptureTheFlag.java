package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.mechanics.common.CaptureTheFlagMechanic;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.CaptureTheFlagMatchData;
import me.pugabyte.nexus.features.minigames.models.matchdata.Flag;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;

public final class CaptureTheFlag extends CaptureTheFlagMechanic {
	@Override
	public @NotNull String getName() {
		return "Capture the Flag";
	}

	@Override
	public @NotNull String getDescription() {
		return "Capture the other team's flag to win the game";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.RED_BANNER);
	}

	@Override
	protected void onFlagInteract(Minigamer minigamer, Sign sign) {
		Match match = minigamer.getMatch();
		CaptureTheFlagMatchData matchData = match.getMatchData();
		Arena arena = match.getArena();

		Optional<Team> optionalTeam = arena.getTeams().stream()
				.filter(team -> team.getName().equalsIgnoreCase(stripColor(sign.getLine(2))))
				.findFirst();

		if (optionalTeam.isPresent()) {
			Team clickedTeam = optionalTeam.get();
			Flag clickedFlag = matchData.getFlag(clickedTeam);

			if (clickedFlag == null) {
				clickedFlag = new Flag(sign, match, clickedTeam);
				matchData.addFlag(clickedTeam, clickedFlag);
			}

			Flag carriedFlag = matchData.getFlagByCarrier(minigamer);

			if (clickedTeam == minigamer.getTeam()) {
				if (carriedFlag != null) {
					if (clickedFlag.getCurrentLocation() == null)
						captureFlag(minigamer, carriedFlag.getTeam());
				} else if (clickedFlag.getCurrentLocation() != null)
					returnFlag(minigamer);
			} else if (carriedFlag == null)
				takeFlag(clickedFlag, minigamer);
		}
	}

	@Override
	protected void onEnterKillRegion(Minigamer minigamer) {
		CaptureTheFlagMatchData matchData = minigamer.getMatch().getMatchData();
		Flag flag = matchData.getFlagByCarrier(minigamer);

		if (flag != null) {
			flag.drop(minigamer.getPlayer().getLocation());

			matchData.removeFlagCarrier(minigamer);

			Tasks.wait(5, () -> minigamer.getMatch().broadcast(minigamer.getColoredName() + " &3dropped " + flag.getTeam().getColoredName() + "&3's flag outside the map"));
		}
	}

	@Override
	protected void doFlagParticles(Match match) {
		CaptureTheFlagMatchData matchData = match.getMatchData();

		matchData.getFlags().values().forEach(flag -> {
			if (flag.getCarrier() != null)
				Flag.particle(flag.getCarrier());
		});
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		Minigamer minigamer = event.getMinigamer();
		CaptureTheFlagMatchData matchData = minigamer.getMatch().getMatchData();
		Flag carriedFlag = matchData.getFlagByCarrier(minigamer);
		if (carriedFlag != null) {
			carriedFlag.drop(minigamer.getPlayer().getLocation());

			matchData.removeFlagCarrier(minigamer);

			Match match = event.getMatch();
			flagMessage(match, minigamer.getTeam(), minigamer, minigamer.getColoredName() + " &3dropped " + carriedFlag.getTeam().getColoredName() + "&3's flag", true);
			flagMessage(match, carriedFlag.getTeam(), event.getAttacker(), minigamer.getColoredName() + "&3 dropped your flag", false);
			if (event.getAttacker() != null)
				event.getAttacker().contributionScored(5);
		}
		super.onDeath(event);
	}

	private void returnFlag(Minigamer minigamer) {
		Match match = minigamer.getMatch();
		CaptureTheFlagMatchData matchData = match.getMatchData();

		String message = minigamer.getColoredName() + "&3 returned " + minigamer.getTeam().getColoredName() + "&3's flag";
		match.broadcast(message);
		// broadcast sound & msg to other team(s)
		match.getAliveTeams().stream().filter(team -> !team.equals(minigamer.getTeam())).forEach(team -> flagMessage(match, team, message, Sound.BLOCK_BEACON_DEACTIVATE, 1.1f, false));
		
		flagMessage(match, minigamer.getTeam(), minigamer, minigamer.getColoredName() + "&3 returned your flag", false);

		Flag flag = matchData.getFlag(minigamer.getTeam());
		flag.respawn();
		match.getTasks().cancel(flag.getTaskId());

		minigamer.contributionScored(3);
	}

	private void captureFlag(Minigamer minigamer, Team team) {
		Match match = minigamer.getMatch();
		CaptureTheFlagMatchData matchData = match.getMatchData();

		flagMessage(match, minigamer.getTeam(), minigamer, minigamer.getColoredName() + "&3 captured " + team.getColoredName() + "&3's flag", true);
		flagMessage(match, team, minigamer.getColoredName() + "&3 captured your flag", Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, false);

		minigamer.scored();
		minigamer.contributionScored(19); // above line adds 1 so let's add 19 more
		minigamer.getMatch().scored(minigamer.getTeam());

		matchData.removeFlagCarrier(minigamer);

		Flag flag = matchData.getFlag(team);
		flag.respawn();
		if (shouldBeOver(match))
			match.end();
	}

	private void takeFlag(Flag flag, Minigamer minigamer) {
		Match match = minigamer.getMatch();
		CaptureTheFlagMatchData matchData = match.getMatchData();

		flagMessage(match, minigamer.getTeam(), minigamer, minigamer.getColoredName() + " &3took " + flag.getTeam().getColoredName() + "&3's flag", true);
		flagMessage(match, flag.getTeam(), minigamer.getColoredName() + "&3 took your flag!", false);

		matchData.addFlagCarrier(flag, minigamer);

		flag.despawn();

		match.getTasks().cancel(flag.getTaskId());
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		super.onEnd(event);

		CaptureTheFlagMatchData matchData = event.getMatch().getMatchData();
		Map<Team, Flag> flags = matchData.getFlags();
		for (Map.Entry<Team, Flag> flagEntry : flags.entrySet())
			flagEntry.getValue().respawn();
	}

}
