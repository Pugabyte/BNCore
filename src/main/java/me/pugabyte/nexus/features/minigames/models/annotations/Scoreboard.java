package me.pugabyte.nexus.features.minigames.models.annotations;

import me.pugabyte.nexus.features.minigames.models.scoreboards.MinigameScoreboard.Type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Scoreboard {
	Type sidebarType() default Type.MATCH;
	boolean teams() default true;
	boolean visibleNameTags() default true; // only applicable if teams() is false
}
