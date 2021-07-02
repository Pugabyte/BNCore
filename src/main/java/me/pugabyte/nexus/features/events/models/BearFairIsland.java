package me.pugabyte.nexus.features.events.models;

import lombok.SneakyThrows;
import me.pugabyte.nexus.features.events.annotations.Region;
import me.pugabyte.nexus.features.events.models.Talker.TalkingNPC;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface BearFairIsland {

	String getEventRegion();

	default String getRegion() {
		return getEventRegion() + "_" + getClass().getAnnotation(Region.class).value();
	}

	default String getRegion(String subregion) {
		return getRegion() + "_" + subregion;
	}

	@SneakyThrows
	default TalkingNPC getNPC(int id) {
		Class<? extends Enum<? extends TalkingNPC>> npcGroup = getNpcGroup();
		for (Enum<? extends TalkingNPC> enumConstant : npcGroup.getEnumConstants()) {
			if (((TalkingNPC) enumConstant).getNpcId() == id)
				return (TalkingNPC) enumConstant;
		}

		return null;
	}

	default Class<? extends Enum<? extends TalkingNPC>> getNpcGroup() {
		return getClass().getAnnotation(BearFairIsland.NPCClass.class).value();
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface NPCClass {
		Class<? extends Enum<? extends TalkingNPC>> value();
	}
}
