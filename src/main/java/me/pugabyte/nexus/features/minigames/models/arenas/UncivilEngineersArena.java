package me.pugabyte.nexus.features.minigames.models.arenas;

import lombok.Data;
import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@SerializableAs("UncivilEngineersArena")
public class UncivilEngineersArena extends CheckpointArena {
	private List<MobPoint> mobPoints = new ArrayList<>();

	public UncivilEngineersArena(Map<String, Object> map) {
		super(map);
		mobPoints = (List<MobPoint>) map.getOrDefault("mobPoints", mobPoints);
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("mobPoints", mobPoints);
		return map;
	}

	@Override
	public @NotNull String getRegionBaseName() {
		return getMechanicName().toLowerCase();
	}

	@Override
	public @NotNull String getSchematicBaseName() {
		return ("minigames/" + getMechanicName() + "/").toLowerCase();
	}

	@Data
	@SerializableAs("MobPoint")
	public static class MobPoint implements ConfigurationSerializable {
		private EntityType type;
		private Location location;

		public MobPoint(Location location, EntityType type) {
			this.type = type;
			this.location = location;
		}

		public MobPoint(Map<String, Object> map) {
			this.type = EntityType.valueOf((String) map.getOrDefault("type", type));
			this.location = (Location) map.getOrDefault("location", location);
		}

		@Override
		public Map<String, Object> serialize() {
			return new LinkedHashMap<>() {{
				put("type", type.name());
				put("location", location);
			}};
		}
	}

}
