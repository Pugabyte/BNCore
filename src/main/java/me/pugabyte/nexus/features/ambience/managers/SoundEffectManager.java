package me.pugabyte.nexus.features.ambience.managers;

import lombok.Getter;
import me.pugabyte.nexus.features.ambience.effects.sounds.common.SoundEffectConfig;
import me.pugabyte.nexus.features.ambience.managers.common.AmbienceManager;
import me.pugabyte.nexus.models.ambience.AmbienceUser;

import java.util.HashSet;
import java.util.Set;

public class SoundEffectManager extends AmbienceManager {
	@Getter
	private final Set<SoundEffectConfig> effects = new HashSet<>();

	@Override
	public void tick() {

	}

	@Override
	public void init(AmbienceUser user) {
		for (SoundEffectConfig effect : effects)
			effect.init(user);
	}

	@Override
	public void update(AmbienceUser user) {
		if (!user.isSounds())
			return;

		for (SoundEffectConfig effect : effects)
			effect.update(user);
	}

	@Override
	public void onStart() {
	}

}
