package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.mechanics.common.CaptureTheFlagMechanic;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.Flag;
import me.pugabyte.nexus.features.minigames.models.matchdata.OneFlagCaptureTheFlagMatchData;
import me.pugabyte.nexus.utils.Tasks;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;

public class OneFlagCaptureTheFlag extends CaptureTheFlagMechanic {

	@Override
	public @NotNull String getName() {
		return "One Flag Capture the Flag";
	}

	@Override
	public @NotNull String getDescription() {
		return "Find the flag and capture it at the other team's base";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.BLUE_BANNER);
	}

	@Override
	protected void onFlagInteract(Minigamer minigamer, Sign sign) {
		Match match = minigamer.getMatch();
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();

		if (!minigamer.isPlaying(this)) return;
		if (minigamer.isPlaying(Siege.class)) return;

		if ((ChatColor.GRAY + "Neutral").equalsIgnoreCase(sign.getLine(2))) {
			if (matchData.getFlag() == null)
				matchData.setFlag(new Flag(sign, match));

			takeFlag(minigamer);
		} else if ((ChatColor.GREEN + "Capture").equalsIgnoreCase(sign.getLine(2)))
			if (minigamer.equals(matchData.getFlagCarrier()))
				if (!stripColor(sign.getLine(3)).equalsIgnoreCase(minigamer.getTeam().getName()))
					minigamer.tell("&cYou must capture the flag at the other team's base");
				else
					captureFlag(minigamer);
	}

	@Override
	protected void onEnterKillRegion(Minigamer minigamer) {
		OneFlagCaptureTheFlagMatchData matchData = minigamer.getMatch().getMatchData();
		Flag flag = matchData.getFlag();

		if (flag != null) {
			flag.drop(minigamer.getPlayer().getLocation());

			matchData.setFlagCarrier(null);

			Tasks.wait(5, () -> minigamer.getMatch().broadcast(minigamer.getColoredName() + " &3dropped the flag outside the map"));
		}
	}

	@Override
	protected void doFlagParticles(Match match) {
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();
		if (matchData.getFlagCarrier() == null)
			return;

		Flag.particle(matchData.getFlagCarrier());
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		Minigamer minigamer = event.getMinigamer();
		OneFlagCaptureTheFlagMatchData matchData = minigamer.getMatch().getMatchData();
		if (minigamer.equals(matchData.getFlagCarrier())) {
			Flag flag = matchData.getFlag();
			if (flag != null) {
				flag.drop(minigamer.getPlayer().getLocation());

				matchData.setFlagCarrier(null);

				flagMessage(event.getMatch().getMinigamers(), minigamer, minigamer.getColoredName() + "&3 dropped the flag", true);

				if (event.getAttacker() != null)
					event.getAttacker().contributionScored(10);
			}
		}

		super.onDeath(event);
	}

	protected void captureFlag(Minigamer minigamer) {
		Match match = minigamer.getMatch();
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();

		flagMessage(match.getMinigamers(), minigamer.getColoredName() + "&3 captured the flag", true);

		minigamer.scored();
		minigamer.contributionScored(19);
		minigamer.getMatch().scored(minigamer.getTeam());

		matchData.setFlagCarrier(null);

		matchData.getFlag().respawn();
		if (shouldBeOver(match))
			match.end();
	}

	protected void takeFlag(Minigamer minigamer) {
		Match match = minigamer.getMatch();
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();
		Flag flag = matchData.getFlag();

		flagMessage(match.getMinigamers(), minigamer, minigamer.getColoredName() + "&3 took the flag", true);

		matchData.setFlagCarrier(minigamer);

		flag.despawn();

		match.getTasks().cancel(flag.getTaskId());
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		super.onEnd(event);

		OneFlagCaptureTheFlagMatchData matchData = event.getMatch().getMatchData();
		if (matchData.getFlag() != null)
			matchData.getFlag().respawn();
	}

}
