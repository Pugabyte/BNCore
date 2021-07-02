package me.pugabyte.nexus.models.lava;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocationConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.utils.WorldGroup;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@Entity("infinite_lava")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, LocationConverter.class})
public class InfiniteLava implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private boolean enabled = true;

	public static final List<WorldGroup> DISABLED_WORLDS = List.of(WorldGroup.MINIGAMES, WorldGroup.SKYBLOCK);

	public boolean isEnabled() {
		if (!isOnline())
			return false;

		if (DISABLED_WORLDS.contains(getWorldGroup()))
			return false;

		return enabled;
	}

}
