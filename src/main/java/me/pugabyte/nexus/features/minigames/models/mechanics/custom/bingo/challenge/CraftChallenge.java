package me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.common.IItemChallenge;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.common.ProgressClass;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.CraftChallengeProgress;
import me.pugabyte.nexus.utils.FuzzyItemStack;

import java.util.Set;

@Data
@AllArgsConstructor
@ProgressClass(CraftChallengeProgress.class)
public class CraftChallenge implements IItemChallenge {
	private Set<FuzzyItemStack> items;

	public CraftChallenge(FuzzyItemStack... items) {
		this.items = Set.of(items);
	}

}
