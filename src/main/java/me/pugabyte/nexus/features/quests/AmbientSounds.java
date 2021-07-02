package me.pugabyte.nexus.features.quests;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.SoundUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.pugabyte.nexus.features.quests.AmbientSounds.AmbientSoundType.UNDERGROUND;

public class AmbientSounds extends Feature {
	int startDelay = Time.SECOND.x(5);
	//
	private final static Map<Player, Integer> undergroundTaskMap = new HashMap<>();
	private final static Sound undergroundLoop = Sound.AMBIENT_CRIMSON_FOREST_LOOP;
	private final static List<Sound> undergroundSounds = Arrays.asList(Sound.AMBIENT_CAVE,
			Sound.AMBIENT_CRIMSON_FOREST_ADDITIONS, Sound.AMBIENT_CRIMSON_FOREST_MOOD);

	@Override
	public void onStop() {
		undergroundTaskMap.forEach((player, integer) -> SoundUtils.stopSound(player, undergroundLoop));
	}

	@Override
	public void onStart() {
		// Random sounds
		Tasks.repeat(startDelay, Time.SECOND.x(15), () -> {
			if (RandomUtils.chanceOf(50)) {
				Sound sound = RandomUtils.randomElement(undergroundSounds);
				Map<Player, Integer> tempMap = new HashMap<>(undergroundTaskMap);

				tempMap.forEach((player, integer) -> new SoundBuilder(sound).receiver(player).category(SoundCategory.AMBIENT).volume(1).pitch(.1).play());
			}
		});

		// Looping Sound Management
		Tasks.repeat(startDelay, Time.SECOND.x(1), () -> {
			for (Player player : PlayerUtils.getOnlinePlayers()) {

				boolean inArea;
				boolean onList;

				// Underground
				inArea = isApplicable(player, UNDERGROUND);
				onList = undergroundTaskMap.containsKey(player);

				if (inArea && !onList) {
					startLoop(player, UNDERGROUND);
				} else if (!inArea && onList) {
					stopLoop(player, UNDERGROUND);
				}
			}
		});
	}

	private void startLoop(Player player, AmbientSoundType type) {
		if (type.equals(UNDERGROUND)) {
			int taskId = Tasks.repeat(0, Time.SECOND.x(37), () -> new SoundBuilder(undergroundLoop).receiver(player).category(SoundCategory.AMBIENT).volume(5).play());

			undergroundTaskMap.put(player, taskId);
		}
	}

	private void stopLoop(Player player, AmbientSoundType type) {
		Integer taskId = null;
		Sound sound = null;
		if (type.equals(UNDERGROUND)) {
			taskId = undergroundTaskMap.get(player);
			undergroundTaskMap.remove(player);

			sound = undergroundLoop;
		}

		if (taskId != null)
			Tasks.cancel(taskId);

		SoundUtils.stopSound(player, sound, SoundCategory.AMBIENT);
	}

	private boolean isApplicable(Player player, AmbientSoundType type) {
		WorldGuardUtils WGUtils = new WorldGuardUtils(player);
		if (type.equals(UNDERGROUND)) {
			return !WGUtils.getRegionsLikeAt(".*_underground", player.getLocation()).isEmpty();
		}

		return false;
	}

	public enum AmbientSoundType {
		UNDERGROUND,
		DUNGEON
	}


}
