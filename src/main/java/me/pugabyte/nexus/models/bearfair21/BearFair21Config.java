package me.pugabyte.nexus.models.bearfair21;

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
import me.pugabyte.nexus.models.PlayerOwnedObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@Entity("bearfair21_config")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class BearFair21Config implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private Map<BearFair21ConfigOption, Boolean> config = new HashMap<>();

	public boolean isEnabled(BearFair21ConfigOption option) {
		return config.getOrDefault(option, false);
	}

	public boolean isDisabled(BearFair21ConfigOption option) {
		return !isEnabled(option);
	}

	public void setEnabled(BearFair21ConfigOption option, boolean enabled) {
		config.put(option, enabled);
	}

	public enum BearFair21ConfigOption {
		WARP,
		EDIT,
		RIDES,
		QUESTS,
		GIVE_REWARDS,
		SKIP_WAITS,
	}

}
