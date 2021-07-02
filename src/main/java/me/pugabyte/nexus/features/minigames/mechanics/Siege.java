package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.matchdata.Flag;
import me.pugabyte.nexus.features.minigames.models.matchdata.OneFlagCaptureTheFlagMatchData;
import me.pugabyte.nexus.utils.JsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;
import static me.pugabyte.nexus.utils.Utils.getMax;

public final class Siege extends OneFlagCaptureTheFlag {

	@Override
	public @NotNull String getName() {
		return "Siege";
	}

	@Override
	public @NotNull String getDescription() {
		return "One team protects their flag while the other tries to capture it";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.GREEN_BANNER);
	}

	protected Team getAttackingTeam(Arena arena) {
		return arena.getTeams().stream().filter(team -> team.getName().toLowerCase().contains("attack")).findFirst().orElse(null);
	}

	protected Team getAttackingTeam(Match match) {
		return getAttackingTeam(match.getArena());
	}

	protected Team getDefendingTeam(Arena arena) {
		return arena.getTeams().stream().filter(team -> team.getName().toLowerCase().contains("defend")).findFirst().orElse(null);
	}

	protected Team getDefendingTeam(Match match) {
		return getDefendingTeam(match.getArena());
	}

	@Override
	public void announceWinners(@NotNull Match match) {
		Map<Team, Integer> scores = match.getScores();

		int winningScore = getWinningScore(scores.values());

		Team winningTeam;
		JsonBuilder builder = new JsonBuilder();
		if (winningScore == 0) {
			winningTeam = getDefendingTeam(match);
			builder.next(winningTeam == null ? Component.text("Defenders", NamedTextColor.BLUE) : winningTeam);
			builder.next(" protected the flag on ");
		} else {
			winningTeam = getMax(match.getAliveTeams(), team -> team.getScore(match)).getObject();
			builder.next(winningTeam).next(" captured the flag on ");
		}
		builder.next(match.getArena());
		Minigames.broadcast(builder);
	}

	@Override
	protected void onFlagInteract(Minigamer minigamer, Sign sign) {
		Match match = minigamer.getMatch();
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();

		if (!minigamer.isPlaying(this)) return;

		if (matchData.getFlag() == null)
			matchData.setFlag(new Flag(sign, match));

		if ((ChatColor.GREEN + "Capture").equalsIgnoreCase(sign.getLine(2))) {
			if (!minigamer.equals(matchData.getFlagCarrier()))
				return;

			captureFlag(minigamer);
		} else if (!minigamer.getTeam().getName().equalsIgnoreCase(stripColor(sign.getLine(2))))
			takeFlag(minigamer);
		else if (minigamer.getTeam().getName().equalsIgnoreCase(stripColor(sign.getLine(2))) && !matchData.getFlag().getSpawnLocation().equals(sign.getLocation()))
			returnFlag(minigamer);
	}

	private void returnFlag(Minigamer minigamer) {
		Match match = minigamer.getMatch();
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();

		flagMessage(match.getMinigamers(), minigamer, minigamer.getColoredName() + "&3 returned the flag", true);

		Flag flag = matchData.getFlag();
		if (flag != null) {
			flag.respawn();
			match.getTasks().cancel(flag.getTaskId());
		}
	}

}
