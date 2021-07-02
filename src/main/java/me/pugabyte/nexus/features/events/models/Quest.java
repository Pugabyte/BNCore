package me.pugabyte.nexus.features.events.models;

import java.util.Map;
import java.util.function.Function;

public interface Quest<T> {

	Function<T, Map<QuestStage, String>> getInstructions();

	default String getInstructions(T user, QuestStage stage) {
		return getInstructions().apply(user).getOrDefault(stage, null);
	}

}
