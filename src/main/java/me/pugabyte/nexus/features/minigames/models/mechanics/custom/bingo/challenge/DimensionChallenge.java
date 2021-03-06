package me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.common.IChallenge;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.common.ProgressClass;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.DimensionChallengeProgress;
import me.pugabyte.nexus.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.World.Environment;

@Data
@AllArgsConstructor
@ProgressClass(DimensionChallengeProgress.class)
public class DimensionChallenge implements IChallenge {
	private Environment dimension;

	@Override
	public Material getDisplayMaterial() {
		return ItemUtils.getDimensionDisplayMaterial(dimension);
	}

}
