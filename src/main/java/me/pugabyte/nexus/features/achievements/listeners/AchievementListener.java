package me.pugabyte.nexus.features.achievements.listeners;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.achievements.events.AchievementCompletedEvent;
import me.pugabyte.nexus.models.achievement.Achievement;
import me.pugabyte.nexus.models.achievement.AchievementGroup;
import me.pugabyte.nexus.models.achievement.AchievementPlayer;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AchievementListener implements Listener {

	@EventHandler
	public void onAchievementCompleted(AchievementCompletedEvent event) {
		AchievementPlayer achievementPlayer = event.getAchievementPlayer();
		Achievement achievement = event.getAchievement();

		achievementPlayer.addAchievement(achievement);
		achievementPlayer.setAchievementProgress(achievement, null);

		// Spammy...
		if (achievement.getGroup() == AchievementGroup.BIOMES) return;

		Nexus.log(achievementPlayer.getOnlinePlayer().getName() + " has completed the " + achievement.toString() + " achievement");

		Player player = achievementPlayer.getOnlinePlayer();
		if (player.isOnline()) {
			String message = StringUtils.getPrefix("Achievements") + "You have completed the &e" + achievement.toString() + " &3achievement!";
			PlayerUtils.send(player, new JsonBuilder(message).hover("&e" + achievement.getDescription()));
		}

	}

}
