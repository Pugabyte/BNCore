package me.pugabyte.nexus.features.minigames.models.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Railgun {
	int cooldownTicks() default 24;
	boolean damageWithConsole() default false;
}
