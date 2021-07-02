package me.pugabyte.nexus.features.minigames.models.arenas;

import com.sk89q.worldedit.regions.Region;
import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.models.Arena;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
@ToString(callSuper = true)
@SerializableAs("PixelPaintersArena")
public class PixelPaintersArena extends Arena {
	private Region designRegion = getRegion("designs");
	private Region nextDesignRegion = getRegion("nextdesign");
	private Region lobbyDesignRegion = getRegion("lobbynextdesign");
	private Region logoRegion = getRegion("logo");
	private Region lobbyAnimationRegion = getRegion("lobbyanimation");

	public PixelPaintersArena(Map<String, Object> map) {
		super(map);
	}

	@Override
	public @NotNull String getRegionBaseName() {
		return "pixelpainters";
	}

}
