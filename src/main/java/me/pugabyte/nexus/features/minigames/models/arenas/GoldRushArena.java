package me.pugabyte.nexus.features.minigames.models.arenas;

import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.models.Arena;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ToString(callSuper = true)
@SerializableAs("GoldRushArena")
public class GoldRushArena extends Arena {
	private int mineStackHeight = 0;

	public GoldRushArena(Map<String, Object> map) {
		super(map);
		this.mineStackHeight = (int) map.getOrDefault("mineStackHeight", mineStackHeight);
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("mineStackHeight", mineStackHeight);

		return map;
	}


}
