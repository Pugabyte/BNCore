package me.pugabyte.nexus.features.minigames.models.arenas;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.models.Arena;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

@Data
@ToString(callSuper = true)
@SerializableAs("ArcheryArena")
public class ArcheryArena extends Arena {
	private Set<ProtectedRegion> targetRegions = getRegionsLike("target_.*");

	public ArcheryArena(Map<String, Object> map) {
		super(map);
	}

	@Override
	public @NotNull String getRegionBaseName() {
		return "archery";
	}

}
