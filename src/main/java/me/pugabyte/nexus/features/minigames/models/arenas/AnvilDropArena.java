package me.pugabyte.nexus.features.minigames.models.arenas;

import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.models.Arena;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@SerializableAs("AnvilDropArena")
public class AnvilDropArena extends Arena {
	private List<AnvilDropMap> anvilDropMaps;
	private AnvilDropMap currentMap;
	private List<String> deathMessages;

	public AnvilDropArena(Map<String, Object> map) {
		super(map);
		this.anvilDropMaps = (List<AnvilDropMap>) map.get("anvilDropMaps");

		if (anvilDropMaps != null)
			currentMap = anvilDropMaps.get(0);
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("anvilDropMaps", getAnvilDropMaps());

		return map;
	}

	@Override
	public @NotNull String getRegionBaseName() {
		return "anvildrop_" + getCurrentMap().getName().toLowerCase();
	}

}

