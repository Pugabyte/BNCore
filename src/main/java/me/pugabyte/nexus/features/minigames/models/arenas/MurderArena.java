package me.pugabyte.nexus.features.minigames.models.arenas;

import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.models.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@SerializableAs("MurderArena")
public class MurderArena extends Arena {
	private List<Location> scrapPoints = new ArrayList<>();

	public MurderArena(Map<String, Object> map) {
		super(map);
		scrapPoints = (List<Location>) map.getOrDefault("scrapPoints", scrapPoints);
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("scrapPoints", scrapPoints);
		return map;
	}


}
