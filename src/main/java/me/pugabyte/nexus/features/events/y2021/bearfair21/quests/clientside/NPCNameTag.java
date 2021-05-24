package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.clientside;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import net.minecraft.server.v1_16_R3.EntityArmorStand;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class NPCNameTag {
	int npcId;
	@NonNull UUID playerUuid;
	List<EntityArmorStand> armorStands;

}
