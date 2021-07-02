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
@SerializableAs("HoleInTheWallArena")
public class HoleInTheWallArena extends Arena {
	private List<Location> designHangerLocation = new ArrayList<>();

	public HoleInTheWallArena(Map<String, Object> map) {
		super(map);
		this.designHangerLocation = (List<Location>) map.getOrDefault("designHangerLocation", designHangerLocation);
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("designHangerLocation", designHangerLocation);

		return map;
	}

}
