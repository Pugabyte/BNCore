package me.pugabyte.nexus.features.minigames.models.arenas;

import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.models.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@SerializableAs("DeathSwapArena")
public class DeathSwapArena extends Arena {
	private List<Location> schematicResetLocations;

	public DeathSwapArena(Map<String, Object> map) {
		super(map);
		this.schematicResetLocations = (List<Location>) map.get("schematicResetLocations");
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("schematicResetLocations", getSchematicResetLocations());

		return map;
	}

}
