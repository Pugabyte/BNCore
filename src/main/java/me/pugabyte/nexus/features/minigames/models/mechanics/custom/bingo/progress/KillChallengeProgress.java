package me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.Challenge;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.KillChallenge;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.common.IEntityChallengeProgress;
import me.pugabyte.nexus.utils.StringUtils;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class KillChallengeProgress implements IEntityChallengeProgress {
	@NonNull
	private Minigamer minigamer;
	private final List<EntityType> kills = new ArrayList<>();

	public Set<String> getRemainingTasks(Challenge challenge) {
		final KillChallenge killChallenge = challenge.getChallenge();
		final Set<EntityType> required = killChallenge.getTypes();

		int remaining = killChallenge.getAmount();
		for (EntityType kill : kills) {
			if (!required.contains(kill))
				continue;

			--remaining;

			if (remaining == 0)
				return Collections.emptySet();
		}

		final String entityTypes = required.stream()
			.map(StringUtils::camelCase)
			.collect(Collectors.joining(" or "));

		return Set.of("Kill " + remaining + " " + entityTypes);
	}

}
